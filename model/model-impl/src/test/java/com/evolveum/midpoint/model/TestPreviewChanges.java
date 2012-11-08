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
package com.evolveum.midpoint.model;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ConsistencyViolationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPreviewChanges extends AbstractModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");

	private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
	private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";
	
	private static String accountOid;
	
	public TestPreviewChanges() throws JAXBException {
		super();
	}
		
	@Test
    public void test100ModifyUserAddAccount() throws Exception {
        displayTestTile(this, "test100ModifyUserAddAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test100ModifyUserAddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
        
		// WHEN
		ModelContext<UserType,AccountShadowType> modelContext = modelInteractionService.previewChanges(deltas, task, result);
		
		// THEN
		display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		assertNull("Unexpected focus secondary delta"+focusContext.getSecondaryDelta(), focusContext.getSecondaryDelta());
		
		Collection<? extends ModelProjectionContext<AccountShadowType>> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
		ModelProjectionContext<AccountShadowType> accContext = projectionContexts.iterator().next();
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<AccountShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<AccountShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                accountToAddPrimary.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(AccountShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        PropertyDelta<String> fullNameDelta = accountSecondaryDelta.findPropertyDelta(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH);
        PrismAsserts.assertReplace(fullNameDelta, "Jack Sparrow");
        PrismAsserts.assertOrigin(fullNameDelta, OriginType.OUTBOUND);

        PrismObject<AccountShadowType> accountNew = accContext.getObjectNew();
        IntegrationTestTools.assertIcfsNameAttribute(accountNew, "jack");
        IntegrationTestTools.assertAttribute(accountNew, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME, "Jack Sparrow");		
	}
		
	@Test
    public void test119ModifyUserDeleteAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException {
        displayTestTile(this, "test119ModifyUserDeleteAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test119ModifyUserDeleteAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_HBARBOSSA_OPENDJ_FILENAME));
        		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_BARBOSSA_OID, prismContext);
		PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_ACCOUNT_REF, getUserDefinition(), account);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        
		// WHEN
		ModelContext<UserType,AccountShadowType> modelContext = modelInteractionService.previewChanges(deltas, task, result);
		
		// THEN
		display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		assertNull("Unexpected focus secondary delta"+focusContext.getSecondaryDelta(), focusContext.getSecondaryDelta());
		
		Collection<? extends ModelProjectionContext<AccountShadowType>> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
		ModelProjectionContext<AccountShadowType> accContext = projectionContexts.iterator().next();
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.DELETE, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<AccountShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.DELETE, accountPrimaryDelta.getChangeType());

	}
	
	@Test
    public void test120AddAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException {
        displayTestTile(this, "test120AddAccount");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test120AddAccount");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createAddDelta(account);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        
		// WHEN
        ModelContext<UserType,AccountShadowType> modelContext = modelInteractionService.previewChanges(deltas, task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNull("Unexpected model focus context", focusContext);
		
		Collection<? extends ModelProjectionContext<AccountShadowType>> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
		ModelProjectionContext<AccountShadowType> accContext = projectionContexts.iterator().next();
		assertNotNull("Null model projection context", accContext);
		
		// Decision does not matter now
//		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<AccountShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<AccountShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                accountToAddPrimary.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(AccountShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta", accountSecondaryDelta);
	}
	
	@Test
    public void test121ModifyUserAddAccountRef() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException {
        displayTestTile(this, "test121ModifyUserAddAccountRef");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test121ModifyUserAddAccountRef");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_GUYBRUSH_OID, prismContext);
        ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), 
        		ACCOUNT_SHADOW_GUYBRUSH_OID);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
        ModelContext<UserType,AccountShadowType> modelContext = modelInteractionService.previewChanges(deltas, task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertNull("Unexpected focus secondary delta: "+focusContext.getSecondaryDelta(), userSecondaryDelta);
		
		Collection<? extends ModelProjectionContext<AccountShadowType>> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
		ModelProjectionContext<AccountShadowType> accContext = projectionContexts.iterator().next();
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<AccountShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta", accountSecondaryDelta);
	}

