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
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
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
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ObjectSource;
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
public class TestPreviewChanges extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/contract");

	private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
	private static final String USER_BLACKBEARD_OID = "c0c010c0-d34d-b33f-f00d-161161116666";
	
	private static String accountOid;
	
	public TestPreviewChanges() throws JAXBException {
		super();
	}
	
	@Test
    public void test100ModifyUserAddAccountBundle() throws Exception {
		final String TEST_NAME = "test100ModifyUserAddAccountBundle";
		final File accountFile = new File(ACCOUNT_JACK_DUMMY_FILENAME);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		ObjectSource<PrismObject<AccountShadowType>> accountSource = new ObjectSource<PrismObject<AccountShadowType>>() {
			@Override
			public PrismObject<AccountShadowType> get() {
				try {
					return PrismTestUtil.parseObject(accountFile);
				} catch (SchemaException e) {
					throw new IllegalStateException(e.getMessage(),e);
				}
			}
		};
        
		ObjectChecker<ModelContext<UserType,AccountShadowType>> checker = new ObjectChecker<ModelContext<UserType,AccountShadowType>>() {
			@Override
			public void check(ModelContext<UserType, AccountShadowType> modelContext) {
				assertAddAccount(modelContext);	
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
		final File accountFile = new File(ACCOUNT_JACK_DUMMY_FILENAME);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
		
		ObjectSource<PrismObject<AccountShadowType>> accountSource = new ObjectSource<PrismObject<AccountShadowType>>() {
			@Override
			public PrismObject<AccountShadowType> get() {
				try {
					PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(accountFile);
					account.removeContainer(AccountShadowType.F_ATTRIBUTES);
					return account;
				} catch (SchemaException e) {
					throw new IllegalStateException(e.getMessage(),e);
				}
			}
		};
        
		ObjectChecker<ModelContext<UserType,AccountShadowType>> checker = new ObjectChecker<ModelContext<UserType,AccountShadowType>>() {
			@Override
			public void check(ModelContext<UserType, AccountShadowType> modelContext) {
				assertAddAccount(modelContext);	
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
		
    private void modifyUserAddAccountImplicit(String bundleName, ObjectSource<PrismObject<AccountShadowType>> accountSource, 
    		ObjectChecker<ModelContext<UserType,AccountShadowType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "Implicit";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<AccountShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta);
		
		doPreview(deltas, checker, task, result);
    }
    
    private void modifyUserAddAccountExplicit(String bundleName, ObjectSource<PrismObject<AccountShadowType>> accountSource, 
    		ObjectChecker<ModelContext<UserType,AccountShadowType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "Explicit";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<AccountShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        ObjectDelta<AccountShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta, accountDelta);
        
		doPreview(deltas, checker, task, result);
	}
    
    private void modifyUserAddAccountImplicitExplicitSame(String bundleName, 
    		ObjectSource<PrismObject<AccountShadowType>> accountSource, ObjectChecker<ModelContext<UserType,AccountShadowType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitSame";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<AccountShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		ObjectDelta<AccountShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta, accountDelta);
        
		doPreview(deltas, checker, task, result);
	}
	
    private void modifyUserAddAccountImplicitExplicitSameReverse(String bundleName, 
    		ObjectSource<PrismObject<AccountShadowType>> accountSource, ObjectChecker<ModelContext<UserType,AccountShadowType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitSameReverse";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<AccountShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		ObjectDelta<AccountShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(accountDelta, userDelta);
        
		doPreview(deltas, checker, task, result);
	}
    
    private void modifyUserAddAccountImplicitExplicitEqual(String bundleName, 
    		ObjectSource<PrismObject<AccountShadowType>> accountSource, ObjectChecker<ModelContext<UserType,AccountShadowType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitEqual";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<AccountShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account.clone());
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		ObjectDelta<AccountShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta, accountDelta);
        
		doPreview(deltas, checker, task, result);
	}
    
    private void modifyUserAddAccountImplicitExplicitEqualReverse(String bundleName, 
    		ObjectSource<PrismObject<AccountShadowType>> accountSource, ObjectChecker<ModelContext<UserType,AccountShadowType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitEqualReverse";
        displayTestTile(this, TEST_NAME);
	
        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<AccountShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account.clone());
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		ObjectDelta<AccountShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(accountDelta, userDelta);
        
		doPreview(deltas, checker, task, result);
	}
	
    private void modifyUserAddAccountImplicitExplicitNotEqual(String bundleName, 
    		ObjectSource<PrismObject<AccountShadowType>> accountSource, ObjectChecker<ModelContext<UserType,AccountShadowType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitNotEqual";
        displayTestTile(this, TEST_NAME);
    
        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<AccountShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account.clone());
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		// Let's make the account different. This should cause the preview to fail
		account.asObjectable().setDescription("aye!");
		ObjectDelta<AccountShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(userDelta, accountDelta);
        
		doPreviewFail(deltas, task, result);
	}
	
    private void modifyUserAddAccountImplicitExplicitNotEqualReverse(String bundleName, 
    		ObjectSource<PrismObject<AccountShadowType>> accountSource, ObjectChecker<ModelContext<UserType,AccountShadowType>> checker) throws Exception {
		final String TEST_NAME = bundleName + "ImplicitExplicitNotEqualReverse";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<AccountShadowType> account = accountSource.get();
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
        PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account.clone());
		ReferenceDelta accountRefDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), accountRefVal);
		userDelta.addModification(accountRefDelta);
		// Let's make the account different. This should cause the preview to fail
		account.asObjectable().setDescription("aye!");
		ObjectDelta<AccountShadowType> accountDelta = account.createAddDelta();
		Collection<ObjectDelta<? extends ObjectType>> deltas = (Collection)MiscUtil.createCollection(accountDelta, userDelta);
		
		doPreviewFail(deltas, task, result);
	}
	
	private void doPreview(Collection<ObjectDelta<? extends ObjectType>> deltas, 
			ObjectChecker<ModelContext<UserType,AccountShadowType>> checker, Task task, OperationResult result) 
					throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
					ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		display("Input deltas: ", deltas);
        
		// WHEN
		ModelContext<UserType,AccountShadowType> modelContext = modelInteractionService.previewChanges(deltas, task, result);
		
		// THEN
		display("Preview context", modelContext);
		checker.check(modelContext);
	}
	
	private void doPreviewFail(Collection<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult result) 
					throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException,
					ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		display("Input deltas: ", deltas);
        
		try {
			// WHEN
			ModelContext<UserType,AccountShadowType> modelContext = modelInteractionService.previewChanges(deltas, task, result);
			
			AssertJUnit.fail("Expected exception, but it haven't come");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}
	}
	
	private void assertAddAccount(ModelContext<UserType, AccountShadowType> modelContext) {
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
    public void test200ModifyUserDeleteAccount() throws Exception {
		final String TEST_NAME = "test200ModifyUserDeleteAccount";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_HBARBOSSA_OPENDJ_FILENAME));
        		
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_BARBOSSA_OID, prismContext);
		PrismReferenceValue accountRefVal = new PrismReferenceValue();
		accountRefVal.setObject(account);
		ReferenceDelta accountDelta = ReferenceDelta.createModificationDelete(UserType.F_ACCOUNT_REF, getUserDefinition(), account);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		display("Input deltas: ", deltas);
        
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
    public void test210AddAccount() throws Exception {
		final String TEST_NAME = "test210AddAccount";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        PrismObject<AccountShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_JACK_DUMMY_FILENAME));
        ObjectDelta<AccountShadowType> accountDelta = ObjectDelta.createAddDelta(account);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
        display("Input deltas: ", deltas);
        
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
    public void test221ModifyUserAddAccountRef() throws Exception {
        final String TEST_NAME = "test221ModifyUserAddAccountRef";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_GUYBRUSH_OID, prismContext);
        ReferenceDelta accountDelta = ReferenceDelta.createModificationAdd(UserType.F_ACCOUNT_REF, getUserDefinition(), 
        		ACCOUNT_SHADOW_GUYBRUSH_OID);
		userDelta.addModification(accountDelta);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
		display("Input deltas: ", userDelta);
                
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
	
	// MAPPING TESTS
	// following tests mostly check correct functions of mappings
		
		
	@Test
    public void test300ModifyElaineAccountDummyReplace() throws Exception {
        final String TEST_NAME = "test300ModifyElaineAccountDummyReplace";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestPreviewChanges.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        ObjectDelta<AccountShadowType> accountDelta = createModifyAccountShadowReplaceAttributeDelta(
        		ACCOUNT_SHADOW_ELAINE_DUMMY_OID, resourceDummy, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elaine Threepwood");
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);
		display("Input deltas: ", deltas);
                
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
		assertEquals("Unexpected number of projection contexts", 3, projectionContexts.size());
		
		ModelProjectionContext<AccountShadowType> accContext = modelContext.findProjectionContext(new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, null));
		assertNotNull("Null model projection context", accContext);
		
		assertEquals("Wrong policy decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
		ObjectDelta<AccountShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
		assertNotNull("No account primary delta", accountPrimaryDelta);
		PrismAsserts.assertModifications(accountPrimaryDelta, 1);
		PrismAsserts.assertPropertyReplace(accountPrimaryDelta, 
				getAttributePath(resourceDummy, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME), "Elaine Threepwood");
		
        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Unexpected account secondary delta", accountSecondaryDelta);
	}

}
