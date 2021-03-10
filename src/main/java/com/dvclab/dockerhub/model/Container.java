package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;
import one.rewind.db.persister.JSONableListPersister;

import java.util.List;

@DBName("dockerhub")
@DatabaseTable(tableName = "containers")
public class Container extends ModelD {

	public enum Status {
		Init,
		Repo_Clone_Success,
		Pip_Install_Success,
		Dataset_Load_Success,
		Jupyterlab_Start_Success,
		Port_Forwarding_Success
	}

	@DatabaseField(dataType = DataType.STRING, width = 64, canBeNull = false, indexName = "default")
	public String uid;

	@DatabaseField(dataType = DataType.STRING, width = 64)
	public int host_id;

	@DatabaseField(dataType = DataType.BOOLEAN)
	public boolean user_host = false;

	// CPU核数
	@DatabaseField(dataType = DataType.INTEGER, width = 4, canBeNull = false)
	public float cpus = 2;

	// 内存数量，单位GiB
	@DatabaseField(dataType = DataType.INTEGER, width = 4, canBeNull = false)
	public float mem = 4;

	// 使用GPU设备编号
	@DatabaseField(dataType = DataType.STRING, width = 16)
	public String gpu_devices;

	@DatabaseField(persisterClass = JSONableListPersister.class, columnDefinition = "TEXT")
	public List<String> gpu_devices_product_names;

	@DatabaseField(dataType = DataType.INTEGER, width = 5, canBeNull = false)
	public int jupyter_port;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String jupyter_url;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String tunnel_wan_addr;

	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String tunnel_lan_addr;

	@DatabaseField(dataType = DataType.INTEGER, width = 5)
	public int tunnel_port;

	@DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
	public Status status;

	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false)
	public float cpu_usage = 0;

	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false)
	public float mem_usage = 0;

	public Container(){}
}
