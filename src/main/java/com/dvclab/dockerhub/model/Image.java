package com.dvclab.dockerhub.model;

import com.dvclab.dockerhub.serialization.EnumListPersister;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;
import one.rewind.db.persister.JSONableListPersister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String name;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String cover_img_url;

	@DatabaseField(persisterClass = EnumListPersister.class, columnDefinition = "TEXT", width = 16)
	public List<Type> types = new ArrayList<>();

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String framework;

	public Image() {}

	/**
	 *
	 * @param name
	 * @param framework
	 * @param types
	 */
	public Image(String name, String framework, Type... types) {
		this.id = name;
		this.name = name;
		this.types = Arrays.asList(types);
		this.framework = framework;
	}
}
