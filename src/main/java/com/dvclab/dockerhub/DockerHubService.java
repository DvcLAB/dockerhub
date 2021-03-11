package com.dvclab.dockerhub;

import com.dvclab.dockerhub.service.ContainerFactory;
import com.dvclab.dockerhub.service.ReverseProxyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static spark.Spark.port;

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

	public ContainerFactory containerFactory;
	public ReverseProxyService reverseProxyMgr;

	public long publish_ts = System.currentTimeMillis();

	public int port = 50000;

	public boolean use_proxy = true;

	/**
	 *
	 */
	private DockerHubService() {

		containerFactory = new ContainerFactory();
		reverseProxyMgr = new ReverseProxyService();
	}

	@Override
	public void run() {

		port(port);
	}
}
