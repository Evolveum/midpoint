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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
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
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;
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
	
	public static final File TEST_DIR = new File("src/test/resources/preview");

	// LEMON dummy resource has a STRICT dependency on default dummy resource
	protected static final File RESOURCE_DUMMY_LEMON_FILE = new File(TEST_DIR, "resource-dummy-lemon.xml");
	protected static final String RESOURCE_DUMMY_LEMON_OID = "10000000-0000-0000-0000-000000000504";
	protected static final String RESOURCE_DUMMY_LEMON_NAME = "lemon";
	protected static final String RESOURCE_DUMMY_LEMON_NAMESPACE = MidPointConstants.NS_RI;

	private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
	private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";

	static final File USER_ROGERS_FILE = new File(TEST_DIR, "user-rogers.xml");
	
	private static String accountOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		initDummyResourcePirate(RESOURCE_DUMMY_LEMON_NAME, 
				RESOURCE_DUMMY_LEMON_FILE, RESOURCE_DUMMY_LEMON_OID, initTask, initResult);
	}

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
        assertEquals(getDummyResourceController().getAccountObjectClass(),
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceObject().getOid(), resourceRef.getOid());

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
    public void test130GetAdminGuiConfig() throws Exception {
		final String TEST_NAME = "test130GetAdminGuiConfig";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
		AdminGuiConfigurationType adminGuiConfiguration = modelInteractionService.getAdminGuiConfiguration(task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		assertAdminGuiConfigurations(adminGuiConfiguration, 0, 1, 3, 1, 0);
		
		RichHyperlinkType link = adminGuiConfiguration.getUserDashboardLink().get(0);
		assertEquals("Bad link label", "Foo", link.getLabel());
		assertEquals("Bad link targetUrl", "/foo", link.getTargetUrl());
		
		assertEquals("Bad timezone targetUrl", "Jamaica", adminGuiConfiguration.getDefaultTimezone());
	}
	
	@Test
    public void test150GetGuybrushRefinedObjectClassDef() throws Exception {
		final String TEST_NAME = "test150GetGuybrushRefinedObjectClassDef";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<ShadowType> shadow = getShadowModel(ACCOUNT_SHADOW_GUYBRUSH_OID);
        
		// WHEN
		RefinedObjectClassDefinition rOCDef = modelInteractionService.getEditObjectClassDefinition(shadow, 
				getDummyResourceObject(), AuthorizationPhaseType.REQUEST);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		display("Refined object class", rOCDef);
		assertNotNull("Null config", rOCDef);
		
		display("Password credentials outbound", rOCDef.getPasswordOutbound());
		assertNotNull("Assert not null", rOCDef.getPasswordOutbound());
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
        assertEquals(getDummyResourceController().getAccountObjectClass(),
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceObject().getOid(), resourceRef.getOid());

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
        assertEquals("Unexpected size of account secondary delta: "+accountSecondaryDelta, 2, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME), "rum");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME), "Arr!");
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
        		ACCOUNT_SHADOW_ELAINE_DUMMY_OID, getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
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
				getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals("Unexpected size of account secondary delta: "+accountSecondaryDelta, 2, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME), "rum");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME), "Arr!");
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
        PropertyDelta<String> fullnameDelta = createAttributeAddDelta(getDummyResourceObject(), 
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
				getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME), "Elaine Threepwood");
		PrismAsserts.assertPropertyDelete(accountPrimaryDelta, 
				getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME), "Elaine Marley");
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals("Unexpected size of account secondary delta: "+accountSecondaryDelta, 2, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME), "rum");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME), "Arr!");
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
        		ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID, getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), 
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
        PropertyDelta<String> fullnameDelta = createAttributeAddDelta(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), 
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

        display("elaine blue account before", getDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_ELAINE_DUMMY_BLUE_USERNAME));
        
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
		assertNotNull("Missing account secondary delta", accountSecondaryDelta);
		// these are originally empty attributes with weak mappings
        assertEquals("Wrong items in account secondary delta", new HashSet<>(
				Arrays.asList(
				dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME),
        		dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME),
        		dummyResourceCtlBlue.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME))),
				new HashSet<>(accountSecondaryDelta.getModifiedItems()));
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
		PrismAsserts.assertModifications(accountSecondaryDelta, 3);
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
				getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME), "rum");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME), "Arr!");
		
		// RED dummy resource: strong mappings
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_RED_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (red)", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNull("Unexpected account primary delta (red)", accountPrimaryDelta);
		
        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
		PrismAsserts.assertModifications(accountSecondaryDelta, 2);
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
				getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
		PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
				getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
				"null -- red resource");
		
		// BLUE dummy resource: weak mappings
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_BLUE_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (blue)", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNull("Unexpected account primary delta (blue)", accountPrimaryDelta);
		
        accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertModifications(accountSecondaryDelta, 3);
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
				ACCOUNT_SHADOW_ELAINE_DUMMY_OID, getDummyResourceObject(),
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
				getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine LeChuck");
		
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals("Unexpected size of account secondary delta (default): "+accountSecondaryDelta, 2, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME), "rum");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME), "Arr!");
        
		// RED dummy resource: strong mappings
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_RED_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (red)", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNull("Unexpected account primary delta (red)", accountPrimaryDelta);		
        accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta (red)", accountSecondaryDelta);
		PrismAsserts.assertModifications(accountSecondaryDelta, 2);
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
				getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
				"Elaine Threepwood");
		PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
				getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
				"null -- red resource");
		
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
        PrismAsserts.assertModifications("account secondary delta (blue)", accountSecondaryDelta, 3);
		
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
		assertNotNull("No focus primary delta: " + userPrimaryDelta, userPrimaryDelta);
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
				getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
		
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
				getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
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
		PrismAsserts.assertModifications(accountSecondaryDelta, 8);
		PrismAsserts.assertNoItemDelta(accountSecondaryDelta,
				getAttributePath(resourceDummyBlue, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
		
	}

	// testing multiple resources with dependencies (dummy -> dummy lemon)

	@Test
	public void test630AddUserRogers() throws Exception {
		final String TEST_NAME = "test630AddUserRogers";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_ROGERS_FILE);
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
		// inbound from ship (explicitly specified) to organizationalUnit (dummy resource)
		// inbound from gossip (computed via outbound) to description (lemon resource)
		assertEffectualDeltas(userSecondaryDelta, "focus secondary delta", ActivationStatusType.ENABLED, 2);

		PrismObject<UserType> finalUser = user.clone();
		userSecondaryDelta.applyTo(finalUser);
		PrismAsserts.assertOrigEqualsPolyStringCollectionUnordered("Wrong organizationalUnit attribute",
				finalUser.asObjectable().getOrganizationalUnit(), "The crew of The Sea Monkey");
		assertEquals("Wrong description attribute", "Rum Rogers Sr. must be the best pirate the  has ever seen", finalUser.asObjectable().getDescription());

		Collection<? extends ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
		assertNotNull("Null model projection context list", projectionContexts);
		assertEquals("Unexpected number of projection contexts", 2, projectionContexts.size());

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
		// administrativeStatus (ENABLED), enableTimestamp, name, drink, quote, iteration, iterationToken, password/value
		PrismAsserts.assertModifications(accountSecondaryDelta, 8);
		PrismAsserts.assertNoItemDelta(accountSecondaryDelta,
				getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));

		// LEMON dummy resource
		accContext = modelContext.findProjectionContext(
				new ResourceShadowDiscriminator(RESOURCE_DUMMY_LEMON_OID, ShadowKindType.ACCOUNT, null));
		assertNotNull("Null model projection context (lemon)", accContext);

		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
		accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta (default)", accountPrimaryDelta);
		PrismAsserts.assertIsAdd(accountPrimaryDelta);

		accountSecondaryDelta = accContext.getSecondaryDelta();
		assertNotNull("No account secondary delta (lemon)", accountSecondaryDelta);
		// administrativeStatus (ENABLED), enableTimestamp, ship (from organizationalUnit), name, gossip, water, iteration, iterationToken, password/value
		PrismAsserts.assertModifications(accountSecondaryDelta, 9);
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
				getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_LEMON_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
				"The crew of The Sea Monkey");
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
				new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME),
				"rogers");
		PrismAsserts.assertPropertyAdd(accountSecondaryDelta,
				getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_LEMON_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME),
				"Rum Rogers Sr. must be the best pirate the  has ever seen");
		PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
				getAttributePath(getDummyResourceObject(RESOURCE_DUMMY_LEMON_NAME), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME),
				"pirate Rum Rogers Sr. drinks only rum!");
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
        		getDummyResourceObject(), ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
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
        PrismAsserts.assertModifications(accountSecondaryDelta, 4);
        assertNotNull("No disableTimestamp delta in account secodary delta (default)", 
        		accountSecondaryDelta.findPropertyDelta(
        				new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP)));
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
        		SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
				getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME),
				"rum");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
				getAttributePath(getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
				"Arr!");
		
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