//	@Test
//    public void test128ModifyUserDeleteAccountRef() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
//    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
//    		PolicyViolationException, SecurityViolationException {
//        displayTestTile(this, "test128ModifyUserDeleteAccountRef");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test128ModifyUserDeleteAccountRef");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//
//        PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
//        PrismReferenceValue accountRefVal = new PrismReferenceValue();
//		accountRefVal.setObject(account);
//		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_ACCOUNT_REF, getUserDefinition(), accountOid);
//		userDelta.addModification(accountDelta);
//		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//		        
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        assertUserJack(userJack);
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//		        
//		// Check shadow (if it is unchanged)
//        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
//        assertDummyShadowRepo(accountShadow, accountOid, "jack");
//        
//        // Check account (if it is unchanged)
//        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
//        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
//        
//        // Check account in dummy resource (if it is unchanged)
//        assertDummyAccount("jack", "Jack Sparrow", true);
//	}
//	
//	@Test
//    public void test129DeleteAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
//    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
//    		PolicyViolationException, SecurityViolationException, ConsistencyViolationException {
//        displayTestTile(this, "test129DeleteAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test129DeleteAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
//        
//        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createDeleteDelta(AccountShadowType.class, accountOid, prismContext);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
//        
//		// WHEN
//        modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//        result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//        assertUserJack(userJack);
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//		// Check is shadow is gone
//        assertNoAccountShadow(accountOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//	}
//
//	
//	@Test
//    public void test130PreviewModifyUserJackAssignAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
//    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
//    		PolicyViolationException, SecurityViolationException {
//        displayTestTile(this, "test130PreviewModifyUserJackAssignAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test130PreviewModifyUserJackAssignAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, true);
//        deltas.add(accountAssignmentUserDelta);
//                
//		// WHEN
//        ModelContext<UserType,AccountShadowType> modelContext = modelInteractionService.previewChanges(deltas, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("previewChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//
//		// TODO: assert context
//		// TODO: assert context
//		// TODO: assert context
//        
//        assertResolvedResourceRefs(modelContext);
//        
//        // Check account in dummy resource
//        assertNoDummyAccount("jack");
//	}
//	
//	@Test
//    public void test131ModifyUserJackAssignAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
//    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
//    		PolicyViolationException, SecurityViolationException {
//        displayTestTile(this, "test131ModifyUserJackAssignAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test131ModifyUserJackAssignAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, true);
//        deltas.add(accountAssignmentUserDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack);
//        accountOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
//        assertDummyShadowRepo(accountShadow, accountOid, "jack");
//        
//        // Check account
//        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
//        assertDummyShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Jack Sparrow", true);
//	}
//	
//	@Test
//    public void test132ModifyAccountJackDummy() throws Exception {
//        displayTestTile(this, "test132ModifyAccountJackDummy");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test132ModifyAccountJackDummy");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(AccountShadowType.class,
//        		accountOid, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, prismContext, "Cpt. Jack Sparrow");
//        deltas.add(accountDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Cpt. Jack Sparrow", "Jack", "Sparrow");
//        accountOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
//        assertDummyShadowRepo(accountShadow, accountOid, "jack");
//        
//        // Check account
//        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
//        assertDummyShadowModel(accountModel, accountOid, "jack", "Cpt. Jack Sparrow");
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Cpt. Jack Sparrow", true);
//	}
//	
//	@Test
//    public void test139ModifyUserJackUnassignAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
//    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
//    		PolicyViolationException, SecurityViolationException {
//        displayTestTile(this, "test139ModifyUserJackUnassignAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test139ModifyUserJackUnassignAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, false);
//        deltas.add(accountAssignmentUserDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		assertUserJack(userJack, "Cpt. Jack Sparrow", "Jack", "Sparrow");
//		// Check accountRef
//        assertUserNoAccountRefs(userJack);
//        
//        // Check is shadow is gone
//        assertNoAccountShadow(accountOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//	}
//	
//	@Test
//    public void test140ModifyUserJackAssignAccountAndModify() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
//    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
//    		PolicyViolationException, SecurityViolationException {
//        displayTestTile(this, "test140ModifyUserJackAssignAccountAndModify");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test140ModifyUserJackAssignAccountAndModify");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        
//        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, true);
//        ShadowDiscriminatorObjectDelta<AccountShadowType> accountDelta = ShadowDiscriminatorObjectDelta.createModificationReplaceProperty(AccountShadowType.class,
//        		RESOURCE_DUMMY_OID, null, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, prismContext, "Cpt. Jack Sparrow");
//        deltas.add(accountDelta);
//        deltas.add(accountAssignmentUserDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Cpt. Jack Sparrow");
//        accountOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
//        assertDummyShadowRepo(accountShadow, accountOid, "jack");
//        
//        // Check account
//        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
//        assertDummyShadowModel(accountModel, accountOid, "jack", "Cpt. Jack Sparrow");
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Cpt. Jack Sparrow", true);
//	}
//	
//	@Test
//    public void test145ModifyUserJack() throws Exception {
//        displayTestTile(this, "test145ModifyUserJack");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test145ModifyUserJack");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//                        
//		// WHEN
//        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, "Magnificent Captain Jack Sparrow");
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Magnificent Captain Jack Sparrow");
//        accountOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
//        assertDummyShadowRepo(accountShadow, accountOid, "jack");
//        
//        // Check account
//        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
//        assertDummyShadowModel(accountModel, accountOid, "jack", "Magnificent Captain Jack Sparrow");
//        
//        // Check account in dummy resource
//        assertDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
//	}
//	
//	@Test
//    public void test146ModifyUserJackRaw() throws Exception {
//        displayTestTile(this, "test146ModifyUserJackRaw");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test146ModifyUserJackRaw");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME,
//        		PrismTestUtil.createPolyString("Marvelous Captain Jack Sparrow"));
//        Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(objectDelta);
//                        
//		// WHEN
//		modelService.executeChanges(deltas, ObjectOperationOption.createCollection(ObjectOperationOption.RAW), task, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//		display("User after change execution", userJack);
//		assertUserJack(userJack, "Marvelous Captain Jack Sparrow");
//        accountOid = getSingleUserAccountRef(userJack);
//        
//		// Check shadow
//        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
//        assertDummyShadowRepo(accountShadow, accountOid, "jack");
//        
//        // Check account - the original fullName should not be changed
//        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
//        assertDummyShadowModel(accountModel, accountOid, "jack", "Magnificent Captain Jack Sparrow");
//        
//        // Check account in dummy resource - the original fullName should not be changed
//        assertDummyAccount("jack", "Magnificent Captain Jack Sparrow", true);
//	}
//		
//	@Test
//    public void test149DeleteUserJack() throws Exception {
//        displayTestTile(this, "test149DeleteUserJack");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test149DeleteUserJack");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        
//        ObjectDelta<UserType> userDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_JACK_OID, prismContext);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		try {
//			PrismObject<UserType> userJack = getUser(USER_JACK_OID);
//			AssertJUnit.fail("Jack is still alive!");
//		} catch (ObjectNotFoundException ex) {
//			// This is OK
//		}
//        
//        // Check is shadow is gone
//        assertNoAccountShadow(accountOid);
//        
//        // Check if dummy resource account is gone
//        assertNoDummyAccount("jack");
//	}
//	
//	@Test
//    public void test150AddUserBlackbeardWithAccount() throws Exception {
//        displayTestTile(this, "test150AddUserBlackbeardWithAccount");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test150AddUserBlackbeardWithAccount");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        
//        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-blackbeard-account-dummy.xml"));
//        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_BLACKBEARD_OID, null, task, result);
//        UserType userMorganType = userMorgan.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getAccountRef().size());
//        ObjectReferenceType accountRefType = userMorganType.getAccountRef().get(0);
//        String accountOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
//        
//		// Check shadow
//        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
//        assertDummyShadowRepo(accountShadow, accountOid, "blackbeard");
//        
//        // Check account
//        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
//        assertDummyShadowModel(accountModel, accountOid, "blackbeard", "Edward Teach");
//        
//        // Check account in dummy resource
//        assertDummyAccount("blackbeard", "Edward Teach", true);
//	}
//
//	
//	@Test
//    public void test210AddUserMorganWithAssignment() throws Exception {
//        displayTestTile(this, "test210AddUserMorganWithAssignment");
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + ".test210AddUserMorganWithAssignment");
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//        
//        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_DIR, "user-morgan-assignment-dummy.xml"));
//        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//                
//		// WHEN
//		modelService.executeChanges(deltas, null, task, result);
//		
//		// THEN
//		result.computeStatus();
//        IntegrationTestTools.assertSuccess("executeChanges result", result);
//        
//		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
//        UserType userMorganType = userMorgan.asObjectable();
//        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getAccountRef().size());
//        ObjectReferenceType accountRefType = userMorganType.getAccountRef().get(0);
//        String accountOid = accountRefType.getOid();
//        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
//        
//		// Check shadow
//        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
//        assertDummyShadowRepo(accountShadow, accountOid, "morgan");
//        
//        // Check account
//        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
//        assertDummyShadowModel(accountModel, accountOid, "morgan", "Sir Henry Morgan");
//        
//        // Check account in dummy resource
//        assertDummyAccount("morgan", "Sir Henry Morgan", true);
//	}
//	

}
