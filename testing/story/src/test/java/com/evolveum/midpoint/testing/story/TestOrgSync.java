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
	
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME = "firstname";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME = "lastname";
	private static final String DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH = "orgpath";
	
	public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
	public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";
	
	protected static final File TASK_LIVE_SYNC_DUMMY_HR_FILE = new File(TEST_DIR, "task-dumy-hr-livesync.xml");
	protected static final String TASK_LIVE_SYNC_DUMMY_HR_OID = "10000000-0000-0000-5555-555500000001";

	private static final String ACCOUNT_GUYBRUSH_USERNAME = "guybrush";
	private static final String ACCOUNT_GUYBRUSH_FIST_NAME = "Guybrush";
	private static final String ACCOUNT_GUYBRUSH_LAST_NAME = "Threepwood";
	private static final String ACCOUNT_GUYBRUSH_ORGPATH = "Scumm Bar";
	
	private static final String ACCOUNT_HERMAN_USERNAME = "ht";
	private static final String ACCOUNT_HERMAN_FIST_NAME = "Herman";
	private static final String ACCOUNT_HERMAN_LAST_NAME = "Toothrot";
	private static final String ACCOUNT_HERMAN_ORGPATH = "Monkey Island/Gov";

	protected static DummyResource dummyResourceHr;
	protected static DummyResourceContoller dummyResourceCtlHr;
	protected ResourceType resourceDummyHrType;
	protected PrismObject<ResourceType> resourceDummyHr;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		// Resources
		dummyResourceCtlHr = DummyResourceContoller.create(RESOURCE_DUMMY_HR_ID, resourceDummyHr);
		DummyObjectClass dummyAdAccountObjectClass = dummyResourceCtlHr.getDummyResource().getAccountObjectClass();
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, String.class, false, false);
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, String.class, false, false);
		dummyResourceCtlHr.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, String.class, false, false);
		dummyResourceHr = dummyResourceCtlHr.getDummyResource();
		resourceDummyHr = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_HR_FILE, RESOURCE_DUMMY_HR_OID, initTask, initResult);
		resourceDummyHrType = resourceDummyHr.asObjectable();
		dummyResourceCtlHr.setResource(resourceDummyHr);
		dummyResourceHr.setSyncStyle(DummySyncStyle.SMART);
	
		// Object Templates
		importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
		setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);
		
		importObjectFromFile(OBJECT_TEMPLATE_ORG_FILE, initResult);
		setDefaultObjectTemplate(OrgType.COMPLEX_TYPE, OBJECT_TEMPLATE_ORG_OID);
		
		// ORG
		importObjectFromFile(ORG_TOP_FILE, initResult);
		
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
        
        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, true);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, true);
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_HR_OID, false);
	}
	
	@Test
    public void test100AddHrAccountGuybrush() throws Exception {
		final String TEST_NAME = "test100AddHrAccountGuybrush";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_GUYBRUSH_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_GUYBRUSH_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_GUYBRUSH_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ACCOUNT_GUYBRUSH_ORGPATH);
		
        // WHEN
        dummyResourceHr.addAccount(newAccount);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_HR_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_GUYBRUSH_USERNAME);
        assertNotNull("No guybrush user", user);
        display("User", user);
        assertUserGuybrush(user);
        assertAccount(user, RESOURCE_DUMMY_HR_OID);
        
        PrismObject<OrgType> org = getOrg(ACCOUNT_GUYBRUSH_ORGPATH);
        assertAssignedOrg(user, org.getOid());
        assertHasOrg(user, org.getOid());
        assertHasOrg(org, ORG_TOP_OID);
        
        assertAssignments(user, 1);
	}
	
	@Test
    public void test110AddHrAccountHerman() throws Exception {
		final String TEST_NAME = "test110AddHrAccountHerman";
        TestUtil.displayTestTile(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        
        DummyAccount newAccount = new DummyAccount(ACCOUNT_HERMAN_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_HERMAN_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_HERMAN_LAST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_ORGPATH, ACCOUNT_HERMAN_ORGPATH);
		
        // WHEN
        dummyResourceHr.addAccount(newAccount);
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_HR_OID, true);
        
        // THEN
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HERMAN_USERNAME);
        assertNotNull("No herman user", user);
        display("User", user);
        assertUserHerman(user);
        assertAccount(user, RESOURCE_DUMMY_HR_OID);
        
        PrismObject<OrgType> orgMonkeyIsland = getOrg("Monkey Island");
        PrismObject<OrgType> orgGov = getOrg("Gov");
        
        assertAssignedOrg(user, orgMonkeyIsland.getOid());
        assertHasOrg(user, orgMonkeyIsland.getOid());
        assertHasOrg(orgMonkeyIsland, orgGov.getOid());
        assertHasOrg(orgGov, ORG_TOP_OID);
        
        assertAssignments(user, 1);
	}
	
	protected void assertUserGuybrush(PrismObject<UserType> user) {
		assertUser(user, user.getOid(), ACCOUNT_GUYBRUSH_USERNAME, ACCOUNT_GUYBRUSH_FIST_NAME + " " + ACCOUNT_GUYBRUSH_LAST_NAME,
				ACCOUNT_GUYBRUSH_FIST_NAME, ACCOUNT_GUYBRUSH_LAST_NAME);
	}

	protected void assertUserHerman(PrismObject<UserType> user) {
		assertUser(user, user.getOid(), ACCOUNT_HERMAN_USERNAME, ACCOUNT_HERMAN_FIST_NAME + " " + ACCOUNT_HERMAN_LAST_NAME,
				ACCOUNT_HERMAN_FIST_NAME, ACCOUNT_HERMAN_LAST_NAME);
	}

	
	private PrismObject<OrgType> getOrg(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<OrgType> org = findObjectByName(OrgType.class, orgName);
		assertNotNull("The org "+orgName+" is missing!", org);
		display("Org "+orgName, org);
		PrismAsserts.assertPropertyValue(org, OrgType.F_NAME, PrismTestUtil.createPolyString(orgName));
		return org;
	}
	
}
