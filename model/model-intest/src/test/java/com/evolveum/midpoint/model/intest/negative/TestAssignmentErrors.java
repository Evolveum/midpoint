/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.negative;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_PASSWORD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertNoRepoThreadLocalCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Tests the model service contract by using a broken CSV resource. Tests for negative test cases, mostly
 * correct handling of connector exceptions.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentErrors extends AbstractInitializedModelIntegrationTest {

    private static final String USER_LEMONHEAD_NAME = "lemonhead";
    private static final String USER_LEMONHEAD_FULLNAME = "Lemonhead";
    private static final String USER_SHARPTOOTH_NAME = "sharptooth";
    private static final String USER_SHARPTOOTH_FULLNAME = "Sharptooth";
    private static final String USER_SHARPTOOTH_PASSWORD_1_CLEAR = "SHARPyourT33TH";
    private static final String USER_SHARPTOOTH_PASSWORD_2_CLEAR = "L00SEyourT33TH";
    private static final String USER_SHARPTOOTH_PASSWORD_3_CLEAR = "HAV3noT33TH";

    private static final String USER_AFET_NAME = "afet";
    private static final String USER_AFET_FULLNAME = "Alfredo Fettucini";
    private static final String USER_BFET_NAME = "bfet";
    private static final String USER_BFET_FULLNAME = "Bill Fettucini";
    private static final String USER_CFET_NAME = "cfet";
    private static final String USER_CFET_FULLNAME = "Carlos Fettucini";

    private String userSharptoothOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test010RefinedSchemaWhite() throws Exception {
        // GIVEN

        // WHEN
        PrismObject<ResourceType> resourceWhite = getObject(ResourceType.class, RESOURCE_DUMMY_WHITE_OID);
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resourceWhite);
        displayDumpable("Refined schema", refinedSchema);

        ResourceObjectDefinition accountDef = refinedSchema.findDefaultDefinitionForKind(ShadowKindType.ACCOUNT);
        // This is an object class definition, as this resource has no schema handling defined
        assertNotNull("Account definition is missing", accountDef);
        assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
        assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
        assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
        assertFalse("No nativeObjectClass in account",
                StringUtils.isEmpty(accountDef.getObjectClassDefinition().getNativeObjectClassName()));

        assertFalse("Account definition is deprecated", accountDef.isDeprecated());
        assertFalse("Account definition in auxiliary", accountDef.getObjectClassDefinition().isAuxiliary());

        ShadowSimpleAttributeDefinition<?> uidDef = accountDef.findSimpleAttributeDefinition(SchemaConstants.ICFS_UID);
        assertNotNull(uidDef);
        assertEquals(1, uidDef.getMaxOccurs());
        assertEquals(0, uidDef.getMinOccurs());
        assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
        assertFalse("UID has create", uidDef.canAdd());
        assertFalse("UID has update", uidDef.canModify());
        assertTrue("No UID read", uidDef.canRead());
        assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(uidDef));

        ShadowSimpleAttributeDefinition<?> nameDef = accountDef.findSimpleAttributeDefinition(SchemaConstants.ICFS_NAME);
        assertNotNull(nameDef);
        assertEquals(1, nameDef.getMaxOccurs());
        assertEquals(1, nameDef.getMinOccurs());
        assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
        assertTrue("No NAME create", nameDef.canAdd());
        assertTrue("No NAME update", nameDef.canModify());
        assertTrue("No NAME read", nameDef.canRead());
        assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

        ShadowSimpleAttributeDefinition<?> fullnameDef = accountDef.findSimpleAttributeDefinition("fullname");
        assertNotNull("No definition for fullname", fullnameDef);
        assertEquals(1, fullnameDef.getMaxOccurs());
        assertEquals(1, fullnameDef.getMinOccurs());
        assertTrue("No fullname create", fullnameDef.canAdd());
        assertTrue("No fullname update", fullnameDef.canModify());
        assertTrue("No fullname read", fullnameDef.canRead());

        assertNull("The _PASSWORD_ attribute sneaked into schema",
                accountDef.findSimpleAttributeDefinition(ICFS_PASSWORD));
    }

    /**
     * The "white" resource has no outbound mapping and there is also no mapping in the assignment. Therefore
     * this results in account without any attributes. It should fail.
     */
    @Test
    public void test100UserJackAssignBlankAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();

        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_WHITE_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        dummyAuditService.clear();

        // WHEN
        //not expected that it fails, insted the fatal error in the result is excpected
        modelService.executeChanges(deltas, null, task, result);

        result.computeStatus();

        display(result);
        // This has to be a partial error as some changes were executed (user) and others were not (account)
        TestUtil.assertPartialError(result);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.PARTIAL_ERROR);
        dummyAuditService.assertExecutionMessage();

    }

    /**
     * The "while" resource has no outbound mapping and there is also no mapping in the assignment. Therefore
     * this results in account without any attributes. It should fail.
     */
    @Test
    public void test101AddUserCharlesAssignBlankAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        PrismObject<UserType> userCharles = createUser("charles", "Charles L. Charles");
        fillinUserAssignmentAccountConstruction(userCharles, RESOURCE_DUMMY_WHITE_OID);

        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(userCharles);

        // WHEN
        when();
        //we do not expect this to throw an exception. instead the fatal error in the result is excpected
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges = executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertFailure(result);

        // Even though the operation failed the addition of a user should be successful. Let's check if user was really added.
        String userOid = ObjectDeltaOperation.findFocusDeltaOidInCollection(executedChanges);
        assertNotNull("No user OID in delta after operation", userOid);

        PrismObject<UserType> userAfter = getUser(userOid);
        assertUser(userAfter, userOid, "charles", "Charles L. Charles", null, null, null);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.PARTIAL_ERROR);
        dummyAuditService.assertExecutionMessage();

    }

    @Test
    public void test200UserLemonheadAssignAccountBrokenNetwork() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> user = createUser(USER_LEMONHEAD_NAME, USER_LEMONHEAD_FULLNAME);
        addObject(user);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(user.getOid(), RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        getDummyResource().setBreakMode(BreakMode.NETWORK);
        dummyAuditService.clear();

        // WHEN
        when();
        // not expected that it fails, instead the error in the result is expected
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        display(result);
        // account cannot be updated due to a network error. The operation was postponed, therefore
        // it is "in progress".
        assertInProgress(result);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertTarget(user.getOid());
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.IN_PROGRESS);

    }

    // PARTIAL_ERROR: Unable to get object from the resource. Probably it has not been created yet because of previous unavailability of the resource.
    // TODO: timeout or explicit retry
