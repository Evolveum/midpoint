/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_ENABLE_TIMESTAMP;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.cast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.intest.sync.TestValidityRecomputeTask;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 * @see TestValidityRecomputeTask
 */
@SuppressWarnings({ "FieldCanBeLocal", "unused", "SameParameterValue", "SimplifiedTestNGAssertion" })
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestActivation extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/activation");

    // This resource does not support native activation. It has simulated activation instead.
    // + unusual validTo and validFrom mappings
    private static final File RESOURCE_DUMMY_KHAKI_FILE = new File(TEST_DIR, "resource-dummy-khaki.xml");
    private static final String RESOURCE_DUMMY_KHAKI_OID = "10000000-0000-0000-0000-0000000a1004";
    private static final String RESOURCE_DUMMY_KHAKI_NAME = "khaki";

    // This resource does not support native activation. It has simulated activation instead.
    // + unusual validTo and validFrom mappings
    private static final File RESOURCE_DUMMY_CORAL_FILE = new File(TEST_DIR, "resource-dummy-coral.xml");
    private static final String RESOURCE_DUMMY_CORAL_OID = "10000000-0000-0000-0000-0000000b1004";
    private static final String RESOURCE_DUMMY_CORAL_NAME = "coral";

    private static final TestResource RESOURCE_DUMMY_PRECREATE = new TestResource(TEST_DIR, "resource-dummy-precreate.xml", "f18711a2-5db5-4562-b50d-3ef4c74f2e1d");
    private static final String RESOURCE_DUMMY_PRECREATE_NAME = "precreate";

    private static final String ACCOUNT_MANCOMB_DUMMY_USERNAME = "mancomb";
    private static final Date ACCOUNT_MANCOMB_VALID_FROM_DATE = MiscUtil.asDate(2011, 2, 3, 4, 5, 6);
    private static final Date ACCOUNT_MANCOMB_VALID_TO_DATE = MiscUtil.asDate(2066, 5, 4, 3, 2, 1);
    private static final String SUSPENDED_ATTRIBUTE_NAME = "suspended";

    private String accountOid;
    private String accountRedOid;
    private String accountYellowOid;
    private XMLGregorianCalendar lastValidityChangeTimestamp;
    private String userMancombOid;
    private XMLGregorianCalendar manana;

    private DummyResource dummyResourceKhaki;
    private DummyResourceContoller dummyResourceCtlKhaki;
    private ResourceType resourceDummyKhakiType;
    private PrismObject<ResourceType> resourceDummyKhaki;

    private DummyResource dummyResourceCoral;
    private DummyResourceContoller dummyResourceCtlCoral;
    private ResourceType resourceDummyCoralType;
    private PrismObject<ResourceType> resourceDummyCoral;

    private DummyResource dummyResourcePrecreate;
    private DummyResourceContoller dummyResourceCtlPrecreate;
    private ResourceType resourceDummyPrecreateType;
    private PrismObject<ResourceType> resourceDummyPrecreate;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceCtlKhaki = DummyResourceContoller.create(RESOURCE_DUMMY_KHAKI_NAME, resourceDummyKhaki);
        dummyResourceCtlKhaki.extendSchemaPirate();
        dummyResourceKhaki = dummyResourceCtlKhaki.getDummyResource();
        resourceDummyKhaki = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_KHAKI_FILE, RESOURCE_DUMMY_KHAKI_OID, initTask, initResult);
        resourceDummyKhakiType = resourceDummyKhaki.asObjectable();
        dummyResourceCtlKhaki.setResource(resourceDummyKhaki);

        dummyResourceCtlCoral = DummyResourceContoller.create(RESOURCE_DUMMY_CORAL_NAME, resourceDummyCoral);
        DummyObjectClass accountObjectClass = dummyResourceCtlCoral.getDummyResource().getAccountObjectClass();
        dummyResourceCtlCoral.addAttrDef(accountObjectClass, SUSPENDED_ATTRIBUTE_NAME, Boolean.class, false, false);
        dummyResourceCoral = dummyResourceCtlCoral.getDummyResource();
        resourceDummyCoral = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_CORAL_FILE, RESOURCE_DUMMY_CORAL_OID, initTask, initResult);
        resourceDummyCoralType = resourceDummyCoral.asObjectable();
        dummyResourceCtlCoral.setResource(resourceDummyCoral);

        dummyResourceCtlPrecreate = DummyResourceContoller.create(RESOURCE_DUMMY_PRECREATE_NAME, resourceDummyPrecreate);
        dummyResourcePrecreate = dummyResourceCtlPrecreate.getDummyResource();
        resourceDummyPrecreate = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_PRECREATE.file, RESOURCE_DUMMY_PRECREATE.oid, initTask, initResult);
        resourceDummyPrecreateType = resourceDummyPrecreate.asObjectable();
        dummyResourceCtlPrecreate.setResource(resourceDummyPrecreate);
    }

    @Test
    public void test050CheckJackEnabled() throws Exception {
        // GIVEN, WHEN
        // this happens during test initialization when user-jack.xml is added

        // THEN
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        // Cannot assert validity or effective status here. The user was added through repo and was not recomputed yet.
    }

    @Test
    public void test051ModifyUserJackDisable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusDisabled(userJack);
        assertValidity(userJack, null);
        assertEffectiveStatus(userJack, ActivationStatusType.DISABLED);
        assertDisableTimestampFocus(userJack, start, end);

        TestUtil.assertModifyTimestamp(userJack, start, end);
    }

    @Test
    public void test052ModifyUserJackNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatus(userJack, null);
        assertValidity(userJack, null);
        assertEffectiveStatus(userJack, ActivationStatusType.ENABLED);

        TestUtil.assertModifyTimestamp(userJack, start, end);
    }

    @Test
    public void test055ModifyUserJackEnable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        assertValidity(userJack, null);
        assertEffectiveStatus(userJack, ActivationStatusType.ENABLED);
        assertEnableTimestampFocus(userJack, null, start);

        TestUtil.assertModifyTimestamp(userJack, start, end);
    }

    @Test
    public void test056RecomputeUserJackEffectiveEnable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("User after change execution", userJackBefore);
        assertUserJack(userJackBefore, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJackBefore);
        assertValidity(userJackBefore, null);
        assertEffectiveStatus(userJackBefore, ActivationStatusType.ENABLED);
        assertEnableTimestampFocus(userJackBefore, null, start);

        // WHEN
        modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, ModelExecuteOptions.createRaw(), task, result, ActivationStatusType.DISABLED);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        assertValidity(userJack, null);
        assertEffectiveStatus(userJack, ActivationStatusType.DISABLED);

        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User after change execution", userJackAfter);
        assertUserJack(userJackAfter, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJackAfter);
        assertValidity(userJackAfter, null);
        assertEffectiveStatus(userJackAfter, ActivationStatusType.ENABLED);

        TestUtil.assertModifyTimestamp(userJackAfter, start, end);

    }

    @Test
    public void test060ModifyUserJackLifecycleActive() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, SchemaConstants.LIFECYCLE_ACTIVE);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        assertValidity(userJack, null);
        assertEffectiveStatus(userJack, ActivationStatusType.ENABLED);
        assertEnableTimestampFocus(userJack, null, start);

        TestUtil.assertModifyTimestamp(userJack, start, end);
    }

    @Test
    public void test061ModifyUserJackLifecycleDraft() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, SchemaConstants.LIFECYCLE_DRAFT);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        assertValidity(userJack, null);
        assertEffectiveStatus(userJack, ActivationStatusType.DISABLED);
        assertDisableTimestampFocus(userJack, start, end);

        TestUtil.assertModifyTimestamp(userJack, start, end);
    }

    @Test
    public void test065ModifyUserJackLifecycleDeprecated() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, SchemaConstants.LIFECYCLE_DEPRECATED);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        assertValidity(userJack, null);
        assertEffectiveStatus(userJack, ActivationStatusType.ENABLED);
        assertEnableTimestampFocus(userJack, start, end);

        TestUtil.assertModifyTimestamp(userJack, start, end);
    }

    @Test
    public void test068ModifyUserJackLifecycleArchived() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, SchemaConstants.LIFECYCLE_ARCHIVED);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        assertValidity(userJack, null);
        assertEffectiveStatus(userJack, ActivationStatusType.ARCHIVED);

        TestUtil.assertModifyTimestamp(userJack, start, end);
    }

    @Test
    public void test069ModifyUserJackLifecycleNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        assertValidity(userJack, null);
        assertEffectiveStatus(userJack, ActivationStatusType.ENABLED);

        TestUtil.assertModifyTimestamp(userJack, start, end);
    }

    @Test
    public void test100ModifyUserJackAssignAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        dummyAuditService.clear();
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountOid = getSingleLinkOid(userJack);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Shadow (repo)", accountShadow);
        assertDummyAccountShadowRepo(accountShadow, accountOid, "jack");
        TestUtil.assertCreateTimestamp(accountShadow, start, end);
        assertEnableTimestampShadow(accountShadow, start, end);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow (model)", accountModel);
        assertDummyAccountShadowModel(accountModel, accountOid, "jack", "Jack Sparrow");
        TestUtil.assertCreateTimestamp(accountModel, start, end);
        assertEnableTimestampShadow(accountModel, start, end);

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);

        assertDummyEnabled("jack");

        TestUtil.assertModifyTimestamp(userJack, start, end);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
        Collection<ObjectDeltaOperation<? extends ObjectType>> executionDeltas = dummyAuditService.getExecutionDeltas();
        boolean found = false;
        for (ObjectDeltaOperation<? extends ObjectType> executionDelta : executionDeltas) {
            ObjectDelta<? extends ObjectType> objectDelta = executionDelta.getObjectDelta();
            if (objectDelta.getObjectTypeClass() == ShadowType.class) {
                PropertyDelta<Object> enableTimestampDelta = objectDelta.findPropertyDelta(PATH_ACTIVATION_ENABLE_TIMESTAMP);
                displayDumpable("Audit enableTimestamp delta", enableTimestampDelta);
                assertNotNull("EnableTimestamp delta vanished from audit record", enableTimestampDelta);
                found = true;
            }
        }
        assertTrue("Shadow delta not found", found);
    }

    @Test
    public void test101ModifyUserJackDisable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusDisabled(userJack);
        assertDummyDisabled("jack");
        assertDisableTimestampFocus(userJack, startTime, endTime);

        String accountOid = getLinkRefOid(userJack, RESOURCE_DUMMY_OID);
        PrismObject<ShadowType> accountShadow = getShadowModel(accountOid);
        assertDisableTimestampShadow(accountShadow, startTime, endTime);
        assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_MAPPED);
    }

    @Test
    public void test102ModifyUserJackEnable() throws Exception {
        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test052ModifyUserJackEnable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        assertDummyEnabled("jack");
        assertEnableTimestampFocus(userJack, startTime, endTime);
    }

    /**
     * Modify account activation. User's activation should be unchanged
     */
    @Test
    public void test111ModifyAccountJackDisable() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test111ModifyAccountJackDisable");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyAccountShadowReplace(accountOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        String accountOid = getLinkRefOid(userJack, RESOURCE_DUMMY_OID);
        PrismObject<ShadowType> accountShadow = getShadowModel(accountOid);
        assertAdministrativeStatusDisabled(accountShadow);
        assertDisableTimestampShadow(accountShadow, startTime, endTime);
        assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);

        assertAdministrativeStatusEnabled(userJack);
        assertDummyDisabled("jack");
    }

    /**
     * Make sure that recompute does not destroy anything.
     */
    @Test
    public void test112UserJackRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        dummyAuditService.clear();

        // WHEN
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("recompute result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        String accountOid = getLinkRefOid(userJack, RESOURCE_DUMMY_OID);
        PrismObject<ShadowType> accountShadow = getShadowModel(accountOid);
        assertAdministrativeStatusDisabled(accountShadow);
        assertDisableTimestampShadow(accountShadow, null, startTime);
        assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);

        assertAdministrativeStatusEnabled(userJack);
        assertDummyDisabled("jack");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertNoRecord();
    }

    /**
     * Re-enabling the user should enable the account as well. Even if the user is already enabled.
     */
    @Test
    public void test114ModifyUserJackEnable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, USER_JACK_FULL_NAME);

        assertAdministrativeStatusEnabled(userJack);
        assertDummyEnabled(ACCOUNT_JACK_DUMMY_USERNAME);
        // No real change in effective status, therefore the enableTimestamp should be unchanged
        assertEnableTimestampFocus(userJack, null, startTime);

        assertAccounts(USER_JACK_OID, 1);
        PrismObject<ShadowType> account = getShadowModel(accountOid);
        assertAccountShadowModel(account, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType());
        assertAdministrativeStatusEnabled(account);
        assertEnableTimestampShadow(account, startTime, endTime);
    }

    /**
     * Re-enabling the user should enable the account as well. Even if the user is already enabled.
     */
    @Test
    public void test115ModifyUserJackAdministrativeStatusNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        DummyAccount account = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        displayDumpable("Account after change", account);

        assertUserJack(userJack, USER_JACK_FULL_NAME);

        assertAdministrativeStatus(userJack, null);
        // Dummy account should still be enabled. It does not support validity, therefore
        // the account/administrativeStatus is mapped from user.effectiveStatus
        assertDummyActivationEnabledState(ACCOUNT_JACK_DUMMY_USERNAME, true);

        assertAccounts(USER_JACK_OID, 1);
        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        assertAccountShadowModel(shadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType());
        assertAdministrativeStatus(shadow, ActivationStatusType.ENABLED);
    }

    /**
     * Modify both user and account activation. As outbound mapping is weak the user should have its own state
     * and account should have its own state.
     */
    @Test
    public void test118ModifyJackActivationUserAndAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountOid, getDummyResourceObject(),
                ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");
        // No real change in effective status, therefore the enableTimestamp should be unchanged
        assertEnableTimestampFocus(userJack, null, startTime);
        assertAdministrativeStatusEnabled(userJack);

        assertDummyDisabled("jack");
        assertAccounts(USER_JACK_OID, 1);
        PrismObject<ShadowType> account = getShadowModel(accountOid);
        assertAccountShadowModel(account, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType());
        assertAdministrativeStatusDisabled(account);
        assertDisableTimestampShadow(account, startTime, endTime);
        assertDisableReasonShadow(account, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
    }

    /**
     * Add red dummy resource to the mix. This will be fun.
     */
    @Test
    public void test120ModifyUserJackAssignAccountDummyRed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);

        PrismObject<ShadowType> accountRedRepo = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
        display("Account red (repo)", accountRedRepo);
        assertEnableTimestampShadow(accountRedRepo, startTime, endTime);

        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Account red (model)", accountRed);
        assertAccountShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME,
                getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertAdministrativeStatusEnabled(accountRed);
        assertEnableTimestampShadow(accountRed, startTime, endTime);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", true);

        assertAdministrativeStatusEnabled(userJack);
        assertDummyDisabled("jack");
        assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");

    }

    /**
     * Modify both user and account activation. Red dummy has a strong mapping. User change should override account
     * change.
     */
    @Test
    public void test121ModifyJackUserAndAccountRed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> userDelta = createModifyUserReplaceDelta(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);

        ObjectDelta<ShadowType> accountDelta = createModifyAccountShadowReplaceDelta(accountRedOid, getDummyResourceObject(),
                ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);

        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta, accountDelta);
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusDisabled(userJack);
        assertDummyDisabled("jack");
        assertDummyDisabled(RESOURCE_DUMMY_RED_NAME, "jack");

        assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        assertAccountShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME,
                getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertAdministrativeStatusDisabled(accountRed);
        assertDisableTimestampShadow(accountRed, startTime, endTime);
        assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_MAPPED);
    }

    @Test
    public void test130ModifyAccountDefaultAndRed() throws Exception {

        // GIVEN
        Task task = taskManager.createTaskInstance(TestActivation.class.getName() + ".test121ModifyJackPasswordUserAndAccountRed");
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<ShadowType> accountDeltaDefault = createModifyAccountShadowReplaceDelta(accountOid,
                getDummyResourceObject(), ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        ObjectDelta<ShadowType> accountDeltaRed = createModifyAccountShadowReplaceDelta(accountRedOid,
                getDummyResourceObject(RESOURCE_DUMMY_RED_NAME), ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDeltaDefault, accountDeltaRed);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusDisabled(userJack);
        assertDummyEnabled("jack");
        assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
    }

    /**
     * Let's make a clean slate for the next test
     */
    @Test
    public void test138ModifyJackEnabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "Jack Sparrow");

        assertAdministrativeStatusEnabled(userJack);
        assertDummyEnabled("jack");
        assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
    }

    /**
     * Red dummy resource disables account instead of deleting it.
     */
    @Test
    public void test139ModifyUserJackUnAssignAccountDummyRed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        assertAccountShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME,
                getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertAdministrativeStatusDisabled(accountRed);
        assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION);

        // Red resource has disabled-instead-of-delete, therefore the account should exists and be disabled
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);

        assertAdministrativeStatusEnabled(userJack);
        assertDummyEnabled("jack");
        assertDummyDisabled(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Assign account red again. The account should be re-enabled.
     */
    @Test
    public void test140ModifyUserJackAssignAccountDummyRed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);

        PrismObject<ShadowType> accountRedRepo = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
        display("Account red (repo)", accountRedRepo);
        assertEnableTimestampShadow(accountRedRepo, startTime, endTime);

        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Account red (model)", accountRed);
        assertAccountShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME,
                getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertAdministrativeStatusEnabled(accountRed);
        assertEnableTimestampShadow(accountRed, startTime, endTime);

        // Red account should be re-enabled
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", true);

        assertAdministrativeStatusEnabled(userJack);
        assertDummyEnabled("jack");
        assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
    }

    /**
     * Unassign red account. But do it raw, so the account activation is not affected.
     * MID-4154
     */
    @Test
    public void test147ModifyUserJackUnassignAccountDummyRedRaw() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), ModelExecuteOptions.createRaw(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);

        PrismObject<ShadowType> accountRedRepo = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
        display("Account red (repo)", accountRedRepo);

        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Account red (model)", accountRed);
        assertAccountShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME,
                getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertAdministrativeStatusEnabled(accountRed);

        // Red account should not be touched (yet)
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", true);

        assertAdministrativeStatusEnabled(userJack);
        assertDummyEnabled("jack");
        assertDummyEnabled(RESOURCE_DUMMY_RED_NAME, "jack");
    }

    /**
     * Recompute jack. There are no assignments for red resource.
     * But the account is enabled. It should be disabled after recompute.
     * MID-4154
     */
    @Test
    public void test149RecomputeJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);
        XMLGregorianCalendar endTime = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertAccounts(USER_JACK_OID, 2);
        accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);

        PrismObject<ShadowType> accountRedRepo = repositoryService.getObject(ShadowType.class, accountRedOid, null, result);
        display("Account red (repo)", accountRedRepo);
        assertDisableTimestampShadow(accountRedRepo, startTime, endTime);

        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Account red (model)", accountRed);
        assertAccountShadowModel(accountRed, accountRedOid, ACCOUNT_JACK_DUMMY_USERNAME,
                getDummyResourceType(RESOURCE_DUMMY_RED_NAME));
        assertAdministrativeStatusDisabled(accountRed);
        assertDisableTimestampShadow(accountRed, startTime, endTime);

        // Red account should be disabled now
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", "Jack Sparrow", false);

        assertAdministrativeStatusEnabled(userJack);
        assertDummyEnabled("jack");
        assertDummyDisabled(RESOURCE_DUMMY_RED_NAME, "jack");
    }

    /**
     * Assign yellow account.
     */
    @Test
    public void test150ModifyUserJackAssignYellowAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        accountYellowOid = getLinkRefOid(userJack, RESOURCE_DUMMY_YELLOW_OID);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Bla bla bla administrator -- administrator");

        // Check shadow
        PrismObject<ShadowType> accountShadowYellow = getShadowModel(accountYellowOid);
        assertAccountShadowModel(accountShadowYellow, accountYellowOid, ACCOUNT_JACK_DUMMY_USERNAME,
                getDummyResourceType(RESOURCE_DUMMY_YELLOW_NAME));
        assertAdministrativeStatusEnabled(accountShadowYellow);
        TestUtil.assertCreateTimestamp(accountShadowYellow, start, end);
        assertEnableTimestampShadow(accountShadowYellow, start, end);

        // Check user
        TestUtil.assertModifyTimestamp(userJack, start, end);
        assertAdministrativeStatusEnabled(userJack);
    }

    /**
     * MID-3946
     */
    @Test
    public void test151ReconcileJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after change execution", userAfter);
        assertUserJack(userAfter);
        assertLinked(userAfter, accountYellowOid);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Bla bla bla administrator -- administrator");

        // Check shadow
        PrismObject<ShadowType> accountShadowYellow = getShadowModel(accountYellowOid);
        assertAccountShadowModel(accountShadowYellow, accountYellowOid, ACCOUNT_JACK_DUMMY_USERNAME,
                getDummyResourceType(RESOURCE_DUMMY_YELLOW_NAME));
        assertAdministrativeStatusEnabled(accountShadowYellow);

        // Check user
        assertAdministrativeStatusEnabled(userAfter);
    }

    /**
     * Disable default & yellow accounts and check them after reconciliation.
     */
    @Test
    public void test152ModifyAccountsJackDisable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        ObjectDelta<ShadowType> dummyDelta = createModifyAccountShadowReplaceDelta(accountOid, null, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);
        ObjectDelta<ShadowType> yellowDelta = createModifyAccountShadowReplaceDelta(accountYellowOid, null, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.DISABLED);

        // WHEN
        when();

        modelService.executeChanges(MiscSchemaUtil.createCollection(dummyDelta, yellowDelta), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);

        checkAdminStatusFor15x(userJack, true, false, false);

        // WHEN (2) - now let's do a reconciliation on both resources
        when();
        ObjectDelta<UserType> innocentDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_LOCALITY,
                userJack.asObjectable().getLocality().toPolyString());
        modelService.executeChanges(MiscSchemaUtil.createCollection(innocentDelta), ModelExecuteOptions.createReconcile(), task, result);

        // THEN
        then();
        result.computeStatus();
        assertSuccess("executeChanges result (after reconciliation)", result);

        checkAdminStatusFor15x(userJack, true, false, true);        // yellow has a STRONG mapping for adminStatus, therefore it should be replaced by the user's adminStatus
    }

    /**
     * Disable default & yellow accounts and check them after reconciliation.
     */
    @Test
    public void test153ModifyAccountsJackEnable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        ObjectDelta<ShadowType> dummyDelta = createModifyAccountShadowReplaceDelta(accountOid, null, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        ObjectDelta<ShadowType> yellowDelta = createModifyAccountShadowReplaceDelta(accountYellowOid, null, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, ActivationStatusType.ENABLED);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(dummyDelta, yellowDelta);

        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);

        checkAdminStatusFor15x(userJack, true, true, true);

        // WHEN (2) - now let's do a reconciliation on both resources

        ObjectDelta innocentDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_LOCALITY,
                userJack.asObjectable().getLocality().toPolyString());
        modelService.executeChanges(MiscSchemaUtil.createCollection(innocentDelta), ModelExecuteOptions.createReconcile(), task, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result (after reconciliation)", result);

        checkAdminStatusFor15x(userJack, true, true, true);
    }

    private void checkAdminStatusFor15x(PrismObject<UserType> user, boolean userStatus, boolean accountStatus, boolean accountStatusYellow) throws Exception {
        PrismObject<ShadowType> account = getShadowModel(accountOid);
        PrismObject<ShadowType> accountYellow = getShadowModel(accountYellowOid);

        assertAccountShadowModel(account, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType());
        assertAdministrativeStatus(account, accountStatus ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);
        if (!accountStatus) {
            assertDisableReasonShadow(account, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
        }

        assertAccountShadowModel(accountYellow, accountYellowOid, ACCOUNT_JACK_DUMMY_USERNAME,
                getDummyResourceType(RESOURCE_DUMMY_YELLOW_NAME));
        if (accountStatusYellow) {
            assertAdministrativeStatus(accountYellow, ActivationStatusType.ENABLED);
            assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                    DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Bla bla bla administrator -- administrator");
        } else {
            assertAdministrativeStatus(accountYellow, ActivationStatusType.DISABLED);
            assertDisableReasonShadow(accountYellow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
            assertNoDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME);
        }

        assertAdministrativeStatus(user, userStatus ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", accountStatus);
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", accountStatusYellow);
    }

    /**
     * Khaki resource has simulated activation capability.
     */
    @Test
    public void test160ModifyUserJackAssignAccountKhaki() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        dummyAuditService.clear();
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_KHAKI_OID, null, task, result);

        // THEN
        then();
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);
        String accountKhakiOid = getLinkRefOid(userAfter, RESOURCE_DUMMY_KHAKI_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountKhakiOid, null, result);
        display("Shadow (repo)", accountShadow);
        assertAccountShadowRepo(accountShadow, accountKhakiOid, "jack", resourceDummyKhakiType);
        TestUtil.assertCreateTimestamp(accountShadow, start, end);
        assertEnableTimestampShadow(accountShadow, start, end);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountKhakiOid, null, task, result);
        display("Shadow (model)", accountModel);
        assertAccountShadowModel(accountModel, accountKhakiOid, "jack", resourceDummyKhakiType);
        TestUtil.assertCreateTimestamp(accountModel, start, end);
        assertEnableTimestampShadow(accountModel, start, end);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_KHAKI_NAME, "jack", "Jack Sparrow", true);

        assertDummyEnabled(RESOURCE_DUMMY_KHAKI_NAME, "jack");

        TestUtil.assertModifyTimestamp(userAfter, start, end);

        checkAdminStatusFor15x(userAfter, true, true, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);        // fourth one (trigger-related) was here by mistake
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
        Collection<ObjectDeltaOperation<? extends ObjectType>> executionDeltas = dummyAuditService.getExecutionDeltas();
        boolean found = false;
        for (ObjectDeltaOperation<? extends ObjectType> executionDelta : executionDeltas) {
            ObjectDelta<? extends ObjectType> objectDelta = executionDelta.getObjectDelta();
            if (objectDelta.getObjectTypeClass() == ShadowType.class) {
                // Actually, there should be no F_TRIGGER delta! It was there by mistake.
//                if (objectDelta.findContainerDelta(ShadowType.F_TRIGGER) != null) {
//                    continue;
//                }
                PropertyDelta<Object> enableTimestampDelta = objectDelta.findPropertyDelta(PATH_ACTIVATION_ENABLE_TIMESTAMP);
                displayDumpable("Audit enableTimestamp delta", enableTimestampDelta);
                assertNotNull("EnableTimestamp delta vanished from audit record, delta: " + objectDelta, enableTimestampDelta);
                found = true;
            }
        }
        assertTrue("Shadow delta not found", found);
    }

    @Test
    public void test170GetAccountUnlocked() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismAsserts.assertNoItem(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        checkAdminStatusFor15x(userAfter, true, true, true);
    }

    @Test
    public void test172GetAccountLocked() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        DummyAccount dummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setLockout(true);

        // WHEN
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
                LockoutStatusType.LOCKED);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        checkAdminStatusFor15x(userAfter, true, true, true);
    }

    @Test
    public void test174ModifyAccountUnlock() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        ObjectDelta<ShadowType> dummyDelta = createModifyAccountShadowReplaceDelta(accountOid, null,
                SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, LockoutStatusType.NORMAL);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(dummyDelta);

        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        DummyAccount dummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        assertFalse("Dummy account was not unlocked", dummyAccount.isLockout());

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);

        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
                LockoutStatusType.NORMAL);

        checkAdminStatusFor15x(userJack, true, true, true);
    }

    @Test
    public void test176ModifyUserUnlock() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        DummyAccount dummyAccount = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccount.setLockout(true);

        // WHEN
        modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, task, result,
                LockoutStatusType.NORMAL);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        DummyAccount dummyAccountAfter = getDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        assertFalse("Dummy account was not unlocked", dummyAccountAfter.isLockout());

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);

        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS,
                LockoutStatusType.NORMAL);

        checkAdminStatusFor15x(userJack, true, true, true);
    }

    @Test
    public void test199DeleteUserJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createDeleteDelta(UserType.class, USER_JACK_OID
        );
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        try {
            getUser(USER_JACK_OID);
            AssertJUnit.fail("Jack is still alive!");
        } catch (ObjectNotFoundException ex) {
            // This is OK
        }

        // Check that the accounts are gone
        assertNoDummyAccount(null, "jack");
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack");
    }

    @Test
    public void test200AddUserLargo() throws Exception {
        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userLargo = PrismTestUtil.parseObject(USER_LARGO_FILE);
        ObjectDelta<UserType> addDelta = userLargo.createAddDelta();
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(addDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        userLargo = getUser(USER_LARGO_OID);
        display("User after change execution", userLargo);

        assertValidity(userLargo, TimeIntervalStatusType.IN);
        assertValidityTimestamp(userLargo, startMillis, System.currentTimeMillis());
        assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);
    }

    @Test
    public void test205ModifyUserLargoAssignAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_LARGO_OID, RESOURCE_DUMMY_OID, null, true);
        accountAssignmentUserDelta.addModificationAddProperty(UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Largo LaGrande"));
        deltas.add(accountAssignmentUserDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess("executeChanges result", result);

        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
        display("User after change execution", userLargo);
        assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleLinkOid(userLargo);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
    }

    @Test
    public void test210ModifyLargoValidTo10MinsAgo() throws Exception {
        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        XMLGregorianCalendar tenMinutesAgo = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis() - 10 * 60 * 1000);

        // WHEN
        modifyUserReplace(USER_LARGO_OID, ACTIVATION_VALID_TO_PATH, task, result, tenMinutesAgo);

        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
        display("User after change execution", userLargo);

        assertValidity(userLargo, TimeIntervalStatusType.AFTER);
        assertValidityTimestamp(userLargo, startMillis, System.currentTimeMillis());
        assertEffectiveStatus(userLargo, ActivationStatusType.DISABLED);

        assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleLinkOid(userLargo);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", false);
    }

    @Test
    public void test211ModifyLargoValidToManana() throws Exception {
        // GIVEN
        long startMillis = System.currentTimeMillis();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        manana = XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000);

        // WHEN
        modifyUserReplace(USER_LARGO_OID, ACTIVATION_VALID_TO_PATH, task, result, manana);

        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
        display("User after change execution", userLargo);

        assertValidity(userLargo, TimeIntervalStatusType.IN);
        assertValidityTimestamp(userLargo, startMillis, System.currentTimeMillis());
        lastValidityChangeTimestamp = userLargo.asObjectable().getActivation().getValidityChangeTimestamp();
        assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);

        assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleLinkOid(userLargo);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
    }

    /**
     * Move time to tomorrow. Nothing should change yet. It is not yet manana.
     */
    @Test
    public void test212SeeLargoTomorrow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Let's play with the clock, move the time to tomorrow
        long currentNow = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
        clock.override(currentNow);

        // WHEN
        recomputeUser(USER_LARGO_OID, task, result);

        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
        display("User after change execution", userLargo);

        assertValidity(userLargo, TimeIntervalStatusType.IN);
        assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);
        assertValidityTimestamp(userLargo, lastValidityChangeTimestamp);

        assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleLinkOid(userLargo);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
    }

    /**
     * Move time after manana. Largo should be invalid.
     */
    @Test
    public void test213HastaLaMananaLargo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Let's play with the clock, move the time forward 20 days
        long currentNow = System.currentTimeMillis() + 20 * 24 * 60 * 60 * 1000;
        clock.override(currentNow);

        // WHEN
        modelService.recompute(UserType.class, USER_LARGO_OID, null, task, result);

        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
        display("User after change execution", userLargo);

        assertValidity(userLargo, TimeIntervalStatusType.AFTER);
        assertValidityTimestamp(userLargo, currentNow);
        assertEffectiveStatus(userLargo, ActivationStatusType.DISABLED);

        assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleLinkOid(userLargo);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", false);
    }

    @Test
    public void test215ModifyLargoRemoveValidTo() throws Exception {
        // GIVEN
        long startMillis = clock.currentTimeMillis();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        modifyUserDelete(USER_LARGO_OID, ACTIVATION_VALID_TO_PATH, task, result, manana);

        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
        display("User after change execution", userLargo);

        assertValidity(userLargo, TimeIntervalStatusType.IN);
        assertValidityTimestamp(userLargo, startMillis, clock.currentTimeMillis());
        lastValidityChangeTimestamp = userLargo.asObjectable().getActivation().getValidityChangeTimestamp();
        assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);

        assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleLinkOid(userLargo);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
    }

    @Test
    public void test217ModifyLargoRemoveValidFrom() throws Exception {
        // GIVEN
        long startMillis = clock.currentTimeMillis();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        modifyUserReplace(USER_LARGO_OID, ACTIVATION_VALID_FROM_PATH, task, result);

        // THEN
        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
        display("User after change execution", userLargo);

        assertValidity(userLargo, null);
        assertValidityTimestamp(userLargo, startMillis, clock.currentTimeMillis());
        lastValidityChangeTimestamp = userLargo.asObjectable().getActivation().getValidityChangeTimestamp();
        assertEffectiveStatus(userLargo, ActivationStatusType.ENABLED);

        assertUser(userLargo, USER_LARGO_OID, USER_LARGO_USERNAME, "Largo LaGrande", "Largo", "LaGrande");
        accountOid = getSingleLinkOid(userLargo);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, USER_LARGO_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, USER_LARGO_USERNAME, "Largo LaGrande");

        // Check account in dummy resource
        assertDefaultDummyAccount(USER_LARGO_USERNAME, "Largo LaGrande", true);
    }

    /**
     * Delete assignment from repo. Model should not notice.
     * The change should be applied after recompute, because RED resource has
     * strong mappings, which trigger account to be loaded.
     */
    @Test
    public void test230JackUnassignRepoRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(USER_JACK_FILE);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, task, result);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        // Delete the assignment from the repo. Really use the repo directly. We do not want the model to notice.
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        repositoryService.modifyObject(UserType.class, userDelta.getOid(), userDelta.getModifications(), result);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);
    }

    /**
     * Now recompute with reconcile. The change should be applied after recompute.
     */
    @Test
    public void test231JackRecomputeReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, ModelExecuteOptions.createReconcile(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);
    }

    /**
     * Add draft user with a role assignment.
     * Even though Pirate role gives dummy account the user is in the draft
     * state. No account should be created.
     * MID-3689, MID-3737
     */
    @Test
    public void test240AddUserRappDraft() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_RAPP_FILE);
        UserType userTypeBefore = userBefore.asObjectable();
        userTypeBefore.setLifecycleState(SchemaConstants.LIFECYCLE_DRAFT);
        AssignmentType assignmentType = new AssignmentType();
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(ROLE_CARIBBEAN_PIRATE_OID);
        targetRef.setType(RoleType.COMPLEX_TYPE);
        assignmentType.setTargetRef(targetRef);
        ActivationType activationType = new ActivationType();
        // Make sure that this assignment is very explicitly enabled.
        // Even though it is enabled the lifecycle should overrule it.
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
        assignmentType.setActivation(activationType);
        userTypeBefore.getAssignment().add(assignmentType);
        display("User before", userBefore);

        // WHEN
        when();
        addObject(userBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("user after", userAfter);

        displayDumpable("Dummy", getDummyResource());

        assertEffectiveActivation(userAfter, ActivationStatusType.DISABLED);

        assertAssignedRole(userAfter, ROLE_CARIBBEAN_PIRATE_OID);
        assertAssignments(userAfter, 1);

        // assignments are not active due to lifecycle, there should
        // be no roles in roleMembershipRef
        // MID-3741
        assertRoleMembershipRef(userAfter);

        assertLinks(userAfter, 0);

        assertNoDummyAccount(USER_RAPP_USERNAME);

    }

    /**
     * Still draft. Still no accounts.
     * MID-3689, MID-3737
     */
    @Test
    public void test241RecomputeRappDraft() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(USER_RAPP_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("user after", userAfter);

        displayDumpable("Dummy", getDummyResource());

        assertEffectiveActivation(userAfter, ActivationStatusType.DISABLED);

        assertAssignedRole(userAfter, ROLE_CARIBBEAN_PIRATE_OID);
        assertAssignments(userAfter, 1);

        // assignments are not active due to lifecycle, there should
        // be no roles in roleMembershipRef
        // MID-3741
        assertRoleMembershipRef(userAfter);

        assertLinks(userAfter, 0);

        assertNoDummyAccount(USER_RAPP_USERNAME);

    }

    /**
     * Even though Captain role gives dummy account the user is in the draft
     * state. No account should be created.
     * MID-3689, MID-3737
     */
    @Test
    public void test242RappAssignCaptain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_RAPP_OID, ROLE_CAPTAIN_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("user after", userAfter);
        assertEffectiveActivation(userAfter, ActivationStatusType.DISABLED);

        assertAssignedRoles(userAfter, ROLE_CARIBBEAN_PIRATE_OID, ROLE_CAPTAIN_OID);
        assertAssignments(userAfter, 2);
        assertLinks(userAfter, 0);

        // assignments are not active due to lifecycle, there should
        // be no roles in roleMembershipRef
        // MID-3741
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(USER_RAPP_USERNAME);
    }

    /**
     * Switch Rapp to active state. The assignments should be
     * activated as well.
     * MID-3689, MID-3737
     */
    @Test
    public void test245ActivateRapp() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_RAPP_OID, UserType.F_LIFECYCLE_STATE, task, result,
                SchemaConstants.LIFECYCLE_ACTIVE);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("user after", userAfter);
        assertEffectiveActivation(userAfter, ActivationStatusType.ENABLED);

        assertAssignedRoles(userAfter, ROLE_CARIBBEAN_PIRATE_OID, ROLE_CAPTAIN_OID);
        assertAssignments(userAfter, 2);
        assertLinks(userAfter, 1);

        assertRoleMembershipRef(userAfter, ROLE_CARIBBEAN_PIRATE_OID, ROLE_PIRATE_OID, ROLE_CAPTAIN_OID);

        DummyAccount dummyAccount = assertDummyAccount(null, USER_RAPP_USERNAME);
        displayDumpable("dummy account", dummyAccount);
        assertDummyAccountAttribute(null, USER_RAPP_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDummyAccountAttribute(null, USER_RAPP_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, RESOURCE_DUMMY_QUOTE);
        assertDummyAccountAttribute(null, USER_RAPP_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
    }

    /**
     * Switch Rapp to archived state. The assignments should be
     * deactivated as well.
     * MID-3689
     */
    @Test
    public void test248DeactivateRapp() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_RAPP_OID, UserType.F_LIFECYCLE_STATE, task, result,
                SchemaConstants.LIFECYCLE_ARCHIVED);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_RAPP_OID);
        display("user after", userAfter);
        assertEffectiveActivation(userAfter, ActivationStatusType.ARCHIVED);

        assertAssignedRoles(userAfter, ROLE_CARIBBEAN_PIRATE_OID, ROLE_CAPTAIN_OID);
        assertAssignments(userAfter, 2);
        assertLinks(userAfter, 0);

        // assignments are not active due to lifecycle, there should
        // be no roles in roleMembershipRef
        // MID-3741
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(USER_RAPP_USERNAME);
    }

    /**
     * MID-3689
     */
    @Test
    public void test249DeleteUserRapp() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        deleteObject(UserType.class, USER_RAPP_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoObject(UserType.class, USER_RAPP_OID);
        assertNoDummyAccount(USER_RAPP_USERNAME);
    }

    /**
     * Test reading of validity data through shadow
     */
    @Test
    public void test300AddDummyGreenAccountMancomb() throws Exception {
        // GIVEN
        DummyAccount account = new DummyAccount(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        account.setEnabled(true);
        account.setValidFrom(ACCOUNT_MANCOMB_VALID_FROM_DATE);
        account.setValidTo(ACCOUNT_MANCOMB_VALID_TO_DATE);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Mancomb Seepgood");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");

        /// WHEN
        when();

        getDummyResource(RESOURCE_DUMMY_GREEN_NAME).addAccount(account);

        // THEN
        then();

        PrismObject<ShadowType> accountMancomb = findAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME, getDummyResourceObject(RESOURCE_DUMMY_GREEN_NAME));
        display("Account shadow after", accountMancomb);

        DummyAccount dummyAccountAfter = getDummyResource(RESOURCE_DUMMY_GREEN_NAME).getAccountByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        displayDumpable("Account after", dummyAccountAfter);

        assertNotNull("No mancomb account shadow", accountMancomb);
        assertEquals("Wrong resourceRef in mancomb account", RESOURCE_DUMMY_GREEN_OID,
                accountMancomb.asObjectable().getResourceRef().getOid());
        assertValidFrom(accountMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(accountMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);
    }

    /**
     * Testing of inbound mappings for validity data. Done by initiating an import of accouts from green resource.
     */
    @Test
    public void test310ImportAccountsFromDummyGreen() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Preconditions
        assertUsers(7);
        PrismObject<UserType> userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNull("Unexpected user mancomb before import", userMancomb);

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_GREEN_OID, new QName(getDummyResourceController(RESOURCE_DUMMY_GREEN_NAME).getNamespace(), SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        waitForTaskFinish(task, true, 40000);

        then();

        userMancomb = findUserByUsername(ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No user mancomb after import", userMancomb);
        userMancombOid = userMancomb.getOid();

        assertUsers(8);

        assertAdministrativeStatusEnabled(userMancomb);
        assertValidFrom(userMancomb, ACCOUNT_MANCOMB_VALID_FROM_DATE);
        assertValidTo(userMancomb, ACCOUNT_MANCOMB_VALID_TO_DATE);
    }

    @Test
    public void test350AssignMancombBlueAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignAccountToUser(userMancombOid, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userMancomb = getUser(userMancombOid);
        display("User after change execution", userMancomb);
        assertAccounts(userMancombOid, 2);

        DummyAccount mancombBlueAccount = getDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No mancomb blue account", mancombBlueAccount);
        assertTrue("mancomb blue account not enabled", mancombBlueAccount.isEnabled());
        assertEquals("Wrong validFrom in mancomb blue account", ACCOUNT_MANCOMB_VALID_FROM_DATE, mancombBlueAccount.getValidFrom());
        assertEquals("Wrong validTo in mancomb blue account", ACCOUNT_MANCOMB_VALID_TO_DATE, mancombBlueAccount.getValidTo());
    }

    @Test
    public void test352AssignMancombBlackAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignAccountToUser(userMancombOid, RESOURCE_DUMMY_BLACK_OID, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userMancomb = getUser(userMancombOid);
        display("User after change execution", userMancomb);
        assertAccounts(userMancombOid, 3);

        DummyAccount mancombBlueAccount = getDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No mancomb blue account", mancombBlueAccount);
        assertTrue("mancomb blue account not enabled", mancombBlueAccount.isEnabled());
        assertEquals("Wrong validFrom in mancomb blue account", ACCOUNT_MANCOMB_VALID_FROM_DATE, mancombBlueAccount.getValidFrom());
        assertEquals("Wrong validTo in mancomb blue account", ACCOUNT_MANCOMB_VALID_TO_DATE, mancombBlueAccount.getValidTo());

        DummyAccount mancombBlackAccount = getDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No mancomb black account", mancombBlackAccount);
        assertTrue("mancomb black account not enabled", mancombBlackAccount.isEnabled());
        assertEquals("Wrong validFrom in mancomb black account", ACCOUNT_MANCOMB_VALID_FROM_DATE, mancombBlackAccount.getValidFrom());
        assertEquals("Wrong validTo in mancomb black account", ACCOUNT_MANCOMB_VALID_TO_DATE, mancombBlackAccount.getValidTo());
    }

    @Test
    public void test355MancombModifyAdministrativeStatusNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        modifyUserReplace(userMancombOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userMancomb = getUser(userMancombOid);
        display("User after change execution", userMancomb);
        assertAccounts(userMancombOid, 3);

        DummyAccount mancombBlueAccount = getDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No mancomb blue account", mancombBlueAccount);
        // Blue resouce has only weak administrativeStatus mapping. The values is not reset to null.
        // This does not work now: MID-3418
//        assertEquals("Wring mancomb blue account enabled flag", Boolean.TRUE, mancombBlueAccount.isEnabled());
        assertEquals("Wrong validFrom in mancomb blue account", ACCOUNT_MANCOMB_VALID_FROM_DATE, mancombBlueAccount.getValidFrom());
        assertEquals("Wrong validTo in mancomb blue account", ACCOUNT_MANCOMB_VALID_TO_DATE, mancombBlueAccount.getValidTo());

        DummyAccount mancombBlackAccount = getDummyAccount(RESOURCE_DUMMY_BLACK_NAME, ACCOUNT_MANCOMB_DUMMY_USERNAME);
        assertNotNull("No mancomb black account", mancombBlackAccount);
        assertEquals("Wring mancomb black account enabled flag", null, mancombBlackAccount.isEnabled());
        assertEquals("Wrong validFrom in mancomb black account", ACCOUNT_MANCOMB_VALID_FROM_DATE, mancombBlackAccount.getValidFrom());
        assertEquals("Wrong validTo in mancomb black account", ACCOUNT_MANCOMB_VALID_TO_DATE, mancombBlackAccount.getValidTo());
    }

    @Test
    public void test400AddHerman() throws Exception {
        // WHEN
        addObject(USER_HERMAN_FILE);

        // THEN
        // Make sure that it is effectively enabled
        PrismObject<UserType> userHermanAfter = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanAfter, ActivationStatusType.ENABLED);
    }

    /**
     * Herman has validTo/validFrom. Khaki resource has strange mappings for these.
     */
    @Test
    public void test410AssignHermanKhakiAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        assignAccountToUser(USER_HERMAN_OID, RESOURCE_DUMMY_KHAKI_OID, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User after change execution", user);
        assertLinks(user, 1);

        DummyAccount khakiAccount = getDummyAccount(RESOURCE_DUMMY_KHAKI_NAME, USER_HERMAN_USERNAME);
        assertNotNull("No khaki account", khakiAccount);
        assertTrue("khaki account not enabled", khakiAccount.isEnabled());
        assertEquals("Wrong quote (validFrom) in khaki account", "from: 1700-05-30T11:00:00.000Z",
                khakiAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME));
        assertEquals("Wrong drink (validTo) in khaki account", "to: 2233-03-23T18:30:00.000Z",
                khakiAccount.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME));
    }

    @Test
    public void test500CheckRolePirateInitial() throws Exception {
        test5X0CheckInitial(RoleType.class, ROLE_PIRATE_OID);
    }

    @Test
    public void test501RolePirateRecompute() throws Exception {
        test5X1Recompute(RoleType.class, ROLE_PIRATE_OID);
    }

    @Test
    public void test502ModifyRolePirateDisable() throws Exception {
        test5X2ModifyDisable(RoleType.class, ROLE_PIRATE_OID);
    }

    @Test
    public void test504ModifyRolePirateEnable() throws Exception {
        test5X4ModifyEnable(RoleType.class, ROLE_PIRATE_OID);
    }

    @Test
    public void test505RolePirateRecompute() throws Exception {
        test5X5Recompute(RoleType.class, ROLE_PIRATE_OID);
    }

    @Test
    public void test510CheckOrgScummBarInitial() throws Exception {
        test5X0CheckInitial(OrgType.class, ORG_SCUMM_BAR_OID);
    }

    @Test
    public void test511OrgScummBarRecompute() throws Exception {
        test5X1Recompute(OrgType.class, ORG_SCUMM_BAR_OID);
    }

    @Test
    public void test512ModifyOrgScummBarDisable() throws Exception {
        test5X2ModifyDisable(OrgType.class, ORG_SCUMM_BAR_OID);
    }

    @Test
    public void test514ModifyOrgScummBarEnable() throws Exception {
        test5X4ModifyEnable(OrgType.class, ORG_SCUMM_BAR_OID);
    }

    @Test
    public void test515OrgScummBarRecompute() throws Exception {
        test5X5Recompute(OrgType.class, ORG_SCUMM_BAR_OID);
    }

    @Test
    public void test520CheckServiceSeaMonkeyInitial() throws Exception {
        test5X0CheckInitial(ServiceType.class, SERVICE_SHIP_SEA_MONKEY_OID);
    }

    @Test
    public void test521ServiceSeaMonkeyRecompute() throws Exception {
        test5X1Recompute(ServiceType.class, SERVICE_SHIP_SEA_MONKEY_OID);
    }

    @Test
    public void test522ModifyServiceSeaMonkeyDisable() throws Exception {
        test5X2ModifyDisable(ServiceType.class, SERVICE_SHIP_SEA_MONKEY_OID);
    }

    @Test
    public void test524ModifyServiceSeaMonkeyEnable() throws Exception {
        test5X4ModifyEnable(ServiceType.class, SERVICE_SHIP_SEA_MONKEY_OID);
    }

    @Test
    public void test525ServiceSeaMonkeyRecompute() throws Exception {
        test5X5Recompute(ServiceType.class, SERVICE_SHIP_SEA_MONKEY_OID);
    }

    private <F extends FocusType> void test5X0CheckInitial(Class<F> type, String oid) throws Exception {

        // GIVEN, WHEN
        // this happens during test initialization when the role is added

        // THEN
        PrismObject<F> objectAfter = getObject(type, oid);
        display("Object after", objectAfter);

        assertAdministrativeStatus(objectAfter, null);
        // Cannot assert validity or effective status here. The object was added through repo and was not recomputed yet.
    }

    /**
     * Make sure that recompute properly updates the effective status.
     * MID-2877
     */
    private <F extends FocusType> void test5X1Recompute(Class<F> type, String oid) throws Exception {

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        modelService.recompute(type, oid, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<F> objectAfter = getObject(type, oid);
        display("Object after", objectAfter);

        assertAdministrativeStatus(objectAfter, null);
        assertValidity(objectAfter, null);
        assertEffectiveStatus(objectAfter, ActivationStatusType.ENABLED);
    }

    /**
     * MID-2877
     */
    private <F extends FocusType> void test5X2ModifyDisable(Class<F> type, String oid) throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        when();
        modifyObjectReplaceProperty(type, oid,
                ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        then();
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<F> objectAfter = getObject(type, oid);
        display("Object after", objectAfter);

        assertAdministrativeStatusDisabled(objectAfter);
        assertValidity(objectAfter, null);
        assertEffectiveStatus(objectAfter, ActivationStatusType.DISABLED);
        assertDisableTimestampFocus(objectAfter, start, end);

        TestUtil.assertModifyTimestamp(objectAfter, start, end);
    }

    /**
     * MID-2877
     */
    private <F extends FocusType> void test5X4ModifyEnable(Class<F> type, String oid) throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        when();
        modifyObjectReplaceProperty(type, oid,
                ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        then();
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<F> objectAfter = getObject(type, oid);
        display("Object after", objectAfter);

        assertAdministrativeStatusEnabled(objectAfter);
        assertValidity(objectAfter, null);
        assertEffectiveStatus(objectAfter, ActivationStatusType.ENABLED);
        assertEnableTimestampFocus(objectAfter, start, end);

        TestUtil.assertModifyTimestamp(objectAfter, start, end);
    }

    /**
     * Make sure that recompute does not destroy anything.
     */
    private <F extends FocusType> void test5X5Recompute(Class<F> type, String oid) throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        dummyAuditService.clear();

        when();
        modelService.recompute(type, oid, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<F> objectAfter = getObject(type, oid);
        display("Object after", objectAfter);

        assertAdministrativeStatusEnabled(objectAfter);
        assertValidity(objectAfter, null);
        assertEffectiveStatus(objectAfter, ActivationStatusType.ENABLED);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertNoRecord();
    }

    // attempt to simulate MID-3348 (unsuccessful for now)
    @Test
    public void test600AddUser1() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> user1 = prismContext.createObject(UserType.class);
        prismContext.deltaFor(UserType.class)
                .item(UserType.F_NAME).replace(new PolyString("user1"))
                .item(UserType.F_ASSIGNMENT).add(ObjectTypeUtil.createAssignmentTo(resourceDummyCoral, prismContext).asPrismContainerValue())
                .item(ACTIVATION_ADMINISTRATIVE_STATUS_PATH).replace(ActivationStatusType.DISABLED)
                .<UserType>asObjectDelta(null)
                .applyTo(user1);

        ObjectDelta<UserType> addDelta = user1.createAddDelta();

        // WHEN
        when();
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges = executeChanges(addDelta, null, task, result);

        // THEN
        then();
        user1 = getUser(ObjectDeltaOperation.findFocusDeltaOidInCollection(executedChanges));
        display("User after change execution", user1);

        DummyAccount dummyAccount = dummyResourceCoral.getAccountByUsername("user1");
        displayDumpable("Dummy account", dummyAccount);
        checkSuspendedAttribute(dummyAccount, Boolean.TRUE);

        String accountOid = getSingleLinkOid(user1);
        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow: ", shadow);

        // TODO check real state of the account and shadow
    }

    private void checkSuspendedAttribute(DummyAccount dummyAccount, Boolean expectedValue) {
        Object suspendedAttributeValue = dummyAccount.getAttributeValue("suspended", Object.class);
        System.out.println("\nsuspended: " + suspendedAttributeValue + ", class: " + suspendedAttributeValue.getClass());
        assertEquals("Wrong type of 'suspended' attribute", Boolean.class, suspendedAttributeValue.getClass());
        assertEquals("Wrong typed value of 'suspended' attribute", expectedValue, suspendedAttributeValue);
    }

    @Test
    public void test610EnableUser1() throws Exception {
        // GIVEN
        PrismObject<UserType> user1 = findUserByUsername("user1");

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas =
                cast(prismContext.deltaFor(UserType.class)
                        .item(ACTIVATION_ADMINISTRATIVE_STATUS_PATH).replace(ActivationStatusType.ENABLED)
                        .asObjectDeltas(user1.getOid()));

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        user1 = getUser(user1.getOid());
        display("User after change execution", user1);

        DummyAccount dummyAccount = dummyResourceCoral.getAccountByUsername("user1");
        displayDumpable("Dummy account", dummyAccount);
        checkSuspendedAttribute(dummyAccount, Boolean.FALSE);

        String accountOid = getSingleLinkOid(user1);
        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow: ", shadow);

        // TODO check real state of the account and shadow
    }

    /**
     * MID-3695
     */
    @Test
    public void test700ModifyJackRemoveAdministrativeStatus() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_COMPLEX_OID, result);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        assertNoAssignments(userBefore);

        // Disabled RED account. RED resource has disable instead delete
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);

        assertAdministrativeStatusEnabled(userBefore);
        assertEffectiveActivation(userBefore, ActivationStatusType.ENABLED);
        assertValidityStatus(userBefore, null);
        PrismAsserts.assertNoItem(userBefore, SchemaConstants.PATH_ACTIVATION_VALID_FROM);
        PrismAsserts.assertNoItem(userBefore, SchemaConstants.PATH_ACTIVATION_VALID_TO);

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, "Jack Sparrow");

        assertAdministrativeStatus(userAfter, null);
        assertEffectiveActivation(userAfter, ActivationStatusType.ENABLED);
        assertValidityStatus(userAfter, null);
        PrismAsserts.assertNoItem(userAfter, SchemaConstants.PATH_ACTIVATION_VALID_FROM);
        PrismAsserts.assertNoItem(userAfter, SchemaConstants.PATH_ACTIVATION_VALID_TO);

        // Unconditional automatic assignment of Blue Dummy resource in user-template-complex-include
        assertAssignments(userAfter, 1);
    }

    /**
     * MID-3695
     */
    @Test
    public void test702ModifyJackFuneralTimestampBeforeNow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        display("System config before", getSystemConfiguration());
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_COMPLEX_OID, result);
        display("System config after", getSystemConfiguration());

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        assertAssignments(userBefore, 1);

        XMLGregorianCalendar hourAgo = XmlTypeConverter.createXMLGregorianCalendar(clock.currentTimeMillis() - 60 * 60 * 1000);

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, getExtensionPath(PIRACY_FUNERAL_TIMESTAMP), task, result, hourAgo);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, "Jack Sparrow");

        PrismAsserts.assertNoItem(userAfter, SchemaConstants.PATH_ACTIVATION_VALID_FROM);
        assertEquals("Wrong validTo in user", hourAgo, getActivation(userAfter).getValidTo());

        assertAdministrativeStatus(userAfter, null);
        assertValidityStatus(userAfter, TimeIntervalStatusType.AFTER);
        assertEffectiveActivation(userAfter, ActivationStatusType.DISABLED);
    }

    /**
     * This is where MID-3695 is really reproduced.
     * MID-3695
     */
    @Test
    public void test704ModifyJackFuneralTimestampAfterNow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_COMPLEX_OID, result);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        assertAssignments(userBefore, 1);

        XMLGregorianCalendar hourAhead = XmlTypeConverter.createXMLGregorianCalendar(clock.currentTimeMillis() + 60 * 60 * 1000);

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, getExtensionPath(PIRACY_FUNERAL_TIMESTAMP), task, result, hourAhead);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, "Jack Sparrow");

        PrismAsserts.assertNoItem(userAfter, SchemaConstants.PATH_ACTIVATION_VALID_FROM);
        assertEquals("Wrong validTo in user", hourAhead, getActivation(userAfter).getValidTo());

        assertAdministrativeStatus(userAfter, null);
        assertValidityStatus(userAfter, TimeIntervalStatusType.IN);
        assertEffectiveActivation(userAfter, ActivationStatusType.ENABLED);
    }

    /**
     * MID-3507
     */
    @Test
    public void test750AddAndDeleteUserWithPrecreate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        clock.resetOverride();

        XMLGregorianCalendar yesterday = clock.currentTimeXMLGregorianCalendar();
        yesterday.add(XmlTypeConverter.createDuration("-P1D"));
        System.out.println("yesterday = " + yesterday);

        UserType user = new UserType(prismContext)
                .name("test750")
                .beginAssignment()
                .beginConstruction()
                .resourceRef(RESOURCE_DUMMY_PRECREATE.oid, ResourceType.COMPLEX_TYPE)
                .<AssignmentType>end()
                .<UserType>end()
                .beginActivation()
                .validFrom(yesterday)
                .end();
        String oid = addObject(user.asPrismObject(), task, result);
        assertSuccess(result);

        // WHEN
        when();
        deleteObject(UserType.class, oid, task, result);

        // THEN
        then();

        // There's a hidden FATAL_ERROR when unlinking deleted shadow (benign ... but it prevents us from asserting success tree-wide)
        //assertSuccess(result);
        assertSuccess(result, 2);
    }


    /**
     * If an invalid assignment is being deleted, we don't want to update its effectiveStatus (because the resulting replace
     * delta would collide with assignment delete delta). Normally midPoint ensures this behavior. So this test is not
     * strictly necessary.
     *
     * This test was introduced as part of MID-6122 diagnosis and is kept only as "to be sure". In MID-6122 the deltas collision
     * was caused by adding assignment delete delta in the model scripting hook.
     */
    @Test
    public void test800DeleteInvalidAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, null, result);

        XMLGregorianCalendar dayBeforeYesterday = XmlTypeConverter.fromNow("-P2D");
        XMLGregorianCalendar yesterday = XmlTypeConverter.fromNow("-P1D");

        UserType user = new UserType(prismContext)
                .name("test800")
                .beginAssignment()
                .targetRef(ROLE_SUPERUSER_OID, RoleType.COMPLEX_TYPE)
                .beginActivation()
                .effectiveStatus(ActivationStatusType.ENABLED)
                .validFrom(dayBeforeYesterday)
                .validTo(yesterday)
                .<AssignmentType>end()
                .end();
        repoAddObject(user.asPrismObject(), result);

        when();
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(user.getAssignment().get(0).clone())
                .asObjectDelta(user.getOid());
        executeChanges(delta, null, task, result);

        then();
        result.computeStatus();
        assertSuccess(result);
        assertUser(user.getOid(), "after")
                .display()
                .assertAssignments(0);
    }

    private void assertDummyActivationEnabledState(String userId, Boolean expectedEnabled) throws SchemaViolationException, ConflictException, InterruptedException {
        assertDummyActivationEnabledState(null, userId, expectedEnabled);
    }

    private void assertDummyActivationEnabledState(String instance, String userId, Boolean expectedEnabled) throws SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account = getDummyAccount(instance, userId);
        assertNotNull("No dummy account " + userId, account);
        assertEquals("Wrong enabled flag in dummy '" + instance + "' account " + userId, expectedEnabled, account.isEnabled());
    }

    private void assertDummyEnabled(String userId) throws SchemaViolationException, ConflictException, InterruptedException {
        assertDummyActivationEnabledState(userId, true);
    }

    private void assertDummyDisabled(String userId) throws SchemaViolationException, ConflictException, InterruptedException {
        assertDummyActivationEnabledState(userId, false);
    }

    private void assertDummyEnabled(String instance, String userId) throws SchemaViolationException, ConflictException, InterruptedException {
        assertDummyActivationEnabledState(instance, userId, true);
    }

    private void assertDummyDisabled(String instance, String userId) throws SchemaViolationException, ConflictException, InterruptedException {
        assertDummyActivationEnabledState(instance, userId, false);
    }

    private <F extends FocusType> void assertValidity(PrismObject<F> focus, TimeIntervalStatusType expectedValidityStatus) {
        ActivationType activation = focus.asObjectable().getActivation();
        assertNotNull("No activation in " + focus, activation);
        assertEquals("Unexpected validity status in " + focus, expectedValidityStatus, activation.getValidityStatus());
    }

    private void assertValidityTimestamp(PrismObject<UserType> user, long expected) {
        ActivationType activation = user.asObjectable().getActivation();
        assertNotNull("No activation in " + user, activation);
        XMLGregorianCalendar validityChangeTimestamp = activation.getValidityChangeTimestamp();
        assertNotNull("No validityChangeTimestamp in " + user, validityChangeTimestamp);
        assertEquals("wrong validityChangeTimestamp", expected, XmlTypeConverter.toMillis(validityChangeTimestamp));
    }

    private void assertValidityTimestamp(PrismObject<UserType> user, XMLGregorianCalendar expected) {
        ActivationType activation = user.asObjectable().getActivation();
        assertNotNull("No activation in " + user, activation);
        XMLGregorianCalendar validityChangeTimestamp = activation.getValidityChangeTimestamp();
        assertNotNull("No validityChangeTimestamp in " + user, validityChangeTimestamp);
        assertEquals("wrong validityChangeTimestamp", expected, validityChangeTimestamp);
    }

    private void assertValidityTimestamp(PrismObject<UserType> user, long lowerBound, long upperBound) {
        ActivationType activation = user.asObjectable().getActivation();
        assertNotNull("No activation in " + user, activation);
        XMLGregorianCalendar validityChangeTimestamp = activation.getValidityChangeTimestamp();
        assertNotNull("No validityChangeTimestamp in " + user, validityChangeTimestamp);
        long validityMillis = XmlTypeConverter.toMillis(validityChangeTimestamp);
        if (validityMillis >= lowerBound && validityMillis <= upperBound) {
            return;
        }
        AssertJUnit.fail("Expected validityChangeTimestamp to be between " + lowerBound + " and " + upperBound + ", but it was " + validityMillis);
    }
}
