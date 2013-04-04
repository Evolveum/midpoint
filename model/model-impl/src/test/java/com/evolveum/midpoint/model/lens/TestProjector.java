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
package com.evolveum.midpoint.model.lens;

import static com.evolveum.midpoint.model.lens.LensTestConstants.REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.lens.projector.Projector;
import com.evolveum.midpoint.model.test.DummyResourceContoller;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProjector extends AbstractInternalModelIntegrationTest {
		
	public static final String TEST_DIR_NAME = "src/test/resources/lens";
	public static final String USER_BARBOSSA_MODIFY_ASSIGNMENT_REPLACE_AC = TEST_DIR_NAME + "/user-barbossa-modify-assignment-replace-ac.xml";
	
	@Autowired(required = true)
	private Projector projector;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	public TestProjector() throws JAXBException {
		super();
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		setDefaultUserTemplate(USER_TEMPLATE_OID);
	}

	@Test
    public void test000Sanity() throws Exception {
        displayTestTile(this, "test000Sanity");

        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceDummyType, prismContext);
        
        dummyResourceCtl.assertRefinedSchemaSanity(refinedSchema);
        
        assertNoJackShadow();
	}
	
	@Test
    public void test010BasicContextOperations() throws Exception {
		final String TEST_NAME = "test010BasicContextOperations";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext<ShadowType> accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, result);
        
        // User deltas
        ObjectDelta<UserType> userDeltaPrimary = createModifyUserReplaceDelta(USER_ELAINE_OID, UserType.F_FULL_NAME, 
        		PrismTestUtil.createPolyString("Elaine Threepwood"));
        ObjectDelta<UserType> userDeltaPrimaryClone = userDeltaPrimary.clone();
        ObjectDelta<UserType> userDeltaSecondary = createModifyUserReplaceDelta(USER_ELAINE_OID, UserType.F_FULL_NAME, 
        		PrismTestUtil.createPolyString("Elaine LeChuck"));
        ObjectDelta<UserType> userDeltaSecondaryClone = userDeltaSecondary.clone();
        focusContext.setPrimaryDelta(userDeltaPrimary);
        focusContext.setSecondaryDelta(userDeltaSecondary, 0);
        
        // Account Deltas
        ObjectDelta<ShadowType> accountDeltaPrimary = createModifyAccountShadowReplaceAttributeDelta(
        		ACCOUNT_SHADOW_ELAINE_DUMMY_OID, resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elie Marley");
        ObjectDelta<ShadowType> accountDeltaPrimaryClone = accountDeltaPrimary.clone();
        assert accountDeltaPrimaryClone != accountDeltaPrimary : "clone is not cloning";
        assert accountDeltaPrimaryClone.getModifications() != accountDeltaPrimary.getModifications() : "clone is not cloning (modifications)";
        ObjectDelta<ShadowType> accountDeltaSecondary = createModifyAccountShadowReplaceAttributeDelta(
        		ACCOUNT_SHADOW_ELAINE_DUMMY_OID, resourceDummy, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elie LeChuck");
        ObjectDelta<ShadowType> accountDeltaSecondaryClone = accountDeltaSecondary.clone();
        accountContext.setPrimaryDelta(accountDeltaPrimary);
        accountContext.setSecondaryDelta(accountDeltaSecondary);
		
        display("Context before", context);
        
        // WHEN: checkConsistence
        context.checkConsistence();
        
        display("Context after checkConsistence", context);
        
        assert focusContext == context.getFocusContext() : "focus context delta replaced";
        assert focusContext.getPrimaryDelta() == userDeltaPrimary : "focus primary delta replaced";
        assert userDeltaPrimaryClone.equals(userDeltaPrimary) : "focus primary delta changed";
        
        ObjectDelta<UserType> focusSecondaryDelta = focusContext.getSecondaryDelta();
        display("Focus secondary delta", focusSecondaryDelta);
        display("Orig user secondary delta", userDeltaSecondaryClone);
        assert focusSecondaryDelta.equals(userDeltaSecondaryClone) : "focus secondary delta not equal";
        
        assert accountContext == context.findProjectionContext(new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, null))
        		: "wrong account context";
        assert accountContext.getPrimaryDelta() == accountDeltaPrimary : "account primary delta replaced";
        assert accountDeltaPrimaryClone.equals(accountDeltaPrimary) : "account primary delta changed";
        assert accountContext.getSecondaryDelta() == accountDeltaSecondary : "account secondary delta replaced";
        assert accountDeltaSecondaryClone.equals(accountDeltaSecondary) : "account secondary delta changed";
        
        // WHEN: recompute
        context.recompute();
        
        display("Context after recompute", context);
        
        assert focusContext == context.getFocusContext() : "focus context delta replaced";
        assert focusContext.getPrimaryDelta() == userDeltaPrimary : "focus primary delta replaced";
        
        focusSecondaryDelta = focusContext.getSecondaryDelta();
        display("Focus secondary delta", focusSecondaryDelta);
        display("Orig user secondary delta", userDeltaSecondaryClone);
        assert focusSecondaryDelta.equals(userDeltaSecondaryClone) : "focus secondary delta not equal";
        
        assert accountContext == context.findProjectionContext(new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, null))
        		: "wrong account context";
        assert accountContext.getPrimaryDelta() == accountDeltaPrimary : "account primary delta replaced";
        display("Orig account primary delta", accountDeltaPrimaryClone);
        display("Account primary delta after recompute", accountDeltaPrimary);
        assert accountDeltaPrimaryClone.equals(accountDeltaPrimary) : "account primary delta changed";
        assert accountContext.getSecondaryDelta() == accountDeltaSecondary : "account secondary delta replaced";
        assert accountDeltaSecondaryClone.equals(accountDeltaSecondary) : "account secondary delta changed";
        
	}

	
	@Test
    public void test100AddAccountToJackDirect() throws Exception {
		final String TEST_NAME = "test100AddAccountToJackDirect";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        // We want "shadow" so the fullname will be computed by outbound expression 
        addModificationToContextAddAccountFromFile(context, ACCOUNT_SHADOW_JACK_DUMMY_FILENAME);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertNull("Unexpected user primary changes"+context.getFocusContext().getPrimaryDelta(), context.getFocusContext().getPrimaryDelta());
        assertNull("Unexpected user secondary changes"+context.getFocusContext().getSecondaryDelta(), context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        
        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<ShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        PrismProperty<Object> intentProperty = accountToAddPrimary.findProperty(ShadowType.F_INTENT);
        assertNotNull("No account type in account primary add delta", intentProperty);
        assertEquals(DEFAULT_INTENT, intentProperty.getRealValue());
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());
        PrismAsserts.assertNoEmptyItem(accountToAddPrimary);

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        PropertyDelta<String> fullNameDelta = accountSecondaryDelta.findPropertyDelta(dummyResourceCtl.getAttributeFullnamePath());
        PrismAsserts.assertReplace(fullNameDelta, "Jack Sparrow");
        PrismAsserts.assertOrigin(fullNameDelta, OriginType.OUTBOUND);

        PrismObject<ShadowType> accountNew = accContext.getObjectNew();
        IntegrationTestTools.assertIcfsNameAttribute(accountNew, "jack");
        IntegrationTestTools.assertAttribute(accountNew, dummyResourceCtl.getAttributeFullnameQName(), "Jack Sparrow");
        IntegrationTestTools.assertAttribute(accountNew, dummyResourceCtl.getAttributeWeaponQName(), "mouth", "pistol");
	}
	
	@Test
    public void test110AssignAccountToJack() throws Exception {
		final String TEST_NAME = "test110AssignAccountToJack";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        assertAssignAccountToJack(context);
	}
	
	/**
	 * Same sa previous test but the deltas are slightly broken.
	 */
	@Test
    public void test111AssignAccountToJackBroken() throws Exception {
		final String TEST_NAME = "test111AssignAccountToJackBroken";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // Let's break it a bit...
        breakAssignmentDelta(context);
        
        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        assertAssignAccountToJack(context);
	}

	private void assertAssignAccountToJack(LensContext<UserType, ShadowType> context) {
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        
        assertEquals("Wrong decision", SynchronizationPolicyDecision.ADD,accContext.getSynchronizationPolicyDecision());
        
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, getIcfsNameAttributePath() , "jack");
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, dummyResourceCtl.getAttributeFullnamePath() , "Jack Sparrow");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME) , "mouth", "pistol");
        
        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.OUTBOUND);

	}


	/**
	 * User barbossa has a direct account assignment. This assignment has an expression for user/locality -> dummy/location.
	 * Let's try if the "l" gets updated if we update barbosa's locality.
	 */
	@Test
    public void test250ModifyUserBarbossaLocality() throws Exception {
		final String TEST_NAME = "test250ModifyUserBarbossaLocality";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_LOCALITY, PrismTestUtil.createPolyString("Tortuga"));
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 1, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME) , "Tortuga");
        
        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.ASSIGNMENTS);
                
    }
	
	/**
	 * User barbossa has a direct account assignment. This assignment has an expression for user/fullName -> dummy/fullname.
	 * cn is also overriden to be single-value.
	 * Let's try if the "cn" gets updated if we update barbosa's fullName. Also check if delta is replace.
	 */
	@Test
    public void test251ModifyUserBarbossaFullname() throws Exception {
		final String TEST_NAME = "test251ModifyUserBarbossaFullname";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Hector Barbossa"));
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 1, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME) , 
        		"Captain Hector Barbossa");
        
        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.OUTBOUND);
                
    }
	
	/**
	 * User barbossa has a direct account assignment. This assignment has an expression for enabledisable flag.
	 * Let's disable user, the account should be disabled as well.
	 */
	@Test
    public void test254ModifyUserBarbossaDisable() throws Exception {
		final String TEST_NAME = "test254ModifyUserBarbossaDisable";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addModificationToContextReplaceUserProperty(context,
        		new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED),
        		false);
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 1, accountSecondaryDelta.getModifications().size());
        PropertyDelta<Boolean> enabledDelta = accountSecondaryDelta.findPropertyDelta(new ItemPath(ShadowType.F_ACTIVATION, 
        		ActivationType.F_ENABLED));
        PrismAsserts.assertReplace(enabledDelta, false);
        PrismAsserts.assertOrigin(enabledDelta, OriginType.OUTBOUND);
    }
	
	/**
	 * User barbossa has a direct account assignment. Let's modify that assignment and see if the
	 * changes will be reflected.
	 */
	@Test
    public void test255ModifyUserBarbossaAssignment() throws Exception {
		final String TEST_NAME = "test255ModifyUserBarbossaAssignment";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addModificationToContext(context, USER_BARBOSSA_MODIFY_ASSIGNMENT_REPLACE_AC);
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        // This micro-change in assignment will trigger reconciliation, this will cause that also other attributes
        // will be recomputed (e.g. "ship") and will bounce back by INBOUND
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        display("User Secondary Delta", userSecondaryDelta);
        assertEquals("Unexpected number of user secondary changes", 1, userSecondaryDelta.getModifications().size());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta", accountSecondaryDelta);
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        // There is a lot of changes caused byt the reconciliation. But we are only interested in the new one
        assertEquals("Unexpected number of account secondary changes", 4, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
        		"Arr!", "Pirate of Caribbean");
        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.RECONCILIATION);
                
    }
	
	/**
	 * The drink attribute is NOT tolerant. Therefore an attempt to manually change it using
	 * account primary delta should fail.
	 */
	@Test
    public void test260ModifyAccountBarbossaDrinkReplace() throws Exception {
		final String TEST_NAME = "test260ModifyAccountBarbossaDrinkReplace";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addModificationToContextReplaceAccountAttribute(context, ACCOUNT_HBARBOSSA_DUMMY_OID, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "Water");
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        try {
	        // WHEN
	        projector.project(context, "test", result);
	        
	        AssertJUnit.fail("Unexpected success of projector");
        } catch (PolicyViolationException e) {
        	// This is expected
        	
        }
                        
    }
	
	/**
	 * The quote attribute has a strong mapping and is tolerant. Therefore an attempt to manually change it using
	 * account primary delta should succeed. The modification is by purpose a replace modification. Therefore the
	 * value from the mapping should be explicitly added in the secondary delta even though the mapping is static
	 * and it was not changed.
	 */
	@Test
    public void test261ModifyAccountBarbossaQuoteReplace() throws Exception {
		final String TEST_NAME = "test261ModifyAccountBarbossaQuoteReplace";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addModificationToContextReplaceAccountAttribute(context, ACCOUNT_HBARBOSSA_DUMMY_OID, 
        		DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "I'm disinclined to acquiesce to your request.");
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertNull("Unexpected user primary changes", context.getFocusContext().getPrimaryDelta());
        assertNull("Unexpected user secondary changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();

        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.MODIFY, accountPrimaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 1, accountPrimaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME) , 
        		"I'm disinclined to acquiesce to your request.");
        
        assertEquals(SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta", accountSecondaryDelta);
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 1, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, 
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME) , 
        		"Arr!");
        
        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.OUTBOUND);
                
    }
	
	
	/**
	 * User barbossa has a direct account assignment.
	 * Let's try to delete assigned account. It should end up with a policy violation error.
	 */
	@Test
    public void test269DeleteBarbossaDummyAccount() throws Exception {
		final String TEST_NAME = "test269DeleteBarbossaDummyAccount";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        // Do not fill user to context. Projector should figure that out.
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addModificationToContextDeleteAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID);
        context.recompute();

        display("Input context", context);

        try {
        	
            // WHEN        	
        	projector.project(context, "test", result);

            // THEN: fail
        	display("Output context", context);
        	assert context.getFocusContext() != null : "The operation was successful but it should throw expcetion AND " +
        			"there is no focus context";
        	assert false : "The operation was successful but it should throw expcetion";
        } catch (PolicyViolationException e) {
        	// THEN: success
        	// this is expected
        	display("Expected exception",e);
        }
        
    }
	
	@Test
    public void test301AssignConflictingAccountToJack() throws Exception {
		final String TEST_NAME = "test301AssignConflictingAccountToJack";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        
        // Make sure there is a shadow with conflicting account
        addObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILENAME, ShadowType.class, result);
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        
        assertEquals("Wrong decision", SynchronizationPolicyDecision.ADD,accContext.getSynchronizationPolicyDecision());
        
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, getIcfsNameAttributePath() , "jack");
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, dummyResourceCtl.getAttributeFullnamePath() , "Jack Sparrow");

        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.OUTBOUND);
	}
	
	@Test
    public void test400ImportHermanDummy() throws Exception {
		final String TEST_NAME = "test400ImportHermanDummy";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        context.setChannel(SchemaConstants.CHANGE_CHANNEL_IMPORT);
        fillContextWithEmtptyAddUserDelta(context, result);
        fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_DUMMY_FILENAME, result);
        makeImportSyncDelta(context.getProjectionContexts().iterator().next());
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        // TODO
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.ADD);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        PrismAsserts.assertOrigin(userSecondaryDelta, OriginType.INBOUND);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_DESCRIPTION, "Came from Monkey Island");
        
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH);

        // TODO
        
    }
	
	@Test
    public void test401ImportHermanDummy() throws Exception {
		final String TEST_NAME = "test401ImportHermanDummy";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        context.setChannel(SchemaConstants.CHANGE_CHANNEL_IMPORT);
        fillContextWithEmtptyAddUserDelta(context, result);
        fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_DUMMY_FILENAME, result);
        makeImportSyncDelta(context.getProjectionContexts().iterator().next());
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        // TODO
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.ADD);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        PrismAsserts.assertOrigin(userSecondaryDelta, OriginType.INBOUND);
        
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);
        
    }
	
	@Test
    public void test450GuybrushInboundFromDelta() throws Exception {
		final String TEST_NAME = "test450GuybrushInboundFromDelta";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        fillContextWithAccount(context, ACCOUNT_SHADOW_GUYBRUSH_OID, result);
        addSyncModificationToContextReplaceAccountAttribute(context, ACCOUNT_SHADOW_GUYBRUSH_OID, "ship", "Black Pearl");
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertNoUserPrimaryDelta(context);
        assertUserSecondaryDelta(context);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertTrue(userSecondaryDelta.getChangeType() == ChangeType.MODIFY);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_ORGANIZATIONAL_UNIT , 
        		PrismTestUtil.createPolyString("The crew of Black Pearl"));
        PrismAsserts.assertOrigin(userSecondaryDelta, OriginType.INBOUND);
    }

	@Test
    public void test451GuybrushInboundFromAbsolute() throws Exception {
		final String TEST_NAME = "test451GuybrushInboundFromAbsolute";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        try{
        	PrismObject<ValuePolicyType> passPolicy = PrismTestUtil.parseObject(new File(PASSWORD_POLICY_GLOBAL_FILENAME));
        	Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        	ObjectDelta refDelta = ObjectDelta.createModificationAddReference(SystemConfigurationType.class, SYSTEM_CONFIGURATION_OID, SystemConfigurationType.F_GLOBAL_PASSWORD_POLICY_REF, prismContext, passPolicy);
        	Collection<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
        	deltas.add(refDelta);
        	modelService.executeChanges(deltas, null, task, result);
        	
        	PrismObject<SystemConfigurationType> sysConfig = modelService.getObject(SystemConfigurationType.class, SYSTEM_CONFIGURATION_OID, null, task, result);
        	assertNotNull(sysConfig.asObjectable().getGlobalPasswordPolicyRef());
        	assertEquals(PASSWORD_POLICY_GLOBAL_OID, sysConfig.asObjectable().getGlobalPasswordPolicyRef().getOid());
        	
        	ObjectDelta delta = ObjectDelta.createAddDelta(passPolicy);
        	deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        	deltas.add(delta);
        	modelService.executeChanges(deltas, null, task, result);
        	
        	PrismObject<ValuePolicyType> passPol = modelService.getObject(ValuePolicyType.class, PASSWORD_POLICY_GLOBAL_OID, null, task, result);
        	assertNotNull(passPol);
        	        	
        } catch (Exception ex){
        	throw ex;
        }

        // GIVEN
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        fillContextWithAccountFromFile(context, ACCOUNT_GUYBRUSH_DUMMY_FILENAME, result);
        LensProjectionContext<ShadowType> guybrushAccountContext = context.findProjectionContextByOid(ACCOUNT_SHADOW_GUYBRUSH_OID);
        guybrushAccountContext.setFullShadow(true);
        guybrushAccountContext.setDoReconciliation(true);
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertNoUserPrimaryDelta(context);
        assertUserSecondaryDelta(context);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertTrue(userSecondaryDelta.getChangeType() == ChangeType.MODIFY);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_ORGANIZATIONAL_UNIT , 
        		PrismTestUtil.createPolyString("The crew of The Sea Monkey"));
        PrismAsserts.assertOrigin(userSecondaryDelta, OriginType.INBOUND);
    }

	
	@Test
    public void test500ReconcileGuybrushDummy() throws Exception {
		final String TEST_NAME = "test500ReconcileGuybrushDummy";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        
        // Change the guybrush account on dummy resource directly. This creates inconsistency.
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Fuycrush Greepdood");
        dummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Phatt Island");
        
        LensContext<UserType, ShadowType> context = createUserAccountContext();
        context.setChannel(SchemaConstants.CHANGE_CHANNEL_RECON);
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        context.setDoReconciliationForAllProjections(true);
        
        display("Guybrush account before: ", dummyAccount);

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertNull("User primary delta sneaked in", context.getFocusContext().getPrimaryDelta());
        
        // There is an inbound mapping for password that generates it if not present. it is triggered in this case.
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertTrue(userSecondaryDelta.getChangeType() == ChangeType.MODIFY);
        assertEquals("Unexpected number of modifications in user secondary delta", 1, userSecondaryDelta.getModifications().size());
        ItemDelta modification = userSecondaryDelta.getModifications().iterator().next();
        assertEquals("Unexpected modification", PasswordType.F_VALUE, modification.getName());
        PrismAsserts.assertOrigin(userSecondaryDelta, OriginType.INBOUND);

        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        
        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH);
        PropertyDelta<String> fullnameDelta = accountSecondaryDelta.findPropertyDelta(
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME));
        assertNotNull("No fullname Delta in account secondary delta", fullnameDelta);
        PrismAsserts.assertReplace(fullnameDelta, "Guybrush Threepwood");
        PrismAsserts.assertOrigin(fullnameDelta, OriginType.RECONCILIATION);
        PropertyDelta<String> locationDelta = accountSecondaryDelta.findPropertyDelta(
        		dummyResourceCtl.getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        PrismAsserts.assertReplace(locationDelta, "Melee Island");
        PrismAsserts.assertOrigin(locationDelta, OriginType.RECONCILIATION);
        
    }
	
	/**
	 * Let's add user without a fullname. The expression in user template should compute it.
	 */
	@Test
    public void test600AddLargo() throws Exception {
		final String TEST_NAME = "test600AddLargo";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + "." + TEST_NAME);

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(USER_LARGO_FILENAME));
        fillContextWithAddUserDelta(context, user);

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        // TODO
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.ADD);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        assertFalse("Empty user secondary delta", userSecondaryDelta.isEmpty());
        PrismAsserts.assertPropertyReplace(userSecondaryDelta, UserType.F_FULL_NAME, 
        		PrismTestUtil.createPolyString("Largo LaGrande"));
        
    }

	
	private void assertNoJackShadow() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		PrismObject<ShadowType> jackAccount = findAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME, resourceDummy);
        assertNull("Found jack's shadow!", jackAccount);
	}

}
