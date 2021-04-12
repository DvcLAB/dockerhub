package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.DockerHubService;
import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.cache.HostCache;
import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.*;
import com.dvclab.dockerhub.serialization.Msg;
import com.dvclab.dockerhub.service.ContainerService;
import com.dvclab.dockerhub.service.ReverseProxyService;
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

			// 返回结果补全 用户信息、容器运行时长、镜像信息
			Map<String, User> users = User.getUsers(list.stream().map(c -> c.uid).collect(Collectors.toList()));
			Map<String, Image> images = Image.getImages(list.stream().map(c -> c.image_id).collect(Collectors.toList()));
			list.stream().forEach(c -> {
				c.user = users.get(c.uid);
				c.image = images.get(c.image_id);
				if(c.status == Container.Status.Running) c.alive_time = System.currentTimeMillis() - c.begin_run_time.getTime();
			});

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("Unable create container, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 更新容器
	 */
	public static Route updateContainer = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String name = q.params(":name");

		try {

			Container container = Container.getById(Container.class, id);
			container.container_name = name;
			if(container.update()) {
				return Msg.success(container);
			}
			else {
				return Msg.failure();
			}
		}
		catch (Exception e) {

			Routes.logger.error("Update Container[{}] error, ", id, e);
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
			if(container == null) return new Msg(Msg.Code.NOT_FOUND, null, null);

			// TODO 判断用户是否有权限使用 Container 待验证
			if(! container.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

			Host host;
			// 指定 host_id
			if(HostCache.hosts.containsKey(host_id)) {

				if(UserCache.USERS.get(uid).roles.contains(User.Role.DOCKHUB_ADMIN)) {
					host = HostCache.hosts.get(host_id);
				}
				else {
					return new Msg(Msg.Code.ACCESS_DENIED, null, null);
				}
			}
			// 未指定 host_id
			else {
				host = HostCache.getHost(gpu_enabled, container.cpus, container.mem);
			}

			host.runContainer(container);
			return Msg.success();
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

	/**
	 * 获取容器的映射信息
	 */
	public static Route getContainerProxyInfo = (q, a) -> {

		String uid = q.params(":uid");
		String id = q.params(":id");

		String frp_local_ip = "127.0.0.1";
		String frp_type = "tcp";
		Integer frp_local_port = 8988;

		try {

			Container container = ContainerCache.containers.get(id);


			if(container == null || container.status == Container.Status.Deleted)
				return new Msg(Msg.Code.NOT_FOUND, null, null);

			// 权限：用户只能获取自己容器的信息
			if(! container.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);
			// 仍未分配端口
			if(container.tunnel_id == null) return new Msg(Msg.Code.BAD_REQUEST, null, null);

			Tunnel t = ReverseProxyService.getInstance().tunnels.get(container.tunnel_id);
			ContainerService.AssignInfo assignInfo = new ContainerService.AssignInfo()
					.withFrp_local_ip(frp_local_ip)
					.withFrp_server_addr(t.wan_addr)
					.withFrp_type(frp_type)
					.withFrp_local_port(frp_local_port)
					.withFrp_remote_port(container.tunnel_port)
					.withFrp_server_port(t.wan_port);

			return Msg.success(assignInfo);
		}
		catch (Exception e) {

			Routes.logger.error("Get Container[{}] Assign Info error, ", id, e);
			return Msg.failure(e);
		}
	};
}
