package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2014-2017 Evolveum
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

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestVillage extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "village");
	
	public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");
	
	public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
	public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000000222";
		
	protected static final File RESOURCE_DUMMY_SOURCE_FILE = new File(TEST_DIR, "resource-dummy-source.xml");
	protected static final String RESOURCE_DUMMY_SOURCE_ID = "SRC";
	protected static final String RESOURCE_DUMMY_SOURCE_OID = "10000000-0000-0000-0000-000000000001";
	protected static final String RESOURCE_DUMMY_SOURCE_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	protected static final QName OPENDJ_ASSOCIATION_GROUP_NAME = new QName(RESOURCE_OPENDJ_NAMESPACE, "group"); 
	
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_SRC_FIRST_NAME = "firstname";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_SRC_LAST_NAME = "lastname";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_SRC_TYPE = "type";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_SRC_LOC = "loc";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_SRC_ORG = "org";
	
	public static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
	public static final String ROLE_BASIC_OID = "10000000-0000-0000-0000-000000000601";
	public static final String ROLE_BASIC_NAME = "Basic";
	
	public static final File ROLE_SIMPLE_FILE = new File(TEST_DIR, "role-account-construction.xml");
	public static final String ROLE_SIMPLE_OID = "10000000-0000-0000-0000-000000000602";
	public static final String ROLE_SIMPLE_NAME = "Simple account construction";
	
	public static final File ROLE_META_FUNCTIONAL_ORG_FILE = new File(TEST_DIR, "role-meta-functional-org.xml");
	public static final String ROLE_META_FUNCTIONAL_ORG_OID = "74aac2c8-ca0f-11e3-bb29-001e8c717e5b";
	
	public static final File ROLE_META_PROJECT_ORG_FILE = new File(TEST_DIR, "role-meta-project-org.xml");
	public static final String ROLE_META_PROJECT_ORG_OID = "ab33ec1e-0c0b-11e4-ba88-001e8c717e5b";
	
	protected static final File ORGS_FILE = new File(TEST_DIR, "orgs.xml");
	public static final String ORG_GOV_NAME = "Gov";
	public static final String ORG_EXEC_NAME = "Exec";
	public static final String ORG_INFRA_NAME = "Infra";

	public static final String ORG_INFRA_OID = "00000000-8888-6666-0000-100000000004";
	private static final String GLOBAL_PASSWORD_POLICY_OID = "81818181-76e0-59e2-8888-3d4f02d3fffc";
	private static final String ORG_PASSWORD_POLICY_OID = "81818181-76e0-59e2-8888-3d4f02d3fffe";
	private static final File GLOBAL_PASSWORD_POLICY_FILE = new File(TEST_DIR, "global-password-policy.xml");
	private static final File ORG_PASSWORD_POLICY_FILE = new File(TEST_DIR, "org-password-policy.xml");
	
	public static final File ORG_PROJECT_JOLLY_ROGER_FILE = new File(TEST_DIR, "org-project-jolly-roger.xml");
	public static final String ORG_PROJECT_JOLLY_ROGER_OID = "a9ac1aa2-0c0f-11e4-9214-001e8c717e5b";
	
	protected static final File TASK_LIVE_SYNC_DUMMY_SOURCE_FILE = new File(TEST_DIR, "task-dumy-source-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_SOURCE_OID = "10000000-0000-0000-5555-555500000001";
	
	private static final File USER_MIKE_FILE = new File(COMMON_DIR, "user-mike.xml");
	private static final String USER_MIKE_OID = "c0c010c0-d34d-b33f-f00d-222333111111";

	private static final File USER_MURRAY_FILE = new File(TEST_DIR, "user-murray.xml");
	private static final String USER_MURRAY_OID = "c0c010c0-d34d-b33f-f00d-1111111111aa";

	private static final String ACCOUNT_HERMAN_USERNAME = "ht";
	private static final String ACCOUNT_HERMAN_FIST_NAME = "Herman";
	private static final String ACCOUNT_HERMAN_LAST_NAME = "Toothrot";
	private static final String ACCOUNT_HERMAN_LOC = "Monkey Island";
	private static final String ACCOUNT_HERMAN_ORG = "Gov";
	private static final String USER_HERMAN_NAME = "G."+ACCOUNT_HERMAN_FIST_NAME+"."+ACCOUNT_HERMAN_LAST_NAME;
	
	private static final String ACCOUNT_LEMONHEAD_USERNAME = "lemonhead";
	private static final String ACCOUNT_LEMONHEAD_FIST_NAME = "Lemonhead";
	private static final String ACCOUNT_LEMONHEAD_LAST_NAME = "Canibal";
	private static final String ACCOUNT_LEMONHEAD_LOC = "Monkey Island";
	private static final String ACCOUNT_LEMONHEAD_ORG = "Exec";
	private static final String USER_LEMONHEAD_NAME = "E."+ACCOUNT_LEMONHEAD_FIST_NAME+"."+ACCOUNT_LEMONHEAD_LAST_NAME;

	private static final String ACCOUNT_SHARPTOOTH_USERNAME = "sharptooth";
	private static final String ACCOUNT_SHARPTOOTH_FIST_NAME = "Sharptooth";
	private static final String ACCOUNT_SHARPTOOTH_LAST_NAME = "Canibal";

	private static final String ACCOUNT_GUYBRUSH_USERNAME = "guybrush";
	private static final String ACCOUNT_GUYBRUSH_FIST_NAME = "Guybrush";
	private static final String ACCOUNT_GUYBRUSH_LAST_NAME = "Threepwood";
	
	private static final String ACCOUNT_MANCOMB_USERNAME = "mancomb";
	private static final String ACCOUNT_MANCOMB_FIST_NAME = "Mancomb";
	private static final String ACCOUNT_MANCOMB_LAST_NAME = "Seepgood";
	private static final String ACCOUNT_MANCOMB_LOC = "-";
	private static final String ACCOUNT_MANCOMB_ORG = "-";
	private static final String USER_MANCOMB_NAME = ACCOUNT_MANCOMB_FIST_NAME+"."+ACCOUNT_MANCOMB_LAST_NAME;
	
	private static final String ACCOUNT_COBB_USERNAME = "cobb";
	private static final String ACCOUNT_COBB_FIST_NAME = "Cobb";
	private static final String ACCOUNT_COBB_LAST_NAME = "Loom";

	private static final String ACCOUNT_LARGO_USERNAME = "largo";
	private static final String ACCOUNT_LARGO_FIST_NAME = "Largo";
	private static final String ACCOUNT_LARGO_LAST_NAME = "LaGrande";
	
	private static final String ACCOUNT_STAN_USERNAME = "stan";
	private static final String ACCOUNT_STAN_FIST_NAME = "Stan";
	private static final String ACCOUNT_STAN_LAST_NAME = "Salesman";
	
	private static final String ACCOUNT_CAPSIZE_USERNAME = "capsize";
	private static final String ACCOUNT_CAPSIZE_FIST_NAME = "Kate";
	private static final String ACCOUNT_CAPSIZE_LAST_NAME = "Capsize";
	
	private static final String ACCOUNT_WALLY_USERNAME = "wally";
	private static final String ACCOUNT_WALLY_FIST_NAME = "Wally";
	private static final String ACCOUNT_WALLY_LAST_NAME = "Feed";
	private static final String USER_WALLY_NAME = ACCOUNT_WALLY_FIST_NAME+"."+ACCOUNT_WALLY_LAST_NAME;
	
	private static final String ACCOUNT_AUGUSTUS_USERNAME = "augustus";
	private static final String ACCOUNT_AUGUSTUS_FIST_NAME = "Augustus";
	private static final String ACCOUNT_AUGUSTUS_LAST_NAME = "DeWaat";
	
	private static final String ACCOUNT_ROGERSSR_USERNAME = "rogers,sr";
	private static final String ACCOUNT_ROGERSSR_FIST_NAME = "Rum";
	private static final String ACCOUNT_ROGERSSR_LAST_NAME = "Rogers, Sr.";
	
	private static final String ACCOUNT_TELEKE_USERNAME = "tőlőkë";
	private static final String ACCOUNT_TELEKE_FIST_NAME = "Félix";
	private static final String ACCOUNT_TELEKE_LAST_NAME = "Tőlőkë";
	
	private static final File GROUP_GOV_MONKEY_ISLAND_LDIF_FILE = new File(TEST_DIR, "group-gov-monkey-island.ldif");
	private static final File GROUP_EXEC_MONKEY_ISLAND_LDIF_FILE = new File(TEST_DIR, "group-exec-monkey-island.ldif");

	private static final String GROUP_OF_UNIQUE_NAMES_OBJECTCLASS_NAME = "groupOfUniqueNames";
	private static final QName GROUP_OF_UNIQUE_NAMES_OBJECTCLASS_QNAME = new QName(MidPointConstants.NS_RI, GROUP_OF_UNIQUE_NAMES_OBJECTCLASS_NAME);

	private static final String GROUP_MEMBER_ATTRIBUTE_NAME = "uniqueMember";

	private static final String GROUP_JOLLYROGERS_DN = "cn=jollyrogers,ou=groups,dc=example,dc=com";

	private static final String GROUP_PROJECT_JOLLY_ROGER_ADMIN_DN = "cn=admins,ou=Jolly Roger,dc=example,dc=com";


	protected static DummyResource dummyResourceSrc;
	protected static DummyResourceContoller dummyResourceCtlSrc;
	protected ResourceType resourceDummySrcType;
	protected PrismObject<ResourceType> resourceDummySrc;
	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;
	
	@Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() throws Exception {
        openDJController.stop();
    }
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		// Resources
		dummyResourceCtlSrc = DummyResourceContoller.create(RESOURCE_DUMMY_SOURCE_ID, resourceDummySrc);
		DummyObjectClass dummyAdAccountObjectClass = dummyResourceCtlSrc.getDummyResource().getAccountObjectClass();
		dummyResourceCtlSrc.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SRC_FIRST_NAME, String.class, false, false);
		dummyResourceCtlSrc.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SRC_LAST_NAME, String.class, false, false);
		dummyResourceCtlSrc.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SRC_TYPE, String.class, false, false);
		dummyResourceCtlSrc.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SRC_LOC, String.class, false, false);
		dummyResourceCtlSrc.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_SRC_ORG, String.class, false, false);
		dummyResourceSrc = dummyResourceCtlSrc.getDummyResource();
		resourceDummySrc = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_SOURCE_FILE, RESOURCE_DUMMY_SOURCE_OID, initTask, initResult);
		resourceDummySrcType = resourceDummySrc.asObjectable();
		dummyResourceCtlSrc.setResource(resourceDummySrc);
		dummyResourceSrc.setSyncStyle(DummySyncStyle.SMART);
		
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);
		
		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
		setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);
				
		// Role
		importObjectFromFile(ROLE_BASIC_FILE, initResult);
		importObjectFromFile(ROLE_SIMPLE_FILE, initResult);
		importObjectFromFile(ROLE_META_FUNCTIONAL_ORG_FILE, initResult);
		importObjectFromFile(ROLE_META_PROJECT_ORG_FILE, initResult);
		
		// Org
		repoAddObjectsFromFile(ORGS_FILE, OrgType.class, initResult);
		
		// LDAP content
		openDJController.addEntryFromLdifFile(GROUP_GOV_MONKEY_ISLAND_LDIF_FILE);
		openDJController.addEntryFromLdifFile(GROUP_EXEC_MONKEY_ISLAND_LDIF_FILE);

		// Tasks
		importObjectFromFile(TASK_LIVE_SYNC_DUMMY_SOURCE_FILE, initResult);
		
		InternalMonitor.setTracePrismObjectClone(true);
		
	}
	
	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        OperationResult testResultHr = modelService.testResource(RESOURCE_DUMMY_SOURCE_OID, task);
        TestUtil.assertSuccess(testResultHr);
        
        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);
        
        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        assertNotNull("No system configuration", systemConfiguration);
        display("System config", systemConfiguration);
        
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, false);
	}
	
	/**
	 * MID-3424
	 */
	@Test
    public void test020ResourceOpenDjGet() throws Exception {
		final String TEST_NAME = "test020ResourceOpenDjGet";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        rememberResourceSchemaFetchCount();
        rememberResourceSchemaParseCount();
        rememberConnectorCapabilitiesFetchCount();
        rememberConnectorInitializationCount();
        rememberConnectorSchemaParseCount();
        rememberPrismObjectCloneCount();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        // variable number of clones because of trigger scanner task
        assertPrismObjectCloneIncrement(1, 2);
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
	}
	
	/**
	 * MID-3424
	 */
	@Test
    public void test022ResourceOpenDjRefinedSchema() throws Exception {
		final String TEST_NAME = "test022ResourceOpenDjRefinedSchema";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ResourceType> resourceBefore = modelService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result);
        ResourceSchema resourceSchemaBefore = RefinedResourceSchema.getResourceSchema(resourceBefore, prismContext);
        RefinedResourceSchema refinedSchemaBefore = RefinedResourceSchema.getRefinedSchema(resourceBefore);
        
        rememberResourceSchemaFetchCount();
        rememberResourceSchemaParseCount();
        rememberConnectorCapabilitiesFetchCount();
        rememberConnectorInitializationCount();
        rememberConnectorSchemaParseCount();
        rememberPrismObjectCloneCount();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        long t0 = System.currentTimeMillis();
        PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result);
        long t1 = System.currentTimeMillis();
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long t2 = System.currentTimeMillis();
        ResourceSchema resourceSchemaAfter = RefinedResourceSchema.getResourceSchema(resourceAfter, prismContext);
        long t3 = System.currentTimeMillis();
        RefinedResourceSchema refinedSchemaAfter = RefinedResourceSchema.getRefinedSchema(resourceAfter);
        long t4 = System.currentTimeMillis();
        
        display("Times", "getObject(RESOURCE_OPENDJ_OID): "+(t1-t0)+"ms\ngetResourceSchema: "+(t3-t2)
        		+"ms\ngetRefinedSchema: "+(t4-t3)+"ms");
        
        // variable number of clones: 1 or 2 because of trigger scanner task
        assertPrismObjectCloneIncrement(1,2);
        
        assertResourceSchemaFetchIncrement(0);
        assertResourceSchemaParseCountIncrement(0);
        assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
        assertConnectorSchemaParseIncrement(0);
        
        assertTrue("Resource schema has changed", resourceSchemaBefore == resourceSchemaAfter );
        assertTrue("Refined schema has changed", refinedSchemaBefore == refinedSchemaAfter );
	}
	
	@Test
    public void test100AddSrcAccountHerman() throws Exception {
		final String TEST_NAME = "test100AddSrcAccountHerman";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_HERMAN_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_FIRST_NAME, ACCOUNT_HERMAN_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LAST_NAME, ACCOUNT_HERMAN_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LOC, ACCOUNT_HERMAN_LOC);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_ORG, ACCOUNT_HERMAN_ORG);
		
        // WHEN
        dummyResourceSrc.addAccount(newAccount);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        assertUserNoRole(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(user, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}

	@Test
    public void test101HermanAssignBasicRole() throws Exception {
		final String TEST_NAME = "test101HermanAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        
        // WHEN
        assignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserLdap(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
        assertLdapLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}
	
	@Test
    public void test102HermanUnAssignBasicRole() throws Exception {
		final String TEST_NAME = "test102HermanUnAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        
        // WHEN
        unassignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserNoRole(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
        assertNoLdapLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}
	
	@Test
    public void test103HermanAssignBasicAndSimpleRole() throws Exception {
		final String TEST_NAME = "test103HermanAssignBasicAndSimpleRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        
        // WHEN
        assignRole(user.getOid(), ROLE_SIMPLE_OID);
        assignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserLdap(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG, 2);
        assertAssignedRole(userAfter, ROLE_SIMPLE_OID);
        assertLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
        assertLdapLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}
	
	@Test
    public void test104HermanUnAssignSimpleRoleThenBasic() throws Exception {
		final String TEST_NAME = "test104HermanUnAssignSimpleRoleThenBasic";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        
        // WHEN
        unassignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertAssignedRole(userAfter, ROLE_SIMPLE_OID);
//        assertUserLdap(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
        assertNoLdapLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
        
        // WHEN
        unassignRole(user.getOid(), ROLE_SIMPLE_OID);
        
        // THEN
        userAfter = getUser(user.getOid());
        assertUserNoRole(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
        assertNoLdapLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}
	

	@Test
    public void test105ModifySrcAccountHermanRemoveLoc() throws Exception {
		final String TEST_NAME = "test105ModifySrcAccountHermanRemoveLoc";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount account = dummyResourceSrc.getAccountByUsername(ACCOUNT_HERMAN_USERNAME);
		
        // WHEN
        account.replaceAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LOC);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        assertUserNoRole(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(user, null, ACCOUNT_HERMAN_ORG);
	}
	
	@Test
    public void test106HermanAssignBasicRole() throws Exception {
		final String TEST_NAME = "test106HermanAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        
        // WHEN
        assignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserLdap(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(userAfter, null, ACCOUNT_HERMAN_ORG);
        assertNoLdapLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}
	
	@Test
    public void test107ModifySrcAccountHermanAddLoc() throws Exception {
		final String TEST_NAME = "test107ModifySrcAccountHermanAddLoc";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount account = dummyResourceSrc.getAccountByUsername(ACCOUNT_HERMAN_USERNAME);
		
        // WHEN
        account.replaceAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LOC, ACCOUNT_HERMAN_LOC);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        assertUserLdap(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(user, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
        assertLdapLocGov(user, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}
	
	@Test
    public void test108ModifySrcAccountHermanRemoveLoc() throws Exception {
		final String TEST_NAME = "test108ModifySrcAccountHermanRemoveLoc";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount account = dummyResourceSrc.getAccountByUsername(ACCOUNT_HERMAN_USERNAME);
		
        // WHEN
        account.replaceAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LOC);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        assertUserLdap(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(user, null, ACCOUNT_HERMAN_ORG);
        assertNoLdapLocGov(user, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}

	@Test
    public void test109HermanUnAssignBasicRole() throws Exception {
		final String TEST_NAME = "test109HermanUnAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        
        // WHEN
        unassignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserNoRole(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ACCOUNT_HERMAN_ORG);
        assertLocGov(userAfter, null, ACCOUNT_HERMAN_ORG);
        assertNoLdapLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}
	
	@Test
    public void test110AddSrcAccountLemonhead() throws Exception {
		final String TEST_NAME = "test110AddSrcAccountLemonhead";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_LEMONHEAD_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_FIRST_NAME, ACCOUNT_LEMONHEAD_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LAST_NAME, ACCOUNT_LEMONHEAD_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_TYPE, ROLE_BASIC_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LOC, ACCOUNT_LEMONHEAD_LOC);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_ORG, ACCOUNT_LEMONHEAD_ORG);
		
        // WHEN
        dummyResourceSrc.addAccount(newAccount);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> userAfter = findUserByUsername(USER_LEMONHEAD_NAME);
        assertUserLdap(userAfter, ACCOUNT_LEMONHEAD_FIST_NAME, ACCOUNT_LEMONHEAD_LAST_NAME, ACCOUNT_LEMONHEAD_ORG);
        assertLocGov(userAfter, ACCOUNT_LEMONHEAD_LOC, ACCOUNT_LEMONHEAD_ORG);
        assertLdapLocGov(userAfter, ACCOUNT_LEMONHEAD_LOC, ACCOUNT_LEMONHEAD_ORG);
	}
	
	/**
	 * Wally has no org. User without an org should be created.
	 */
	@Test
    public void test120AddSrcAccountWally() throws Exception {
		final String TEST_NAME = "test120AddSrcAccountWally";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_WALLY_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_FIRST_NAME, ACCOUNT_WALLY_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LAST_NAME, ACCOUNT_WALLY_LAST_NAME);
		
        // WHEN
        dummyResourceSrc.addAccount(newAccount);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> userAfter = findUserByUsername(USER_WALLY_NAME);
        assertUserNoRole(userAfter, ACCOUNT_WALLY_FIST_NAME, ACCOUNT_WALLY_LAST_NAME, null);
        assertLocGov(userAfter, null, null);
	}

	@Test
    public void test121WallyAssignBasicRole() throws Exception {
		final String TEST_NAME = "test121WallyAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_WALLY_NAME);
        
        // WHEN
        assignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserLdap(userAfter, ACCOUNT_WALLY_FIST_NAME, ACCOUNT_WALLY_LAST_NAME, null);
        assertLocGov(userAfter, null, null);
	}
	
	@Test
    public void test122WallyUnAssignBasicRole() throws Exception {
		final String TEST_NAME = "test122WallyUnAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_WALLY_NAME);
        
        // WHEN
        unassignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserNoRole(userAfter, ACCOUNT_WALLY_FIST_NAME, ACCOUNT_WALLY_LAST_NAME, null);
        assertLocGov(userAfter, null, null);
	}

	/**
	 * Wally has no org. User without an org should be created.
	 */
	@Test
    public void test130AddSrcAccountMancomb() throws Exception {
		final String TEST_NAME = "test130AddSrcAccountMancomb";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_MANCOMB_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_FIRST_NAME, ACCOUNT_MANCOMB_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LAST_NAME, ACCOUNT_MANCOMB_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_LOC, ACCOUNT_MANCOMB_LOC);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_SRC_ORG, ACCOUNT_MANCOMB_ORG);
		
        // WHEN
        dummyResourceSrc.addAccount(newAccount);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> userAfter = findUserByUsername(USER_MANCOMB_NAME);
        assertUserNoRole(userAfter, ACCOUNT_MANCOMB_FIST_NAME, ACCOUNT_MANCOMB_LAST_NAME, null);
        assertLocGov(userAfter, ACCOUNT_MANCOMB_LOC, ACCOUNT_MANCOMB_ORG);
	}

	@Test
    public void test131MancombAssignBasicRole() throws Exception {
		final String TEST_NAME = "test131WallyAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_MANCOMB_NAME);
        
        // WHEN
        assignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserLdap(userAfter, ACCOUNT_MANCOMB_FIST_NAME, ACCOUNT_MANCOMB_LAST_NAME, null);
        assertLocGov(userAfter, ACCOUNT_MANCOMB_LOC, ACCOUNT_MANCOMB_ORG);
	}
	
	@Test
    public void test132MancombUnAssignBasicRole() throws Exception {
		final String TEST_NAME = "test132MancombUnAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_MANCOMB_NAME);
        
        // WHEN
        unassignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserNoRole(userAfter, ACCOUNT_MANCOMB_FIST_NAME, ACCOUNT_MANCOMB_LAST_NAME, null);
        assertLocGov(userAfter, ACCOUNT_MANCOMB_LOC, ACCOUNT_MANCOMB_ORG);
	}

	
	/**
	 * Change of org should trigger rename
	 */
	@Test
    public void test150ModifySrcAccountHermanReplaceOrg() throws Exception {
		final String TEST_NAME = "test150ModifySrcAccountHermanReplaceOrg";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount account = dummyResourceSrc.getAccountByUsername(ACCOUNT_HERMAN_USERNAME);
		
        // WHEN
        account.replaceAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_SRC_ORG, ORG_INFRA_NAME);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(getUsername(ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ORG_INFRA_NAME));
        assertUserNoRole(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ORG_INFRA_NAME);
        assertLocGov(user, null, ORG_INFRA_NAME);
        
        PrismObject<UserType> userGone = findUserByUsername(USER_HERMAN_NAME);
        assertNull("Original herman is not gone", userGone);
	}
	
	/**
	 * Change of org should trigger rename
	 */
	@Test
    public void test152ModifySrcAccountHermanDeleteOrg() throws Exception {
		final String TEST_NAME = "test152ModifySrcAccountHermanDeleteOrg";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount account = dummyResourceSrc.getAccountByUsername(ACCOUNT_HERMAN_USERNAME);
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        account.replaceAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_SRC_ORG);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        PrismObject<UserType> user = findUserByUsername(getUsername(ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, null));
        assertUserNoRole(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, null);
        assertLocGov(user, null, null);
        
        PrismObject<UserType> userGone = findUserByUsername(USER_HERMAN_NAME);
        assertNull("Original herman is not gone", userGone);
        userGone = findUserByUsername(getUsername(ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME, ORG_INFRA_NAME));
        assertNull("First renamed herman is not gone", userGone);
	}
	
	@Test
	public void test200createUserAssignOrgPwdPolicy() throws Exception{
		
		final String TEST_NAME = "test200createUserAssignOrgPwdPolicy";
        TestUtil.displayTestTile(this, TEST_NAME);
		Task task = taskManager.createTaskInstance(TestVillage.class.getName() + "." + TEST_NAME);
		OperationResult result = new OperationResult(TEST_NAME);
		
		//prepare password policies
		addObject(GLOBAL_PASSWORD_POLICY_FILE);
		addObject(ORG_PASSWORD_POLICY_FILE);
		
		ObjectDelta orgPasswordPolicyRefDelta = ObjectDelta.createModificationAddReference(OrgType.class, ORG_INFRA_OID, OrgType.F_PASSWORD_POLICY_REF, prismContext, ORG_PASSWORD_POLICY_OID);
		
		Collection deltas = MiscUtil.createCollection(orgPasswordPolicyRefDelta);
		modelService.executeChanges(deltas, null, task, result);
		
		InternalsConfig.avoidLoggingChange = true;
		ObjectDelta sysConfigPasswordPolicyRefDelta = ObjectDelta.createModificationAddReference(SystemConfigurationType.class, SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY_REF, prismContext, GLOBAL_PASSWORD_POLICY_OID);
		deltas = MiscUtil.createCollection(sysConfigPasswordPolicyRefDelta);
		modelService.executeChanges(deltas, null, task, result);
		InternalsConfig.avoidLoggingChange = false;
		
		//add user + assign role + assign org with the password policy specified
		PrismObject<UserType> objectToAdd = PrismTestUtil.parseObject(USER_MIKE_FILE);
		ObjectDelta<UserType> addUser = ObjectDelta.createAddDelta(objectToAdd);
		
		deltas = MiscUtil.createCollection(addUser);
		//The user's password has length 4..if the policy is not chosen correctly, it fails
		modelService.executeChanges(deltas, null, task, result);

		//TODO: assert added user
		
	}
	
	@Test
	public void test201unassignRole() throws Exception{
		final String TEST_NAME = "test201unassignRole";
        TestUtil.displayTestTile(this, TEST_NAME);
		unassignRole(USER_MIKE_OID, ROLE_BASIC_OID);
		//TODO: assertions
	}
	
	@Test
	public void test202assignRoleOrgPwdPolicy() throws Exception{
		final String TEST_NAME = "test202assignRoleOrgPwdPolicy";
        TestUtil.displayTestTile(this, TEST_NAME);
		
        //this will throw exception, if incorrect pwd policy is selected...but some assertion will be nice :)
        assignRole(USER_MIKE_OID, ROLE_BASIC_OID);
		
		//TODO: assertion
	}
	
	@Test
    public void test300AddProjectJollyRoger() throws Exception {
		final String TEST_NAME = "test300AddProjectJollyRoger";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(ORG_PROJECT_JOLLY_ROGER_FILE, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> org = getObject(OrgType.class, ORG_PROJECT_JOLLY_ROGER_OID);
        display("Org", org);
        assertLinks(org, 2);
        
        Entry ouEntry = openDJController.fetchAndAssertEntry("ou=Jolly Roger,dc=example,dc=com", "organizationalUnit");
        Entry groupEntry = openDJController.fetchAndAssertEntry(GROUP_PROJECT_JOLLY_ROGER_ADMIN_DN, GROUP_OF_UNIQUE_NAMES_OBJECTCLASS_NAME);
      //TODO: more assertions
	}
	
	/**
	 * MID-3429
	 */
	@Test
    public void test310ProjectJollyRogerNestedGroup() throws Exception {
		final String TEST_NAME = "test310ProjectJollyRogerNestedGroup";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        openDJController.addEntry("dn: "+GROUP_JOLLYROGERS_DN+"\n"+
                                  "objectclass: "+GROUP_OF_UNIQUE_NAMES_OBJECTCLASS_NAME+"\n"+
        		                  "cn: jollyrogers\n"+
        		                  GROUP_MEMBER_ATTRIBUTE_NAME+": "+GROUP_PROJECT_JOLLY_ROGER_ADMIN_DN+"\n");
		
        display("LDAP entries", openDJController.dumpEntries());
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassFilterPrefix(RESOURCE_OPENDJ_OID, GROUP_OF_UNIQUE_NAMES_OBJECTCLASS_QNAME, prismContext)
				.and().itemWithDef(
						new PrismPropertyDefinitionImpl<>(new QName(RESOURCE_OPENDJ_NAMESPACE, "cn"), DOMUtil.XSD_STRING, prismContext),
						ShadowType.F_ATTRIBUTES, new QName(RESOURCE_OPENDJ_NAMESPACE, "cn")).eq("admins")
				.build();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		// TODO: search for cn=admins,ou=Jolly Roger,dc=example,dc=com
        SearchResultList<PrismObject<ShadowType>> groupShadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("groupShadows", groupShadows);
        assertEquals("Wrong number of shadows found", 1, groupShadows.size());
        PrismObject<ShadowType> groupShadow = groupShadows.get(0);
        List<ShadowAssociationType> associations = groupShadow.asObjectable().getAssociation();
        // MID-3430, MID-3429
//        assertEquals("Wrong number of associations in "+groupShadow, 1, associations.size());
	}
	
	@Test
    public void test319DeleteProjectJollyRoger() throws Exception {
		final String TEST_NAME = "test319DeleteProjectJollyRoger";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteObject(OrgType.class, ORG_PROJECT_JOLLY_ROGER_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoObject(OrgType.class, ORG_PROJECT_JOLLY_ROGER_OID, task, result);
        openDJController.assertNoEntry("ou=Jolly Roger,dc=example,dc=com");
        openDJController.assertNoEntry("cn=admins,ou=Jolly Roger,dc=example,dc=com");
	}
	
	/**
	 * User is added to repo directly, so he does not have OID in employee number.
	 * Recompute should fix that. This is a migration scenario.
	 */
	@Test
    public void test350AddRepoUserNoEmployeeNumberRecompute() throws Exception {
		final String TEST_NAME = "test350AddRepoUserNoEmployeeNumberRecompute";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_MURRAY_FILE);
        repositoryService.addObject(user, null, result);
 
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        recomputeUser(USER_MURRAY_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        user = getUser(USER_MURRAY_OID);
        assertEmployeeNumber(user);        
	}
	
	private void assertLocGov(PrismObject<UserType> user, String expLoc, String expOrg) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		UserType userType = user.asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong locality in "+user, expLoc, userType.getLocality());
		if (expOrg == null) {
			assertTrue("Unexpected organization in "+user+": "+userType.getOrganization(), userType.getOrganization().isEmpty());
		} else {
			PrismAsserts.assertEqualsCollectionUnordered("Wrong organization in "+user, userType.getOrganization(), 
				PrismTestUtil.createPolyStringType(expOrg));
		}
		if (expLoc == null || expOrg == null) {
			assertNull("Wrong costCenter in "+user+", expected empty but was "+userType.getCostCenter(), userType.getCostCenter());
		} else {
			assertEquals("Wrong costCenter in "+user, userType.getCostCenter(), expOrg+":"+expLoc);
		}
		if (expOrg != null && !expOrg.equals("-")) {
			PrismObject<OrgType> org = findObjectByName(OrgType.class, expOrg);
			assertAssigned(user, org.getOid(), OrgType.COMPLEX_TYPE);
			String orgId = org.asObjectable().getIdentifier();
			PrismAsserts.assertEqualsCollectionUnordered("Wrong organizationalUnit in "+user, 
					userType.getOrganizationalUnit(), PrismTestUtil.createPolyStringType(orgId));
			assertEquals("Wrong title in "+user, "Member of "+orgId, userType.getTitle().getOrig());
		} else {
			assertAssignments(user, OrgType.class, 0);
			assertTrue("Unexpected organizationalUnit in "+user+": "+userType.getOrganizationalUnit(), userType.getOrganizationalUnit().isEmpty());
			assertNull("Unexpected title in "+user+": "+userType.getDescription(), userType.getTitle());
		}
	}
	
	private void assertUserNoRole(PrismObject<UserType> user, String firstName, String lastName, String orgName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
		String username = getUsername(firstName, lastName, orgName);
		assertNotNull("No "+username+" user", user);
        display("User", user);
   		assertUser(user, user.getOid(), username, firstName+" "+lastName,
   				firstName, lastName);
   		assertEmployeeNumber(user);
   		assertLinks(user, 1);
        assertAccount(user, RESOURCE_DUMMY_SOURCE_OID);
        assertAssignments(user, RoleType.class, 0);

        openDJController.assertNoEntry("uid="+username+",ou=people,dc=example,dc=com");
	}

	private void assertUserLdap(PrismObject<UserType> user, String firstName, String lastName, String orgName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		assertUserLdap(user, firstName, lastName, orgName, 1);
	}
	
	private void assertUserLdap(PrismObject<UserType> user, String firstName, String lastName, String orgName, int assignments) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		String username = getUsername(firstName, lastName, orgName);
		assertNotNull("No "+username+" user", user);
        display("User", user);
   		assertUser(user, user.getOid(), username, firstName+" "+lastName,
   				firstName, lastName);
   		assertEmployeeNumber(user);
        assertLinks(user, 2);
        assertAccount(user, RESOURCE_DUMMY_SOURCE_OID);
        
        assertAssignments(user, RoleType.class, assignments);
        assertAssignedRole(user, ROLE_BASIC_OID);
        
        assertAccount(user, RESOURCE_OPENDJ_OID);
        PrismReferenceValue linkRef = getLinkRef(user, RESOURCE_OPENDJ_OID);
        PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
		display("OpenDJ shadow linked to "+user, shadow);
		IntegrationTestTools.assertSecondaryIdentifier(shadow, "uid="+username+",ou=people,dc=example,dc=com");
	}
	
	private void assertEmployeeNumber(PrismObject<UserType> user) {
		String employeeNumber = user.asObjectable().getEmployeeNumber();
		assertEquals("Wrong employeeNumber in "+user, user.getOid(), employeeNumber);
	}

	private String getUsername(String firstName, String lastName, String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		String username = firstName+"."+lastName;
		if (orgName != null) {
			PrismObject<OrgType> org = findObjectByName(OrgType.class, orgName);
			username = org.asObjectable().getIdentifier()+"."+username;
		}
		return username;
	}
	
	private void assertLdapLocGov(PrismObject<UserType> user, String expLoc, String expOrg) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
		UserType userType = user.asObjectable();
		
		String groupCn = expOrg+":"+expLoc;
		String groupDn = "cn="+groupCn+",ou=groups,"+openDJController.getSuffix();
		Entry groupEntry = openDJController.fetchAndAssertEntry(groupDn, "groupOfUniqueNames");
		display("Group entry", groupEntry);
		
		PrismReferenceValue accountLinkRef = getLinkRef(user, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> accountShadow = getShadowModel(accountLinkRef.getOid());
		String accountDn = IntegrationTestTools.getSecondaryIdentifier(accountShadow);
		openDJController.assertUniqueMember(groupEntry, accountDn);
	}
	
	private void assertNoLdapLocGov(PrismObject<UserType> user, String expLoc, String expOrg) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
		UserType userType = user.asObjectable();
		
		String groupCn = expOrg+":"+expLoc;
		String groupDn = "cn="+groupCn+",ou=groups,"+openDJController.getSuffix();
		Entry groupEntry = openDJController.fetchAndAssertEntry(groupDn, "groupOfUniqueNames");
		display("Group entry", groupEntry);
		String accountDn = "uid="+userType.getName()+",ou=people,"+openDJController.getSuffix();
		openDJController.assertNoUniqueMember(groupEntry, accountDn);
	}
	
}
