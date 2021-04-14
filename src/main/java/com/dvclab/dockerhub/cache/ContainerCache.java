package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.Container;
import com.dvclab.dockerhub.service.ReverseProxyService;
import one.rewind.db.exception.DBInitException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 容器缓存
 */
public class ContainerCache {

	public static Map<String, Container> containers = new HashMap<>();

	/**
	 *
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static void init() throws DBInitException, SQLException {

		// 初始化容器
		for(Container c : Container.getAll(Container.class)) {
			// 已删除状态和新创建状态的容器不初始化代理，因为并没有分配
			if(c.status != Container.Status.Deleted && c.status != Container.Status.New
					&& c.status != Container.Status.Deployed) {
				ReverseProxyService.getInstance().available_ports.get(c.tunnel_id).remove(Integer.valueOf(c.tunnel_port));
				ReverseProxyService.getInstance().setupProxyPass(c);
			}

			containers.put(c.id, c);
		}
	}
}
