package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.Host;
import one.rewind.db.exception.DBInitException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class HostCache {

	public static Map<String, Host> hosts = new HashMap<>();

	public static void init() throws DBInitException, SQLException {
		Host.getAll(Host.class).forEach(host -> {
			addHost(host);
		});
	}

	/**
	 *
	 * @param host
	 */
	public static void addHost(Host host) {
		host.private_key = null;
		host.connectSshHost();
		hosts.put(host.id, host);
	}

	/**
	 * TODO
	 * @return
	 */
	public static Host getHost(boolean gpu_enabled) {

		return null;
	}
}
