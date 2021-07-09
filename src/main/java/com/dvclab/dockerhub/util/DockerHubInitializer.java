package com.dvclab.dockerhub.util;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import one.rewind.db.exception.DBInitException;
import one.rewind.db.util.Refactor;
import one.rewind.util.FileUtil;
import one.rewind.util.KeyGenUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayOutputStream;
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
	 */
	public static void generateRootKeyPair() throws JSchException {

		String name = "dockerhub";

		KeyPair pair = KeyPair.genKeyPair(new JSch(), KeyPair.RSA, 2048);

		/*KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		//SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		keyGen.initialize(2048*//*, random*//*);*/

		/*KeyPair pair = keyGen.generateKeyPair();*/
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		pair.writePrivateKey(baos);

		FileUtil.writeBytesToFile(baos.toByteArray(), "keys/" + name);

		baos = new ByteArrayOutputStream();
		pair.writePublicKey(baos, name);

		FileUtil.writeBytesToFile(baos.toByteArray(), "keys/" + name + ".pub");
	}

	/**
	 * 初始化方法
	 * @param args
	 */
	public static void main(String[] args) throws JSchException {

		generateRootKeyPair();

		// 创建数据库
		// Refactor.createTables("com.dvclab.dockerhub.model");
	}
}
