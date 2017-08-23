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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;


@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTolerantAttributes extends AbstractInitializedModelIntegrationTest {
	
	
	public static final File TEST_DIR = new File("src/test/resources/tolerant");
	
	private static final String ACCOUNT_JACK_DUMMY_BLACK_FILENAME = "src/test/resources/common/account-jack-dummy-black.xml";
	
	private static String accountOid;
	private static PrismObjectDefinition<ShadowType> accountDefinition;
	
	@Test
    public void test100ModifyUserAddAccount() throws Exception {
        TestUtil.displayTestTitle(this, "test100ModifyUserAddAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestTolerantAttributes.class.getName() + ".test100ModifyUserAddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_BLACK_FILENAME));
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
		
		getDummyResource().purgeScriptHistory();
		dummyAuditService.clear();
        dummyTransport.clearMessages();
        notificationManager.setDisabled(false);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        
		// WHEN
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();
        
		// Check accountRef
		PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack);
        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
        accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
        assertEnableTimestampShadow(accountShadow, startTime, endTime);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
        
        accountDefinition = accountModel.getDefinition();
        
        // Check account in dummy resource
        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);
                
 	}
	
	@Test
	public void test101modifyAddAttributesIntolerantPattern() throws Exception{
		 TestUtil.displayTestTitle(this, "test101modifyAddAttributesIntolerantPattern");

	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestTolerantAttributes.class.getName() + ".test101modifyAddAttributesIntolerantPattern");
	        OperationResult result = task.getResult();
	        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
	        
	        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
	        PropertyDelta propertyDelta = PropertyDelta.createModificationAddProperty(new ItemPath(UserType.F_DESCRIPTION), getUserDefinition().findPropertyDefinition(UserType.F_DESCRIPTION), "This value will be not added");
			userDelta.addModification(propertyDelta);
			Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
			
			modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);
			
			result.computeStatus();
	        TestUtil.assertSuccess(result);
	        
	     // Check value in "quote attribute"
			PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
	        assertUserJack(userJack);
	        UserType userJackType = userJack.asObjectable();
	        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
	        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
	        accountOid = accountRefType.getOid();
	        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
	        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
	        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
	        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
	        
			// Check shadow
	        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
	        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
	        
	        // Check account
	        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
	        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
	        
	        // Check account in dummy resource
	        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);
	        
	        // Check value of quote attribute
	        assertDummyAccountAttribute(RESOURCE_DUMMY_BLACK_NAME, "jack", "quote", null);

	}

	
	@Test
	public void test102modifyAddAttributeTolerantPattern() throws Exception{
		 TestUtil.displayTestTitle(this, "test102modifyAddAttributeTolerantPattern");

	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestTolerantAttributes.class.getName() + ".test102modifyAddAttributeTolerantPattern");
	        OperationResult result = task.getResult();
	        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
	        
	        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
	        PropertyDelta propertyDelta = PropertyDelta.createModificationAddProperty(new ItemPath(UserType.F_DESCRIPTION), getUserDefinition().findPropertyDefinition(UserType.F_DESCRIPTION), "res-thiIsOk");
			userDelta.addModification(propertyDelta);
			Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
			
			modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);
			
			result.computeStatus();
	        TestUtil.assertSuccess(result);
	        
	     // Check value in "quote attribute"
			PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
	        assertUserJack(userJack);
	        UserType userJackType = userJack.asObjectable();
	        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
	        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
	        accountOid = accountRefType.getOid();
	        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
	        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
	        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
	        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
	        
			// Check shadow
	        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
	        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
	        
	        // Check account
	        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
	        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
	        
	        // Check account in dummy resource
	        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);
	        
	        // Check value of quote attribute
	        assertDummyAccountAttribute(RESOURCE_DUMMY_BLACK_NAME, "jack", "quote", "res-thiIsOk");

	}

	
	@Test
	public void test103modifyReplaceAttributeIntolerant() throws Exception{
		 TestUtil.displayTestTitle(this, "test103modifyReplaceAttributeIntolerant");

	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestTolerantAttributes.class.getName() + ".test103modifyReplaceAttributeIntolerant");
	        OperationResult result = task.getResult();
	        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
	        
	        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
	        PropertyDelta propertyDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(UserType.F_EMPLOYEE_NUMBER), getUserDefinition(), "gossip-thiIsNotOk");
			userDelta.addModification(propertyDelta);
			Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
			
			modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);
			
			result.computeStatus();
	        TestUtil.assertSuccess(result);
	        
	     // Check value in "quote attribute"
			PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
