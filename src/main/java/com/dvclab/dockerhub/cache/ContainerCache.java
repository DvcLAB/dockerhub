package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.Container;
import com.dvclab.dockerhub.service.ReverseProxyService;
import one.rewind.db.exception.DBInitException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ContainerCache {

	public static Map<String, Container> containers = new HashMap<>();

	public static void init() throws DBInitException, SQLException {

		// 初始化容器
		for(Container c : Container.getAll(Container.class)) {
			if(c.user_host && c.status != Container.Status.Deleted && c.status != Container.Status.New) {
				ReverseProxyService.getInstance().available_ports.get(c.tunnel_id).remove(Integer.valueOf(c.tunnel_port));
				ReverseProxyService.getInstance().setupProxyPass(c);
			}

			containers.put(c.id, c);
		}
	}
}
