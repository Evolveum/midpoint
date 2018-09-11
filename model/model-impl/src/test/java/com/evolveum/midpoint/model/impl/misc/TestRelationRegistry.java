/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.misc;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.util.QNameUtil.unqualify;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRelationRegistry extends AbstractInternalModelIntegrationTest {

	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "misc");

	@Autowired protected RelationRegistry relationRegistry;

	@Test
	public void test100DefaultRelations() throws SchemaException {
		final String TEST_NAME = "test100DefaultRelations";

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		assertEquals("Wrong # of default relations", RelationTypes.values().length, relationRegistry.getRelationDefinitions().size());

		RelationDefinitionType orgDefaultDef = relationRegistry.getRelationDefinition(SchemaConstants.ORG_DEFAULT);
		RelationDefinitionType defaultDef = relationRegistry.getRelationDefinition(unqualify(SchemaConstants.ORG_DEFAULT));
		RelationDefinitionType nullDef = relationRegistry.getRelationDefinition(null);
		assertNotNull("No definition for null relation", nullDef);
		assertEquals("null and 'org:default' definitions differ", nullDef, orgDefaultDef);
		assertEquals("null and 'default' definitions differ", nullDef, defaultDef);

		assertTrue(relationRegistry.isManager(SchemaConstants.ORG_MANAGER));
		assertTrue(relationRegistry.isManager(unqualify(SchemaConstants.ORG_MANAGER)));
		assertFalse(relationRegistry.isManager(SchemaConstants.ORG_APPROVER));
		assertFalse(relationRegistry.isManager(unqualify(SchemaConstants.ORG_APPROVER)));
		assertFalse(relationRegistry.isManager(SchemaConstants.ORG_DEFAULT));
		assertFalse(relationRegistry.isManager(unqualify(SchemaConstants.ORG_DEFAULT)));
		assertFalse(relationRegistry.isManager(null));

		assertTrue(relationRegistry.isMember(SchemaConstants.ORG_DEFAULT));
		assertTrue(relationRegistry.isMember(unqualify(SchemaConstants.ORG_DEFAULT)));
		assertTrue(relationRegistry.isMember(null));
		assertTrue(relationRegistry.isMember(SchemaConstants.ORG_MANAGER));
		assertTrue(relationRegistry.isMember(unqualify(SchemaConstants.ORG_MANAGER)));
		assertFalse(relationRegistry.isMember(SchemaConstants.ORG_APPROVER));
		assertFalse(relationRegistry.isMember(unqualify(SchemaConstants.ORG_APPROVER)));

		// TODO
	}

}
