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

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;

import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDiag extends AbstractInitializedModelIntegrationTest {
		
	public TestDiag() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
	}

	@Test
    public void test100GetRepositoryDiag() throws Exception {
		final String TEST_NAME = "test100GetRepositoryDiag";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
   
        // WHEN
        RepositoryDiag diag = modelDiagnosticService.getRepositoryDiag(task, result);
        
        // THEN
		display("Diag", diag);
		result.computeStatus();
        IntegrationTestTools.assertSuccess("getRepositoryDiag result", result);

        assertEquals("Wrong implementationShortName", "SQL", diag.getImplementationShortName());
        assertNotNull("Missing implementationDescription", diag.getImplementationDescription());
        // TODO
	}
	
	@Test
    public void test200RepositorySelfTest() throws Exception {
		final String TEST_NAME = "test200RepositorySelfTest";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
   
        // WHEN
        OperationResult testResult = modelDiagnosticService.repositorySelfTest(task);
        
        // THEN
		display("Repository self-test result", testResult);
        IntegrationTestTools.assertSuccess("Repository self-test result", testResult);

        // TODO: check the number of tests, etc.
	}

}
