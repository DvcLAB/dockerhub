package com.dvclab.dockerhub;

import com.dvclab.dockerhub.cache.ContainerCache;
import com.dvclab.dockerhub.cache.ImageCache;
import com.dvclab.dockerhub.route.Routes;
import com.dvclab.dockerhub.service.ContainerService;
import com.dvclab.dockerhub.websocket.ContainerInfoPublisher;
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

	}

	@Override
	public void run() {

		try {
			ContainerCache.init();
			ImageCache.init();

			port(port);

			webSocket("/containers", ContainerInfoPublisher.class);

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
