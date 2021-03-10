package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.auth.KeycloakAdapter;
import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.*;
import com.dvclab.dockerhub.serialization.Msg;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import one.rewind.db.Daos;
import one.rewind.util.FileUtil;
import spark.Route;

import java.util.List;

import static com.dvclab.dockerhub.cache.ContainerCache.createDockerComposeConfig;

public class ContainerRoute {

	public static Route createContainerDockerComposeConfig = (q, a) -> {

		String uid = q.session().attribute("uid");

		String image_id = q.queryParams("image_id");
		String project_id = q.queryParams("project_id");
		String project_branch = q.queryParams("project_id");
		String dataset_id = q.queryParams("dataset_id");

		float cpus = Float.parseFloat(q.queryParamOrDefault("cpus", "2"));
		float mem = Float.parseFloat(q.queryParamOrDefault("mem", "4"));
		boolean gpu = Boolean.parseBoolean(q.queryParamOrDefault("gpu", "false"));

		try {
			return Msg.success(
					createDockerComposeConfig(uid, image_id, project_id, project_branch, dataset_id, cpus, mem, gpu)
			);
		}
		catch (Exception e) {

			Routes.logger.error("Unable create container, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	public static Route listContainers = (q, a) -> {

		String uid = q.session().attribute("uid");
		String query = q.queryParamOrDefault("q", "");
		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));
		Long size = Long.parseLong(q.queryParamOrDefault("size", "10"));

		try {

			Dao<Container, ?> dao = Daos.get(Container.class);
			QueryBuilder<Container, ?> containerQueryBuilder = dao.queryBuilder();
			Where<Container, ?> containerWhere = containerQueryBuilder.where();

			// 管理员查询分支
			if(UserCache.USERS.get(uid).roles.contains(User.Role.DOCKHUB_ADMIN)) {
				containerWhere.like("id", query + "%")
						.or().like("uid", query + "%");
			}
			// 一般用户查询分支
			else {
				containerWhere.like("id", query + "%")
						.and().eq("uid", uid);
			}



			return null;
		}
		catch (Exception e) {

			Routes.logger.error("Unable create container, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};



}
