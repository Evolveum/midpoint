/*
 * Copyright (c) 2010-2014 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.PolyStringOrigMatchingRule;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.expression.evaluator.LiteralExpressionEvaluatorFactory;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test various case ignore and case transformation scenarios.
 * 
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestCaseIgnore extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/caseignore");
	
	protected static final File ROLE_X_FILE = new File(TEST_DIR, "role-x.xml");
	protected static final String ROLE_X_OID = "ef7edff4-813c-11e4-b893-3c970e467874";
	
	@Autowired(required = true)
	protected MatchingRuleRegistry matchingRuleRegistry;
	
	private MatchingRule<String> uidMatchingRule;
	
	private static String accountOid;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		uidMatchingRule = matchingRuleRegistry.getMatchingRule(StringIgnoreCaseMatchingRule.NAME, DOMUtil.XSD_STRING);
        preTestCleanup(AssignmentPolicyEnforcementType.FULL);
        
        repoAddObjectFromFile(ROLE_X_FILE, RoleType.class, initResult);
        
		InternalMonitor.reset();
		InternalMonitor.setTraceShadowFetchOperation(true);
		InternalMonitor.setTraceResourceSchemaOperations(true);
	}
			
	@Test
    public void test131ModifyUserJackAssignAccount() throws Exception {
		final String TEST_NAME="test131ModifyUserJackAssignAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCaseIgnore.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_UPCASE_OID, null, true);
        deltas.add(accountAssignmentUserDelta);
        
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
                  
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        assertShadowFetchOperationCountIncrement(0);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        rememberShadowFetchOperationCount();
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertShadowFetchOperationCountIncrement(0);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", resourceDummyUpcaseType);
        assertEnableTimestampShadow(accountShadow, startTime, endTime);
        
        // Check account
        rememberShadowFetchOperationCount();
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertShadowFetchOperationCountIncrement(1);
        assertAccountShadowModel(accountModel, accountOid, "jack", resourceDummyUpcaseType, uidMatchingRule);
        assertEnableTimestampShadow(accountModel, startTime, endTime);
        
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "jack", "Jack Sparrow", true);
                
        assertSteadyResources();
    }

	@Test
    public void test133SeachShadows() throws Exception {
		final String TEST_NAME="test133SeachShadows";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCaseIgnore.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndAccountQuery(RESOURCE_DUMMY_UPCASE_OID, 
        		new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyUpcaseType),
                ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), prismContext);
        rememberShadowFetchOperationCount();    
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        SearchResultList<PrismObject<ShadowType>> foundShadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Shadows", foundShadows);
        assertShadowFetchOperationCountIncrement(1);
        assertEquals("Wrong number of shadows found", 1, foundShadows.size());
        PrismObject<ShadowType> foundShadow = foundShadows.get(0);
        assertAccountShadowModel(foundShadow, accountOid, "jack", resourceDummyUpcaseType, uidMatchingRule);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", resourceDummyUpcaseType);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertAccountShadowModel(accountModel, accountOid, "jack", resourceDummyUpcaseType, uidMatchingRule);
        
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "JACK", "Jack Sparrow", true);
                
        assertSteadyResources();
    }
	
	@Test
    public void test139ModifyUserJackUnassignAccount() throws Exception {
		final String TEST_NAME = "test139ModifyUserJackUnassignAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        rememberShadowFetchOperationCount();
        
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_UPCASE_OID, null, false);
        deltas.add(accountAssignmentUserDelta);
                
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        assertShadowFetchOperationCountIncrement(0);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");
		// Check accountRef
        assertUserNoAccountRefs(userJack);
        
        // Check is shadow is gone
        assertNoShadow(accountOid);
        
        // Check if dummy resource account is gone
        assertNoDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "jack");
        
        assertSteadyResources();
    }

	
	@Test
    public void test150JackAssignRoleX() throws Exception {
		final String TEST_NAME = "test150JackAssignRoleX";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);
        
        // WHEN
        assignRole(USER_JACK_OID, ROLE_X_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Make sure this is repository so we do not destroy the "evidence" yet.
        PrismObject<UserType> userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
        
		display("User after change execution", userJack);
		assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_X_OID);
        accountOid = getSingleLinkOid(userJack);
        
		// Check shadow
        rememberShadowFetchOperationCount();
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertShadowFetchOperationCountIncrement(0);
        assertAccountShadowRepo(accountShadow, accountOid, "x-jack", resourceDummyUpcaseType);
        
        // Check account
        rememberShadowFetchOperationCount();
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertShadowFetchOperationCountIncrement(1);
        assertAccountShadowModel(accountModel, accountOid, "x-jack", resourceDummyUpcaseType, uidMatchingRule);
        
        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "X-JACK", ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UPCASE_NAME, "X-JACK", "title", "XXX");
	}

	@Test
    public void test152GetJack() throws Exception {
		final String TEST_NAME = "test152GetJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		display("User", userJack);
		assertUserJack(userJack);
        assertAssignedRole(userJack, ROLE_X_OID);
        String accountOidAfter = getSingleLinkOid(userJack);
        assertEquals("Account OID has changed", accountOid, accountOidAfter);
        
		// Check shadow
        rememberShadowFetchOperationCount();
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow", accountShadow);
        assertShadowFetchOperationCountIncrement(0);
        assertAccountShadowRepo(accountShadow, accountOid, "x-jack", resourceDummyUpcaseType);
        
        // Check account
        rememberShadowFetchOperationCount();
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Model shadow", accountModel);
        assertShadowFetchOperationCountIncrement(1);
        assertAccountShadowModel(accountModel, accountOid, "x-jack", resourceDummyUpcaseType, uidMatchingRule);
        
        assertDummyAccount(RESOURCE_DUMMY_UPCASE_NAME, "X-JACK", ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_UPCASE_NAME, "X-JACK", "title", "XXX");
	}

	
	private void preTestCleanup(AssignmentPolicyEnforcementType enforcementPolicy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementPolicy);
        dummyAuditService.clear();
        prepareNotifications();
        purgeScriptHistory();
        rememberShadowFetchOperationCount();
	}
	
	private void purgeScriptHistory() {
		dummyResource.purgeScriptHistory();
	}

}
