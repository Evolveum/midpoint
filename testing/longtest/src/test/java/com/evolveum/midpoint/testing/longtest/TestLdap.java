package com.evolveum.midpoint.testing.longtest;
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


import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Mix of various tests for issues that are difficult to replicate using dummy resources.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-longtest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdap extends AbstractModelIntegrationTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ldap");
	
	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	protected static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";
	
	protected static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";
	
	protected static final File ROLE_PIRATE_FILE = new File(TEST_DIR, "role-pirate.xml");
	protected static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556666";
	
	protected static final File RESOURCE_OPENDJ_FILE = new File(COMMON_DIR, "resource-opendj.xml");
    protected static final String RESOURCE_OPENDJ_NAME = "Localhost OpenDJ";
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File USER_BARBOSSA_FILE = new File(COMMON_DIR, "user-barbossa.xml");
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
	protected static final String USER_BARBOSSA_USERNAME = "barbossa";
	protected static final String USER_BARBOSSA_FULL_NAME = "Hector Barbossa";
	
	protected static final File USER_GUYBRUSH_FILE = new File (COMMON_DIR, "user-guybrush.xml");
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	protected static final String USER_GUYBRUSH_USERNAME = "guybrush";
	protected static final String USER_GUYBRUSH_FULL_NAME = "Guybrush Threepwood";
	
	private static final String USER_LECHUCK_NAME = "lechuck";
	private static final String ACCOUNT_LECHUCK_NAME = "lechuck";
	private static final String ACCOUNT_CHARLES_NAME = "charles";
	
    protected static final File TASK_DELETE_OPENDJ_SHADOWS_FILE = new File(TEST_DIR, "task-delete-opendj-shadows.xml");
    protected static final String TASK_DELETE_OPENDJ_SHADOWS_OID = "412218e4-184b-11e5-9c9b-3c970e467874";
    
    protected static final File TASK_DELETE_OPENDJ_ACCOUNTS_FILE = new File(TEST_DIR, "task-delete-opendj-accounts.xml");
    protected static final String TASK_DELETE_OPENDJ_ACCOUNTS_OID = "b22c5d72-18d4-11e5-b266-001e8c717e5b";
    
    public static final String DOT_JPG_FILENAME = "src/test/resources/common/dot.jpg";
	
    private static final int NUM_INITIAL_USERS = 3;
	// Make it at least 1501 so it will go over the 3000 entries size limit
	private static final int NUM_LDAP_ENTRIES = 1600;

	private static final String LDAP_GROUP_PIRATES_DN = "cn=Pirates,ou=groups,dc=example,dc=com";
	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;

    @Autowired
    private ReconciliationTaskHandler reconciliationTaskHandler;
	
    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() throws Exception {
        //end profiling
        ProfilingDataManager.getInstance().printMapAfterTest();
        ProfilingDataManager.getInstance().stopProfilingAfterTest();

        openDJController.stop();
    }

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		modelService.postInit(initResult);
		
		// System Configuration
        PrismObject<SystemConfigurationType> config;
		try {
			config = repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}

        // to get profiling facilities (until better API is available)
