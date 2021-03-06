package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.cache.Caches;
import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.Project;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.dvclab.dockerhub.util.ResourceInfoFetcher;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import one.rewind.db.Daos;
import one.rewind.db.exception.DBInitException;
import one.rewind.nio.json.JSON;
import spark.Route;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectRoute {

	/**
	 * 获取项目列表
	 */
	public static Route listProjects = (q, a) -> {

		String uid = q.session().attribute("uid");
		String query = q.queryParamOrDefault("q", "");
		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));
		Long size = Long.parseLong(q.queryParamOrDefault("size", "10"));

		try {

			Dao<Project, ?> dao = Daos.get(Project.class);
			QueryBuilder<Project, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("update_time", false);
			long total = dao.queryBuilder().countOf();

			qb.where().like("id", query + "%")
					.or().like("name", query + "%")
					.or().like("framework", query + "%");

			List<Project> list = qb.query();

			// 返回结果补全 用户信息、数据集信息、镜像信息
			Map<String, User> users = User.getUsers(list.stream().map(c -> c.uid).collect(Collectors.toList()));
			list.stream().forEach(c -> {
				c.user = users.get(c.uid);
				// 补全项目的数据集信息
				if(! c.dataset_ids.isEmpty()) {
					try {
						c.datasets = Dataset.getByIdlList(c.dataset_ids);
					} catch (DBInitException e) {
						e.printStackTrace();
					} catch (SQLException throwables) {
						throwables.printStackTrace();
					}
				}
			});

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("List Project error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 创建项目
	 */
	public static Route createProject = (q, a) -> {

		String uid = q.session().attribute("uid");
		String source = q.body();

		try {

			Project obj = JSON.fromJson(source, Project.class);
			obj.genId();
			obj.uid = uid;
			if(obj.insert()) {

				// 补全用户信息、镜像信息
				obj.user = User.getById(User.class, obj.uid);
				if(! obj.dataset_ids.isEmpty()) {
					obj.datasets = Dataset.getByIdlList(obj.dataset_ids);
				}
				return Msg.success();
			}
			else {
				return Msg.failure();
			}
		}
		catch (Exception e) {

			Routes.logger.error("Create Project[{}] error, ", source, e);
			return Msg.failure(e);
		}
	};


	/**
	 * 获取项目
	 */
	public static Route getProject = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Project obj = Project.getById(Project.class, id);
			if(obj != null) {
				// 补全项目的数据集信息
				if(! obj.dataset_ids.isEmpty()) {
					obj.datasets = Dataset.getByIdlList(obj.dataset_ids);
				}
				return Msg.success(obj);
			}
			else {
				return new Msg(Msg.Code.NOT_FOUND, null, null);
			}
		}
		catch (Exception e) {

			Routes.logger.error("Get Project[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 更新项目
	 */
	public static Route updateProject = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String source = q.body();

		try {

			Project obj = JSON.fromJson(source, Project.class);
			obj.genId();

			if(!obj.id.equals(id)) throw new Exception("Project url can not be changed");

			if(obj.update()) {
				// 补全项目的数据集信息
				if(! obj.dataset_ids.isEmpty()) {
					obj.datasets = Dataset.getByIdlList(obj.dataset_ids);
				}
				return Msg.success(obj);
			}
			else {
				return Msg.failure();
			}
		}
		catch (Exception e) {

			Routes.logger.error("Update Project[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 删除项目
	 */
	public static Route deleteProject = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		// 只有管理员才能删除记录
		if(! Caches.userCache.USERS.get(uid).hasRole(User.Role.DOCKHUB_ADMIN)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

		try {

			Project.deleteById(Project.class, id);
			return Msg.success();
		}
		catch (Exception e) {

			Routes.logger.error("Delete Project[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取项目信息
	 */
	public static Route getInfo = (q, a) -> {

		String uid = q.session().attribute("uid");
		String url = q.queryParamOrDefault("url", "");

		if(url.length() == 0) return Msg.failure("Null URL");
		// TODO 仅支持GitHub？
		if(!url.matches("^https://github.com/.*$")) return new Msg(Msg.Code.BAD_REQUEST, null, null);
		url = URLDecoder.decode(url, StandardCharsets.UTF_8);

		try {
			return Msg.success(ResourceInfoFetcher.getProjectInfo(url));
		}
		catch (Exception e) {
			Routes.logger.error("Unable get Project info, url[{}], ", url, e);
			return Msg.failure(e);
		}
	};
}
