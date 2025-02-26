/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.schema.internals.InternalsConfig.isShadowCachingFullByDefault;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.expression.evaluator.GenerateExpressionEvaluator;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUserTemplate extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/object-template");

    private static final File ROLE_RASTAMAN_FILE = new File(TEST_DIR, "role-rastaman.xml");
    private static final String ROLE_RASTAMAN_OID = "81ac6b8c-225c-11e6-ab0f-87a169c85cca";

    private static final File USER_TEMPLATE_MAROONED_FILE = new File(TEST_DIR, "user-template-marooned.xml");
    private static final String USER_TEMPLATE_MAROONED_OID = "766215e8-5f1e-11e6-94bb-c3b21af53235";

    private static final File USER_TEMPLATE_USELESS_FILE = new File(TEST_DIR, "user-template-useless.xml");
    private static final String USER_TEMPLATE_USELESS_OID = "29b2936a-d1f6-4942-8e44-9ba44fc27423";

    private static final TestObject<?> USER_TEMPLATE_MID_5892 = TestObject.file(
            TEST_DIR, "user-template-mid-5892.xml", "064993c0-34b4-4440-9331-e909fc923504");
    private static final TestObject<?> USER_TEMPLATE_MID_6045 = TestObject.file(
            TEST_DIR, "user-template-mid-6045.xml", "f3dbd582-11dc-473f-8b51-a30be5cbd5ce");

    private static final String ACCOUNT_STAN_USERNAME = "stan";
    private static final String ACCOUNT_STAN_FULLNAME = "Stan the Salesman";

    private static final String SUBTYPE_MAROONED = "marooned";
    private static final String SUBTYPE_USELESS = "useless";
    private static final String SUBTYPE_MID_5892 = "mid-5892";
    private static final String SUBTYPE_MID_6045 = "mid-6045";

    private static final int NUMBER_OF_IMPORTED_ROLES = 5;

    private static final String CANNIBAL_LEMONHEAD_USERNAME = "lemonhead";
    private static final String CANNIBAL_REDSKULL_USERNAME = "redskull";
    private static final String CANNIBAL_SHARPTOOTH_USERNAME = "sharptooth";
    private static final String CANNIBAL_ORANGESKIN_USERNAME = "orangeskin";
    private static final String CANNIBAL_CHERRYBRAIN_USERNAME = "cherrybrain";
    private static final String CANNIBAL_PINEAPPLENOSE_USERNAME = "pineapplenose";
    private static final String CANNIBAL_POTATOLEG_USERNAME = "potatoleg";

    private static final long DAY_MILLIS = 24 * 60 * 60 * 1000L;

    private static String jackEmployeeNumber;

    private XMLGregorianCalendar funeralTimestamp;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        repoAddObjectFromFile(ROLE_RASTAMAN_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTOMATIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTOCRATIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTODIDACTIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_AUTOGRAPHIC_FILE, initResult);

        repoAddObjectFromFile(USER_TEMPLATE_MAROONED_FILE, initResult);
        repoAddObjectFromFile(USER_TEMPLATE_USELESS_FILE, initResult);
        repoAdd(USER_TEMPLATE_MID_5892, initResult);
        repoAdd(USER_TEMPLATE_MID_6045, initResult);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_COMPLEX_OID, initResult);
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, SUBTYPE_MAROONED, USER_TEMPLATE_MAROONED_OID, initResult);
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, SUBTYPE_USELESS, USER_TEMPLATE_USELESS_OID, initResult);
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, SUBTYPE_MID_5892, USER_TEMPLATE_MID_5892.oid, initResult);
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, SUBTYPE_MID_6045, USER_TEMPLATE_MID_6045.oid, initResult);

        addObject(CommonTasks.TASK_TRIGGER_SCANNER_ON_DEMAND, initTask, initResult);
    }

    @Override
    protected ConflictResolutionActionType getDefaultConflictResolutionAction() {
        // This test can incur harmless conflicts (e.g. when trigger scanner touches an object
        // that is being recomputed at the same time).
        return ConflictResolutionActionType.NONE;
    }

    protected int getNumberOfRoles() {
        return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        PrismObject<SystemConfigurationType> systemConfiguration = modelService.getObject(
                SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("System config", systemConfiguration);
        assertNotNull("no system config", systemConfiguration);
        List<ObjectPolicyConfigurationType> defaultObjectPolicyConfiguration = systemConfiguration.asObjectable().getDefaultObjectPolicyConfiguration();
        assertNotNull("No object policy", defaultObjectPolicyConfiguration);
        assertEquals("Wrong object policy size", 7, defaultObjectPolicyConfiguration.size());       // last two are conflict resolution rules
        assertObjectTemplate(defaultObjectPolicyConfiguration, UserType.COMPLEX_TYPE, null, USER_TEMPLATE_COMPLEX_OID);
        assertObjectTemplate(defaultObjectPolicyConfiguration, UserType.COMPLEX_TYPE, SUBTYPE_MAROONED, USER_TEMPLATE_MAROONED_OID);
        assertObjectTemplate(defaultObjectPolicyConfiguration, UserType.COMPLEX_TYPE, SUBTYPE_USELESS, USER_TEMPLATE_USELESS_OID);
        assertObjectTemplate(defaultObjectPolicyConfiguration, UserType.COMPLEX_TYPE, SUBTYPE_MID_5892, USER_TEMPLATE_MID_5892.oid);
        assertObjectTemplate(defaultObjectPolicyConfiguration, UserType.COMPLEX_TYPE, SUBTYPE_MID_6045, USER_TEMPLATE_MID_6045.oid);

        assertRoles(getNumberOfRoles());
    }

    @SuppressWarnings("SameParameterValue")
    private void assertObjectTemplate(List<ObjectPolicyConfigurationType> defaultObjectPolicyConfigurations,
            QName objectType, String subtype, String userTemplateOid) {
        for (ObjectPolicyConfigurationType objectPolicyConfiguration : defaultObjectPolicyConfigurations) {
            if (MiscUtil.equals(objectPolicyConfiguration.getType(), objectType) &&
                    MiscUtil.equals(objectPolicyConfiguration.getSubtype(), subtype) &&
                    MiscUtil.equals(objectPolicyConfiguration.getObjectTemplateRef().getOid(), userTemplateOid)) {
                return;
            }
        }
        AssertJUnit.fail("Object template for " + objectType + ":" + subtype + "=" + userTemplateOid + " not found");
    }

    @Test
    public void test100ModifyUserGivenName() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_GIVEN_NAME, new PolyString("Jackie"));
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        assertUserJack(userJack, "Jackie Sparrow", "Jackie", "Sparrow");
        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");

        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userJack);
        assertAssignments(userJack, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        PrismAsserts.assertPropertyValue(userJack, UserType.F_ADDITIONAL_NAME, PolyString.fromOrig(USER_JACK_ADDITIONAL_NAME));

        // original value of 0 should be gone now, because the corresponding item in user template is marked as non-tolerant
        PrismAsserts.assertPropertyValue(userJack.findContainer(UserType.F_EXTENSION), PIRACY_BAD_LUCK, 123L, 456L);

        // timezone mapping is normal-strength. The source (locality) has not changed.
        // The mapping should not be activated (MID-3040)
        PrismAsserts.assertNoItem(userJack, UserType.F_TIMEZONE);
    }

    @Test
    public void test101ModifyUserEmployeeTypePirate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_SUBTYPE, "PIRATE");
        // Make sure that the user has no employeeNumber so it will be generated by userTemplate
        userDelta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER);
        deltas.add(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        // @formatter:off
        assertUserAfter(userJack)
                .assertDescription("Where's the rum?")
                .assignments()
                    .assertAssignments(2)
                    .by().roleOid(ROLE_PIRATE_OID).find()
                        .assertOriginMappingName("assignment-from-subtype")
                    .end()
                    .by().accountOn(RESOURCE_DUMMY_BLUE_OID).find().end()
                .end()
                .assertLiveLinks(2)
                .assertCostCenter("G001");
        // @formatter:on

        UserType userJackBean = userJack.asObjectable();
        jackEmployeeNumber = userJackBean.getEmployeeNumber();
        assertEquals("Unexpected length of employeeNumber, maybe it was not generated?",
                GenerateExpressionEvaluator.DEFAULT_LENGTH, jackEmployeeNumber.length());
    }

    /**
     * Switch employeeType from PIRATE to BUCCANEER. This makes one condition to go false and the other to go
     * true. For the same role assignment value.
     *
     */
    @Test
    public void test102ModifyUserEmployeeTypeBuccaneer() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_SUBTYPE, "BUCCANEER");
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        // @formatter:off
        assertUserAfter(userJack)
                .assertDescription("Where's the rum?")
                .assignments()
                    .assertAssignments(2)
                    .by().roleOid(ROLE_PIRATE_OID).find()
                        // the value was already there; so the identifier should remain intact
                        .assertOriginMappingName("assignment-from-subtype-buccaneer")
                    .end()
                    .by().accountOn(RESOURCE_DUMMY_BLUE_OID).find().end()
                .end()
                .assertLiveLinks(2)
                .assertCostCenter("B666");
        // @formatter:on

        assertEquals("Employee number has changed", jackEmployeeNumber, userJack.asObjectable().getEmployeeNumber());
    }

    @Test
    public void test103ModifyUserEmployeeTypeBartender() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_SUBTYPE, "BARTENDER");
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "G001", userJackType.getCostCenter());
        assertEquals("Employee number has changed", jackEmployeeNumber, userJackType.getEmployeeNumber());
    }

    /**
     * Cost center has two mappings. Strong mapping should not be applied here as the condition is false.
     * The weak mapping should be overridden by the change we try here.
     */
    @Test
    public void test104ModifyUserCostCenter() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_COST_CENTER, "X000");
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Employee number has changed", jackEmployeeNumber, userJackType.getEmployeeNumber());
    }

    @Test
    public void test105ModifyUserTelephoneNumber() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, "1234");
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1234", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
    }

    @Test
    public void test106ModifyUserRemoveTelephoneNumber() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_TELEPHONE_NUMBER);
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertNull("Unexpected telephone number: " + userJackType.getTelephoneNumber(), userJackType.getTelephoneNumber());
        assertEquals("Wrong Title", PrismTestUtil.createPolyStringType("Happy Pirate"), userJackType.getTitle());
    }

    @Test
    public void test107ModifyUserSetTelephoneNumber() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, task, result, "1 222 3456789");

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
    }

    /**
     * Reconcile the user. Check that nothing really changes.
     * MID-3040
     */
    @Test
    public void test120ReconcileUser() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());

        // timezone mapping is normal-strength. This is reconciliation.
        // The mapping should not be activated (MID-3040)
        PrismAsserts.assertNoItem(userJack, UserType.F_TIMEZONE);
    }

    /**
     * MID-3040
     */
    @Test
    public void test121ModifyUserReplaceLocality() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig("Tortuga"));

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismAsserts.assertEqualsPolyString("Wrong locality", "Tortuga", userJackType.getLocality());
        assertEquals("Wrong timezone", "High Seas/Tortuga", userJackType.getTimezone());

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
    }

    @Test
    public void test140AssignDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedAccount(userJack, RESOURCE_DUMMY_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 2);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 2, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
        IntegrationTestTools.assertExtensionProperty(userJack, PIRACY_COLORS, "none");
    }

    @Test
    public void test149UnAssignDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignments(userJack, 1);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
        IntegrationTestTools.assertNoExtensionProperty(userJack, PIRACY_COLORS);
    }

    @Test
    public void test150ModifyJackOrganizationalUnitRum() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PolyString.fromOrig("F0004"));

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        // @formatter:off
        assertUserAfter(userJack)
                .assertDescription("Where's the rum?")
                .assignments()
                    .assertAssignments(2)
                    .by().accountOn(RESOURCE_DUMMY_BLUE_OID).find().end()
                    .by().orgOid(ORG_MINISTRY_OF_RUM_OID).find()
                        .assertOriginMappingName("Org mapping")
                    .end()
                .end()
                .assertLiveLinks(1)
                .assertCostCenter("X000")
                .assertTelephoneNumber("1 222 3456789")
                .assertEmployeeNumber(jackEmployeeNumber)
                .assertNoTitle()
                .assertParentOrgRefs(ORG_MINISTRY_OF_RUM_OID);
        // @formatter:on
    }

    @Test
    public void test151ModifyJackOrganizationalUnitOffense() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PolyString.fromOrig("F0003"));

        // THEN
        // @formatter:off
        assertUserAfter(USER_JACK_OID)
                .assertDescription("Where's the rum?")
                .assignments()
                    .assertAssignments(2)
                    .by().accountOn(RESOURCE_DUMMY_BLUE_OID).find().end()
                    .by().orgOid(ORG_MINISTRY_OF_OFFENSE_OID).find()
                        .assertOriginMappingName("Org mapping")
                    .end()
                .end()
                .assertLiveLinks(1)
                .assertCostCenter("X000")
                .assertTelephoneNumber("1 222 3456789")
                .assertEmployeeNumber(jackEmployeeNumber)
                .assertNoTitle()
                .assertParentOrgRefs(ORG_MINISTRY_OF_OFFENSE_OID);
        // @formatter:on
    }

    @Test
    public void test152ModifyJackOrganizationalUnitAddRum() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PolyString.fromOrig("F0004"));

        // THEN
        // @formatter:off
        assertUserAfter(USER_JACK_OID)
                .assertDescription("Where's the rum?")
                .assignments()
                    .assertAssignments(3)
                    .by().accountOn(RESOURCE_DUMMY_BLUE_OID).find().end()
                    .by().orgOid(ORG_MINISTRY_OF_RUM_OID).find()
                        .assertOriginMappingName("Org mapping")
                    .end()
                    .by().orgOid(ORG_MINISTRY_OF_OFFENSE_OID).find()
                        .assertOriginMappingName("Org mapping")
                    .end()
                .end()
                .assertLiveLinks(1)
                .assertCostCenter("X000")
                .assertTelephoneNumber("1 222 3456789")
                .assertEmployeeNumber(jackEmployeeNumber)
                .assertNoTitle()
                .assertParentOrgRefs(ORG_MINISTRY_OF_RUM_OID, ORG_MINISTRY_OF_OFFENSE_OID);
        // @formatter:on
    }

    @Test
    public void test153ModifyJackOrganizationalUnitDeleteOffense() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserDelete(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PolyString.fromOrig("F0003"));

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertAssignments(userJack, 2);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
    }

    /**
     * Creates org on demand.
     */
    @Test
    public void test155ModifyJackOrganizationalUnitFD001() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PolyString.fromOrig("FD001"));

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);

        assertOnDemandOrgAssigned("FD001", userJack);

        assertAssignments(userJack, 3);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
    }

    /**
     * Reconcile user Jack, see that everything is OK.
     */
    @Test
    public void test156ReconcileJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);
        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);

        assertOnDemandOrgAssigned("FD001", userJack);

        assertAssignments(userJack, 3);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
    }

    /**
     * Creates two orgs on demand.
     */
    @Test
    public void test157ModifyJackOrganizationalUnitFD0023() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserAdd(
                USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                PolyString.fromOrig("FD002"), PolyString.fromOrig("FD003"));

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);

        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);

        assertOnDemandOrgAssigned("FD001", userJack);
        assertOnDemandOrgAssigned("FD002", userJack);
        assertOnDemandOrgAssigned("FD003", userJack);

        assertAssignments(userJack, 5);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
    }

    @Test
    public void test159ModifyJackDeleteOrganizationalUnitFD002() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserDelete(
                USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                PolyString.fromOrig("FD002"));

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertNotAssignedRole(userJack, ROLE_PIRATE_OID);

        assertAssignedOrg(userJack, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(userJack, ORG_MINISTRY_OF_RUM_OID);

        assertOnDemandOrgAssigned("FD001", userJack);
        assertOnDemandOrgAssigned("FD003", userJack);

        assertAssignments(userJack, 4);

        assertOnDemandOrgExists("FD002");

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userJackType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "X000", userJackType.getCostCenter());
        assertEquals("Wrong employee number", jackEmployeeNumber, userJackType.getEmployeeNumber());
        assertEquals("Wrong telephone number", "1 222 3456789", userJackType.getTelephoneNumber());
        assertNull("Unexpected title: " + userJackType.getTitle(), userJackType.getTitle());
    }

    @Test
    public void test160ModifyUserGivenNameAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_GIVEN_NAME, new PolyString("JACKIE"));
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        PrismAsserts.assertPropertyValue(userJack.findContainer(UserType.F_EXTENSION), PIRACY_BAD_LUCK, 123L);

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
        ObjectDeltaOperation<?> objectDeltaOperation =
                dummyAuditService.getExecutionDelta(0, ChangeType.MODIFY, UserType.class);
        assertEquals("unexpected number of modifications in audited delta",
                10, objectDeltaOperation.getObjectDelta().getModifications().size()); // givenName + badLuck + modifyTimestamp
        PropertyDelta<?> badLuckDelta = objectDeltaOperation.getObjectDelta().findPropertyDelta(
                ItemPath.create(UserType.F_EXTENSION, PIRACY_BAD_LUCK));
        assertNotNull("badLuck delta was not found", badLuckDelta);
        List<? extends PrismValue> oldValues = (List<? extends PrismValue>) badLuckDelta.getEstimatedOldValues();
        assertNotNull("badLuck delta has null estimatedOldValues field", oldValues);
        ItemFactory factory = prismContext.itemFactory();

        PrismAsserts.assertEqualsCollectionUnordered("badLuck delta has wrong estimatedOldValues",
                oldValues.stream().map(p -> p.getRealValue()).toList(), 123L, 456L);
    }

    @Test
    public void test162ModifyUserGivenNameAgainPhantomChange() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = modelService.getObject(
                UserType.class, USER_JACK_OID, null, task, result);
        display("User before", userBefore);

        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(
                        UserType.class, USER_JACK_OID, UserType.F_GIVEN_NAME, new PolyString("JACKIE")); // this is a phantom change
        deltas.add(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack.findContainer(UserType.F_EXTENSION), PIRACY_BAD_LUCK, 123L);

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);             // operation is idempotent
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test165ModifyUserGivenNameAgainAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User before", userBefore);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                USER_JACK_OID, UserType.F_GIVEN_NAME, new PolyString("jackie"));
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        // all the values should be gone now, because the corresponding item in user template is marked as non-tolerant
        PrismAsserts.assertNoItem(userJack, ItemPath.create(UserType.F_EXTENSION, PIRACY_BAD_LUCK));
    }

    private PrismObject<OrgType> assertOnDemandOrgExists(String orgName)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<OrgType> org = findObjectByName(OrgType.class, orgName);
        assertNotNull("The org " + orgName + " is missing!", org);
        display("Org " + orgName, org);
        PrismAsserts.assertPropertyValue(org, OrgType.F_NAME, PolyString.fromOrig(orgName));
        return org;
    }

    private void assertOnDemandOrgAssigned(String orgName, PrismObject<UserType> user)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<OrgType> org = assertOnDemandOrgExists(orgName);
        PrismAsserts.assertPropertyValue(org, OrgType.F_DESCRIPTION, "Created on demand from user " + user.asObjectable().getName());
        assertAssignedOrg(user, org.getOid());
        assertHasOrg(user, org.getOid());
    }

    /**
     * Setting employee type to THIEF is just one part of the condition to assign
     * the Thief role. The role should not be assigned now.
     */
    @Test
    public void test170ModifyUserGuybrushEmployeeTypeThief() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_SUBTYPE, task, result, "THIEF");

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        assertAssignedNoRole(userAfter);
    }

    /**
     * Setting honorificPrefix satisfies the condition to assign
     * the Thief role.
     */
    @Test
    public void test172ModifyUserGuybrushHonorificPrefix() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

        // WHEN
        when();
        modifyUserReplace(
                USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, task, result, PolyString.fromOrig("Thf."));

        // THEN
        then();
        assertSuccess(result);

        // THEN
        assertUserAfter(USER_GUYBRUSH_OID)
                .assignments()
                .by().roleOid(ROLE_THIEF_OID).find()
                .assertOriginMappingName("assignment-from-subtype-thief");
    }

    /**
     * Removing honorificPrefix should make the condition false again, which should cause
     * that Thief role is unassigned.
     */
    @Test
    public void test174ModifyUserGuybrushHonorificPrefixNone() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedRole(userBefore, ROLE_THIEF_OID);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        assertAssignedNoRole(userAfter);
    }

    /**
     * Setting employee type to marooned. This should cause switch to different user template.
     * Almost same as test185ModifyUserGuybrushSubtypeMarooned.
     * employeeType is deprecated, but it should still work for compatibility.
     */
    @Test
    public void test180ModifyUserGuybrushEmployeeTypeMarooned() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertEquals("Wrong costCenter", "G001", userBefore.asObjectable().getCostCenter());
        assertAssignedNoRole(userBefore);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_SUBTYPE, task, result, SUBTYPE_MAROONED);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        assertEquals("Wrong costCenter", "NOCOST", userAfter.asObjectable().getCostCenter());

        assertAssignedNoRole(userAfter);
    }

    /**
     * employeeType is deprecated, but it should still work for compatibility.
     */
    @Test
    public void test184ModifyUserGuybrushEmployeeTypeNone() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_SUBTYPE, task, result);

        // THEN
        then();
        assertSuccess(result);

        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_COST_CENTER, task, result, "S321");

        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        assertEquals("Wrong costCenter", "S321", userAfter.asObjectable().getCostCenter());

        assertAssignedNoRole(userAfter);
    }

    /**
     * Setting subtype to marooned. This should cause switch to different user template.
     */
    @Test
    public void test185ModifyUserGuybrushSubtypeMarooned() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_SUBTYPE, task, result, SUBTYPE_MAROONED);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        assertEquals("Wrong costCenter", "NOCOST", userAfter.asObjectable().getCostCenter());

        assertAssignedNoRole(userAfter);
    }

    @Test
    public void test189ModifyUserGuybrushSubtypeNone() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_SUBTYPE, task, result);

        // THEN
        then();
        assertSuccess(result);

        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_COST_CENTER, task, result, "S321");

        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        assertEquals("Wrong costCenter", "S321", userAfter.asObjectable().getCostCenter());

        assertAssignedNoRole(userAfter);
    }

    /**
     * Assignment mapping with domain. Control: nothing should happen.
     * MID-3692
     */
    @Test
    public void test190ModifyUserGuybrushOrganizationWhateveric() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);
        assertAssignments(userBefore, 1);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result, PolyString.fromOrig("Whateveric"));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION, PolyString.fromOrig("Whateveric"));

        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);
    }

    /**
     * MID-3692
     */
    @Test
    public void test191ModifyUserGuybrushOrganizationAutomatic() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

        // WHEN
        when();
        modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result, PolyString.fromOrig("AUTO-matic"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assertOrganizations("Whateveric", "AUTO-matic")
                .assignments()
                .assertAssignments(2)
                .by().roleOid(ROLE_AUTOMATIC_OID).find()
                .assertOriginMappingName("automappic");

        assertRoles(getNumberOfRoles());
    }

    /**
     * MID-3692
     */
    @Test
    public void test192ModifyUserGuybrushOrganizationAddMixed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);

        // WHEN
        when();
        modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
                PolyString.fromOrig("DEMO-cratic"),
                PolyString.fromOrig("AUTO-cratic"),
                PolyString.fromOrig("plutocratic"),
                PolyString.fromOrig("AUTO-didactic"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("Whateveric"),
                PolyString.fromOrig("AUTO-matic"),
                PolyString.fromOrig("DEMO-cratic"),
                PolyString.fromOrig("AUTO-cratic"),
                PolyString.fromOrig("plutocratic"),
                PolyString.fromOrig("AUTO-didactic"));

        assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTODIDACTIC_OID);
        assertAssignments(userAfter, 4);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * MID-3692
     */
    @Test
    public void test193ModifyUserGuybrushOrganizationAddOutOfDomain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 4);

        // WHEN
        when();
        modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
                PolyString.fromOrig("meritocratic"),
                PolyString.fromOrig("piratocratic"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("Whateveric"),
                PolyString.fromOrig("AUTO-matic"),
                PolyString.fromOrig("DEMO-cratic"),
                PolyString.fromOrig("AUTO-cratic"),
                PolyString.fromOrig("plutocratic"),
                PolyString.fromOrig("AUTO-didactic"),
                PolyString.fromOrig("meritocratic"),
                PolyString.fromOrig("piratocratic"));

        assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTODIDACTIC_OID);
        assertAssignments(userAfter, 4);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * MID-3692
     */
    @Test
    public void test194ModifyUserGuybrushOrganizationDeleteMixed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 4);

        // WHEN
        when();
        modifyUserDelete(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
                PolyString.fromOrig("AUTO-matic"),
                PolyString.fromOrig("plutocratic"),
                PolyString.fromOrig("meritocratic"),
                PolyString.fromOrig("AUTO-didactic"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("Whateveric"),
                PolyString.fromOrig("DEMO-cratic"),
                PolyString.fromOrig("AUTO-cratic"),
                PolyString.fromOrig("piratocratic"));

        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignments(userAfter, 2);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * MID-3692
     */
    @Test
    public void test195ModifyUserGuybrushOrganizationDeleteOutOfDomain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);

        // WHEN
        when();
        modifyUserDelete(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
                PolyString.fromOrig("piratocratic"),
                PolyString.fromOrig("DEMO-cratic"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("Whateveric"),
                PolyString.fromOrig("AUTO-cratic"));

        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignments(userAfter, 2);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * Make sure that the manually assigned roles will not mess with the mapping.
     * MID-3692
     */
    @Test
    public void test196GuybrushAssignCaptain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);

        // WHEN
        when();
        assignRole(USER_GUYBRUSH_OID, ROLE_CAPTAIN_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("Whateveric"),
                PolyString.fromOrig("AUTO-cratic"));

        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
        assertAssignments(userAfter, 3);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * Make sure that a role automatically assigned by a different mapping will not mess with this mapping.
     * MID-3692
     */
    @Test
    public void test197ModifyGuybrushEmployeeTypePirate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_SUBTYPE, task, result, "PIRATE");

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("Whateveric"),
                PolyString.fromOrig("AUTO-cratic"));

        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertAssignments(userAfter, 4);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * Make sure that changes in this mapping will not influence other assigned roles.
     * MID-3692
     */
    @Test
    public void test198AModifyUserGuybrushOrganizationAddInDomain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 4);

        // WHEN
        when();
        modifyUserAdd(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
                PolyString.fromOrig("AUTO-graphic"),
                PolyString.fromOrig("AUTO-matic"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("Whateveric"),
                PolyString.fromOrig("AUTO-cratic"),
                PolyString.fromOrig("AUTO-graphic"),
                PolyString.fromOrig("AUTO-matic"));

        assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOCRATIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertAssignments(userAfter, 6);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * Make sure that changes in this mapping will not influence other assigned roles.
     * MID-3692
     */
    @Test
    public void test198BModifyUserGuybrushOrganizationDeleteMixed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 6);

        // WHEN
        when();
        modifyUserDelete(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result,
                PolyString.fromOrig("AUTO-cratic"),
                PolyString.fromOrig("Whateveric"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("AUTO-graphic"),
                PolyString.fromOrig("AUTO-matic"));

        assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertAssignments(userAfter, 5);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * MID-3692
     */
    @Test
    public void test199AGuyBrushModifyEmployeeTypeWannabe() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 5);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_SUBTYPE, task, result, "wannabe");

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("AUTO-graphic"),
                PolyString.fromOrig("AUTO-matic"));

        assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
        assertAssignments(userAfter, 4);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * MID-3692
     */
    @Test
    public void test199BGuyBrushUnassignCaptain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 4);

        // WHEN
        when();
        unassignRole(USER_GUYBRUSH_OID, ROLE_CAPTAIN_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig("AUTO-graphic"),
                PolyString.fromOrig("AUTO-matic"));

        assertAssignedRole(userAfter, ROLE_AUTOMATIC_OID);
        assertAssignedRole(userAfter, ROLE_AUTOGRAPHIC_OID);
        assertAssignments(userAfter, 3);

        // Make sure nothing was created on demand
        assertRoles(getNumberOfRoles());
    }

    /**
     * MID-3692, MID-3700
     */
    @Test
    public void test199CModifyUserGuybrushOrganizationCleanup() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_ORGANIZATION, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_GUYBRUSH_OID, null, task, result);
        display("User after", userAfter);

        PrismAsserts.assertNoItem(userAfter, UserType.F_ORGANIZATION);

        assertAssignedNoRole(userAfter);

        assertRoles(getNumberOfRoles());
    }

    @Test
    public void test200AddUserRapp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_RAPP_FILE);
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber, maybe it was generated and should not be?",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "G001", userAfterType.getCostCenter());
    }

    @Test
    public void test201AddUserLargo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        // This simulates IMPORT to trigger the channel-limited mapping
        task.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_IMPORT));

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_LARGO_FILE);
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_LARGO_OID, null, task, result);
        display("Largo after", userAfter);
        assertUser(userAfter, USER_LARGO_OID, "largo", "Largo LaGrande", "Largo", "LaGrande");

        // locality is null; the description comes from inbound mapping on dummy resource
        // PrismAsserts.assertPropertyValue(userAfter, UserType.F_DESCRIPTION, "Came from null");

        // TODO TEMPORARILY allowing value of "Imported user", because the inbound mapping is not applied because of
        //  the "locality" attribute is null (Skipping inbound for {...}location in Discr(RSD(account (default)
        //  @10000000-0000-0000-0000-000000000004)): Not a full shadow and account a priori delta exists, but
        //  doesn't have change for processed property.
        //
        // Either we fix this or recommend setting volatility=unpredictable for such situations.
        PrismAsserts.assertPropertyValue(
                userAfter, UserType.F_DESCRIPTION, isShadowCachingFullByDefault() ? "Came from null" : "Imported user");
        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertEquals("Unexpected number of accountRefs", 2, userAfterType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected length  of employeeNumber, maybe it was not generated?",
                GenerateExpressionEvaluator.DEFAULT_LENGTH, userAfterType.getEmployeeNumber().length());
    }

    @Test
    public void test202AddUserMonkey() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_THREE_HEADED_MONKEY_FILE);
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userAfter =
                modelService.getObject(UserType.class, USER_THREE_HEADED_MONKEY_OID, null, task, result);
        display("User after", userAfter);
