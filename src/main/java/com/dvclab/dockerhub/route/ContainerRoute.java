package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.cache.Caches;
import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.cache.HostCache;
import com.dvclab.dockerhub.model.*;
import com.dvclab.dockerhub.serialization.Msg;
import com.dvclab.dockerhub.service.ContainerService;
import com.dvclab.dockerhub.service.ReverseProxyService;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import one.rewind.db.Daos;
import spark.Route;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 容器相关路由
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

		String owner = q.queryParamOrDefault("owner", "");
		List<Container.Status> status_list = List.of(q.queryParamsValues("status")).stream()
				.map(Container.Status::valueOf)
				.collect(Collectors.toList());

		try {

			Dao<Container, ?> dao = Daos.get(Container.class);
			QueryBuilder<Container, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("create_time", false);

			// TODO 总数查询没有考虑到条件
			long total = dao.queryBuilder().countOf();

			Where<Container, ?> where = qb.where();

			// 管理员查询分支
			if(Caches.userCache.USERS.get(uid).hasRole(User.Role.DOCKHUB_ADMIN)) {
				where.like("id", query + "%")
						.or().like("uid", query + "%")
						// query可为用户名
						.or().eq("uid", User.getUserId(query));
			}
			// 一般用户查询分支，只能查询到自己的容器
			// TODO 还应该查询到分享给我的容器
			else {
				where.like("id", query + "%")
						.and().eq("uid", uid);
			}

			if(owner != null && owner.length() > 0) where.and().eq("uid", owner);

			if(status_list.size() > 0) where.and().in("status", status_list);

			List<Container> list = qb.query();

			// 返回结果补全 用户信息、容器运行时长、镜像信息、时序数据信息
			Map<String, User> users = User.getUsers(list.stream().map(c -> c.uid).collect(Collectors.toList()));
			Map<String, Image> images = Image.getImages(list.stream().map(c -> c.image_id).collect(Collectors.toList()));
			list.forEach(c -> {
				c.user = users.get(c.uid);
				c.image = images.get(c.image_id);
				if(c.status == Container.Status.Running) {
					// 维持时序数据
					c.cpu_series = ContainerCache.containers.get(c.id).cpu_series;
					c.mem_series = ContainerCache.containers.get(c.id).mem_series;
					c.proc_series = ContainerCache.containers.get(c.id).proc_series;
					c.alive_time = System.currentTimeMillis() - c.begin_run_time.getTime();
				}

			});

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			/*e.printStackTrace();*/
			Routes.logger.error("Unable get container list, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 更新容器
	 * 当前只能更新容器名
	 */
	public static Route updateContainer = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String name = q.params(":name");

		try {

			Container container = Container.getById(Container.class, id);
			container.container_name = name;

			if(container.update()) {

				// 维持时序数据
				container.cpu_series = ContainerCache.containers.get(container.id).cpu_series;
				container.mem_series = ContainerCache.containers.get(container.id).mem_series;
				container.proc_series = ContainerCache.containers.get(container.id).proc_series;
				// 更新容器缓存
				ContainerCache.containers.put(id, container);
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
	 * 在指定主机运行容器
	 */
	public static Route runContainer = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		String host_id = q.queryParamOrDefault("host_id", "");

		try {

			Container container = Container.getById(Container.class, id);
			// 容器不存在或者容器是除New之外的其余状态
			if(container == null || container.status != Container.Status.New) return new Msg(Msg.Code.NOT_FOUND, null, null);
			// 无权执行容器
			if(!container.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

			Host host;
			// 指定 host_id
			if(HostCache.hosts.containsKey(host_id)) {
				// 只有管理员才可以选取主机
				if(Caches.userCache.USERS.get(uid).hasRole(User.Role.DOCKHUB_ADMIN)) {
					host = HostCache.hosts.get(host_id);
				}
				else {
					return new Msg(Msg.Code.ACCESS_DENIED, null, null);
				}
			}
			// 未指定 host_id
			else {
				host = HostCache.getHost(container.gpu_enabled, container.cpus, container.mem);
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
	 * 暂停容器
	 */
	public static Route pauseContainer = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Container container = ContainerCache.containers.computeIfAbsent(id, v -> Container.getById(Container.class, id));

			// 容器不存在
			if(container == null) return new Msg(Msg.Code.NOT_FOUND, null, null);
			// 无权执行容器
			if(!Caches.userCache.USERS.get(uid).hasRole(User.Role.DOCKHUB_ADMIN) && !container.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);
			// 首先检查容器状态，只在Running和Port_Forwarding_Success状态才能执行暂停操作
			if(container.status != Container.Status.Running &&
			container.status != Container.Status.Port_Forwarding_Success)
				return new Msg(Msg.Code.METHOD_REJECTED, null, null);

			Host host = HostCache.hosts.get(container.host_id);
			if(host == null) return new Msg(Msg.Code.NOT_FOUND, null, null);
			host.pauseContainer(container);

			return Msg.success();
		}
		catch (Exception e) {

			Routes.logger.error("Run Container[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 重启容器
	 */
	public static Route restartContainer = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Container container = ContainerCache.containers.computeIfAbsent(id, v -> Container.getById(Container.class, id));

			// 容器不存在
			if(container == null) return Msg.failure(Msg.Code.NOT_FOUND);
			// 无权执行容器
			if(!Caches.userCache.USERS.get(uid).hasRole(User.Role.DOCKHUB_ADMIN) && !container.uid.equals(uid))
				return Msg.failure(Msg.Code.ACCESS_DENIED);
			// 首先检查容器状态，避免重复运行
			if(container.status != Container.Status.Paused) return Msg.failure(Msg.Code.METHOD_REJECTED);

			Host host = HostCache.hosts.get(container.host_id);
			if(host == null) return Msg.failure(Msg.Code.NOT_FOUND);
			host.restartContainer(container);

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
			// 普通用户不能查到删除状态的容器
			if(!Caches.userCache.USERS.get(uid).hasRole(User.Role.DOCKHUB_ADMIN)
					&& obj.status == Container.Status.Deleted) return new Msg(Msg.Code.NOT_FOUND, null, null);

			if(obj != null) {

				// 补全容器用户信息、镜像信息、运行时长信息、时序信息
				obj.user = User.getById(User.class, obj.uid);
				obj.image = Image.getById(Image.class, obj.image_id);

				if(obj.status == Container.Status.Running) {
					// 维持时序数据
					obj.cpu_series = ContainerCache.containers.get(obj.id).cpu_series;
					obj.mem_series = ContainerCache.containers.get(obj.id).mem_series;
					obj.proc_series = ContainerCache.containers.get(obj.id).proc_series;

					obj.alive_time = System.currentTimeMillis() - obj.begin_run_time.getTime();
				}


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

			if(container == null
					|| container.status == Container.Status.Deleted) return new Msg(Msg.Code.NOT_FOUND, null, null);

			// 权限：用户删除自己的容器
			if(!Caches.userCache.USERS.get(uid).hasRole(User.Role.DOCKHUB_ADMIN)
					&& ! container.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

			// 公共服务器shutdown容器
			if(!container.user_host) {
				HostCache.hosts.get(container.host_id).removeContainer(container);
			}
			// 回收容器资源
			ContainerService.getInstance().removeContainer(container);

			return Msg.success();
		}
		catch (Exception e) {

			Routes.logger.error("Delete Container[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取分配给容器的frp配置信息
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
			if(! container.uid.equals(uid)) return new Msg<>(Msg.Code.ACCESS_DENIED, null, null);
			// 仍未分配端口
			if(container.tunnel_id == null) return new Msg<>(Msg.Code.BAD_REQUEST, null, null);

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
