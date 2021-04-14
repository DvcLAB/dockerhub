package com.dvclab.dockerhub.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.rewind.json.JSON;
import spark.ResponseTransformer;

import java.text.DateFormat;
import java.util.Date;

/**
 * 使用Gson构建的json序列化器
 */
public class JsonTransformer implements ResponseTransformer {

	private Gson gson = new GsonBuilder()
		.registerTypeAdapter(Date.class, new JSON.DateSerializer())
		.setDateFormat(DateFormat.LONG)
		.create();

	@Override
	public String render(Object model) {
		return gson.toJson(model);
	}
}
