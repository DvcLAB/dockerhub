package com.dvclab.dockerhub.model;

import com.dvclab.dockerhub.serialization.EnumListPersister;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.Daos;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.ModelD;
import one.rewind.db.persister.JSONableListPersister;
import one.rewind.txt.StringUtil;

import java.sql.SQLException;
import java.util.*;
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

	public User user;

	@DatabaseField(dataType = DataType.STRING, width = 128, index = true)
	public String name;

	@DatabaseField(persisterClass = JSONableListPersister.class, columnDefinition = "TEXT", width = 16)
	public List<String> tags = new ArrayList<>();

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

	/**
	 * 根据镜像id列表获取镜像列表
	 * @return
	 */
	public static Map<String, Image> getImages(List<String> image_ids) throws DBInitException, SQLException {

		if(image_ids.size() == 0) {
			return new HashMap<>();
		}

		return Daos.get(Image.class).queryBuilder()
				.where().in("id", image_ids).query()
				.stream().collect(Collectors.toMap(Image::getId, image -> image));
	}
}
