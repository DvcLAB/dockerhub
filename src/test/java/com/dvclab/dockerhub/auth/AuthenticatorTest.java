package com.dvclab.dockerhub.auth;

import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.route.UserRoute;
import io.netty.handler.codec.http.HttpMethod;
import one.rewind.db.model.Model;
import one.rewind.nio.http.ReqObj;
import one.rewind.nio.http.Requester;
import one.rewind.nio.web.cache.Caches;
import one.rewind.nio.web.cache.UserCache;
import one.rewind.nio.web.filter.Authenticator;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static spark.Spark.*;
import static spark.Spark.get;

public class AuthenticatorTest {

	@Test
	public void test1() throws InterruptedException {


		Caches.userCache = new UserCache<>(User::getById, User::new);
		Authenticator authenticator = new Authenticator(com.dvclab.dockerhub.cache.Caches.userCache);

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

		port(80);

		Thread.sleep(1000);
		Optional<ReqObj> r = Requester.req("http://127.0.0.1/test", HttpMethod.GET, Map.of("Auth....", "Bearer "));
		String text = r.get().getText();


		Thread.sleep(600000L);
	}
}
