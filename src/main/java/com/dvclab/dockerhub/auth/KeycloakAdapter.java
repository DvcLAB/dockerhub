package com.dvclab.dockerhub.auth;

import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.service.ContainerService;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import io.netty.handler.codec.http.HttpMethod;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.task.Task;
import one.rewind.json.JSON;
import one.rewind.util.Configs;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class KeycloakAdapter {

	public static KeycloakAdapter instance;

	/**
	 * 单例模式
	 * @return
	 */
	public static KeycloakAdapter getInstance() {

		if (instance == null) {
			synchronized (KeycloakAdapter.class) {
				if (instance == null) {
					instance = new KeycloakAdapter();
				}
			}
		}
		return instance;
	}

	public String host = "http://127.0.0.1:8080";
	public String realm = "test";

	public String frontend_client_id = "test";
	public String frontend_client_secret = "71034e0a-50f5-4795-b961-e28f206e8f82";

	public String client_id = "test";
	public String client_secret = "71034e0a-50f5-4795-b961-e28f206e8f82";

	private String request_pat_template = "%s/auth/realms/%s/protocol/openid-connect/token";
	private String request_pat_body_template = "client_id=%s&client_secret=%s&grant_type=client_credentials";
	private String exchange_token_body_template = "client_id=%s&client_secret=%s&" +
			"grant_type=urn:ietf:params:oauth:grant-type:token-exchange&audience=%s&" +
			"subject_token=%s&requested_token_type=urn:ietf:params:oauth:token-type:access_token";
	private String verify_url_template = "%s/auth/realms/%s/protocol/openid-connect/token/introspect";
	private String verify_url;
	private String verify_body_template = "client_id=%s&client_secret=%s&token=%s";
	private String create_resource_template = "%s/auth/realms/%s/authz/protection/resource_set";
	private String delete_resource_template = "%s/auth/realms/%s/authz/protection/resource_set/%s";
	private String create_resource_url;
	private String delete_resource_url;
	private String apply_resource_policy_template = "%s/auth/realms/%s/authz/protection/uma-policy/%s";
	private String apply_resource_policy_url;
	private String request_pat_url;
	private String request_pat_body;
	private String exchange_token_body;

	public Admin admin;


	/**
	 *
	 */
	public KeycloakAdapter() {

		Config config = Configs.getConfig(KeycloakAdapter.class);

		host = config.getString("host");
		realm = config.getString("realm");

		frontend_client_id = config.getString("client_name");
		frontend_client_secret = config.getString("client_secret");

		client_id = config.getString("client_name");
		client_secret = config.getString("client_secret");
		verify_url_template = config.getString("verify_url_template");
		verify_body_template = config.getString("verify_body_template");

		verify_url = String.format(verify_url_template, host, realm);

		create_resource_template = config.getString("create_resource_template");
		delete_resource_template = config.getString("delete_resource_template");
		request_pat_body_template = config.getString("request_pat_body_template");
		exchange_token_body_template = config.getString("exchange_token_body_template");
		apply_resource_policy_template = config.getString("apply_resource_policy_template");
		request_pat_template = config.getString("request_pat_template");

		create_resource_url = String.format(create_resource_template, host, realm);
		request_pat_body = String.format(request_pat_body_template, client_id, client_secret);
		request_pat_url = String.format(request_pat_template, host, realm);
		Config admin_config = config.getConfig("admin");
		if(admin_config != null) {
			admin = new Admin(admin_config);
		}
	}

	/**
	 * 从KeyCloak验证token
	 * @param token
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public Pair<User, Date> verifyAccessToken(String token) throws URISyntaxException, IOException {

		String body = String.format(verify_body_template, client_id, client_secret, token);

		Task t = new Task(verify_url, HttpMethod.POST, body.getBytes());
		BasicRequester.req(t);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(t.r.rBody);

		// 获取用户id，获取token过期时间，获取用户当前系统权限
		// 保存token到UserCache，设定过期失效，有效期间本地验证即可
		Date exp = new Date(node.get("exp").asLong() * 1000);

		User u = new User();
		u.id = node.get("sub").asText();

		for(JsonNode roleNode : node.get("realm_access").get("roles")) {
			u.roles.add(User.Role.valueOf(roleNode.asText().toUpperCase()));
		}

		u.username = node.get("username").asText();
		u.email = node.get("email").asText();
		u.avatar_url = node.get("avatar_url").asText();
		u.enabled = node.get("enabled").asBoolean();

		return new ImmutablePair<>(u, exp);
	}

	/**
	 * 设置Bearer Authorization和 Content-Type请求头
	 * @param token
	 * @return
	 */
	private static Map<String, String> getHeaders(String token) {
		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Authorization", "bearer " + token);
		return headers;
	}

	/**
	 * 申请pat
	 */
	private String requestPat () throws URISyntaxException, IOException {

		Task t = new Task(request_pat_url, HttpMethod.POST, request_pat_body.getBytes());
		BasicRequester.req(t);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(t.r.rBody);

		return node.get("access_token").asText();
	}

	/**
	 * 创建Keycloak资源
	 */
	public void createResource (ContainerService.CreateResourceBody create_resource_body) throws URISyntaxException, IOException {
		String body = JSON.toJson(create_resource_body);
		Task t = new Task(create_resource_url, HttpMethod.POST, getHeaders(requestPat()), null, null, body.getBytes());
		BasicRequester.req(t);
	}

	/**
	 * 向Keycloak资源添加policy
	 */
	public void applyResourcePolicy (String access_token, String resource_id, ContainerService.ApplyResourcePolicyBody apply_resource_policy_body) throws URISyntaxException, IOException {
		apply_resource_policy_url = String.format(apply_resource_policy_template, host, realm, resource_id);
		String body = JSON.toJson(apply_resource_policy_body);
		Task t = new Task(apply_resource_policy_url, HttpMethod.POST, getHeaders(access_token), null, null, body.getBytes());
		BasicRequester.req(t);
	}

	/**
	 * 删除Keycloak资源
	 */
	public void deleteResource (String resource_id) throws IOException, URISyntaxException {
		delete_resource_url = String.format(delete_resource_template, host, realm, resource_id);
		Task t = new Task(delete_resource_url, HttpMethod.DELETE, getHeaders(requestPat()), null, null, null);
		BasicRequester.req(t);
	}

	/**
	 * 交换token
	 */
	public String exchangeToken (String front_token) throws URISyntaxException, IOException {

		exchange_token_body = String.format(exchange_token_body_template, client_id, client_secret, client_id, front_token);
		Task t = new Task(request_pat_url, HttpMethod.POST, exchange_token_body.getBytes());
		BasicRequester.req(t);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(t.r.rBody);

		return node.get("access_token").asText();
	}




	/**
	 * Admin用户行为
	 */
	public class Admin {

		private String admin = "admin";
		private String admin_password = "admin";

		public Map<String, String> roles = new HashMap<>();

		private String admin_url_template = "%s/auth/realms/master/protocol/openid-connect/token";
		private String admin_body_template = "username=%s&password=%s&grant_type=password&client_id=admin-cli";

		public Admin(Config admin_config) {

			admin_url_template = admin_config.getString("admin_url_template");
			admin_body_template = admin_config.getString("admin_body_template");

			for(Config c : admin_config.getConfigList("roles")) {
				roles.put(c.getString("name"), c.getString("name"));
			}
		}

		/**
		 *
		 * @return
		 * @throws URISyntaxException
		 * @throws IOException
		 */
		public String getAdminToken() throws URISyntaxException, IOException {

			String admin_url = String.format(admin_url_template, host);
			String admin_body = String.format(admin_body_template, admin, admin_password);

			Task t = new Task(admin_url, HttpMethod.POST, admin_body.getBytes());
			BasicRequester.req(t);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(t.r.rBody);
			return node.get("access_token").asText();
		}

		/**
		 *
		 * @param token
		 * @param user_representation
		 * @throws URISyntaxException
		 */
		public String addUser(String token, Map<String, Object> user_representation) throws URISyntaxException {

			String url = host + "/auth/admin/realms/" + realm + "/users";

			String body = JSON.toJson(user_representation);

			Task t = new Task(url, HttpMethod.POST, getHeaders(token), null, null, body.getBytes());
			BasicRequester.req(t);
			//System.err.println(t.r.getText());

			String uid = t.r.rHeaders.get("Location").get(0).replaceAll(".+?users/", "");

			return uid;
		}

		/**
		 *
		 * @param token
		 * @param uid
		 * @param role_id
		 * @param role_name
		 * @throws URISyntaxException
		 */
		public void addUserRealmRole(String token, String uid, String role_id, String role_name) throws URISyntaxException {

			String url = host + "/auth/admin/realms/" + realm + "/users/" + uid + "/role-mappings/realm";

			String body = JSON.toJson(Arrays.asList(ImmutableMap.of(
					"id", role_id,
					"name", role_name
			)));

			Task t = new Task(url, HttpMethod.POST, getHeaders(token), null, null, body.getBytes());
			BasicRequester.req(t);

			System.err.println(t.r.getText());
		}

		/**
		 *
		 * @param token
		 * @param uid
		 * @param role_id
		 * @param role_name
		 * @throws URISyntaxException
		 */
		public void removeUserRealmRole(String token, String uid, String role_id, String role_name) throws URISyntaxException {

			String url = host + "/auth/admin/realms/" + realm + "/users/" + uid + "/role-mappings/realm";

			String body = JSON.toJson(Arrays.asList(ImmutableMap.of(
					"id", role_id,
					"name", role_name
			)));

			Task t = new Task(url, HttpMethod.DELETE, getHeaders(token), null, null, body.getBytes());
			BasicRequester.req(t);
		}

		/**
		 *
		 * @param token
		 * @param uid
		 * @param user_representation
		 * @throws URISyntaxException
		 */
		public void updateUser(String token, String uid, Map<String, Object> user_representation) throws URISyntaxException {

			String url = host + "/admin/realms/" + realm + "/users/" + uid;

			String body = JSON.toJson(user_representation);

			Task t = new Task(url, HttpMethod.POST, getHeaders(token), null, null, body.getBytes());
			BasicRequester.req(t);
		}
	}
}
