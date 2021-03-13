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
		New,
		Init,
		Repo_Clone_Success,
		Pip_Install_Success,
		Dataset_Load_Success,
		Jupyterlab_Start_Success,
		Port_Forwarding_Success,
		Failure,
		Deleted
	}

	@DatabaseField(dataType = DataType.STRING, width = 64, canBeNull = false, indexName = "default")
	public String uid;

	public User user;

	@DatabaseField(dataType = DataType.STRING, width = 64)
	public String host_id;

	// 是否时用户主机创建的容器
	@DatabaseField(dataType = DataType.BOOLEAN)
	public boolean user_host = false;

	// CPU核数
	@DatabaseField(dataType = DataType.FLOAT, width = 4, canBeNull = false)
	public float cpus = 2;

	// 内存数量，单位GiB
	@DatabaseField(dataType = DataType.FLOAT, width = 4, canBeNull = false)
	public float mem = 4;

	// 使用GPU设备编号
	@DatabaseField(dataType = DataType.STRING, width = 16)
	public String gpu_devices;

	@DatabaseField(persisterClass = JSONableListPersister.class, columnDefinition = "TEXT")
	public List<String> gpu_devices_product_names;

	// 可访问的JupyterLab地址
	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String jupyter_url;

	// 跳板机公网地址
	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String tunnel_wan_addr;

	// 跳板机内网地址
	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String tunnel_lan_addr;

	// 跳板机内网地址映射端口号
	@DatabaseField(dataType = DataType.INTEGER, width = 5)
	public int tunnel_port;

	// docker_compose配置文件
	@DatabaseField(dataType = DataType.STRING, columnDefinition = "TEXT")
	public String docker_compose_config;

	// 状态
	@DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
	public Status status = Status.New;

	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false)
	public float cpu_usage = 0;

	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false)
	public float mem_usage = 0;

	public Container(){}
}
