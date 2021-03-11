package com.dvclab.dockerhub.util;

import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.Project;
import one.rewind.db.exception.DBInitException;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Arrays;

public class ResourceInfoFetcher {

	public static Proxy proxy = new ProxyImpl("10.0.0.11", 11111, null, null);

	/**
	 *
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 */
	public static Project getProjectInfo(String url) throws URISyntaxException, DBInitException, SQLException {

		Task t1 = new Task(url);
		BasicRequester.req(t1, proxy);

		String name = t1.r.getDom().select("title")
				.html().replaceAll("GitHub - ", "")
				.split(" ")[0]
				.replaceAll(":", "")
				.replaceAll("^.+?/", "");

		String desc = t1.r.getDom().select("meta[name='description']")
				.attr("content");

		String dataset_conf_url = url.replaceAll("(\\.git|/)$", "")
				.replaceAll("(?si)(www\\.)?github\\.com", "raw.githubusercontent.com")
				+ "/master/config/dataset.conf";


		return new Project(
				name, url, desc, null,
				Arrays.asList(BasicRequester.req(dataset_conf_url, proxy).split("\r?\n"))
		);
	}

	/**
	 *
	 * @param url
	 * @return
	 */
	public static Dataset getDatasetInfo(String url) throws Exception {

		url = url.replaceAll("/?$", "");
		String name = url.replaceAll("^.+/", "");
		String desc = BasicRequester.req(url + "/README.md", proxy);
		String cover_img_url = url + "/.cover_img.png";

		if(desc.length() == 0) throw new Exception("Readme file not exist");

		return new Dataset(name, url, desc, cover_img_url);
	}
}
