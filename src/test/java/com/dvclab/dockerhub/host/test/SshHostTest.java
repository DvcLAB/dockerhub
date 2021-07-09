package com.dvclab.dockerhub.host.test;

import com.jcraft.jsch.JSchException;
import one.rewind.io.ssh.SshHost;
import one.rewind.util.FileUtil;
import org.junit.Test;

import java.io.IOException;

public class SshHostTest {

	@Test
	public void test() throws JSchException, IOException {

		SshHost host = new SshHost("10.0.9.31", 22, "root",
				FileUtil.readBytesFromFile("keys/dockerhub"));
		host.connect();
		String ps_aux = host.exec("ps aux");
		System.err.println(ps_aux);
	}
}
