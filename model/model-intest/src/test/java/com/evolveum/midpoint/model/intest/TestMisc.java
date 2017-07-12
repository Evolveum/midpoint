/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import com.evolveum.midpoint.prism.PrismContext;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMisc extends AbstractInitializedModelIntegrationTest {
	
	public static final byte[] KEY = { 0x01, 0x02, 0x03, 0x04, 0x05 };
	
	private static final String USER_CLEAN_NAME = "clean";
	private static final String USER_CLEAN_GIVEN_NAME = "John";
	private static final String USER_CLEAN_FAMILY_NAME = "Clean";
	
	private String userCleanOid;
		
	public TestMisc() throws JAXBException {
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
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
   
        // WHEN
        displayWhen(TEST_NAME);
        RepositoryDiag diag = modelDiagnosticService.getRepositoryDiag(task, result);
        
        // THEN
        displayThen(TEST_NAME);
		display("Diag", diag);
		assertSuccess(result);

        assertEquals("Wrong implementationShortName", "SQL", diag.getImplementationShortName());
        assertNotNull("Missing implementationDescription", diag.getImplementationDescription());
        // TODO
	}
	
	@Test
    public void test110RepositorySelfTest() throws Exception {
		final String TEST_NAME = "test110RepositorySelfTest";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
   
        // WHEN
        displayWhen(TEST_NAME);
        OperationResult testResult = modelDiagnosticService.repositorySelfTest(task);
        
        // THEN
        displayThen(TEST_NAME);
		display("Repository self-test result", testResult);
        TestUtil.assertSuccess("Repository self-test result", testResult);

        // TODO: check the number of tests, etc.
	}
	
	@Test
    public void test200ExportUsers() throws Exception {
		final String TEST_NAME = "test200ExportUsers";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
   
        // WHEN
        displayWhen(TEST_NAME);
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, 
        		SelectorOptions.createCollection(ItemPath.EMPTY_PATH, GetOperationOptions.createRaw()), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertEquals("Unexpected number of users", 5, users.size());
        for (PrismObject<UserType> user: users) {
        	display("Exporting user", user);
        	assertNotNull("Null definition in "+user, user.getDefinition());
        	display("Definition", user.getDefinition());
        	String xmlString = prismContext.serializerFor(PrismContext.LANG_XML).serialize(user);
        	display("Exported user", xmlString);
        	
        	Document xmlDocument = DOMUtil.parseDocument(xmlString);
    		Schema javaxSchema = prismContext.getSchemaRegistry().getJavaxSchema();
    		Validator validator = javaxSchema.newValidator();
    		validator.setResourceResolver(prismContext.getEntityResolver());
    		validator.validate(new DOMSource(xmlDocument));
    		
    		PrismObject<Objectable> parsedUser = prismContext.parseObject(xmlString);
    		assertTrue("Re-parsed user is not equal to original: "+user, user.equals(parsedUser));
    		
        }
        
	}

	/**
	 * Just to make sure Jack is clean and that the next text will
	 * start from a clean state.
	 */
	@Test
    public void test300RecomputeJack() throws Exception {
		final String TEST_NAME = "test300RecomputeJack";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
           
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
	}
	
	/**
	 * Modify custom binary property.
	 * MID-3999
	 */
	@Test
    public void test302UpdateKeyJack() throws Exception {
		final String TEST_NAME = "test302UpdateKeyJack";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
           
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, getExtensionPath(PIRACY_KEY), task, result, KEY);
        
        // THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        PrismAsserts.assertPropertyValue(userAfter, getExtensionPath(PIRACY_KEY), KEY);
	}
	
	@Test
    public void test310AddUserClean() throws Exception {
		final String TEST_NAME = "test310AddUserClean";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = createUser(USER_CLEAN_NAME, USER_CLEAN_GIVEN_NAME, USER_CLEAN_FAMILY_NAME, true);
        
        // WHEN
        displayWhen(TEST_NAME);
        addObject(userBefore, task, result);
        
        // THEN
        displayThen(TEST_NAME);
		assertSuccess(result);
		
		userCleanOid = userBefore.getOid();

        PrismObject<UserType> userAfter = getUser(userCleanOid);
        display("User after", userAfter);
	}
	
	/**
	 * Modify custom binary property.
	 * MID-3999
	 */
	@Test(enabled=false) // MID-3999
    public void test312UpdateBinaryIdClean() throws Exception {
		final String TEST_NAME = "test312UpdateBinaryIdClean";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
           
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(userCleanOid, getExtensionPath(PIRACY_BINARY_ID), task, result, KEY);
        
        // THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userCleanOid);
        display("User after", userAfter);
        PrismAsserts.assertPropertyValue(userAfter, getExtensionPath(PIRACY_BINARY_ID), KEY);
	}

}
