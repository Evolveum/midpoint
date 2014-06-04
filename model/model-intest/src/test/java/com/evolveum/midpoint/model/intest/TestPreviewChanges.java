/*
 * Copyright (c) 2010-2013 Evolveum
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

import static com.evolveum.midpoint.test.util.TestUtil.assertFailure;
import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ObjectSource;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPreviewChanges extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");

	private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
	private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";
	
	private static String accountOid;
	
	@Test
    public void test100ModifyUserAddAccountBundle() throws Exception {
		final String TEST_NAME = "test100ModifyUserAddAccountBundle";
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		ObjectSource<PrismObject<ShadowType>> accountSource = new ObjectSource<PrismObject<ShadowType>>() {
			@Override
			public PrismObject<ShadowType> get() {
				try {
					return PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
				} catch (SchemaException e) {
					throw new IllegalStateException(e.getMessage(),e);
				} catch (IOException e) {
                    throw new IllegalStateException(e.getMessage(),e);
                }
            }
		};
        
		ObjectChecker<ModelContext<UserType>> checker = new ObjectChecker<ModelContext<UserType>>() {
			@Override
			public void check(ModelContext<UserType> modelContext) {
				assertAddAccount(modelContext, false);	
			}
		};
		
		modifyUserAddAccountImplicit(TEST_NAME, accountSource, checker);
		modifyUserAddAccountExplicit(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitSame(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitSameReverse(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitEqual(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitEqualReverse(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitNotEqual(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitNotEqualReverse(TEST_NAME, accountSource, checker);
	}
	
	@Test
    public void test101ModifyUserAddAccountNoAttributesBundle() throws Exception {
		final String TEST_NAME = "test101ModifyUserAddAccountNoAttributesBundle";
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		ObjectSource<PrismObject<ShadowType>> accountSource = new ObjectSource<PrismObject<ShadowType>>() {
			@Override
			public PrismObject<ShadowType> get() {
				try {
					PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
					account.removeContainer(ShadowType.F_ATTRIBUTES);
					return account;
				} catch (SchemaException|IOException e) {
					throw new IllegalStateException(e.getMessage(),e);
				}
			}
		};
        
		ObjectChecker<ModelContext<UserType>> checker = new ObjectChecker<ModelContext<UserType>>() {
			@Override
			public void check(ModelContext<UserType> modelContext) {
				assertAddAccount(modelContext, true);	
			}
		};
		
		modifyUserAddAccountImplicit(TEST_NAME, accountSource, checker);
		modifyUserAddAccountExplicit(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitSame(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitSameReverse(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitEqual(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitEqualReverse(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitNotEqual(TEST_NAME, accountSource, checker);
		modifyUserAddAccountImplicitExplicitNotEqualReverse(TEST_NAME, accountSource, checker);
	}
		
    private void modifyUserAddAccountImplicit(String bundleName, ObjectSource<PrismObject<ShadowType>> accountSource, 
    		ObjectChecker<ModelContext<UserType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "Implicit";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		
		doPreview(deltas, checker, task, result);
    }
    
    private void modifyUserAddAccountExplicit(String bundleName, ObjectSource<PrismObject<ShadowType>> accountSource, 
    		ObjectChecker<ModelContext<UserType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "Explicit";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
		doPreview(deltas, checker, task, result);
	}
    
    private void modifyUserAddAccountImplicitExplicitSame(String bundleName, 
    		ObjectSource<PrismObject<ShadowType>> accountSource, ObjectChecker<ModelContext<UserType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitSame";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
		doPreview(deltas, checker, task, result);
	}
	
    private void modifyUserAddAccountImplicitExplicitSameReverse(String bundleName, 
    		ObjectSource<PrismObject<ShadowType>> accountSource, ObjectChecker<ModelContext<UserType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitSameReverse";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);
        
		doPreview(deltas, checker, task, result);
	}
    
    private void modifyUserAddAccountImplicitExplicitEqual(String bundleName, 
    		ObjectSource<PrismObject<ShadowType>> accountSource, ObjectChecker<ModelContext<UserType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitEqual";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account.clone());
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
		doPreview(deltas, checker, task, result);
	}
    
    private void modifyUserAddAccountImplicitExplicitEqualReverse(String bundleName, 
    		ObjectSource<PrismObject<ShadowType>> accountSource, ObjectChecker<ModelContext<UserType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitEqualReverse";
        TestUtil.displayTestTile(this, TEST_NAME);
	
        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account.clone());
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);
        
		doPreview(deltas, checker, task, result);
	}
	
    private void modifyUserAddAccountImplicitExplicitNotEqual(String bundleName, 
    		ObjectSource<PrismObject<ShadowType>> accountSource, ObjectChecker<ModelContext<UserType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitNotEqual";
        TestUtil.displayTestTile(this, TEST_NAME);
    
        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account.clone());
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		// Let's make the account different. This should cause the preview to fail
		account.asObjectable().setDescription("aye!");
		ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        
		doPreviewFail(deltas, task, result);
	}
	
    private void modifyUserAddAccountImplicitExplicitNotEqualReverse(String bundleName, 
    		ObjectSource<PrismObject<ShadowType>> accountSource, ObjectChecker<ModelContext<UserType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitNotEqualReverse";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<ShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account.clone());
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		// Let's make the account different. This should cause the preview to fail
		account.asObjectable().setDescription("aye!");
		ObjectDelta<ShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta, userDelta);
		
		doPreviewFail(deltas, task, result);
	}
	
	private void doPreview(Collection<ObjectDelta<? extends ObjectType>> deltas, 
			ObjectChecker<ModelContext<UserType>> checker, Task task, OperationResult result) 
					throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
					ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		display("Input deltas: ", deltas);
        
		// WHEN
		ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
		display("Preview context", modelContext);
		checker.check(modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
	}
	
	private void doPreviewFail(Collection<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result) 
					throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
					ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		display("Input deltas: ", deltas);
        
		try {
			// WHEN
			ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
			
			AssertJUnit.fail("Expected exception, but it haven't come");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
        TestUtil.assertFailure(result);
	}
	
	private void assertAddAccount(ModelContext<UserType> modelContext, boolean expectFullNameDelta) {
		assertNotNull("Null model context", modelContext);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		assertSideEffectiveDeltasOnly(focusContext.getSecondaryDelta(), "focus secondary delta", ActivationStatusType.ENABLED);
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
		ModelProjectionContext accContext = projectionContexts.iterator().next();
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<ShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        PropertyDelta<String> fullNameDelta = accountSecondaryDelta.findPropertyDelta(
        		dummyResourceCtl.getAttributeFullnamePath());
        if (expectFullNameDelta) {
            assertNotNull("No full name delta in account secondary delta", fullNameDelta);
            PrismAsserts.assertReplace(fullNameDelta, "Jack Sparrow");
            PrismAsserts.assertOrigin(fullNameDelta, OriginType.OUTBOUND);        	
        } else {
        	assertNull("Unexpected full name delta in account secondary delta", fullNameDelta);
        }

        PrismObject<ShadowType> accountNew = accContext.getObjectNew();
        IntegrationTestTools.assertIcfsNameAttribute(accountNew, "jack");
        IntegrationTestTools.assertAttribute(accountNew, dummyResourceCtl.getAttributeFullnameQName(), "Jack Sparrow");	
	}
	
	
	@Test
    public void test200ModifyUserDeleteAccount() throws Exception {
		final String TEST_NAME = "test200ModifyUserDeleteAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_GUYBRUSH_OID, prismContext);
		PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF, getUserDefinition(), account);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		display("Input deltas: ", deltas);
        
		// WHEN
		ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
		display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSecondaryDelta());
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
		ModelProjectionContext accContext = projectionContexts.iterator().next();
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.DELETE, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.DELETE, accountPrimaryDelta.getChangeType());

	}
	
	@Test
    public void test210AddAccount() throws Exception {
		final String TEST_NAME = "test210AddAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        ObjectDelta<ShadowType> accountDelta = ObjectDelta.createAddDelta(account);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        display("Input deltas: ", deltas);
        
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNull("Unexpected model focus context", focusContext);
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
		ModelProjectionContext accContext = projectionContexts.iterator().next();
		assertNotNull("Null model projection context", accContext);
		
		// Decision does not matter now
//		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<ShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 1);
	}
	
	@Test
    public void test221ModifyUserAddAccountRef() throws Exception {
        final String TEST_NAME = "test221ModifyUserAddAccountRef";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_GUYBRUSH_OID, prismContext);
        ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), 
        		ACCOUNT_SHADOW_GUYBRUSH_OID);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		display("Input deltas: ", userDelta);
                
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSecondaryDelta());
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 1, projectionContexts.size());
		ModelProjectionContext accContext = projectionContexts.iterator().next();
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNull("Unexpected account primary delta", accountPrimaryDelta);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta", accountSecondaryDelta);
	}
	
	// MAPPING TESTS
	// following tests mostly check correct functions of mappings
		
		
	// the test3xx is testing mappings with default dummy resource. It has NORMAL mappings.
	
	/**
	 * Changing ACCOUNT fullname (replace delta), no user changes.
	 */
	@Test
    public void test300ModifyElaineAccountDummyReplace() throws Exception {
        final String TEST_NAME = "test300ModifyElaineAccountDummyReplace";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
        		ACCOUNT_SHADOW_ELAINE_DUMMY_OID, resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
        		"Elaine Threepwood");
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
		display("Input deltas: ", deltas);
                
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSecondaryDelta());
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());
		
		ModelProjectionContext accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta", accountPrimaryDelta);
		PrismAsserts.assertModifications(accountPrimaryDelta, 1);
		PrismAsserts.assertPropertyReplace(accountPrimaryDelta, 
				getAttributePath(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta", accountSecondaryDelta);
	}
	
	/**
	 * Changing ACCOUNT fullname (add/delete delta), no user changes.
	 */
	@Test
    public void test301ModifyElaineAccountDummyDeleteAdd() throws Exception {
        final String TEST_NAME = "test301ModifyElaineAccountDummyDeleteAdd";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowEmptyDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        PropertyDelta<String> fullnameDelta = createAttributeAddDelta(resourceDummy, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
        fullnameDelta.addValueToDelete(new PrismPropertyValue<String>("Elaine Marley"));
        accountDelta.addModification(fullnameDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
		display("Input deltas: ", deltas);
                
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertSideEffectiveDeltasOnly("focus secondary delta", focusContext.getSecondaryDelta());
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());
		
		ModelProjectionContext accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta", accountPrimaryDelta);
		PrismAsserts.assertModifications(accountPrimaryDelta, 1);
		PrismAsserts.assertPropertyAdd(accountPrimaryDelta, 
				getAttributePath(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME), "Elaine Threepwood");
		PrismAsserts.assertPropertyDelete(accountPrimaryDelta, 
				getAttributePath(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME), "Elaine Marley");
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta", accountSecondaryDelta);
	}
		
	/**
	 * Changing ACCOUNT fullname (replace delta), no user changes.
	 * Attempt to make a change to a single-valued attribute or which there is already a strong mapping.
	 * As it cannot have both values (from the delta and from the mapping) the preview should fail.
	 */
	@Test
    public void test400ModifyElaineAccountDummyRedReplace() throws Exception {
        final String TEST_NAME = "test400ModifyElaineAccountDummyRedReplace";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
        		ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID, resourceDummyRed, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
		display("Input deltas: ", deltas);
               
		try {
			// WHEN
	        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
	        display("Preview context", modelContext);
	        
	        AssertJUnit.fail("Preview unexpectedly succeeded");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
        TestUtil.assertFailure(result);
	}
	
	/**
	 * Changing ACCOUNT fullname (add/delete delta), no user changes.
	 * Attempt to make a change to a single-valued attribute or which there is already a strong mapping.
	 * As it cannot have both values (from the delta and from the mapping) the preview should fail.
	 */
	@Test
    public void test401ModifyElaineAccountDummyRedDeleteAdd() throws Exception {
        final String TEST_NAME = "test401ModifyElaineAccountDummyRedDeleteAdd";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowEmptyDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID);
        PropertyDelta<String> fullnameDelta = createAttributeAddDelta(resourceDummyRed, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
        fullnameDelta.addValueToDelete(new PrismPropertyValue<String>("Elaine Marley"));
        accountDelta.addModification(fullnameDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
		display("Input deltas: ", deltas);
                
		try {
			// WHEN
	        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
	        display("Preview context", modelContext);
	        
	        AssertJUnit.fail("Preview unexpectedly succeeded");
		} catch (PolicyViolationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
        TestUtil.assertFailure(result);
	}

	// the test5xx is testing mappings with blue dummy resource. It has WEAK mappings.
	
	/**
	 * Changing ACCOUNT fullname (replace delta), no user changes.
	 */
	@Test
    public void test500ModifyElaineAccountDummyBlueReplace() throws Exception {
        final String TEST_NAME = "test500ModifyElaineAccountDummyBlueReplace";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
        		ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID, resourceDummyBlue, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
		display("Input deltas: ", deltas);
                
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());
		
		ModelProjectionContext accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta", accountPrimaryDelta);
		PrismAsserts.assertModifications(accountPrimaryDelta, 1);
		PrismAsserts.assertPropertyReplace(accountPrimaryDelta, 
				getAttributePath(resourceDummyBlue, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta", accountSecondaryDelta);
	}
	
	/**
	 * Changing ACCOUNT fullname (add/delete delta), no user changes.
	 */
	@Test
    public void test501ModifyElaineAccountDummyBlueDeleteAdd() throws Exception {
        final String TEST_NAME = "test501ModifyElaineAccountDummyBlueDeleteAdd";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowEmptyDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID);
        PropertyDelta<String> fullnameDelta = createAttributeAddDelta(resourceDummyBlue, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
        fullnameDelta.addValueToDelete(new PrismPropertyValue<String>("Elaine Marley"));
        accountDelta.addModification(fullnameDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
		display("Input deltas: ", deltas);
                
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());
		
		ModelProjectionContext accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta", accountPrimaryDelta);
		PrismAsserts.assertModifications(accountPrimaryDelta, 1);
		PrismAsserts.assertPropertyAdd(accountPrimaryDelta, 
				getAttributePath(resourceDummyBlue, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
		PrismAsserts.assertPropertyDelete(accountPrimaryDelta, 
				getAttributePath(resourceDummyBlue, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Marley");
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta", accountSecondaryDelta);
	}

	
	/**
	 * Changing USER fullName (replace delta), no account changes.
	 */
	@Test
    public void test600ModifyElaineUserDummyReplace() throws Exception {
        final String TEST_NAME = "test600ModifyElaineUserDummyReplace";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_ELAINE_OID, UserType.F_FULL_NAME, 
        		PrismTestUtil.createPolyString("Elaine Threepwood"));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		display("Input deltas: ", deltas);
                
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
		assertNotNull("No focus primary delta: "+userPrimaryDelta, userPrimaryDelta);
		PrismAsserts.assertModifications(userPrimaryDelta, 1);
		PrismAsserts.assertPropertyReplace(userPrimaryDelta, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Elaine Threepwood"));
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());
		
		// DEFAULT dummy resource: normal mappings
		ModelProjectionContext accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (default)", accContext);
		
		assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNull("Unexpected account primary delta (default)", accountPrimaryDelta);
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
		PrismAsserts.assertModifications(accountSecondaryDelta, 1);
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
				getAttributePath(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
		
		// RED dummy resource: strong mappings
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_RED_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (red)", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNull("Unexpected account primary delta (red)", accountPrimaryDelta);
		
        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
		PrismAsserts.assertModifications(accountSecondaryDelta, 1);
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
				getAttributePath(resourceDummyRed, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
		
		// BLUE dummy resource: weak mappings
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (blue)", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNull("Unexpected account primary delta (blue)", accountPrimaryDelta);
		
        accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 2);
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
				getAttributePath(resourceDummyBlue, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME),
				"SystemConfiguration");
		
	}
	
	/**
	 * Changing USER fullName (replace delta), change account fullname (replace delta).
	 */
	@Test
    public void test610ModifyElaineUserAccountDummyReplace() throws Exception {
        final String TEST_NAME = "test610ModifyElaineUserAccountDummyReplace";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_ELAINE_OID, UserType.F_FULL_NAME, 
        		PrismTestUtil.createPolyString("Elaine Threepwood"));
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
        		ACCOUNT_SHADOW_ELAINE_DUMMY_OID, resourceDummy, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine LeChuck");
        // Cannot change the attribute on RED resource. It would conflict with the strong mapping and therefore fail.
//        ObjectDelta<ResourceObjectShadowType> accountDeltaRed = createModifyAccountShadowReplaceAttributeDelta(
//        		ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID, resourceDummyRed, 
//        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine LeChuck");
        ObjectDelta<ShadowType> accountDeltaBlue = createModifyAccountShadowReplaceAttributeDelta(
        		ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID, resourceDummyBlue, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine LeChuck");
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta, 
				accountDeltaBlue);
		display("Input deltas: ", deltas);
                
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
		assertNotNull("No focus primary delta: "+userPrimaryDelta, userPrimaryDelta);
		PrismAsserts.assertModifications(userPrimaryDelta, 1);
		PrismAsserts.assertPropertyReplace(userPrimaryDelta, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Elaine Threepwood"));
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());
		
		// DEFAULT dummy resource: normal mappings
		ModelProjectionContext accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (default)", accContext);
		
		assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta (default)", accountPrimaryDelta);
		PrismAsserts.assertModifications(accountPrimaryDelta, 1);
		PrismAsserts.assertPropertyReplace(accountPrimaryDelta, 
				getAttributePath(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine LeChuck");
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta (default)", accountSecondaryDelta);
		
		// RED dummy resource: strong mappings
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_RED_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (red)", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNull("Unexpected account primary delta (red)", accountPrimaryDelta);		
        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
		PrismAsserts.assertModifications(accountSecondaryDelta, 1);
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
				getAttributePath(resourceDummyRed, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
		
		// BLUE dummy resource: weak mappings
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (blue)", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta (blue)", accountPrimaryDelta);
		PrismAsserts.assertModifications(accountPrimaryDelta, 1);
		PrismAsserts.assertPropertyReplace(accountPrimaryDelta, 
				getAttributePath(resourceDummyBlue, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine LeChuck");
		
        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta (blue)", accountSecondaryDelta);
		
	}
	
	@Test
    public void test620AddUserCapsize() throws Exception {
        final String TEST_NAME = "test620AddUserCapsize";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_CAPSIZE_FILE);
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);        
        
        ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
		assertNotNull("No focus primary delta: "+userPrimaryDelta, userPrimaryDelta);
		PrismAsserts.assertIsAdd(userPrimaryDelta);
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertSideEffectiveDeltasOnly(userSecondaryDelta, "focus secondary delta", ActivationStatusType.ENABLED);
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());
		
		// DEFAULT dummy resource: normal mappings
		ModelProjectionContext accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (default)", accContext);
		
		assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta (default)", accountPrimaryDelta);
		PrismAsserts.assertIsAdd(accountPrimaryDelta);
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
		PrismAsserts.assertModifications(accountSecondaryDelta, 8);
		PrismAsserts.assertNoItemDelta(accountSecondaryDelta, 
				getAttributePath(resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
		
		// RED dummy resource: strong mappings
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_RED_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (red)", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta (default)", accountPrimaryDelta);
		PrismAsserts.assertIsAdd(accountPrimaryDelta);
		
        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
		PrismAsserts.assertModifications(accountSecondaryDelta, 8);
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
				getAttributePath(resourceDummyRed, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Kate Capsize");
		
		// BLUE dummy resource: weak mappings
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (blue)", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta (default)", accountPrimaryDelta);
		PrismAsserts.assertIsAdd(accountPrimaryDelta);

        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (default)", accountSecondaryDelta);
		PrismAsserts.assertModifications(accountSecondaryDelta, 7);
		PrismAsserts.assertNoItemDelta(accountSecondaryDelta, 
				getAttributePath(resourceDummyBlue, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
		
	}
	
	// The 7xx tests try to do various non-common cases
	
	/**
	 * Enable two accounts at once. Both accounts belongs to the same user. But no user delta is here.
	 * This may cause problems when constructing the lens context inside model implementation.
	 */
	@Test
    public void test700DisableElaineAccountTwoResources() throws Exception {
        final String TEST_NAME = "test700DisableElaineAccountTwoResources";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<ShadowType> accountDeltaDefault = createModifyAccountShadowReplaceDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_OID, 
        		resourceDummy, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
        ObjectDelta<ShadowType> accountDeltaBlue = createModifyAccountShadowReplaceDelta(ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID, 
        		resourceDummyBlue, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDeltaDefault, accountDeltaBlue);
		display("Input deltas: ", deltas);
                
		// WHEN
        ModelContext<UserType> modelContext = modelInteractionService.previewChanges(deltas, new ModelExecuteOptions(), task, result);
		
		// THEN
        display("Preview context", modelContext);
		assertNotNull("Null model context", modelContext);
		
		result.computeStatus();
        TestUtil.assertSuccess(result);
		
		ModelElementContext<UserType> focusContext = modelContext.getFocusContext();
		assertNotNull("Null model focus context", focusContext);
		assertNull("Unexpected focus primary delta: "+focusContext.getPrimaryDelta(), focusContext.getPrimaryDelta());
		
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertSideEffectiveDeltasOnly("focus secondary delta", userSecondaryDelta);
		
		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());
		
		ModelProjectionContext accContextDefault = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (default)", accContextDefault);
		
		assertEquals("Wrong policy decision (default)", SynchronizationPolicyDecision.KEEP, accContextDefault.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDelta = accContextDefault.getPrimaryDelta();
		assertNotNull("No account primary delta (default)", accountPrimaryDelta);
		PrismAsserts.assertModifications(accountPrimaryDelta, 1);
		PrismAsserts.assertPropertyReplace(accountPrimaryDelta, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContextDefault.getSecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 2);
        assertNotNull("No disableTimestamp delta in account secodary delta (default)", 
        		accountSecondaryDelta.findPropertyDelta(
        				new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP)));
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
        		SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
		
		ModelProjectionContext accContextBlue = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (blue)", accContextBlue);
		
		assertEquals("Wrong policy decision (blue)", SynchronizationPolicyDecision.KEEP, accContextBlue.getSynchronizationPolicyDecision());
		ObjectDelta<ShadowType> accountPrimaryDeltaBlue = accContextBlue.getPrimaryDelta();
		assertNotNull("No account primary delta (blue)", accountPrimaryDeltaBlue);
		PrismAsserts.assertModifications(accountPrimaryDeltaBlue, 1);
		PrismAsserts.assertPropertyReplace(accountPrimaryDeltaBlue, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
		
        ObjectDelta<ShadowType> accountSecondaryDeltaBlue = accContextBlue.getSecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDeltaBlue, 2);
        assertNotNull("No disableTimestamp delta in account secodary delta (blue)", 
        		accountSecondaryDeltaBlue.findPropertyDelta(
        				new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP)));
        PrismAsserts.assertPropertyReplace(accountSecondaryDeltaBlue, SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
        		SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
	}
}
