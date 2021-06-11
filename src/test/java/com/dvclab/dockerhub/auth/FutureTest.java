package com.dvclab.dockerhub.auth;

import com.google.common.util.concurrent.*;
import one.rewind.db.model.Model;
import one.rewind.io.requester.basic.BasicRequester;
import one.rewind.io.requester.exception.AccountException;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FutureTest {

	@Test
	public void test() throws InterruptedException {

		ListeningExecutorService es = MoreExecutors.listeningDecorator(
				Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat("T" + "-%d").build()));
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(2);

		ListenableFuture<Void> f1 = es.submit(
				new Callable<Void>() {
					public Void call() throws URISyntaxException, AccountException, InterruptedException {
						Thread.sleep(10000);
						//System.err.println(src);
						return null;
					}
				});

		ListenableFuture<Void> f2 = Futures.withTimeout(f1, 5000, TimeUnit.MILLISECONDS, ses);

		Futures.addCallback(
				f1,
				new FutureCallback<Void>() {
					public void onSuccess(Void void_) {
						System.err.println("A1");
					}
					public void onFailure(Throwable thrown) {
						Model.logger.error("A2", thrown);
					}
				}, es);

		Futures.addCallback(
				f2,
				new FutureCallback<Void>() {
					public void onSuccess(Void void_) {
						System.err.println("B1");
					}
					public void onFailure(Throwable thrown) {
						f1.cancel(true);
						Model.logger.error("B2", thrown);
					}
				}, es);

		Thread.sleep(100000);
	}
}
