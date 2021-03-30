package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.DockerHubService;
import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.cache.HostCache;
import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.Container;
import com.dvclab.dockerhub.model.Host;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.dvclab.dockerhub.service.ContainerService;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import one.rewind.db.Daos;
import spark.Route;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class ContainerRoute {

	/**
	 * 构建容器DockerCompose配置文件
	 */
	public static Route createContainer = (q, a) -> {

		String uid = q.session().attribute("uid");

		String image_id = q.queryParams("image_id");
		String project_id = q.queryParams("project_id");
		String project_branch = q.queryParams("project_branch");
		String[] dataset_urls = q.queryParamsValues("dataset_url");

		float cpus = Float.parseFloat(q.queryParamOrDefault("cpus", "2"));
		float mem = Float.parseFloat(q.queryParamOrDefault("mem", "4"));
		boolean gpu = Boolean.parseBoolean(q.queryParamOrDefault("gpu", "false"));

		try {
			return Msg.success(
					ContainerService.getInstance()
							.createDockerComposeConfig(uid, image_id, project_id, project_branch, dataset_urls, cpus, mem, gpu)
			);
		}
		catch (Exception e) {

			Routes.logger.error("Unable create container, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取容器列表
	 */
	public static Route listContainers = (q, a) -> {

		String uid = q.session().attribute("uid");
		String query = q.queryParamOrDefault("q", "");
		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));
		Long size = Long.parseLong(q.queryParamOrDefault("size", "10"));

		try {

			Dao<Container, ?> dao = Daos.get(Container.class);
			QueryBuilder<Container, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("update_time", false);

			long total = dao.queryBuilder().countOf();

			// 管理员查询分支
			if(UserCache.USERS.get(uid).roles.contains(User.Role.DOCKHUB_ADMIN)) {
				qb.where().like("id", query + "%")
						.or().like("uid", query + "%");
			}
			// 一般用户查询分支
			else {
				qb.where().like("id", query + "%")
						.and().eq("uid", uid).and().ne("status", Container.Status.Deleted); // 使用Enum，而不是对应的字符串
			}


			List<Container> list = qb.query();

			// 返回结果补全 用户信息
			Map<String, User> users = User.getUsers(list.stream().map(c -> c.uid).collect(Collectors.toList()));
			list.stream().forEach(c -> c.user = users.get(c.uid));

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("Unable create container, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 *
	 */
	public static Route runContainer = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		boolean gpu_enabled = Boolean.parseBoolean(q.queryParamOrDefault("gpu_enabled", "false"));

		String host_id = q.queryParamOrDefault("host_id", "");

		try {

			Container container = Container.getById(Container.class, id);
			// TODO 判断用户是否有权限使用 Container  待验证
			if(! container.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

			Host host = HostCache.hosts.get(host_id);

			// TODO 判断用户是否有权限操作该Host
			// if(! host.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

			if(host == null) {
				host = HostCache.getHost(gpu_enabled);
			}

			if(container != null) {

				host.runContainer(container);

				return Msg.success();
			}
			else {
				return new Msg(Msg.Code.NOT_FOUND, null, null);
			}
		}
		catch (Exception e) {

			Routes.logger.error("Run Container[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取容器详情
	 */
	public static Route getContainer = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Container obj = Container.getById(Container.class, id);

			if(obj != null) {

				// 补全容器用户信息
				obj.user = User.getById(User.class, obj.uid);
				return Msg.success(obj);
			}
			else {
				return new Msg(Msg.Code.NOT_FOUND, null, null);
			}
		}
		catch (Exception e) {

			Routes.logger.error("Get Container[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 删除容器
	 */
	public static Route deleteContainer = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Container container = ContainerCache.containers.get(id);

			// 权限：用户删除自己的容器
			if(! container.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

			// Host remove container
			if(!container.user_host) {
				HostCache.hosts.get(container.host_id).removeContainer(container);
			}

			ContainerService.getInstance().removeContainer(container);

			return Msg.success();
		}
		catch (Exception e) {

			Routes.logger.error("Delete Container[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};
}
