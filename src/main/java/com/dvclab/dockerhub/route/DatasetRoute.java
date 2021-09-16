package com.dvclab.dockerhub.route;

import com.amazonaws.services.s3.model.CannedAccessControlList;
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
import one.rewind.nio.http.Requester;
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
					.or().like("name", query + "%")
					.or().like("uid", query + "%")
					.or().raw("tags LIKE '%" + query + "%'");

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
		String keycloak_token = q.headers("AUTHORIZATION").replace("Bearer ", "");

		q.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

		// 1 解析前端传来的multipart类型数据， 数据集name、tags、isPrivate（text），头图、说明文件（file）
		String username = User.getById(User.class, uid).username;
		List<String> tags = Arrays.asList(q.queryParamsValues("tag"));
		String name = q.queryParams("name");
		Dataset.Type type = Dataset.Type.valueOf(q.queryParamOrDefault("type", Dataset.Type.PUBLIC.name()));

		// 2 创建Dataset记录
		Dataset ds = new Dataset(name, null, type, tags, uid, username);

		String bucket_name = ds.id;
		String cover_img_name = ".cover_img.png";
		String rm_name = "README.md";

		// 3 创建bucket
		try {
			S3Adapter s3_admin = S3Adapter.get("dvclab");
			S3Adapter.createBucketWithKeycloak(keycloak_token, bucket_name, s3_admin);
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

			S3Adapter.get(s3_name).s3.putObject(bucket_name, rm_name, q.raw().getPart(rm_name).getInputStream(), metadata_rm);
			// 4.3 将头图和README.md文件设为公开可读
			S3Adapter.get(s3_name).s3.setObjectAcl(bucket_name, cover_img_name, CannedAccessControlList.PublicRead);
			S3Adapter.get(s3_name).s3.setObjectAcl(bucket_name, rm_name, CannedAccessControlList.PublicRead);
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
			String desc = Requester.req(ds.url + "/README.md").get().getText();
			ds.desc = desc;
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
	 * 公有数据集都可获取数据集信息
	 * 私有数据集只有owner和成员可获取信息
	 */
	public static Route getDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");

		try {
			Member member = Member.getById(Member.class, Member.genId(id, uid));
			Dataset ds = Dataset.getById(Dataset.class, id);

			if(ds.type == Dataset.Type.PRIVATE && (!ds.uid.equals(uid) && member ==null)) return Msg.failure(Msg.Code.ACCESS_DENIED);

			// 补全数据集用户信息
			ds.user = User.getById(User.class, ds.uid);

			return Msg.success(ds);
		}
		catch (Exception e) {

			Routes.logger.error("Get Dataset[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 更新数据集
	 * 仅更改名称，标签和是否为私有
	 * 如果将私有数据集改为public，删除私有数据集原有的成员
	 */
	public static Route updateDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		Dataset ds = Dataset.getById(Dataset.class, id);
		String name = q.queryParamOrDefault("name", ds.name);
		List<String> tags = Arrays.asList(q.queryParamsValues("tag"));
		Dataset.Type type = Dataset.Type.valueOf(q.queryParamOrDefault("type", ds.type.name()));
		String desc = q.queryParamOrDefault("desc", "");

		try {
			// 1 只有数据集的owner可以更改数据集信息
			if(!ds.uid.equals(uid)) return Msg.failure(Msg.Code.ACCESS_DENIED);

			// 2 如果将私有数据集改为public，删除私有数据集原有的成员
			if(ds.type == Dataset.Type.PRIVATE && type == Dataset.Type.PUBLIC) {
				List<Member> members = Daos.get(Member.class).queryBuilder().where().eq("did", ds.id).query();
				Collection<Object> member_ids = members.stream().map(m -> m.id).collect(Collectors.toList());
				Daos.get(Member.class).deleteIds(member_ids);
			}

			// 3 修改数据集的desc，并更新README.md
			if(!desc.equals("")) {
				Config config = Configs.getConfig(KeycloakAdapter.class);
				String s3_name = config.getString("s3_admin");
				S3Adapter.get(s3_name).s3.putObject(ds.id, "README.md", desc);
				ds.desc = desc;
			}

			ds.name = name;
			ds.tags = tags;
			ds.type = type;
			// 3 更新数据集
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
	 * 管理员或数据集的owner可删除数据集
	 */
	public static Route deleteDataset = (q, a) -> {

		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String keycloak_token = q.headers("AUTHORIZATION").replace("Bearer ", "");

		// 管理员或数据集的owner可删除数据集
		Dataset ds = Dataset.getById(Dataset.class, id);
		if(!Caches.userCache.USERS.get(uid).hasRole(User.Role.DOCKHUB_ADMIN) && !ds.uid.equals(uid))
			return new Msg(Msg.Code.ACCESS_DENIED, null, null);

		try {

			// 删除数据集的同时，删除对应的bucket
			S3Adapter s3_admin = S3Adapter.get("dvclab");
			S3Adapter.delBucketWithKeycloak(keycloak_token, ds.id, s3_admin);
			// 如果数据集为私有数据集，删除members表中对应的成员
			if(ds.type == Dataset.Type.PRIVATE) {
				List<Member> members = Daos.get(Member.class).queryBuilder().where().eq("did", ds.id).query();
				Collection<Object> member_ids = members.stream().map(m -> m.id).collect(Collectors.toList());
				Daos.get(Member.class).deleteIds(member_ids);
			}
			Dataset.deleteById(Dataset.class, id);
			return Msg.success();
		}
		catch (Throwable e) {

			Routes.logger.error("Delete Dataset[{}] error, ", id, e);
			return Msg.failure(e);
		}
	};


	/**
	 * 私有数据集
	 * 数据集添加成员
	 * 数据集的owner可以添加成员为Admin或Viewer
	 * 数据集的Admin可以添加成员为Viewer
	 */
	public static Route addMember = (q, a) -> {
		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String mid = q.queryParams("mid");
		Member.Roles role = Member.Roles.valueOf(q.queryParamOrDefault("role", Member.Roles.Viewer.name()));

		// 1 私有数据集  数据集的owner可以添加成员为Admin或Viewer，数据集的Admin可以添加成员为Viewer
		Dataset ds = Dataset.getById(Dataset.class, id);
		if(ds.type == Dataset.Type.PUBLIC) return Msg.failure(Msg.Code.BAD_REQUEST);
		Member memberById = Member.getById(Member.class, Member.genId(id, mid));
		if(!ds.uid.equals(uid) && !(memberById.role == Member.Roles.Admin && role == Member.Roles.Viewer))
			return Msg.failure(Msg.Code.ACCESS_DENIED);

		try {
			// 2 判断是否是操作者添加的自己
			if(ds.uid.equals(mid)) return Msg.failure(Msg.Code.INVALID_PARAMETERS);

			// 3 数据集成员用户不存在，添加
			if(memberById == null){
				Member member = new Member(id, mid);
				member.status = Member.Status.Normal;
				member.role = role;
				if(member.insert()){
					return Msg.success();
				} else {
					return Msg.failure(Msg.Code.INSERT_FAILURE);
				}
			}
			// 4 数据集成员用户状态为Deleted，更新为Normal
			else if(memberById.status == Member.Status.Deleted) {
				memberById.status = Member.Status.Normal;
				memberById.role = role;
				if(memberById.update()){
					return Msg.success();
				} else {
					return Msg.failure(Msg.Code.UPDATE_FAILURE);
				}
			}
			// 5 数据集成员用户状态为Normal
			else {
				return Msg.failure();
			}

		}
		catch (Exception e){
			Routes.logger.error("Add Member[{}] error, ", mid, e);
			return Msg.failure(e);
		}
	};

	/**
	 * 私有数据集
	 * 剔除数据集成员或用户主动退出，将其状态设置为Deleted
	 * owner可以剔除role为Admin或Viewer的数据集成员
	 * Admin可以剔除role为Viewer的数据集成员
	 */
	public static Route delMember = (q, a) -> {
		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String mid = q.queryParams("mid");

		// 私有数据集 数据集的owner可以剔除所有成员，数据集的Admin可以剔除Viewer，用户主动退出
		Dataset ds = Dataset.getById(Dataset.class, id);
		if(ds.type == Dataset.Type.PUBLIC) return Msg.failure(Msg.Code.BAD_REQUEST);
		Member member_operator = Member.getById(Member.class, Member.genId(id, uid));
		Member member = Member.getById(Member.class, Member.genId(id, mid));
		if(!ds.uid.equals(uid) && !member.uid.equals(uid) && !(member_operator.role == Member.Roles.Admin && member.role == Member.Roles.Viewer))
			return Msg.failure(Msg.Code.ACCESS_DENIED);

		try {
			if(member == null) return Msg.success();

			member.status = Member.Status.Deleted;
			if(member.update()){
				return Msg.success();
			} else {
				return Msg.failure(Msg.Code.UPDATE_FAILURE);
			}

		}
		catch (Exception e) {
			Routes.logger.error("Delete or Exited Member[{}] error, ", mid, e);
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
	 * 更改数据集成员的权限
	 */
	public static Route updateMember = (q, a) -> {
		String uid = q.session().attribute("uid");
		String id = q.params(":id");
		String mid = q.queryParams("mid");
		Member.Roles role = Member.Roles.valueOf(q.queryParams("role"));

		try {
			Dataset ds = Dataset.getById(Dataset.class, id);

			// 如果此操作者为数据集owner，可将数据集成员的role设置为Admin或Viewer
			if(!ds.uid.equals(uid)) return Msg.failure(Msg.Code.ACCESS_DENIED);

			Member member = Member.getById(Member.class, Member.genId(id, mid));
			member.role = role;
			if(member.update()) {
				return Msg.success();
			} else {
				return Msg.failure();
			}
		}
		catch (Exception e) {
			Routes.logger.error("Update Member error, mid[{}], ", mid, e);
			return Msg.failure(e);
		}
	};
}