//    @Test
//    public void test205UserLemonheadRecovery() throws Exception {
//        // GIVEN
//        Task task = getTestTask();
//        OperationResult result = task.getResult();
//        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
//
//        dummyResource.setBreakMode(BreakMode.NONE);
//        dummyAuditService.clear();
//
//        // WHEN
//        //not expected that it fails, instead the error in the result is expected
//        modelService.recompute(UserType.class, userLemonheadOid, task, result);
//
//        result.computeStatus();
//
//        display(result);
//        // This has to be a partial error as some changes were executed (user) and others were not (account)
//        TestUtil.assertSuccess(result);
//
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertSimpleRecordSanity();
//        dummyAuditService.assertRecords(2);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertTarget(userLemonheadOid);
//        dummyAuditService.assertExecutionOutcome(OperationResultStatus.HANDLED_ERROR);
//        dummyAuditService.assertExecutionMessage();
//
//    }

    @Test
    public void test210UserSharptoothAssignAccountBrokenGeneric() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> user = createUser(USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME);
        CredentialsType credentialsType = new CredentialsType();
        PasswordType passwordType = new PasswordType();
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(USER_SHARPTOOTH_PASSWORD_1_CLEAR);
        passwordType.setValue(passwordPs);
        credentialsType.setPassword(passwordType);
        user.asObjectable().setCredentials(credentialsType);
        addObject(user);
        userSharptoothOid = user.getOid();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(user.getOid(), RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        getDummyResource().setBreakMode(BreakMode.GENERIC);
        dummyAuditService.clear();

        try {
            // WHEN
            when();
            //not expected that it fails, instead the error in the result is expected
            modelService.executeChanges(deltas, null, task, result);

            assertNotReached();
        } catch (GenericConnectorException e) {
            then();
            displayExpectedException(e);
        }
        assertFailure(result);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class, OperationResultStatus.FATAL_ERROR);
        dummyAuditService.assertTarget(user.getOid());
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.FATAL_ERROR);
        dummyAuditService.assertExecutionMessage();

        //noinspection unchecked
        LensContext<UserType> lastLensContext = (LensContext<UserType>) profilingModelInspectorManager.getLastLensContext();
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = lastLensContext.getExecutedDeltas();
        display("Executed deltas", executedDeltas);
        assertEquals("Unexpected number of execution deltas in context", 2, executedDeltas.size());
        Iterator<ObjectDeltaOperation<? extends ObjectType>> i = executedDeltas.iterator();
        ObjectDeltaOperation<? extends ObjectType> deltaop1 = i.next();
        assertNotNull(deltaop1.getExecutionResult());
        assertEquals("Unexpected result of first executed deltas", OperationResultStatus.SUCCESS, deltaop1.getExecutionResult().getStatus());
        ObjectDeltaOperation<? extends ObjectType> deltaop2 = i.next();
        assertNotNull(deltaop2.getExecutionResult());
        assertEquals("Unexpected result of second executed deltas", OperationResultStatus.FATAL_ERROR, deltaop2.getExecutionResult().getStatus());

    }

    /**
     * User has assigned account. We recover the resource (clear break mode) and recompute.
     * The account should be created.
     */
    @Test
    public void test212UserSharptoothAssignAccountRecovery() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        getDummyResource().resetBreakMode();
        dummyAuditService.clear();

        // WHEN
        when();
        recomputeUser(userSharptoothOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertDummyAccount(null, USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME, true);
        assertDummyPassword(null, USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_PASSWORD_1_CLEAR);
    }

    /**
     * Change user password. But there is error on the resource.
     * User password should be changed, account password unchanged and there
     * should be handled error in the result. Delta is remembered in the shadow.
     * MID-3569
     */
    @Test
    public void test214UserSharptoothChangePasswordNetworkError() throws Exception {
        testUserSharptoothChangePasswordError(
                BreakMode.NETWORK, USER_SHARPTOOTH_PASSWORD_1_CLEAR, USER_SHARPTOOTH_PASSWORD_2_CLEAR,
                OperationResultStatus.IN_PROGRESS);
    }

    /**
     * Change user password. But there is generic error on the resource.
     * <p>
     * Default dummy resource has no error criticality definition. Therefore generic errors
     * are considered critical. This operation is supposed to die.
     * <p>
     * MID-3569
     */
    @Test
    public void test215UserSharptoothChangePasswordGenericError() throws Exception {
        try {
            testUserSharptoothChangePasswordError(
                    BreakMode.GENERIC, USER_SHARPTOOTH_PASSWORD_1_CLEAR, USER_SHARPTOOTH_PASSWORD_3_CLEAR,
                    InternalsConfig.isShadowCachingFullByDefault() ?
                            OperationResultStatus.IN_PROGRESS : // when caching is on, there is no ConnId read op to fail
                            OperationResultStatus.FATAL_ERROR);

            if (!InternalsConfig.isShadowCachingFullByDefault()) {
                assertNotReached();
            }
        } catch (GenericConnectorException e) {
            // expected
        }
    }

    // This test doesn't work on default dummy
    // TODO: apply it to black dummy
