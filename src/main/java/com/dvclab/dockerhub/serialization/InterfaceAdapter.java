package com.dvclab.dockerhub.serialization;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * @ClassName InterfaceAdapter
 * @Desc
 * @Author jialee812@gmail.com
 * @Date 2021/4/17 4:24 下午
 * @Version 1.0
 */
public class InterfaceAdapter<T> implements JsonSerializer, JsonDeserializer {

    private static final String CLASSNAME = "com.dvclab.dockerhub.model.EnumType";

    public T deserialize(JsonElement jsonElement, Type type,
                         JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        JsonPrimitive prim = jsonElement.getAsJsonPrimitive();
        Class klass = getObjectClass(CLASSNAME);
        return jsonDeserializationContext.deserialize(prim, klass);
    }

    /****** Helper method to get the className of the object to be deserialized *****/
    public Class getObjectClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException(e.getMessage());
        }
    }

    @Override
    public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src);
    }
}
