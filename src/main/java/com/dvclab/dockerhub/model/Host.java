package com.dvclab.dockerhub.model;

import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.service.ReverseProxyService;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.jcraft.jsch.JSchException;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.io.docker.model.DockerHost;
import one.rewind.io.ssh.SshHost;
import one.rewind.txt.StringUtil;
import one.rewind.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 *
 */
@DBName("dockerhub")
@DatabaseTable(tableName = "hosts")
public class Host extends DockerHost {

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean gpu_enabled = false;

	@DatabaseField(dataType = DataType.STRING, width = 64, indexName = "uid-idx")
	public String uid;

	public User user;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean user_host = false;

	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false)
	public int node_exporter_port = 9100;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean ssh_session = false;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String fingerprint;

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
	 *
	 * @return
	 */
	public Host genId() {
		this.id = StringUtil.md5(ip + port + username);
		return this;
	}

	/**
	 *
	 */
	public void connectSshHost() throws JSchException {
		this.sshHost = new SshHost(ip, port, username, this.private_key.getBytes());
		this.sshHost.connect();
		this.ssh_session = true;
	}

	/**
	 *
	 */
	public void disconnectSshHost() {
		this.sshHost.close();
		this.ssh_session = false;
	}

	/**
	 *
	 */
	public void runNodeExporter() {
		String cmd = FileUtil.readFileByLines("tpl/docker-run-node-exporter.sh");
		this.exec(cmd);
	}

	/**
	 * 创建容器
	 * @param container
	 * @throws IOException
	 * @throws JSchException
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public void runContainer(Container container) throws IOException, JSchException, DBInitException, SQLException {

		container.host_id = this.id;
		container.user_host = false;

		// 复制配置文件
		String name = "jupyterlab-" + container.id + ".yml";
		FileUtil.writeBytesToFile(container.docker_compose_config.getBytes(), name);
		sshHost.copyLocalToRemote(name, name);

		exec("docker-compose -f " + name + " up");
		// exec("rm " + name);
		container_num.incrementAndGet();

		new File(name).delete();

		// 同步缓存
		ContainerCache.containers.get(container.id).host_id = this.id;
		ContainerCache.containers.get(container.id).user_host = false;

		container.update();
		this.update();
	}

	/**
	 * 删除容器
	 * @param container
	 */
	public void removeContainer(Container container) throws DBInitException, SQLException {

		String name = "jupyterlab-" + container.id + ".yml";
		exec("docker-compose -f " + name + " down -v");
		exec("rm " + name);

		container_num.decrementAndGet();
		this.update();
	}
}
