package com.dvclab.dockerhub.util;

import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import com.jcraft.jsch.JSchException;
import com.typesafe.config.Config;
import io.netty.handler.codec.http.HttpMethod;
import one.rewind.db.exception.DBInitException;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.proxy.Proxy;
import one.rewind.io.requester.proxy.ProxyImpl;
import one.rewind.io.requester.task.Task;
import one.rewind.json.JSON;
import one.rewind.util.Configs;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 远端信息请求器
 */
public class ResourceInfoFetcher {

	private static String proxy_host;
	private static int proxy_port;

	// 用户获取Github相关信息
	private static Proxy proxy;

	public static String docker_auth_service_addr;
	public static String docker_auth_service_name;
	public static String docker_auth_admin;
	public static String docker_auth_admin_password;

	static {

		Config config = Configs.getConfig(ResourceInfoFetcher.class);
		proxy_host = config.getString("proxy_host");
		proxy_port = config.getInt("proxy_port");

		docker_auth_service_addr = config.getString("docker_auth_service_addr");
		docker_auth_service_name = config.getString("docker_auth_service_name");
		docker_auth_admin = config.getString("docker_auth_admin");
		docker_auth_admin_password = config.getString("docker_auth_admin_password");

		proxy = new ProxyImpl(proxy_host, proxy_port, null, null);
	}

	/**
	 * 请求获得GitHub项目信息
	 * TODO 不能正常采集时 应抛出对应异常 阻止创建Project
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 * @throws DBInitException
	 * @throws SQLException
	 * @throws IOException
	 */
	public static Project getProjectInfo(String url) throws URISyntaxException, DBInitException, SQLException, IOException, JSchException {

		Task t1 = new Task(url);
		BasicRequester.req(t1, proxy);
		// 解析项目名
		String name = t1.r.getDom().select("title")
				.html().replaceAll("GitHub - ", "")
				.split(" ")[0]
				.replaceAll(":", "")
				.replaceAll("^.+?/", "");
		// 解析项目描述
		String desc = t1.r.getDom().select("meta[name='description']")
				.attr("content");
		// 项目数据集配置文件的url
		String dataset_conf_url = url.replaceAll("(\\.git|/)$", "")
				.replaceAll("(?si)(www\\.)?github\\.com", "raw.githubusercontent.com")
				+ "/master/config/dataset.conf";

		// 项目封面图的url
		String cover_image_url = url.replaceAll("(\\.git|/)$", "")
				.replaceAll("(?si)(www\\.)?github\\.com", "raw.githubusercontent.com")
				+ "/master/config/.cover_img.png"; // 1024 * 1024 png

		// 项目依赖配置文件的url
		String dependencies_url = url.replaceAll("(\\.git|/)$", "")
				.replaceAll("(?si)(www\\.)?github\\.com", "raw.githubusercontent.com")
				+ "/master/config/dep.conf";

		// 项目依赖配置文件的url
		String requirements_url = url.replaceAll("(\\.git|/)$", "")
				.replaceAll("(?si)(www\\.)?github\\.com", "raw.githubusercontent.com")
				+ "/master/config/requirements.txt";

		String dependencies_src = BasicRequester.req(dependencies_url, proxy);
		// 解析得到项目的依赖
		Map<String, String> deps = JSON.fromJson(dependencies_src, new TypeToken<HashMap<String, String>>(){}.getType());


		String requirements_src = BasicRequester.req(requirements_url, proxy);

		Map<String, String> pypi_deps = Arrays.stream(requirements_src.split("\r?\n")).map(line -> {
			String[] tokens = line.split("=");
			return new AbstractMap.SimpleEntry<>(tokens[0], tokens[1]);
		}).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));


		// 解析得到项目的所有分支
		List<String> branches = new ArrayList<>();
		String branches_url = url.replaceAll("(\\.git|/)$", "") + "/branches/all";
		String branches_src = BasicRequester.req(branches_url, proxy);
		Pattern p = Pattern.compile("(?<=branch=\").+?(?=\")");
		Matcher m = p.matcher(branches_src);
		while (m.find()) {
			branches.add(m.group());
		}

		if(name.isEmpty()) throw new IOException("Can not retrieve project name");
		if(branches.size() == 0) throw new IOException("Can not retrieve project branches");

		// 更新私有PypiServer的依赖库
		PypiServerUpdater.getInstance().update(pypi_deps.keySet());

		return new Project(
				name, url, desc, cover_image_url,
				branches,
				Arrays.asList(BasicRequester.req(dataset_conf_url, proxy).split("\r?\n")),
				deps,
				pypi_deps
		);
	}

	/**
	 * 请求获得Ceph中存储的数据集信息
	 * @param url
	 * @return
	 */
	public static Dataset getDatasetInfo(String url) throws Exception {

		url = url.replaceAll("/?$", "");
		String name = url.replaceAll("^.+/", "");
		String desc = BasicRequester.req(url + "/README.md");
		String cover_img_url = url + "/.cover_img.png";

		if(desc == null || desc.length() == 0) throw new Exception("Readme file not exist");

		return new Dataset(name, url, desc, cover_img_url);
	}

	/**
	 * 请求获得Docker Registry的 auth token (Docker Auth)
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
		return map.get("token");
	}
}
