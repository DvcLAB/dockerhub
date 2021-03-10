package com.dvclab.dockerhub.util;

import one.rewind.util.FileUtil;
import one.rewind.util.KeyGenUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class DockerHubInitializer {

	/**
	 *
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException
	 */
	public static void generateRootKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, IOException {

		Pair<String, String> keyPair = KeyGenUtil.genKeyPair("dockerhub");
		FileUtil.writeBytesToFile(keyPair.getKey().getBytes(), "keys/dockerhub.pub");
		FileUtil.writeBytesToFile(keyPair.getValue().getBytes(), "keys/dockerhub");

	}

	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		// generateRootKeyPair();
	}
}
