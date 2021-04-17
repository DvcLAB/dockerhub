package com.dvclab.dockerhub.serialization;

import com.dvclab.dockerhub.model.EnumType;
import com.dvclab.dockerhub.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;
import one.rewind.json.JSON;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * List类型的自定义序列化器
 */
public class EnumListPersister extends StringType {

	private static final EnumListPersister INSTANCE = new EnumListPersister();
	private final static Gson gson = new GsonBuilder()
			.registerTypeAdapter(EnumType.class, new InterfaceAdapter<EnumType>())
			.create();

	protected EnumListPersister() {
		super(SqlType.STRING, new Class[]{List.class, EnumType.class});
	}

	public static EnumListPersister getSingleton() {
		return INSTANCE;
	}

	public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
		List list = (List)javaObject;
		return list != null ? gson.toJson(list) : null;
	}

	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
		List list = (List)gson.fromJson((String)sqlArg, List.class);
		return sqlArg != null ? list : null;
	}
}