//        assertUser(userAfter, USER_THREE_HEADED_MONKEY_OID, "monkey", " Monkey", null, "Monkey");
        assertUser(userAfter, USER_THREE_HEADED_MONKEY_OID, "monkey", "Three-Headed Monkey", null, "Monkey");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userAfterType.getLinkRef().size());

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected length  of employeeNumber, maybe it was not generated?",
                GenerateExpressionEvaluator.DEFAULT_LENGTH, userAfterType.getEmployeeNumber().length());
    }

    /**
     * MID-3186
     */
    @Test
    public void test204AddUserHerman() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_HERMAN_FILE);
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_HERMAN_OID, null, task, result);
        assertUser(userAfter, USER_HERMAN_OID, USER_HERMAN_USERNAME, USER_HERMAN_FULL_NAME, "Herman", "Toothrot");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_TIMEZONE, "High Seas/Monkey Island");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong costCenter", "G001", userAfterType.getCostCenter());
    }

    @Test
    public void test220AssignRoleSailorToUserRapp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        assignRole(USER_RAPP_OID, ROLE_SAILOR_OID, task, result);

        // THEN
        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_SAILOR_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 2);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-TITANIC", userAfterType.getCostCenter());
    }

    /**
     * MID-3028
     */
    @Test
    public void test229UnassignRoleSailorFromUserRapp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        unassignRole(USER_RAPP_OID, ROLE_SAILOR_OID, task, result);

        // THEN
        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "G001", userAfterType.getCostCenter());
    }

    /**
     * Role Captains has focus mapping for the same costCenter as is given
     * by the user template.
     */
    @Test
    public void test230AssignRoleCaptainToUserRapp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        assignRole(USER_RAPP_OID, ROLE_CAPTAIN_OID, task, result);

        // THEN
        PrismObject<UserType> userAfter = modelService.getObject(UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "G001", userAfterType.getCostCenter());
    }

    /**
     * Object template mapping for cost center is weak, role mapping is normal.
     * Direct modification should override both.
     */
    @Test
    public void test232ModifyUserRappCostCenter() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserReplace(USER_RAPP_OID, UserType.F_COST_CENTER, task, result, "CC-RAPP");

        // THEN
        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_CAPTAIN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
    }

    /**
     * Role Captains has focus mapping for the same costCenter as is given
     * by the user template.
     * MID-3028
     */
    @Test
    public void test239UnassignRoleCaptainFromUserRapp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        unassignRole(USER_RAPP_OID, ROLE_CAPTAIN_OID, task, result);

        // THEN
        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User after", userAfter);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
    }

    @Test
    public void test240ModifyUserRappLocalityScabb() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User before", userBefore);

        assertEquals("Wrong timezone", "High Seas/null", userBefore.asObjectable().getTimezone());
        assertNull("Wrong locale", userBefore.asObjectable().getLocale());

        // WHEN
        modifyUserReplace(
                USER_RAPP_OID, UserType.F_LOCALITY,
                task, result, PolyString.fromOrig("Scabb Island"));

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User after", userAfter);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        assertEquals("Wrong timezone", "High Seas/Scabb Island", userAfterType.getTimezone());
        assertEquals("Wrong locale", "SC", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
    }

    /**
     * Role Rastaman has focus mapping for the same timezone as is given
     * by the user template. This mapping is normal strength. Even though
     * it is evaluated after the template the mapping, role assignment is an
     * explicit delta and the mapping should be applied.
     * MID-3040
     */
    @Test
    public void test242AssignRoleRastamanToUserRapp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        assignRole(USER_RAPP_OID, ROLE_RASTAMAN_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        assertEquals("Wrong timezone", "Caribbean/Whatever", userAfterType.getTimezone());
        assertEquals("Wrong locale", "WE", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
    }

    /**
     * Role Rastaman has focus mapping for the same timezone as is given
     * by the user template. This mapping is normal strength. It is evaluated
     * after the template the mapping, so it should not be applied because
     * there is already a-priori delta from the template.
     * MID-3040
     */
    @Test
    public void test244ModifyUserRappLocalityCoffin() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User before", userBefore);

        // WHEN
        modifyUserReplace(
                USER_RAPP_OID, UserType.F_LOCALITY,
                task, result, PolyString.fromOrig("Coffin"));

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User after", userAfter);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        // There is a mapping "locality->timezone" in the object template (plus: High Seas/Coffin)
        // but also a mapping "()->timezone" in Rastaman role (zero: Caribbean/Whatever)
        // Because the normal mapping is source-less, it is evaluated and its result is taken into account.
        assertEquals("Wrong timezone", "Caribbean/Whatever", userAfterType.getTimezone());
        assertEquals("Wrong locale", "WE", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
    }

    /**
     * Similar to test244, but also use reconcile option.
     * MID-3040
     */
    @Test
    public void test245ModifyUserRappLocalityUnderReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User before", userBefore);

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(
                USER_RAPP_OID, UserType.F_LOCALITY, PolyString.fromOrig("Six feet under"));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        ModelExecuteOptions options = executeOptions().reconcile();

        // WHEN
        modelService.executeChanges(deltas, options, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User after", userAfter);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        // There is a mapping "locality->timezone" in the object template (plus: High Seas/Six feet under)
        // but also a mapping "()->timezone" in Rastaman role (zero: Caribbean/Whatever)
        // Because the normal mapping is source-less, it is evaluated and its result is taken into account.
        assertEquals("Wrong timezone", "Caribbean/Whatever", userAfterType.getTimezone());
        assertEquals("Wrong locale", "WE", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
    }

    /**
     * Changing timezone. timezone is a target of (normal) object template mapping and
     * (normal) role mapping. But as this is primary delta none of the mappings should
     * be applied.
     * MID-3040
     */
    @Test
    public void test246ModifyUserRappTimezoneMonkey() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User before", userBefore);

        // WHEN
        modifyUserReplace(USER_RAPP_OID, UserType.F_TIMEZONE, task, result, "Monkey Island");

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User after", userAfter);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        assertEquals("Wrong timezone", "Monkey Island", userAfterType.getTimezone());
        assertEquals("Wrong locale", "WE", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
    }

    /**
     * Changing locale. Locale is a target of (weak) object template mapping and
     * (normal) role mapping. But as this is primary delta none of the mappings should
     * be applied.
     * MID-3040
     */
    @Test
    public void test247ModifyUserRappLocaleMI() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User before", userBefore);

        // WHEN
        modifyUserReplace(USER_RAPP_OID, UserType.F_LOCALE, task, result, "MI");

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        display("User after", userAfter);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedRole(userAfter, ROLE_RASTAMAN_OID);
        assertAssignments(userAfter, 2);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        // The normal mapping from the rastaman role was applied at this point
        // This is sourceless mapping and there is no a-priori delta
        assertEquals("Wrong timezone", "Caribbean/Whatever", userAfterType.getTimezone());

        assertEquals("Wrong locale", "MI", userAfterType.getLocale());

        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
    }

    @Test
    public void test249UnassignRoleRastamanFromUserRapp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        unassignRole(USER_RAPP_OID, ROLE_RASTAMAN_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, USER_RAPP_OID, null, task, result);
        assertUser(userAfter, USER_RAPP_OID, "rapp", "Rapp Scallion", "Rapp", "Scallion");
        PrismAsserts.assertNoItem(userAfter, UserType.F_DESCRIPTION);

        assertAssignedAccount(userAfter, RESOURCE_DUMMY_BLUE_OID);
        assertAssignedNoRole(userAfter);
        assertAssignments(userAfter, 1);

        UserType userAfterType = userAfter.asObjectable();
        assertLiveLinks(userAfter, 1);

        // Role is unassigned. The mapping was authoritative, so it removed the value
        assertNull("Wrong timezone", userAfterType.getTimezone());
        assertEquals("Wrong locale", "MI", userAfterType.getLocale());
        assertEquals("Unexpected value of employeeNumber",
                "D3ADB33F", userAfterType.getEmployeeNumber());
        assertEquals("Wrong costCenter", "CC-RAPP", userAfterType.getCostCenter());
    }

    /**
     * MID-3186
     */
    @Test
    public void test300ImportStanFromEmeraldResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount dummyAccountBefore = new DummyAccount(ACCOUNT_STAN_USERNAME);
        dummyAccountBefore.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                ACCOUNT_STAN_FULLNAME);
        dummyAccountBefore.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                "Melee Island");
        dummyResourceEmerald.addAccount(dummyAccountBefore);

        PrismObject<ShadowType> shadowBefore = findAccountByUsername(ACCOUNT_STAN_USERNAME, resourceDummyEmerald);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        modelService.importFromResource(shadowBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_STAN_USERNAME);
        display("User after", userAfter);
        assertNotNull("No stan user", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PolyString.fromOrig(ACCOUNT_STAN_FULLNAME));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_LOCALITY, PolyString.fromOrig("Melee Island"));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_TIMEZONE, "High Seas/Melee Island");
    }

    /**
     * Modify stan accoutn and reimport from the emerald resource. Make sure that
     * the normal mapping for locality in the object template is properly activated (as there is
     * an delta from inbound mapping in the emerald resource).
     * MID-3186
     */
    @Test
    public void test302ModifyStanAccountAndReimport() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount dummyAccountBefore = dummyResourceEmerald.getAccountByName(ACCOUNT_STAN_USERNAME);
        dummyAccountBefore.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                "Booty Island");

        PrismObject<ShadowType> shadowBefore = findAccountByUsername(ACCOUNT_STAN_USERNAME, resourceDummyEmerald);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        modelService.importFromResource(shadowBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_STAN_USERNAME);
        display("User after", userAfter);
        assertNotNull("No stan user", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_FULL_NAME, PolyString.fromOrig(ACCOUNT_STAN_FULLNAME));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_LOCALITY, PolyString.fromOrig("Booty Island"));
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_TIMEZONE, "High Seas/Booty Island");
    }

    /**
     * There is no funeralTimestamp. But there is user template mapping ("time bomb")
     * that is using funeralTimestamp as timeFrom. Make sure that it will not die with
     * null value.
     * MID-5603
     */
    @Test
    public void test800NullTimeFrom() throws Exception {
        OperationResult result = getTestOperationResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assertAdditionalName(USER_JACK_ADDITIONAL_NAME)
            .extension()
                .assertNoItem(PIRACY_TALES)
                .assertNoItem(PIRACY_LOOT)
                .end()
            .assertNoTrigger();
        // @formatter:on
    }

    /**
     * Set funeral timestamp. But the time-based mapping should not trigger yet.
     */
    @Test
    public void test802FuneralTimestamp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        funeralTimestamp = clock.currentTimeXMLGregorianCalendar();
        modifyUserReplace(USER_JACK_OID, getExtensionPath(PIRACY_FUNERAL_TIMESTAMP), task, result, funeralTimestamp);

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assertAdditionalName(USER_JACK_ADDITIONAL_NAME)
            .extension()
                .assertNoItem(PIRACY_TALES)
                .assertNoItem(PIRACY_LOOT)
                .end()
            .triggers()
                .single()
                    .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                    .assertTimestampFuture(funeralTimestamp, "P30D", 10*1000L);
        // @formatter:on
    }

    /**
     * Move the time to the future a bit. But not far enough to cause an explosion.
     */
    @Test
    public void test804PreKaboom() throws Exception {
        OperationResult result = getTestOperationResult();

        // GIVEN
        clockForward("P2D"); // total override is realTime + 2D

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assertAdditionalName(USER_JACK_ADDITIONAL_NAME)
            .triggers()
                .single()
                    .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                    .assertTimestampFuture("P30D", 5 * DAY_MILLIS);
        // @formatter:on
    }

    /**
     * Move the time to the future. See if the time-based mapping in user template is properly recomputed.
     */
    @Test
    public void test808Kaboom() throws Exception {
        OperationResult result = getTestOperationResult();

        // GIVEN
        clockForward("P30D"); // total override is realTime + 32D

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assertAdditionalName("Kaboom!")
            .extension()
                .assertNoItem(PIRACY_TALES)
                .end()
            .triggers()
                .single()
                    // Trigger for "tales bomb" mapping (see below) - it was computed as funeralTimestamp + 90D
                    // (i.e. should be approximately equal to clock + 60D - 2D, because clock = realTime + 32D)
                    .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                    .assertTimestampFuture("P60D", 5 * DAY_MILLIS);
        // @formatter:on
    }

    /**
     * Move the time to the future a bit further.
     * There is another time-sensitive mapping (tales bomb).
     * This should get it prepared, creating trigger.
     * MID-4630
     */
    @Test
    public void test810PreTalesBomb() throws Exception {
        OperationResult result = getTestOperationResult();

        // GIVEN
        clockForward("P1D"); // total override is realTime + 32D + 1D

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assertAdditionalName("Kaboom!")
            .extension()
                .assertNoItem(PIRACY_TALES)
                .assertNoItem(PIRACY_LOOT)
                .end()
            .triggers()
                .single()
                    // Trigger for "tales bomb" mapping - it was computed as funeralTimestamp + 90D
                    // (i.e. should be approximately equal to clock + 60D - 3D, because clock = realTime + 2D + 30D + 1D)
                    .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                    .assertTimestampFuture("P60D", 5 * DAY_MILLIS);
        // @formatter:on
    }

    /**
     * Move the time 90D the future. Tales bomb should explode.
     * MID-4630
     */
    @Test
    public void test812TalesBoom() throws Exception {
        OperationResult result = getTestOperationResult();

        // GIVEN
        clockForward("P90D");

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assertAdditionalName("Kaboom!")
            .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .end()
            .triggers()
                .assertNone();
        // @formatter:on
    }

    /**
     * Recompute. Make sure situation is stable.
     * MID-4630
     */
    @Test
    public void test813TalesBoomRecompute() throws Exception {
        OperationResult result = getTestOperationResult();
        // GIVEN

        // WHEN
        recomputeUser(USER_JACK_OID);

        // THEN
        // @formatter:off
        assertUserAfter(USER_JACK_OID)
            .assertAdditionalName("Kaboom!")
            .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .end()
            .triggers()
                .assertNone();

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        assertUserAfter(USER_JACK_OID)
            .assertAdditionalName("Kaboom!")
            .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .end()
            .triggers()
                .assertNone();
        // @formatter:on
    }

    /**
     * Remove tales and recompute. The mapping should kick in and re-set the tales.
     * MID-4630
     */
    @Test
    public void test820TalesUnBoom() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        modifyUserReplace(USER_JACK_OID, getExtensionPath(PIRACY_TALES), task, result /* no value */);

        // The tales mapping is normal. Therefore primary delta will override it.
        assertUserBefore(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertNoItem(PIRACY_TALES)
                .end()
                .triggers()
                .assertNone();

        // WHEN
        // No delta here. The normal tales mapping will fire again.
        recomputeUser(USER_JACK_OID);

        // THEN
        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .end()
                .triggers()
                .assertNone();

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .end()
                .triggers()
                .assertNone();
    }

    /**
     * Changing description will prepare the "loot bomb" mapping.
     * But it is not yet the time to fire it. Only a trigger should be set.
     * MID-4630
     */
    @Test
    public void test830PreLootBoom() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        modifyUserReplace(USER_JACK_OID, UserType.F_DESCRIPTION, task, result, "Rum is gone");

        // The tales mapping is normal. Therefore primary delta will override it.
        assertUserBefore(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .single()
                .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .assertTimestampFuture(funeralTimestamp, "P1Y", 2 * DAY_MILLIS);

        // WHEN
        // No delta here. The normal tales mapping will fire again.
        recomputeUser(USER_JACK_OID);

        // THEN
        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .single()
                .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .assertTimestampFuture(funeralTimestamp, "P1Y", 2 * DAY_MILLIS);

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .single()
                .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .assertTimestampFuture(funeralTimestamp, "P1Y", 2 * DAY_MILLIS);

    }

    /**
     * Move the time 9M the future. Loot bomb could explode.
     * But loot property is not set yet. The condition is still false.
     * MID-4630
     */
    @Test
    public void test832LootBoomConditionFalse() throws Exception {
        OperationResult result = getTestOperationResult();

        // GIVEN
        clockForward("P9M");

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .assertNone();

        recomputeUser(USER_JACK_OID);

        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .assertNone();
    }

    /**
     * Loot bomb is over its timeFrom. Now make condition true. And it should explode.
     * MID-4630
     */
    @Test
    public void test835LootBoom() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, PolyString.fromOrig("Lootjack"));

        // THEN
        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertPropertyValuesEqual(PIRACY_LOOT, 1000000)
                .end()
                .triggers()
                .assertNone();

        recomputeUser(USER_JACK_OID);

        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertPropertyValuesEqual(PIRACY_LOOT, 1000000)
                .end()
                .triggers()
                .assertNone();
    }

    /**
     * Reset funeral timestamp. Prepare for subsequent tests.
     */
    @Test
    public void test840ResetFuneralTimestamp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        modifyUserReplace(USER_JACK_OID, getExtensionPath(PIRACY_FUNERAL_TIMESTAMP), task, result /* no value */);

        // WHEN
        runTriggerScannerOnDemand(result);

        // THEN
        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                // We have Kaboom. And no funeral timestamp. Tales mapping should be applied.
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                // All conditions are satisfied. And no funeral timestamp. Loot mapping should be applied.
                .assertPropertyValuesEqual(PIRACY_LOOT, 1000000)
                .end()
                .triggers()
                .assertNone();
    }

    /**
     * Turn off condition on loot mapping
     * MID-4630
     */
    @Test
    public void test843UnLoot() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, PolyString.fromOrig("Justjack"));

        // THEN
        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .assertNone();

        recomputeUser(USER_JACK_OID);

        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .assertNone();
    }

    /**
     * Turn no condition on loot mapping
     * MID-4630
     */
    @Test
    public void test845ReLoot() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, PolyString.fromOrig("Lootjack"));

        // THEN
        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertPropertyValuesEqual(PIRACY_LOOT, 1000000)
                .end()
                .triggers()
                .assertNone();

        recomputeUser(USER_JACK_OID);

        assertUserAfter(USER_JACK_OID)
                .assertAdditionalName("Kaboom!")
                .extension()
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                .assertPropertyValuesEqual(PIRACY_LOOT, 1000000)
                .end()
                .triggers()
                .assertNone();
    }

    /**
     * Lemonhead: no funeralDate, no description, no additional name
     * MID-4630
     */
    @Test
    public void test850AddCannibalLemonhead() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_LEMONHEAD_USERNAME, null, false), task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                .assertNoAdditionalName()
                .extension()
                // Additional name is not Kaboom!, tales are not there
                .assertNoItem(PIRACY_TALES)
                // Description does not match, condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .assertNone();
    }

    /**
     * Redskull: funeralDate=+0, no description, no additional name
     * MID-4630
     */
    @Test
    public void test852AddCanibalRedSkull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_REDSKULL_USERNAME, "", false), task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                .assertNoAdditionalName()
                .extension()
                // Additional name is not Kaboom!, tales are not there
                .assertNoItem(PIRACY_TALES)
                // Description does not match, condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .single()
                // Trigger from time bomb. There is no condition and the reference time is set.
                .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .assertTimestampFuture(funeralTimestamp, "P30D", DAY_MILLIS);
    }

    /**
     * Shaprtooth: funeralDate=-31D, no description, no additional name
     * Time bomb should explode
     * MID-4630
     */
    @Test
    public void test854AddCanibalShaprtooth() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_SHARPTOOTH_USERNAME, "-P31D", false), task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                // Time bomb exploded
                .assertAdditionalName("Kaboom!")
                .extension()
                // Time is not up for tales bomb to explode.
                .assertNoItem(PIRACY_TALES)
                // Description does not match, condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .single()
                // Trigger from tales bomb. We have Kaboomed, the time constraint has activated
                .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .assertTimestampFuture(funeralTimestamp, "P90D", DAY_MILLIS);
    }

    /**
     * Orangeskin: funeralDate=-91D, no description, no additional name
     * Time bomb should explode, tales bomb should explode
     * MID-4630
     */
    @Test
    public void test856AddCanibalOrangeskin() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_ORANGESKIN_USERNAME, "-P91D", false), task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                // Time bomb exploded
                .assertAdditionalName("Kaboom!")
                .extension()
                // Additional name is Kaboom! and time is up
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                // Description does not match, condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                // No trigger for loot bomb. Description does not match.
                .assertNone();
    }

    /**
     * Cherrybrain: funeralDate=-1Y1D, no description, no additional name
     * Time bomb should explode, tales bomb should explode, but not loot bomb
     * MID-4630
     */
    @Test
    public void test858AddCanibalCherrybrain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_CHERRYBRAIN_USERNAME, "-P1Y1D", false), task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                // Time bomb exploded
                .assertAdditionalName("Kaboom!")
                .extension()
                // Additional name is Kaboom! and time is up
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                // Time is up. But description does not match and condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                // No trigger for loot bomb. Description does not match.
                // And we are already after timeFrom anyway.
                .assertNone();
    }

    /**
     * Pineapplenose: funeralDate=-1Y1D, no description, no additional name, givenName=Lootjack
     * Time bomb should explode, tales bomb should explode
     * But not loot bomb, as the description does not match.
     * MID-4630
     */
    @Test
    public void test859AddCanibalPineapplenose() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(
                createCannibal(CANNIBAL_PINEAPPLENOSE_USERNAME, "-P1Y1D", false)
                        .asObjectable().givenName("Lootjack").asPrismObject(),
                task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                // Time bomb exploded
                .assertAdditionalName("Kaboom!")
                .extension()
                // Additional name is Kaboom! and time is up
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                // Time is up. But description does not match and condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                // No trigger for loot bomb. Description does not match.
                // And we are already after timeFrom anyway.
                .assertNone();
    }

    /**
     * Rotten Lemonhead: no funeralDate, description=gone, no additional name
     * MID-4630
     */
    @Test
    public void test860AddCannibalRottenLemonhead() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_LEMONHEAD_USERNAME, null, true), task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                .assertNoAdditionalName()
                .extension()
                // Additional name is not Kaboom!, tales are not there
                .assertNoItem(PIRACY_TALES)
                // Description does not match, condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .assertNone();
    }

    /**
     * Rotten Redskull: funeralDate=+0, description=gone, no additional name
     * MID-4630
     */
    @Test
    public void test862AddCanibalRottenRedSkull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_REDSKULL_USERNAME, "", true), task, result);

        // THEN
        assertSuccess(result);

        // @formatter:on
        assertUserAfter(userOid)
                .assertNoAdditionalName()
                .extension()
                // Additional name is not Kaboom!, tales are not there
                .assertNoItem(PIRACY_TALES)
                // Description does not match, condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .single()
                // Trigger from time bomb. There is no condition and the reference time is set.
                .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .assertTimestampFuture(funeralTimestamp, "P30D", DAY_MILLIS);
        // @formatter:off
    }

    /**
     * Rotten Shaprtooth: funeralDate=-31D, description=gone, no additional name
     * Time bomb should explode
     * MID-4630
     */
    @Test
    public void test864AddCannibalRottenSharptooth() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_SHARPTOOTH_USERNAME, "-P31D", true), task, result);

        // THEN
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(userOid)
            // Time bomb exploded
            .assertAdditionalName("Kaboom!")
            .extension()
                // Time is not up for tales bomb to explode.
                .assertNoItem(PIRACY_TALES)
                // Description does not match, condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
            .triggers()
                .assertTriggers(2)
                .by()
                    .originDescription("tales bomb")
                .find()
                    // Trigger from tales bomb. We have Kaboomed, the time constraint has activated
                    .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                    .assertTimestampFuture(funeralTimestamp, "P90D", DAY_MILLIS)
                    .end()
                .by()
                    .originDescription("loot bomb")
                .find()
                    // Trigger from loot bomb. We have Kaboomed and we have the right description, the time constraint has activated
                    .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                    .assertTimestampFuture(funeralTimestamp, "P1Y", DAY_MILLIS);
        // @formatter:on
    }

    /**
     * Rotten Orangeskin: funeralDate=-91D, description=gone, no additional name
     * Time bomb should explode, tales bomb should explode
     * MID-4630
     */
    @Test
    public void test866AddCanibalRottenOrangeskin() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_ORANGESKIN_USERNAME, "-P91D", true), task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                // Time bomb exploded
                .assertAdditionalName("Kaboom!")
                .extension()
                // Additional name is Kaboom! and time is up
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                // Description does not match, condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                .single()
                // Trigger from loot bomb. We have Kaboomed and we have the right description, the time constraint has activated
                .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .assertTimestampFuture(funeralTimestamp, "P1Y", DAY_MILLIS);
    }

    /**
     * Rotten Cherrybrain: funeralDate=-1Y1D, description=gone, no additional name
     * Time bomb should explode, tales bomb should explode
     * but not loot bomb, the condition is not satisfied
     * MID-4630
     */
    @Test
    public void test868AddCanibalRottenCherrybrain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(createCannibal(CANNIBAL_CHERRYBRAIN_USERNAME, "-P1Y1D", true), task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                // Time bomb exploded
                .assertAdditionalName("Kaboom!")
                .extension()
                // Additional name is Kaboom! and time is up
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                // Time is up. But description does not match and condition is false. No loot.
                .assertNoItem(PIRACY_LOOT)
                .end()
                .triggers()
                // No trigger for loot bomb. We are already after timeFrom.
                .assertNone();
    }

    /**
     * Rotten Pineapplenose: funeralDate=-1Y1D, no description, no additional name, givenName=Lootjack
     * Time bomb should explode, tales bomb should explode, loot bomb should explode
     * MID-4630
     */
    @Test
    public void test869AddCanibalRottenPineapplenose() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(
                createCannibal(CANNIBAL_PINEAPPLENOSE_USERNAME, "-P1Y1D", true)
                        .asObjectable().givenName("Lootjack").asPrismObject(),
                task, result);

        // THEN
        assertSuccess(result);

        assertUserAfter(userOid)
                // Time bomb exploded
                .assertAdditionalName("Kaboom!")
                .extension()
                // Additional name is Kaboom! and time is up
                .assertPropertyValuesEqual(PIRACY_TALES, "Once upon a time")
                // Time is up. Description matches and condition is true.
                .assertPropertyValuesEqual(PIRACY_LOOT, 1000000)
                .end()
                .triggers()
                // No trigger for loot bomb. Description does not match.
                // And we are already after timeFrom anyway.
                .assertNone();
    }

    /**
     * Rotten Potatoleg: no funeralDate, rotten description, no additional name, givenName=Lootjack
     * Time bomb does not explode, tales bomb does not explode, loot bomb should explode
     * MID-4630
     */
    @Test
    public void test870AddCanibalRottenPotatoleg() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        String userOid = addObject(
                createCannibal(CANNIBAL_POTATOLEG_USERNAME, null, true)
                        .asObjectable().givenName("Lootjack").asPrismObject(),
                task, result);

        // THEN
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(userOid)
            // No funeralTime, no explosion
            .assertNoAdditionalName()
            .extension()
                // No kaboom, no explosion
                .assertNoItem(PIRACY_TALES)
                // No funeral time, description matches and condition is true.
                .assertPropertyValuesEqual(PIRACY_LOOT, 1_000_000)
                .end()
            .triggers()
                // No trigger for loot bomb. Description does not match.
                // And we are already after timeFrom anyway.
                .assertNone();
        // @formatter:on
    }

    private PrismObject<UserType> createCannibal(String name, String funeralDateOffset, boolean rotten) throws SchemaException {
        String username = rotten ? "rotten-" + name : name;
        String fullname = (rotten ? "Cannibal Rotten " : "Cannibal ") + StringUtils.capitalize(name);
        PrismObject<UserType> user = createUser(username, fullname);
        user.asObjectable().givenName(StringUtils.capitalize(name));
        if (rotten) {
            user.asObjectable().setDescription("The fruit is gone");
        }
        if (funeralDateOffset != null) {
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            XMLGregorianCalendar funeralDate;
            if (funeralDateOffset.isEmpty()) {
                funeralDate = now;
            } else {
                funeralDate = XmlTypeConverter.addDuration(now, funeralDateOffset);
            }
            funeralTimestamp = funeralDate;
            user.createExtension().setPropertyRealValue(PIRACY_FUNERAL_TIMESTAMP, funeralDate);
        }
        return user;
    }

    @Test
    public void test900DeleteUser() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createDeleteDelta(UserType.class,
                USER_JACK_OID);
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        try {
            PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
            display("User after", userJack);
            assert false : "User was not deleted: " + userJack;
        } catch (ObjectNotFoundException e) {
            // This is expected
        }

        // TODO: check on resource

        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    @Test
    public void test950CreateUserJackWithoutTemplate() throws Exception {
        // GIVEN
        setDefaultUserTemplate(null);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // WHEN
        addObject(USER_JACK_FILE, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignments(userJack, 0);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

        PrismAsserts.assertNoItem(userJack, UserType.F_ORGANIZATIONAL_UNIT);

    }

    /**
     * Would creates org on demand if the template would be active. But it is not.
     */
    @Test
    public void test952ModifyJackOrganizationalUnitFD004() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result, PolyString.fromOrig("FD004"));

        // THEN
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATIONAL_UNIT, PolyString.fromOrig("FD004"));

        assertAssignments(userJack, 0);

        UserType userJackType = userJack.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userJackType.getLinkRef().size());

        PrismObject<OrgType> org = findObjectByName(OrgType.class, "FD004");
        assertNull("Found org " + org + " but not expecting it", org);
    }

    /**
     * Set the template. Reconcile the user that should have org created on demand (but does not).
     * The org should be created.
     */
    @Test
    public void test960ReconcileUserJackWithTemplate() throws Exception {
        // GIVEN
        setDefaultUserTemplate(USER_TEMPLATE_COMPLEX_OID);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // WHEN
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
        display("User after", userJack);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_DESCRIPTION, "Where's the rum?");
        assertAssignedAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertOnDemandOrgAssigned("FD004", userJack);

        assertAssignments(userJack, 2);
        assertLiveLinks(userJack, 1);
    }

    /**
     * Setting employee type to useless. This should cause switch to different user template.
     */
    @Test
    public void test970ModifyUserGuybrushEmployeeTypeUseless() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        assertAssignedNoRole(userBefore);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_SUBTYPE, task, result, SUBTYPE_USELESS);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_GUYBRUSH_OID)
                .assignments()
                .by().roleOid(ROLE_USELESS_OID).find()
                .assertOriginMappingName("assignment-for-useless-role");
    }

    /**
     * MID-5892
     */
    @Test
    public void test980DeleteUserGivenName() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        UserType user = new UserType()
                .name("test980DeleteUserGivenName")
                .givenName("jim")
                .subtype(SUBTYPE_MID_5892)
                .beginAssignment()
                .targetRef(SystemObjectsType.ROLE_SUPERUSER.value(), RoleType.COMPLEX_TYPE)
                .end();
        repositoryService.addObject(user.asPrismObject(), null, result);

        // WHEN
        when();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).delete(new PolyString("jim"))
                .asObjectDelta(user.getOid());
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = modelService.getObject(
                UserType.class, user.getOid(), null, task, result);
        display("User after", userAfter);

        assertAssignedRole(userAfter, SystemObjectsType.ROLE_SUPERUSER.value());
    }

    /**
     * MID-6045
     */
    @Test
    public void test990SpecialTimedMapping() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clock.resetOverride();

        XMLGregorianCalendar firstTriggerTime = XmlTypeConverter.fromNow("PT20M");

        UserType user = new UserType()
                .name("test990SpecialTimedMapping")
                .givenName("jim")
                .subtype(SUBTYPE_MID_6045)
                .beginTrigger()
                .timestamp(firstTriggerTime)
                .handlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .<UserType>end()
                .beginAssignment()
                .targetRef(SystemObjectsType.ROLE_SUPERUSER.value(), RoleType.COMPLEX_TYPE)
                .end();
        repositoryService.addObject(user.asPrismObject(), null, result);

        when();
        recomputeUser(user.getOid(), task, result);

        then();
        assertSuccess(result);

        assertUser(user.getOid(), "user after")
                .display()
                .triggers()
                .assertTriggers(3)      // for some reason two new triggers are created
                .end()
                .assertAssignments(1);
    }

    /**
     * MID-10359
     */
    @Test
    public void test999CheckItemEmphasizeOverride() throws Exception {
        // GIVEN
        setDefaultUserTemplate(USER_TEMPLATE_COMPLEX_OID);

        // WHEN
        when();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userJack = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);

        PrismObjectDefinition<UserType> objectDef = modelInteractionService.getEditObjectDefinition(userJack, AuthorizationPhaseType.REQUEST, task, result);
        userJack.applyDefinition(objectDef);

        PrismProperty<Object> email = userJack.findProperty(UserType.F_EMAIL_ADDRESS);
        PrismPropertyDefinition<Object> emailDef = email.getDefinition();

        //THEN
        then();
        assertTrue("Expected that item email is emphasized", emailDef.isEmphasized());
    }
}
