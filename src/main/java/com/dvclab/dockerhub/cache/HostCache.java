package com.dvclab.dockerhub.cache;

import com.dvclab.dockerhub.DockerHubService;
import com.dvclab.dockerhub.model.Host;
import com.dvclab.dockerhub.websocket.HostInfoPublisher;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jcraft.jsch.JSchException;
import one.rewind.db.exception.DBInitException;
import one.rewind.io.docker.model.DockerHost;
import one.rewind.monitor.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import static com.dvclab.dockerhub.DockerHubService.logger;

/**
 * 主机缓存
 */
public class HostCache {

	public static Map<String, Host> hosts = new HashMap<>();

	public static ScheduledExecutorService ses;

	/**
	 *
	 * @throws DBInitException
	 * @throws SQLException
	 * @throws JSchException
	 */
	public static void init() throws DBInitException, SQLException, JSchException {

		ses = Executors.newScheduledThreadPool(2,
				new ThreadFactoryBuilder().setNameFormat("HostStatUpdater-%d").build());

		for (Host host : Host.getAll(Host.class)) {
			addHost(host);
		}
	}

	/**
	 * 添加主机，并通过websocket定时发布主机状态信息
	 * @param host
	 * @throws JSchException
	 */
	public static void addHost(Host host) throws JSchException {

		host.container_num.set(0);
		host.connectSshHost();
		hosts.put(host.id, host);

		// 定期更新CPU/内存使用情况
		ScheduledFuture<?> scheduledFuture = ses.scheduleAtFixedRate(() -> {

			try {
				long ts = System.currentTimeMillis();

				CPUInfo ci = new CPUInfo().probe(host.sshHost);
				host.cpu_info = ci;
				host.cpu_series.add(new Object[] {ts, ci.usage});

				MemInfo mi = new MemInfo().probe(host.sshHost);
				host.mem_info = mi;

				IoInfo io = new IoInfo().probe(host.sshHost);
				host.io_info = io;

				NetInfo ni = new NetInfo().probe(host.sshHost);
				host.net_info = ni;
				host.network_series.add(new Object[] {ts, ni.total_rate});

				if(host.gpu_enabled) {
					try {
						GPUInfo gi = new GPUInfo().probe(host.sshHost);
						host.gpu_info = gi;
						host.gpu_series.add(new Object[] {ts,
								gi.gpus.stream().map(gpu -> gpu.gpu_util).mapToDouble(Double::valueOf).average().orElse(0)
						});
					}
					// Host 不支持 nvidia-smi 命令，gpu_info解析错误
					catch (NullPointerException e) {
						host.gpu_info = null;
						host.gpu_enabled = false;
						host.gpu_series = null;
					}
				}

				updateContainerStatus(host);
				host.status = DockerHost.Status.RUNNING;

			}
			catch (JSchException e) {
				try {
					host.sshHost.connect();
				} catch (JSchException ex) {
					host.status = DockerHost.Status.STOPPED;
					logger.error("Error reconnect ssh, ", e);
				}
			}
			catch (InterruptedException | IOException e) {
				logger.error("Error get host info, ", e);
				host.status = DockerHost.Status.STOPPED;
			}

			try {
				// 更新数据库状态
				host.update();
			} catch (DBInitException | SQLException e) {
				logger.error("Error save host info to db, ", e);
			}

			// WebSocket 更新消息
			HostInfoPublisher.broadcast(host.id, host);

		}, 5, 5, TimeUnit.SECONDS);
	}

	/**
	 * 获取主机
	 * TODO gpu使用模式
	 * @param gpu_enabled
	 * @param cpus
	 * @param mem
	 * @return
	 * @throws HostException
	 */
	public static Host getHost(boolean gpu_enabled, float cpus, float mem) throws HostException {

		return hosts.values().stream()
			.filter(h -> {
				if(gpu_enabled) return h.gpu_enabled;
				return true;
			})
			.filter(h -> h.getCpuAvailable() > cpus && h.getMemAvailable() > mem)
			.min(Comparator.comparing(h -> h.container_num.get()))
			.orElseThrow(() -> new HostException("No available host"));
	}

	/**
	 * 更新自有主机的容器状态信息
	 * @param host
	 */
	public static void updateContainerStatus(Host host) {

		String output = host.exec("docker stats --no-stream --format \"{{.Name}}\\t{{.CPUPerc}}\\t{{.MemPerc}}\\t{{.PIDs}}\"");

		for(String line : output.split("\n")) {

			String[] tokens = line.split("\t");
			String name = tokens[0];
			Optional.ofNullable(ContainerCache.containers.get(name)).ifPresent(c -> {

				c.cpu_usage = Float.parseFloat(tokens[1].replaceAll("%", ""));
				c.mem_usage = Float.parseFloat(tokens[2].replaceAll("%", ""));
				c.proc_num = Integer.parseInt(tokens[3]);

				try {
					c.update();
				}
				catch (DBInitException | SQLException e) {
					DockerHubService.logger.error("Error, ", e);
				}
			});
		}
	}

	/**
	 *
	 */
	public static class HostException extends Exception {

		public HostException(String msg) {
			super(msg);
		}
	}
}
