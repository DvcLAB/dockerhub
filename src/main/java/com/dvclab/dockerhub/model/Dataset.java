package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.Daos;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.ModelD;
import one.rewind.txt.StringUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@DBName("dockerhub")
@DatabaseTable(tableName = "datasets")
public class Dataset extends ModelD {

	@DatabaseField(dataType = DataType.STRING, width = 64, index = true)
	public String uid;

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String url;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String cover_img_url;

	@DatabaseField(dataType = DataType.STRING, width = 2048)
	public String desc;

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

	public Dataset genId() {
		id = StringUtil.md5(url);
		return this;
	}

	/**
	 *
	 * @param urls
	 * @return
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static List<Dataset> getByUrlList(List<String> urls) throws DBInitException, SQLException {

		return Daos.get(Dataset.class).queryBuilder().where()
				.in(
					"url",
					urls.stream().map(StringUtil::md5).collect(Collectors.toList())
				)
				.query();
	}
}
