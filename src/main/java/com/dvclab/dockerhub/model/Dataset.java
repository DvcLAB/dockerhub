package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.Daos;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.ModelD;
import one.rewind.nio.json.persister.JSONAbleFieldPersister;
import one.rewind.txt.StringUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@DBName("dockerhub")
@DatabaseTable(tableName = "datasets")
public class Dataset extends ModelD {
	public enum Type {
		PUBLIC,
		PRIVATE
	}
	//是否是私有数据集 true私有
	@DatabaseField(dataType = DataType.ENUM_STRING, canBeNull = false)
	public Type type;

	@DatabaseField(dataType = DataType.STRING, width = 64, index = true)
	public String uid;

	public User user;

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String url;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String cover_img_url;

	@DatabaseField(dataType = DataType.STRING, width = 2048)
	public String desc;
	// 数据集标签
	@DatabaseField(persisterClass = JSONAbleFieldPersister.class, columnDefinition = "TEXT")
	public List<String> tags = new ArrayList<>();

	public Dataset() {}

	/**
	 *
	 * @param name
	 * @param url
	 * @param desc
	 */
	public Dataset(String name, String url, String desc, String cover_img_url) {
		this.name = name;
		this.url = url;
		this.desc = desc;
		this.cover_img_url = cover_img_url;
		this.id = StringUtil.md5(this.url);
		this.genId();
	}

	/**
	 *
	 * @param name
	 * @param desc
	 * @param type
	 * @param tags
	 * @param uid
	 * @param username
	 */
	public Dataset(String name, String desc, Type type, List<String> tags, String uid, String username) {
		this.name = name;
		this.desc = desc;
		this.tags = tags;
		this.type = type;
		this.uid = uid;
		this.id = genId(username, name);
	}

	public static String genId(String username, String name){
		return StringUtil.md5(username + name);
	}

	/**
	 * 使用数据集url生成数据集ID
	 * @return
	 */
	public Dataset genId() {
		id = StringUtil.md5(url);
		return this;
	}

	/**
	 * 获取一组数据集url对应的数据集列表
	 * @param urls
	 * @return
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static List<Dataset> getByUrlList(List<String> urls) throws DBInitException, SQLException {

		return Daos.get(Dataset.class).queryBuilder().where()
				.in(
					"id",
					urls.stream().map(StringUtil::md5).collect(Collectors.toList())
				)
				.query();
	}

	/**
	 * 获取一组数据集id对应的数据集列表
	 * @param ids
	 * @return
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static List<Dataset> getByIdlList(List<String> ids) throws DBInitException, SQLException {

		return Daos.get(Dataset.class).queryBuilder().where()
				.in(
						"id",
						ids
				)
				.query();
	}
}
