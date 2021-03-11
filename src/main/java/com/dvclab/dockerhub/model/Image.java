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

	@DatabaseField(dataType = DataType.STRING, width = 128, indexName = "n-f")
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String cover_img_url;

	@DatabaseField(persisterClass = EnumListPersister.class, columnDefinition = "TEXT", width = 16)
	public List<Type> types = new ArrayList<>();

	@DatabaseField(dataType = DataType.STRING, width = 256, indexName = "n-f")
	public String framework;

	public Image() {}

	/**
	 *
	 * @param name
	 * @param framework
	 * @param types
	 */
	public Image(String name, String framework, Type... types) {

		this.name = name;
		this.types = Arrays.asList(types);
		this.framework = framework;
		this.genId();
	}

	/**
	 *
	 * @return
	 */
	public Image genId() {
		id = StringUtil.md5(name + "::" + framework + "::"
				+ this.types.stream().map(t->t.name()).collect(Collectors.joining(",")));
		return this;
	}
}
