package com.dvclab.dockerhub.util;

import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpMethod;
import one.rewind.db.exception.DBInitException;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.UsernamePasswordCredentials;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResourceInfoFetcher {

	public static Proxy proxy = new ProxyImpl("10.0.0.11", 11111, null, null);

	public static String docker_auth_service_addr = "registry.dvclab.com:61768";
	public static String docker_auth_service_name = "Docker registry";
	public static String docker_auth_admin = "root";
	public static String docker_auth_admin_password = "hanwuji412";

	/**
	 * TODO 不能正常采集时 应抛出对应异常 阻止创建Project
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

		List<String> branches = new ArrayList<>();
		String branches_url = url.replaceAll("(\\.git|/)$", "") + "/branches/all";
		String branches_src = BasicRequester.req(branches_url, proxy);
		Pattern p = Pattern.compile("(?<=branch=\").+?(?=\")");
		Matcher m = p.matcher(branches_src);
		if(m.find()) {
			branches.add(m.group());
		}

		return new Project(
				name, url, desc, null,
				branches,
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

		if(desc == null || desc.length() == 0) throw new Exception("Readme file not exist");

		return new Dataset(name, url, desc, cover_img_url);
	}

	/**
	 *
	 * @param scope
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public static String getDockerAuthToken(String scope) throws URISyntaxException, IOException {

		String url = "https://" + docker_auth_service_addr + "/auth?service=" + URLEncoder.encode(docker_auth_service_name, StandardCharsets.UTF_8) + "&scope=" + scope;

		String auth = docker_auth_admin + ":" + docker_auth_admin_password;
		byte[] encodedAuth = Base64.encodeBase64(
				auth.getBytes(StandardCharsets.ISO_8859_1));
		String authHeader = "Basic " + new String(encodedAuth);

		Task t = new Task(url, HttpMethod.GET, Map.of(HttpHeaders.AUTHORIZATION, authHeader), null, null, null);
		BasicRequester.req(t);

		Map<String, String> map = new ObjectMapper().readValue(t.r.getText(), Map.class);
		return map.get("access_token");
	}
}
