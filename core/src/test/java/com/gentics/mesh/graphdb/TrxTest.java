package com.gentics.mesh.graphdb;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.test.TestUtil;

import io.vertx.core.AsyncResult;

public class TrxTest extends AbstractBasicDBTest {

	@Test
	public void testConcurrentUpdate() throws InterruptedException, BrokenBarrierException, TimeoutException {
		final int nThreads = 10;
		final int nRuns = 200;

		TagFamily tagFamily = tagFamily("colors");

		for (int r = 0; r < nRuns; r++) {
			final int currentRun = r;
			CountDownLatch latch = new CountDownLatch(nThreads);
			Node node = content();

			// Start two threads with a retry trx
			for (int i = 0; i < nThreads; i++) {
				final int threadNo = i;
				System.out.println("Thread [" + threadNo + "] Starting");
				db.asyncTrx(trx -> {
					Tag tag = tagFamily.create("bogus_" + threadNo + "_" + currentRun, project(), user());
					node.addTag(tag);
					trx.success();
				} , rh -> {
					latch.countDown();
				});
			}

			System.out.println("Waiting on lock");
			latch.await();

			try (Trx tx = db.trx()) {
				int expect = nThreads * (r + 1);
				assertEquals("Expected {" + expect + "} tags since this is the " + r + "th run.", expect, content().getTags().size());
			}
		}
	}

	@Test
	public void testAsyncTrxFailed() throws Throwable {
		CompletableFuture<AsyncResult<Object>> cf = new CompletableFuture<>();
		db.asyncTrx(trx -> {
			trx.failure();
		} , rh -> {
			cf.complete(rh);
		});
		assertTrue(cf.get().succeeded());
	}

	@Test(expected = RuntimeException.class)
	public void testAsyncTrxWithError() throws Throwable {
		CompletableFuture<Throwable> cf = new CompletableFuture<>();
		db.asyncTrx(trx -> {
			throw new RuntimeException("error");
		} , rh -> {
			cf.complete(rh.cause());
		});
		assertEquals("error", cf.get().getMessage());
		throw cf.get();
	}

	@Test(expected = RuntimeException.class)
	public void testAsyncNoTrxWithError() throws Throwable {
		CompletableFuture<Throwable> cf = new CompletableFuture<>();
		db.asyncNoTrx(noTrx -> {
			throw new RuntimeException("error");
		} , rh -> {
			cf.complete(rh.cause());
		});
		assertEquals("error", cf.get().getMessage());
		throw cf.get();
	}

	@Test
	@Ignore
	public void testUpdateMultithreadedSimple() throws InterruptedException, BrokenBarrierException, TimeoutException {

		final int nThreads = 10;
		final int nRuns = 20;

		for (int r = 0; r < nRuns; r++) {
			CyclicBarrier barrier = new CyclicBarrier(nThreads);
			AtomicInteger integer = new AtomicInteger(0);

			TagFamily tagFamily = tagFamily("colors");
			Node node = content();

			ThreadLocal<Boolean> firstTry = new ThreadLocal<>();
			List<Thread> threads = new ArrayList<>();
			for (int i = 1; i < nThreads; i++) {
				System.out.println("Thread [" + i + "] Starting");
				Thread t = TestUtil.run(() -> {
					firstTry.set(true);
					int n = integer.incrementAndGet();
					db.trx(tx -> {
						//Load the elements again
						TagFamily cTagFamily = tagFamily.load();
						Project cProject = project().load();
						Node cNode = node.load();
						User cUser = user().load();

						Tag tag = cTagFamily.create("bogus_" + n, cProject, cUser);
						cNode.addTag(tag);
						tx.success();
						if (firstTry.get()) {
							firstTry.set(false);
							try {
								System.out.println("Thread [" + n + "] Waiting..");
								barrier.await(10, TimeUnit.SECONDS);
								System.out.println("Thread [" + n + "] Waited");
							} catch (Exception e) {
								System.out.println("Thread [" + n + "] Error handling.");
								e.printStackTrace();
							}
						}
						System.out.println("Thread [" + n + "] Successful updated element.");
					});
				});
				threads.add(t);
			}

			System.out.println("Waiting on lock");
			for (Thread currentThread : threads) {
				currentThread.join();
			}
			try (Trx tx = db.trx()) {
				int expect = nThreads * (r + 1);
				assertEquals("Expected {" + expect + "} tags since this is run {" + r + "}", expect, content().getTags().size());
			}
		}
	}

