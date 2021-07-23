package com.dvclab.dockerhub.websocket;

import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.model.Container;
import com.dvclab.dockerhub.route.Routes;
import one.rewind.nio.web.auth.KeycloakAdapter;
import one.rewind.txt.URLUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 容器信息的WebSocket发布器
 */
@WebSocket
public class ContainerInfoPublisher {

	// private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

	public static Map<String, Queue<Session>> container_sessions = new HashMap<>();
	private static Map<Session, String> session_container_map = new HashMap<>(); // TODO 一个Session多个Container是否能处理？
	private final String WS_AUTH_HEADER = "Sec-WebSocket-Protocol";

	@OnWebSocketConnect
	public void connected(Session session) {

		String token = session.getUpgradeRequest().getHeader(WS_AUTH_HEADER);
		String container_id = URLUtil.getParam(session.getUpgradeRequest().getQueryString(), "id");

		// ws连接认证
		try {
			KeycloakAdapter.getInstance().verifyAccessToken(token);
		} catch (IOException e) {
			session.close(1011, "Not Valid");
		}

		if(container_id != null && ContainerCache.containers.containsKey(container_id)) {
			container_sessions.computeIfAbsent(container_id, v -> new ConcurrentLinkedQueue<>()).add(session);
			session_container_map.put(session, container_id);
		}
		else {
			session.close(1011, "Not Valid");
		}
	}

	@OnWebSocketClose
	public void closed(Session session, int statusCode, String reason) {
		Optional.ofNullable(container_sessions.get(session_container_map.remove(session))).ifPresent(q -> q.remove(session));
	}

	@OnWebSocketMessage
	public void message(Session session, String message) throws IOException {
		Routes.logger.info("Got: " + message);
		/*session.getRemote().sendString(message);*/
	}

	/**
	 * 向所有已连接客户端更新状态
	 * @param container_id
	 * @param container
	 */
	public static void broadcast(String container_id, Container container) {

		Optional.ofNullable(container_sessions.get(container_id)).ifPresent(sessions -> {
			sessions.forEach(session -> {
				try {
					session.getRemote().sendString(container.toJson());
				}
				// IO异常，移除Session
				catch (IOException e) {
					session.close();
				}
			});
		});
	}
}
