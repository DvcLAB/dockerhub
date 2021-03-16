package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.Host;
import one.rewind.db.exception.DBInitException;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class HostCache {

	public static Map<String, Host> hosts = new HashMap<>();

	public static void init() throws DBInitException, SQLException {
		Host.getAll(Host.class).forEach(HostCache::addHost);
	}

	/**
	 *
	 * @param host
	 */
	public static void addHost(Host host) {
		host.connectSshHost();
//		host.private_key = null;
		hosts.put(host.id, host);
	}

	/**
	 * 获取主机
	 * @return
	 */
	public static Host getHost(boolean gpu_enabled) {

		return hosts.values().stream()
			.filter(host -> {
				if(gpu_enabled) return host.gpu_enabled;
				return true;
			})
			.min(Comparator.comparing(host -> host.container_num.get()))
			.orElseThrow(NoSuchElementException::new);
	}
}
