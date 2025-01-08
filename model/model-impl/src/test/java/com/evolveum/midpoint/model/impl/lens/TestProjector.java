/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;
import static com.evolveum.midpoint.test.util.MidPointAsserts.assertSerializable;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.context.ProjectionContextFilter;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProjector extends AbstractLensTest {

    private static final File USER_BARBOSSA_MODIFY_ASSIGNMENT_REPLACE_AC_FILE =
            new File(TEST_DIR, "user-barbossa-modify-assignment-replace-ac.xml");

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
        focusContext.swallowToSecondaryDelta(userDeltaSecondary.getModifications());

        // Account Deltas
        ObjectDelta<ShadowType> accountDeltaPrimary = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_OID, getDummyResourceObject(), DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elie Marley");
        ObjectDelta<ShadowType> accountDeltaPrimaryClone = accountDeltaPrimary.clone();
        assert accountDeltaPrimaryClone != accountDeltaPrimary : "clone is not cloning";
        assert accountDeltaPrimaryClone.getModifications() != accountDeltaPrimary.getModifications() : "clone is not cloning (modifications)";
        ObjectDelta<ShadowType> accountDeltaSecondary = createModifyAccountShadowReplaceAttributeDelta(
                ACCOUNT_SHADOW_ELAINE_DUMMY_OID, getDummyResourceObject(), DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Elie LeChuck");
        ObjectDelta<ShadowType> accountDeltaSecondaryClone = accountDeltaSecondary.clone();
        accountContext.setPrimaryDelta(accountDeltaPrimary);
        accountContext.swallowToSecondaryDelta(accountDeltaSecondary.getModifications());

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
        assert withoutOldValues(focusSecondaryDelta).equals(userDeltaSecondaryClone) : "focus secondary delta not equal";

        assert accountContext == context.findProjectionContext(
                new ProjectionContextFilter(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT))
                : "wrong account context";
        assert accountContext.getPrimaryDelta() == accountDeltaPrimary : "account primary delta replaced";
        assert accountDeltaPrimaryClone.equals(accountDeltaPrimary) : "account primary delta changed";
        assert accountDeltaSecondaryClone.equals(accountDeltaSecondary) : "account secondary delta changed";

        assert focusContext == context.getFocusContext() : "focus context delta replaced";
        assert focusContext.getPrimaryDelta() == userDeltaPrimary : "focus primary delta replaced";

        focusSecondaryDelta = focusContext.getSecondaryDelta();
        displayDumpable("Focus secondary delta", focusSecondaryDelta);
        displayDumpable("Orig user secondary delta", userDeltaSecondaryClone);

        assert withoutOldValues(focusSecondaryDelta).equals(userDeltaSecondaryClone) : "focus secondary delta not equal";

        assert accountContext == context.findProjectionContextByKeyExact(
                ProjectionContextKey.classified(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT, null))
                : "wrong account context";
        assert accountContext.getPrimaryDelta() == accountDeltaPrimary : "account primary delta replaced";
        displayDumpable("Orig account primary delta", accountDeltaPrimaryClone);
        displayDumpable("Account primary delta after recompute", accountDeltaPrimary);
        assert accountDeltaPrimaryClone.equals(accountDeltaPrimary) : "account primary delta changed";
        assert accountDeltaSecondaryClone.equals(accountDeltaSecondary) : "account secondary delta changed";
    }

    private ObjectDelta<?> withoutOldValues(ObjectDelta<?> delta) {
        if (delta != null) {
            ObjectDelta<?> clone = delta.clone();
            clone.removeEstimatedOldValues();
            return clone;
        } else {
            return null;
        }
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
        getDummyResourceType();
        assertEquals(RI_ACCOUNT_OBJECT_CLASS, accountToAddPrimary.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
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

        assertSerializable(context);
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

        assertSerializable(context);
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

        assertSerializable(context);
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
                DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH, "mouth", "pistol");

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
        assertEquals("Unexpected number of account secondary changes", 4, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta,
                DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_PATH, "Tortuga");
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH, "Sword");

        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.ASSIGNMENTS, OriginType.OUTBOUND);

        assertSerializable(context);
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
        assertEquals("Unexpected number of account secondary changes", 4, accountSecondaryDelta.getModifications().size());

        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Captain Hector Barbossa");

        PrismAsserts.assertOrigin(accountSecondaryDelta.findItemDelta(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH), OriginType.OUTBOUND);
        PrismAsserts.assertOrigin(accountSecondaryDelta.findItemDelta(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH), OriginType.ASSIGNMENTS);

        assertSerializable(context);
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
        assertEquals("Unexpected number of account secondary changes", 7, accountSecondaryDelta.getModifications().size());
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
        TestUtil.assertBetween("trigger timestamp", start, end, triggerType.getTimestamp());

        assertSerializable(context);
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
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH, "Pirate of Caribbean");

        PrismAsserts.assertOrigin(accountSecondaryDelta.findItemDelta(DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH), OriginType.ASSIGNMENTS);
        PrismAsserts.assertOrigin(accountSecondaryDelta.findItemDelta(ShadowType.F_ITERATION), OriginType.OUTBOUND);
        PrismAsserts.assertOrigin(accountSecondaryDelta.findItemDelta(ShadowType.F_ITERATION_TOKEN), OriginType.OUTBOUND);

        assertSerializable(context);
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
                DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "Water");

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        assertPartialError(result);

        assertSerializable(context);
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
                DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "I'm disinclined to acquiesce to your request.");

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
                DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH,
                "I'm disinclined to acquiesce to your request.");

        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNotNull("No account secondary delta", accountSecondaryDelta);
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 4, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH, "Arr!");

        PrismAsserts.assertOrigin(accountSecondaryDelta.findItemDelta(DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_PATH), OriginType.OUTBOUND); // OK?
        PrismAsserts.assertOrigin(accountSecondaryDelta.findItemDelta(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH), OriginType.ASSIGNMENTS);
        PrismAsserts.assertOrigin(accountSecondaryDelta.findItemDelta(ShadowType.F_ITERATION), OriginType.OUTBOUND);
        PrismAsserts.assertOrigin(accountSecondaryDelta.findItemDelta(ShadowType.F_ITERATION_TOKEN), OriginType.OUTBOUND);

        assertSerializable(context);
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

        displayDumpable("Input context", context);

        try {

            // WHEN
            projector.project(context, "test", task, result);

            // THEN: fail
            displayDumpable("Output context", context);
            assert context.getFocusContext() != null : "The operation was successful but it should throw exception AND " +
                    "there is no focus context";
            assert false : "The operation was successful but it should throw exception";
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        assertSerializable(context);
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
        // iteration & iterationToken & sword
        assertEquals("Unexpected number of account secondary changes", 3, accountSecondaryDelta.getModifications().size());

        assertSerializable(context);
    }

    @Test
    public void test275DeleteUserBarbossaAssignmentBrethren() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        var orgPropValue = PrismContext.get().itemFactory().createPropertyValue();
        orgPropValue.setValue(PrismTestUtil.createPolyString(ORG_BRETHREN_INDUCED_ORGANIZATION));
        orgPropValue.setValueMetadata(new ValueMetadataType()
                .provenance(new ProvenanceMetadataType()
                        .mappingSpecification(new MappingSpecificationType()
                                .definitionObjectRef("9c6bfc9a-ca01-11e3-a5aa-001e8c717e5b", OrgType.COMPLEX_TYPE)
                                .mappingName("brethen-organization")
                        )));
        // @formatter:off
        repositoryService.modifyObject(
                UserType.class,
                USER_BARBOSSA_OID,
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ORGANIZATION)
                            .replace(orgPropValue)
                        .item(UserType.F_ASSIGNMENT)
                            .add(new AssignmentType()
                                .targetRef(ORG_BRETHREN_OID, OrgType.COMPLEX_TYPE))
                        .asItemDeltas(),
                result);
        // @formatter:on
        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> focus = repositoryService.getObject(UserType.class, USER_BARBOSSA_OID, null, result);
        fillContextWithFocus(context, focus);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);

        addFocusDeltaToContext(context, createAssignmentUserDelta(USER_BARBOSSA_OID, ORG_BRETHREN_OID,
                OrgType.COMPLEX_TYPE, null, null, false));

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
            try {
                projector.project(context, "test", task, result);
            } catch (ConflictDetectedException e) {
                throw new RuntimeException(e);
            }

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

        assertSerializable(context);
        // Delete the assignment, to restore the environment for further tests
        // @formatter:off
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                            .delete(new AssignmentType()
                                .targetRef(ORG_BRETHREN_OID, OrgType.COMPLEX_TYPE))
                        .asObjectDelta(USER_BARBOSSA_OID),
                null,
                task,
                result);
        // @formatter:on
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
        PrismAsserts.assertPropertyAdd(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_PATH, "Damned mutineer");
        PrismAsserts.assertOrigin(accountSecondaryDelta, OriginType.ASSIGNMENTS);

        assertSerializable(context);
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

        assertSerializable(context);
    }

    @Test
    public void test400ImportHermanDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        context.setChannel(SchemaConstants.CHANNEL_IMPORT);
        fillContextWithEmptyAddUserDelta(context);
        var projCtx = fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_DUMMY_FILE, task, result);
        projCtx.setFullShadow(true);
        makeImportSyncDelta(context.getProjectionContexts().iterator().next());

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
        PrismAsserts.assertPropertyReplace(userSecondaryDelta, UserType.F_DESCRIPTION, "Came from Monkey Island");

        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaConstants.ICFS_NAME_PATH);

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

        assertSerializable(context);
    }

    @Test
    public void test401ImportHermanDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        context.setChannel(SchemaConstants.CHANNEL_IMPORT);
        fillContextWithEmptyAddUserDelta(context);
        var projCtx = fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_DUMMY_FILE, task, result);
        projCtx.setFullShadow(true);
        makeImportSyncDelta(context.getProjectionContexts().iterator().next());

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

        assertSerializable(context);
    }

    @Test
    public void test450GuybrushInboundFromDelta() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        var projCtx = fillContextWithAccount(context, ACCOUNT_SHADOW_GUYBRUSH_OID, task, result);
        var delta = addSyncModificationToContextReplaceAccountAttribute(
                context, ACCOUNT_SHADOW_GUYBRUSH_OID, "ship", "Black Pearl");

        // We have to manually apply the delta to the initial shadow, because the delta is the sync delta, meaning
        // it is thought to be already applied to the shadow in question.
        var shadow = projCtx.getObjectOld().clone();
        delta.applyTo(shadow);
        projCtx.setInitialObject(shadow);
        projCtx.setFullShadow(true); // To run the inbound mappings

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

        assertSerializable(context);
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

        assertSerializable(context);
    }

    @Test
    public void test500ReconcileGuybrushDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);

        // Change the guybrush account on dummy resource directly. This creates inconsistency.
        DummyAccount dummyAccount = getDummyResource().getAccountByName(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccount.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Fuycrush Greepdood");
        dummyAccount.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Phatt Island");

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_OID);

        LensContext<UserType> context = createUserLensContext();
        context.setChannel(SchemaConstants.CHANNEL_RECON);
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
        assertTrue("Modifications in user secondary delta do not match expected ones",
                MiscUtil.unorderedCollectionEquals(
                        userSecondaryDelta.getModifiedItems(),
                        List.of(
                                PATH_PASSWORD_VALUE,
                                PATH_ACTIVATION_EFFECTIVE_STATUS,
                                PATH_ACTIVATION_ENABLE_TIMESTAMP,
                                PATH_PASSWORD_METADATA,
                                UserType.F_ITERATION,
                                UserType.F_ITERATION_TOKEN),
                        (a, b) -> a.equivalent(b)));
        ItemDelta<?, ?> modification = userSecondaryDelta.getModifications().iterator().next();
        assertEquals("Unexpected modification", PasswordType.F_VALUE, modification.getElementName());
        assertOriginWithSideEffectChanges(userSecondaryDelta, OriginType.INBOUND);

        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaConstants.ICFS_NAME_PATH);
        // Full name is not changed, it has normal mapping strength
        // Location is changed back, it has strong mapping
        PropertyDelta<String> locationDelta = accountSecondaryDelta.findPropertyDelta(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_PATH);
        assertNotNull("No location delta in projection secondary delta", locationDelta);
        PrismAsserts.assertReplace(locationDelta, "Melee Island");

        // now the value is applied by the IvwoConsolidator, not waiting for the reconciliation
        PrismAsserts.assertOrigin(locationDelta, OriginType.OUTBOUND);

        assertSerializable(context);
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

        assertSerializable(context);
    }

    private void assertNoJackShadow() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> jackAccount = findAccountByUsername(ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceObject());
        assertNull("Found jack's shadow!", jackAccount);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertOriginWithSideEffectChanges(ObjectDelta<UserType> delta, OriginType expectedOrigin) {
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
            if (modification.getPath().containsNameExactly(InfraItemName.METADATA)) {
                iterator.remove();
            }
        }
        PrismAsserts.assertOrigin(delta, expectedOrigin);
    }
}
