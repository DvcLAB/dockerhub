package com.dvclab.dockerhub.service;

import com.dvclab.dockerhub.DockerHubService;
import com.dvclab.dockerhub.auth.KeycloakAdapter;
import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.model.*;
import com.dvclab.dockerhub.serialization.ServiceMsg;
import com.dvclab.dockerhub.websocket.ContainerInfoPublisher;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.exception.ModelException;
import one.rewind.db.kafka.KafkaClient;
import one.rewind.db.kafka.msg.MsgStringSerializer;
import one.rewind.txt.StringUtil;
import one.rewind.util.FileUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 *
 */
public class ContainerService {

	public static final Logger logger = LogManager.getLogger(ContainerService.class);

	public static ContainerService instance;

	/**
	 * 单例模式
	 * @return
	 */
	public static ContainerService getInstance() throws DBInitException, SQLException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

		if (instance == null) {
			synchronized (ContainerService.class) {
				if (instance == null) {
					instance = new ContainerService();
				}
			}
		}
		return instance;
	}

	String tpl = FileUtil.readFileByLines("tpl/jupyterlab-inst.yaml");

	public String kafka_db_name = "dvclab_kafka";
	public String kafka_server_addr = KafkaClient.getInstance(kafka_db_name).server_addr;
	public String container_event_topic_name = "Container_Events";

	public static class AssignInfo {

		public String frp_type;
		public String frp_local_ip;
		public Integer frp_local_port;
		public String frp_server_addr;
		public Integer frp_server_port;
		public Integer frp_remote_port;

		public AssignInfo() {}

		public AssignInfo withFrp_type(String frp_type) {
			this.frp_type = frp_type;
			return this;
		}

		public AssignInfo withFrp_local_ip(String frp_local_ip) {
			this.frp_local_ip = frp_local_ip;
			return this;
		}

		public AssignInfo withFrp_local_port(Integer frp_local_port) {
			this.frp_local_port = frp_local_port;
			return this;
		}

		public AssignInfo withFrp_server_addr(String frp_server_addr) {
			this.frp_server_addr = frp_server_addr;
			return this;
		}

		public AssignInfo withFrp_server_port(Integer frp_server_port) {
			this.frp_server_port = frp_server_port;
			return this;
		}

		public AssignInfo withFrp_remote_port(Integer frp_remote_port) {
			this.frp_remote_port = frp_remote_port;
			return this;
		}
	}

	/**
	 *
	 */
	public ContainerService() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

		// 添加消息监听，向前端发送状态更新消息
		KafkaClient.getInstance(kafka_db_name).setSerializers(
				MsgStringSerializer.class,
				MsgStringSerializer.class,
				ServiceMsg.class.getName()
		).addConsumer(container_event_topic_name, 2, KafkaClient.OffsetPolicy.latest, getContainerMsgCallback()); // OffsetPolicy.latest不接受旧消息
	}

	/**
	 *
	 * @param container
	 */
	public void removeContainer(Container container) throws DBInitException, SQLException {

		// 取消 proxy pass 映射
		ReverseProxyService.getInstance().removeProxyPass(container);

		// 返还占用端口
		ReverseProxyService.getInstance().removeTunnelPort(container);

		// 同步缓存
		ContainerCache.containers.remove(container.id);

		// 更新状态
		container.status = Container.Status.Deleted;
		container.update();
	}

	/**
	 *
	 * @param uid
	 * @param image_id
	 * @param project_id
	 * @param project_branch
	 * @param dataset_urls
	 * @param cpus
	 * @param mem
	 * @param gpu
	 * @return
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public Container createDockerComposeConfig(
			String uid, String image_id, String project_id, String project_branch, String[] dataset_urls, float cpus, float mem, boolean gpu)
			throws DBInitException, SQLException
	{

		Image image = Image.getById(Image.class, image_id);
		Project project = Project.getById(Project.class, project_id);

		// 创建容器对象
		Container container = new Container();
		container.uid = uid;
		container.cpus = cpus;
		container.mem = mem;
		// TODO 容器ID的生成方法需要更新
		container.id = StringUtil.md5(uid + "::" + System.currentTimeMillis());
		container.container_name = container.id;
		String service_path = "/users/" + container.uid + "/containers/" + container.id;
		container.user_host = true;

		// 创建配置文件
		container.docker_compose_config = tpl.replaceAll("\\$\\{image_name\\}", image.name)
				.replaceAll("\\$\\{container_id\\}", container.id)
				.replaceAll("\\$\\{project_git_url\\}", project.url)
				.replaceAll("\\$\\{project_branch\\}", project_branch)
				.replaceAll("\\$\\{project_name\\}", project.name)
				.replaceAll("\\$\\{uid\\}", uid)
				.replaceAll("\\$\\{keycloak_server_addr\\}", KeycloakAdapter.getInstance().host)
				.replaceAll("\\$\\{keycloak_realm\\}", KeycloakAdapter.getInstance().realm)
				.replaceAll("\\$\\{client_id\\}", KeycloakAdapter.getInstance().client_id)
				.replaceAll("\\$\\{client_secret\\}", KeycloakAdapter.getInstance().client_secret)
				.replaceAll("\\$\\{service_addr\\}", service_path)
				.replaceAll("\\$\\{kafka_server\\}", kafka_server_addr)
				.replaceAll("\\$\\{kafka_topic\\}", container_event_topic_name)
				.replaceAll("\\$\\{jupyter_port\\}", "8988")
				.replaceAll("\\$\\{cpus\\}", String.valueOf(container.cpus))
				.replaceAll("\\$\\{mem\\}", String.valueOf(Math.round(container.mem)));

		if(dataset_urls != null && dataset_urls.length > 0) {
			container.docker_compose_config = container.docker_compose_config.replaceAll("\\$\\{datasets\\}",
					"- datasets=" + Arrays.stream(dataset_urls).collect(Collectors.joining(",")));
		}

		if(gpu) {
			container.docker_compose_config = container.docker_compose_config
					.replaceAll("\\$\\{runtime\\}", "runtime: nvidia");
		} else {
			container.docker_compose_config = container.docker_compose_config
					.replaceAll("\\$\\{runtime\\}", "");
		}

		container.insert();
		ContainerCache.containers.put(container.id, container);

		return container;
	}

	/**
	 *
	 * @return
	 */
	public KafkaClient.ReceiveCallback<ServiceMsg> getContainerMsgCallback() {
		return (id, msg, addr) -> {

			Map<String, ?> data = msg.data;
			String container_id = (String) data.get("container_id");

			try {

				Container container = Container.getById(Container.class, container_id);
				// 过滤Deleted状态容器的消息
				if(container.status == Container.Status.Deleted) return;

				if(data.get("event").equals("Keep_Alive")) {
					container.status = Container.Status.Running;
				} else container.status = Container.Status.valueOf((String) data.get("event"));

				switch (container.status) {
					// 用户创建容器的第一个消息，创建host记录
					case Init: {
						assignPort(container);
						break;
					}
					case Repo_Clone_Success: {
						break;
					}
					case Pip_Install_Success: {
						break;
					}
					case Dataset_Load_Success: {
						break;
					}
					case Jupyterlab_Start_Success: {
						break;
					}
					case Port_Forwarding_Success: {
						break;
					}
					default:
						break;
				}

				container.update();
				// 缓存更新
				ContainerCache.containers.put(container.id, container);
				ContainerInfoPublisher.broadcast(container_id, container);

			}
			catch (DBInitException | SQLException e) {
				DockerHubService.logger.error("Container[{}] not found, ", container_id, e);
			}
		};
	}

	/**
	 * 向容器分配端口,并做映射
	 */
	private void assignPort(Container container) throws DBInitException, SQLException {

		// 收到容器成功启动的信息，向容器分配端口
		// 分配容器跳板机内网端口
		Pair<Tunnel, Integer> tunnel_port = ReverseProxyService.getInstance().selectTunnelPort();
		Tunnel tunnel = tunnel_port.getLeft();

		container.tunnel_id = tunnel_port.getLeft().id;
		container.tunnel_port = tunnel_port.getRight();

		container.jupyter_url = "https://" + tunnel.wan_addr + "/users/" + container.uid + "/containers/" + container.id;
		ReverseProxyService.getInstance().setupProxyPass(container);
	}
}