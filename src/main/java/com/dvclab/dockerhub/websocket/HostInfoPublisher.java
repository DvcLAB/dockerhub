package com.dvclab.dockerhub.websocket;

import com.dvclab.dockerhub.cache.HostCache;
import com.dvclab.dockerhub.model.Host;
import one.rewind.nio.web.auth.KeycloakAdapter;
import one.rewind.txt.URLUtil;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;

import static com.dvclab.dockerhub.DockerHubService.logger;

/**
 * 容主机信息的WebSocket发布器
 */
@WebSocket
public class HostInfoPublisher {

	// private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

	public static Map<String, Queue<Session>> host_sessions = new HashMap<>();
	private static Map<Session, String> session_host_map = new HashMap<>();
	private final String WS_AUTH_HEADER = "Sec-WebSocket-Protocol";

	@OnWebSocketConnect
	public void connected(Session session) {

		String token = session.getUpgradeRequest().getHeader(WS_AUTH_HEADER);
		String host_id = URLUtil.getParam(session.getUpgradeRequest().getQueryString(), "id");

		// ws连接认证
		try {
			KeycloakAdapter.getInstance().verifyAccessToken(token);
		} catch (IOException e) {
			session.close(1011, "Not Valid");
		}

		if(host_id != null && HostCache.hosts.containsKey(host_id)) {

			host_sessions.computeIfAbsent(host_id, v -> new ConcurrentLinkedQueue<>()).add(session);
			session_host_map.put(session, host_id);
		}
		else {
			session.close(1011, "Not valid");
		}
	}

	@OnWebSocketClose
	public void closed(Session session, int statusCode, String reason) {
		Optional.ofNullable(host_sessions.get(session_host_map.remove(session))).ifPresent(q -> q.remove(session));
	}

	@OnWebSocketMessage
	public void message(Session session, String message) throws IOException {
		logger.info("Got: " + message);
		/*session.getRemote().sendString(message);*/
	}

	/**
	 * 向所有已连接客户端更新Host状态
	 * @param hostId
	 * @param host
	 */
	public static void broadcast(String hostId, Host host) {

		Optional.ofNullable(host_sessions.get(hostId)).ifPresent(sessions -> {
			sessions.forEach(session -> {
				try {
					session.getRemote().sendString(host.toJson());
				}
				// IO异常，移除Session
				catch (IOException e) {
					session.close();
				}
			});
		});
	}
}
