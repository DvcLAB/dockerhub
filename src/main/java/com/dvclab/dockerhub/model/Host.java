package com.dvclab.dockerhub.model;

import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.service.ReverseProxyService;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.jcraft.jsch.JSchException;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.monitor.*;
import one.rewind.network.ssh.SshHost;
import one.rewind.nio.docker.DockerHost;
import one.rewind.nio.json.persister.JSONAbleFieldPersister;
import one.rewind.txt.StringUtil;
import one.rewind.util.FileUtil;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *
 */
@DBName("dockerhub")
@DatabaseTable(tableName = "hosts")
public class Host extends DockerHost {

	// 主机是否支持GPU
	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean gpu_enabled = false;

	@DatabaseField(dataType = DataType.STRING, width = 64, indexName = "uid-idx")
	public String uid;

	public User user;
	// 是否是项目自有的服务器
	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean user_host = false;

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false)
	public int node_exporter_port = 9100;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean ssh_session = false;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String fingerprint;

	// cpu用量
	@DatabaseField(dataType = DataType.FLOAT)
	public float cpu_assign = 0;

	// 内存用量
	@DatabaseField(dataType = DataType.FLOAT)
	public float mem_assign = 0;

	@DatabaseField(persisterClass = JSONAbleFieldPersister.class, columnDefinition = "TEXT")
	public CPUInfo cpu_info = new CPUInfo();

	@DatabaseField(persisterClass = JSONAbleFieldPersister.class, columnDefinition = "TEXT")
	public MemInfo mem_info = new MemInfo();

	@DatabaseField(persisterClass = JSONAbleFieldPersister.class, columnDefinition = "TEXT")
	public IoInfo io_info = new IoInfo();

	@DatabaseField(persisterClass = JSONAbleFieldPersister.class, columnDefinition = "TEXT")
	public NetInfo net_info = new NetInfo();

	@DatabaseField(persisterClass = JSONAbleFieldPersister.class, columnDefinition = "TEXT")
	public GPUInfo gpu_info = null;

	// 主机CPU时序用量
	public Queue<Object[]> cpu_series = new CircularFifoQueue<>(30);
	// 主机网络时序用量
	public Queue<Object[]> network_series = new CircularFifoQueue<>(30);
	// 主机GPU时序用量
	public Queue<Object[]> gpu_series = new CircularFifoQueue<>(30);

	@DatabaseField(dataType = DataType.STRING, width = 8192, canBeNull = false)
	public transient String private_key;

	/**
	 *
	 */
	public Host() {}

	/**
	 *
	 * @param uid
	 * @param fingerprint
	 */
	public Host(String uid, String fingerprint) {
		this.uid = uid;
		this.fingerprint = fingerprint;
		this.user_host = true;
		this.id = StringUtil.md5(uid + "::" + fingerprint);
	}

	/**
	 *
	 * @param ip
	 * @param port
	 * @param node_exporter_port
	 * @param username
	 * @param gpu_enabled
	 * @throws JSchException
	 */
	public Host(String ip, int port, int node_exporter_port, String username, String private_key, boolean gpu_enabled) throws JSchException {
		super(ip, port, username, private_key);
		this.genId();
		this.node_exporter_port = node_exporter_port;
		this.gpu_enabled = gpu_enabled;
		this.ssh_session = true;

		this.container_num.set(0);
		this.runNodeExporter();
	}

	/**
	 * 生成主机ID
	 * @return
	 */
	public Host genId() {
		this.id = StringUtil.md5(ip + port + username);
		return this;
	}

	/**
	 * 构建中台到服务器的ssh连接
	 * @throws JSchException
	 */
	public void connectSshHost() throws JSchException {
		this.sshHost = new SshHost(ip, port, username, this.private_key.getBytes());
		this.sshHost.connect();
		this.ssh_session = true;
	}

	/**
	 * 断开中台到服务器的ssh连接
	 */
	public void disconnectSshHost() {
		this.sshHost.close();
		this.ssh_session = false;
	}

	/**
	 * 获取当前主机可用GPU id列表
	 * @return
	 */
	public List<Integer> getAvailableGPUIdList() {

		if(this.gpu_info != null && this.gpu_info.gpus.size() > 0) {
			return IntStream.range(0, this.gpu_info.gpus.size()).boxed().collect(Collectors.toList());
		}

		return new ArrayList<>();
	}

	/**
	 * 在服务器上运行node-exporter
	 */
	public void runNodeExporter() {
		String cmd = FileUtil.readFileByLines("tpl/docker-run-node-exporter.sh");
		this.exec(cmd);
	}

	/**
	 * 在主机上运行指定容器
	 * @param container
	 * @throws IOException
	 * @throws JSchException
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public void runContainer(Container container) throws IOException, JSchException, DBInitException, SQLException {

		// 复制配置文件
		String name = "jupyterlab-" + container.id + ".yml";
		FileUtil.writeBytesToFile(container.docker_compose_config.getBytes(), name);
		sshHost.copyLocalToRemote(name, name);
		// 构建容器
		exec("docker-compose -f " + name + " -p " + container.id +" up -d");
		// 更新主机容器信息
		container_num.incrementAndGet();
		cpu_assign += container.cpus;
		mem_assign += container.mem;

		new File(name).delete();

		// 同步缓存
		Optional.ofNullable(ContainerCache.containers.get(container.id)).ifPresent(c -> {
			c.host_id = this.id;
			c.user_host = false;
			c.status = Container.Status.Deployed;
		});


		// 同步数据库
		container.host_id = this.id;
		container.user_host = false;
		container.status = Container.Status.Deployed;

		// 更新容器信息
		container.update();
		this.update();
	}

	/**
	 * 暂停容器
	 * @param container
	 * @throws IOException
	 * @throws JSchException
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public void pauseContainer(Container container) throws IOException, JSchException, DBInitException, SQLException {

		// 复制配置文件
		String name = "jupyterlab-" + container.id + ".yml";
		FileUtil.writeBytesToFile(container.docker_compose_config.getBytes(), name);
		sshHost.copyLocalToRemote(name, name);

		// 暂停容器
		exec("docker-compose -f " + name + " -p " + container.id + " stop");

		// 更新主机容器信息
		container_num.decrementAndGet();
		cpu_assign -= container.cpus;
		mem_assign -= container.mem;

		new File(name).delete();

		container.status = Container.Status.Paused;
		// 重置启动时间
		container.begin_run_time = null;

		// 更新缓存
		Optional.ofNullable(ContainerCache.containers.get(container.id)).ifPresent(c -> {
			c.status = Container.Status.Paused;
		});

		// 更新容器信息
		container.update();
		this.update();
	}

	/**
	 * 重新启动容器
	 * @param container
	 * @throws IOException
	 * @throws JSchException
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public void restartContainer(Container container) throws IOException, JSchException, DBInitException, SQLException {

		// 复制配置文件
		String name = "jupyterlab-" + container.id + ".yml";
		FileUtil.writeBytesToFile(container.docker_compose_config.getBytes(), name);
		sshHost.copyLocalToRemote(name, name);

		// 开启容器
		exec("docker-compose -f " + name + " -p " + container.id + " start");

		// 更新主机容器信息
		container_num.incrementAndGet();
		cpu_assign += container.cpus;
		mem_assign += container.mem;

		new File(name).delete();

		container.status = Container.Status.Deployed;
		// 更新缓存
		Optional.ofNullable(ContainerCache.containers.get(container.id)).ifPresent(c -> {
			c.status = Container.Status.Deployed;
		});
		// 更新容器信息
		container.update();
		this.update();
	}

	/**
	 * 删除容器
	 * @param container
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public void removeContainer(Container container) throws DBInitException, SQLException {

		String name = "jupyterlab-" + container.id + ".yml";
		exec("docker-compose -f " + name + " -p " + container.id + " down -v");
		exec("rm " + name);

		cpu_assign -= container.cpus;
		mem_assign -= container.mem;

		container_num.decrementAndGet();
		this.update();
	}

	/**
	 * 获取当前可用的cpu资源
	 * @return
	 */
	public float getCpuAvailable() {
		return this.cpu_info.cpu_num - this.cpu_assign;
	}

	/**
	 * 获取当前可用的mem资源
	 * @return
	 */
	public float getMemAvailable() {
		return this.mem_info.total - this.mem_assign;
	}
}
