package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.auth.KeycloakAdapter;
import com.dvclab.dockerhub.model.*;
import com.dvclab.dockerhub.serialization.ServiceMsg;
import com.dvclab.dockerhub.websocket.ContainerInfoPublisher;
import one.rewind.db.RedissonAdapter;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.exception.ModelException;
import one.rewind.db.kafka.KafkaClient;
import one.rewind.txt.StringUtil;
import one.rewind.util.FileUtil;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

/**
 *
 */
public class ContainerCache {

	public static final Logger logger = LogManager.getLogger(ContainerCache.class);

	public static String redis_db_name = "dvclab_kafka";

	public static String kafka_db_name = "dvclab_kafka";
	public static String kafka_server_addr = KafkaClient.getInstance(kafka_db_name).server_addr;
	public static String container_event_topic_name = "Container_Events";
	public static KafkaClient.ReceiveCallback<ServiceMsg> containerMsgCallback = (id, msg, addr) -> {

		Map<String, ?> data = ((ServiceMsg) msg).data;
		String container_id = (String) data.get("container_id");

		try {
			Container container = Container.getById(Container.class, container_id);
			container.status = Container.Status.valueOf((String) data.get("event"));

			switch (container.status) {
				// 用户创建容器的第一个消息，创建host记录
				case Init: {

					String dataMsg = (String) data.get("msg");
					Host host = new Host(container.uid, dataMsg);
					host.upsert();

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
			ContainerInfoPublisher.broadcast(container_id, container);

		} catch (DBInitException | SQLException | ModelException.ClassNotEqual | IllegalAccessException e) {
			logger.error("Container[{}] not found, ", container_id, e);
		}
	};;

	public static String frp_server_addr = "j.dvclab.com";
	public static int frp_server_port = 53000;
	public static String frp_server_lan_addr = "127.0.0.1";

	static String tpl = FileUtil.readFileByLines("tpl/jupyterlab-inst.yaml");
	static int jupyter_port = 8898;

	public static Map<String, Container> containers = new HashMap<>();

	private static List<Integer> tunnel_ports = new ArrayList<>();

	/**
	 * 容器初始化
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static void init() throws DBInitException, SQLException {

		// 初始化容器端口池
		for(int i=53001; i<54000; i++) {
			tunnel_ports.add(i);
		}

		// 初始化容器
		Container.getAll(Container.class).forEach(c -> {
			if(c.user_host) {
				tunnel_ports.remove((Integer) c.tunnel_port);
				setupProxyPass(c);
			}

			containers.put(c.id, c);
		});

		// 添加消息监听，向前端发送状态更新消息
		KafkaClient.getInstance(kafka_db_name).addConsumer(container_event_topic_name, 2, containerMsgCallback);
	}

	/**
	 * 外部创建容器
	 * @param uid
	 */
	public static Container createContainer(String uid, float cpus, float mem, int jupyter_port, String tunnel_wan_addr, String tunnel_lan_addr)
			throws DBInitException, SQLException {

		Random r = new Random();
		int tunnel_port = tunnel_ports.remove(r.nextInt(tunnel_ports.size()));

		Container container = new Container();
		container.uid = uid;
		container.cpus = cpus;
		container.mem = mem;
		container.jupyter_port = jupyter_port;
		container.tunnel_wan_addr = tunnel_wan_addr;
		container.tunnel_lan_addr = tunnel_lan_addr;
		container.tunnel_port = tunnel_port;

		container.id = StringUtil.md5(uid + "::" + tunnel_wan_addr + "::" + tunnel_port + "::" + System.currentTimeMillis());
		container.jupyter_url = "https://" + container.tunnel_wan_addr + "/users/" + container.uid + "/containers/" + container.id;

		container.insert();

		return container;
	}

	/**
	 *
	 * @param container
	 */
	public static void setupProxyPass(Container container) {

		RedissonAdapter.get(redis_db_name).getMap("DynamicRoute")
				.put(container.uid + "_" + container.id, container.tunnel_lan_addr+":"+container.tunnel_port);
	}

	/**
	 *
	 * @param container
	 */
	public static void removeProxyPass(Container container) {

		RedissonAdapter.get(redis_db_name).getMap("DynamicRoute")
				.remove(container.uid + "_" + container.id);
	}

	/**
	 *
	 * @param uid
	 * @param image_id
	 * @param project_id
	 * @param project_branch
	 * @param dataset_id
	 * @param cpus
	 * @param mem
	 * @param gpu
	 * @return
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static String createDockerComposeConfig(
			String uid, String image_id, String project_id, String project_branch, String dataset_id, float cpus, float mem, boolean gpu)
			throws DBInitException, SQLException
	{

		Image image = Image.getById(Image.class, image_id);
		Project project = Project.getById(Project.class, project_id);
		DataSet dataset = DataSet.getById(DataSet.class, dataset_id);

		Container container = ContainerCache.createContainer(uid, cpus, mem, jupyter_port,
				ContainerCache.frp_server_addr,
				ContainerCache.frp_server_lan_addr);

		String docker_compose_config = tpl.replaceAll("\\$\\{image_name\\}", image.name)
				.replaceAll("\\$\\{container_id\\}", container.id)
				.replaceAll("\\$\\{jupyter_port\\}", String.valueOf(jupyter_port))
				.replaceAll("\\$\\{project_git_url\\}", project.url)
				.replaceAll("\\$\\{project_branch\\}", project_branch)
				.replaceAll("\\$\\{project_name\\}", project.name)
				.replaceAll("\\$\\{uid\\}", uid)
				.replaceAll("\\$\\{keycloak_server_addr\\}", KeycloakAdapter.getInstance().host)
				.replaceAll("\\$\\{keycloak_realm\\}", KeycloakAdapter.getInstance().realm)
				.replaceAll("\\$\\{client_id\\}", KeycloakAdapter.getInstance().frontend_client_id)
				.replaceAll("\\$\\{client_secret\\}", KeycloakAdapter.getInstance().frontend_client_secret)
				.replaceAll("\\$\\{container_login_url\\}", container.jupyter_url + "/login")
				.replaceAll("\\$\\{kafka_server\\}", ContainerCache.kafka_server_addr)
				.replaceAll("\\$\\{kafka_topic\\}", ContainerCache.container_event_topic_name)
				.replaceAll("\\$\\{frp_server_addr\\}", ContainerCache.frp_server_addr)
				.replaceAll("\\$\\{frp_server_port\\}", String.valueOf(ContainerCache.frp_server_port))
				.replaceAll("\\$\\{frp_remote_port\\}", String.valueOf(container.tunnel_port))
				.replaceAll("\\$\\{cpus\\}", String.valueOf(container.cpus))
				.replaceAll("\\$\\{mem\\}", String.valueOf(container.mem));

		if(gpu) {
			docker_compose_config = docker_compose_config
					.replaceAll("\\$\\{runtime\\}", "runtime: nvidia");
		}

		return docker_compose_config;
	}
}
