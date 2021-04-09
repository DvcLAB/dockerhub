package com.dvclab.dockerhub.cache;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Caches {

	public static ScheduledExecutorService ses;

	static {
		ses = Executors.newScheduledThreadPool(4,
				new ThreadFactoryBuilder().setNameFormat("CacheUpdater-%d").build());

	}
}
