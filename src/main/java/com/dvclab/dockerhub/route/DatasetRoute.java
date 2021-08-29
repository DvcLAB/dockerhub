package com.dvclab.dockerhub.route;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.dvclab.dockerhub.cache.Caches;
import com.dvclab.dockerhub.cache.HostCache;
import com.dvclab.dockerhub.model.*;
import com.dvclab.dockerhub.serialization.Msg;
import com.dvclab.dockerhub.util.PypiServerUpdater;
import com.dvclab.dockerhub.util.ResourceInfoFetcher;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.typesafe.config.Config;
import one.rewind.db.Daos;
import one.rewind.db.S3Adapter;
import one.rewind.nio.json.JSON;
import one.rewind.nio.persistence.Source;
import one.rewind.nio.tpl.Vars;
import one.rewind.nio.web.auth.KeycloakAdapter;
import one.rewind.nio.web.cache.UserCache;
import one.rewind.txt.StringUtil;
import one.rewind.util.Configs;
import org.apache.commons.io.IOUtils;
import spark.Route;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static one.rewind.db.model.Model.batchInsert;
import static one.rewind.db.model.Model.batchUpsert;

/**
 * 数据集路由
 */
public class DatasetRoute {

	/**
	 * 获取数据集列表
	 */
	public static Route listDatasets = (q, a) -> {

		String uid = q.session().attribute("uid");
		String query = q.queryParamOrDefault("q", "");
		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));
		Long size = Long.parseLong(q.queryParamOrDefault("size", "10"));

		try {

			Dao<Dataset, ?> dao = Daos.get(Dataset.class);
			QueryBuilder<Dataset, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("update_time", false);
			long total = dao.queryBuilder().countOf();

			qb.where().like("id", query + "%")
					.or().like("name", query + "%");

			List<Dataset> list = qb.query();

			// 返回结果补全 用户信息
			Map<String, User> users = User.getUsers(list.stream().map(c -> c.uid).collect(Collectors.toList()));
			list.stream().forEach(c -> c.user = users.get(c.uid));

			return Msg.success(list, size, page, total);
		}
		catch (Exception e) {

			Routes.logger.error("List Dataset error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 创建数据集
	 */
	public static Route createDataset = (q, a) -> {
		// 0 token验证
		String uid = q.session().attribute("uid");
		String keycloak_token = q.headers("AUTHORIZATION").replace("bearer", "");

		q.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

		// 1 解析前端传来的multipart类型数据， 数据集name、tags、isPrivate（text），头图、说明文件（file）
		String username = UserCache.userCache.USERS.get(uid).username;
		List<String> tags = Arrays.asList(q.queryParamsValues("tags"));
		String name = q.queryParamOrDefault("name", "");
		Dataset.Type type = Dataset.Type.valueOf(q.queryParamOrDefault("type", Dataset.Type.PUBLIC.name()));

		// 2 创建Dataset记录
		Dataset ds = new Dataset(name, null, type, tags, uid, username);

		String bucket_name = username + "_" + ds.name;
		String cover_img_name = ".cover_img.png";
		String rm_name = "README.md";

		// 3 创建bucket，user.username + "_" + dataset.name作为bucket_name
		try {
			S3Adapter.createBucketWithKeycloak(keycloak_token, bucket_name);
		}
		// S3权限问题、服务问题
		catch (Throwable e) {

			Routes.logger.error("Create Bucket Error, {}", bucket_name, e);
			return Msg.failure(e);
		}

		// 4 将头图（.cover_image.png）、说明文件（README.md）保存至bucket中
		try {
			// 4.1 上传头图到bucket
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(q.raw().getPart(cover_img_name).getSize());
			metadata.setContentType(q.raw().getPart(cover_img_name).getContentType());

			Config config = Configs.getConfig(KeycloakAdapter.class);
			String s3_name = config.getString("s3_admin");
			S3Adapter.get(s3_name).s3.putObject(bucket_name, cover_img_name, q.raw().getPart(cover_img_name).getInputStream(), metadata);

			// 4.2 上传README.md文件到bucket
			ObjectMetadata metadata_rm = new ObjectMetadata();
			metadata_rm.setContentLength(q.raw().getPart(rm_name).getSize());
			metadata_rm.setContentType(q.raw().getPart(rm_name).getContentType());

			S3Adapter.get(s3_name).s3.putObject(bucket_name, rm_name, q.raw().getPart(rm_name).getInputStream(), metadata);
		}
		// 文件上传报错
		catch (Throwable e) {

			Routes.logger.error("Bucket[{}] upload file error, ", bucket_name, e);
			return Msg.failure(e);
		}

		// 5 更新数据集url和头图url
		try {
			ds.url = "https://s3.33.dvc/" + bucket_name;
			ds.cover_img_url = ds.url + "/.cover_img.png";
			if(ds.insert()) {
				return Msg.success();
			}
			else {
				return Msg.failure();
			}
		}
		// id冲突、sql语句报错
		catch (Exception e) {

			Routes.logger.error("Create Dataset[{}] error, ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取数据集
	 */
	public static Route getDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {

			Dataset ds = Dataset.getById(Dataset.class, id);
			if(ds != null) {

				// 补全数据集用户信息
				ds.user = User.getById(User.class, ds.uid);

				return Msg.success(ds);
			}
			else {
				return new Msg(Msg.Code.NOT_FOUND, null, null);
			}
		}
		catch (Exception e) {

			Routes.logger.error("Get Dataset[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 更新数据集
	 * Note: 数据集URL不能修改
	 */
	public static Route updateDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String source = q.body();

		try {

			Dataset ds = JSON.fromJson(source, Dataset.class);
			ds.genId();
			ds.uid = uid;
			if(!ds.id.equals(id)) throw new Exception("Dataset url can not be changed");

			if(ds.update()) {
				return Msg.success(ds);
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
	 * 删除数据集
	 */
	public static Route deleteDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String keycloak_token = q.headers("AUTHORIZATION").replace("bearer", "");

		// 只有管理员才能删除记录    或     私有数据集的owner删除自己的数据集
		Dataset ds = Dataset.getById(Dataset.class, id);
		if(!Caches.userCache.USERS.get(uid).hasRole(User.Role.DOCKHUB_ADMIN) && !(ds.type.equals(Dataset.Type.PRIVATE) && ds.uid.equals(uid))) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

		try {

			//删除数据集的同时，删除对应的bucket
			S3Adapter.delBucketWithKeycloak(keycloak_token, ds.id);
			Dataset.deleteById(Dataset.class, id);
			return Msg.success();
		}
		catch (Exception e) {

			Routes.logger.error("Delete Dataset[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 通过url获取数据集信息
	 */
	public static Route getInfo = (q, a) -> {

		String uid = q.session().attribute("uid");
		String url = q.queryParamOrDefault("url", "");

		if(url.length() == 0) return Msg.failure("Null URL");
		if(!url.matches("^https://s3.dvclab.com/.*$")) return new Msg(Msg.Code.BAD_REQUEST, null, null);
		url = URLDecoder.decode(url, StandardCharsets.UTF_8);

		try {
			return Msg.success(ResourceInfoFetcher.getDatasetInfo(url));
		}
		catch (Exception e) {
			Routes.logger.error("Unable get Dataset info, url[{}], ", url, e);
			return Msg.failure(e);
		}
	};


	/**
	 * 数据集添加成员
	 * 只有数据集的owner可以添加成员
	 */
	public static Route addMembers = (q, a) -> {
		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String mid = q.queryParams("mid");

		// 只有数据集的owner可以添加成员
		Dataset ds = Dataset.getById(Dataset.class, id);
		if(!ds.uid.equals(uid)) return Msg.failure(Msg.Code.ACCESS_DENIED);

		try {
			Member memberById = Member.getById(Member.class, StringUtil.md5(id + "::" + mid));

			// 数据集成员用户不存在，添加
			if(memberById == null){
				Member member = new Member(id, mid);
				member.status = Member.Status.Normal;
				if(member.insert()){
					return Msg.success();
				}else{
					return Msg.failure();
				}
			}
			// 数据集成员用户状态为Deleted，更新为Normal
			else if(memberById.status.equals(Member.Status.Deleted)){
				memberById.status = Member.Status.Normal;
				if(memberById.update()){
					return Msg.success();
				}else{
					return Msg.failure();
				}
			}
			// 数据集成员用户状态为Normal
			else{
				return Msg.failure();
			}

		}
		catch (Exception e){
			Routes.logger.error("Add Member[{}] error, ", mid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 剔除数据集成员，将其状态设置为Deleted
	 * 只有owner可以剔除数据集成员
	 */
	public static Route delMembers = (q, a) -> {
		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String mid = q.queryParams("mid");

		// 只有数据集的owner可以剔除成员
		Dataset ds = Dataset.getById(Dataset.class, id);
		if(!ds.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

		try {
			Member member = Member.getById(Member.class, StringUtil.md5(id + "::" + mid));
			member.status = Member.Status.Deleted;
			if(member.update()){
				return Msg.success();
			}else{
				return Msg.failure();
			}

		}
		catch (Exception e) {
			Routes.logger.error("Delete Member[{}] error, ", mid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 获取某一数据集的成员列表
	 */
	public static Route listMembers = (q, a) -> {
		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		Long page = Long.parseLong(q.queryParamOrDefault("page", "1"));
		Long size = Long.parseLong(q.queryParamOrDefault("size", "10"));

		try {
			Dao<Member, Object> dao = Daos.get(Member.class);
			QueryBuilder<Member, ?> qb = dao.queryBuilder()
					.offset((page-1)*size).limit(size).orderBy("update_time", false);
			long total = dao.queryBuilder().countOf();
			
			List<Member> members = qb.where().eq("did", id).and().eq("status", Member.Status.Normal).query();

			// 补全用户信息
			Map<String, User> users = User.getUsers(members.stream().map(m -> m.uid).collect(Collectors.toList()));
			members.stream().forEach(m -> m.user = users.get(m.uid));

			return Msg.success(members, size, page, total);
		}
		catch (Exception e) {
			Routes.logger.error("List Host error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 用户主动退出，将其状态设置为Exit
	 */
	public static Route exitMember = (q, a) -> {
		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String mid = q.queryParams("mid");

		// 用户自己退出
		Member member = Member.getById(Member.class, StringUtil.md5(id + "::" + mid));
		if(member.uid.equals(uid)) return new Msg(Msg.Code.ACCESS_DENIED, null, null);

		try {
			member.status = Member.Status.Deleted;
			if (member.update()){
				return Msg.success();
			} else {
				return Msg.failure();
			}
		}
		catch (Exception e) {
			Routes.logger.error("Member Exit error, uid[{}], ", uid, e);
			return Msg.failure(e);
		}
	};
}
