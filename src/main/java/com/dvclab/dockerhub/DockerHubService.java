package com.dvclab.dockerhub;

import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.cache.HostCache;
import com.dvclab.dockerhub.cache.ImageCache;
import com.dvclab.dockerhub.route.Routes;
import com.dvclab.dockerhub.websocket.ContainerInfoPublisher;
import com.typesafe.config.Config;
import one.rewind.util.Configs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static spark.Spark.port;
import static spark.Spark.webSocket;

/**
 *
 */
public class DockerHubService implements Runnable {

	public static final Logger logger = LogManager.getLogger(DockerHubService.class);

	public static DockerHubService instance;

	public String service_url;

	/**
	 * 单例模式
	 * @return
	 */
	public static DockerHubService getInstance() {

		if (instance == null) {
			synchronized (DockerHubService.class) {
				if (instance == null) {
					instance = new DockerHubService();
				}
			}
		}
		return instance;
	}

	public long publish_ts = System.currentTimeMillis();

	public int port = 50000;

	/**
	 *
	 */
	private DockerHubService() {
		Config config = Configs.getConfig(DockerHubService.class);
		service_url = config.getString("service_url");
	}

	@Override
	public void run() {

		try {
			ContainerCache.init();
			ImageCache.init();
			HostCache.init();

			port(port);

			webSocket("/_containers", ContainerInfoPublisher.class);

			Routes.init();
		}
		catch (Exception e) {
			logger.error("INIT Failed, ", e);
		}

	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		getInstance().run();
	}
}
