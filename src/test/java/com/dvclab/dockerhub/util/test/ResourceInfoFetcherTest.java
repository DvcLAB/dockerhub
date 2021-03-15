package com.dvclab.dockerhub.util.test;

import com.dvclab.dockerhub.util.ResourceInfoFetcher;
import one.rewind.db.exception.DBInitException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ResourceInfoFetcherTest {

	@Test
	public void getProjectInfo() throws URISyntaxException, DBInitException, SQLException, IOException {

		System.err.println(ResourceInfoFetcher.getProjectInfo("https://github.com/DvcLAB/CNN"));
	}

	@Test
	public void getDockerAuthToken() throws IOException, URISyntaxException {

		System.err.println(ResourceInfoFetcher.getDockerAuthToken("registry:catalog:*"));
	}

}