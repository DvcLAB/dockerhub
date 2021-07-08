package com.dvclab.dockerhub.route.test;

import com.dvclab.dockerhub.route.UserRoute;
import org.junit.Test;

import java.util.List;

import static spark.Spark.get;
import static spark.Spark.port;

public class SparkJavaTest {

	@Test
	public void test() throws InterruptedException {

		port(80);

		get("/", (q, a) -> {

			List<String> a1 = List.of(q.queryParamsValues("a"));
			System.err.println(a1);

			return "OK";
		});

		Thread.sleep(600000);
	}
}
