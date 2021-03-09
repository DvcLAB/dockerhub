package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.Container;
import com.dvclab.dockerhub.model.Host;
import com.dvclab.dockerhub.serialization.ServiceMsg;
import com.dvclab.dockerhub.websocket.ContainerInfoPublisher;
import one.rewind.db.RedissonAdapter;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.exception.ModelException;
import one.rewind.db.kafka.KafkaClient;
import one.rewind.txt.StringUtil;
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
	public static String kafka_server_addr;
	public static String container_event_topic_name = "Container_Events";

	public static Map<String, Container> containers = new HashMap<>();

	private static List<Integer> tunnel_ports = new ArrayList<>();

	static {
		kafka_server_addr = KafkaClient.getInstance(kafka_db_name).server_addr;
	}

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
		KafkaClient.getInstance(kafka_db_name).addConsumer(container_event_topic_name, 2, (id, msg, addr) -> {

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

			}
			catch (DBInitException | SQLException | ModelException.ClassNotEqual | IllegalAccessException e) {
				logger.error("Container[{}] not found, ", container_id, e);
			}
		});
	}

	/**
	 * 外部创建容器
	 * @param uid
	 */
	public static Container createContainer(String uid, float cpus, float mem, int jupyter_port, String tunnel_host)
			throws DBInitException, SQLException {

		Random r = new Random();
		int tunnel_port = tunnel_ports.remove(r.nextInt(tunnel_ports.size()));

		Container container = new Container();
		container.uid = uid;
		container.cpus = cpus;
		container.mem = mem;
		container.jupyter_port = jupyter_port;
		container.tunnel_host = tunnel_host;
		container.tunnel_port = tunnel_port;

		container.id = StringUtil.md5(uid + "::" + tunnel_host + "::" + tunnel_port + "::" + System.currentTimeMillis());
		container.jupyter_url = "https://" + container.tunnel_host + "/users/" + container.uid + "/containers/" + container.id;

		container.insert();

		return container;
	}

	/**
	 *
	 * @param container
	 */
	public static void setupProxyPass(Container container) {

		// TODO container.tunnel_host 应映射为 内网IP，此处做了简化操作
		RedissonAdapter.get(redis_db_name).getMap("DynamicRoute")
				.put(container.uid + "_" + container.id, "127.0.0.1:"+container.tunnel_port);
	}

	/**
	 *
	 * @param container
	 */
	public static void removeProxyPass(Container container) {

		// TODO container.tunnel_host 应映射为 内网IP，此处做了简化操作
		RedissonAdapter.get(redis_db_name).getMap("DynamicRoute")
				.remove(container.uid + "_" + container.id);
	}
}
