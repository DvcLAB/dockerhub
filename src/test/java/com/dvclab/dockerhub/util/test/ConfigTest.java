package com.dvclab.dockerhub.util.test;

import one.rewind.util.Configs;
import org.junit.Test;

import java.io.IOException;

public class ConfigTest {

	@Test
	public void genConf() throws IOException {

		Configs.export("prod");
	}
}
