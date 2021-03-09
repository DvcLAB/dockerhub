package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.User;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.exception.ModelException;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.auth.AuthenticationException;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UserCache {

	public static ScheduledExecutorService ses;

	static {
		ses = Executors.newScheduledThreadPool(1,
				new ThreadFactoryBuilder().setNameFormat("UserCacheUpdater-%d").build());
	}

	public static Map<String, User> TOKEN_USERS = new HashMap<>();

	/**
	 * 更新用户缓存信息
	 * @param token
	 * @param info
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static User update(String token, User user_info, Date expiration) throws DBInitException, SQLException, ModelException.ClassNotEqual, IllegalAccessException {

		User user = User.getById(User.class, user_info.id);

		// 用户不存在，创建对应记录
		if(user == null) {
			user = user_info;
		}

		user.username = user_info.username;
		user.email = user_info.email;
		user.avatar_url = user_info.avatar_url;
		user.roles = user_info.roles;

		TOKEN_USERS.put(token, user);

		long delay = expiration.getTime() - System.currentTimeMillis();

		// 超过有效期清空用户token缓存
		ses.schedule(() -> {
			TOKEN_USERS.remove(token);
		}, delay, TimeUnit.SECONDS);

		user.upsert();

		return user;
	}
}
