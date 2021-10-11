/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProjector extends AbstractLensTest {

    public static final File USER_BARBOSSA_MODIFY_ASSIGNMENT_REPLACE_AC_FILE = new File(TEST_DIR,
            "user-barbossa-modify-assignment-replace-ac.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        setDefaultUserTemplate(USER_TEMPLATE_OID);
        addObject(ORG_BRETHREN_FILE);
        addObject(ROLE_MUTINEER_FILE);

//        repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);

        InternalMonitor.reset();
    }

    @Test
    public void test000Sanity() throws Exception {
        assertNoJackShadow();
    }

    @Test
    public void test010BasicContextOperations() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        LensContext<UserType> context = createUserLensContext();
        LensFocusContext<UserType> focusContext = fillContextWithUser(context, USER_ELAINE_OID, result);
        LensProjectionContext accountContext = fillContextWithAccount(context, ACCOUNT_SHADOW_ELAINE_DUMMY_OID, task, result);

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
                ACCOUNT_SHADOW_ELAINE_DUMMY_OID, getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elie Marley");
        ObjectDelta<ShadowType> accountDeltaPrimaryClone = accountDeltaPrimary.clone();
        assert accountDeltaPrimaryClone != accountDeltaPrimary : "clone is not cloning";
        assert accountDeltaPrimaryClone.getModifications() != accountDeltaPrimary.getModifications() : "clone is not cloning (modifications)";
        ObjectDelta<ShadowType> accountDeltaSecondary = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_OID, getDummyResourceObject(), DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elie LeChuck");
        ObjectDelta<ShadowType> accountDeltaSecondaryClone = accountDeltaSecondary.clone();
        accountContext.setPrimaryDelta(accountDeltaPrimary);
        accountContext.setSecondaryDelta(accountDeltaSecondary);

        displayDumpable("Context before", context);

        // WHEN: checkConsistence
        context.checkConsistence();

        displayDumpable("Context after checkConsistence", context);

        assert focusContext == context.getFocusContext() : "focus context delta replaced";
        assert focusContext.getPrimaryDelta() == userDeltaPrimary : "focus primary delta replaced";
        assert userDeltaPrimaryClone.equals(userDeltaPrimary) : "focus primary delta changed";

        ObjectDelta<UserType> focusSecondaryDelta = focusContext.getSecondaryDelta();
        displayDumpable("Focus secondary delta", focusSecondaryDelta);
        displayDumpable("Orig user secondary delta", userDeltaSecondaryClone);
        assert focusSecondaryDelta.equals(userDeltaSecondaryClone) : "focus secondary delta not equal";

        assert accountContext == context.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, null, false))
                : "wrong account context";
        assert accountContext.getPrimaryDelta() == accountDeltaPrimary : "account primary delta replaced";
        assert accountDeltaPrimaryClone.equals(accountDeltaPrimary) : "account primary delta changed";
        assert accountContext.getSecondaryDelta() == accountDeltaSecondary : "account secondary delta replaced";
        assert accountDeltaSecondaryClone.equals(accountDeltaSecondary) : "account secondary delta changed";

        // WHEN: recompute
        context.recompute();

        displayDumpable("Context after recompute", context);

        assert focusContext == context.getFocusContext() : "focus context delta replaced";
        assert focusContext.getPrimaryDelta() == userDeltaPrimary : "focus primary delta replaced";

        focusSecondaryDelta = focusContext.getSecondaryDelta();
        displayDumpable("Focus secondary delta", focusSecondaryDelta);
        displayDumpable("Orig user secondary delta", userDeltaSecondaryClone);
        assert focusSecondaryDelta.equals(userDeltaSecondaryClone) : "focus secondary delta not equal";

        assert accountContext == context.findProjectionContext(
                new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, null, false))
                : "wrong account context";
        assert accountContext.getPrimaryDelta() == accountDeltaPrimary : "account primary delta replaced";
        displayDumpable("Orig account primary delta", accountDeltaPrimaryClone);
        displayDumpable("Account primary delta after recompute", accountDeltaPrimary);
        assert accountDeltaPrimaryClone.equals(accountDeltaPrimary) : "account primary delta changed";
        assert accountContext.getSecondaryDelta() == accountDeltaSecondary : "account secondary delta replaced";
        assert accountDeltaSecondaryClone.equals(accountDeltaSecondary) : "account secondary delta changed";

    }

    @Test
    public void test100AddAccountToJackDirect() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        // We want "shadow" so the fullname will be computed by outbound expression
        addModificationToContextAddAccountFromFile(context, ACCOUNT_SHADOW_JACK_DUMMY_FILE);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        displayDumpable("Output context", context);
        // Not loading anything. The account is already loaded in the context
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertNull("Unexpected user primary changes " + context.getFocusContext().getPrimaryDelta(), context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta", ActivationStatusType.ENABLED);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();

        assertEquals("Wrong policy decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());
        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<ShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        PrismProperty<Object> intentProperty = accountToAddPrimary.findProperty(ShadowType.F_INTENT);
        assertNotNull("No account type in account primary add delta", intentProperty);
        assertEquals(DEFAULT_INTENT, intentProperty.getRealValue());
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(getDummyResourceType()), "AccountObjectClass"),
                accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(getDummyResourceType().getOid(), resourceRef.getOid());
        accountToAddPrimary.checkConsistence();

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        PropertyDelta<String> fullNameDelta = accountSecondaryDelta.findPropertyDelta(
                getDummyResourceController().getAttributeFullnamePath());
        PrismAsserts.assertReplace(fullNameDelta, "Jack Sparrow");
        PrismAsserts.assertOrigin(fullNameDelta, OriginType.OUTBOUND);

        PrismObject<ShadowType> accountNew = accContext.getObjectNew();
        IntegrationTestTools.assertIcfsNameAttribute(accountNew, "jack");
        IntegrationTestTools.assertAttribute(accountNew,
                getDummyResourceController().getAttributeFullnameQName(), "Jack Sparrow");
        IntegrationTestTools.assertAttribute(accountNew,
                getDummyResourceController().getAttributeWeaponQName(), "mouth", "pistol");
    }

    @Test
    public void test110AssignAccountToJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        assertAssignAccountToJack(context);
    }

    /**
     * Same sa previous test but the deltas are slightly broken.
     */
    @Test
    public void test111AssignAccountToJackBroken() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // Let's break it a bit...
        breakAssignmentDelta(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        assertAssignAccountToJack(context);
    }

    private void assertAssignAccountToJack(LensContext<UserType> context) {
        displayDumpable("Output context", context);
        // Not loading anything. The account is already loaded in the context
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta", ActivationStatusType.ENABLED);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();

        assertEquals("Wrong decision", SynchronizationPolicyDecision.ADD, accContext.getSynchronizationPolicyDecision());

        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, getIcfsNameAttributePath(), "jack");
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                getDummyResourceController().getAttributeFullnamePath(), "Jack Sparrow");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME), "mouth", "pistol");

        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.OUTBOUND);

    }

    /**
     * User barbossa has a direct account assignment. This assignment has an expression for user/locality -> dummy/location.
     * Let's try if the "l" gets updated if we update barbosa's locality.
     */
    @Test
    public void test250ModifyUserBarbossaLocality() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_LOCALITY, PrismTestUtil.createPolyString("Tortuga"));
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly("user secondary delta", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 3, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Tortuga");

        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.ASSIGNMENTS, OriginType.OUTBOUND);

    }

    /**
     * User barbossa has a direct account assignment. This assignment has an expression for user/fullName -> dummy/fullname.
     * cn is also overridden to be single-value.
     * Let's try if the "cn" gets updated if we update barbosa's fullName. Also check if delta is replace.
     */
    @Test
    public void test251ModifyUserBarbossaFullname() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Hector Barbossa"));
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly("user secondary delta", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 3, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME),
                "Captain Hector Barbossa");

        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.OUTBOUND);

    }

    /**
     * User barbossa has a direct account assignment. This assignment has an expression for enabledisable flag.
     * Let's disable user, the account should be disabled as well.
     */
    @Test
    public void test254ModifyUserBarbossaDisable() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextReplaceUserProperty(context, PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ActivationStatusType.DISABLED);
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta", ActivationStatusType.DISABLED);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta", accountSecondaryDelta);
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 6, accountSecondaryDelta.getModifications().size());
        PropertyDelta<ActivationStatusType> enabledDelta = accountSecondaryDelta.findPropertyDelta(PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        PrismAsserts.assertReplace(enabledDelta, ActivationStatusType.DISABLED);
        PrismAsserts.assertOrigin(enabledDelta, OriginType.OUTBOUND);
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, SchemaConstants.PATH_ACTIVATION_DISABLE_REASON,
                SchemaConstants.MODEL_DISABLE_REASON_MAPPED);

        ContainerDelta<TriggerType> triggerDelta = accountSecondaryDelta.findContainerDelta(ObjectType.F_TRIGGER);
        assertNotNull("No trigger delta in account secondary delta", triggerDelta);
        assertEquals("Wrong trigger delta size", 1, triggerDelta.getValuesToAdd().size());
        TriggerType triggerType = triggerDelta.getValuesToAdd().iterator().next().asContainerable();
        assertEquals("Wrong trigger URL", RecomputeTriggerHandler.HANDLER_URI, triggerType.getHandlerUri());
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 25, 0, 0, 0));
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        end.add(XmlTypeConverter.createDuration(true, 0, 0, 35, 0, 0, 0));
        TestUtil.assertBetween("Wrong trigger timestamp", start, end, triggerType.getTimestamp());
    }

    /**
     * User barbossa has a direct account assignment. Let's modify that assignment and see if the
     * changes will be reflected.
     */
    @Test
    public void test255ModifyUserBarbossaAssignment() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addFocusModificationToContext(context, USER_BARBOSSA_MODIFY_ASSIGNMENT_REPLACE_AC_FILE);
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        displayDumpable("User Secondary Delta", userSecondaryDelta);
        assertSideEffectiveDeltasOnly("user secondary delta", userSecondaryDelta);

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta", accountSecondaryDelta);
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        // There is a lot of changes caused by the reconciliation. But we are only interested in the new one
        assertEquals("Unexpected number of account secondary changes", 3, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
                "Pirate of Caribbean");
        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.RECONCILIATION, OriginType.OUTBOUND);

    }

    /**
     * The drink attribute is NOT tolerant. Therefore an attempt to manually change it using
     * account primary delta should fail.
     */
    @Test
    public void test260ModifyAccountBarbossaDrinkReplace() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextReplaceAccountAttribute(context, ACCOUNT_HBARBOSSA_DUMMY_OID,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "Water");
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        assertPartialError(result);
    }

    /**
     * The quote attribute has a strong mapping and is tolerant. Therefore an attempt to manually change it using
     * account primary delta should succeed. The modification is by purpose a replace modification. Therefore the
     * value from the mapping should be explicitly added in the secondary delta even though the mapping is static
     * and it was not changed.
     */
    @Test
    public void test261ModifyAccountBarbossaQuoteReplace() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextReplaceAccountAttribute(context, ACCOUNT_HBARBOSSA_DUMMY_OID,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "I'm disinclined to acquiesce to your request.");
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertNull("Unexpected user primary changes", context.getFocusContext().getPrimaryDelta());
        assertSideEffectiveDeltasOnly("user secondary delta", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();

        ObjectDelta<ShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.MODIFY, accountPrimaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 1, accountPrimaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(accountPrimaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
                "I'm disinclined to acquiesce to your request.");

        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta", accountSecondaryDelta);
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 3, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME),
                "Arr!");

        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.OUTBOUND);

    }

    /**
     * User barbossa has a direct account assignment.
     * Let's try to delete assigned account. It should end up with a policy violation error.
     */
    @Test
    public void test269DeleteBarbossaDummyAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        // Do not fill user to context. Projector should figure that out.
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextDeleteAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID);
        context.recompute();

        displayDumpable("Input context", context);

        try {

            // WHEN
            projector.project(context, "test", task, result);

            // THEN: fail
            displayDumpable("Output context", context);
            assert context.getFocusContext() != null : "The operation was successful but it should throw expcetion AND " +
                    "there is no focus context";
            assert false : "The operation was successful but it should throw expcetion";
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

    }

    @Test
    public void test270AddUserBarbossaAssignmentBrethren() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addFocusDeltaToContext(context, createAssignmentUserDelta(USER_BARBOSSA_OID, ORG_BRETHREN_OID,
                OrgType.COMPLEX_TYPE, null, null, true));
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        displayDumpable("User Secondary Delta", userSecondaryDelta);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_ORGANIZATION, PrismTestUtil.createPolyString(ORG_BRETHREN_INDUCED_ORGANIZATION));

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta", accountSecondaryDelta);
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        // iteration & iterationToken
        assertEquals("Unexpected number of account secondary changes", 2, accountSecondaryDelta.getModifications().size());

    }

    @Test
    public void test275DeleteUserBarbossaAssignmentBrethren() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_ORGANIZATION, task, result, PrismTestUtil.createPolyString(ORG_BRETHREN_INDUCED_ORGANIZATION));
        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> focus = repositoryService.getObject(UserType.class, USER_BARBOSSA_OID, null, result);
        ObjectDelta<UserType> addAssignmentDelta = createAssignmentUserDelta(USER_BARBOSSA_OID, ORG_BRETHREN_OID,
                OrgType.COMPLEX_TYPE, null, null, true);
        addAssignmentDelta.applyTo(focus);
        fillContextWithFocus(context, focus);

        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);

        addFocusDeltaToContext(context, createAssignmentUserDelta(USER_BARBOSSA_OID, ORG_BRETHREN_OID,
                OrgType.COMPLEX_TYPE, null, null, false));

        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        displayDumpable("User Secondary Delta", userSecondaryDelta);
        PrismAsserts.assertPropertyDelete(userSecondaryDelta, UserType.F_ORGANIZATION, PrismTestUtil.createPolyString(ORG_BRETHREN_INDUCED_ORGANIZATION));

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
    }

    @Test
    public void test280AddUserBarbossaAssignmentMutineer() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addFocusDeltaToContext(context, createAssignmentUserDelta(USER_BARBOSSA_OID, ROLE_MUTINEER_OID,
                RoleType.COMPLEX_TYPE, null, null, true));
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertSideEffectiveDeltasOnly("user secondary delta", userSecondaryDelta);

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta", accountSecondaryDelta);
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 1, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta,
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME),
                "Damned mutineer");
        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.ASSIGNMENTS);
    }

    @Test
    public void test301AssignConflictingAccountToJack() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // Make sure there is a shadow with conflicting account
        repoAddObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILE, result);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta", ActivationStatusType.ENABLED);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();

        // Not sure about this. KEEP or ADD?
        assertEquals("Wrong decision", SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, getDummyResourceController().getAttributeFullnamePath(), "Jack Sparrow");
    }

    @Test
    public void test400ImportHermanDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        context.setChannel(SchemaConstants.CHANGE_CHANNEL_IMPORT);
        fillContextWithEmtptyAddUserDelta(context);
        fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_DUMMY_FILE, task, result);
        makeImportSyncDelta(context.getProjectionContexts().iterator().next());
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        // TODO

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.ADD);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_DESCRIPTION, "Came from Monkey Island");

        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH_PARTS);

        // Activation is created in user policy. Therefore assert the origin of that as special case
        // and remove it from the delta so the next assert passes
        Iterator<? extends ItemDelta<?, ?>> iterator = userSecondaryDelta.getModifications().iterator();
        while (iterator.hasNext()) {
            ItemDelta<?, ?> modification = iterator.next();
            if (modification.getPath().startsWithName(UserType.F_ACTIVATION)) {
                PrismAsserts.assertOrigin(modification, OriginType.USER_POLICY);
                iterator.remove();
            }
        }
        assertOriginWithSideEffectChanges(userSecondaryDelta, OriginType.INBOUND);
    }

    @Test
    public void test401ImportHermanDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        context.setChannel(SchemaConstants.CHANGE_CHANNEL_IMPORT);
        fillContextWithEmtptyAddUserDelta(context);
        fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_DUMMY_FILE, task, result);
        makeImportSyncDelta(context.getProjectionContexts().iterator().next());
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        // TODO

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.ADD);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);

        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals("Unexpected number of account secondary changes", 2, accountSecondaryDelta.getModifications().size());

        assertOriginWithSideEffectChanges(userSecondaryDelta, OriginType.INBOUND);
    }

    @Test
    public void test450GuybrushInboundFromDelta() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        fillContextWithAccount(context, ACCOUNT_SHADOW_GUYBRUSH_OID, task, result);
        addSyncModificationToContextReplaceAccountAttribute(context, ACCOUNT_SHADOW_GUYBRUSH_OID, "ship", "Black Pearl");
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertNoUserPrimaryDelta(context);
        assertUserSecondaryDelta(context);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertSame(userSecondaryDelta.getChangeType(), ChangeType.MODIFY);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_ORGANIZATIONAL_UNIT,
                PrismTestUtil.createPolyString("The crew of Black Pearl"));
        assertOriginWithSideEffectChanges(userSecondaryDelta, OriginType.INBOUND);
    }

    @Test
    public void test451GuybrushInboundFromAbsolute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        addObject(PASSWORD_POLICY_GLOBAL_FILE);
        applyPasswordPolicy(PASSWORD_POLICY_GLOBAL_OID, SECURITY_POLICY_OID, task, result);

        // GIVEN
        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        fillContextWithAccountFromFile(context, ACCOUNT_GUYBRUSH_DUMMY_FILE, task, result);
        LensProjectionContext guybrushAccountContext = context.findProjectionContextByOid(ACCOUNT_SHADOW_GUYBRUSH_OID);
        guybrushAccountContext.setFullShadow(true);
        guybrushAccountContext.setDoReconciliation(true);
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertNoUserPrimaryDelta(context);
        assertUserSecondaryDelta(context);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertSame(userSecondaryDelta.getChangeType(), ChangeType.MODIFY);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_ORGANIZATIONAL_UNIT,
                PrismTestUtil.createPolyString("The crew of The Sea Monkey"));
        assertOriginWithSideEffectChanges(userSecondaryDelta, OriginType.INBOUND);
    }

    @Test
    public void test500ReconcileGuybrushDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        // Change the guybrush account on dummy resource directly. This creates inconsistency.
        DummyAccount dummyAccount = getDummyResource().getAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Fuycrush Greepdood");
        dummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Phatt Island");

        LensContext<UserType> context = createUserLensContext();
        context.setChannel(SchemaConstants.CHANGE_CHANNEL_RECON);
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        context.setDoReconciliationForAllProjections(true);

        displayDumpable("Guybrush account before: ", dummyAccount);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        assertNull("User primary delta sneaked in", context.getFocusContext().getPrimaryDelta());

        // There is an inbound mapping for password that generates it if not present. it is triggered in this case.
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertSame(userSecondaryDelta.getChangeType(), ChangeType.MODIFY);
        assertEquals("Unexpected number of modifications in user secondary delta", 9, userSecondaryDelta.getModifications().size());
        ItemDelta<?, ?> modification = userSecondaryDelta.getModifications().iterator().next();
        assertEquals("Unexpected modification", PasswordType.F_VALUE, modification.getElementName());
        assertOriginWithSideEffectChanges(userSecondaryDelta, OriginType.INBOUND);

        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH_PARTS);
        // Full name is not changed, it has normal mapping strength
        // Location is changed back, it has strong mapping
        PropertyDelta<String> locationDelta = accountSecondaryDelta.findPropertyDelta(
                getDummyResourceController().getAttributePath(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        assertNotNull("No location delta in projection secondary delta", locationDelta);
        PrismAsserts.assertReplace(locationDelta, "Melee Island");
        PrismAsserts.assertOrigin(locationDelta, OriginType.RECONCILIATION);
    }

    /**
     * Let's add user without a fullname. The expression in user template should compute it.
     */
    @Test
    public void test600AddLargo() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

//        result.tracingProfile(tracer.compileProfile(createModelLoggingTracingProfile(), result));

        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_LARGO_FILE);
        fillContextWithAddUserDelta(context, user);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);

        // TODO

