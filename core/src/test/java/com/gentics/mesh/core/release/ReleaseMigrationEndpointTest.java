package com.gentics.mesh.core.release;

import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.FAILED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatusResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.DeploymentOptions;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class ReleaseMigrationEndpointTest extends AbstractMeshTest {

	@Before
	public void setupVerticleTest() throws Exception {
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx().deployVerticle(meshDagger().jobWorkerVerticle(), options);
	}

	@Test
	public void testStartReleaseMigration() throws Throwable {
		Release newRelease;
		List<? extends Node> nodes;
		List<? extends Node> published;
		Project project = project();

		try (Tx tx = tx()) {
			assertThat(project.getInitialRelease().isMigrated()).as("Initial release migration status").isEqualTo(true);
		}

		call(() -> client().takeNodeOffline(PROJECT_NAME, tx(() -> project().getBaseNode().getUuid()),
				new PublishParametersImpl().setRecursive(true)));

		published = Arrays.asList(folder("news"), folder("2015"), folder("2014"), folder("march"));
		try (Tx tx = tx()) {
			nodes = project.getNodeRoot().findAll().stream().filter(node -> node.getParentNode(project.getLatestRelease().getUuid()) != null)
					.collect(Collectors.toList());
			assertThat(nodes).as("Nodes list").isNotEmpty();
		}

		// publish some nodes
		published.forEach(node -> {
			call(() -> client().publishNode(PROJECT_NAME, tx(() -> node.getUuid())));
		});

		try (Tx tx = tx()) {
			newRelease = project.getReleaseRoot().create("newrelease", user());
			assertThat(newRelease.isMigrated()).as("Release migration status").isEqualTo(false);
			tx.success();
		}
		nodes.forEach(node -> {
			Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT, ContainerType.PUBLISHED).forEach(type -> {
				assertThat(tx(() -> node.getGraphFieldContainers(newRelease, type))).as(type + " Field Containers before Migration").isNotNull()
						.isEmpty();
			});
		});
		requestReleaseMigration(newRelease);
		triggerAndWaitForMigration();

		try (Tx tx = tx()) {
			assertThat(newRelease.isMigrated()).as("Release migration status").isEqualTo(true);

			nodes.forEach(node -> {
				Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT).forEach(type -> {
					assertThat(node.getGraphFieldContainers(newRelease, type)).as(type + " Field Containers after Migration").isNotNull()
							.isNotEmpty();
				});

				if (published.contains(node)) {
					assertThat(node.getGraphFieldContainers(newRelease, ContainerType.PUBLISHED)).as("Published field containers after migration")
							.isNotNull().isNotEmpty();
				} else {
					assertThat(node.getGraphFieldContainers(newRelease, ContainerType.PUBLISHED)).as("Published field containers after migration")
							.isNotNull().isEmpty();
				}

				Node initialParent = node.getParentNode(initialReleaseUuid());
				if (initialParent == null) {
					assertThat(node.getParentNode(newRelease.getUuid())).as("Parent in new release").isNull();
				} else {
					assertThat(node.getParentNode(newRelease.getUuid())).as("Parent in new release").isNotNull()
							.isEqualToComparingOnlyGivenFields(initialParent, "uuid");
				}

				// TODO assert tags
			});
		}

	}

	@Test
	public void testStartForInitial() throws Throwable {
		try (Tx tx = tx()) {
			requestReleaseMigration(initialRelease());
			triggerAndWaitForMigration(MigrationStatus.FAILED);
		}
	}

	@Test
	public void testStartAgain() throws Throwable {
		Release newRelease;
		try (Tx tx = tx()) {
			newRelease = project().getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			requestReleaseMigration(newRelease);
			triggerAndWaitForMigration(MigrationStatus.COMPLETED);

			requestReleaseMigration(newRelease);
			MigrationStatusResponse response = triggerAndWaitForMigration(null);
			List<MigrationStatus> status = response.getMigrations().stream().map(e -> e.getStatus()).collect(Collectors.toList());
			assertThat(status).containsExactly(MigrationStatus.COMPLETED, MigrationStatus.FAILED);
		}

	}

	@Test
	public void testStartOrder() throws Throwable {

		Release newRelease;
		Release newestRelease;
		try (Tx tx = tx()) {
			Project project = project();
			newRelease = project.getReleaseRoot().create("newrelease", user());
			newestRelease = project.getReleaseRoot().create("newestrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			requestReleaseMigration(newestRelease);
			triggerAndWaitForMigration(FAILED);

			requestReleaseMigration(newRelease);
			MigrationStatusResponse response = triggerAndWaitForMigration(null);
			List<MigrationStatus> status = response.getMigrations().stream().map(e -> e.getStatus()).collect(Collectors.toList());
			assertThat(status).containsExactly(FAILED, COMPLETED);

			requestReleaseMigration(newestRelease);
			response = triggerAndWaitForMigration(null);
			status = response.getMigrations().stream().map(e -> e.getStatus()).collect(Collectors.toList());
			assertThat(status).containsExactly(FAILED, COMPLETED, COMPLETED);
		}
	}

	@Test
	public void testBigData() throws Throwable {

		MetricRegistry metrics = new MetricRegistry();
		Meter createdNode = metrics.meter("Create Node");
		Timer migrationTimer = metrics.timer("Migration");
		Release newRelease;
		try (Tx tx = tx()) {
			int numThreads = 1;
			int numFolders = 1000;
			Project project = project();
			String projectName = project.getName();
			String baseNodeUuid = project.getBaseNode().getUuid();

			ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS)
					.build();
			reporter.start(1, TimeUnit.SECONDS);

			ExecutorService service = Executors.newFixedThreadPool(numThreads);

			List<Future<Boolean>> futures = new ArrayList<>();

			for (int i = 0; i < numFolders; i++) {
				futures.add(service.submit(() -> {
					NodeCreateRequest create = new NodeCreateRequest();
					create.setLanguage("en");
					create.setSchema(new SchemaReference().setName("folder"));
					create.setParentNodeUuid(baseNodeUuid);
					call(() -> client().createNode(projectName, create));
					createdNode.mark();
					return true;
				}));
			}

			for (Future<Boolean> future : futures) {
				future.get();
			}

			newRelease = project.getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		requestReleaseMigration(newRelease);
		triggerAndWaitForMigration();
	}

	/**
	 * Request release migration and return the future
	 * 
	 * @param release
	 * @return future
	 */
	protected void requestReleaseMigration(Release release) {
		tx(() -> {
			boot().jobRoot().enqueueReleaseMigration(user(), release);
		});
	}
}
