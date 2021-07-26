package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.model.Host;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import one.rewind.db.Daos;
import one.rewind.db.S3Adapter;
import org.apache.http.HttpHeaders;
import spark.Route;

import java.util.List;
import java.util.Map;

/**
 * 用户路由
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

	/**
	 * 获取用户S3的临时AK、SK和SessionToken
	 */
	public static Route listS3Tokens = (q, a) -> {
		String uid = q.session().attribute("uid");
		String keycloak_token = q.headers(HttpHeaders.AUTHORIZATION).replace("bearer ","");

		try {
			Map<String, String> tempS3Tokens = S3Adapter.getS3CredentialWithKeycloak(keycloak_token);

			return Msg.success(tempS3Tokens);
		} catch (Exception e) {
			Routes.logger.error("List S3 tokens error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};
}
