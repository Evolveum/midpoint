/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.DISABLED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.ENABLED;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.test.asserter.RepoShadowAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractDirectManualResourceTest extends AbstractManualResourceTest {

    protected static final File RESOURCE_MANUAL_FILE = new File(TEST_DIR, "resource-manual.xml");
    protected static final String RESOURCE_MANUAL_OID = "0ef80ab8-2906-11e7-b81a-3f343e28c264";

    protected static final File RESOURCE_MANUAL_CAPABILITIES_FILE = new File(TEST_DIR, "resource-manual-capabilities.xml");
    protected static final String RESOURCE_MANUAL_CAPABILITIES_OID = "0ef80ab8-2906-11e7-b81a-3f343e28c264";

    protected static final File RESOURCE_SEMI_MANUAL_FILE = new File(TEST_DIR, "resource-semi-manual.xml");
    protected static final String RESOURCE_SEMI_MANUAL_OID = "aea5a57c-2904-11e7-8020-7b121a9e3595";

    protected static final File RESOURCE_SEMI_MANUAL_DISABLE_FILE = new File(TEST_DIR, "resource-semi-manual-disable.xml");
    protected static final String RESOURCE_SEMI_MANUAL_DISABLE_OID = "5e497cb0-5cdb-11e7-9cfe-4bfe0342d181";

    protected static final File RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_FILE = new File(TEST_DIR, "resource-semi-manual-slow-proposed.xml");
    protected static final String RESOURCE_SEMI_MANUAL_SLOW_PROPOSED_OID = "512d749a-75ff-11e7-8176-8be7fb6f4e45";

    protected static final File RESOURCE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE = new File(TEST_DIR, "resource-semi-manual-disable-slow-proposed.xml");
    protected static final String RESOURCE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID = "8ed29734-a1ed-11e7-b7f9-7bce8b17fd64";

    protected static final File ROLE_ONE_MANUAL_FILE = new File(TEST_DIR, "role-one-manual.xml");
    protected static final String ROLE_ONE_MANUAL_OID = "9149b3ca-5da1-11e7-8e84-130a91fb5876";

    protected static final File ROLE_ONE_SEMI_MANUAL_FILE = new File(TEST_DIR, "role-one-semi-manual.xml");
    protected static final String ROLE_ONE_SEMI_MANUAL_OID = "29eab2c8-5da2-11e7-85d3-c78728e05ca3";

    protected static final File ROLE_ONE_SEMI_MANUAL_DISABLE_FILE = new File(TEST_DIR, "role-one-semi-manual-disable.xml");
    protected static final String ROLE_ONE_SEMI_MANUAL_DISABLE_OID = "3b8cb17a-5da2-11e7-b260-a760972b54fb";

    protected static final File ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-one-semi-manual-slow-proposed.xml");
    protected static final String ROLE_ONE_SEMI_MANUAL_SLOW_PROPOSED_OID = "ca7fefc6-75ff-11e7-9833-572f6bf86a81";

    protected static final File ROLE_ONE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-one-semi-manual-disable-slow-proposed.xml");
    protected static final String ROLE_ONE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID = "38c9fc7a-a200-11e7-8157-2f9beeb541bc";

    protected static final File ROLE_TWO_MANUAL_FILE = new File(TEST_DIR, "role-two-manual.xml");
    protected static final String ROLE_TWO_MANUAL_OID = "414e3766-775e-11e7-b8cb-c7ca37c1dc9e";

    protected static final File ROLE_TWO_SEMI_MANUAL_FILE = new File(TEST_DIR, "role-two-semi-manual.xml");
    protected static final String ROLE_TWO_SEMI_MANUAL_OID = "b95f7252-7776-11e7-bd96-cf05f4c21966";

    protected static final File ROLE_TWO_SEMI_MANUAL_DISABLE_FILE = new File(TEST_DIR, "role-two-semi-manual-disable.xml");
    protected static final String ROLE_TWO_SEMI_MANUAL_DISABLE_OID = "d1eaa4f4-7776-11e7-bb53-eb1218c49dd9";

    protected static final File ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-two-semi-manual-slow-proposed.xml");
    protected static final String ROLE_TWO_SEMI_MANUAL_SLOW_PROPOSED_OID = "eaf3569e-7776-11e7-93f3-3f1b853d6525";

    protected static final File ROLE_TWO_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE = new File(TEST_DIR, "role-two-semi-manual-disable-slow-proposed.xml");
    protected static final String ROLE_TWO_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID = "5ecd6fa6-a200-11e7-b0cb-af5e1792d327";

    private XMLGregorianCalendar roleTwoValidFromTimestamp;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(USER_BARBOSSA_FILE);
        InternalMonitor.setTrace(InternalOperationClasses.REPOSITORY_OPERATIONS, true);
    }

    @Override
    protected boolean isDirect() {
        return true;
    }

    /**
     * disable - do not complete yet (do not wait for delta to expire, we want several deltas at once).
     */
    @Test
    public void test220ModifyUserWillDisable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserReplace(userWillOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        var repoShadow = getShadowRepo(accountWillOid);
        PrismObject<ShadowType> repoShadowObj = repoShadow.getPrismObject();
        display("Repo shadow", repoShadowObj);
        assertPendingOperationDeltas(repoShadowObj, 2);
        PendingOperationType pendingOperation = findPendingOperation(repoShadowObj,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(repoShadowObj, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ENABLED);
        RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 2);
        pendingOperation = findPendingOperation(shadowModel,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowModel, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.DISABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertNotNull("No async reference in result", willLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

        assertSteadyResources();
    }

    /**
     * Change password, enable. There is still pending disable delta. Make sure all the deltas are
     * stored correctly.
     */
    @Test
    public void test230ModifyAccountWillChangePasswordAndEnable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                userWillOid, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ENABLED);
        ProtectedStringType ps = new ProtectedStringType();
        ps.setClearValue(USER_WILL_PASSWORD_NEW);
        delta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, ps);
        displayDumpable("ObjectDelta", delta);

        accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        display("result", result);
        willSecondLastCaseOid = assertInProgress(result);

        accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        var repoShadow = getShadowRepo(accountWillOid);
        PrismObject<ShadowType> repoShadowObj = repoShadow.getPrismObject();
        display("Repo shadow", repoShadowObj);
        assertPendingOperationDeltas(repoShadowObj, 3);
        PendingOperationType pendingOperation = findPendingOperation(repoShadowObj,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(repoShadowObj, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ENABLED);
        RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowProvisioning = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 3);
        pendingOperation = findPendingOperation(shadowProvisioning,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowProvisioning, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        new PrismObjectAsserter<>(shadowProvisioningFuture)
                .assertSanity();

        assertNotNull("No async reference in result", willSecondLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

        assertSteadyResources();
    }

    /**
     * Disable case is closed. The account should be disabled. But there is still the other
     * delta pending.
     * Do NOT explicitly refresh the shadow in this case. Just reading it should cause the refresh.
     */
    @Test
    public void test240CloseDisableCaseAndReadAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        var repoShadow = getShadowRepo(accountWillOid);
        PrismObject<ShadowType> repoShadowObj = repoShadow.getPrismObject();
        display("Repo shadow", repoShadowObj);

        assertPendingOperationDeltas(repoShadowObj, 3);

        PendingOperationType pendingOperation = findPendingOperation(repoShadowObj,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(repoShadowObj, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        pendingOperation = findPendingOperation(repoShadowObj,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(repoShadowObj, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ActivationStatusType.DISABLED);
        RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeModel = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeModel.getKind());
        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadowModel, ENABLED);
        } else {
            assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
        }

        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 3);
        pendingOperation = findPendingOperation(shadowModel,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowModel, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ENABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

        assertSteadyResources();
    }

    /**
     * lets ff 5min just for fun. Refresh, make sure everything should be the same (grace not expired yet)
     */
    @Test
    public void test250RecomputeWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT5M");

        PrismObject<ShadowType> shadowBefore = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        var repoShadow = getShadowRepo(accountWillOid);
        PrismObject<ShadowType> repoShadowObj = repoShadow.getPrismObject();
        display("Repo shadow", repoShadowObj);

        assertPendingOperationDeltas(repoShadowObj, 3);

        PendingOperationType pendingOperation = findPendingOperation(repoShadowObj,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(repoShadowObj, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        pendingOperation = findPendingOperation(repoShadowObj,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(repoShadowObj, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ActivationStatusType.DISABLED);
        RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadowModel, ENABLED);
        } else {
            assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
        }
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 3);
        pendingOperation = findPendingOperation(shadowModel,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowModel, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

        assertSteadyResources();
    }

    @Test
    public void test252UpdateBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        backingStoreUpdateWill(USER_WILL_FULL_NAME_PIRATE, INTEREST_ONE, ActivationStatusType.DISABLED, USER_WILL_PASSWORD_OLD);

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeModel = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeModel.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 3);

        var repoShadow = getShadowRepo(accountWillOid);
        PrismObject<ShadowType> repoShadowObj = repoShadow.getPrismObject();
        display("Repo shadow", repoShadowObj);
        assertPendingOperationDeltas(repoShadowObj, 3);

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ENABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

        assertSteadyResources();
    }

    /**
     * Password change/enable case is closed. The account should be disabled. But there is still the other
     * delta pending.
     */
    @Test
    public void test260ClosePasswordChangeCaseAndRecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willSecondLastCaseOid);

        accountWillSecondCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        refreshShadowIfNeeded(accountWillOid);
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillSecondCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = assertRepoShadow(accountWillOid)
            .pendingOperations()
                .assertOperations(3)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                    .end()
                .end()
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end()
            .getObject();

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ENABLED);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowModel = assertModelShadow(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end()
            .pendingOperations()
                .assertOperations(3)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                .end()
            .end()
            .getObject();

        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
        } else {
            assertShadowActivationAdministrativeStatus(shadowModel, ENABLED);
        }
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        PrismObject<ShadowType> shadowModelFuture = assertModelShadowFuture(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertAdministrativeStatus(ENABLED)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end()
            .pendingOperations()
                .assertOperations(3)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                .end()
            .end()
            .getObject();

        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * ff 7min. Refresh. Oldest pending operation is over grace period, but still
     * in retention period. It should stay in the shadow, but it should not influence
     * future shadow result.
     */
    @Test
    public void test270RecomputeWillAfter7min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT7M");

        PrismObject<ShadowType> shadowBefore = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = assertRepoShadow(accountWillOid)
            .pendingOperations()
                .assertOperations(3)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                    .end()
                .end()
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end()
            .getObject();

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ENABLED);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowModel = assertModelShadow(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end()
            .pendingOperations()
                .assertOperations(3)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                .end()
            .end()
            .getObject();

        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.DISABLED);
        } else {
            assertShadowActivationAdministrativeStatus(shadowModel, ENABLED);
        }
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);


        PrismObject<ShadowType> shadowModelFuture = assertModelShadowFuture(accountWillOid)
                .assertName(USER_WILL_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ENABLED)
                .attributes()
                    .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                    .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                    .end()
                .pendingOperations()
                    .assertOperations(3)
                    .by()
                        .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                        .item(SchemaConstants.PATH_PASSWORD_VALUE)
                    .find()
                        .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                        .assertResultStatus(OperationResultStatusType.SUCCESS)
                        .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                    .end()
                .end()
                .getObject();
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    @Test
    public void test272UpdateBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        backingStoreUpdateWill(USER_WILL_FULL_NAME_PIRATE, INTEREST_ONE, ENABLED, USER_WILL_PASSWORD_NEW);

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        ShadowAsserter.forShadow(shadowModel, "model")
            .display()
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertAdministrativeStatus(ENABLED)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 3);

        assertRepoShadow(accountWillOid)
            .pendingOperations()
                .assertOperations(3);

        PrismObject<ShadowType> shadowModelFuture = assertModelShadowFuture(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertAdministrativeStatus(ENABLED)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end()
            .getObject();
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * ff 15min. Refresh. Oldest pending operation is over retention period. It should be gone.
     */
    @Test
    public void test274RecomputeWillAfter22min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        PrismObject<ShadowType> shadowBefore = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = assertRepoShadow(accountWillOid)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end()
            .pendingOperations()
                .assertOperations(2)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                    .end()
                .end()
            .getObject();
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ENABLED);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowModel = assertModelShadow(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertAdministrativeStatus(ENABLED)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end()
            .pendingOperations()
                .assertOperations(2)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                    .end()
                .end()
            .getObject();

        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ENABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * ff 5min. Refresh. Another delta should expire.
     */
    @Test
    public void test280RecomputeWillAfter27min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT5M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = assertRepoShadow(accountWillOid)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end()
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                    .end()
                .end()
            .getObject();
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowModel = assertModelShadow(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertAdministrativeStatus(ENABLED)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end()
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                    .end()
                .end()
            .getObject();
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        PrismObject<ShadowType> shadowModelFuture = assertModelShadowFuture(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertAdministrativeStatus(ENABLED)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end()
            .getObject();
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * ff 5min. Refresh. All pending operations should expire.
     */
    @Test
    public void test290RecomputeWillAfter32min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT5M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        var repoShadow = getShadowRepo(accountWillOid);
        PrismObject<ShadowType> repoShadowObj = repoShadow.getPrismObject();
        display("Repo shadow", repoShadowObj);

        assertPendingOperationDeltas(repoShadowObj, 0);

        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ENABLED);
        RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 0);

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ENABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    @Test
    public void test300UnassignAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertSteadyResources();

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        unassignRole(userWillOid, getRoleOneOid(), task, result);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertSteadyResources();

        var repoShadow = getShadowRepo(accountWillOid);
        PrismObject<ShadowType> repoShadowObj = repoShadow.getPrismObject();
        display("Repo shadow", repoShadowObj);
        assertPendingOperationDeltas(repoShadowObj, 1);
        PendingOperationType pendingOperation = findPendingOperation(repoShadowObj, OperationResultStatusType.IN_PROGRESS);
        assertPendingOperation(repoShadowObj, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ENABLED);
        RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 1);
        pendingOperation = findPendingOperation(shadowModel, OperationResultStatusType.IN_PROGRESS);
        assertPendingOperation(shadowModel, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        assertWillUnassignedFuture(assertModelShadowFuture(accountWillOid), true);

        assertSteadyResources();

        // Make sure that the account is still linked
        PrismObject<UserType> userAfter = getUser(userWillOid);
        display("User after", userAfter);
        String accountWillOidAfter = getSingleLinkOid(userAfter);
        assertEquals(accountWillOid, accountWillOidAfter);
        assertNoAssignments(userAfter);

        assertNotNull("No async reference in result", willLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

        assertSteadyResources();
    }

    /**
     * Recompute. Make sure model will not try to delete the account again.
     */
    @Test
    public void test302RecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        display("result", result);
        assertSuccess(result);

        var repoShadow = getShadowRepo(accountWillOid);
        PrismObject<ShadowType> repoShadowObj = repoShadow.getPrismObject();
        display("Repo shadow", repoShadowObj);
        assertPendingOperationDeltas(repoShadowObj, 1);
        PendingOperationType pendingOperation = findPendingOperation(repoShadowObj, OperationResultStatusType.IN_PROGRESS);
        assertPendingOperation(repoShadowObj, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ENABLED);
        RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertPendingOperationDeltas(shadowModel, 1);
        pendingOperation = findPendingOperation(shadowModel, OperationResultStatusType.IN_PROGRESS);
        assertPendingOperation(shadowModel, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        assertWillUnassignedFuture(assertModelShadowFuture(accountWillOid), true);

        // Make sure that the account is still linked
        PrismObject<UserType> userAfter = getUser(userWillOid);
        display("User after", userAfter);
        String accountWillOidAfter = getSingleLinkOid(userAfter);
        assertEquals(accountWillOid, accountWillOidAfter);
        assertNoAssignments(userAfter);

        assertNotNull("No async reference in result", willLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

        assertSteadyResources();
    }

    /**
     * Case is closed. The unassign operation is complete.
     * However, in the semi-manual case this gets really interesting.
     * We have Schrodinger's shadow here.  deleted account, ticket closed, account is deleted
     * by administrator in the target system. But the account is still in the backing store (CSV)
     * because scheduled export has not refreshed the file yet.
     */
    @Test
    public void test310CloseCaseAndReconcileWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        refreshShadowIfNeeded(accountWillOid);
        // We need reconcile and not recompute here. We need to fetch the updated case status.
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        display("result", result);
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .end()
                .end();
        assertUnassignedShadow(shadowRepoAsserter, false, isCaching() ? ENABLED : null);

        modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        ShadowAsserter<Void> shadowModelAsserter = assertModelShadow(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .end()
                .end();
        assertUnassignedShadow(shadowModelAsserter, false, ENABLED); // backing store not yet updated
        assertShadowPassword(shadowModelAsserter);

        assertWillUnassignedFuture(assertModelShadowFuture(accountWillOid), true);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    protected void assertUnassignedShadow(ShadowAsserter<?> shadowModelAsserter, boolean backingStoreUpdated, ActivationStatusType expectAlternativeActivationStatus) {
        shadowModelAsserter.assertTombstone();
    }

    /**
     * ff 5min, everything should be the same (grace not expired yet)
     */
    @Test
    public void test320RecomputeWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT5M");

        // WHEN
        when();
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .end()
                .end();
        assertUnassignedShadow(shadowRepoAsserter, false, isCaching() ? ENABLED : null);

        ShadowAsserter<Void> shadowModelAsserter = assertModelShadow(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .end()
                .end();
        assertUnassignedShadow(shadowModelAsserter, false, ENABLED); // backing store not yet updated
        assertShadowPassword(shadowModelAsserter);

        assertWillUnassignedFuture(assertModelShadowFuture(accountWillOid), true);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * For semi-manual case this is the place where the quantum state of Schrodinger's
     * shadow collapses. From now on we should have ordinary tombstone shadow.
     */
    @Test
    public void test330UpdateBackingStoreAndRecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        backingStoreDeprovisionWill();
        displayBackingStore();

        // WHEN
        when();
        // Reconcile is needed here. Recompute means noFetch which means that we won't
        // discover that an account is missing from backing store which means that the
        // quantum state won't collapse.
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .end()
                .end();
        assertUnassignedShadow(shadowRepoAsserter, true, isCaching() ? ENABLED : null);

        ShadowAsserter<Void> shadowModelAsserterNoFetch = assertModelShadowNoFetch(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .end()
                .end();
        assertUnassignedShadow(shadowModelAsserterNoFetch, true, isCaching() ? ENABLED : null);
        // Do NOT assert password here. There is no password even for semi-manual case as the shadow is dead and account gone.

        assertUnassignedShadow(assertModelShadow(accountWillOid), true, ActivationStatusType.DISABLED);
        assertUnassignedShadow(assertModelShadowFuture(accountWillOid), true, ActivationStatusType.DISABLED);
        assertUnassignedShadow(assertModelShadowFutureNoFetch(accountWillOid), true, ActivationStatusType.DISABLED);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    // TODO: nofetch, nofetch+future

    // TODO: Scheroedinger shadow, let the operation go over grace period.

    /**
     * ff 20min, grace period expired, but retention period not expired yet.
     * shadow should stay.
     */
    @Test
    public void test340RecomputeWillAfter25min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT20M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userWillOid)
            .singleLink()
                .assertOid(accountWillOid);

        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
                .pendingOperations()
                    .singleOperation()
                        .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                        .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                        .assertResultStatus(OperationResultStatusType.SUCCESS)
                        .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                        .end()
                    .end();
        assertUnassignedShadow(shadowRepoAsserter, true, isCaching() ? DISABLED : null);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * ff 10min, retention period expired. Operation should be expired.
     * But dead shadow should still remain.
     */
    @Test
    public void test342RecomputeWillAfter35min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT10M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertRepoShadow(accountWillOid);

        assertUserAfter(userWillOid)
            .singleLink()
                .assertOid(accountWillOid);

        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
                .pendingOperations()
                    .assertNone()
                    .end();
        assertUnassignedShadow(shadowRepoAsserter, true, isCaching() ? DISABLED : null);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * ff 130min, dead shadow retention period expired
     * shadow should be gone, linkRef should be gone.
     */
    @Test
    public void test344RecomputeWillAfter165min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT130M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        UserAsserter<Void> userAfterAsserter = assertUserAfter(userWillOid);
        assertDeprovisionedTimedOutUser(userAfterAsserter, accountWillOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    // TODO: create, close case, then update backing store.

    // TODO: let grace period expire without updating the backing store (semi-manual-only)

    protected <R> void assertDeprovisionedTimedOutUser(UserAsserter<R> userAsserter, String accountOid) throws Exception {
        userAsserter
            .assertLiveLinks(0);
        assertNoShadow(accountOid);
    }

    /**
     * Put everything in a clean state so we can start over.
     */
    @Test
    public void test349CleanUp() throws Exception {
        cleanupUser(userWillOid, USER_WILL_NAME, accountWillOid);
    }

    protected void cleanupUser(String userOid, String username, String accountOid) throws Exception {
        // nothing to do here
    }

    /**
     * MID-4037
     */
    @Test
    public void test500AssignWillRoleOne() throws Exception {
        assignWillRoleOne(USER_WILL_FULL_NAME_PIRATE, PendingOperationExecutionStatusType.EXECUTION_PENDING);
    }

    /**
     * Unassign account before anybody had the time to do anything about it.
     * Snapshot (backing store) is never updated.
     * Create ticket is not closed, the account is not yet created and we
     * want to delete it.
     * The shadow should exist, all the operations should be cancelled.
     * MID-4037
     */
    @Test
    public void test510UnassignWillRoleOne() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        unassignRole(userWillOid, getRoleOneOid(), task, result);

        // THEN
        then();
        display("result", result);
        willSecondLastCaseOid = assertInProgress(result);

        accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
            .pendingOperations()
                .assertOperations(2)
                .by()
                    .changeType(ChangeTypeType.ADD)
                .find()
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                    .assertId()
                .end()
            .end()
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
            .end();
        assertWillUnassignPendingOperationExecuting(shadowRepoAsserter);
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter, ENABLED);
        assertAttributeFromCache(shadowRepoAsserter, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        assertModelShadow(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
            .end()
            .pendingOperations()
                .assertOperations(2);

        ShadowAsserter<Void> shadowFutureAsserter = assertModelShadowFuture(accountWillOid)
            .assertName(USER_WILL_NAME);
        assertUnassignedShadow(shadowFutureAsserter, true, ActivationStatusType.DISABLED);

        // Make sure that the account is still linked
        assertUserAfter(userWillOid)
            .singleLink()
                .assertOid(accountWillOid)
                .end()
            .assignments()
                .assertNone();

        assertNotNull("No async reference in result", willSecondLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    /**
     * Just make sure recon does not ruin anything.
     * MID-4037
     */
    @Test
    public void test512ReconcileWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        display("result", result);
        assertSuccess(result);

        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
                .pendingOperations()
                    .assertOperations(2)
                    .by()
                        .changeType(ChangeTypeType.ADD)
                    .find()
                        .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                        .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                        .assertId()
                    .end()
                .end()
                .attributes()
                    .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end();
        assertWillUnassignPendingOperationExecuting(shadowRepoAsserter);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter, ENABLED);
        assertAttributeFromCache(shadowRepoAsserter, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);

        assertModelShadow(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
            .end()
            .pendingOperations()
                .assertOperations(2);

        ShadowAsserter<Void> shadowFutureAsserter = assertModelShadowFuture(accountWillOid)
                .assertName(USER_WILL_NAME);
        assertUnassignedShadow(shadowFutureAsserter, true, ActivationStatusType.DISABLED);

        // Make sure that the account is still linked
        assertUserAfter(userWillOid)
            .singleLink()
                .assertOid(accountWillOid)
                .end()
            .assignments()
                .assertNone();

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    /**
     * Close both cases (assign and unassign) at the same time.
     *
     * This is an interesting case for SemiManualDisable. In that case
     * we would expect that the account was created and that it was disabled.
     * But snapshot (backing store) is never updated as the account was not
     * created at all. But as long as the pending operations are in
     * grace period we have to pretend that the account was created.
     *
     * MID-4037
     */
    @Test
    public void test515CloseCasesAndReconcileWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);
        closeCase(willSecondLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        refreshShadowIfNeeded(accountWillOid);
        // We need reconcile and not recompute here. We need to fetch the updated case status.
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        display("result", result);
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        // For SemiManualDisable case the shadow is not dead yet. We do not know whether
        // the account was created or not. We have to assume that it was created.
        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
            .pendingOperations()
                .assertOperations(2)
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .changeType(ChangeTypeType.ADD)
                .find()
                    .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .assertId()
                    .end()
                .end();
        assertWillUnassignPendingOperationCompleted(shadowRepoAsserter);
        assertUnassignedShadow(shadowRepoAsserter, true, isCaching() ? DISABLED : null);

        ShadowAsserter<Void> shadowModelAsserter = assertModelShadow(accountWillOid)
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT);
        assertUnassignedShadow(shadowModelAsserter, true, isCaching() ? DISABLED : null); // Shadow in not in the backing store

        // For SemiManualDisable case we still pretend that the shadow will exist
        assertWillUnassignedFuture(assertModelShadowFuture(accountWillOid), false);

        assertUserAfter(userWillOid)
            .singleLink()
                .assertOid(accountWillOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    protected void assertWillUnassignPendingOperationExecuting(ShadowAsserter<Void> shadowRepoAsserter) {
        shadowRepoAsserter
            .pendingOperations()
                .by()
                    .changeType(ChangeTypeType.DELETE)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                    .assertId()
                    .end()
                .end();
    }

    protected void assertWillUnassignPendingOperationCompleted(ShadowAsserter<Void> shadowRepoAsserter) {
        shadowRepoAsserter
            .pendingOperations()
                .by()
                    .changeType(ChangeTypeType.DELETE)
                .find()
                    .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .assertId()
                    .end()
                .end();
    }

    /**
     * ff 20min, grace period expired, But we keep pending operation and shadow
     * because they are not expired yet.
     *
     * SemiManualDisable case gets even more interesting here.
     * Snapshot (backing store) was never updated as the account was in fact not
     * created at all. As pending operations are over grace period now, we stop
     * pretending that the account was created. And the reality shows that the
     * account was in fact not created at all. This is the point where we should
     * end up with a dead shadow.
     *
     * MID-4037
     */
    @Test
    public void test516RecomputeWillAfter20min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT20M");

        // WHEN
        when();
        refreshShadowIfNeeded(accountWillOid);
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userWillOid)
            .singleLink()
                .resolveTarget()
                    .assertOid(accountWillOid)
                    .assertDead()
                    .pendingOperations()
                        .assertOperations(2);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 15min, retention period expired, pending operation should be gone.
     * But we still retain dead shadow.
     * MID-4037
     */
    @Test
    public void test517RecomputeWillAfter50min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        refreshShadowIfNeeded(accountWillOid);
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertRepoShadow(accountWillOid);

        assertUserAfter(userWillOid)
        .singleLink()
            .resolveTarget()
                .assertOid(accountWillOid)
                .assertDead()
                .pendingOperations()
                    .assertNone();

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 130min, shadow retention period expired, shadow should be gone, linkRef should be gone.
     * So we have clean state for next tests.
     * MID-4037
     */
    @Test
    public void test518RecomputeWillAfter180min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT130M");

        // WHEN
        when();
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userWillOid)
            .assertLiveLinks(0);
        // Shadow will not stay even in the "disable" case.
        // It was never created in the backing store

        assertNoShadow(accountWillOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * Put everything in a clean state so we can start over.
     */
    @Test
    public void test519CleanUp() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        cleanupUser(userWillOid, USER_WILL_NAME, accountWillOid);

        // Make sure that all pending operations are expired
        clockForward("PT1H");
        recomputeUser(userWillOid, task, result);
        assertSuccess(result);

        assertNoShadow(accountWillOid);
    }

    /**
     * MID-4095
     */
    @Test
    public void test520AssignWillRoleOne() throws Exception {
        assignWillRoleOne(USER_WILL_FULL_NAME_PIRATE, PendingOperationExecutionStatusType.EXECUTION_PENDING);
    }


    /**
     * Not much happens here yet. Role two is assigned. But it has validity in the future.
     * Therefore there are no changes in the account, no new pending deltas.
     * MID-4095
     */
    @Test
    public void test522AssignWillRoleTwoValidFrom() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        roleTwoValidFromTimestamp = getTimestamp("PT2H");
        activationType.setValidFrom(roleTwoValidFromTimestamp);

        // WHEN
        when();
        assignRole(userWillOid, getRoleTwoOid(), activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillOid = assertUserAfter(userWillOid)
            .singleLink()
                .getOid();

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertAccountWillAfterAssign(USER_WILL_FULL_NAME_PIRATE, PendingOperationExecutionStatusType.EXECUTION_PENDING);
    }


    /**
     * Two hours forward. Role two is valid now.
     * MID-4095
     */
    @Test
    public void test524TwoHoursForRoleTwo() throws Exception {
        // GIVEN

        clockForward("PT2H5M");

        Task task = getTestTask();
        OperationResult result = task.getResult();

        accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        display("result", result);
        willSecondLastCaseOid = assertInProgress(result);

        accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertUserAfter(userWillOid)
        .singleLink()
            .assertOid(accountWillOid);

        ShadowAsserter<Void> repoShadowAsserter = assertRepoShadow(accountWillOid)
            .assertConception()
            .pendingOperations()
                .assertOperations(2)
                .by()
                    .changeType(ChangeTypeType.ADD)
                .find()
                    .delta()
                        .display()
                    .end()
                .end()
                .by()
                    .changeType(ChangeTypeType.MODIFY)
                .find()
                    .assertAsynchronousOperationReference(willSecondLastCaseOid)
                .end()
            .end()
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
            .end()
            //.assertNoPasswordIf(!isCaching());
            .assertNoPassword(); // change when resolving MID-10050
        assertAttributeFromCache(repoShadowAsserter, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertShadowActivationAdministrativeStatusFromCache(repoShadowAsserter, ENABLED);

        assertModelShadowFuture(accountWillOid)
            .assertLive()
            .assertAdministrativeStatus(ENABLED)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .end();

        assertNotNull("No async reference in result", willSecondLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    /**
     * MID-4095
     *
     * This test tries to ADD value of "one" to attributes/interests, creating a new pending operation.
     * TODO
     */
    @Test
    public void test525CloseCasesAndReconcileWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);
        closeCase(willSecondLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        refreshShadowIfNeeded(accountWillOid);
        // We need reconcile and not recompute here. We need to fetch the updated case status.
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        display("result", result);
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);

        assertPendingOperationDeltas(shadowRepo, 2);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * Unassign both roles at the same time.
     * Note: In semi-manual cases the backing store is never updated.
     */
    @Test
    public void test526UnassignWillBothRoles() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(userWillOid);

        accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        unassignAll(userBefore, task, result);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);

        assertTest526Deltas(shadowRepo, result);

        assertNotNull("No async reference in result", willLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    protected void assertTest526Deltas(PrismObject<ShadowType> shadowRepo, OperationResult result) {
        assertPendingOperationDeltas(shadowRepo, 3);

        ObjectDeltaType deltaModify = null;
        ObjectDeltaType deltaAdd = null;
        ObjectDeltaType deltaDelete = null;
        for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
            ObjectDeltaType delta = pendingOperation.getDelta();
            if (ChangeTypeType.ADD.equals(delta.getChangeType())) {
                deltaAdd = delta;
                assertEquals("Wrong status in add delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
            }
            if (ChangeTypeType.MODIFY.equals(delta.getChangeType())) {
                deltaModify = delta;
                assertEquals("Wrong status in modify delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
            }
            if (ChangeTypeType.DELETE.equals(delta.getChangeType())) {
                deltaDelete = delta;
                assertEquals("Wrong status in delete delta", OperationResultStatusType.IN_PROGRESS, pendingOperation.getResultStatus());
            }
        }
        assertNotNull("No add pending delta", deltaAdd);
        assertNotNull("No modify pending delta", deltaModify);
        assertNotNull("No delete pending delta", deltaDelete);
    }

    @Test
    public void test528CloseCaseAndRecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        refreshShadowIfNeeded(accountWillOid);
        // We need reconcile and not recompute here. We need to fetch the updated case status.
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        display("result", result);
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);

        assertTest528Deltas(shadowRepo, result);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    protected void assertTest528Deltas(PrismObject<ShadowType> shadowRepo, OperationResult result) {
        assertPendingOperationDeltas(shadowRepo, 3);

        ObjectDeltaType deltaModify = null;
        ObjectDeltaType deltaAdd = null;
        ObjectDeltaType deltaDelete = null;
        for (PendingOperationType pendingOperation: shadowRepo.asObjectable().getPendingOperation()) {
            assertEquals("Wrong status in pending delta", OperationResultStatusType.SUCCESS, pendingOperation.getResultStatus());
            ObjectDeltaType delta = pendingOperation.getDelta();
            if (ChangeTypeType.ADD.equals(delta.getChangeType())) {
                deltaAdd = delta;
            }
            if (ChangeTypeType.MODIFY.equals(delta.getChangeType())) {
                deltaModify = delta;
            }
            if (ChangeTypeType.DELETE.equals(delta.getChangeType())) {
                deltaDelete = delta;
            }
        }
        assertNotNull("No add pending delta", deltaAdd);
        assertNotNull("No modify pending delta", deltaModify);
        assertNotNull("No delete pending delta", deltaDelete);

    }

    /**
     * Put everything in a clean state so we can start over.
     */
    @Test
    public void test529CleanUp() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        cleanupUser(userWillOid, USER_WILL_NAME, accountWillOid);

        // Make sure that all pending operations are expired
        clockForward("PT3H");

        // during this operation (more specifically get shadow "will") shadow is refreshed, pending operations deleted and
        // modifyTimestamp updated, meaning deadRetentionPeriod will not be exceeded
        recomputeUser(userWillOid, task, result);

        // this will remove shadow during shadow refresh, since it's dead and modifications didn't happend
        clockForward("PT3H");
        recomputeUser(userWillOid, task, result);

        assertSuccess(result);

        assertNoShadow(accountWillOid);
    }

    // Tests 7xx are in the subclasses

    /**
     * The 8xx tests is similar routine as 1xx,2xx,3xx, but this time
     * with automatic updates using refresh task.
     */
    @Test
    public void test800ImportShadowRefreshTask() throws Exception {
        // GIVEN

        // WHEN
        when();
        addTask(TASK_SHADOW_REFRESH_FILE);

        // THEN
        then();

        waitForTaskStart(TASK_SHADOW_REFRESH_OID);
    }

    @Test
    public void test810AssignAccountWill() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertUserBefore(userWillOid)
            .assertLiveLinks(0);
        assertNoShadow(accountWillOid);

        modifyUserReplace(userWillOid, UserType.F_FULL_NAME, task, result, PolyString.fromOrig(USER_WILL_FULL_NAME));

        // WHEN
        assignWillRoleOne(USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);

        // THEN
        restartTask(TASK_SHADOW_REFRESH_OID);
        waitForTaskFinish(TASK_SHADOW_REFRESH_OID);

        assertAccountWillAfterAssign(USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);
    }

    @Test
    public void test820AssignAccountJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT5M");

        accountJackReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, getResourceOid(), null, task, result);

        // THEN
        then();
        display("result", result);
        jackLastCaseOid = assertInProgress(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        accountJackOid = getSingleLinkOid(userAfter);

        accountJackReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertAccountJackAfterAssign();

        // THEN
        restartTask(TASK_SHADOW_REFRESH_OID);
        waitForTaskFinish(TASK_SHADOW_REFRESH_OID);

        assertAccountJackAfterAssign();

        assertAccountWillAfterAssign(USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);
    }

    @Test
    public void test830CloseCaseWillAndWaitForRefresh() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        restartTask(TASK_SHADOW_REFRESH_OID);
        waitForTaskFinish(TASK_SHADOW_REFRESH_OID);

        // THEN
        then();
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertWillAfterCreateCaseClosed(false);

        assertAccountJackAfterAssign();
    }

    @Test
    public void test840AddToBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        backingStoreProvisionWill(INTEREST_ONE);

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        ShadowAsserter.forShadow(shadowModel, "model")
            .assertName(USER_WILL_NAME)
            .assertKind(ShadowKindType.ACCOUNT)
            .assertNotDead()
            .assertIsExists()
            .assertAdministrativeStatus(ENABLED)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME)
            .end();
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        PrismObject<ShadowType> shadowRepo = assertRepoShadow(accountWillOid)
            .assertNotDead()
            .assertIsExists()
            .pendingOperations()
                .singleOperation()
                    .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .end()
                .end()
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertNoSimpleAttributeIfNotCached(ATTR_DESCRIPTION_QNAME)
            .end()
            //.assertNoPasswordIf(!isCaching());
            .assertNoPassword() // change when resolving MID-10050
            .getObject();
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ENABLED);
    }

    // Direct execution. The operation is always executing immediately after it is requested.
    protected PendingOperationExecutionStatusType getExpectedExecutionStatus(PendingOperationExecutionStatusType executionStage) {
        return PendingOperationExecutionStatusType.EXECUTING;
    }

    @Override
    protected OperationResultStatusType getExpectedResultStatus(PendingOperationExecutionStatusType executionStage) {
        return OperationResultStatusType.IN_PROGRESS;
    }
}
