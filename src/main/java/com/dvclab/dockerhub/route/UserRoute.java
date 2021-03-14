package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.model.Host;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import one.rewind.db.Daos;
import spark.Route;

import java.util.List;

/**
 *
 */
public class UserRoute {

	/**
	 * 获取用户列表
	 */
	public static Route listUsers = (q, a) -> {

		String uid = q.session().attribute("uid");
		String query = q.queryParamOrDefault("q", "");
		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));
		Long size = Long.parseLong(q.queryParamOrDefault("size", "10"));

		try {

			Dao<User, ?> dao = Daos.get(User.class);
			QueryBuilder<User, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("update_time", false);
			long total = dao.queryBuilder().countOf();

			qb.where().like("id", query + "%")
					.or().like("username", query + "%");

			List<User> list = qb.query();

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("List User error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};
}
