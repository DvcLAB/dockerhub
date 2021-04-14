package com.dvclab.dockerhub.route;

import com.dvclab.dockerhub.cache.ImageCache;
import com.dvclab.dockerhub.cache.UserCache;
import com.dvclab.dockerhub.model.Dataset;
import com.dvclab.dockerhub.model.Image;
import com.dvclab.dockerhub.model.Project;
import com.dvclab.dockerhub.model.User;
import com.dvclab.dockerhub.serialization.Msg;
import com.dvclab.dockerhub.util.ResourceInfoFetcher;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import io.netty.handler.codec.http.HttpMethod;
import one.rewind.db.Daos;
import one.rewind.db.model.ModelD;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.task.Task;
import one.rewind.txt.StringUtil;
import org.apache.http.HttpHeaders;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import spark.Route;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class ImageRoute {

	static String docker_registry_addr = "registry.dvclab.com";

	/**
	 * 获取镜像列表
	 */
	public static Route listImages = (q, a) -> {

		String uid = q.session().attribute("uid");

		// 上一页最后一个 repository name
		String last = q.queryParamOrDefault("last", "");
		// 返回结果最大数量
		Long n = Long.parseLong(q.queryParamOrDefault("n", "10"));

		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));

		/**
		 * https://docs.docker.com/registry/spec/api/#catalog
		 * https://github.com/distribution/distribution/blob/main/docs/spec/api.md
		 */
		String url = "https://" + docker_registry_addr + "/v2/_catalog?n=" + n + "&last=" + last;
		String scope = "registry:catalog:*";

		try {

			List<String> repo_names = new ArrayList<>();

			// 代理请求
			Task t = new Task(url, HttpMethod.GET,
					Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + ResourceInfoFetcher.getDockerAuthToken(scope)), null, null, null);
			BasicRequester.req(t);

			// 解析docker_registry返回结果
			JSONObject res = new JSONObject(t.r.getText());
			JSONArray repos = res.getJSONArray("repositories");

			for(int i=0; i<repos.length(); i++) {
				repo_names.add(repos.getString(i));
			}

			// 数据库查询
			Dao<Image, ?> dao = Daos.get(Image.class);

			List<Image> images = dao.queryBuilder().where().in("name", repo_names).query();

			// 数据库中不存在的镜像 --> 增加新记录
			repo_names.removeAll(images.stream().map(img->img.name).collect(Collectors.toList()));
			List<Image> new_images = repo_names.stream()
					.map(name -> {
						Image image = new Image(name, "");
						// 同步镜像缓存
						ImageCache.images.put(image.name, image);
						return image;
					})
					.collect(Collectors.toList());
			ModelD.batchInsertIgnore(new_images);

			// 合并结果，并排序
			images.addAll(new_images);
			images.sort(Comparator.comparing(Image::getName));

			// 补全用户信息
			Map<String, User> users = User.getUsers(images.stream().filter(i -> i.uid != null).map(i -> i.uid)
					.collect(Collectors.toList()));
			images.stream().forEach(i -> i.user = users.get(i.uid));

			long total = dao.queryBuilder().countOf();

			// 返回结果
			return Msg.success(images, n, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("List Image error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取镜像tag列表
	 */
	public static Route listImageTags = (q, a) -> {

		String uid = q.session().attribute("uid");
		String name = q.params(":name");

		/**
		 * https://docs.docker.com/registry/spec/api/#catalog
		 * https://github.com/distribution/distribution/blob/main/docs/spec/api.md
		 */
		String url = "https://" + docker_registry_addr + "/v2/" + name + "/tags/list";
		String scope = "repository:" + name + ":pull";

		try {

			List<String> tags = new ArrayList<>();

			// 代理请求
			Task t = new Task(url, HttpMethod.GET, Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + ResourceInfoFetcher.getDockerAuthToken(scope)), null, null, null);
			BasicRequester.req(t);

			// 解析docker_registry返回结果
			JSONObject res = new JSONObject(t.r.getText());
			JSONArray tags_ = res.getJSONArray("tags");

			for(int i=0; i<tags_.length(); i++) {
				tags.add(tags_.getString(i));
			}

			// 同步标签信息到缓存
			Optional.ofNullable(ImageCache.images.get(name)).ifPresent(image -> {
				image.tags = tags;
			});

			// 返回结果
			return Msg.success(tags);
		}
		catch (Exception e) {

			Routes.logger.error("List Image error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 *
	 */
	public static Route getImagesForProject = (q, a) -> {

		String uid = q.session().attribute("uid");
		String pid = q.queryParamOrDefault("project_id", "");

		try {

			Project p = Project.getById(Project.class, pid);

			if(p != null) {
				return Msg.success(ImageCache.getImagesForProject(p));
			}
			else {
				return new Msg(Msg.Code.NOT_FOUND, null, null);
			}
		}
		catch (Exception e) {

			Routes.logger.error("Get Project[{}] images error, ", pid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取镜像
	 */
	public static Route getImage = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Image obj = Image.getById(Image.class, id);

			if(obj != null) {

				// 返回结果补全 用户信息
				obj.user = User.getById(User.class, obj.uid);
				return Msg.success(obj);
			}
			else {
				return new Msg(Msg.Code.NOT_FOUND, null, null);
			}
		}
		catch (Exception e) {

			Routes.logger.error("Get Image[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 更新镜像，用户认领镜像
	 */
	public static Route updateImage = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String source = q.body();

		// 只有管理员才可以更新镜像
		if(! UserCache.USERS.get(uid).roles.contains(User.Role.DOCKHUB_ADMIN)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

		try {

			Image obj = Image.fromJSON(source, Image.class);
			obj.genId();

			// 认领镜像用户
			obj.uid = uid;
			obj.user = UserCache.USERS.get(uid);

			if(!obj.id.equals(id)) throw new Exception("Dataset url can not be changed");
			if(obj.update()) {
				ImageCache.images.put(obj.name, obj);
				return Msg.success(obj);
			}
			else {
				return Msg.failure();
			}
		}
		catch (Exception e) {

			Routes.logger.error("Update Dataset[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};


	/**
	 *
	 */
	public static Route deleteImage = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		// 只有管理员才能删除记录
		if(! UserCache.USERS.get(uid).roles.contains(User.Role.DOCKHUB_ADMIN)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

		try {

			Image.deleteById(Image.class, id);
			return Msg.success();
		}
		catch (Exception e) {

			Routes.logger.error("Delete Image[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};
}
