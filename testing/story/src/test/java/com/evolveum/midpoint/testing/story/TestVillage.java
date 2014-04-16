package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2014 Evolveum
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


import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.notifications.api.events.AccountEvent;
import com.evolveum.midpoint.notifications.api.transports.Message;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestVillage extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "village");
	
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

	protected static final File TASK_LIVE_SYNC_DUMMY_SOURCE_FILE = new File(TEST_DIR, "task-dumy-source-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_SOURCE_OID = "10000000-0000-0000-5555-555500000001";
	
	private static final String ACCOUNT_HERMAN_USERNAME = "ht";
	private static final String ACCOUNT_HERMAN_FIST_NAME = "Herman";
	private static final String ACCOUNT_HERMAN_LAST_NAME = "Toothrot";
	private static final String ACCOUNT_HERMAN_LOC = "Monkey Island";
	private static final String ACCOUNT_HERMAN_ORG = "Gov";
	private static final String USER_HERMAN_NAME = ACCOUNT_HERMAN_FIST_NAME+"."+ACCOUNT_HERMAN_LAST_NAME;
	
	private static final String ACCOUNT_LEMONHEAD_USERNAME = "lemonhead";
	private static final String ACCOUNT_LEMONHEAD_FIST_NAME = "Lemonhead";
	private static final String ACCOUNT_LEMONHEAD_LAST_NAME = "Canibal";
	private static final String ACCOUNT_LEMONHEAD_LOC = "Monkey Island";
	private static final String ACCOUNT_LEMONHEAD_ORG = "Exec";
	private static final String USER_LEMONHEAD_NAME = ACCOUNT_LEMONHEAD_FIST_NAME+"."+ACCOUNT_LEMONHEAD_LAST_NAME;

	private static final String ACCOUNT_SHARPTOOTH_USERNAME = "sharptooth";
	private static final String ACCOUNT_SHARPTOOTH_FIST_NAME = "Sharptooth";
	private static final String ACCOUNT_SHARPTOOTH_LAST_NAME = "Canibal";

	private static final String ACCOUNT_GUYBRUSH_USERNAME = "guybrush";
	private static final String ACCOUNT_GUYBRUSH_FIST_NAME = "Guybrush";
	private static final String ACCOUNT_GUYBRUSH_LAST_NAME = "Threepwood";
	
	private static final String ACCOUNT_MANCOMB_USERNAME = "mancomb";
	private static final String ACCOUNT_MANCOMB_FIST_NAME = "Mancomb";
	private static final String ACCOUNT_MANCOMB_LAST_NAME = "Seepgood";

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
		
		// LDAP content
		openDJController.addEntryFromLdifFile(GROUP_GOV_MONKEY_ISLAND_LDIF_FILE);
		openDJController.addEntryFromLdifFile(GROUP_EXEC_MONKEY_ISLAND_LDIF_FILE);

		// Tasks
		importObjectFromFile(TASK_LIVE_SYNC_DUMMY_SOURCE_FILE, initResult);
		
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
        
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, false);
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
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        assertUserNoRole(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
        assertLocGov(user, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}

	@Test
    public void test102HermanAssignBasicRole() throws Exception {
		final String TEST_NAME = "test102HermanAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        
        // WHEN
        assignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserLdap(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
        assertLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
        assertLdapLocGov(userAfter, ACCOUNT_HERMAN_LOC, ACCOUNT_HERMAN_ORG);
	}
	
	@Test
    public void test104HermanUnAssignBasicRole() throws Exception {
		final String TEST_NAME = "test104HermanUnAssignBasicRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        
        // WHEN
        unassignRole(user.getOid(), ROLE_BASIC_OID);
        
        // THEN
        PrismObject<UserType> userAfter = getUser(user.getOid());
        assertUserNoRole(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
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
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        assertUserNoRole(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
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
        assertUserLdap(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
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
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        assertUserLdap(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
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
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(USER_HERMAN_NAME);
        assertUserLdap(user, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
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
        assertUserNoRole(userAfter, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
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
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_SOURCE_OID, true);
        
        // THEN
        PrismObject<UserType> userAfter = findUserByUsername(USER_LEMONHEAD_NAME);
        assertUserLdap(userAfter, ACCOUNT_LEMONHEAD_FIST_NAME, ACCOUNT_LEMONHEAD_LAST_NAME);
        assertLocGov(userAfter, ACCOUNT_LEMONHEAD_LOC, ACCOUNT_LEMONHEAD_ORG);
        assertLdapLocGov(userAfter, ACCOUNT_LEMONHEAD_LOC, ACCOUNT_LEMONHEAD_ORG);
	}
	
	private void assertLocGov(PrismObject<UserType> user, String expLoc, String expOrg) {
		UserType userType = user.asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong locality in "+user, expLoc, userType.getLocality());
		PrismAsserts.assertEqualsCollectionUnordered("Wrong organization in "+user, userType.getOrganization(), 
				PrismTestUtil.createPolyStringType(expOrg));
		if (expLoc == null || expOrg == null) {
			assertTrue("Wrong organizationalUnit in "+user+", expected empty but was "+userType.getOrganizationalUnit(), userType.getOrganizationalUnit().isEmpty());
		} else {
			PrismAsserts.assertEqualsCollectionUnordered("Wrong organizationalUnit in "+user, userType.getOrganizationalUnit(), 
				PrismTestUtil.createPolyStringType(expOrg+":"+expLoc));
		}
	}
	
	private void assertUserNoRole(PrismObject<UserType> user, String firstName, String lastName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
		String username = firstName+"."+lastName;
		assertNotNull("No "+username+" user", user);
        display("User", user);
   		assertUser(user, user.getOid(), username, firstName+" "+lastName,
   				firstName, lastName);
   		assertLinks(user, 1);
        assertAccount(user, RESOURCE_DUMMY_SOURCE_OID);
        assertAssignments(user, 0);

        openDJController.assertNoEntry("uid="+username+",ou=people,dc=example,dc=com");
	}

	private void assertUserLdap(PrismObject<UserType> user, String firstName, String lastName) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		String username = firstName+"."+lastName;
		assertNotNull("No "+username+" user", user);
        display("User", user);
   		assertUser(user, user.getOid(), username, firstName+" "+lastName,
   				firstName, lastName);
        assertLinks(user, 2);
        assertAccount(user, RESOURCE_DUMMY_SOURCE_OID);
        
        assertAssignments(user, 1);
        assertAssignedRole(user, ROLE_BASIC_OID);
        
        assertAccount(user, RESOURCE_OPENDJ_OID);
        PrismReferenceValue linkRef = getLinkRef(user, RESOURCE_OPENDJ_OID);
        PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
		display("OpenDJ shadow linked to "+user, shadow);
		IntegrationTestTools.assertIcfsNameAttribute(shadow, "uid="+username+",ou=people,dc=example,dc=com");
	}
	
	private void assertLdapLocGov(PrismObject<UserType> user, String expLoc, String expOrg) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
		UserType userType = user.asObjectable();
		
		String groupCn = expOrg+":"+expLoc;
		String groupDn = "cn="+groupCn+",ou=groups,"+openDJController.getSuffix();
		SearchResultEntry groupEntry = openDJController.fetchAndAssertEntry(groupDn, "groupOfUniqueNames");
		display("Group entry", groupEntry);
		
		PrismReferenceValue accountLinkRef = getLinkRef(user, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> accountShadow = getShadowModel(accountLinkRef.getOid());
		String accountDn = IntegrationTestTools.getIcfsNameAttribute(accountShadow);
		openDJController.assertUniqueMember(groupEntry, accountDn);
	}
	
	private void assertNoLdapLocGov(PrismObject<UserType> user, String expLoc, String expOrg) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException {
		UserType userType = user.asObjectable();
		
		String groupCn = expOrg+":"+expLoc;
		String groupDn = "cn="+groupCn+",ou=groups,"+openDJController.getSuffix();
		SearchResultEntry groupEntry = openDJController.fetchAndAssertEntry(groupDn, "groupOfUniqueNames");
		display("Group entry", groupEntry);
		String accountDn = "uid="+userType.getName()+",ou=people,"+openDJController.getSuffix();
		openDJController.assertNoUniqueMember(groupEntry, accountDn);
	}
	
}
