package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;

public class MicronodeListFieldMigrationTest extends AbstractFieldMigrationTest {
	private final DataProvider FILL = (container, name) -> {
		MicronodeGraphFieldList field = container.createMicronodeFieldList(name);

		Micronode micronode = field.createMicronode(new MicronodeResponse());
		micronode.setMicroschemaContainer(microschemaContainers().get("vcard"));
		micronode.createString("firstName").setString("Donald");
		micronode.createString("lastName").setString("Duck");

		micronode = field.createMicronode(new MicronodeResponse());
		micronode.setMicroschemaContainer(microschemaContainers().get("vcard"));
		micronode.createString("firstName").setString("Mickey");
		micronode.createString("lastName").setString("Mouse");
	};

	private static final FieldFetcher FETCH = (container, name) -> container.getMicronodeList(name);

	@Override
	@Test
	public void testRemove() {
		removeField(CREATEMICRONODELIST, FILL, FETCH);
	}

	@Override
	@Test
	public void testRename() {
		renameField(CREATEMICRONODELIST, FILL, FETCH, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getMicronodeList(name).getValues()).as(NEWFIELDVALUE).hasSize(2);
			assertThat(container.getMicronodeList(name).getValues().get(0)).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Donald").containsStringField("lastName", "Duck");
			assertThat(container.getMicronodeList(name).getValues().get(1)).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Mickey").containsStringField("lastName", "Mouse");
		});
	}

	@Override
	@Test
	public void testChangeToBinary() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEBINARY, (container, name) -> {
			assertThat(container.getBinary(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBoolean() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEBOOLEAN, (container, name) -> {
			assertThat(container.getBoolean(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToBooleanList() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEBOOLEANLIST, (container, name) -> {
			assertThat(container.getBooleanList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDate() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEDATE, (container, name) -> {
			assertThat(container.getDate(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToDateList() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEDATELIST, (container, name) -> {
			assertThat(container.getDateList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtml() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEHTML, (container, name) -> {
			assertThat(container.getHtml(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToHtmlList() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEHTMLLIST, (container, name) -> {
			assertThat(container.getHTMLList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToMicronode() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEMICRONODE, (container, name) -> {
			assertThat(container.getMicronode(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getMicronode(name).getMicronode()).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Donald").containsStringField("lastName", "Duck");
		});
	}

	@Override
	@Test
	public void testChangeToMicronodeList() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATEMICRONODELIST, (container, name) -> {
			assertThat(container.getMicronodeList(name)).as(NEWFIELD).isNotNull();
			assertThat(container.getMicronodeList(name).getValues()).as(NEWFIELDVALUE).hasSize(2);
			assertThat(container.getMicronodeList(name).getValues().get(0)).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Donald").containsStringField("lastName", "Duck");
			assertThat(container.getMicronodeList(name).getValues().get(1)).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Mickey").containsStringField("lastName", "Mouse");
		});
	}

	@Override
	@Test
	public void testChangeToNode() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATENODE, (container, name) -> {
			assertThat(container.getNode(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNodeList() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATENODELIST, (container, name) -> {
			assertThat(container.getNodeList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumber() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATENUMBER, (container, name) -> {
			assertThat(container.getNumber(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToNumberList() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATENUMBERLIST, (container, name) -> {
			assertThat(container.getNumberList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToString() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATESTRING, (container, name) -> {
			assertThat(container.getString(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testChangeToStringList() {
		changeType(CREATEMICRONODELIST, FILL, FETCH, CREATESTRINGLIST, (container, name) -> {
			assertThat(container.getStringList(name)).as(NEWFIELD).isNull();
		});
	}

	@Override
	@Test
	public void testCustomMigrationScript() {
		customMigrationScript(CREATEMICRONODELIST, FILL, FETCH, "function migrate(node, fieldname, convert) {node.fields[fieldname].reverse(); return node;}", (container, name) -> {
			MicronodeGraphFieldList field = container.getMicronodeList(name);
			assertThat(field).as(NEWFIELD).isNotNull();
			field.reload();
			assertThat(field.getValues()).as(NEWFIELDVALUE).hasSize(2);
			assertThat(field.getValues().get(0)).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Mickey").containsStringField("lastName", "Mouse");
			assertThat(field.getValues().get(1)).as(NEWFIELDVALUE)
					.containsStringField("firstName", "Donald").containsStringField("lastName", "Duck");
		});
	}

	@Override
	@Test
	public void testInvalidMigrationScript() {
		invalidMigrationScript(CREATEMICRONODELIST, FILL);
	}
}
