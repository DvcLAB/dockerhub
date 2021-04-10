package com.dvclab.dockerhub.service;

import com.dvclab.dockerhub.DockerHubService;
import com.dvclab.dockerhub.auth.KeycloakAdapter;
import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.cache.HostCache;
import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.*;
import com.dvclab.dockerhub.serialization.ServiceMsg;
import com.dvclab.dockerhub.websocket.ContainerInfoPublisher;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jcraft.jsch.JSchException;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.kafka.KafkaClient;
import one.rewind.db.kafka.msg.MsgStringSerializer;
import one.rewind.txt.StringUtil;
import one.rewind.util.FileUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 */
public class ContainerService {

	public static final Logger logger = LogManager.getLogger(ContainerService.class);

	public static ContainerService instance;
	private ScheduledExecutorService ses;
	private final Integer KEEP_ALIVE_CHECK_INTERVAL = 90;
	private final Integer INITIAL_KEEP_ALIVE_CHECK_INTERVAL = 30;

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

	public static class CreateResourceBody {

		public String name;
		public String type;
		public String owner;
		public boolean ownerManagedAccess;
		public List<String> resource_scopes = new ArrayList<>();
		public String _id;

		public CreateResourceBody() {}

		public CreateResourceBody withName(String name) {
			this.name = name;
			return this;
		}

		public CreateResourceBody withType(String type) {
			this.type = type;
			return this;
		}

		public CreateResourceBody withOwner(String owner) {
			this.owner = owner;
			return this;
		}

		public CreateResourceBody withOwnerManagedAccess(boolean ownerManagedAccess) {
			this.ownerManagedAccess = ownerManagedAccess;
			return this;
		}

		public CreateResourceBody withResource_scopes(List<String> resource_scopes) {
			this.resource_scopes = resource_scopes;
			return this;
		}

		public CreateResourceBody withId(String _id) {
			this._id = _id;
			return this;
		}
	}

	public static class ApplyResourcePolicyBody {

		public String name;
		public String description;
		public List<String> users;
		public List<String> scopes = new ArrayList<>();
		public String logic = "POSITIVE";
		public String decisionStrategy = "UNANIMOUS";

		public ApplyResourcePolicyBody() {}

		public ApplyResourcePolicyBody withName(String name) {
			this.name = name;
			return this;
		}

		public ApplyResourcePolicyBody withDesc(String description) {
			this.description = description;
			return this;
		}

		public ApplyResourcePolicyBody withUsers(List<String> users) {
			this.users = users;
			return this;
		}

		public ApplyResourcePolicyBody withScopes(List<String> scopes) {
			this.scopes = scopes;
			return this;
		}

		public ApplyResourcePolicyBody withLogic(String logic) {
			this.logic = logic;
			return this;
		}

		public ApplyResourcePolicyBody withDecisionStrategy(String decisionStrategy) {
			this.decisionStrategy = decisionStrategy;
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

		ses = Executors.newScheduledThreadPool(2,
				new ThreadFactoryBuilder().setNameFormat("KeepAliveCheck-%d").build());

		ses.scheduleWithFixedDelay(() -> {

			try {
				ContainerService.logger.info("----清扫----");

				Container.getAll(Container.class).stream()
						.filter(container -> container.status == Container.Status.Running
								&& System.currentTimeMillis() - container.last_keep_alive.getTime() > KEEP_ALIVE_CHECK_INTERVAL * 1000)
						.forEach(container -> {
							try {
								// 认定为容器已经失效，执行容器删除操作
								if(!container.user_host) {
									HostCache.hosts.get(container.host_id).removeContainer(container);
								}

								ContainerService.getInstance().removeContainer(container);
							} catch (DBInitException | SQLException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | IOException | URISyntaxException e) {
								ContainerService.logger.error("Keep Alive Check Error", e);
							}
						});
			} catch (DBInitException | SQLException e) {
				ContainerService.logger.error("Keep Alive Check Error", e);
			}

		}, INITIAL_KEEP_ALIVE_CHECK_INTERVAL, KEEP_ALIVE_CHECK_INTERVAL, TimeUnit.SECONDS);
	}

	/**
	 *
	 * @param container
	 */
	public void removeContainer(Container container) throws DBInitException, SQLException, IOException, URISyntaxException {

		// 取消 proxy pass 映射
		ReverseProxyService.getInstance().removeProxyPass(container);

		// 返还占用端口
		ReverseProxyService.getInstance().removeTunnelPort(container);

		// 同步缓存
		ContainerCache.containers.remove(container.id);

		// 删除Keycloak资源
		if(container.status != Container.Status.Deleted && container.status != Container.Status.New) {
			// 更新tokens文件
			removeToken(container);
			KeycloakAdapter.getInstance().deleteResource(new StringBuilder(container.id).insert(8, "-")
					.insert(13, "-")
					.insert(18, "-")
					.insert(23, "-").toString());
		}

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
		container.image_id = image_id;

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
				.replaceAll("\\$\\{mem\\}", String.valueOf(Math.round(container.mem)))
				.replaceAll("\\$\\{frp_service_url\\}", DockerHubService.getInstance().service_url);

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
				if(container == null || container.status == Container.Status.Deleted) return;

				if(data.get("event").equals("Keep_Alive")) {
					if(container.begin_run_time == null) container.begin_run_time = new Date();
					container.status = Container.Status.Running;
					container.last_keep_alive = new Date();
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
						createContainerResource(container_id, container.uid);
						applyContainerPolicy(container_id, container.uid);
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
			catch (DBInitException | SQLException | IOException | JSchException | URISyntaxException e) {
				DockerHubService.logger.error("Container[{}] not found, ", container_id, e);
			}
		};
	}

