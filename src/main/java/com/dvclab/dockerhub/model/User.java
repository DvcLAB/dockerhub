package com.dvclab.dockerhub.model;

import com.dvclab.dockerhub.serialization.EnumListPersister;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.Daos;
import one.rewind.db.annotation.DBName;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.model.ModelD;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DBName("dockerhub")
@DatabaseTable(tableName = "users")
public class User extends ModelD {

	public static enum Role {
		DOCKHUB_ADMIN,
		DOCKHUB_USER,
		OFFLINE_ACCESS,
		UMA_AUTHORIZATION,
	}

	@DatabaseField(dataType = DataType.STRING, width = 32, indexName = "ue")
	public String username;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String email;

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String avatar_url;

	@DatabaseField(dataType = DataType.BOOLEAN, width = 1, indexName = "ue")
	public boolean enabled = true;

	@DatabaseField(persisterClass = EnumListPersister.class, columnDefinition = "TEXT", width = 256)
	public List<Role> roles = new ArrayList<>();

	@DatabaseField(dataType = DataType.INTEGER, width = 4)
	public int max_container_num = 10;

	@DatabaseField(dataType = DataType.INTEGER, width = 4)
	public int container_num = 0;

	public User() {}

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
	 *
	 * @param init_password
	 * @return
	 */
	@Deprecated
	public Map<String, Object> genUserRepresentation(String init_password) {

		Map<String, Object> rep = new HashMap<>();
		rep.put("firstName", this.username);
		rep.put("lastName", this.username);
		rep.put("username", this.username);
		rep.put("email", this.email);
		rep.put("emailVerified", true);
		rep.put("enabled", this.enabled);

		if(init_password != null) {
			rep.put("credentials",
				List.of(
					Map.of(
						"type", "password",
						"value", init_password,
						"temporary", true
					)
				)
			);
		}

		return rep;
	}
}
