package com.dvclab.dockerhub.model;

import com.dvclab.dockerhub.serialization.EnumListPersister;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;
import one.rewind.db.persister.JSONableListPersister;
import one.rewind.txt.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@DBName("dockerhub")
@DatabaseTable(tableName = "images")
public class Image extends ModelD {

	public static enum Type {
		CPU,
		GPU
	}

	@DatabaseField(dataType = DataType.STRING, width = 64, index = true)
	public String uid;

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String cover_img_url;

	@DatabaseField(persisterClass = EnumListPersister.class, columnDefinition = "TEXT", width = 16)
	public List<Type> types = new ArrayList<>();

	@DatabaseField(dataType = DataType.STRING, width = 4096)
	public String desc;

	public Image() {}

	/**
	 *
	 * @param name
	 * @param desc
	 * @param types
	 */
	public Image(String name, String desc, Type... types) {

		this.name = name;
		this.types = Arrays.asList(types);
		this.desc = desc;
		this.genId();
	}

	public String getName() {
		return this.name;
	}

	/**
	 *
	 * @return
	 */
	public Image genId() {
		id = StringUtil.md5(name);
		return this;
	}
}
