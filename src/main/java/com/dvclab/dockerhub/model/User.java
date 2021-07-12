package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.Daos;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@DBName("dockerhub")
@DatabaseTable(tableName = "users")
public class User extends one.rewind.nio.web.model.User {

	// 用户角色类型
	public enum Role {
		DOCKHUB_ADMIN,
		DOCKHUB_USER,
		OFFLINE_ACCESS,
		UMA_AUTHORIZATION,
	}

	@DatabaseField(dataType = DataType.INTEGER, width = 4)
	public int max_container_num = 10;

	@DatabaseField(dataType = DataType.INTEGER, width = 4)
	public int container_num = 0;

	public User() {}

	/**
	 *
	 * @param u_
	 */
	public User(one.rewind.nio.web.model.User u_) {
		this.id = u_.id;
		this.username = u_.username;
		this.email = u_.email;
		this.avatar_url = u_.avatar_url;
		this.roles = u_.roles;
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public static User getById(String id) {
		try {
			return Daos.get(User.class).queryForId(id);
		} catch (SQLException | DBInitException e) {
			logger.error("Error query User[{}]", id, e);
			return null;
		}
	}

	/**
	 * 根据用户id列表获取用户列表
	 * @return
	 */
	public static Map<String, User> getUsers(List<String> uids) throws DBInitException, SQLException {

	    if(uids.size() == 0) {
		    return new HashMap<>();
		}

		return Daos.get(User.class).queryBuilder()
				.where().in("id", uids).query()
				.stream().collect(Collectors.toMap(User::getId, user -> user));
	}

	/**
	 * 根据用户名获取用户ID
	 */
	public static String getUserId (String username) throws SQLException, DBInitException {

		QueryBuilder<User, Object> queryBuilder = Daos.get(User.class).queryBuilder();
		queryBuilder.where().like("username", username + "%");
		Optional<User> user = Optional.ofNullable(queryBuilder.queryForFirst());

		if(user.isPresent()) {
			return user.get().id;
		}

		return "";
	}

	/**
	 *
	 * @param role
	 * @return
	 */
	public boolean hasRole(Role role) {
		return this.roles.contains(role.name());
	}
}
