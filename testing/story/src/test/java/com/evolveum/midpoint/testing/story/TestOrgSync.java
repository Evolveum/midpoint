package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2013 Evolveum
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
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
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
public class TestOrgSync extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "orgsync");
	
	public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
	public static final String OBJECT_TEMPLATE_USER_OID = "10000000-0000-0000-0000-000000000222";
	
	public static final File OBJECT_TEMPLATE_ORG_FILE = new File(TEST_DIR, "object-template-org.xml");
	public static final String OBJECT_TEMPLATE_ORG_OID = "10000000-0000-0000-0000-000000000231";
	
	protected static final File RESOURCE_DUMMY_HR_FILE = new File(TEST_DIR, "resource-dummy-hr.xml");
	protected static final String RESOURCE_DUMMY_HR_ID = "HR";
	protected static final String RESOURCE_DUMMY_HR_OID = "10000000-0000-0000-0000-000000000001";
	protected static final String RESOURCE_DUMMY_HR_NAMESPACE = MidPointConstants.NS_RI;
	
	protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;
	
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME = "firstname";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME = "lastname";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH = "orgpath";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES = "responsibilities";
	
	public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
	public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";
	
	public static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
	public static final String ROLE_BASIC_OID = "10000000-0000-0000-0000-000000000601";
	
	public static final File ROLE_META_REPLICATED_ORG_FILE = new File(TEST_DIR, "role-meta-replicated-org.xml");
	public static final String ROLE_META_REPLICATED_ORG_OID = "10000000-0000-0000-0000-000000006601";
	
	protected static final File TASK_LIVE_SYNC_DUMMY_HR_FILE = new File(TEST_DIR, "task-dumy-hr-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_HR_OID = "10000000-0000-0000-5555-555500000001";
	
	private static final String ACCOUNT_HERMAN_USERNAME = "ht";
	private static final String ACCOUNT_HERMAN_FIST_NAME = "Herman";
	private static final String ACCOUNT_HERMAN_LAST_NAME = "Toothrot";
	
	private static final String ACCOUNT_LEMONHEAD_USERNAME = "lemonhead";
	private static final String ACCOUNT_LEMONHEAD_FIST_NAME = "Lemonhead";
	private static final String ACCOUNT_LEMONHEAD_LAST_NAME = "Canibal";

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

	private static final String ORGPATH_MONKEY_ISLAND = "Monkey Island";
	private static final String ORGPATH_FREELANCE = "Freelance/Ministry of Rum";
	private static final String ORGPATH_SCUMM_BAR = "Scumm Bar/Ministry of Rum";
	private static final String ORGPATH_BRUTE = "Brute Office/Violence Section/Department of Mischief/Ministry of Offense";
	
	private static final String RESP_CANIBALISM = "canibalism";
	
	protected static DummyResource dummyResourceHr;
	protected static DummyResourceContoller dummyResourceCtlHr;
	protected ResourceType resourceDummyHrType;
	protected PrismObject<ResourceType> resourceDummyHr;
	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;

	private String orgMonkeyIslandOid;
	private String orgMoROid;
	private String orgScummBarOid;

	private String roleCanibalismOid;
	
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
		dummyResourceCtlHr = DummyResourceContoller.create(RESOURCE_DUMMY_HR_ID, resourceDummyHr);
		DummyObjectClass dummyAdAccountObjectClass = dummyResourceCtlHr.getDummyResource().getAccountObjectClass();
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, String.class, false, false);
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, String.class, false, false);
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, String.class, false, false);
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES, String.class, false, true);
		dummyResourceHr = dummyResourceCtlHr.getDummyResource();
		resourceDummyHr = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_HR_FILE, RESOURCE_DUMMY_HR_OID, initTask, initResult);
		resourceDummyHrType = resourceDummyHr.asObjectable();
		dummyResourceCtlHr.setResource(resourceDummyHr);
		dummyResourceHr.setSyncStyle(DummySyncStyle.SMART);
		
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);
	
		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
		setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);
		
		importObjectFromFile(OBJECT_TEMPLATE_ORG_FILE, initResult);
		setDefaultObjectTemplate(OrgType.COMPLEX_TYPE, OBJECT_TEMPLATE_ORG_OID);
		
		// Org
		importObjectFromFile(ORG_TOP_FILE, initResult);
		
		// Role
		importObjectFromFile(ROLE_BASIC_FILE, initResult);
		importObjectFromFile(ROLE_META_REPLICATED_ORG_FILE, initResult);
		
		// Tasks
		importObjectFromFile(TASK_LIVE_SYNC_DUMMY_HR_FILE, initResult);
		
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        OperationResult testResultHr = modelService.testResource(RESOURCE_DUMMY_HR_OID, task);
        TestUtil.assertSuccess(testResultHr);
        
        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);
        
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_HR_OID, false);
	}
	
	/**
	 * First account on Monkey Island. The Monkey Island org should be created.
	 */
	@Test
    public void test100AddHrAccountHerman() throws Exception {
		final String TEST_NAME = "test100AddHrAccountHerman";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_HERMAN_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_HERMAN_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_HERMAN_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_MONKEY_ISLAND);
		
        // WHEN
        dummyResourceHr.addAccount(newAccount);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_HR_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HERMAN_USERNAME);
        assertNotNull("No herman user", user);
        display("User", user);
        assertUserHerman(user);
        assertAccount(user, RESOURCE_DUMMY_HR_OID);
        
        PrismObject<OrgType> org = getAndAssertReplicatedOrg(ORGPATH_MONKEY_ISLAND);
        orgMonkeyIslandOid = org.getOid();
        assertAssignedOrg(user, org.getOid());
        assertHasOrg(user, org.getOid());
        assertHasOrg(org, ORG_TOP_OID);
        
        assertBasicRoleAndResources(user);
        assertAssignments(user, 2);
	}
	
	/**
	 * Second account on Monkey Island. The existing org should be reused.
	 */
	@Test
    public void test105AddHrAccountLemonhead() throws Exception {
		final String TEST_NAME = "test105AddHrAccountLemonhead";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_LEMONHEAD_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_LEMONHEAD_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_LEMONHEAD_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_MONKEY_ISLAND);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES, RESP_CANIBALISM);
		
        // WHEN
        dummyResourceHr.addAccount(newAccount);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_HR_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_LEMONHEAD_USERNAME);
        assertNotNull("No lemonhead user", user);
        display("User", user);
        assertUser(user, ACCOUNT_LEMONHEAD_USERNAME, ACCOUNT_LEMONHEAD_FIST_NAME, ACCOUNT_LEMONHEAD_LAST_NAME);
        assertAccount(user, RESOURCE_DUMMY_HR_OID);
        
        PrismObject<OrgType> org = getAndAssertReplicatedOrg(ORGPATH_MONKEY_ISLAND);
        assertAssignedOrg(user, org.getOid());
        assertHasOrg(user, org.getOid());
        assertHasOrg(org, ORG_TOP_OID);
        
        assertEquals("Monkey island Org OID has changed", orgMonkeyIslandOid, org.getOid());
        
        assertBasicRoleAndResources(user);
        roleCanibalismOid = assertResponsibility(user, RESP_CANIBALISM);
        assertAssignments(user, 3);
	}
	
	/**
	 * Yet another canibal. Same org, same responsibility. Make sure everything is reused and not created again.
	 */
	@Test
    public void test106AddHrAccountSharptooth() throws Exception {
		final String TEST_NAME = "test106AddHrAccountSharptooth";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_SHARPTOOTH_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_SHARPTOOTH_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_SHARPTOOTH_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_MONKEY_ISLAND);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_RESPONSIBILITIES, RESP_CANIBALISM);
		
        // WHEN
        dummyResourceHr.addAccount(newAccount);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_HR_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_SHARPTOOTH_USERNAME);
        assertNotNull("No sharptooth user", user);
        display("User", user);
        assertUser(user, ACCOUNT_SHARPTOOTH_USERNAME, ACCOUNT_SHARPTOOTH_FIST_NAME, ACCOUNT_SHARPTOOTH_LAST_NAME);
        assertAccount(user, RESOURCE_DUMMY_HR_OID);
        
        PrismObject<OrgType> org = getAndAssertReplicatedOrg(ORGPATH_MONKEY_ISLAND);
        assertAssignedOrg(user, org.getOid());
        assertHasOrg(user, org.getOid());
        assertHasOrg(org, ORG_TOP_OID);
        assertEquals("Monkey island Org OID has changed", orgMonkeyIslandOid, org.getOid());
        
        assertBasicRoleAndResources(user);
        String thisRoleCanibalismOid = assertResponsibility(user, RESP_CANIBALISM);
        assertEquals("Canibalism role OID has changed", roleCanibalismOid, thisRoleCanibalismOid);
        assertAssignments(user, 3);
	}
	
	/**
	 * Two-level orgpath. Both orgs should be created.
	 */
	@Test
    public void test110AddHrAccountGuybrush() throws Exception {
		final String TEST_NAME = "test110AddHrAccountGuybrush";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_GUYBRUSH_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_GUYBRUSH_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_GUYBRUSH_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_FREELANCE);
		
        // WHEN
        dummyResourceHr.addAccount(newAccount);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_HR_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_GUYBRUSH_USERNAME);
        assertNotNull("No guybrush user", user);
        display("User", user);
        assertUserGuybrush(user);
        assertAccount(user, RESOURCE_DUMMY_HR_OID);
        
        PrismObject<OrgType> orgFreelance = getAndAssertReplicatedOrg("Freelance");
        PrismObject<OrgType> orgMoR = getAndAssertReplicatedOrg("Ministry of Rum");
        orgMoROid = orgMoR.getOid();
        
        assertAssignedOrg(user, orgFreelance.getOid());
        assertHasOrg(user, orgFreelance.getOid());
        assertHasOrg(orgFreelance, orgMoR.getOid());
        assertHasOrg(orgMoR, ORG_TOP_OID);
        
        assertBasicRoleAndResources(user);
        assertAssignments(user, 2);
	}
	
	/**
	 * Two-level orgpath, partially created. Only scumm bar should be crated. Ministry should be reused.
	 */
	@Test
    public void test115AddHrAccountMancomb() throws Exception {
		final String TEST_NAME = "test115AddHrAccountMancomb";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_MANCOMB_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_MANCOMB_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_MANCOMB_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_SCUMM_BAR);
		
        // WHEN
        dummyResourceHr.addAccount(newAccount);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_HR_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_MANCOMB_USERNAME);
        assertNotNull("No mancomb user", user);
        display("User", user);
        assertUser(user, ACCOUNT_MANCOMB_USERNAME, ACCOUNT_MANCOMB_FIST_NAME, ACCOUNT_MANCOMB_LAST_NAME);
        assertAccount(user, RESOURCE_DUMMY_HR_OID);
        
        PrismObject<OrgType> orgScummBar = getAndAssertReplicatedOrg("Scumm Bar");
        orgScummBarOid = orgScummBar.getOid();
        PrismObject<OrgType> orgMoR = getAndAssertReplicatedOrg("Ministry of Rum");
        
        assertAssignedOrg(user, orgScummBar.getOid());
        assertHasOrg(user, orgScummBar.getOid());
        assertHasOrg(orgScummBar, orgMoR.getOid());
        assertHasOrg(orgMoR, ORG_TOP_OID);
        
        assertEquals("MoR Org OID has changed", orgMoROid, orgMoR.getOid());
        
        assertBasicRoleAndResources(user);
        assertAssignments(user, 2);
	}
	
	/**
	 * Two-level orgpath, completely created. No new orgs should be created.
	 */
	@Test
    public void test117AddHrAccountCobb() throws Exception {
		final String TEST_NAME = "test117AddHrAccountCobb";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_COBB_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_COBB_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_COBB_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_SCUMM_BAR);
		
        // WHEN
        dummyResourceHr.addAccount(newAccount);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_HR_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_COBB_USERNAME);
        assertNotNull("No cobb user", user);
        display("User", user);
        assertUser(user, ACCOUNT_COBB_USERNAME, ACCOUNT_COBB_FIST_NAME, ACCOUNT_COBB_LAST_NAME);
        assertAccount(user, RESOURCE_DUMMY_HR_OID);
        
        PrismObject<OrgType> orgScummBar = getAndAssertReplicatedOrg("Scumm Bar");
        PrismObject<OrgType> orgMoR = getAndAssertReplicatedOrg("Ministry of Rum");
        
        assertAssignedOrg(user, orgScummBar.getOid());
        assertHasOrg(user, orgScummBar.getOid());
        assertHasOrg(orgScummBar, orgMoR.getOid());
        assertHasOrg(orgMoR, ORG_TOP_OID);
        
        assertEquals("MoR Org OID has changed", orgMoROid, orgMoR.getOid());
        assertEquals("Scumm bar Org OID has changed", orgScummBarOid, orgScummBar.getOid());
        
        assertBasicRoleAndResources(user);
        assertAssignments(user, 2);
	}
	
	/**
	 * Four-level orgpath, completely new.
	 */
	@Test
    public void test130AddHrAccountLargo() throws Exception {
		final String TEST_NAME = "test130AddHrAccountLargo";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_LARGO_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_LARGO_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_LARGO_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ORGPATH_BRUTE);
		
        // WHEN
        dummyResourceHr.addAccount(newAccount);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_HR_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_LARGO_USERNAME);
        assertNotNull("No largo user", user);
        display("User", user);
        assertUser(user, ACCOUNT_LARGO_USERNAME, ACCOUNT_LARGO_FIST_NAME, ACCOUNT_LARGO_LAST_NAME);
        assertAccount(user, RESOURCE_DUMMY_HR_OID);
        
        PrismObject<OrgType> orgMoO = getAndAssertReplicatedOrg("Ministry of Offense");
        PrismObject<OrgType> orgDoM = getAndAssertReplicatedOrg("Department of Mischief");
        PrismObject<OrgType> orgVSec = getAndAssertReplicatedOrg("Violence Section");
        PrismObject<OrgType> orgBOff = getAndAssertReplicatedOrg("Brute Office");
        
        assertAssignedOrg(user, orgBOff.getOid());
        assertHasOrg(user, orgBOff.getOid());
        assertHasOrg(orgBOff, orgVSec.getOid());
        assertHasOrg(orgVSec, orgDoM.getOid());
        assertHasOrg(orgDoM, orgMoO.getOid());
        assertHasOrg(orgMoO, ORG_TOP_OID);
        
        assertBasicRoleAndResources(user);
        assertAssignments(user, 2);
	}
	
	protected void assertUserGuybrush(PrismObject<UserType> user) {
		assertUser(user, ACCOUNT_GUYBRUSH_USERNAME, ACCOUNT_GUYBRUSH_FIST_NAME, ACCOUNT_GUYBRUSH_LAST_NAME);
	}

	protected void assertUserHerman(PrismObject<UserType> user) {
		assertUser(user, ACCOUNT_HERMAN_USERNAME, ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
	}
	
	protected void assertUser(PrismObject<UserType> user, String username, String firstName, String lastName) {
		assertUser(user, user.getOid(), username, firstName + " " + lastName,
				firstName, lastName);
	}

	private PrismObject<OrgType> getAndAssertReplicatedOrg(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<OrgType> org = getOrg(orgName);
		PrismAsserts.assertPropertyValue(org, OrgType.F_ORG_TYPE, "replicated");
		assertAssignedRole(org, ROLE_META_REPLICATED_ORG_OID);
		PrismReferenceValue linkRef = getSingleLinkRef(org);
		// We are bold enough to get the whole shadow
		PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
		display("Org "+orgName+" shadow", shadow);
		return org;
	}
	
	private PrismObject<OrgType> getOrg(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<OrgType> org = findObjectByName(OrgType.class, orgName);
		assertNotNull("The org "+orgName+" is missing!", org);
		display("Org "+orgName, org);
		PrismAsserts.assertPropertyValue(org, OrgType.F_NAME, PrismTestUtil.createPolyString(orgName));
		return org;
	}

	private void assertBasicRoleAndResources(PrismObject<UserType> user) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		assertAssignedRole(user, ROLE_BASIC_OID);
		PrismReferenceValue linkRef = getLinkRef(user, RESOURCE_OPENDJ_OID);
		PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
		display("OpenDJ shadow linked to "+user, shadow);
	}
	
	private String assertResponsibility(PrismObject<UserType> user, String respName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		String respRoleName = "R_"+respName;
		PrismObject<RoleType> respRole = searchObjectByName(RoleType.class, respRoleName);
		assertNotNull("No role for responsibility "+respName);
		display("Responsibility role for "+respName, respRole);
		assertAssignedRole(user, respRole.getOid());
		return respRole.getOid();
	}


	
}
