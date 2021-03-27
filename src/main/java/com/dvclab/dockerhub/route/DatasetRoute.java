package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.dvclab.dockerhub.util.ResourceInfoFetcher;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import one.rewind.db.Daos;
import one.rewind.txt.StringUtil;
import spark.Route;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据集路由
 */
public class DatasetRoute {

	/**
	 * 获取数据集列表
	 */
	public static Route listDatasets = (q, a) -> {

		String uid = q.session().attribute("uid");
		String query = q.queryParamOrDefault("q", "");
		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));
		Long size = Long.parseLong(q.queryParamOrDefault("size", "10"));

		try {

			Dao<Dataset, ?> dao = Daos.get(Dataset.class);
			QueryBuilder<Dataset, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("update_time", false);
			long total = dao.queryBuilder().countOf();

			qb.where().like("id", query + "%")
					.or().like("name", query + "%");

			List<Dataset> list = qb.query();

			// 返回结果补全 用户信息
			Map<String, User> users = User.getUsers(list.stream().map(c -> c.uid).collect(Collectors.toList()));
			list.stream().forEach(c -> c.user = users.get(c.uid));

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("List Dataset error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 创建数据集
	 */
	public static Route createDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String source = q.body();

		try {

			Dataset ds = Dataset.fromJSON(source, Dataset.class);
			ds.genId();
			ds.uid = uid;
			if(ds.insert()) {
				return Msg.success();
			}
			else {
				return Msg.failure();
			}
		}
		catch (Exception e) {

			Routes.logger.error("Create Dataset[{}] error, ", source, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取数据集
	 */
	public static Route getDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Dataset ds = Dataset.getById(Dataset.class, id);
			if(ds != null) {

				// 补全数据集用户信息
				ds.user = User.getById(User.class, ds.uid);

				return Msg.success(ds);
			}
			else {
				return new Msg(Msg.Code.NOT_FOUND, null, null);
			}
		}
		catch (Exception e) {

			Routes.logger.error("Get Dataset[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 更新数据集
	 * Note: 数据集URL不能修改
	 */
	public static Route updateDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String source = q.body();

		try {

			Dataset ds = Dataset.fromJSON(source, Dataset.class);
			ds.genId();

			if(!ds.id.equals(id)) throw new Exception("Dataset url can not be changed");

			if(ds.update()) {
				return Msg.success(ds);
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
	 * 通过url获取简单信息
	 */
	public static Route getInfo = (q, a) -> {

		String uid = q.session().attribute("uid");
		String url = q.queryParamOrDefault("url", "");

		if(url.length() == 0) return Msg.failure("Null URL");
		url = URLDecoder.decode(url, StandardCharsets.UTF_8);

		try {
			return Msg.success(ResourceInfoFetcher.getDatasetInfo(url));
		}
		catch (Exception e) {
			Routes.logger.error("Unable get Dataset info, url[{}], ", url, e);
			return Msg.failure(e);
		}
	};
}
