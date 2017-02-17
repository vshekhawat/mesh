package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.test.context.MeshTestHelper.call;

import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, useTinyDataset = false, startServer = true)
public class CrossEndpointTest extends AbstractMeshTest {

	@Test
	public void testAccessNewProjectRoute() {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setSchema(new SchemaReference().setName("folder"));
		request.setName(name);

		call(() -> client().createProject(request));

		call(() -> client().findNodes(name));

		call(() -> client().findTagFamilies(name));

	}

}