	@Test
	@Ignore
	public void testUpdateMultithreaded() throws InterruptedException, BrokenBarrierException, TimeoutException {

		final int nThreads = 10;
		final int nRuns = 20;
		final int maxRetry = 20;

		for (int r = 0; r < nRuns; r++) {
			final int currentRun = r;
			System.out.println("\n\n\n\n");
			//			TraversalHelper.printDebugVertices();
			CyclicBarrier barrierA = new CyclicBarrier(nThreads);
			CyclicBarrier barrierB = new CyclicBarrier(nThreads);
			Node node = content();
			TagFamily tagFamily = tagFamily("colors");
			List<Thread> threads = new ArrayList<>();
			Project project = project();
			User user = user();

			for (int i = 0; i < nThreads; i++) {
				final int threadNo = i;
				System.out.println("Thread [" + threadNo + "] Starting");
				Thread t = TestUtil.run(() -> {

					for (int retry = 0; retry < maxRetry; retry++) {
						try {
							try (Trx tx = db.trx()) {

								if (retry == 0) {
									try {
										System.out.println("Thread [" + threadNo + "] Waiting..");
										barrierA.await(10, TimeUnit.SECONDS);
										System.out.println("Thread [" + threadNo + "] Waited");
									} catch (Exception e) {
										System.out.println("Thread [" + threadNo + "] Error handling barrier timeout? - retry: " + retry);
										//e.printStackTrace();
									}
								}
								//Load used elements
								TagFamily reloadedTagFamily = tx.getGraph().getFramedVertexExplicit(TagFamilyImpl.class, tagFamily.getImpl().getId());
								Node reloadedNode = tx.getGraph().getFramedVertexExplicit(NodeImpl.class, node.getImpl().getId());
								User reloadedUser = tx.getGraph().getFramedVertexExplicit(UserImpl.class, user.getImpl().getId());
								Project reloadedProject = tx.getGraph().getFramedVertexExplicit(ProjectImpl.class, project.getImpl().getId());

								Tag tag = reloadedTagFamily.create("bogus_" + threadNo + "_" + currentRun, reloadedProject, reloadedUser);
								// Reload the node
								reloadedNode.addTag(tag);
								tx.success();
								if (retry == 0) {
									try {
										System.out.println("Thread [" + threadNo + "] Waiting..");
										barrierB.await(10, TimeUnit.SECONDS);
										System.out.println("Thread [" + threadNo + "] Waited");
									} catch (Exception e) {
										System.out.println("Thread [" + threadNo + "] Error handling barrier timeout? - retry: " + retry);
										//e.printStackTrace();
									}
								}
							}
							System.out.println("Thread [" + threadNo + "] Successful updated element - retry: " + retry);
							break;
						} catch (Exception e) {

							//trx.rollback();
							System.out.println("Thread [" + threadNo + "] Got exception {" + e.getClass().getName() + "}  - retry: " + retry);
							e.printStackTrace();
						}
					}
				});
				threads.add(t);
			}
			System.out.println("Waiting on lock");
			//barrier.await(2, TimeUnit.SECONDS);
			for (Thread currentThread : threads) {
				currentThread.join();
			}
			//			Thread.sleep(1000);
			try (Trx tx = db.trx()) {
				int expect = nThreads * (r + 1);
				Node reloadedNode = tx.getGraph().getFramedVertexExplicit(NodeImpl.class, node.getImpl().getId());
				//				node.reload();
				assertEquals("Expected {" + expect + "} tags since this is run {" + r + "}.", expect, reloadedNode.getTags().size());
			}
		}
	}
}
