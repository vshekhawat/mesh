package com.gentics.mesh.core.schema.change;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.graphdb.spi.Database;

public class RemoveFieldChangeTest extends AbstractChangeTest {

	@Test
	@Override
	public void testFields() throws IOException {
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);

		change.setFieldName("someField");
		assertEquals("someField", change.getFieldName());
	}

	@Test
	@Override
	public void testApply() {

		SchemaContainerVersion version = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);

		// 1. Create schema with field
		Schema schema = new SchemaModel();
		schema.addField(FieldUtil.createStringFieldSchema("test"));

		// 2. Create remove field change
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName("test");

		version.setNextChange(change);
		version.setSchema(schema);

		// 3. Apply the change
		FieldSchemaContainer updatedSchema = mutator.apply(version);

		assertThat(updatedSchema).hasNoField("test");

	}

	@Test
	@Override
	public void testUpdateFromRest() throws IOException {
		SchemaChangeModel model = new SchemaChangeModel();
		model.setMigrationScript("test");
		model.setProperty(SchemaChangeModel.FIELD_NAME_KEY, "someField");
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.updateFromRest(model);
		assertEquals("test", change.getMigrationScript());
		assertEquals("someField", change.getFieldName());
	}

	@Test
	@Override
	public void testGetMigrationScript() throws IOException {
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		assertNotNull("Remove Type changes have a auto migation script.", change.getAutoMigrationScript());

		assertNotNull("Intitially the default migration script should be set.", change.getMigrationScript());
		change.setCustomMigrationScript("test");
		assertEquals("The custom migration script was not changed.", "test", change.getMigrationScript());
	}

	@Test
	@Override
	public void testTransformToRest() throws IOException {
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		assertEquals(RemoveFieldChange.OPERATION, change.transformToRest().getOperation());
		change.setCustomMigrationScript("test");
		change.setFieldName("test2");

		SchemaChangeModel model = change.transformToRest();
		assertEquals("test", model.getMigrationScript());
		assertEquals("test2", model.getProperty(SchemaChangeModel.FIELD_NAME_KEY));
	}

}
