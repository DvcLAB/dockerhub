package com.dvclab.dockerhub.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import one.rewind.json.JSON;

import java.util.List;

/**
 * List类型的自定义序列化器
 */
public class EnumListPersister extends StringType {

	private static final EnumListPersister INSTANCE = new EnumListPersister();

	protected EnumListPersister() {
		super(SqlType.STRING, new Class[]{List.class, Enum.class});
	}

	public static EnumListPersister getSingleton() {
		return INSTANCE;
	}

	public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
		List list = (List)javaObject;
		return list != null ? JSON.toJson(list) : null;
	}

	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
		List list = (List)JSON.fromJson((String)sqlArg, List.class);
		return sqlArg != null ? list : null;
	}
}
