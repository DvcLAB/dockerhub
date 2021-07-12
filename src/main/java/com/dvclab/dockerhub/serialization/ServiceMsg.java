package com.dvclab.dockerhub.serialization;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.db.model.ModelD;
import one.rewind.nio.json.persister.JSONAbleFieldPersister;

import java.util.HashMap;
import java.util.Map;

/**
 * kafka消息的结构定义
 */
public class ServiceMsg extends ModelD {

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String type;

	@DatabaseField(persisterClass = JSONAbleFieldPersister.class, columnDefinition = "TEXT")
	public Map<String, ?> data = new HashMap<>();

	public ServiceMsg() {}
}
