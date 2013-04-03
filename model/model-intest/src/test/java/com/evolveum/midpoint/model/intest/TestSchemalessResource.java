/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertTestResourceFailure;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertTestResourceSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertNotNull;

import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * Test if a resource without a schema can pass basic operations such as getObject and testResource.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSchemalessResource extends AbstractInitializedModelIntegrationTest {
	
	private static String accountOid;
	
	public TestSchemalessResource() throws JAXBException {
		super();
	}
	
	/**
	 * Just test if this does not die on an exception.
	 */
	@Test
    public void test001GetObject() throws Exception {
        displayTestTile(this, "test001GetObject");

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + ".test001GetObject");
        OperationResult result = task.getResult();
        
        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_SCHEMALESS_OID, null, task, result);
        
        // THEN
        assertNotNull("Null resource returned", resource);
	}
	
	@Test
    public void test002TestConnection() throws Exception {
        displayTestTile(this, "test002TestConnection");

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + ".test002TestConnection");
        
        // WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_SCHEMALESS_OID, task);
        
        // THEN
		display("Test result", testResult);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONNECTOR_INITIALIZATION);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONFIGURATION_VALIDATION);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONNECTOR_CONNECTION);
		assertTestResourceFailure(testResult, ConnectorTestOperation.CONNECTOR_SCHEMA);

	}
	
}
