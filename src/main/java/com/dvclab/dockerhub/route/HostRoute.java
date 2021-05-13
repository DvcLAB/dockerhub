package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.cache.HostCache;
import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.Host;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import one.rewind.db.Daos;
import spark.Route;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HostRoute {

	/**
	 * 获取主机列表
	 */
	public static Route listHosts = (q, a) -> {

		String uid = q.session().attribute("uid");
		String query = q.queryParamOrDefault("q", "");
		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));
		Long size = Long.parseLong(q.queryParamOrDefault("size", "10"));
		// 只有管理员可以获得主机列表
		if(!UserCache.USERS.get(uid).roles.contains(User.Role.DOCKHUB_ADMIN)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

		try {

			Dao<Host, ?> dao = Daos.get(Host.class);
			QueryBuilder<Host, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("update_time", false);
			long total = dao.queryBuilder().countOf();

			qb.where().like("id", query + "%")
					.or().like("ip", query + "%");

			List<Host> list = qb.query();

			// 返回结果补全 用户信息
			Map<String, User> users = User.getUsers(list.stream().map(c -> c.uid).collect(Collectors.toList()));
			list.forEach(c -> {
				c.user = users.get(c.uid);
				c.private_key = null;
			});

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("List Host error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 创建主机
	 */
	public static Route createHost = (q, a) -> {

		String uid = q.session().attribute("uid");
		String source = q.body();

		try {
			Host obj = Host.fromJSON(source, Host.class);
			obj.genId();
			obj.uid = uid;

			if(obj.insert()) {
				HostCache.addHost(obj);
				return Msg.success();
			}
			else {
				return Msg.failure();
			}
		}
		catch (Exception e) {

			Routes.logger.error("Create Host[{}] error, ", source, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取主机
	 */
	public static Route getHost = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Host obj = Host.getById(Host.class, id);
			if(obj != null) {

				// 补全用户信息
				obj.user = User.getById(User.class, obj.uid);
				obj.private_key = null;
				return Msg.success(obj);
			}
			else {
				return new Msg(Msg.Code.NOT_FOUND, null, null);
			}
		}
		catch (Exception e) {

			Routes.logger.error("Get Host[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 更新主机
	 * Note: ip port username 不可更改
	 */
	public static Route updateHost = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String source = q.body();

		try {

			Host obj = Host.fromJSON(source, Host.class);
			obj.genId();

			if(!obj.id.equals(id)) throw new Exception("Host ip/port/username can not be changed");

			if(obj.update()) {
				HostCache.hosts.put(obj.id, obj);
				return Msg.success(obj);
			}
			else {
				return Msg.failure();
			}
		}
		catch (Exception e) {

			Routes.logger.error("Update Host[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 删除主机
	 */
	public static Route deleteHost = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {
			// 用户只能删除自己的Host
			if(! HostCache.hosts.get(id).uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

			Host.deleteById(Host.class, id);
			// 断开中台与指定主机的ssh连接
			HostCache.hosts.remove(id).disconnectSshHost();

			return Msg.success();
		}
		catch (Exception e) {

			Routes.logger.error("Delete Host[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

}