	/**
	 * 在keycloak中创建Container对应的资源
	 */
	private void createContainerResource (String container_id, String container_uid) throws IOException, URISyntaxException {

		// UUID
		String resource_id = new StringBuilder(container_id).insert(8, "-")
				.insert(13, "-")
				.insert(18, "-")
				.insert(23, "-").toString();
		CreateResourceBody cr_body = new CreateResourceBody()
				// 容器ID转UUID
				.withId(resource_id)
				.withName("jupyterlab_" + container_id)
				.withOwner(UserCache.USERS.get(container_uid).username)
				.withType("jupyterlab")
				.withResource_scopes(List.of("view"))
				.withOwnerManagedAccess(true);
		// 获取资源ID
		KeycloakAdapter.getInstance().createResource(cr_body);
	}

	/**
	 * 为keycalok中的Container资源分配Policy
	 */
	private void applyContainerPolicy (String container_id, String container_uid) throws IOException, URISyntaxException {

		// UUID
		String resource_id = new StringBuilder(container_id).insert(8, "-")
				.insert(13, "-")
				.insert(18, "-")
				.insert(23, "-").toString();
		// 为资源添加policy
		String token = UserCache.UID_TOKEN.get(container_uid);
		// 换取后端token
		token = KeycloakAdapter.getInstance().exchangeToken(token);
		ApplyResourcePolicyBody arp_body = new ApplyResourcePolicyBody()
				.withDesc(container_uid + "_access_" + container_id)
				.withName(container_uid + "_access_" + container_id)
				.withScopes(List.of("view"))
				.withUsers(List.of(UserCache.USERS.get(container_uid).username));
		KeycloakAdapter.getInstance().applyResourcePolicy(token, resource_id, arp_body);
	}

	/**
	 * 向容器分配端口,并做映射
	 */
	private void assignPort(Container container) throws DBInitException, SQLException, IOException, JSchException {

		// 检查是否已经分配端口
		if(container.tunnel_id != null) return;

		// 收到容器成功启动的信息，向容器分配端口
		// 分配容器跳板机内网端口
		Pair<Tunnel, Integer> tunnel_port = ReverseProxyService.getInstance().selectTunnelPort();
		Tunnel tunnel = tunnel_port.getLeft();

		container.tunnel_id = tunnel_port.getLeft().id;
		container.tunnel_port = tunnel_port.getRight();

		container.jupyter_url = "https://" + tunnel.wan_addr + "/users/" + container.uid + "/containers/" + container.id;
		ReverseProxyService.getInstance().setupProxyPass(container);
		ContainerService.logger.info("container [{}] assign port", container.id);
		// 分配一个meta_token写入tunnel的tokens文件
		addToken(container);
	}

	/**
	 * 删除tokens文件记录
	 * TODO 删除效率可能需要改进
	 */
	private void removeToken(Container container) throws IOException {

		String local_token_path = "/opt/frps/tokens";

		// 更改tokens文件
		String raw = FileUtil.readFileByLines(local_token_path);
		String s = raw.replace(container.id + "=" + container.id, "")
				.replaceAll("(\r?\n){2}", "\n");

		FileUtil.writeBytesToFile(s.getBytes(), local_token_path);

		updateToken();
	}
	/**
	 * 向tokens文件添加记录
	 */
	private void addToken(Container container) throws IOException {

		String local_token_path = "/opt/frps/tokens";
		FileUtil.appendLineToFile( "\n" + container.id + "=" + container.id, local_token_path);

		updateToken();
	}

	/**
	 * 更新fp-multiuser服务
	 */
	private synchronized void updateToken() throws IOException {

		String update_token_shell_path = "tpl/updateToken.sh";
		String cmd = "#! /bin/bash\n" +
				"pkill fp-multiuser && nohup /opt/frps/fp-multiuser -l 127.0.0.1:7200 -f /opt/frps/tokens > nohup.out 2> nohup.err < /dev/null &\n";

		// 清除旧脚本
		File file = new File(update_token_shell_path);
		file.delete();

		// 设置脚本可执行权限
		Set<PosixFilePermission> ownerWritable = PosixFilePermissions.fromString("rwxrwxrwx");
		FileAttribute<?> permissions = PosixFilePermissions.asFileAttribute(ownerWritable);
		Files.createFile(Path.of(update_token_shell_path), permissions);
		Files.write(Path.of(update_token_shell_path), cmd.getBytes());

		// 执行脚本
		ProcessBuilder processBuilder = new ProcessBuilder();
		processBuilder.command(List.of(update_token_shell_path));
		processBuilder.start();

		ContainerService.logger.info("------update token------");
	}
}