//        tracer.storeTrace(task, result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.ADD);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertNotNull("No user secondary delta", userSecondaryDelta);
        assertFalse("Empty user secondary delta", userSecondaryDelta.isEmpty());
        PrismAsserts.assertPropertyReplace(userSecondaryDelta, UserType.F_FULL_NAME,
                PrismTestUtil.createPolyString("Largo LaGrande"));
        PrismAsserts.assertPropertyReplace(userSecondaryDelta, UserType.F_NICK_NAME,
                PrismTestUtil.createPolyString("Largo LaGrande"));        // MID-2149
    }

    private void assertNoJackShadow() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> jackAccount = findAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceObject());
        assertNull("Found jack's shadow!", jackAccount);
    }

    private void assertOriginWithSideEffectChanges(ObjectDelta<UserType> delta, OriginType expectedOrigi) {
        // Activation is created in user policy. Therefore assert the origin of that as special case
        // and remove it from the delta so the next assert passes
        Iterator<? extends ItemDelta<?, ?>> iterator = delta.getModifications().iterator();
        while (iterator.hasNext()) {
            ItemDelta<?, ?> modification = iterator.next();
            QName firstName = modification.getPath().firstToName();
            if (firstName.equals(UserType.F_ACTIVATION) ||
                    firstName.equals(FocusType.F_ITERATION) || firstName.equals(FocusType.F_ITERATION_TOKEN)) {
                PrismAsserts.assertOrigin(modification, OriginType.USER_POLICY);
                iterator.remove();
            }
            if (modification.getPath().containsNameExactly(ObjectType.F_METADATA)) {
                iterator.remove();
            }
        }
        PrismAsserts.assertOrigin(delta, expectedOrigi);
    }
}
