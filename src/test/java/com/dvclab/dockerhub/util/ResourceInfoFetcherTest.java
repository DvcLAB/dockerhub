package com.dvclab.dockerhub.util;

import one.rewind.db.exception.DBInitException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ResourceInfoFetcherTest {

	@Test
	public void getProjectInfo() throws URISyntaxException, DBInitException, SQLException {

		System.err.println(ResourceInfoFetcher.getProjectInfo("https://github.com/DvcLAB/CNN"));

	}

}