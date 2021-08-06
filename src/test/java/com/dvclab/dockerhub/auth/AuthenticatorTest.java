package com.dvclab.dockerhub.auth;

import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.route.Routes;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import io.netty.handler.codec.http.HttpMethod;
import one.rewind.db.Daos;
import one.rewind.db.model.Model;
import one.rewind.nio.http.ReqObj;
import one.rewind.nio.http.Requester;
import com.dvclab.dockerhub.cache.Caches;
import one.rewind.nio.web.cache.UserCache;
import one.rewind.nio.web.filter.Authenticator;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static spark.Spark.*;
import static spark.Spark.get;

public class AuthenticatorTest {

	@Test
	public void test1() throws InterruptedException {

		Caches.userCache = new UserCache<>(User::getById, User::new);
		Authenticator authenticator = new Authenticator(Caches.userCache);

		port(80);
		path("/test", () -> {
			get("", (q, a) -> {

				try {
					authenticator.handle(q, a);
				}
				catch (Exception e) {
					Model.logger.error("Token verify error, ", e);
					halt(401, "Token error");
				}

				return "OK";
			});
		});



		Thread.sleep(1000);
		Optional<ReqObj> r = Requester.req("http://127.0.0.1/test", HttpMethod.GET, Map.of(
				"Authorization",
				"Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI5Q3RkR01FVDRmTl9YNXFJZHp3OGI1bkE2bTJBUDhJeWlKdFRUMi1QeVFZIn0.eyJleHAiOjE2MjgyMjY0MTMsImlhdCI6MTYyODIxOTIxMywiYXV0aF90aW1lIjoxNjI4MjE5MjEyLCJqdGkiOiJlMWFjZGQyMC01ZjRkLTRlNWMtYTA4MS1mN2MxNGNjYTgwNDkiLCJpc3MiOiJodHRwczovL2F1dGguMzMuZHZjL2F1dGgvcmVhbG1zL0R2Y0xBQiIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiIwN2ZiYzQyYS1kNDRhLTQwMmMtYTVjYy1lY2M4MDJiMDRlYTEiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJmcm9udF9lbmQiLCJub25jZSI6IjMyNDEzNWNkLWYwYTQtNGFlYS05Y2VjLTg1YzBmMTRkYTRmYiIsInNlc3Npb25fc3RhdGUiOiI5ZDgzYTZiYS0zYTVlLTQ0OTgtYmZhMi1lYjU3YzNkZWNmZTEiLCJhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbIioiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwiRE9DS0hVQl9VU0VSIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIHVzZXJpbmZvIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoidGVzdDAxIHlvdSIsInByZWZlcnJlZF91c2VybmFtZSI6InlvdXRlc3QwMSIsImdpdmVuX25hbWUiOiJ0ZXN0MDEiLCJmYW1pbHlfbmFtZSI6InlvdSIsImVtYWlsIjoiYWRtaW5AZHZjLmNvbSIsImVuYWJsZWQiOnRydWV9.CsMbQOdI4MnE3TsLc6aDxP2wLGUQn00ZZ4bF5BkXOFPbUV6HzZHPVQ_KRw7JsYvJBnP6_T5tO-6IKEdIaZxxCizvbpbe5jD3cw0FAGFKKeflELEuyeTlwIJsyZdNjOhITVGTenZrApRKVB0LBSXU4a7XlVQ6L0XtWhlGvVd7sDnmgvxdmeMn9Ds7Sq8u3au3bEtmx8VixtTUdMSSIgcOADVj5pFgeAnQ6UklGbt2i_YiaocFzyCP2waY0cDapdwCzFypH4Yjt2ow8KCPn0Sws-A_FsfdeMepBGAnyuFL3UFqZR4xjeoBXXNS1prFMm1BbH4DDTuEWfR1x8GsQi_W1Q"));
		String text = r.get().getText();

		Model.logger.info("test="+text);

		Thread.sleep(600000L);
	}

	@Test
	public void test2(){
		int page = 1;
		int size = 10;
		String query = "";
		String uid = "";
		try {
			Dao<User, ?> dao = Daos.get(User.class);
			QueryBuilder<User, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("update_time", false);
			long total = dao.queryBuilder().countOf();

			qb.where().like("id", query + "%")
					.or().like("username", query + "%");

			List<User> list = qb.query();
			Model.logger.info("list="+list.get(0).id);

		}
		catch (Exception e) {

			Routes.logger.error("List User error, uid[{}], ", uid, e);

		}
	}
}
