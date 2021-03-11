package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.Image;
import com.dvclab.dockerhub.serialization.Msg;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import one.rewind.db.Daos;
import one.rewind.txt.StringUtil;
import spark.Route;

import java.util.List;

public class ImageRoute {

	/**
	 * 获取镜像列表
	 */
	public static Route listImages = (q, a) -> {

		String uid = q.session().attribute("uid");
		String query = q.queryParamOrDefault("q", "");
		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));
		Long size = Long.parseLong(q.queryParamOrDefault("size", "10"));

		try {

			Dao<Image, ?> dao = Daos.get(Image.class);
			QueryBuilder<Image, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("update_time", false);
			long total = dao.queryBuilder().countOf();

			qb.where().like("id", query + "%")
					.or().like("name", query + "%")
					.or().like("framework", query + "%");

			List<Image> list = qb.query();

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("List Image error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 创建镜像
	 */
	public static Route createImage = (q, a) -> {

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
}