//        LoggingConfigurationManager.configure(
//                ProfilingConfigurationManager.checkSystemProfilingConfiguration(config),
//                config.asObjectable().getVersion(), initResult);

        // administrator
		PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
		login(userAdministrator);
		
		// Users
		repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
		repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);
		
		// Roles
		repoAddObjectFromFile(ROLE_PIRATE_FILE, initResult);
		
		// Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        //initProfiling - start
        ProfilingDataManager profilingManager = ProfilingDataManager.getInstance();

        Map<ProfilingDataManager.Subsystem, Boolean> subsystems = new HashMap<>();
        subsystems.put(ProfilingDataManager.Subsystem.MODEL, true);
        subsystems.put(ProfilingDataManager.Subsystem.REPOSITORY, true);
        profilingManager.configureProfilingDataManagerForTest(subsystems, true);

        profilingManager.appendProfilingToTest();
        //initProfiling - end

		display("initial LDAP content", openDJController.dumpEntries());
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);

        assertUsers(NUM_INITIAL_USERS);
	}
	
	/**
	 * Barbossa is already member of LDAP group "pirates". The role adds this group as well.
	 * This should go smoothly. No error expected.
	 */
	@Test
    public void test200AssignRolePiratesToBarbossa() throws Exception {
		final String TEST_NAME = "test200AssignRolePiratesToBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_BARBOSSA_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        String accountDn = assertOpenDjAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true).getDN().toString();
        openDJController.assertUniqueMember(LDAP_GROUP_PIRATES_DN, accountDn);
        
        assertUsers(NUM_INITIAL_USERS);
	}
	
	/**
	 * Just a first step for the following test.
	 * Also, Guybrush has a photo. Check that binary property mapping works.
	 */
	@Test
    public void test202AssignLdapAccountToGuybrush() throws Exception {
		final String TEST_NAME = "test202AssignLdapAccountToGuybrush";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        byte[] photoIn = Files.readAllBytes(Paths.get(DOT_JPG_FILENAME));
		display("Photo in", MiscUtil.binaryToHex(photoIn));
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_JPEG_PHOTO, task, result, 
        		photoIn);
        
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
        		UserType.F_JPEG_PHOTO, GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
		PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, options, task, result);
        display("User before", userBefore);
        byte[] userJpegPhotoBefore = userBefore.asObjectable().getJpegPhoto();
        assertEquals("Photo byte length changed (user before)", photoIn.length, userJpegPhotoBefore.length);
		assertTrue("Photo bytes do not match (user before)", Arrays.equals(photoIn, userJpegPhotoBefore));
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_GUYBRUSH_OID, RESOURCE_OPENDJ_OID, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertOpenDjAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true);
        byte[] jpegPhotoLdap = OpenDJController.getAttributeValueBinary(entry, "jpegPhoto");
		assertNotNull("No jpegPhoto in LDAP entry", jpegPhotoLdap);
		assertEquals("Byte length changed (LDAP)", photoIn.length, jpegPhotoLdap.length);
		assertTrue("Bytes do not match (LDAP)", Arrays.equals(photoIn, jpegPhotoLdap));
		
		PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, options, task, result);
		display("User after", userAfter);
		String accountOid = getSingleLinkOid(userAfter);
		PrismObject<ShadowType> shadow = getShadowModel(accountOid);
		
		PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		QName jpegPhotoQName = new QName(RESOURCE_OPENDJ_NAMESPACE, "jpegPhoto");
		PrismProperty<byte[]> jpegPhotoAttr = attributesContainer.findProperty(jpegPhotoQName);
		byte[] photoBytesOut = jpegPhotoAttr.getValues().get(0).getValue();
		
		display("Photo bytes out", MiscUtil.binaryToHex(photoBytesOut));
		
		assertEquals("Photo byte length changed (shadow)", photoIn.length, photoBytesOut.length);
		assertTrue("Photo bytes do not match (shadow)", Arrays.equals(photoIn, photoBytesOut));
		
		assertUsers(NUM_INITIAL_USERS);
	}

	/**
	 * Add guybrush to LDAP group before he gets the role. Make sure that the DN in the uniqueMember
	 * attribute does not match (wrong case). This will check matching rule implementation in provisioning.
	 */
	@Test
    public void test204AssignRolePiratesToGuybrush() throws Exception {
		final String TEST_NAME = "test204AssignRolePiratesToGuybrush";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        openDJController.executeLdifChange(
        		"dn: cn=Pirates,ou=groups,dc=example,dc=com\n" +
        		"changetype: modify\n" +
        		"add: uniqueMember\n" +
        		"uniqueMember: uid=GuyBrush,ou=pEOPle,dc=EXAMPLE,dc=cOm"
        	);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_PIRATE_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        String accountDn = assertOpenDjAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, true).getDN().toString();
        openDJController.assertUniqueMember(LDAP_GROUP_PIRATES_DN, accountDn);
        
        assertUsers(NUM_INITIAL_USERS);
	}

	
	@Test
    public void test400RenameLeChuckConflicting() throws Exception {
		final String TEST_NAME = "test400RenameLeChuckConflicting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userLechuck = createUser(USER_LECHUCK_NAME, "LeChuck", true);
        userLechuck.asObjectable().getAssignment().add(createAccountAssignment(RESOURCE_OPENDJ_OID, null));
        userLechuck.asObjectable().setFamilyName(PrismTestUtil.createPolyStringType("LeChuck"));
        addObject(userLechuck);
        String userLechuckOid = userLechuck.getOid();
        
        PrismObject<ShadowType> accountCharles = createAccount(resourceOpenDj, toDn(ACCOUNT_CHARLES_NAME), true);
        addAttributeToShadow(accountCharles, resourceOpenDj, "sn", "Charles");
        addAttributeToShadow(accountCharles, resourceOpenDj, "cn", "Charles L. Charles");
        addObject(accountCharles);
        
        // preconditions
        assertOpenDjAccount(ACCOUNT_LECHUCK_NAME, "LeChuck", true);
        assertOpenDjAccount(ACCOUNT_CHARLES_NAME, "Charles L. Charles", true);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(userLechuckOid, UserType.F_NAME, task, result,
        		PrismTestUtil.createPolyString(ACCOUNT_CHARLES_NAME));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertOpenDjAccount(ACCOUNT_CHARLES_NAME, "Charles L. Charles", true);
        assertOpenDjAccount(ACCOUNT_CHARLES_NAME + "1", "LeChuck", true);
        assertNoOpenDjAccount(ACCOUNT_LECHUCK_NAME);
        
        assertUsers(NUM_INITIAL_USERS + 1);
	}
	
	@Test
    public void test800BigLdapSearch() throws Exception {
		final String TEST_NAME = "test800BigLdapSearch";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        
        assertUsers(NUM_INITIAL_USERS + 1);
        
        loadEntries("a");
        
        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, 
        		new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), prismContext);
        
        final MutableInt count = new MutableInt(0);
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> shadow, OperationResult parentResult) {
				count.increment();
				display("Found",shadow);
				return true;
			}
		};
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.searchObjectsIterative(ShadowType.class, query, handler, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        assertEquals("Unexpected number of search results", NUM_LDAP_ENTRIES + 8, count.getValue());
        
        assertUsers(NUM_INITIAL_USERS + 1);
	}

	@Test
    public void test810BigImport() throws Exception {
		final String TEST_NAME = "test810BigImport";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        
        assertUsers(NUM_INITIAL_USERS + 1);
        
        loadEntries("u");
        
        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        //task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, 2);
        modelService.importFromResource(RESOURCE_OPENDJ_OID, 
        		new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        
        waitForTaskFinish(task, true, 20000 + NUM_LDAP_ENTRIES*2000);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        display("Users", userCount);
        assertEquals("Unexpected number of users", 2*NUM_LDAP_ENTRIES + 8, userCount);
	}

    @Test
    public void test820BigReconciliation() throws Exception {
        final String TEST_NAME = "test820BigReconciliation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN

        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

//        System.out.println("openDJController.isRunning = " + openDJController.isRunning());
//        OperationResult testResult = modelService.testResource(RESOURCE_OPENDJ_OID, task);
//        System.out.println("Test resource result = " + testResult.debugDump());

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        //task.setExtensionPropertyValue(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS, 2);

        ResourceType resource = modelService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result).asObjectable();
        reconciliationTaskHandler.launch(resource,
                new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
//        OperationResult subresult = result.getLastSubresult();
//        TestUtil.assertInProgress("reconciliation launch result", subresult);

        waitForTaskFinish(task, true, 20000 + NUM_LDAP_ENTRIES*2000);

        // THEN
        TestUtil.displayThen(TEST_NAME);

        int userCount = modelService.countObjects(UserType.class, null, null, task, result);
        display("Users", userCount);
        assertEquals("Unexpected number of users", 2*NUM_LDAP_ENTRIES + 8, userCount);
    }

    @Test
    public void test900DeleteShadows() throws Exception {
        final String TEST_NAME = "test900DeleteShadows";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN

        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        rememberShadowFetchOperationCount();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(TASK_DELETE_OPENDJ_SHADOWS_FILE);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskFinish(TASK_DELETE_OPENDJ_SHADOWS_OID, true, 20000 + NUM_LDAP_ENTRIES*2000);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        assertShadowFetchOperationCountIncrement(0);
        
        
        
        PrismObject<TaskType> deleteTask = getTask(TASK_DELETE_OPENDJ_SHADOWS_OID);
        OperationResultType deleteTaskResultType = deleteTask.asObjectable().getResult();
        display("Final delete task result", deleteTaskResultType);
        TestUtil.assertSuccess(deleteTaskResultType);
        OperationResult deleteTaskResult = OperationResult.createOperationResult(deleteTaskResultType);
        TestUtil.assertSuccess(deleteTaskResult);
        List<OperationResult> opExecResults = deleteTaskResult.findSubresults(ModelService.EXECUTE_CHANGES);
        assertEquals(1, opExecResults.size());
        OperationResult opExecResult = opExecResults.get(0);
        TestUtil.assertSuccess(opExecResult);
        assertEquals("Wrong exec operation count", 2*NUM_LDAP_ENTRIES+8, opExecResult.getCount());
        assertTrue("Too many subresults: "+deleteTaskResult.getSubresults().size(), deleteTaskResult.getSubresults().size() < 10);
        
        assertOpenDjAccountShadows(0, true, task, result);
        assertUsers(2*NUM_LDAP_ENTRIES + 8);
        
        // Check that the actual accounts were NOT deleted
        // (This also re-creates shadows)
        assertOpenDjAccountShadows(2*NUM_LDAP_ENTRIES+8, false, task, result);
    }
    
    @Test
    public void test910DeleteAccounts() throws Exception {
        final String TEST_NAME = "test910DeleteAccounts";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestLdap.class.getName() + "." + TEST_NAME);
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        
        rememberShadowFetchOperationCount();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        importObjectFromFile(TASK_DELETE_OPENDJ_ACCOUNTS_FILE);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        waitForTaskFinish(TASK_DELETE_OPENDJ_ACCOUNTS_OID, true, 20000 + NUM_LDAP_ENTRIES*3000);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        
        assertShadowFetchOperationCountIncrement((2*NUM_LDAP_ENTRIES)/100+2);
        
        
        PrismObject<TaskType> deleteTask = getTask(TASK_DELETE_OPENDJ_SHADOWS_OID);
        OperationResultType deleteTaskResultType = deleteTask.asObjectable().getResult();
        display("Final delete task result", deleteTaskResultType);
        TestUtil.assertSuccess(deleteTaskResultType);
        OperationResult deleteTaskResult = OperationResult.createOperationResult(deleteTaskResultType);
        TestUtil.assertSuccess(deleteTaskResult);
        List<OperationResult> opExecResults = deleteTaskResult.findSubresults(ModelService.EXECUTE_CHANGES);
        assertEquals(1, opExecResults.size());
        OperationResult opExecResult = opExecResults.get(0);
        TestUtil.assertSuccess(opExecResult);
        assertEquals("Wrong exec operation count", 2*NUM_LDAP_ENTRIES + 8, opExecResult.getCount());
        assertTrue("Too many subresults: "+deleteTaskResult.getSubresults().size(), deleteTaskResult.getSubresults().size() < 10);
        
        assertOpenDjAccountShadows(1, true, task, result);
        assertUsers(2*NUM_LDAP_ENTRIES + 8);
        assertOpenDjAccountShadows(1, false, task, result);
    }
    
    private void assertOpenDjAccountShadows(int expected, boolean raw, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
    	ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, 
        		new QName(RESOURCE_OPENDJ_NAMESPACE, "inetOrgPerson"), prismContext);
        
        final MutableInt count = new MutableInt(0);
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> shadow, OperationResult parentResult) {
				count.increment();
				display("Found",shadow);
				return true;
			}
		};
		Collection<SelectorOptions<GetOperationOptions>> options = null;
		if (raw) {
			options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
		}
		modelService.searchObjectsIterative(ShadowType.class, query, handler, options, task, result);
        assertEquals("Unexpected number of search results (raw="+raw+")", expected, count.getValue());
	}

	private void loadEntries(String prefix) throws LDIFException, IOException {
        long ldapPopStart = System.currentTimeMillis();
        
        for(int i=0; i < NUM_LDAP_ENTRIES; i++) {
        	String name = "user"+i;
        	Entry entry = createEntry(prefix+i, name);
        	openDJController.addEntry(entry);
        }
        
        long ldapPopEnd = System.currentTimeMillis();
        
        display("Loaded "+NUM_LDAP_ENTRIES+" LDAP entries (prefix "+prefix+") in "+((ldapPopEnd-ldapPopStart)/1000)+" seconds");
	}

	private Entry createEntry(String uid, String name) throws IOException, LDIFException {
		StringBuilder sb = new StringBuilder();
		String dn = "uid="+uid+","+openDJController.getSuffixPeople();
		sb.append("dn: ").append(dn).append("\n");
		sb.append("objectClass: inetOrgPerson\n");
		sb.append("uid: ").append(uid).append("\n");
		sb.append("cn: ").append(name).append("\n");
		sb.append("sn: ").append(name).append("\n");
		LDIFImportConfig importConfig = new LDIFImportConfig(IOUtils.toInputStream(sb.toString(), "utf-8"));
        LDIFReader ldifReader = new LDIFReader(importConfig);
        Entry ldifEntry = ldifReader.readEntry();
		return ldifEntry;
	}
	
	private String toDn(String username) {
		return "uid="+username+","+OPENDJ_PEOPLE_SUFFIX;
	}
}
