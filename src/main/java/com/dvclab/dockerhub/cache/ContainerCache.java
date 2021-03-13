package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.DockerHubService;
import com.dvclab.dockerhub.model.Container;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.kafka.KafkaClient;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerCache {

	public static Map<String, Container> containers = new HashMap<>();
	public static List<Integer> tunnel_ports = new ArrayList<>();

	public static void init() throws DBInitException, SQLException {

		// A 初始化容器端口池
		for(int i = 53001; i < 56000; i++) {
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
	}
}
