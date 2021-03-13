package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.DockerHubService;
import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.Container;
import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.Image;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.dvclab.dockerhub.service.ContainerFactory;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import one.rewind.db.Daos;
import spark.Route;

import java.util.List;

public class ContainerRoute {

	/**
	 * 构建容器DockerCompose配置文件
	 */
	public static Route createContainerDockerComposeConfig = (q, a) -> {

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
					DockerHubService.getInstance().containerFactory
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
						.and().eq("uid", uid);
			}

			List<Container> list = qb.query();

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("Unable create container, uid[{}], ", uid, e);
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

			// 只有管理员才能删除记录
			if(! ContainerCache.containers.get(id).uid.equals(uid))
				return new Msg(Msg.Code.ACCESS_DENIED, null, null);

			Dataset.deleteById(Dataset.class, id);
			return Msg.success();
		}
		catch (Exception e) {

			Routes.logger.error("Delete Container[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};
}
