package com.dvclab.dockerhub.serialization;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.db.model.ModelD;
import one.rewind.db.persister.JSONableMapPersister;

import java.util.HashMap;
import java.util.Map;

public class ServiceMsg extends ModelD {

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String type;

	@DatabaseField(persisterClass = JSONableMapPersister.class, columnDefinition = "TEXT")
	public Map<String, ?> data = new HashMap<>();

	public ServiceMsg() {}
}
