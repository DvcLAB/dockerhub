package com.dvclab.dockerhub.cache;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 内存缓存
 */
public class Caches {

	public static ScheduledExecutorService ses;

	static {
		// 定时缓存更新器
		ses = Executors.newScheduledThreadPool(4,
				new ThreadFactoryBuilder().setNameFormat("CacheUpdater-%d").build());

	}
}
