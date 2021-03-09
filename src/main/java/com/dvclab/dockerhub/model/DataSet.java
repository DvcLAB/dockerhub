package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;

@DBName("dockerhub")
@DatabaseTable(tableName = "datasets")
public class DataSet extends ModelD {

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String url;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String cover_img_url;

	@DatabaseField(dataType = DataType.STRING, width = 64, index = true)
	public String uid;

	@DatabaseField(dataType = DataType.STRING, width = 2048)
	public String desc;

	public DataSet() {}

	/**
	 *
	 * @param name
	 * @param url
	 * @param desc
	 */
	public DataSet(String name, String url, String desc) {
		this.name = name;
		this.url = url;
		this.desc = desc;
	}
}
