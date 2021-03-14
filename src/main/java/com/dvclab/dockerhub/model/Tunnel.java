package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;
import one.rewind.txt.StringUtil;

@DBName("dockerhub")
@DatabaseTable(tableName = "tunnels")
public class Tunnel extends ModelD {

	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String wan_addr;

	@DatabaseField(dataType = DataType.INTEGER, width = 5)
	public int wan_port;
	@DatabaseField(dataType = DataType.STRING, width = 128)
	public String lan_addr;

	@DatabaseField(dataType = DataType.INTEGER, width = 5)
	public int begin_port;

	@DatabaseField(dataType = DataType.INTEGER, width = 5)
	public int end_port;

	public Tunnel() {}

	/**
	 *
	 * @param wan_addr
	 * @param wan_port
	 * @param lan_addr
	 * @param begin_port
	 * @param end_port
	 */
	public Tunnel(String wan_addr, int wan_port, String lan_addr, int begin_port, int end_port) {
		this.wan_addr = wan_addr;
		this.wan_port = wan_port;
		this.lan_addr = lan_addr;
		this.begin_port = begin_port;
		this.end_port = end_port;
		genId();
	}

	/**
	 *
	 * @return
	 */
	public Tunnel genId(){
		this.id = StringUtil.md5(wan_addr + ":" + wan_port);
		return this;
	}
}