//    /**
//     * Change user password. But there is generic error on the resource.
//     * User password should be changed, account password unchanged and there
//     * should be partial error in the result. We have no idea what's going on here.
//     * Default dummy
//     * MID-3569
//     */
//    @Test
//    public void test216UserSharptoothChangePasswordGenericError() throws Exception {
//        testUserSharptoothChangePasswordError("test216UserSharptoothChangePasswordGenericError",
//                BreakMode.GENERIC, USER_SHARPTOOTH_PASSWORD_1_CLEAR, USER_SHARPTOOTH_PASSWORD_3_CLEAR,
//                OperationResultStatus.PARTIAL_ERROR);
//    }

    public void testUserSharptoothChangePasswordError(BreakMode breakMode,
            String oldPassword, String newPassword, OperationResultStatus expectedResultStatus)
            throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        getDummyResource().setBreakMode(breakMode);
        dummyAuditService.clear();

        // WHEN
        when();
        modifyUserChangePassword(userSharptoothOid, newPassword, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertStatus(result, expectedResultStatus);

        getDummyResource().resetBreakMode();

        PrismObject<UserType> userAfter = getUser(userSharptoothOid);
        display("User after", userAfter);
        assertEncryptedUserPassword(userAfter, newPassword);

        assertDummyAccount(null, USER_SHARPTOOTH_NAME, USER_SHARPTOOTH_FULLNAME, true);
        assertDummyPassword(null, USER_SHARPTOOTH_NAME, oldPassword);
    }

    /**
     * Assign account to user, delete the account shadow (not the account), recompute the user.
     * We expect that the shadow will be re-created and re-linked.
     * <p>
     * This is tried on the default dummy resource where synchronization is enabled.
     */
    @Test
    public void test220UserAssignAccountDeletedShadowRecomputeSync() throws Exception {
        //GIVEN
        PrismObject<UserType> user = setupUserAssignAccountDeletedShadowRecompute(RESOURCE_DUMMY_OID, null,
                USER_AFET_NAME, USER_AFET_FULLNAME);
        String shadowOidBefore = getSingleLinkOid(user);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        recomputeUser(user.getOid(), task, result);

        // THEN
        result.computeStatus();
        display("Recompute result", result);
        TestUtil.assertSuccess(result, 2);

        user = getUser(user.getOid());
        display("User after", user);
        String shadowOidAfter = getSingleLinkOid(user);
        displayValue("Shadow OID after", shadowOidAfter);
        PrismObject<ShadowType> shadowAfter = repositoryService.getObject(ShadowType.class, shadowOidAfter, null, result);
        display("Shadow after", shadowAfter);

        assertThat(shadowOidAfter)
                .as("shadowOidAfter")
                .isNotEqualTo(shadowOidBefore);

        // ... and again ...

        task = getTestTask();
        result = task.getResult();

        // WHEN
        recomputeUser(user.getOid(), task, result);

        // THEN
        result.computeStatus();
        display("Recompute result", result);
        TestUtil.assertSuccess(result, 2);

        user = getUser(user.getOid());
        display("User after", user);
        String shadowOidAfterAfter = getSingleLinkOid(user);
        displayValue("Shadow OID after the second time", shadowOidAfterAfter);

        assertEquals("The shadow OIDs has changed after second recompute", shadowOidAfter, shadowOidAfterAfter);
    }

    /**
     * Assign account to user, delete the account shadow (not the account), recompute the user.
     * We expect an error.
     * <p>
     * This is tried on the red dummy resource where there is no synchronization.
     */
    //TODO: after fixing uniqueness check in ProjectionValueProcessor, change the test little bit
    //see git commit: 7052f9628a76815d27a119090a97ec57fbdebaec
    @Test
    public void test222UserAssignAccountDeletedShadowRecomputeNoSync() throws Exception {
        // GIVEN
        PrismObject<UserType> user = setupUserAssignAccountDeletedShadowRecompute(
                RESOURCE_DUMMY_RED_OID, RESOURCE_DUMMY_RED_NAME, USER_BFET_NAME, USER_BFET_FULLNAME);
        Task task = createPlainTask();
        OperationResult result = task.getResult();

        try {
            // WHEN
            recomputeUser(user.getOid(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (ObjectAlreadyExistsException e) {
            // this is expected
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        user = getUser(user.getOid());
        display("User after", user);
        assertNoLinkedAccount(user);

        // and again ...

        task = createPlainTask();
        result = task.getResult();

        try {
            // WHEN
            recomputeUser(user.getOid(), task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (ObjectAlreadyExistsException e) {
            // this is expected
            result.computeStatus();
            TestUtil.assertFailure(result);
        }

        user = getUser(user.getOid());
        display("User after", user);
        assertNoLinkedAccount(user);
    }

    /**
     * Assign account to user, delete the account shadow (not the account), recompute the user.
     * We expect that the shadow will be re-created and re-linked.
     * <p>
     * This is tried on the yellow dummy resource where there is reduced synchronization config.
     */
    @Test
    public void test224UserAssignAccountDeletedShadowRecomputeReducedSync() throws Exception {
        //GIVEN
        PrismObject<UserType> user = setupUserAssignAccountDeletedShadowRecompute(
                RESOURCE_DUMMY_YELLOW_OID, RESOURCE_DUMMY_YELLOW_NAME,
                USER_CFET_NAME, USER_CFET_FULLNAME);
        String shadowOidBefore = getSingleLinkOid(user);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        recomputeUser(user.getOid(), task, result);

        // THEN
        result.computeStatus();
        display("Recompute result", result);
        TestUtil.assertSuccess(result, 2);

        user = getUser(user.getOid());
        display("User after", user);
        String shadowOidAfter = getSingleLinkOid(user);
        displayValue("Shadow OID after", shadowOidAfter);
        PrismObject<ShadowType> shadowAfter = repositoryService.getObject(ShadowType.class, shadowOidAfter, null, result);
        display("Shadow after", shadowAfter);

        assertThat(shadowOidAfter)
                .as("shadowOidAfter")
                .isNotEqualTo(shadowOidBefore);

        // ... and again ...

        task = getTestTask();
        result = task.getResult();

        // WHEN
        recomputeUser(user.getOid(), task, result);

        // THEN
        result.computeStatus();
        display("Recompute result", result);
        TestUtil.assertSuccess(result, 2);

        user = getUser(user.getOid());
        display("User after", user);
        String shadowOidAfterAfter = getSingleLinkOid(user);
        displayValue("Shadow OID after the second time", shadowOidAfterAfter);

        assertEquals("The shadow OIDs has changed after second recompute", shadowOidAfter, shadowOidAfterAfter);

    }

    private PrismObject<UserType> setupUserAssignAccountDeletedShadowRecompute(
            String dummyResourceOid, String dummyResourceName, String userName, String userFullName)
            throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        getDummyResource().resetBreakMode();

        PrismObject<UserType> user = createUser(userName, userFullName);
        AssignmentType assignmentType = createConstructionAssignment(dummyResourceOid, ShadowKindType.ACCOUNT, null);
        user.asObjectable().getAssignment().add(assignmentType);
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
        user.asObjectable().setActivation(activationType);
        setPassword(user, "blablabla");
        addObject(user);

        // precondition
        assertDummyAccount(dummyResourceName, userName, userFullName, true);

        // Re-read user to get the links
        user = getUser(user.getOid());
        display("User before", user);
        String shadowOidBefore = getSingleLinkOid(user);

        // precondition
        PrismObject<ShadowType> shadowBefore = repositoryService.getObject(ShadowType.class, shadowOidBefore, null, result);
        display("Shadow before", shadowBefore);

        // delete just the shadow, not the account
        repositoryService.deleteObject(ShadowType.class, shadowOidBefore, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoRepoThreadLocalCache();
        dummyAuditService.clear();

        return user;
    }

}
