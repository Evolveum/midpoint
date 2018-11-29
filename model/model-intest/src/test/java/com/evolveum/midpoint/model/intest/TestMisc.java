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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.List;

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
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMisc extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/misc");
	
	protected static final File ROLE_IMPORT_FILTERS_FILE = new File(TEST_DIR, "role-import-filters.xml");
	protected static final String ROLE_IMPORT_FILTERS_OID = "aad19b9a-d511-11e7-8bf7-cfecde275e59";
	
	protected static final File ROLE_SHIP_FILE = new File(TEST_DIR, "role-ship.xml");
	protected static final String ROLE_SHIP_OID = "bbd19b9a-d511-11e7-8bf7-cfecde275e59";
	
	protected static final File RESOURCE_SCRIPTY_FILE = new File(TEST_DIR, "resource-dummy-scripty.xml");
	protected static final String RESOURCE_SCRIPTY_OID = "399f5308-0447-11e8-91e9-a7f9c4100ffb";
	protected static final String RESOURCE_DUMMY_SCRIPTY_NAME = "scripty";

	public static final byte[] KEY = { 0x01, 0x02, 0x03, 0x04, 0x05 };

	private static final String USER_CLEAN_NAME = "clean";
	private static final String USER_CLEAN_GIVEN_NAME = "John";
	private static final String USER_CLEAN_FAMILY_NAME = "Clean";

	private String userCleanOid;
	private Integer lastDummyConnectorNumber;

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		
		initDummyResourcePirate(RESOURCE_DUMMY_SCRIPTY_NAME,
				RESOURCE_SCRIPTY_FILE, RESOURCE_SCRIPTY_OID, initTask, initResult);
		
		importObjectFromFile(ROLE_SHIP_FILE);
	}

	@Test
    public void test100GetRepositoryDiag() throws Exception {
		final String TEST_NAME = "test100GetRepositoryDiag";
        displayTestTitle(TEST_NAME);

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
        displayTestTitle(TEST_NAME);

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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null,
        		SelectorOptions.createCollection(GetOperationOptions.createRaw()), task, result);

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
        displayTestTitle(TEST_NAME);

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
        displayTestTitle(TEST_NAME);

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
        displayTestTitle(TEST_NAME);

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
	@Test
    public void test312UpdateBinaryIdClean() throws Exception {
		final String TEST_NAME = "test312UpdateBinaryIdClean";
        displayTestTitle(TEST_NAME);

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
	
	/**
     * MID-4660, MID-4491, MID-3581
     */
    @Test
    public void test320DefaultRelations() throws Exception {
		final String TEST_NAME="test320DefaultRelations";
        displayTestTitle(TEST_NAME);
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        List<RelationDefinitionType> relations = modelInteractionService.getRelationDefinitions();
        
        // THEN
 		displayThen(TEST_NAME);
 		display("Relations", relations);
        assertRelationDef(relations, SchemaConstants.ORG_MANAGER, "RelationTypes.manager");
        assertRelationDef(relations, SchemaConstants.ORG_OWNER, "RelationTypes.owner");
        assertEquals("Unexpected number of relation definitions", 7, relations.size());
    }
	
	/**
	 * MID-3879
	 */
	@Test
    public void test400ImportRoleWithFilters() throws Exception {
		final String TEST_NAME = "test400ImportRoleWithFilters";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        modelService.importObjectsFromFile(ROLE_IMPORT_FILTERS_FILE, null, task, result);

        // THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

        PrismObject<RoleType> roleAfter = getRole(ROLE_IMPORT_FILTERS_OID);
        display("Role after", roleAfter);
        
        assertInducedRole(roleAfter, ROLE_PIRATE_OID);
        assertInducedRole(roleAfter, ROLE_SAILOR_OID);
        assertInducements(roleAfter, 2);
	}
	
	@Test
    public void test500AddHocProvisioningScriptAssignJackResourceScripty() throws Exception {
		final String TEST_NAME = "test500AddHocProvisioningScriptAssignJackResourceScripty";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);

        // WHEN
        displayWhen(TEST_NAME);
        assignAccountToUser(USER_JACK_OID, RESOURCE_SCRIPTY_OID, null, task, result);

        // THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        
        assertDummyAccount(RESOURCE_DUMMY_SCRIPTY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_SCRIPTY_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
        		"Mr. POLY JACK SPARROW");
	}
	
	/**
	 * MID-3044
	 */
	@Test
    public void test502GetAccountJackResourceScripty() throws Exception {
		final String TEST_NAME = "test502GetAccountJackResourceScripty";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        String accountOid = getSingleLinkOid(userBefore);

        // WHEN
        displayWhen(TEST_NAME);
        PrismObject<ShadowType> accountShadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        // THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

		assertAttribute(getDummyResourceObject(RESOURCE_DUMMY_SCRIPTY_NAME), accountShadow.asObjectable(), 
				getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME), 
				"Dummy Resource: Scripty");
		lastDummyConnectorNumber = ShadowUtil.getAttributeValue(accountShadow, 
				getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME));
	}
	
	/**
	 * Check that the same connector instance is used. The connector should be pooled and cached.
	 * MID-3104
	 */
	@Test
    public void test504GetAccountJackResourceScriptyAgain() throws Exception {
		final String TEST_NAME = "test504GetAccountJackResourceScriptyAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        String accountOid = getSingleLinkOid(userBefore);

        // WHEN
        displayWhen(TEST_NAME);
        PrismObject<ShadowType> accountShadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        // THEN
        displayThen(TEST_NAME);
		assertSuccess(result);

		assertAttribute(getDummyResourceObject(RESOURCE_DUMMY_SCRIPTY_NAME), accountShadow.asObjectable(), 
				getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME), 
				"Dummy Resource: Scripty");
		Integer dummyConnectorNumber = ShadowUtil.getAttributeValue(accountShadow, 
				getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME));
		assertEquals("Connector number has changed", lastDummyConnectorNumber, dummyConnectorNumber);
	}
	
	/**
	 * Modify resource (but make sure that connector configuration is the same).
	 * Make just small an unimportant change in the connector. That should increase the version
	 * number which should purge all the caches. Therefore a new connector instance should be used
	 * (new connector instance number).
	 * The problem with MID-3104 was, that midPoint caches got purged. But as the configuration
	 * of old and new connector was the same, then ConnId assumed that it is still the same
	 * connector and reused the pooled instances.
	 * MID-3104
	 */
	@Test(enabled = false)
    public void test506ModifyResourceGetAccountJackResourceScripty() throws Exception {
		final String TEST_NAME = "test506ModifyResourceGetAccountJackResourceScripty";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        String accountOid = getSingleLinkOid(userBefore);
        PrismObject<ResourceType> resourceBefore = getObject(ResourceType.class, RESOURCE_SCRIPTY_OID);
        display("Resource version before", resourceBefore.getVersion());

        // WHEN
        displayWhen(TEST_NAME);
        modifyObjectReplaceProperty(ResourceType.class, RESOURCE_SCRIPTY_OID, 
        		ResourceType.F_DESCRIPTION, null, task, result, "Whatever");

        // THEN
        displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<ResourceType> resourceAfter = getObject(ResourceType.class, RESOURCE_SCRIPTY_OID);
        display("Resource version after", resourceAfter.getVersion());
        assertFalse("Resource version is still the same: "+resourceAfter.getVersion(), resourceBefore.getVersion().equals(resourceAfter.getVersion()));
		
        PrismObject<ShadowType> accountShadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

		Integer dummyConnectorNumber = ShadowUtil.getAttributeValue(accountShadow, 
				getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME));
		assertFalse("Connector number is still the same: "+dummyConnectorNumber,
				lastDummyConnectorNumber.equals(dummyConnectorNumber));
	}
	

	
	/**
	 * MID-4504
	 * midpoint.getLinkedShadow fails recomputing without throwing exception during shadow delete
	 * 
	 * the ship attribute in the role "Ship" has mapping with calling midpoint.getLinkedShadow() on the reosurce which doesn't exist
	 */
	@Test
	public void test600jackAssignRoleShip() throws Exception {
		final String TEST_NAME = "test600jackAssignRoleShip";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
       

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_SHIP_OID);
        
        //THEN
        displayThen(TEST_NAME);
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User before", userAfter);
        assertAssignments(userAfter, 2);
        assertLinks(userAfter, 1);
        
        PrismReference linkRef = userAfter.findReference(UserType.F_LINK_REF);
	    assertTrue(!linkRef.isEmpty());
	            
//	    PrismObject<ShadowType> shadowModel = getShadowModel(linkRef.getOid());
	      
	    assertDummyAccountAttribute(RESOURCE_DUMMY_SCRIPTY_NAME, USER_JACK_USERNAME, "ship", "ship");
        
	}
	
	@Test
	public void test601jackUnassignResourceAccount() throws Exception {
		final String TEST_NAME = "test601jackUnassignResourceAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_SCRIPTY_OID, null);
        
        //THEN
        displayThen(TEST_NAME);
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertLinks(userAfter, 1);
	}
	
	
	/**
	 * MID-4504
	 * midpoint.getLinkedShadow fails recomputing without throwing exception during shadow delete
	 * 
	 * first assign role ship, the ship attribute in the role has mapping with calling midpoint.getLinkedShadow()
	 */
	@Test
	public void test602jackUnssigndRoleShip() throws Exception {
		final String TEST_NAME = "test602jackUnssigndRoleShip";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
       

        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_SHIP_OID);
        
        //THEN
        displayThen(TEST_NAME);
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User before", userAfter);
        assertAssignments(userAfter, 0);
        assertLinks(userAfter, 0);
        
	}

}
