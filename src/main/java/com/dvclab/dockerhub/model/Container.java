package com.dvclab.dockerhub.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.annotation.DBName;
import one.rewind.db.model.ModelD;
import one.rewind.nio.json.persister.JSONAbleFieldPersister;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Date;
import java.util.List;
import java.util.Queue;

@DBName("dockerhub")
@DatabaseTable(tableName = "containers")
public class Container extends ModelD {

	public enum Status {
		New,
		Init,
		Deployed,
		Repo_Clone_Success,
		Pip_Install_Success,
		Dataset_Load_Success,
		Jupyterlab_Start_Success,
		Port_Forwarding_Success,
		Failure,
		Paused,
		Deleted,
		Running
	}

	@DatabaseField(dataType = DataType.STRING, width = 64, canBeNull = false, indexName = "default")
	public String uid;

	public User user;

	@DatabaseField(dataType = DataType.STRING, width = 64)
	public String host_id;

	// 是否是用户主机创建的容器
	@DatabaseField(dataType = DataType.BOOLEAN)
	public boolean user_host = false;

	// CPU核数
	@DatabaseField(dataType = DataType.FLOAT, width = 4, canBeNull = false)
	public float cpus = 2;

	// 内存数量，单位GiB
	@DatabaseField(dataType = DataType.FLOAT, width = 4, canBeNull = false)
	public float mem = 4;

	// 容器是否支持GPU
	@DatabaseField(dataType = DataType.BOOLEAN, canBeNull = false)
	public boolean gpu_enabled = false;

	// 使用GPU设备编号
	@DatabaseField(dataType = DataType.STRING, width = 16)
	public String gpu_devices;

	// GPU 设备名
	@DatabaseField(persisterClass = JSONAbleFieldPersister.class, columnDefinition = "TEXT")
	public List<String> gpu_devices_product_names;

	// 可访问的JupyterLab地址
	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String jupyter_url;

	// 跳板机id
	@DatabaseField(dataType = DataType.STRING, width = 64)
	public String tunnel_id;

	// 跳板机内网地址映射端口号
	@DatabaseField(dataType = DataType.INTEGER, width = 5)
	public int tunnel_port;

	// docker_compose配置文件
	@DatabaseField(dataType = DataType.STRING, columnDefinition = "TEXT")
	public String docker_compose_config;

	// 状态
	@DatabaseField(dataType = DataType.ENUM_STRING, width = 32)
	public Status status = Status.New;

	// CPU使用率
	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false)
	public float cpu_usage = 0;

	// 内存使用率
	@DatabaseField(dataType = DataType.FLOAT, canBeNull = false)
	public float mem_usage = 0;

	// 进程数量
	@DatabaseField(dataType = DataType.INTEGER, canBeNull = false)
	public int proc_num = 0;

	// 容器名
	@DatabaseField(dataType = DataType.STRING, width = 64)
	public String container_name;

	// 镜像ID
	@DatabaseField(dataType = DataType.STRING, width = 64)
	public String image_id;

	public Image image;

	// 上一次接收到心跳包的时间
	@DatabaseField(
			dataType = DataType.DATE,
			width = 3
	)
	public Date last_keep_alive;

	// 开始运行的时间
	@DatabaseField(
			dataType = DataType.DATE,
			width = 3
	)
	public Date begin_run_time;

	// 容器运行的时长
	public Long alive_time;

	// 容器CPU时序用量
	public Queue<Object[]> cpu_series = new CircularFifoQueue<>(30);
	// 容器内存时序用量
	public Queue<Object[]> mem_series = new CircularFifoQueue<>(30);
	// 容器proc时序用量
	public Queue<Object[]> proc_series = new CircularFifoQueue<>(30);

	public Container(){}

	public void clearStatus () {
		this.cpu_usage = 0;
		this.mem_usage = 0;
		this.proc_num = 0;
	}
}
