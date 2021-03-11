package com.dvclab.dockerhub.service;

import com.dvclab.dockerhub.DockerHubService;
import com.dvclab.dockerhub.auth.KeycloakAdapter;
import com.dvclab.dockerhub.model.*;
import com.dvclab.dockerhub.serialization.ServiceMsg;
import com.dvclab.dockerhub.websocket.ContainerInfoPublisher;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.exception.ModelException;
import one.rewind.db.kafka.KafkaClient;
import one.rewind.txt.StringUtil;
import one.rewind.util.FileUtil;

import java.sql.SQLException;
import java.util.*;

public class ContainerFactory {

	public Map<String, Container> containers = new HashMap<>();

	private List<Integer> tunnel_ports = new ArrayList<>();

	public String frp_server_addr = "j.dvclab.com";
	public int frp_server_port = 53000;
	public String frp_server_lan_addr = "127.0.0.1";

	String tpl = FileUtil.readFileByLines("tpl/jupyterlab-inst.yaml");
	int jupyter_port = 8898;

	public String kafka_db_name = "dvclab_kafka";
	public String kafka_server_addr = KafkaClient.getInstance(kafka_db_name).server_addr;
	public String container_event_topic_name = "Container_Events";

	public ContainerFactory() {

	}

	/**
	 * 全部容器容器初始化
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public void init() throws DBInitException, SQLException {

		// A 初始化容器端口池
		for(int i = 53001; i < 54000; i++) {
			tunnel_ports.add(i);
		}

		// B 初始化容器
		Container.getAll(Container.class).forEach(c -> {
			if(c.user_host) {
				tunnel_ports.remove((Integer) c.tunnel_port);
				DockerHubService.getInstance().reverseProxyMgr.setupProxyPass(c);
			}

			containers.put(c.id, c);
		});

		// C 添加消息监听，向前端发送状态更新消息
		KafkaClient.getInstance(kafka_db_name).addConsumer(container_event_topic_name, 2, getContainerMsgCallback());
	}

	/**
	 * 外部创建容器
	 * @param uid
	 */
	public Container createContainer(String uid, float cpus, float mem, int jupyter_port, String tunnel_wan_addr, String tunnel_lan_addr)
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


	public String createDockerComposeConfig(
			String uid, String image_id, String project_id, String project_branch, String dataset_id, float cpus, float mem, boolean gpu)
			throws DBInitException, SQLException
	{

		Image image = Image.getById(Image.class, image_id);
		Project project = Project.getById(Project.class, project_id);
		Dataset dataset = Dataset.getById(Dataset.class, dataset_id);

		Container container = createContainer(uid, cpus, mem, jupyter_port,
				frp_server_addr,
				frp_server_lan_addr);

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
				.replaceAll("\\$\\{kafka_server\\}", kafka_server_addr)
				.replaceAll("\\$\\{kafka_topic\\}", container_event_topic_name)
				.replaceAll("\\$\\{frp_server_addr\\}", frp_server_addr)
				.replaceAll("\\$\\{frp_server_port\\}", String.valueOf(frp_server_port))
				.replaceAll("\\$\\{frp_remote_port\\}", String.valueOf(container.tunnel_port))
				.replaceAll("\\$\\{cpus\\}", String.valueOf(container.cpus))
				.replaceAll("\\$\\{mem\\}", String.valueOf(container.mem));

		if(gpu) {
			docker_compose_config = docker_compose_config
					.replaceAll("\\$\\{runtime\\}", "runtime: nvidia");
		}

		return docker_compose_config;
	}

	/**
	 *
	 * @return
	 */
	public KafkaClient.ReceiveCallback<ServiceMsg> getContainerMsgCallback() {
		return (id, msg, addr) -> {

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
				DockerHubService.logger.error("Container[{}] not found, ", container_id, e);
			}
		};
	}
}
