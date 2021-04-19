package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.filter.Authenticator;
import com.dvclab.dockerhub.serialization.JsonTransformer;
import com.dvclab.dockerhub.serialization.Msg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.WebSocketConstants;
import spark.Response;

import static spark.Spark.*;

public class Routes {

	public static final Logger logger = LogManager.getLogger(Routes.class.getName());
	public static String NoAuthRoutes = String.join("|",
			"/_containers",
			"/containers/.*/assign$",
			"/_hosts"
	);

	/**
	 *
	 * @param a
	 */
	private static void setHeader(Response a) {

		// 允许跨域调用 调试时反注释
		a.header("Access-Control-Allow-Origin", "*");
		a.header("Access-Control-Request-Method", "*");
		a.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
		a.header("Access-Control-Allow-Headers", "*");
		a.header("Access-Control-Allow-Credentials", "true");

		// 返回token
		a.header("Access-Control-Expose-Headers", "Authorization");
		// 禁用HTTP cache
		a.header("Cache-Control", "no-store");
		// gzip 压缩
		a.header("Content-Encoding", "gzip");
		// 返回JSON
		a.type("application/json");
	}

	/**
	 *
	 */
	public static void init() {

		Authenticator authenticator = new Authenticator();

		JsonTransformer transformer = new JsonTransformer();

		logger.info("Init routes ...");


		/**
		 * CORS 跨域请求设置
		 */
		options("/*", (q, a) -> {

			String accessControlRequestHeaders = q.headers("Access-Control-Request-Headers");
			if (accessControlRequestHeaders != null) {
				a.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
			}

			String accessControlRequestMethod = q.headers("Access-Control-Request-Method");
			if (accessControlRequestMethod != null) {
				a.header("Access-Control-Allow-Methods", accessControlRequestMethod);
			}

			return "OK";
		});

		/**
		 * 请求预处理
		 */
		before((q, a) -> {

			// 访问时间
			q.session().attribute("begin_ts", System.currentTimeMillis());

			// 登录验证
			if(!q.pathInfo().toLowerCase().matches(NoAuthRoutes) && !q.requestMethod().equals("OPTIONS") ) {
				try {
					authenticator.handle(q, a);
				}
				catch (Exception e) {
					logger.error("Token verify error, ", e);

					// 这里如果不写成halt的话，如果报token错误，会有401的响应头，但是后续的请求仍然进行，导致数据是正常的数据，这里的响应会被覆盖
					halt(401, "Token error");
				}
			}
		});

		/**
		 * 请求后处理
		 */
		after((q, a) -> {

			String uid = q.session().attribute("uid");

			logger.info("[{}] [{}] {} {} in {}ms", uid == null ? "" :
							uid,
					q.ip(),
					q.requestMethod(),
					q.pathInfo(),
					System.currentTimeMillis() - (long) q.session().attribute("begin_ts")
			);

			// 设置返回 HTTP header
			setHeader(a);
			// 设置ws header
			if(q.pathInfo().toLowerCase().matches("/_containers")) a.header(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL,
					q.headers(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL));
		});

		/**
		 * 404的处理
		 */
		notFound((q, a) -> new Msg(Msg.Code.NOT_FOUND, null, null).toJSON());

		// 用户列表
		path("/users", () -> {
			get("", UserRoute.listUsers, transformer);
		});

		// 主机
		path("/hosts", () -> {
			get("", HostRoute.listHosts, transformer);
			put("", HostRoute.createHost, transformer);
			get("/:id", HostRoute.getHost, transformer);
			post("/:id", HostRoute.updateHost, transformer);
			delete("/:id", HostRoute.deleteHost, transformer);
		});

		// 镜像
		path("/images", () -> {
			get("", ImageRoute.listImages, transformer);
			get("_project", ImageRoute.getImagesForProject, transformer);
			get("/:name/tags", ImageRoute.listImageTags, transformer);
			post("/:id", ImageRoute.updateImage, transformer);
			get("/:id", ImageRoute.getImage, transformer);
			delete("/:id", ImageRoute.deleteImage, transformer);
		});

		// 项目
		path("/projects", () -> {
			get("_info", ProjectRoute.getInfo, transformer);
			get("", ProjectRoute.listProjects, transformer);
			put("", ProjectRoute.createProject, transformer);
			get("/:id", ProjectRoute.getProject, transformer);
			post("/:id", ProjectRoute.updateProject, transformer);
			delete("/:id", ProjectRoute.deleteProject, transformer);
		});

		// 数据集
		path("/datasets", () -> {
			get("_info", DatasetRoute.getInfo, transformer);
			get("", DatasetRoute.listDatasets, transformer);
			put("", DatasetRoute.createDataset, transformer);
			get("/:id", DatasetRoute.getDataset, transformer);
			post("/:id", DatasetRoute.updateDataset, transformer);
			delete("/:id", DatasetRoute.deleteDataset, transformer);
		});

		// 容器
		path("/containers", () -> {
			put("", ContainerRoute.createContainer, transformer);
			get("", ContainerRoute.listContainers, transformer);
			get("/:id", ContainerRoute.getContainer, transformer);
			post("/:id/names/:name", ContainerRoute.updateContainer, transformer);
			post("/:id/run", ContainerRoute.runContainer, transformer);
			post("/:id/pause", ContainerRoute.pauseContainer, transformer);
			post("/:id/restart", ContainerRoute.restartContainer, transformer);
			delete("/:id", ContainerRoute.deleteContainer, transformer);
			get("/:id/users/:uid/assign", ContainerRoute.getContainerProxyInfo, transformer);
		});
	}
}
