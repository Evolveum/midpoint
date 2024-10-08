/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.model.intest.CommonTasks.TASK_TRIGGER_SCANNER_ON_DEMAND;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.predefinedActivationMapping.DelayedDeleteEvaluator;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.DummyAccountAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 */
@SuppressWarnings("SameParameterValue")
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMapping extends AbstractMappingTest {

    // CRIMSON resource has STRONG mappings, non-tolerant attributes, absolute-like mappings
    private static final File RESOURCE_DUMMY_CRIMSON_FILE = new File(TEST_DIR, "resource-dummy-crimson.xml");
    private static final String RESOURCE_DUMMY_CRIMSON_OID = "10000000-0000-0000-0000-0000000001c4";
    private static final String RESOURCE_DUMMY_CRIMSON_NAME = "crimson";

    // LIGHT CRIMSON is like CRIMSON but slightly stripped down
    private static final File RESOURCE_DUMMY_LIGHT_CRIMSON_FILE = new File(TEST_DIR, "resource-dummy-light-crimson.xml");
    private static final String RESOURCE_DUMMY_LIGHT_CRIMSON_OID = "aa5d09b4-54d9-11e7-8ece-576137828ab7";
    private static final String RESOURCE_DUMMY_LIGHT_CRIMSON_NAME = "lightCrimson";

    // CUSTOM FUNCTION CRIMSON is like CRIMSON but using custom library in script expressions
    private static final File RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_FILE = new File(TEST_DIR, "resource-dummy-custom-function-crimson.xml");
    private static final String RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_OID = "aa5d09b4-54d9-11e7-8888-576137828ab7";
    private static final String RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME = "customFunction";

    // COBALT: weak non-tolerant mappings
    private static final File RESOURCE_DUMMY_COBALT_FILE = new File(TEST_DIR, "resource-dummy-cobalt.xml");
    private static final String RESOURCE_DUMMY_COBALT_OID = "7f8a927c-cac4-11e7-9733-9f90849f6d4a";
    private static final String RESOURCE_DUMMY_COBALT_NAME = "cobalt";

    private static final DummyTestResource RESOURCE_DUMMY_SERVICES_OUTBOUND = new DummyTestResource(TEST_DIR,
            "resource-dummy-services-outbound.xml", "00cff96b-f283-4814-a024-4c1361e6a40d",
            "services-outbound");

    private static final DummyTestResource RESOURCE_DUMMY_SERVICES_INBOUND_PWD_COPY = new DummyTestResource(TEST_DIR,
            "resource-dummy-services-inbound-pwd-copy.xml", "81c080f2-dce5-43b9-b748-a2a5fdb48c51",
            "services-inbound-pwd-copy");

    private static final DummyTestResource RESOURCE_DUMMY_SERVICES_INBOUND_PWD_GENERATE = new DummyTestResource(TEST_DIR,
            "resource-dummy-services-inbound-pwd-generate.xml", "ae149e1e-5992-4557-829e-8dfc069276b3",
            "services-inbound-pwd-generate");

    private static final DummyTestResource RESOURCE_DUMMY_TIMED = new DummyTestResource(TEST_DIR,
            "resource-dummy-timed.xml", "567d9834-4f2c-4e5b-89a6-ebd804c7d469", "timed");

    private static final DummyTestResource RESOURCE_DUMMY_MEGA_OUTBOUND = new DummyTestResource(TEST_DIR,
            "resource-dummy-mega-outbound.xml", "2b1c05f1-8b70-43e6-ac46-3e5ee621ee36",
            "mega-outbound", TestMapping::initMegaResource);

    private static final int MEGA_ATTRIBUTES = 1000;

    private static final File ROLE_ANTINIHILIST_FILE = new File(TEST_DIR, "role-antinihilist.xml");
    private static final String ROLE_ANTINIHILIST_OID = "4c5c6c44-bd7d-11e7-99ef-9b82464da93d";

    private static final File ROLE_BLUE_TITANIC_FILE = new File(TEST_DIR, "role-blue-titanic.xml");
    private static final String ROLE_BLUE_TITANIC_OID = "97f8d44a-cab5-11e7-9d72-fbe451f26944";
    private static final String ROLE_TITANIC_SHIP_VALUE = "Titanic";

    private static final File ROLE_BLUE_POETRY_FILE = new File(TEST_DIR, "role-blue-poetry.xml");
    private static final String ROLE_BLUE_POETRY_OID = "22d3d4f6-cabc-11e7-9441-4b5c10dd30e0";
    private static final String ROLE_POETRY_QUOTE_VALUE = "Oh freddled gruntbuggly";

    private static final File ROLE_COBALT_NEVERLAND_FILE = new File(TEST_DIR, "role-cobalt-neverland.xml");
    private static final String ROLE_COBALT_NEVERLAND_OID = "04aca9d6-caca-11e7-9c6a-97b71af3e545";
    private static final String ROLE_COBALT_NEVERLAND_VALUE = "Neverland";

    private static final TestObject<RoleType> ROLE_TIMED = TestObject.file(
            TEST_DIR, "role-timed.xml", "9af2f6d7-564f-45f8-bd8a-2f5cef1596a8");

    private static final TestObject<RoleType> ROLE_DISABLED_MAPPING = TestObject.file(
            TEST_DIR, "role-disabled-mapping.xml", "f7228a46-bc75-11eb-8529-0242ac130003");

    private static final String CAPTAIN_JACK_FULL_NAME = "Captain Jack Sparrow";

    private static final String SHIP_BLACK_PEARL = "Black Pearl";

    private static final String USER_GUYBRUSH_PASSWORD_1_CLEAR = "1wannaBEaP1rat3";
    private static final String USER_GUYBRUSH_PASSWORD_2_CLEAR = "1wannaBEtheP1rat3";

    private static final String LOCALITY_BLOOD_ISLAND = "Blood Island";
    private static final String LOCALITY_BOOTY_ISLAND = "Booty Island";
    private static final String LOCALITY_SCABB_ISLAND = "Scabb Island";

    private static final String DRINK_VODKA = "vodka";
    private static final String DRINK_WHISKY = "whisky";
    private static final String DRINK_BRANDY = "brandy";
    private static final String DRINK_GRAPPA = "grappa";
    private static final String DRINK_GIN = "gin";
    private static final String DRINK_MEZCAL = "mezcal";

    private static final String USER_JIM_NAME = "jim";
    private static final String USER_TYPE_CARTHESIAN = "carthesian";

    private static final TestObject<ServiceType> SERVICE_ROUTER =
            TestObject.file(TEST_DIR, "service-router.xml", "fbe770e4-75ef-4663-93b6-a9cd484f694b");
    private static final String SERVICE_ROUTER_NAME = "router";
    private static final String SERVICE_BRIDGE_NAME = "bridge";
    private static final String SERVICE_GATEWAY_NAME = "gateway";

    private static final TestObject<TaskType> TASK_IMPORT_PWD_COPY = TestObject.file(
            TEST_DIR, "task-dummy-services-pwd-copy-import.xml", "598e0ac7-4dd7-476e-bba8-d39ebf6c951a");
    private static final TestObject<TaskType> TASK_IMPORT_PWD_GENERATE = TestObject.file(
            TEST_DIR, "task-dummy-services-pwd-generate-import.xml", "7a987537-9e87-47db-a62c-a7ba25a8fee5");

    private static final File PREDEFINED_DIR = new File(TEST_DIR, "predefined");

    private static final File RESOURCE_DUMMY_PREDEFINED_DID_FILE = new File(PREDEFINED_DIR,
            "resource-dummy-predefine-disable-instead-delete.xml");
    private static final String RESOURCE_DUMMY_PREDEFINED_DID_NAME = "predefine-disable-instead-delete";
    private static final String RESOURCE_DUMMY_PREDEFINED_DID_OID = "2b1c05f1-8b70-43e6-ac46-3e5ee621ee55";

    private static final File ROLE_PREDEFINED_DID_FILE = new File(PREDEFINED_DIR, "role-predefine-disable-instead-delete.xml");
    private static final String ROLE_PREDEFINED_DID_OID = "97f8d44a-cab5-11e7-9d72-fbe451f26932";

    private static final File RESOURCE_DUMMY_PREDEFINED_DD_FILE = new File(PREDEFINED_DIR,
            "resource-dummy-predefine-delayed-delete.xml");
    private static final String RESOURCE_DUMMY_PREDEFINED_DD_NAME = "predefine-delayed-delete";
    private static final String RESOURCE_DUMMY_PREDEFINED_DD_OID = "2b1c05f1-8b70-43e6-ac46-3e5ee621ee66";
    private static final File ROLE_PREDEFINED_DD_FILE = new File(PREDEFINED_DIR, "role-predefine-delayed-delete.xml");
    private static final String ROLE_PREDEFINED_DD_OID = "97f8d44a-cab5-11e7-9d72-fbe451f26966";

    private static final File RESOURCE_DUMMY_PREDEFINED_PPA_FILE = new File(PREDEFINED_DIR,
            "resource-dummy-predefine-pre-provision.xml");
    private static final String RESOURCE_DUMMY_PREDEFINED_PPA_NAME = "predefine-pre-provision";
    private static final String RESOURCE_DUMMY_PREDEFINED_PPA_OID = "2b1c05f1-8b70-43e6-ac46-3e5ee621ee77";
    private static final File ROLE_PREDEFINED_PPA_FILE = new File(PREDEFINED_DIR, "role-predefine-pre-provision.xml");
    private static final String ROLE_PREDEFINED_PPA_OID = "97f8d44a-cab5-11e7-9d72-fbe451f26977";

    private static final DummyTestResource RESOURCE_DUMMY_PREDEFINED_ALL = new DummyTestResource(
            PREDEFINED_DIR, "resource-dummy-predefine-all.xml", "2b1c05f1-8b70-43e6-ac46-3e5ee621ee88",
            "predefine-all");

    private static final DummyTestResource RESOURCE_DUMMY_LEGACY = new DummyTestResource(
            PREDEFINED_DIR, "resource-dummy-legacy.xml", "47c7b626-2255-46f4-8d09-72e82add1894",
            "legacy");

    private static final File ROLE_PREDEFINED_ALL_FILE = new File(PREDEFINED_DIR, "role-predefine-all.xml");
    private static final String ROLE_PREDEFINED_ALL_OID = "97f8d44a-cab5-11e7-9d72-fbe451f26988";

    private static final File USER_SHELDON_FILE = new File(PREDEFINED_DIR, "user-sheldon.xml");
    private static final String USER_SHELDON_OID = "c0c010c0-d34d-b33f-f00d-111111111122";
    private static final String USER_SHELDON_USERNAME = "sheldon";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_CRIMSON_NAME,
                RESOURCE_DUMMY_CRIMSON_FILE, RESOURCE_DUMMY_CRIMSON_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME,
                RESOURCE_DUMMY_LIGHT_CRIMSON_FILE, RESOURCE_DUMMY_LIGHT_CRIMSON_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME,
                RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_FILE, RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_COBALT_NAME,
                RESOURCE_DUMMY_COBALT_FILE, RESOURCE_DUMMY_COBALT_OID, initTask, initResult);
        initDummyResource(RESOURCE_DUMMY_PREDEFINED_DID_NAME,
                RESOURCE_DUMMY_PREDEFINED_DID_FILE,
                RESOURCE_DUMMY_PREDEFINED_DID_OID,
                initTask,
                initResult);
        initDummyResource(RESOURCE_DUMMY_PREDEFINED_DD_NAME,
                RESOURCE_DUMMY_PREDEFINED_DD_FILE,
                RESOURCE_DUMMY_PREDEFINED_DD_OID,
                initTask,
                initResult);

        initDummyResource(RESOURCE_DUMMY_PREDEFINED_PPA_NAME,
                RESOURCE_DUMMY_PREDEFINED_PPA_FILE,
                RESOURCE_DUMMY_PREDEFINED_PPA_OID,
                initTask,
                initResult);

        RESOURCE_DUMMY_PREDEFINED_ALL.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_LEGACY.initAndTest(this, initTask, initResult);

        // Do not we also want to test these resources?
        RESOURCE_DUMMY_SERVICES_OUTBOUND.init(this, initTask, initResult);
        RESOURCE_DUMMY_SERVICES_INBOUND_PWD_COPY.init(this, initTask, initResult);
        RESOURCE_DUMMY_SERVICES_INBOUND_PWD_GENERATE.init(this, initTask, initResult);
        RESOURCE_DUMMY_TIMED.init(this, initTask, initResult);
        RESOURCE_DUMMY_MEGA_OUTBOUND.init(this, initTask, initResult);

        repoAddObjectFromFile(ROLE_ANTINIHILIST_FILE, initResult);
        repoAddObjectFromFile(ROLE_BLUE_TITANIC_FILE, initResult);
        repoAddObjectFromFile(ROLE_BLUE_POETRY_FILE, initResult);
        repoAddObjectFromFile(ROLE_COBALT_NEVERLAND_FILE, initResult);
        repoAddObjectFromFile(ROLE_PREDEFINED_DID_FILE, initResult);
        repoAddObjectFromFile(ROLE_PREDEFINED_DD_FILE, initResult);
        repoAddObjectFromFile(ROLE_PREDEFINED_PPA_FILE, initResult);
        repoAddObjectFromFile(ROLE_PREDEFINED_ALL_FILE, initResult);
        repoAdd(ROLE_TIMED, initResult);
        repoAdd(ROLE_DISABLED_MAPPING, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TYPE_CARTHESIAN, USER_TEMPLATE_CARTHESIAN_OID, initResult);

        repoAddObjectFromFile(USER_SHELDON_FILE, initResult);

        addObject(TASK_TRIGGER_SCANNER_ON_DEMAND, initTask, initResult);
    }

    private static void initMegaResource(DummyResourceContoller controller) throws ConflictException,
            FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        DummyObjectClass objectClass = controller.getDummyResource().getAccountObjectClass();
        for (int i = 0; i < MEGA_ATTRIBUTES; i++) {
            controller.addAttrDef(objectClass, String.format("a-single-%04d", i), String.class, false, false);
        }
    }

    /**
     * Blue dummy has WEAK mappings. Let's play a bit with that.
     */
    @Test
    public void test100ModifyUserAssignAccountDummyBlue() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "SystemConfiguration");
        DummyAccount accountJackBlue = getDummyResource(RESOURCE_DUMMY_BLUE_NAME).getAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        String drinkBlue = accountJackBlue.getAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
        assertNotNull("No blue drink", drinkBlue);
        UUID drinkUuidBlue = UUID.fromString(drinkBlue);
        assertNotNull("No drink UUID", drinkUuidBlue);
        displayValue("Drink UUID", drinkUuidBlue.toString());

        assertAccountShip(userJack, ACCOUNT_JACK_DUMMY_FULLNAME, null, RESOURCE_DUMMY_BLUE_NAME, task);
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Where's the rum? -- Jack Sparrow");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test101ModifyUserFullName() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
                PolyString.fromOrig(CAPTAIN_JACK_FULL_NAME));

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, USER_JACK_FULL_NAME, null, RESOURCE_DUMMY_BLUE_NAME, task);

        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                getQuote(USER_JACK_DESCRIPTION, CAPTAIN_JACK_FULL_NAME));

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test102ModifyUserFullNameRecon() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_FULL_NAME,
                PolyString.fromOrig(CAPTAIN_JACK_FULL_NAME));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        modelService.executeChanges(deltas, executeOptions().reconcile(), task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, USER_JACK_FULL_NAME, null, RESOURCE_DUMMY_BLUE_NAME, task);

        // The quote attribute was empty before this operation. So the weak mapping kicks in
        // and sets a new value.
        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                getQuote(USER_JACK_DESCRIPTION, CAPTAIN_JACK_FULL_NAME));

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);         // operation is idempotent
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test104ModifyUserOrganizationalUnit() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                PolyString.fromOrig("Black Pearl"));

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, USER_JACK_FULL_NAME, "Black Pearl", RESOURCE_DUMMY_BLUE_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test105ModifyAccountShip() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                accountOid, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "Flying Dutchman");
        deltas.add(accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, USER_JACK_FULL_NAME, "Flying Dutchman", RESOURCE_DUMMY_BLUE_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * There is a weak mapping for ship attribute.
     * Therefore try to remove the value. The weak mapping should be applied.
     */
    @Test
    public void test106ModifyAccountShipReplaceEmpty() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(
                ShadowType.class, accountOid, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH);
        deltas.add(accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, USER_JACK_FULL_NAME, "Black Pearl", RESOURCE_DUMMY_BLUE_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test107ModifyAccountShipAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                accountOid, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "HMS Dauntless");
        deltas.add(accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, USER_JACK_FULL_NAME, "HMS Dauntless", RESOURCE_DUMMY_BLUE_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * There is a weak mapping for ship attribute.
     * Therefore try to remove the value. The weak mapping should be applied.
     */
    @Test
    public void test108ModifyAccountShipDelete() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationDeleteProperty(ShadowType.class,
                accountOid, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "HMS Dauntless");
        deltas.add(accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, USER_JACK_FULL_NAME, SHIP_BLACK_PEARL, RESOURCE_DUMMY_BLUE_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Assign Blue Titanic role. This role has strong mapping to blue resource
     * ship attribute. The weak mapping on blue resource should NOT be applied.
     * MID-4236
     */
    @Test
    public void test110AssignBlueTitanic() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        assignRole(USER_JACK_OID, ROLE_BLUE_TITANIC_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userAfter, USER_JACK_FULL_NAME, ROLE_TITANIC_SHIP_VALUE, RESOURCE_DUMMY_BLUE_NAME, task);
    }

    @Test
    public void test111Recompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userAfter, USER_JACK_FULL_NAME, ROLE_TITANIC_SHIP_VALUE, RESOURCE_DUMMY_BLUE_NAME, task);
    }

    /**
     * Disable assignment of Blue Titanic role.
     * The weak mapping should kick in and return black pearl back.
     * MID-4236
     */
    @Test
    public void test112DisableBlueTitanicAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType titanicAssignment = getAssignment(userBefore, ROLE_BLUE_TITANIC_OID);
        ItemPath assignmentStatusPath = ItemPath.create(FocusType.F_ASSIGNMENT, titanicAssignment.getId(), AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);

        when();
        modifyUserReplace(USER_JACK_OID, assignmentStatusPath, task, result, ActivationStatusType.DISABLED);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userAfter, USER_JACK_FULL_NAME, SHIP_BLACK_PEARL, RESOURCE_DUMMY_BLUE_NAME, task);
    }

    /**
     * MID-4236
     */
    @Test
    public void test113Recompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userAfter, USER_JACK_FULL_NAME, SHIP_BLACK_PEARL, RESOURCE_DUMMY_BLUE_NAME, task);
    }

    /**
     * MID-4236
     */
    @Test
    public void test114Reconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        reconcileUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userAfter, USER_JACK_FULL_NAME, SHIP_BLACK_PEARL, RESOURCE_DUMMY_BLUE_NAME, task);
    }

    /**
     * Re-enable assignment of Blue Titanic role.
     * MID-4236
     */
    @Test
    public void test115EnableBlueTitanicAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType titanicAssignment = getAssignment(userBefore, ROLE_BLUE_TITANIC_OID);
        ItemPath assignmentStatusPath = ItemPath.create(FocusType.F_ASSIGNMENT, titanicAssignment.getId(), AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);

        when();
        modifyUserReplace(USER_JACK_OID, assignmentStatusPath, task, result, ActivationStatusType.ENABLED);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userAfter, USER_JACK_FULL_NAME, ROLE_TITANIC_SHIP_VALUE, RESOURCE_DUMMY_BLUE_NAME, task);
    }

    @Test
    public void test118UnassignBlueTitanic() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType titanicAssignment = getAssignment(userBefore, ROLE_BLUE_TITANIC_OID);

        when();
        unassign(UserType.class, USER_JACK_OID, titanicAssignment, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userAfter, USER_JACK_FULL_NAME, SHIP_BLACK_PEARL, RESOURCE_DUMMY_BLUE_NAME, task);
    }

    /**
     * Assign Blue Poetry role. This role has strong mapping to blue resource
     * quote attribute. The weak mapping on blue resource should NOT be applied.
     * This is similar to Blue Titanic, but quote attribute is non-tolerant.
     * MID-4236
     */
    @Test
    public void test120AssignBluePoetry() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                getQuote(USER_JACK_DESCRIPTION, CAPTAIN_JACK_FULL_NAME));

        when();
        assignRole(USER_JACK_OID, ROLE_BLUE_POETRY_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, ROLE_POETRY_QUOTE_VALUE);
    }

    @Test
    public void test121Recompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, ROLE_POETRY_QUOTE_VALUE);
    }

    /**
     * MID-4236
     */
    @Test
    public void test122DisableBlueTitanicAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType poetryAssignment = getAssignment(userBefore, ROLE_BLUE_POETRY_OID);
        ItemPath assignmentStatusPath = ItemPath.create(FocusType.F_ASSIGNMENT, poetryAssignment.getId(),
                AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);

        when();
        modifyUserReplace(USER_JACK_OID, assignmentStatusPath, task, result, ActivationStatusType.DISABLED);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                getQuote(USER_JACK_DESCRIPTION, CAPTAIN_JACK_FULL_NAME));
    }

    /**
     * MID-4236
     */
    @Test
    public void test123Recompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                getQuote(USER_JACK_DESCRIPTION, CAPTAIN_JACK_FULL_NAME));
    }

    /**
     * MID-4236
     */
    @Test
    public void test124Reconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        reconcileUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                getQuote(USER_JACK_DESCRIPTION, CAPTAIN_JACK_FULL_NAME));
    }

    /**
     * Re-enable assignment of Blue Poetry role.
     * MID-4236
     */
    @Test
    public void test125EnableBluePoetryAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType poetryAssignment = getAssignment(userBefore, ROLE_BLUE_POETRY_OID);
        ItemPath assignmentStatusPath = ItemPath.create(FocusType.F_ASSIGNMENT, poetryAssignment.getId(),
                AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);

        when();
        modifyUserReplace(USER_JACK_OID, assignmentStatusPath, task, result, ActivationStatusType.ENABLED);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, ROLE_POETRY_QUOTE_VALUE);
    }

    @Test
    public void test128UnassignBluePoetry() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType poetryAssignment = getAssignment(userBefore, ROLE_BLUE_POETRY_OID);

        when();
        unassign(UserType.class, USER_JACK_OID, poetryAssignment, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertDummyAccountAttribute(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                getQuote(USER_JACK_DESCRIPTION, CAPTAIN_JACK_FULL_NAME));
        assertAccountShip(userAfter, USER_JACK_FULL_NAME, SHIP_BLACK_PEARL, RESOURCE_DUMMY_BLUE_NAME, task);
    }

    @Test
    public void test129ModifyUserUnassignAccountBlue() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, false);
        userDelta.addModificationReplaceProperty(UserType.F_FULL_NAME, PolyString.fromOrig(USER_JACK_FULL_NAME));
        userDelta.addModificationReplaceProperty(UserType.F_ORGANIZATIONAL_UNIT);
        deltas.add(userDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack);
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test140AssignCobaltAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertUserJack(userBefore);
        assertLiveLinks(userBefore, 0);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_COBALT_NAME, USER_JACK_USERNAME);

        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_COBALT_OID, null, task, result);

        then();
        assertSuccess(result);
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);
        assertLiveLinks(userAfter, 1);

        assertDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    /**
     * Destroy the value of account location attribute. Recompute should fix it.
     * This is a "control group" for MID-4236
     */
    @Test
    public void test141DestroyAndRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValue(
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Wrongland");
        displayDumpable("Account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_COBALT_OID);

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        displayDumpable("Account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    /**
     * Destroy the value of account location attribute. Reconcile should fix it.
     * This is a "control group" for MID-4236
     */
    @Test
    public void test142DestroyAndReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                "Wrongland");
        displayDumpable("Account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_COBALT_OID);

        when();
        reconcileUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        displayDumpable("Account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    /**
     * Destroy the value of account location attribute. Recompute should fix it.
     * This is a "control group" for MID-4236
     */
    @Test
    public void test143ClearAndRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME
                /* no value */);
        displayDumpable("Account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_COBALT_OID);

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        displayDumpable("Account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    /**
     * Assign Cobalt role. This role has strong mapping to cobalt resource
     * location attribute. The weak mapping on cobalt resource should NOT be applied.
     * This is similar to Blue Titanic, but location attribute is non-tolerant and single-value.
     * MID-4236
     */
    @Test
    public void test150AssignCobaltNeverland() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        assignRole(USER_JACK_OID, ROLE_COBALT_NEVERLAND_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);
        assertLiveLinks(userAfter, 1);

        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ROLE_COBALT_NEVERLAND_VALUE);
    }

    @Test
    public void test151Recompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ROLE_COBALT_NEVERLAND_VALUE);
    }

    /**
     * MID-4236
     */
    @Test
    public void test152DisableCobalNeverlandAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType roleAssignment = getAssignment(userBefore, ROLE_COBALT_NEVERLAND_OID);
        ItemPath assignmentStatusPath = ItemPath.create(FocusType.F_ASSIGNMENT, roleAssignment.getId(),
                AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);

        when();
        modifyUserReplace(USER_JACK_OID, assignmentStatusPath, task, result, ActivationStatusType.DISABLED);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    /**
     * MID-4236
     */
    @Test
    public void test153Recompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    /**
     * MID-4236
     */
    @Test
    public void test154Reconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        reconcileUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    /**
     * Destroy the value of account location attribute. Recompute should fix it.
     * MID-4236 (this is where it is really reproduced)
     */
    @Test
    public void test155DestroyAndRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                "Wrongland");
        displayDumpable("Account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_COBALT_OID);

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        displayDumpable("Account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    /**
     * Destroy the value of account location attribute. Recompute should fix it.
     * MID-4236 (this is where it is really reproduced)
     */
    @Test
    public void test156ClearAndRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME
                /* no value */);
        displayDumpable("Account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_COBALT_OID);

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_FULL_NAME, true);
        displayDumpable("Account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    /**
     * Re-enable assignment of Blue Poetry role.
     * MID-4236
     */
    @Test
    public void test157EnableCobaltNeverlandAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType roleAssignment = getAssignment(userBefore, ROLE_COBALT_NEVERLAND_OID);
        ItemPath assignmentStatusPath = ItemPath.create(FocusType.F_ASSIGNMENT, roleAssignment.getId(),
                AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);

        when();
        modifyUserReplace(USER_JACK_OID, assignmentStatusPath, task, result, ActivationStatusType.ENABLED);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ROLE_COBALT_NEVERLAND_VALUE);
    }

    @Test
    public void test158UnassignCobaltNeverland() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType roleAssignment = getAssignment(userBefore, ROLE_COBALT_NEVERLAND_OID);

        when();
        unassign(UserType.class, USER_JACK_OID, roleAssignment, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);

        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    @Test
    public void test159UnassignCobaltAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_COBALT_OID, null, task, result);

        then();
        assertSuccess(result);
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertUserJack(userAfter);
        assertLiveLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, USER_JACK_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_COBALT_NAME, USER_JACK_USERNAME);
    }

    /**
     * Red dummy has STRONG mappings.
     */
    @Test
    public void test160ModifyUserAssignAccountDummyRed() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
                RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(userDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_RED_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType(RESOURCE_DUMMY_RED_NAME));

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack", USER_JACK_FULL_NAME, true);

        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "mouth", "pistol");
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Where's the rum? -- red resource");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test161ModifyUserFullName() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
                PolyString.fromOrig(CAPTAIN_JACK_FULL_NAME));

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, CAPTAIN_JACK_FULL_NAME, null, RESOURCE_DUMMY_RED_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test162ModifyUserOrganizationalUnit() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                PolyString.fromOrig("Black Pearl"));

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, CAPTAIN_JACK_FULL_NAME, "Black Pearl", RESOURCE_DUMMY_RED_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test163ModifyAccountShip() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                accountOid, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "Flying Dutchman");
        deltas.add(accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertPartialError(result);

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, CAPTAIN_JACK_FULL_NAME, "Black Pearl", RESOURCE_DUMMY_RED_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.PARTIAL_ERROR);
    }

    /**
     * This test will not fail. It will splice the strong mapping into an empty replace delta.
     * That still results in a single value and is a valid operation, although it really changes nothing
     * (replace with the same value that was already there).
     */
    @Test
    public void test164ModifyAccountShipReplaceEmpty() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(
                ShadowType.class, accountOid, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH);
        deltas.add(accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, CAPTAIN_JACK_FULL_NAME, "Black Pearl", RESOURCE_DUMMY_RED_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test166ModifyAccountShipDelete() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationDeleteProperty(ShadowType.class,
                accountOid, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_PATH, "Black Pearl");
        deltas.add(accountDelta);

        when();
        setTracing(task, createDefaultTracingProfile());
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result); // MID-6372
        assertThatOperationResult(result)
                .anyLogEntryMatches(text ->
                        text.contains(" WARN ") &&
                                text.contains("Attempt to delete value") &&
                                text.contains("but that value is mandated by a strong mapping mapping"));

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, CAPTAIN_JACK_FULL_NAME, "Black Pearl", RESOURCE_DUMMY_RED_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2); // MID-6372
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.SUCCESS); // MID-6372
    }

    /**
     * Organization is used in the expression for "ship" attribute. But it is not specified as a source.
     * Nevertheless the mapping is strong, therefore the result should be applied anyway.
     * Reconciliation should be triggered.
     */
    @Test
    public void test168ModifyUserOrganization() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATION, task, result,
                PolyString.fromOrig("Brethren of the Coast"));

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, CAPTAIN_JACK_FULL_NAME, "Brethren of the Coast / Black Pearl", RESOURCE_DUMMY_RED_NAME, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Note: red resource disables account on unassign, does NOT delete it
     */
    @Test
    public void test178ModifyUserUnassignAccountRed() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        String accountRedOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);

        XMLGregorianCalendar trigStart = clock.currentTimeXMLGregorianCalendar();
        trigStart.add(XmlTypeConverter.createDuration(true, 0, 0, 25, 0, 0, 0));
        XMLGregorianCalendar trigEnd = clock.currentTimeXMLGregorianCalendar();
        trigEnd.add(XmlTypeConverter.createDuration(true, 0, 0, 35, 0, 0, 0));
        assertTrigger(accountRed, RecomputeTriggerHandler.HANDLER_URI, trigStart, trigEnd);

        XMLGregorianCalendar disableTimestamp = accountRed.asObjectable().getActivation().getDisableTimestamp();
        TestUtil.assertBetween("disableTimestamp", start, end, disableTimestamp);

        assertAccountShip(userJack, CAPTAIN_JACK_FULL_NAME, "Brethren of the Coast / Black Pearl", false, getDummyResourceController(RESOURCE_DUMMY_RED_NAME), task);

        // Check if dummy resource account is gone
        assertNoDummyAccount("jack");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Note: red resource disables account on unassign, does NOT delete it
     * So let's delete the account explicitly to make room for the following tests
     */
    @Test
    public void test179DeleteAccountRed() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String acccountRedOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> shadowDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, acccountRedOid);
        deltas.add(shadowDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");
        assertNoLinkedAccount(userJack);

        // Check if dummy resource accounts are gone
        assertNoDummyAccount("jack");
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "jack");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Default dummy has combination of NORMAL, WEAK and STRONG mappings.
     */
    @Test
    public void test180ModifyUserAssignAccountDummyDefault() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
                RESOURCE_DUMMY_OID, null, true);
        userDelta.addModificationReplaceProperty(UserType.F_FULL_NAME, PolyString.fromOrig(USER_JACK_FULL_NAME));
        userDelta.addModificationReplaceProperty(UserType.F_ORGANIZATIONAL_UNIT);
        deltas.add(userDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, "jack", getDummyResourceType());

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "jack", getDummyResourceType());

        // Check account in dummy resource
        assertDummyAccount(null, "jack", USER_JACK_FULL_NAME, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * fullName mapping is NORMAL, the change should go through
     */
    @Test
    public void test181ModifyUserFullName() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
                PolyString.fromOrig(CAPTAIN_JACK_FULL_NAME));

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow");

        assertAccountShip(userJack, CAPTAIN_JACK_FULL_NAME, null, null, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * location mapping is STRONG
     */
    @Test
    public void test182ModifyUserLocality() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_LOCALITY, task, result,
                PolyString.fromOrig("Fountain of Youth"));

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow", "Fountain of Youth");

        assertAccountLocation(userJack, CAPTAIN_JACK_FULL_NAME, "Fountain of Youth", dummyResourceCtl, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test183ModifyAccountLocation() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                accountOid, DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_PATH, "Davie Jones Locker");
        deltas.add(accountDelta);

        when();

        modelService.executeChanges(deltas, null, task, result);

        then();
        assertPartialError(result);

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow", "Fountain of Youth");

        assertAccountLocation(userJack, CAPTAIN_JACK_FULL_NAME, "Fountain of Youth", dummyResourceCtl, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.PARTIAL_ERROR);
    }

    /**
     * This test will not fail. It will splice the strong mapping into an empty replace delta.
     * That still results in a single value and is a valid operation, although it really changes nothing
     * (replace with the same value that was already there).
     */
    @Test
    public void test184ModifyAccountLocationReplaceEmpty() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationReplaceProperty(
                ShadowType.class, accountOid, DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_PATH);
        deltas.add(accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow", "Fountain of Youth");

        assertAccountLocation(userJack, CAPTAIN_JACK_FULL_NAME, "Fountain of Youth", dummyResourceCtl, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test185ModifyAccountLocationDelete() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(userJack);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object().createModificationDeleteProperty(
                ShadowType.class, accountOid, DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_PATH, "Fountain of Youth");
        deltas.add(accountDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result); // MID-6372

        userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow", "Fountain of Youth");

        assertAccountLocation(userJack, CAPTAIN_JACK_FULL_NAME, "Fountain of Youth", dummyResourceCtl, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2); // MID-6372
        dummyAuditService.assertExecutionOutcome(OperationResultStatus.SUCCESS); // MID-6372
    }

    @Test
    public void test188ModifyUserRename() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_NAME, task, result,
                PolyString.fromOrig("renamedJack"));

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, "renamedJack", CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow", "Fountain of Youth");

        assertAccountRename(userJack, "renamedJack", CAPTAIN_JACK_FULL_NAME, dummyResourceCtl, task);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test189ModifyUserUnassignAccountDummy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, false);
        deltas.add(accountAssignmentUserDelta);

        when();
        modelService.executeChanges(deltas, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertUserJack(userJack, "renamedJack", CAPTAIN_JACK_FULL_NAME, "Jack", "Sparrow", "Fountain of Youth");
        // Check accountRef
        assertUserNoAccountRefs(userJack);

        // Check if dummy resource account is gone
        assertNoDummyAccount("renamedJack");

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    private void assertAccountShip(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
            String dummyResourceName, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException, InterruptedException {
        assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, expectedShip, true, getDummyResourceController(dummyResourceName), task);
    }

    private void assertAccountShip(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
            boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException, InterruptedException {
        assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, expectedShip, expectedEnabled, resourceCtl, task);
    }

    private void assertAccountLocation(PrismObject<UserType> userJack, String expectedFullName, String expectedShip,
            DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException, InterruptedException {
        assertAccount(userJack, expectedFullName, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, expectedShip, true, resourceCtl, task);
    }

    private void assertAccountRename(PrismObject<UserType> userJack, String name, String expectedFullName,
            DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException, InterruptedException {
        assertAccount(userJack, name, expectedFullName, null, null, true, resourceCtl, task);
    }

    private void assertAccount(PrismObject<UserType> userJack, String name, String expectedFullName, String shipAttributeName, String expectedShip,
            boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException, InterruptedException {
        // ship inbound mapping is used, it is strong
        String accountOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        display("Repo shadow", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, name, resourceCtl.getResource().asObjectable());

        // Check account
        // All the changes should be reflected to the account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, task.getResult());
        display("Model shadow", accountModel);
        assertAccountShadowModel(accountModel, accountOid, name, resourceCtl.getResource().asObjectable());
        PrismAsserts.assertPropertyValue(accountModel, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, expectedFullName);
        if (shipAttributeName != null) {
            if (expectedShip == null) {
                PrismAsserts.assertNoItem(accountModel,
                        resourceCtl.getAttributePath(shipAttributeName));
            } else {
                PrismAsserts.assertPropertyValue(accountModel,
                        resourceCtl.getAttributePath(shipAttributeName),
                        expectedShip);
            }
        }

        // Check account in dummy resource
        assertDummyAccount(resourceCtl.getName(), name, expectedFullName, expectedEnabled);
    }

    private void assertAccount(PrismObject<UserType> userJack, String expectedFullName, String attributeName, String expectedShip,
            boolean expectedEnabled, DummyResourceContoller resourceCtl, Task task) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException, InterruptedException {
        assertAccount(userJack, "jack", expectedFullName, attributeName, expectedShip, expectedEnabled, resourceCtl, task);
    }

    @Test
    public void test200ModifyUserAssignAccountDummyCrimson() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_CRIMSON_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);

    }

    /**
     * MID-3661
     */
    @Test
    public void test202NativeModifyDummyCrimsonThenReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_VODKA, DRINK_WHISKY);

        displayDumpable("Dummy account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_CRIMSON_OID);

        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_VODKA, DRINK_WHISKY, rumFrom(ACCOUNT_GUYBRUSH_DUMMY_LOCATION));

    }

    /**
     * Just make sure that plain recon does not destroy anything.
     * MID-3661
     */
    @Test
    public void test204DummyCrimsonReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_CRIMSON_OID);

        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_VODKA, DRINK_WHISKY, rumFrom(ACCOUNT_GUYBRUSH_DUMMY_LOCATION));
    }

    /**
     * IO Error on the resource. The account is not fetched. The operation should fail
     * and nothing should be destroyed.
     * MID-3661
     */
    @Test
    public void test206DummyCrimsonReconcileIOError() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_CRIMSON_OID);

        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        then();
        result.computeStatus();
        TestUtil.assertPartialError(result);

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_VODKA, DRINK_WHISKY, rumFrom(ACCOUNT_GUYBRUSH_DUMMY_LOCATION));
    }

    /**
     * Just make sure that second recon run does not destroy anything.
     * MID-3661
     */
    @Test
    public void test208DummyCrimsonReconcileAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, ACCOUNT_GUYBRUSH_DUMMY_LOCATION);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_VODKA, DRINK_WHISKY, rumFrom(ACCOUNT_GUYBRUSH_DUMMY_LOCATION));
    }

    /**
     * MID-3661
     */
    @Test
    public void test210ModifyUserLocality() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig(LOCALITY_BLOOD_ISLAND));

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BLOOD_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_VODKA, DRINK_WHISKY, rumFrom(LOCALITY_BLOOD_ISLAND));
    }

    /**
     * MID-3661
     */
    @Test
    public void test212ModifyUserLocalityRecon() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        when();
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_GUYBRUSH_OID, UserType.F_LOCALITY,
                PolyString.fromOrig(LOCALITY_SCABB_ISLAND));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        ModelExecuteOptions options = executeOptions().reconcile();
        modelService.executeChanges(deltas, options, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_SCABB_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_VODKA, DRINK_WHISKY, "rum from Scabb Island");
    }

    /**
     * MID-3661
     */
    @Test
    public void test214ModifyUserLocalityIOError() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_CRIMSON_OID);

        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig(LOCALITY_BOOTY_ISLAND));

        then();
        assertSuccess(result, 1);           // there's hidden PARTIAL_ERROR deep inside

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        // TODO: How? Why?
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_VODKA, DRINK_WHISKY, "rum from Scabb Island");
    }

    /**
     * MID-3661
     */
    @Test
    public void test220NativeModifyDummyCrimsonThenChangePassword() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_BRANDY, DRINK_GRAPPA);
        displayDumpable("Dummy account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_CRIMSON_OID);

        // Make sure that only get is broken and not modify. We want to give the test a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

        when();
        modifyUserChangePassword(USER_GUYBRUSH_OID, USER_GUYBRUSH_PASSWORD_1_CLEAR, task, result);

        then();
        assertSuccess(result, 1);           // there's hidden PARTIAL_ERROR deep inside

        getDummyResource(RESOURCE_DUMMY_CRIMSON_NAME).resetBreakMode();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        assertEncryptedUserPassword(userAfter, USER_GUYBRUSH_PASSWORD_1_CLEAR);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        // location haven't changed and recon was not requested. The mapping was not evaluated.
        assertDummyAccountAttribute(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_BRANDY, DRINK_GRAPPA);
    }

    @Test
    public void test229ModifyUserUnassignAccountDummyCrimson() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        unassignAccountFromUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_CRIMSON_OID, null, task, result);

        then();
        assertSuccess(result, 1);           // there's hidden PARTIAL_ERROR deep inside

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        assertNoAssignments(userAfter);
        assertLiveLinks(userAfter, 0);

        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

    }

    @Test
    public void test250ModifyUserAssignAccountDummyLightCrimson() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // preconditions
        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_LOCALITY, PolyString.fromOrig(LOCALITY_BOOTY_ISLAND));
        assertNoDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        when();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_LIGHT_CRIMSON_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);

    }

    /**
     * MID-3661, MID-3674
     */
    @Test
    public void test252NativeModifyDummyLightCrimsonThenReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_GIN, DRINK_MEZCAL);

        displayDumpable("Dummy account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIGHT_CRIMSON_OID);

        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BOOTY_ISLAND));
    }

    /**
     * Just make sure that plain recon does not destroy anything.
     * MID-3661, MID-3674
     */
    @Test
    public void test254DummyLightCrimsonReconcile() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BOOTY_ISLAND));
    }

    /**
     * IO Error on the resource. The account is not fetched. The operation should fail
     * and nothing should be destroyed.
     * MID-3661, MID-3674
     */
    @Test
    public void test256DummyLightCrimsonReconcileIOError() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        // Make sure that only get is broken and not modify. We want to give the test a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        then();
        result.computeStatus();
        if (InternalsConfig.isShadowCachingFullByDefault()) {
            assertSuccess(result);
        } else {
            TestUtil.assertPartialError(result);
        }

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BOOTY_ISLAND));
    }

    /**
     * Just make sure that second recon run does not destroy anything.
     * MID-3661, MID-3674
     */
    @Test
    public void test258DummyLightCrimsonReconcileAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        when();
        reconcileUser(USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BOOTY_ISLAND));
    }

    /**
     * MID-3661, MID-3674
     */
    @Test
    public void test260ModifyUserLocality() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig(LOCALITY_BLOOD_ISLAND));

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BLOOD_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_BLOOD_ISLAND));
    }

    /**
     * MID-3661, MID-3674
     */
    @Test
    public void test262ModifyUserLocalityRecon() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        when();
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_GUYBRUSH_OID, UserType.F_LOCALITY,
                PolyString.fromOrig(LOCALITY_SCABB_ISLAND));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        ModelExecuteOptions options = executeOptions().reconcile();
        modelService.executeChanges(deltas, options, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_SCABB_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_GIN, DRINK_MEZCAL, rumFrom(LOCALITY_SCABB_ISLAND));
    }

    /**
     * MID-3661, MID-3674
     */
    @Test
    public void test264ModifyUserLocalityIOError() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        displayDumpable("Dummy account before", dummyAccountBefore);

        // Make sure that only get is broken and not modify. We want to give the test a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig(LOCALITY_BOOTY_ISLAND));

        then();
        if (InternalsConfig.isShadowCachingFullByDefault()) {
            assertSuccess(result);
        } else {
            assertSuccess(result, 1); // there's hidden PARTIAL_ERROR deep inside
        }

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        // TODO: How? Why?
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_GIN, DRINK_MEZCAL,
                InternalsConfig.isShadowCachingFullByDefault() ?
                        rumFrom(LOCALITY_BOOTY_ISLAND) : // the read operation was not needed
                        rumFrom(LOCALITY_SCABB_ISLAND));
    }

    /**
     * MID-3661, MID-3674
     */
    @Test
    public void test270NativeModifyDummyLightCrimsonThenChangePassword() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        DummyAccount dummyAccountBefore = getDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccountBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_BRANDY, DRINK_GRAPPA);
        displayDumpable("Dummy account before", dummyAccountBefore);

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIGHT_CRIMSON_OID);

        // Make sure that only get is broken and not modify. We want to give the test
        // a chance to destroy data.
        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).setGetBreakMode(BreakMode.IO);

        when();
        modifyUserChangePassword(USER_GUYBRUSH_OID, USER_GUYBRUSH_PASSWORD_2_CLEAR, task, result);

        then();
        assertSuccess(result, 1);           // there's hidden PARTIAL_ERROR deep inside

        getDummyResource(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME).resetBreakMode();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        String accountOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Repo shadow after", repoShadow);
        assertNoPostponedOperation(repoShadow);

        assertEncryptedUserPassword(userAfter, USER_GUYBRUSH_PASSWORD_2_CLEAR);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BOOTY_ISLAND);
        // location haven't changed and recon was not requested. The mapping was not evaluated.
        assertDummyAccountAttribute(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                DRINK_BRANDY, DRINK_GRAPPA);
    }

    @Test
    public void test279ModifyUserUnassignAccountDummyLightCrimson() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        unassignAccountFromUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_LIGHT_CRIMSON_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        assertNoAssignments(userAfter);
        assertLiveLinks(userAfter, 0);

        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_LIGHT_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

    }

    /**
     * MID-3816, MID-4008
     */
    @Test
    public void test300AssignGuybrushDummyYellow() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                IntegrationTestTools.CONST_DRINK);
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                IntegrationTestTools.CONST_BLABLA + " administrator -- administrator");
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Some say elaine -- administrator");
    }

    @Test
    public void test302ModifyGuybrushLocality() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig("Forbidden dodecahedron"));

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                "Forbidden dodecahedron");
    }

    @Test
    public void test309UnassignGuybrushDummyYellow() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        unassignAccountFromUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_YELLOW_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);
        assertNoAssignments(userAfter);
        assertLiveLinks(userAfter, 0);

        // Check account in dummy resource
        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
    }

    @Test
    public void test400ModifyUserAssignAccountDummyCrimsonCustomFunction() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, USER_GUYBRUSH_USERNAME.toUpperCase(),
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account", dummyAccount);

    }

    @Test
    public void test401ModifyUserLocalityDummyCrisomCustomFunction() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig(LOCALITY_SCABB_ISLAND));

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccount = assertDummyAccount(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, USER_GUYBRUSH_USERNAME.toUpperCase(),
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account", dummyAccount);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, USER_GUYBRUSH_USERNAME.toUpperCase(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_SCABB_ISLAND);
    }

    @Test
    public void test402ModifyDrinkDummyCustomFunctionCrimson() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig(LOCALITY_BLOOD_ISLAND));

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        assertUser(userAfter, USER_GUYBRUSH_OID, USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME,
                USER_GUYBRUSH_GIVEN_NAME, USER_GUYBRUSH_FAMILY_NAME);

        // Check account in dummy resource
        DummyAccount dummyAccountAfter = assertDummyAccount(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME.toUpperCase(),
                ACCOUNT_GUYBRUSH_DUMMY_FULLNAME, true);
        displayDumpable("Dummy account after", dummyAccountAfter);
        assertDummyAccountAttribute(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME.toUpperCase(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, LOCALITY_BLOOD_ISLAND);
        // location haven't changed and recon was not requested. The mapping was not evaluated.
        assertDummyAccountAttribute(RESOURCE_DUMMY_CUSTOM_FUNCTION_CRIMSON_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME.toUpperCase(),
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                "rum from " + LOCALITY_BLOOD_ISLAND);
    }

    /**
     * MID-2860
     */
    @Test
    public void test420AssignAntinihilistToJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertNoAssignments(userBefore);
        assertLiveLinks(userBefore, 0);

        try {
            when();
            assignRole(USER_JACK_OID, ROLE_ANTINIHILIST_OID, task, result);
        } catch (ExpressionEvaluationException e) {
            displayException("Exception", e);
            Throwable cause = e.getCause();
            if (!(cause instanceof AssertionError)) {
                throw e;
            }
        }

        then();
        assertFailure(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertNoAssignments(userAfter);
        assertLiveLinks(userAfter, 0);
    }

    /**
     * MID-2860
     */
    @Test
    public void test422AssignAccountAndAntinihilistToJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        assertLiveLinks(userBefore, 1);

        when();
        assignRole(USER_JACK_OID, ROLE_ANTINIHILIST_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertAssignedRole(userAfter, ROLE_ANTINIHILIST_OID);
        assertLiveLinks(userAfter, 1);
    }

    /**
     * MID-2860
     */
    @Test
    public void test425UnassignAntinihilistFromJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);
        assertLiveLinks(userBefore, 1);

        when();
        unassignRole(USER_JACK_OID, ROLE_ANTINIHILIST_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertNotAssignedRole(userAfter, ROLE_ANTINIHILIST_OID);
        assertLiveLinks(userAfter, 1);
    }

    /**
     * MID-2860
     */
    @Test
    public void test427UnassignAccountFromJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        assertLiveLinks(userBefore, 1);

        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 0);
    }

    /**
     * MID-4862
     */
    @Test
    public void test500AssignmentsCombinationSingle() throws Exception {
        given();
        UserType jim = prismContext.createKnownObjectable(UserType.class)
                .name(USER_JIM_NAME)
                .subtype(USER_TYPE_CARTHESIAN)
                .beginAssignment()
                .targetRef(ROLE_SUPERUSER.oid, RoleType.COMPLEX_TYPE)
                .end();

        when();
        addObject(jim.asPrismObject());

        then();

        PrismObject<UserType> userAfter = getUser(jim.getOid());
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
    }

    /**
     * MID-4862
     */
    @Test
    public void test510AssignmentsCombinationCouple() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> jim = findUserByUsername(USER_JIM_NAME);

        when();
        assignOrg(jim.getOid(), ORG_SAVE_ELAINE_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(jim.getOid());
        display("User after", userAfter);
        assertAssignments(userAfter, 3);
    }

    /**
     * MID-4863
     */
    @Test
    public void test520DeleteUserAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<UserType> jim = findUserByUsername(USER_JIM_NAME);

        when();
        AssignmentType orgAssignment = findAssignment(jim, ORG_SAVE_ELAINE_OID, SchemaConstants.ORG_DEFAULT);
        assertNotNull("org assignment not found", orgAssignment);
        PrismContainerValue<Containerable> orgAssignmentPcv = prismContext.itemFactory().createContainerValue();
        orgAssignmentPcv.setId(orgAssignment.getId());
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(orgAssignmentPcv)
                .asObjectDelta(jim.getOid());
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(jim.getOid());
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
    }

    /**
     * MID-4863 + MID-7057
     */
    @Test
    public void test530DeleteAssignmentByIdWithMegaMappings() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        InternalMonitor.reset();
        InternalMonitor.setTrace(InternalCounters.PRISM_OBJECT_CLONE_COUNT, true);

        final String userName = "test530";
        UserType user = new UserType()
                .name(userName)
                .beginAssignment()
                .targetRef(ROLE_SUPERUSER.oid, RoleType.COMPLEX_TYPE)
                .<UserType>end()
                .beginAssignment()
                .beginConstruction()
                .resourceRef(RESOURCE_DUMMY_MEGA_OUTBOUND.oid, ResourceType.COMPLEX_TYPE)
                .<AssignmentType>end()
                .end();
        String oid = addObject(user.asPrismObject(), null, task, result);

        PrismObject<UserType> userCreated = assertUser(oid, "after creation")
                .display()
                .assertAssignments(2)
                .assertLinks(1, 0)
                .getObject();
        DummyAccount account = assertDummyAccount(RESOURCE_DUMMY_MEGA_OUTBOUND.name, userName);
        assertThat(account.getAttributeValue("a-single-0555")).as("attribute value").isEqualTo(userName);

        when();

        AssignmentType roleAssignment = findAssignment(userCreated, ROLE_SUPERUSER.oid, SchemaConstants.ORG_DEFAULT);
        assertNotNull("role assignment not found", roleAssignment);
        PrismContainerValue<Containerable> roleAssignmentIdOnlyPcv = prismContext.itemFactory().createContainerValue();
        roleAssignmentIdOnlyPcv.setId(roleAssignment.getId());
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(roleAssignmentIdOnlyPcv)
                .asObjectDelta(oid);

        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        // we will be happy to get a number significantly lower than ~2000 (2x1000 mappings)
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_CLONE_COUNT, 0, 100);

        assertUser(oid, "after assignment deletion")
                .display()
                .assertAssignments(1)
                .assertLinks(1, 0);
    }

    /**
     * MID-6025
     */
    @Test
    public void test600AddService() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        final String password = "pwd1234";

        when();
        addObject(SERVICE_ROUTER, task, result);

        then();
        assertSuccess(result);

        assertService(SERVICE_ROUTER.oid, "service")
                .display()
                .assertLiveLinks(1)
                .assertPassword(password);

        DummyResource resource = getDummyResource(RESOURCE_DUMMY_SERVICES_OUTBOUND.name);
        DummyAccount account = resource.getAccountByName(SERVICE_ROUTER_NAME);
        new DummyAccountAsserter<>(account, RESOURCE_DUMMY_SERVICES_OUTBOUND.name)
                .display()
                .assertPassword(password);
    }

    /**
     * MID-6025
     */
    @Test
    public void test610ModifyServicePassword() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        final String newPassword = "dummy";
        ProtectedStringType newPasswordProtected = new ProtectedStringType();
        newPasswordProtected.setClearValue(newPassword);

        when();
        ObjectDelta<ServiceType> delta = deltaFor(ServiceType.class)
                .item(ServiceType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE)
                .replace(newPasswordProtected)
                .asObjectDelta(SERVICE_ROUTER.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        assertService(SERVICE_ROUTER.oid, "service")
                .display()
                .assertLiveLinks(1)
                .assertPassword(newPassword);

        DummyResource resource = getDummyResource(RESOURCE_DUMMY_SERVICES_OUTBOUND.name);
        DummyAccount account = resource.getAccountByName(SERVICE_ROUTER_NAME);
        new DummyAccountAsserter<>(account, RESOURCE_DUMMY_SERVICES_OUTBOUND.name)
                .display()
                .assertPassword(newPassword);
    }

    /**
     * MID-6025
     */
    @Test
    public void test650ImportFromInboundPwdCopy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        final String password = "secret";

        DummyResource resource = getDummyResource(RESOURCE_DUMMY_SERVICES_INBOUND_PWD_COPY.name);
        DummyAccount bridge = new DummyAccount(SERVICE_BRIDGE_NAME);
        bridge.setPassword(password);
        resource.addAccount(bridge);

        when();
        addObject(TASK_IMPORT_PWD_COPY, task, result);
        waitForTaskFinish(TASK_IMPORT_PWD_COPY.oid);

        then();
        assertServiceByName(SERVICE_BRIDGE_NAME, "service")
                .display()
                .assertLiveLinks(1)
                .assertPassword(password);
    }

    /**
     * MID-6025
     */
    @Test
    public void test660ImportFromInboundPwdCopyModifyPassword() throws Exception {
        given();
        final String newPassword = "SeCrEt123";

        DummyResource resource = getDummyResource(RESOURCE_DUMMY_SERVICES_INBOUND_PWD_COPY.name);
        resource.getAccountByName(SERVICE_BRIDGE_NAME).setPassword(newPassword);

        when();
        rerunTask(TASK_IMPORT_PWD_COPY.oid);

        then();
        assertServiceByName(SERVICE_BRIDGE_NAME, "service")
                .display()
                .assertLiveLinks(1)
                .assertPassword(newPassword);
    }

    /**
     * MID-6025
     */
    @Test
    public void test670ImportFromInboundPwdGenerate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        final String password = "secret-gw";

        DummyResource resource = getDummyResource(RESOURCE_DUMMY_SERVICES_INBOUND_PWD_GENERATE.name);
        DummyAccount gateway = new DummyAccount(SERVICE_GATEWAY_NAME);
        gateway.setPassword(password);
        resource.addAccount(gateway);

        when();
        addObject(TASK_IMPORT_PWD_GENERATE, task, result);
        waitForTaskFinish(TASK_IMPORT_PWD_GENERATE.oid);

        then();
        assertServiceByName(SERVICE_GATEWAY_NAME, "service")
                .display()
                .assertLiveLinks(1)
                .assertHasPassword();
    }

    /**
     * MID-6025
     */
    @Test
    public void test680ImportFromInboundPwdGenerateModifyPassword() throws Exception {
        given();
        final String newPassword = "secret-gw-2";

        PrismObject<ServiceType> serviceBefore = findObjectByName(ServiceType.class, SERVICE_GATEWAY_NAME);
        ProtectedStringType passwordBefore = serviceBefore.asObjectable().getCredentials().getPassword().getValue();
        String clearValueBefore = protector.decryptString(passwordBefore);
        System.out.println("Generated password = " + clearValueBefore);

        DummyResource resource = getDummyResource(RESOURCE_DUMMY_SERVICES_INBOUND_PWD_GENERATE.name);
        resource.getAccountByName(SERVICE_GATEWAY_NAME).setPassword(newPassword);

        when();
        rerunTask(TASK_IMPORT_PWD_GENERATE.oid);

        then();
        assertServiceByName(SERVICE_GATEWAY_NAME, "service")
                .display()
                .assertLiveLinks(1)
                .assertPassword(clearValueBefore);
    }

    /**
     * MID-5874
     */
    @Test
    public void test700TimedOutbound() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        UserType user = new UserType()
                .name("test700")
                .beginAssignment()
                .targetRef(ROLE_TIMED.oid, RoleType.COMPLEX_TYPE)
                .end();

        when();
        String oid = addObject(user, task, result);

        then();
        assertUser(oid, "user after")
                .display()
                .triggers()
                .single()
                .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .assertTimestampFuture("P2M", 20000);
    }

    /**
     * Assign Disabled Mapping role. This role has strong mapping to cobalt resource
     * wealth attribute and strong, but disabled mapping to weapon attribute. Account
     * on the Cobalt resource should be created, byt weapon attribute has not to be
     * evaluated.
     */
    @Test
    public void test750assignRoleDisabledMapping() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when();
        UserType user = new UserType()
                .name("test750")
                .beginAssignment()
                .targetRef(ROLE_DISABLED_MAPPING.oid, RoleType.COMPLEX_TYPE)
                .end();
        String oid = addObject(user, task, result);

        then();
        assertSuccess(result);

        assertUser(oid, "User after")
                .assertName("test750")
                .assertAssignments(1)
                .assignments().assertRole(ROLE_DISABLED_MAPPING.oid);

        assertDummyAccountAttribute(RESOURCE_DUMMY_COBALT_NAME, "test750",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME, 30000);
        assertDummyAccountNoAttribute(RESOURCE_DUMMY_COBALT_NAME, "test750",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
    }

    @Test
    public void test800PredefinedDisableInsteadDelete() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when("assign and unassign role");
        assignRole(USER_SHELDON_OID, ROLE_PREDEFINED_DID_OID, task, result);
        unassignRole(USER_SHELDON_OID, ROLE_PREDEFINED_DID_OID, task, result);

        then("expected that account is disabled");
        assertSuccess(result);
        assertDummyAccountActivation(RESOURCE_DUMMY_PREDEFINED_DID_NAME, USER_SHELDON_USERNAME, false);

        assertShadow(
                findShadowByNameViaModel(
                        ShadowKindType.ACCOUNT,
                        "default",
                        USER_SHELDON_USERNAME,
                        getDummyResourceObject(RESOURCE_DUMMY_PREDEFINED_DID_NAME),
                        null,
                        task,
                        result), "shadow after")
                .display()
                .asShadow()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED);

        when("again assign role");
        assignRole(USER_SHELDON_OID, ROLE_PREDEFINED_DID_OID, task, result);

        then("expected that account is enabled");
        assertSuccess(result);
        assertDummyAccountActivation(RESOURCE_DUMMY_PREDEFINED_DID_NAME, USER_SHELDON_USERNAME, true);

        assertShadow(
                findShadowByNameViaModel(
                        ShadowKindType.ACCOUNT,
                        "default",
                        USER_SHELDON_USERNAME,
                        getDummyResourceObject(RESOURCE_DUMMY_PREDEFINED_DID_NAME),
                        null,
                        task,
                        result), "shadow after")
                .display()
                .asShadow()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
    }

    @Test
    public void test801PredefinedDelayedDelete() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when("assign and unassign role");
        assignRole(USER_SHELDON_OID, ROLE_PREDEFINED_DD_OID, task, result);
        unassignRole(USER_SHELDON_OID, ROLE_PREDEFINED_DD_OID, task, result);

        then("expected that account is disabled");
        assertSuccess(result);
        assertDummyAccountActivation(RESOURCE_DUMMY_PREDEFINED_DD_NAME, USER_SHELDON_USERNAME, false);

        // @formatter:off
        assertShadow(
                findShadowByNameViaModel(
                        ShadowKindType.ACCOUNT,
                        "default",
                        USER_SHELDON_USERNAME,
                        getDummyResourceObject(RESOURCE_DUMMY_PREDEFINED_DD_NAME),
                        null,
                        task,
                        result), "shadow after")
                .display()
                .triggers()
                    .single()
                        .assertHandlerUri(RecomputeTriggerHandler.HANDLER_URI)
                        .assertOriginDescription(DelayedDeleteEvaluator.class.getSimpleName())
                        .assertTimestampFuture("P5D", 20000)
                    .end()
                .end()
                .asShadow()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED);
        // @formatter:on

        when("override time to future and recompute user");
        assertDummyAccount(RESOURCE_DUMMY_PREDEFINED_DD_NAME, USER_SHELDON_USERNAME);
        clock.overrideDuration(XmlTypeConverter.createDuration(true, 0, 0, 10, 0, 0, 0));
        runTriggerScannerOnDemand(result);

        then("expected that account non-exist");
        assertSuccess(result);
        assertNoDummyAccount(RESOURCE_DUMMY_PREDEFINED_DD_NAME, USER_SHELDON_USERNAME);
    }

    @Test
    public void test802PredefinedPreProvisionEvaluator() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        XMLGregorianCalendar validFrom = XmlTypeConverter.createXMLGregorianCalendar(clock.currentTimeMillis());
        validFrom.add(XmlTypeConverter.createDuration(true, 0, 0, 10, 0, 0, 0));
        modifyObjectReplaceProperty(
                UserType.class,
                USER_SHELDON_OID,
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                task,
                result,
                validFrom);

        recomputeUser(USER_SHELDON_OID);

        when("assign role");
        assignRole(USER_SHELDON_OID, ROLE_PREDEFINED_PPA_OID, task, result);

        then("expected that account non-exists");
        assertSuccess(result);
        assertNoDummyAccount(RESOURCE_DUMMY_PREDEFINED_PPA_NAME, USER_SHELDON_USERNAME);

        when("override time to future and recompute user");
        clock.overrideDuration(XmlTypeConverter.createDuration(true, 0, 0, 6, 0, 0, 0));
        recomputeUser(USER_SHELDON_OID);

        then("expected that account exist and is disabled");
        assertSuccess(result);
        assertDummyAccount(RESOURCE_DUMMY_PREDEFINED_PPA_NAME, USER_SHELDON_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_PREDEFINED_PPA_NAME, USER_SHELDON_USERNAME, false);

        assertShadow(
                findShadowByNameViaModel(
                        ShadowKindType.ACCOUNT,
                        "default",
                        USER_SHELDON_USERNAME,
                        getDummyResourceObject(RESOURCE_DUMMY_PREDEFINED_PPA_NAME),
                        null,
                        task,
                        result), "shadow after")
                .display()
                .asShadow()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED);

        when("override time to future and recompute user");
        clock.overrideDuration(XmlTypeConverter.createDuration(true, 0, 0, 6, 0, 0, 0));
        recomputeUser(USER_SHELDON_OID);

        then("expected that account exist and is enabled");
        assertSuccess(result);
        assertDummyAccount(RESOURCE_DUMMY_PREDEFINED_PPA_NAME, USER_SHELDON_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_PREDEFINED_PPA_NAME, USER_SHELDON_USERNAME, true);

        assertShadow(
                findShadowByNameViaModel(
                        ShadowKindType.ACCOUNT,
                        "default",
                        USER_SHELDON_USERNAME,
                        getDummyResourceObject(RESOURCE_DUMMY_PREDEFINED_PPA_NAME),
                        null,
                        task,
                        result), "shadow after")
                .display()
                .asShadow()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
    }

    @Test
    public void test803AllPredefinedAllEvaluators() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        XMLGregorianCalendar validFrom = XmlTypeConverter.createXMLGregorianCalendar(clock.currentTimeMillis());
        validFrom.add(XmlTypeConverter.createDuration(true, 0, 0, 10, 0, 0, 0));
        modifyObjectReplaceProperty(
                UserType.class,
                USER_SHELDON_OID,
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                task,
                result,
                validFrom);
        recomputeUser(USER_SHELDON_OID);

        when("role 'predefined all' is assigned");
        assignRole(USER_SHELDON_OID, ROLE_PREDEFINED_ALL_OID, task, result);

        then("account does not exist");
        assertSuccess(result);
        assertNoDummyAccount(RESOURCE_DUMMY_PREDEFINED_ALL.name, USER_SHELDON_USERNAME);

        when("time is moved to the future and user is recomputed");
        clock.overrideDuration(XmlTypeConverter.createDuration(true, 0, 0, 6, 0, 0, 0));
        recomputeUser(USER_SHELDON_OID);

        then("account exists and is disabled");
        assertSuccess(result);
        assertDummyAccount(RESOURCE_DUMMY_PREDEFINED_ALL.name, USER_SHELDON_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_PREDEFINED_ALL.name, USER_SHELDON_USERNAME, false);

        assertShadow(findShadow(RESOURCE_DUMMY_PREDEFINED_ALL, USER_SHELDON_USERNAME), "shadow after")
                .display()
                .asShadow()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED);

        when("time is moved to the future and user is recomputed again");
        clock.overrideDuration(XmlTypeConverter.createDuration(true, 0, 0, 11, 0, 0, 0));
        recomputeUser(USER_SHELDON_OID);

        then("account exists and is enabled");
        assertSuccess(result);
        assertDummyAccount(RESOURCE_DUMMY_PREDEFINED_ALL.name, USER_SHELDON_USERNAME);
        assertDummyAccountActivation(RESOURCE_DUMMY_PREDEFINED_ALL.name, USER_SHELDON_USERNAME, true);

        assertShadow(findShadow(RESOURCE_DUMMY_PREDEFINED_ALL, USER_SHELDON_USERNAME), "shadow after")
                .display()
                .asShadow()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        when("role is unassigned");
        unassignRole(USER_SHELDON_OID, ROLE_PREDEFINED_ALL_OID, task, result);

        then("account is still there, but disabled");
        assertSuccess(result);
        assertDummyAccountActivation(RESOURCE_DUMMY_PREDEFINED_ALL.name, USER_SHELDON_USERNAME, false);

        assertDelayDeletedAccount(RESOURCE_DUMMY_PREDEFINED_ALL, USER_SHELDON_USERNAME);
    }

    private void assertDelayDeletedAccount(DummyTestResource resource, String userName) throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        then("shadow is disabled, with a pending deletion");
        var shadow = assertShadow(findShadow(resource, userName), "shadow after")
                .display()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED)
                .assertDisableReason(SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION)
                .triggers()
                .assertTriggers(1)
                .end()
                .getObjectable();

        var trigger = shadow.getTrigger().get(0);
        assertThat(trigger.getOriginDescription())
                .as("trigger origin description")
                .contains("Delayed"); // either "DelayedDeleteEvaluator" or "Delayed delete" (fragile)

        when("user is recomputed (to see if the triggers will not multiply)");
        // Not the system time, as this is to be compared with trigger timestamp
        long recomputationTimestamp = clock.currentTimeMillis();
        var userOid = findUserByUsername(userName).getOid();
        recomputeUser(userOid, task, result);

        then("shadow is still disabled, with a pending deletion");
        var shadowAfterRecomputation = assertShadow(findShadow(resource, userName), "shadow after recomputation")
                .display()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED)
                .assertDisableReason(SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION)
                .assertDisableTimestamp(0, recomputationTimestamp) // the timestamp should not be moved by the recomputation
                .triggers()
                .assertTriggers(1)
                .end()
                .getObjectable();
        var triggerAfterRecomputation = shadowAfterRecomputation.getTrigger().get(0);
        assertThat(triggerAfterRecomputation)
                .withFailMessage("Trigger is changed during recomputation: %s vs %s", triggerAfterRecomputation, trigger)
                .isEqualTo(trigger);

        when("time is moved to the future and user is recomputed again");
        clock.overrideDuration(XmlTypeConverter.createDuration(true, 0, 2, 0, 0, 0, 0));
        runTriggerScannerOnDemand(result);

        then("account is gone, because of the delayed delete");
        assertSuccess(result);
        assertNoDummyAccount(resource.name, userName);

        clock.resetOverride();
    }

    private PrismObject<ShadowType> findShadow(DummyTestResource resource, String userName) throws CommonException {
        return findShadowByNameViaModel(
                ShadowKindType.ACCOUNT,
                "default",
                userName,
                getDummyResourceObject(resource.name),
                null,
                getTestTask(),
                getTestOperationResult());
    }

    /**
     * User has linked an account that is already disabled.
     * The disable-instead-of-delete mappings shouldn't try to disable this account again.
     *
     * MID-9154
     */
    @Test
    public void test810LinkingDisabledAccount() throws Exception {
        executeLinkingDisabledAccount(RESOURCE_DUMMY_PREDEFINED_ALL);
    }

    /**
     * As {@link #test810LinkingDisabledAccount()} but with a legacy configuration.
     *
     * MID-9154
     */
    @Test
    public void test815LinkingDisabledAccountLegacy() throws Exception {
        executeLinkingDisabledAccount(RESOURCE_DUMMY_LEGACY);
    }

    /**
     * User has linked an enabled account.
     * The disable-instead-of-delete and delayed-delete mappings should apply.
     *
     * MID-9154
     */
    @Test
    public void test820LinkingEnabledAccount() throws Exception {
        executeLinkingEnabledAccount(RESOURCE_DUMMY_PREDEFINED_ALL);
    }

    /**
     * As {@link #test820LinkingEnabledAccount()} but with a legacy configuration.
     *
     * MID-9154
     */
    @Test
    public void test825LinkingEnabledAccountLegacy() throws Exception {
        executeLinkingEnabledAccount(RESOURCE_DUMMY_LEGACY);
    }

    private void executeLinkingDisabledAccount(DummyTestResource resource) throws Exception {
        var shadow = linkExistingAccount(resource, false);

        assertDelayDeletedAccount(resource, shadow.getName().getOrig());
    }

    private void executeLinkingEnabledAccount(DummyTestResource resource) throws Exception {
        var shadow = linkExistingAccount(resource, true);

        assertDelayDeletedAccount(resource, shadow.getName().getOrig());
    }

    private PrismObject<ShadowType> linkExistingAccount(DummyTestResource resource, boolean enabled) throws Exception {
        var task = getTestTask();
        var result = getTestOperationResult();
        var userName = getTestNameShort();

        given("an account");
        DummyAccount account = resource.controller.addAccount(userName);
        account.setEnabled(enabled);

        var matchingShadows = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(resource.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .and().item(ShadowType.F_ATTRIBUTES, ICFS_NAME).eq(userName)
                        .build(),
                null,
                task, result);
        var shadow = MiscUtil.extractSingletonRequired(matchingShadows);

        when("a disabled user with that account is created");
        UserType user = new UserType()
                .name(userName)
                .activation(new ActivationType()
                        .administrativeStatus(ActivationStatusType.DISABLED))
                .linkRef(shadow.getOid(), ShadowType.COMPLEX_TYPE);
        addObject(user, task, result);
        return shadow;
    }

    /**
     * A suspended user is being archived. His account should be disabled and delayed-deleted. MID-9220.
     */
    @Test
    public void test830DelayedDeleteWhenArchivingSuspendedUser() throws CommonException {
        testDelayedDeleteWhenArchivingSuspendedUser(RESOURCE_DUMMY_PREDEFINED_ALL);
    }

    /**
     * As {@link #test830DelayedDeleteWhenArchivingSuspendedUser()} but using legacy configuration. MID-9220.
     */
    @Test
    public void test835DelayedDeleteWhenArchivingSuspendedUserLegacy() throws CommonException {
        testDelayedDeleteWhenArchivingSuspendedUser(RESOURCE_DUMMY_LEGACY);
    }

    private void testDelayedDeleteWhenArchivingSuspendedUser(DummyTestResource resource) throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        given("a regular user");
        UserType user = new UserType()
                .name(getTestNameShort())
                .assignment(new AssignmentType()
                        .construction(resource.defaultConstruction()))
                .credentials(new CredentialsType()
                        .password(new PasswordType()
                                .value(new ProtectedStringType().clearValue("abcdef123456"))));
        var userOid = addObject(user, task, result);

        assertUser(userOid, "after creation")
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        and("he is suspended now");
        long suspensionStart = System.currentTimeMillis();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LIFECYCLE_STATE)
                        .replace(SchemaConstants.LIFECYCLE_SUSPENDED)
                        .asObjectDelta(userOid),
                null, task, result);
        long suspensionEnd = System.currentTimeMillis();

        assertUser(userOid, "after suspending")
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED)
                .assertDisableReason(SchemaConstants.MODEL_DISABLE_REASON_MAPPED)
                .assertDisableTimestamp(suspensionStart, suspensionEnd);

        when("he is archived");
        long archivalStart = System.currentTimeMillis();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_LIFECYCLE_STATE)
                        .replace(SchemaConstants.LIFECYCLE_ARCHIVED)
                        .asObjectDelta(userOid),
                null, task, result);
        long archivalEnd = System.currentTimeMillis();

        assertUser(userOid, "after archiving")
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED)
                .assertDisableReason(SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION)
                .assertDisableTimestamp(archivalStart, archivalEnd) // maybe questionable
                .triggers()
                .assertTriggers(1);
    }

    private String rumFrom(String locality) {
        return "rum from " + locality;
    }
}
