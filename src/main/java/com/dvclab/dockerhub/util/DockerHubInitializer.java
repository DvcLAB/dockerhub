package com.dvclab.dockerhub.util;

import one.rewind.db.exception.DBInitException;
import one.rewind.db.util.Refactor;
import one.rewind.util.FileUtil;
import one.rewind.util.KeyGenUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;

/**
 * 服务初始化
 */
public class DockerHubInitializer {

	/**
	 * 创建秘钥对
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException
	 */
	public static void generateRootKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, IOException {

		Pair<String, String> keyPair = KeyGenUtil.genKeyPair("dockerhub");
		FileUtil.writeBytesToFile(keyPair.getKey().getBytes(), "keys/dockerhub.pub");
		FileUtil.writeBytesToFile(keyPair.getValue().getBytes(), "keys/dockerhub");
	}

	/**
	 * 初始化方法
	 * @param args
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException
	 * @throws DBInitException
	 * @throws SQLException
	 */
	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchProviderException, IOException, DBInitException, SQLException {
		generateRootKeyPair();

		// 创建数据库
		// Refactor.createTables("com.dvclab.dockerhub.model");
	}
}