//	        assertUserJack(userJack);
	        UserType userJackType = userJack.asObjectable();
	        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
	        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
	        accountOid = accountRefType.getOid();
	        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
	        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
	        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
	        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
	        
			// Check shadow
	        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
	        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
	        
	        // Check account
	        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
	        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
	        
	        // Check account in dummy resource
	        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);
	        
	        // Check value of drink attribute
	        assertDummyAccountAttribute(RESOURCE_DUMMY_BLACK_NAME, "jack", "gossip", null);

	}
	
	@Test
	public void test104modifyReplaceAttributeTolerantPattern() throws Exception{
		 TestUtil.displayTestTitle(this, "test104modifyReplaceAttributeTolerantPattern");

	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestTolerantAttributes.class.getName() + ".test104modifyReplaceAttributeTolerantPattern");
	        OperationResult result = task.getResult();
	        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
	        
	        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
	        ItemPath drinkItemPath = new ItemPath(new QName(getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME).getNamespace(), "drink"));
	        PropertyDelta propertyDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(UserType.F_EMPLOYEE_NUMBER), getUserDefinition(), "thiIsOk");
			userDelta.addModification(propertyDelta);
			Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
			
			modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);
			
			result.computeStatus();
	        TestUtil.assertSuccess(result);
	        
	     // Check value in "quote attribute"
			PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
//	        assertUserJack(userJack);
	        UserType userJackType = userJack.asObjectable();
	        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());
	        ObjectReferenceType accountRefType = userJackType.getLinkRef().get(0);
	        accountOid = accountRefType.getOid();
	        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
	        PrismReferenceValue accountRefValue = accountRefType.asReferenceValue();
	        assertEquals("OID mismatch in accountRefValue", accountOid, accountRefValue.getOid());
	        assertNull("Unexpected object in accountRefValue", accountRefValue.getObject());
	        
			// Check shadow
	        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
	        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
	        
	        // Check account
	        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
	        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_BLACK_NAME));
	        
	        // Check account in dummy resource
	        assertAccount(userJack, RESOURCE_DUMMY_BLACK_OID);
	        
	        // Check value of drink attribute
	        assertDummyAccountAttribute(RESOURCE_DUMMY_BLACK_NAME, "jack", "gossip", "thiIsOk");

	}

	@Test
	public void test105modifyAddNonTolerantAttribute() throws Exception{
		 TestUtil.displayTestTitle(this, "test105modifyAddNonTolerantAttribute");

	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestTolerantAttributes.class.getName() + ".test105modifyAddNonTolerantAttribute");
	        OperationResult result = task.getResult();
	        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
	        
	        ObjectDelta<ShadowType> userDelta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountOid, prismContext);
	        
	        ItemPath drinkItemPath = new ItemPath(ShadowType.F_ATTRIBUTES, new QName(RESOURCE_DUMMY_BLACK_NAMESPACE, "drink"));
	        assertNotNull("null definition for drink attribute ", accountDefinition.findPropertyDefinition(drinkItemPath));
	        PropertyDelta propertyDelta = PropertyDelta.createModificationAddProperty(drinkItemPath, accountDefinition.findPropertyDefinition(drinkItemPath), "This should be ignored");
			userDelta.addModification(propertyDelta);
			Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
			
			try{
			modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, result);
			fail("Expected Policy violation exception, because non-tolerant attribute is modified, but haven't got one.");
			} catch (PolicyViolationException ex){
				//this is expected
			}
			}

  
	
}
