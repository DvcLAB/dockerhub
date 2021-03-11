package com.dvclab.dockerhub.service;

import com.dvclab.dockerhub.model.Container;
import one.rewind.db.RedissonAdapter;

import java.util.Map;

/**
 *
 */
public class ReverseProxyService {

	public String redis_db_name = "dvclab_redis";

	public ReverseProxyService() {

	}

	/**
	 *
	 * @param container
	 */
	public void setupProxyPass(Container container) {

		RedissonAdapter.get(redis_db_name).getMap("DynamicRoute")
				.put(container.uid + "_" + container.id, container.tunnel_lan_addr + ":" + container.tunnel_port);
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
	 * @return
	 */
	public Map<String, String> getCurrentRoutes() {
		return RedissonAdapter.get(redis_db_name).getMap("DynamicRoute");
	}
}
