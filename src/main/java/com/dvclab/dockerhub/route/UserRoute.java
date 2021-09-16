package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.model.Host;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.typesafe.config.Config;
import io.netty.handler.codec.http.HttpMethod;
import one.rewind.db.Daos;
import one.rewind.db.S3Adapter;
import one.rewind.nio.http.ReqObj;
import one.rewind.nio.http.Requester;
import one.rewind.nio.web.auth.KeycloakAdapter;
import one.rewind.util.Configs;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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
	public static Route getS3Credential = (q, a) -> {
		String uid = q.session().attribute("uid");
		String access_token = q.headers("AUTHORIZATION").replace("Bearer ", "");

		try {
			Config config = Configs.getConfig(KeycloakAdapter.class);
			String client_id = config.getString("client_id");
			String client_secret = config.getString("client_secret");
			String auth_url = config.getString("auth_url");
			String auth_body = "client_id=%s&client_secret=%s&grant_type=urn:ietf:params:oauth:grant-type:token-exchange&requested_token_type=urn:ietf:params:oauth:token-type:access_token&scope=openid&audience=%s&subject_token=%s";
			String body = String.format(auth_body, client_id, client_secret, client_id, access_token);
			// 1 前端token换取后端token
			ReqObj r = Requester.req(auth_url, HttpMethod.POST, null, body.getBytes()).get();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(r.rBody);
			String id_token = node.get("id_token").asText();

			// 2 通过后端id_token获取临时的ak/sk和session
			S3Adapter s3_admin = S3Adapter.get("dvclab");
			Map<String, String> tempS3Tokens = S3Adapter.getS3TemporaryCredential(id_token, s3_admin);

			return Msg.success(tempS3Tokens);
		} catch (Exception e) {
			Routes.logger.error("List S3 tokens error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};
}
