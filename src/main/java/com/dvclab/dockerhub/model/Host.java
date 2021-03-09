package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.jcraft.jsch.JSchException;
import one.rewind.db.annotation.DBName;
import one.rewind.io.docker.model.DockerHost;
import one.rewind.io.ssh.SshHost;
import one.rewind.txt.StringUtil;

/**
 *
 */
@DBName("dockerhub")
@DatabaseTable(tableName = "hosts")
public class Host extends DockerHost {

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean gpu_enabled = false;

	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean user_host = false;

	@DatabaseField(dataType = DataType.STRING, width = 64, indexName = "uid-idx")
	public String uid;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String private_key;

	/**
	 *
	 */
	public Host() {}

	/**
	 *
	 * @param uid
	 * @param private_key
	 */
	public Host(String uid, String private_key) {
		this.uid = uid;
		this.private_key = private_key;
		this.user_host = true;
		this.id = StringUtil.md5(uid + "::" + private_key);
	}

	/**
	 *
	 * @param ip
	 * @param port
	 * @param username
	 */
	public Host(String ip, int port, String username) throws JSchException {
		super(ip, port, username);
		this.id = StringUtil.md5(ip + port + username);
	}

	/**
	 *
	 * @return
	 * @throws JSchException
	 * @throws IllegalAccessException
	 */
	public Host init() throws JSchException, IllegalAccessException {

		if(user_host) throw new IllegalAccessException();

		this.container_num.set(0);

		if (sshHost == null) {
			sshHost = new SshHost(ip, port, username, PK);
			sshHost.connect();
		}

		return this;
	}
}
