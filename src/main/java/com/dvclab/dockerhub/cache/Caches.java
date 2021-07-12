package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.User;
import one.rewind.nio.web.cache.UserCache;

public class Caches extends one.rewind.nio.web.cache.Caches {

	public static UserCache<User> userCache;
}
