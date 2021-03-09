package com.dvclab.dockerhub;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static spark.Spark.port;

public class DockerHubServiceWrapper implements Runnable {

	public static final Logger logger = LogManager.getLogger(DockerHubServiceWrapper.class);

	public static DockerHubServiceWrapper instance;

	/**
	 * 单例模式
	 * @return
	 */
	public static DockerHubServiceWrapper getInstance() {

		if (instance == null) {
			synchronized (DockerHubServiceWrapper.class) {
				if (instance == null) {
					instance = new DockerHubServiceWrapper();
				}
			}
		}
		return instance;
	}

	public long publish_ts = System.currentTimeMillis();

	public int port = 50000;

	private DockerHubServiceWrapper() {

	}

	@Override
	public void run() {

		port(port);
	}
}
