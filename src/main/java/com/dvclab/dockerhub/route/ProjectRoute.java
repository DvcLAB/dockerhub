package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.Image;
import com.dvclab.dockerhub.model.Project;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.dvclab.dockerhub.util.ResourceInfoFetcher;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import one.rewind.db.Daos;
import spark.Route;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("List Project error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 创建镜像
	 */
	public static Route createProject = (q, a) -> {

		String uid = q.session().attribute("uid");
		String source = q.body();

		try {

			Image obj = Image.fromJSON(source, Image.class);
			obj.genId();
			obj.uid = uid;
			if(obj.insert()) {
				return Msg.success();
			}
			else {
				return Msg.failure();
			}
		}
		catch (Exception e) {

			Routes.logger.error("Create Image[{}] error, ", source, e);
			return Msg.failure(e);
		}
	};


	/**
	 * 获取镜像
	 */
	public static Route getDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Image obj = Image.getById(Image.class, id);
			if(obj != null) {
				return Msg.success(obj);
			}
			else {
				return new Msg(Msg.Code.NOT_FOUND, null, null);
			}
		}
		catch (Exception e) {

			Routes.logger.error("Get Image[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 更新镜像
	 */
	public static Route updateDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String source = q.body();

		try {

			Image obj = Image.fromJSON(source, Image.class);
			obj.genId();

			if(!obj.id.equals(id)) throw new Exception("Dataset url can not be changed");

			if(obj.update()) {
				return Msg.success(obj);
			}
			else {
				return Msg.failure();
			}
		}
		catch (Exception e) {

			Routes.logger.error("Update Dataset[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 *
	 */
	public static Route deleteDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		// 只有管理员才能删除记录
		if(! UserCache.USERS.get(uid).roles.contains(User.Role.DOCKHUB_ADMIN)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

		try {

			Dataset.deleteById(Dataset.class, id);
			return Msg.success();
		}
		catch (Exception e) {

			Routes.logger.error("Delete Dataset[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 *
	 */
	public static Route getInfo = (q, a) -> {

		String uid = q.session().attribute("uid");
		String url = q.queryParamOrDefault("url", "");

		if(url.length() == 0) return Msg.failure("Null URL");
		url = URLDecoder.decode(url, StandardCharsets.UTF_8);

		try {
			return ResourceInfoFetcher.getProjectInfo(url);
		}
		catch (Exception e) {
			Routes.logger.error("Unable get Project info, url[{}], ", url, e);
			return Msg.failure(e);
		}
	};
}
