package com.dvclab.dockerhub.service;

import com.dvclab.dockerhub.model.Container;
import com.dvclab.dockerhub.model.Tunnel;
import one.rewind.db.RedissonAdapter;
import one.rewind.db.exception.DBInitException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.*;

/**
 *
 */
public class ReverseProxyService {

	public static final Logger logger = LogManager.getLogger(ReverseProxyService.class);

	public static ReverseProxyService instance;

	/**
	 * 单例模式
	 * @return
	 */
	public static ReverseProxyService getInstance() throws DBInitException, SQLException {

		if (instance == null) {
			synchronized (ReverseProxyService.class) {
				if (instance == null) {
					instance = new ReverseProxyService();
				}
			}
		}
		return instance;
	}

	// redis 数据库名称
	public String redis_db_name = "dvclab_redis";

	//
	public Map<String, List<Integer>> available_ports = new HashMap<>();
	public Map<String, Tunnel> tunnels = new HashMap<>();

	/**
	 *
	 */
	public ReverseProxyService() throws DBInitException, SQLException {

		// 初始化Tunnel
		List<Tunnel> tunnels_ = Tunnel.getAll(Tunnel.class);

		tunnels_.forEach(t -> {

			List<Integer> list = new ArrayList<>();
			for(int i = t.begin_port; i < t.end_port; i++) {
				list.add(i);
			}

			available_ports.put(t.id, list);
			tunnels.put(t.id, t);
		});
	}

	/**
	 *
	 * @return
	 */
	public Pair<Tunnel, Integer> selectTunnelPort() {

		Map.Entry<String, List<Integer>> entry = available_ports.entrySet().stream()
				.max(Comparator.comparing(en -> en.getValue().size()))
				.orElseThrow(NoSuchElementException::new);

		Random r = new Random();
		int port = entry.getValue().remove(r.nextInt(entry.getValue().size()));

		return new ImmutablePair<>(tunnels.get(entry.getKey()), port);
	}

	/**
	 *
	 * @param container
	 */
	public void setupProxyPass(Container container) {

		Tunnel t = tunnels.get(container.tunnel_id);

		RedissonAdapter.get(redis_db_name).getMap("DynamicRoute")
				.put(container.uid + "_" + container.id, t.lan_addr + ":" + container.tunnel_port);
	}

	/**
	 *
	 * @param container
	 */
	public void removeProxyPass(Container container) {

		RedissonAdapter.get(redis_db_name).getMap("DynamicRoute")
				.remove(container.uid + "_" + container.id);
	}

	/**
	 *
	 * @param container
	 */
	public void removeTunnelPort(Container container) {
		available_ports.get(container.tunnel_id).add(container.tunnel_port);
	}

	/**
	 *
	 * @return
	 */
	public Map<String, String> getCurrentRoutes() {
		return RedissonAdapter.get(redis_db_name).getMap("DynamicRoute");
	}
}
