package com.dvclab.dockerhub.util;

import com.dvclab.dockerhub.DockerHubService;
import com.jcraft.jsch.JSchException;
import com.typesafe.config.Config;
import one.rewind.io.ssh.SshHost;
import one.rewind.util.Configs;
import one.rewind.util.FileUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 *
 */
public class PypiServerUpdater {

	private static PypiServerUpdater instance;

	public static PypiServerUpdater getInstance() throws JSchException {

		synchronized (PypiServerUpdater.class) {
			if (instance == null) {
				instance = new PypiServerUpdater();
			}
		}

		return instance;
	}

	private String srvHost;

	private int srvSshPort;

	private String srvSshPrivateKey;

	private SshHost sshHost;

	private ExecutorService es = Executors.newFixedThreadPool(1);

	private PypiServerUpdater() throws JSchException {

		Config config = Configs.getConfig(PypiServerUpdater.class);
		srvHost = config.getString("srv_host");
		srvSshPort = config.getInt("srv_ssh_port");
		srvSshPrivateKey = config.getString("srv_ssh_private_key");

		sshHost = new SshHost(srvHost, srvSshPort, "root", srvSshPrivateKey.getBytes());
		sshHost.connect();
	}

	/**
	 *
	 * @param newPackages 新项目中的pypi依赖包
	 * @throws JSchException
	 * @throws IOException
	 */
	public void update(Set<String> newPackages)  {

		es.submit(() -> {

			try {
				String localPath = "tpl/bandersnatch.conf";
				String remotePath = "/opt/bandersnatch/bandersnatch.conf";

				// 将远程主机文件拷贝到本地
				sshHost.copyRemoteToLocal(localPath, remotePath);

				String src = FileUtil.readFileByLines(localPath);

				// 读取原有依赖包
				Set<String> packages = Arrays.stream(src.replaceAll("(?si)^.+?packages =", "").split("\r?\n"))
						.map(i -> i.trim())
						.collect(Collectors.toSet());

				// 依赖包合并
				packages.addAll(newPackages);

				// 构建新配置文件
				String newSrc = src.replaceAll("(?si)(?<=packages =).+$", "") +
						packages.stream().sorted().collect(Collectors.joining("\n\t", "\n\t", ""));

				// 写入本地
				FileUtil.writeBytesToFile(newSrc.getBytes(), localPath);

				// 拷贝至远端主机
				sshHost.copyLocalToRemote(localPath, remotePath);

				// 执行bandersnatch 同步命令
				sshHost.exec("docker exec -it bandersnatch bandersnatch --config /conf/bandersnatch.conf mirror");
			}
			catch (Exception e) {
				DockerHubService.logger.error("Error refresh bandersnatch mirror, ", e);
			}
		});
	}
}
