package com.dvclab.dockerhub.websocket;

import com.dvclab.dockerhub.cache.HostCache;
import com.dvclab.dockerhub.model.Host;
import one.rewind.txt.URLUtil;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static com.dvclab.dockerhub.DockerHubService.logger;

@WebSocket
public class HostInfoPublisher {

	// private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

	public static Map<Integer, Queue<Session>> host_sessions = new HashMap<>();
	private static Map<Session, Integer> session_host_map = new HashMap<>();

	@OnWebSocketConnect
	public void connected(Session session) {

		String host_id = URLUtil.getParam(session.getUpgradeRequest().getQueryString(), "host_id");

		if(host_id != null && HostCache.hosts.keySet().contains(Integer.parseInt(host_id))) {

			logger.info(host_id);

			int hostId = Integer.parseInt(host_id);
			host_sessions.computeIfAbsent(hostId, v -> new ConcurrentLinkedQueue<>()).add(session);
			session_host_map.put(session, hostId);
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
					session.getRemote().sendString(host.toJSON());
				}
				// IO异常，移除Session
				catch (IOException e) {
					session.close();
				}
			});
		});
	}
}
