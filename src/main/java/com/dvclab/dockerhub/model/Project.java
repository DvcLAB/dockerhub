package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.ModelD;
import one.rewind.db.persister.JSONableListPersister;
import one.rewind.db.persister.JSONableMapPersister;
import one.rewind.db.persister.JSONablePersister;
import one.rewind.txt.StringUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
@DBName("dockerhub")
@DatabaseTable(tableName = "projects")
public class Project extends ModelD {

	@DatabaseField(dataType = DataType.STRING, width = 64, index = true)
	public String uid;

	public User user;

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String framework;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String url;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String cover_img_url = "";

	@DatabaseField(persisterClass = JSONableListPersister.class, columnDefinition = "TEXT")
	public List<String> branches = new ArrayList<>();
	// 项目依赖
	@DatabaseField(persisterClass = JSONableMapPersister.class, columnDefinition = "TEXT")
	public Map<String, String> deps = new HashMap<>();

	@DatabaseField(dataType = DataType.STRING, width = 2048)
	public String desc;
	// 项目数据集的ID列表
	@DatabaseField(persisterClass = JSONableListPersister.class, columnDefinition = "TEXT")
	public List<String> dataset_ids = new ArrayList<>();

	public List<Dataset> datasets;

	public Project() {}

	/**
	 *
	 * @param name
	 * @param url
	 * @param desc
	 */
	public Project(String name, String url, String desc, String cover_img_url,
				   List<String> branches, List<String> dataset_urls, Map<String, String> deps) throws DBInitException, SQLException {

		this.name = name;
		this.url = url;
		this.desc = desc;
		this.cover_img_url = cover_img_url;
		this.branches = branches;
		this.deps = deps;
		this.genId();

		if(! dataset_urls.isEmpty()) {
			this.datasets = Dataset.getByUrlList(dataset_urls);
			this.dataset_ids = this.datasets.stream().map(ds -> ds.id).collect(Collectors.toList());
		}
	}

	/**
	 * 生成项目ID
	 * @return
	 */
	public Project genId() {
		id = StringUtil.md5(url);
		return this;
	}
}
