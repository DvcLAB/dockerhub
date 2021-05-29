package com.dvclab.dockerhub.util.test;

import one.rewind.db.S3Adapter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Set;

public class AnotherTest {

	public void test() {


		S3Adapter.get("test");
	}
}
