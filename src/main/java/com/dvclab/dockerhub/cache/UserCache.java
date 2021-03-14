package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.model.User;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.exception.ModelException;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class UserCache extends Caches {

	public static Map<String, String> TOKEN_UID = new HashMap<>();
	public static Map<String, User> USERS = new HashMap<>();

	/**
	 * 更新用户缓存信息
	 *
	 * @param token
	 * @param user_info
	 * @param expiration
	 * @return
	 * @throws DBInitException
	 * @throws SQLException
	 * @throws ModelException.ClassNotEqual
	 * @throws IllegalAccessException
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

		TOKEN_UID.put(token, user.id);
		USERS.put(user.id, user);

		long delay = expiration.getTime() - System.currentTimeMillis();

		// 超过有效期清空用户token缓存
		ses.schedule(() -> {
			USERS.remove(TOKEN_UID.remove(token));
		}, delay, TimeUnit.SECONDS);

		user.upsert();

		return user;
	}
}
