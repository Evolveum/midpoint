/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.test.DummyTestResource;

import com.evolveum.midpoint.test.TestObject;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIteration extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/iteration");

    // Plain iteration, no iteration expressions
    private static final File RESOURCE_DUMMY_PINK_FILE = new File(TEST_DIR, "resource-dummy-pink.xml");
    private static final String RESOURCE_DUMMY_PINK_OID = "10000000-0000-0000-0000-00000000a104";
    private static final String RESOURCE_DUMMY_PINK_NAME = "pink";

    // Iteration with token expression, pre-iteration condition and post-iteration condition
    private static final File RESOURCE_DUMMY_VIOLET_FILE = new File(TEST_DIR, "resource-dummy-violet.xml");
    private static final String RESOURCE_DUMMY_VIOLET_OID = "10000000-0000-0000-0000-00000000a204";
    private static final String RESOURCE_DUMMY_VIOLET_NAME = "violet";

    // similar to violet but it works in the inbound direction
    private static final File RESOURCE_DUMMY_DARK_VIOLET_FILE = new File(TEST_DIR, "resource-dummy-dark-violet.xml");
    private static final String RESOURCE_DUMMY_DARK_VIOLET_OID = "10000000-0000-0000-0000-0000000da204";
    private static final String RESOURCE_DUMMY_DARK_VIOLET_NAME = "darkViolet";

    // iteration, token expression, post-iteration condition that invokes isUniquAccountValue()
    private static final File RESOURCE_DUMMY_MAGENTA_FILE = new File(TEST_DIR, "resource-dummy-magenta.xml");
    private static final String RESOURCE_DUMMY_MAGENTA_OID = "10000000-0000-0000-0000-00000000a304";
    private static final String RESOURCE_DUMMY_MAGENTA_NAME = "magenta";

    // Plain iteration (no expressions). Has synchronization block.
    private static final File RESOURCE_DUMMY_FUCHSIA_FILE = new File(TEST_DIR, "resource-dummy-fuchsia.xml");
    private static final String RESOURCE_DUMMY_FUCHSIA_OID = "10000000-0000-0000-0000-0000000dd204";
    private static final String RESOURCE_DUMMY_FUCHSIA_NAME = "fuchsia";

    // Source for "changing template" test (test820)
    private static final File RESOURCE_DUMMY_ASSOCIATE_FILE = new File(TEST_DIR, "resource-dummy-associate.xml");
    private static final String RESOURCE_DUMMY_ASSOCIATE_OID = "18c109fd-1287-4a9b-9086-9ab878931ac0";
    private static final String RESOURCE_DUMMY_ASSOCIATE_NAME = "associate";

    private static final File TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_FILE = new File(TEST_DIR, "task-dumy-dark-violet-livesync.xml");
    private static final String TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID = "10000000-0000-0000-5555-555500da0204";

    // Iteration with token expression and pre- and post-condition. Sequential suffix.
    // Configured in dark-violet dummy resource as account sync template
    private static final File USER_TEMPLATE_ITERATION_FILE = new File(TEST_DIR, "user-template-iteration.xml");
    private static final String USER_TEMPLATE_ITERATION_OID = "10000000-0000-0000-0000-0000000d0002";

    // Iteration that generates random suffix. Token expression and post- and pre-conditions.
    private static final File USER_TEMPLATE_ITERATION_RANDOM_FILE = new File(TEST_DIR, "user-template-iteration-random.xml");

    // Iteration with token expression (sequential) and post-condition that checks for e-mail uniquness.
    private static final File USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_FILE = new File(TEST_DIR, "user-template-iteration-unique-email.xml");
    private static final String USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_OID = "10000000-0000-0000-0000-0000000d0004";

    // Simple focus iteration (to be used with "changing template" test)
    private static final File USER_TEMPLATE_ITERATION_ASSOCIATE_FILE = new File(TEST_DIR, "user-template-iteration-associate.xml");
    private static final String USER_TEMPLATE_ITERATION_ASSOCIATE_OID = "c0ee8964-0d2a-45d5-8a8e-6ee4f31e1c12";

    // Used e.g. in test910
    private static final TestObject<ObjectTemplateType> USER_TEMPLATE_SIMPLE_ITERATION = TestObject.file(
            TEST_DIR, "user-template-simple-iteration.xml", "1f104a6b-f7f1-4a69-a68b-c71fa2f9f1ac");

    private static final String USER_ANGELICA_NAME = "angelica";
    private static final String ACCOUNT_SPARROW_NAME = "sparrow";

    private static final String USER_DEWATT_NAME = "dewatt";
    private static final String ACCOUNT_DEWATT_NAME = "DeWatt";

    private static final String USER_LARGO_NAME = "largo";
    private static final String ACCOUNT_LARGO_DUMMY_USERNAME = "largo";

    private static final String DESCRIPTION_RUM = "Where's the rum?";

    private static final String USER_JACK_RENAMED_NAME = "cptjack";

    private static final String ACCOUNT_LECHUCK_USERNAME = "lechuck";
    private static final String LECHUCK_FULLNAME = "LeChuck";
    private static final String LE_CHUCK_FULLNAME = "Le-Chuck";
    private static final String ACCOUNT_CHARLES_USERNAME = "charles";
    private static final String ACCOUNT_SHINETOP_USERNAME = "shinetop";
    private static final String ACCOUNT_LE_CHUCK_USERNAME = "le-chuck";
    private static final String CHUCKIE_FULLNAME = "Chuckie";

    private static final String ACCOUNT_MATUSALEM_USERNAME = "matusalem";
    private static final String ACCOUNT_DIPLOMATICO_USERNAME = "diplomatico";
    private static final String ACCOUNT_MILLONARIO_USERNAME = "millonario";
    private static final String RUM_FULLNAME = "Rum";
    private static final String RON_FULLNAME = "Ron";

    private static final File USER_JUPITER_FILE = new File(TEST_DIR, "user-jupiter.xml");
    private static final String USER_JUPITER_NAME = "jupiter";
    private static final String ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME = "Jupiter Jones";

    private static final File USER_ALFRED_FILE = new File(TEST_DIR, "user-alfred.xml");
    private static final String USER_ALFRED_NAME = "alfred";

    private static final File ACCOUNT_ALFRED_FILE = new File(TEST_DIR, "account-alfred.xml");

    private static final String USER_BOB_NAME = "bob";

    private static final String USER_ALFREDO_FETTUCINI_USERNAME = "afettucini";
    private static final String USER_ALFREDO_FETTUCINI_GIVEN_NAME = "Alfredo";
    private static final String USER_ALFREDO_FETTUCINI_FAMILY_NAME = "Fettucini";

    private static final String USER_BILL_FETTUCINI_USERNAME = "bfettucini";
    private static final String USER_BILL_FETTUCINI_GIVEN_NAME = "Bill";
    private static final String USER_BILL_FETTUCINI_FAMILY_NAME = "Fettucini";

    private static final String USER_FETTUCINI_NICKNAME = "fetty";

    private static final String EMAIL_SUFFIX = "@example.com";

    private static final DummyTestResource RESOURCE_DUMMY_ASSOCIATIONS = new DummyTestResource(
            TEST_DIR, "resource-dummy-associations.xml", "64ae70db-2b2c-418e-b2bd-d167a28cfbd3",
            "associations");
    private static final TestObject<RoleType> METAROLE_DUMMY_ASSOCIATIONS = TestObject.file(
            TEST_DIR, "metarole-dummy-associations.xml", "fc07a007-fdd4-44d3-99cf-d57b4df9509d");
    private static final TestObject<RoleType> ROLE_CS_101 = TestObject.file(
            TEST_DIR, "role-cs-101.xml", "4e8170d7-31e6-4c5f-b3c6-ad9f7c4bbb62");

    private static final TestObject<ObjectTemplateType> USER_TEMPLATE_ITERATION_BUT_CONSTANT_NAME = TestObject.file(
            TEST_DIR, "user-template-iteration-but-constant-name.xml", "2ebfbbf4-e680-455d-a64f-a49d779a4a53");

    private String jupiterUserOid;

    private String iterationTokenDiplomatico;
    private String iterationTokenMillonario;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_PINK_NAME,
                RESOURCE_DUMMY_PINK_FILE, RESOURCE_DUMMY_PINK_OID, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_VIOLET_NAME,
                RESOURCE_DUMMY_VIOLET_FILE, RESOURCE_DUMMY_VIOLET_OID, initTask, initResult);

        DummyResourceContoller darkVioletCtl = initDummyResourcePirate(RESOURCE_DUMMY_DARK_VIOLET_NAME,
                RESOURCE_DUMMY_DARK_VIOLET_FILE, RESOURCE_DUMMY_DARK_VIOLET_OID, initTask, initResult);
        darkVioletCtl.getDummyResource().setSyncStyle(DummySyncStyle.SMART);

        initDummyResourcePirate(RESOURCE_DUMMY_MAGENTA_NAME,
                RESOURCE_DUMMY_MAGENTA_FILE, RESOURCE_DUMMY_MAGENTA_OID, initTask, initResult);

        initDummyResourceAd(RESOURCE_DUMMY_FUCHSIA_NAME,
                RESOURCE_DUMMY_FUCHSIA_FILE, RESOURCE_DUMMY_FUCHSIA_OID, initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_ASSOCIATE_NAME,
                RESOURCE_DUMMY_ASSOCIATE_FILE, RESOURCE_DUMMY_ASSOCIATE_OID, initTask, initResult);

        addObject(USER_TEMPLATE_ITERATION_FILE);
        addObject(USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_FILE);
        addObject(USER_TEMPLATE_ITERATION_ASSOCIATE_FILE);

        addObject(USER_LARGO_FILE);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        USER_TEMPLATE_SIMPLE_ITERATION.init(this, initTask, initResult);

        RESOURCE_DUMMY_ASSOCIATIONS.initAndTest(this, initTask, initResult);
        METAROLE_DUMMY_ASSOCIATIONS.init(this, initTask, initResult);
        ROLE_CS_101.init(this, initTask, initResult);

        USER_TEMPLATE_ITERATION_BUT_CONSTANT_NAME.init(this, initTask, initResult);
    }

    /**
     * The default dummy instance will not iterate. It has correlation rule which will link the account instead.
     */
    @Test
    public void test100JackAssignAccountDummyConflicting() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Sparrow");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Tortuga");
        getDummyResource().addAccount(account);
        repoAddObject(
                createRepoShadow(getDummyResourceObject(), ACCOUNT_JACK_DUMMY_USERNAME).getPrismObject(),
                result);

        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_OID, null, true);

        // WHEN
        when();
        executeChanges(accountAssignmentUserDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertDummyAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test200JackAssignAccountDummyPinkConflicting() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Pinky");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Red Sea");
        getDummyResource(RESOURCE_DUMMY_PINK_NAME).addAccount(account);
        repoAddObject(
                createRepoShadow(getDummyResourceObject(RESOURCE_DUMMY_PINK_NAME), ACCOUNT_JACK_DUMMY_USERNAME).getPrismObject(),
                result);

        // assignment with weapon := 'pistol' (test for
        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        AssignmentType assignmentType = createConstructionAssignment(RESOURCE_DUMMY_PINK_OID, ShadowKindType.ACCOUNT, null);
        ConstructionType constructionType = assignmentType.getConstruction();
        ResourceAttributeDefinitionType attributeDefinitionType = new ResourceAttributeDefinitionType();
        attributeDefinitionType.setRef(new ItemPathType(ItemPath.create(getDummyResourceController(RESOURCE_DUMMY_PINK_NAME).getAttributeWeaponQName())));
        MappingType mappingType = new MappingType();
        mappingType.setStrength(MappingStrengthType.STRONG);
        ExpressionType expressionType = new ExpressionType();
        expressionType.getExpressionEvaluator().add(new ObjectFactory().createValue(RawType.create("pistol")));
        mappingType.setExpression(expressionType);
        attributeDefinitionType.setOutbound(mappingType);
        constructionType.getAttribute().add(attributeDefinitionType);
        modifications.add(createAssignmentModification(assignmentType, true));
        ObjectDelta<UserType> accountAssignmentUserDelta = prismContext.deltaFactory().object()
                .createModifyDelta(USER_JACK_OID, modifications, UserType.class);

        // WHEN
        when();
        executeChanges(accountAssignmentUserDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLiveLinks(userJack, 2);
        assertAccount(userJack, RESOURCE_DUMMY_OID);
        assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);

        String accountPinkOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_PINK_OID);

        // Check shadow
        var accountPinkShadow = getShadowRepo(accountPinkOid);
        assertAccountShadowRepo(accountPinkShadow, accountPinkOid, "jack1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // Check account
        PrismObject<ShadowType> accountPinkModel = modelService.getObject(ShadowType.class, accountPinkOid, null, task, result);
        assertAccountShadowModel(accountPinkModel, accountPinkOid, "jack1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));
        display("accountPinkModel", accountPinkModel);
        PrismAsserts.assertPropertyValue(
                accountPinkModel, DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_PATH, "pistol");

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Pinky", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "jack1", "Jack Sparrow", true);

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
     * Test the normal case. Just to be sure the default iteration algorithm works well.
     */
    @Test
    public void test210GuybrushAssignAccountDummyPink() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_PINK_OID, null, true);

        // WHEN
        when();
        executeChanges(accountAssignmentUserDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
        display("User after change execution", userGuybrush);
        assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
        assertLiveLinks(userGuybrush, 2);
        // Guybrush had dummy account before
        assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
        assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);

        String accountPinkOid = getLiveLinkRefOid(userGuybrush, RESOURCE_DUMMY_PINK_OID);

        // Check shadow
        var accountPinkShadow = getShadowRepo(accountPinkOid);
        assertAccountShadowRepo(accountPinkShadow, accountPinkOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // Check account
        PrismObject<ShadowType> accountPinkModel = modelService.getObject(ShadowType.class, accountPinkOid, null, task, result);
        assertAccountShadowModel(accountPinkModel, accountPinkOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // The new account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(4);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test220DeWattAssignAccountDummyPinkCaseIgnore() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userDeWatt = createUser(USER_DEWATT_NAME, "Augustus DeWatt", true);
        addObject(userDeWatt);
        String userDeWattOid = userDeWatt.getOid();

        var accountDeWatt = createAccount(getDummyResourceObject(RESOURCE_DUMMY_PINK_NAME), ACCOUNT_DEWATT_NAME, true);
        addAttributeToShadow(
                accountDeWatt, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Augustus DeWatt");
        addObject(accountDeWatt);

        // precondition
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_DEWATT_NAME, "Augustus DeWatt", true);

        var accountAssignmentUserDelta =
                createAccountAssignmentUserDelta(userDeWattOid, RESOURCE_DUMMY_PINK_OID, null, true);

        dummyAuditService.clear();

        // WHEN
        when();
        executeChanges(accountAssignmentUserDelta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userDeWattAfter = getUser(userDeWattOid);
        display("User after change execution", userDeWattAfter);
        assertUser(userDeWattAfter, userDeWattOid, USER_DEWATT_NAME, "Augustus DeWatt", null, null);
        assertLiveLinks(userDeWattAfter, 1);
        assertAccount(userDeWattAfter, RESOURCE_DUMMY_PINK_OID);

        String accountPinkOid = getLiveLinkRefOid(userDeWattAfter, RESOURCE_DUMMY_PINK_OID);

        // Check shadow
        var accountPinkShadow = getShadowRepo(accountPinkOid);
        assertAccountShadowRepo(accountPinkShadow, accountPinkOid, USER_DEWATT_NAME + "1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // Check account
        PrismObject<ShadowType> accountPinkModel = modelService.getObject(ShadowType.class, accountPinkOid, null, task, result);
        assertAccountShadowModel(accountPinkModel, accountPinkOid, USER_DEWATT_NAME + "1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // Old account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_DEWATT_NAME, "Augustus DeWatt", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_DEWATT_NAME + "1", "Augustus DeWatt", true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        // THE FOLLOWING WILL APPLY AFTER MID-10050 IS RESOLVED, NOW THERE ARE ONLY 2 RECORDS
        // When caching is enabled, the weak credentials mapping gets executed in wave 1, providing a generated password
        // along with a separate audit record. Actually, there is little we can do about this; it is inherently nondeterministic
        // behavior of inbound mappings.
        //dummyAuditService.assertRecords(InternalsConfig.isShadowCachingFullByDefault() ? 3 : 2);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test230ScroogeAddAccountDummyConflictingNoShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Make sure there is a conflicting account and NO shadow for it
        DummyAccount account = new DummyAccount("scrooge");
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Scrooge Pinky");
        getDummyResource(RESOURCE_DUMMY_PINK_NAME).addAccount(account);

        PrismObject<UserType> userScrooge = createUser("scrooge", "Scrooge McDuck", true);
        PrismObject<ShadowType> newPinkyShadow =
                createShadow(getDummyResourceType(RESOURCE_DUMMY_PINK_NAME).asPrismObject(), null, null)
                        .getPrismObject();
        ObjectReferenceType linkRef = new ObjectReferenceType();
        linkRef.asReferenceValue().setObject(newPinkyShadow);
        userScrooge.asObjectable().getLinkRef().add(linkRef);

        // WHEN
        when();
        executeChanges(DeltaFactory.Object.createAddDelta(userScrooge), null, task, result);

        // THEN
        then();
        assertSuccess(result, 2);

        PrismObject<UserType> userScroogeAfter = findUserByUsername("scrooge");
        display("User after change execution", userScroogeAfter);
        assertUser(userScroogeAfter, null, "scrooge", "Scrooge McDuck", null, null, null);
        String accountOid = getSingleLinkOid(userScroogeAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, "scrooge1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "scrooge1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "scrooge1", "Scrooge McDuck", true);
    }

    /**
     * This tests a situation where the ObjectAlreadyExists conflict occurs because of some misconfiguration.
     * For example, the reason of the conflict is not the naming attribute itself.
     */
    @Test
    public void test235HackerAddAccountDummyEternalConflict() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> userJoeHacker = createUser("hacker", "Joe Hacker", true);
        PrismObject<ShadowType> newPinkyShadow =
                createShadow(getDummyResourceObject(RESOURCE_DUMMY_PINK_NAME), null, null).getPrismObject();
        ObjectReferenceType linkRef = new ObjectReferenceType();
        linkRef.asReferenceValue().setObject(newPinkyShadow);
        userJoeHacker.asObjectable().getLinkRef().add(linkRef);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        deltas.add(DeltaFactory.Object.createAddDelta(userJoeHacker));

        // WHEN
        when();

        // wrong behavior is throwing "java.lang.IllegalStateException: Model operation took too many clicks (limit is 30). Is there a cycle?"
        // good behavior is reporting ObjectAlreadyExistsException here
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        display("Result", result);
        TestUtil.assertPartialError(result);
        String exp = "hacker is forbidden to use as an object name";
        String msg = result.getMessage();
        if (msg == null) {
            msg = "(null)";
        }
        assertTrue("result message is does not contain expected '" + exp + "', instead it is: '" + msg + "'", msg.contains(exp));

        and("details are captured in the exception message (MID-6712)");
        assertThat(msg).as("exception message")
                .contains("Dummy Resource Pink")
                .contains("AccountObjectClass")
                .contains("Default Account")
                .contains("DummyConnector");

        PrismObject<UserType> userHackerAfter = findUserByUsername("hacker");
        display("User after change execution", userHackerAfter);
        assertUser(userHackerAfter, null, "hacker", "Joe Hacker", null, null, null);
        assertNoLinkedAccount(userHackerAfter);

        assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, "hacker");        // just in case ;)
    }

    @Test
    public void test240LargoAssignAccountDummyConflictingNoShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Make sure there is a conflicting account and NO shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_LARGO_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Largo Pinky");
        getDummyResource(RESOURCE_DUMMY_PINK_NAME).addAccount(account);

        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(
                USER_LARGO_OID, RESOURCE_DUMMY_PINK_OID, null, true);

        // WHEN
        when();
        executeChanges(accountAssignmentUserDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result, 2);

        PrismObject<UserType> userLargo = getUser(USER_LARGO_OID);
        display("User after change execution", userLargo);
        assertUserLargo(userLargo);
        String accountOid = getSingleLinkOid(userLargo);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        assertAccountShadowRepo(accountShadow, accountOid, "largo1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "largo1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "largo1", null, true);

        // Check audit
        // USER Largo MODIFY(add-assignment):   request + execution (focus(assignment) + account/failed) + execution (focus(linkRef) / account/OK)
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Doing the same as test240 (conflict without pre-existing shadow -> surprising the model with AlreadyExists,
     * causing re-running given wave in model. But this time doing this on resource that has synchronization
     * defined (fuchsia).
     * <p>
     * test260: first case - existing account corresponds to user being created
     * <p>
     * 1) manually create account Jupiter Jones (no shadow!), description = "jupiter"
     * 2) create user Jupiter Jones (name = jupiter)
     * <p>
     * Create account operation should fail, account should be synchronized back to repo (creating the user!), and
     * model should clean it up somehow...
     */
    @Test
    public void test260JupiterConflictNoShadowSyncBack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Make sure there is a conflicting account and NO shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME);        // Jupiter Jones
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, USER_JUPITER_NAME);                // jupiter
        getDummyResource(RESOURCE_DUMMY_FUCHSIA_NAME).addAccount(account);

        PrismObject<UserType> userJupiter = PrismTestUtil.parseObject(USER_JUPITER_FILE);

        // WHEN
        when();
        executeChanges(DeltaFactory.Object.createAddDelta(userJupiter), null, task, result);

        // THEN
        then();
        assertSuccess(result, 2);

        PrismObject<UserType> userJupiterAfter = findUserByUsername(USER_JUPITER_NAME);        // jupiter
        display("User after change execution", userJupiterAfter);
        assertUserJupiter(userJupiterAfter);
        jupiterUserOid = userJupiterAfter.getOid();
        String accountOid = getSingleLinkOid(userJupiterAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        display("Account shadow from repo", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
        assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, null, true);

        // TODO Check audit
        displayDumpable("Audit", dummyAuditService);
    }

    // remove the assignment, shadow and account to prepare for following tests
    @Test
    public void test262JupiterCleanup() throws Exception {
        cleanUpJupiter();
    }

    private void cleanUpJupiter()
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException, SchemaViolationException,
            ConflictException, InterruptedException {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        ObjectDelta<UserType> delta = createAccountAssignmentUserDelta(jupiterUserOid, RESOURCE_DUMMY_FUCHSIA_OID, "default", false);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJupiterAfter = findUserByUsername(USER_JUPITER_NAME);        // jupiter
        display("User after change execution", userJupiterAfter);
        assertUserJupiter(userJupiterAfter);

        assertEquals("Unexpected number of linkRefs", 0, userJupiterAfter.asObjectable().getLinkRef().size());
        assertNull("Unexpected account for jupiter", getDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME));

        // TODO Check audit
        displayDumpable("Audit", dummyAuditService);
    }

    /**
     * Doing the same as test260. But this time assigns the account in separate step.
     */
    @Test
    public void test264JupiterConflictNoShadowSyncBackSeparate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Make sure there is a conflicting account and NO shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME);        // Jupiter Jones
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, USER_JUPITER_NAME);                // jupiter
        getDummyResource(RESOURCE_DUMMY_FUCHSIA_NAME).addAccount(account);

        ObjectDelta<UserType> delta = createAccountAssignmentUserDelta(jupiterUserOid, RESOURCE_DUMMY_FUCHSIA_OID, "default", true);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result, 2);

        PrismObject<UserType> userJupiterAfter = findUserByUsername(USER_JUPITER_NAME);        // jupiter
        display("User after change execution", userJupiterAfter);
        assertUserJupiter(userJupiterAfter);
        jupiterUserOid = userJupiterAfter.getOid();
        String accountOid = getSingleLinkOid(userJupiterAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        display("Account shadow from repo", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
        assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME, null, true);

        // TODO Check audit
        displayDumpable("Audit", dummyAuditService);
    }

    // remove the assignment, shadow and account to prepare for following tests
    @Test
    public void test266JupiterCleanupAgain() throws Exception {
        cleanUpJupiter();
    }

    /**
     * Doing the same as test264, but the conflicting account does not belong to the user being created
     * (and causes another user to be added).
     */
    @Test
    public void test270JupiterConflictOtherNoShadowSyncBack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Make sure there is a conflicting account and NO shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JUPITER_DUMMY_FUCHSIA_USERNAME);        // Jupiter Jones
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "jupiter0");        // different from our jupiter
        getDummyResource(RESOURCE_DUMMY_FUCHSIA_NAME).addAccount(account);

        ObjectDelta<UserType> delta = createAccountAssignmentUserDelta(jupiterUserOid, RESOURCE_DUMMY_FUCHSIA_OID, "default", true);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result, 2);

        PrismObject<UserType> userJupiterAfter = findUserByUsername(USER_JUPITER_NAME);        // jupiter
        display("User after change execution", userJupiterAfter);
        assertUserJupiter(userJupiterAfter);
        jupiterUserOid = userJupiterAfter.getOid();
        String accountOid = getSingleLinkOid(userJupiterAfter);

        // Check shadow & account
        var accountShadow = getShadowRepo(accountOid);
        display("Account shadow from repo", accountShadow);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Account shadow from model", accountModel);

        assertAccountShadowRepo(accountShadow, accountOid, "Jupiter Jones1", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));
        assertAccountShadowModel(accountModel, accountOid, "Jupiter Jones1", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
        assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Jupiter Jones1", null, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_FUCHSIA_NAME, "Jupiter Jones1", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "jupiter");

        // Now check the other (newly created) user, jupiter0
        PrismObject<UserType> userJupiter0 = findUserByUsername("jupiter0");
        display("Newly created jupiter0 user", userJupiter0);
        assertUser(userJupiter0, null, "jupiter0", "Jupiter Jones", null, null, null);
        String accountOidJ0 = getSingleLinkOid(userJupiter0);

        // Check shadow
        var accountShadowJ0 = getShadowRepo(accountOidJ0);
        display("Account shadow from repo (jupiter0)", accountShadowJ0);
        assertAccountShadowRepo(accountShadowJ0, accountOidJ0, "Jupiter Jones", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account
        PrismObject<ShadowType> accountModelJ0 = modelService.getObject(ShadowType.class, accountOidJ0, null, task, result);
        assertAccountShadowModel(accountModelJ0, accountOidJ0, "Jupiter Jones", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
        assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Jupiter Jones", null, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_FUCHSIA_NAME, "Jupiter Jones", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "jupiter0");

        // TODO Check audit
        displayDumpable("Audit", dummyAuditService);
    }

    private void assertUserJupiter(PrismObject<UserType> user) {
        assertUser(user, null, USER_JUPITER_NAME, "Jupiter Jones", "Jupiter", "Jones", null);
    }

    /**
     * Same as test240 (conflict with no shadow), but including rename operation.
     */
    @Test
    public void test280RenameBobNoShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBob = createUser(USER_BOB_NAME, "Bob Andrews", true);
        addObject(userBob);
        String userBobOid = userBob.getOid();

        // Make sure there is a conflicting account and NO shadow for it
        DummyAccount account = new DummyAccount("bobby");
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Bobby Pinky");
        getDummyResource(RESOURCE_DUMMY_PINK_NAME).addAccount(account);

        // preconditions
        assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_BOB_NAME);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "bobby", "Bobby Pinky", true);

        // prepare change

        ObjectDelta<UserType> objectDelta = createAccountAssignmentUserDelta(userBobOid, RESOURCE_DUMMY_PINK_OID, "default", true);
        objectDelta.addModification(createUserPropertyReplaceModification(UserType.F_NAME, new PolyString("bobby")));    // will conflict with Bobby Pinky
        objectDelta.addModification(createUserPropertyReplaceModification(UserType.F_FULL_NAME, new PolyString("Bobby Andrews")));

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        deltas.add(objectDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        PrismObject<UserType> userBobAfter = getUser(userBobOid);
        display("User after change execution", userBobAfter);
        assertUser(userBobAfter, userBobOid, "bobby", "Bobby Andrews", null, null, null);
        String accountOid = getSingleLinkOid(userBobAfter);

        // Check shadow & account
        var accountShadow = getShadowRepo(accountOid);
        display("Account shadow from repo", accountShadow);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Account shadow from model", accountModel);

        assertAccountShadowRepo(accountShadow, accountOid, "bobby1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));
        assertAccountShadowModel(accountModel, accountOid, "bobby1", getDummyResourceType(RESOURCE_DUMMY_PINK_NAME));

        // THEN
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "bobby", "Bobby Pinky", true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, "bobby1", "Bobby Andrews", true);
        assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, "bob");
    }

    /**
     * Same as test280 (conflict with no shadow with rename), but including synchronization.
     */
    @Test
    public void test282RenamePeterNoShadowSync() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userPeter = createUser("peter", "Peter Crenshaw", true);
        addObject(userPeter);
        String userPeterOid = userPeter.getOid();

        // Make sure there is a conflicting account and NO shadow for it
        DummyAccount account = new DummyAccount("Pete Crenshaw");                // will conflict after rename
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "pete0");
        getDummyResource(RESOURCE_DUMMY_FUCHSIA_NAME).addAccount(account);

        // preconditions
        assertNoDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Peter Crenshaw");
        assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw", null, true);        // conflicting account (pete0)

        // prepare change

        ObjectDelta<UserType> objectDelta = createAccountAssignmentUserDelta(userPeterOid, RESOURCE_DUMMY_FUCHSIA_OID, "default", true);
        objectDelta.addModification(createUserPropertyReplaceModification(UserType.F_NAME, new PolyString("pete")));
        objectDelta.addModification(createUserPropertyReplaceModification(UserType.F_FULL_NAME, new PolyString("Pete Crenshaw")));

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        deltas.add(objectDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        PrismObject<UserType> userPeteAfter = getUser(userPeterOid);
        display("User after change execution", userPeteAfter);
        assertUser(userPeteAfter, userPeterOid, "pete", "Pete Crenshaw", null, null, null);
        String accountOid = getSingleLinkOid(userPeteAfter);

        // Check shadow & account
        var accountShadow = getShadowRepo(accountOid);
        display("Account shadow from repo", accountShadow);
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Account shadow from model", accountModel);

        assertAccountShadowRepo(accountShadow, accountOid, "Pete Crenshaw1", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));
        assertAccountShadowModel(accountModel, accountOid, "Pete Crenshaw1", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Now check the other (newly created) user, pete0
        PrismObject<UserType> userPete0 = findUserByUsername("pete0");
        display("Newly created pete0 user", userPete0);
        assertUser(userPete0, null, "pete0", "Pete Crenshaw", null, null, null);
        String accountOidP0 = getSingleLinkOid(userPete0);

        // Check shadow
        var accountShadowP0 = getShadowRepo(accountOidP0);
        display("Account shadow from repo (pete0)", accountShadowP0);
        assertAccountShadowRepo(accountShadowP0, accountOidP0, "Pete Crenshaw", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account
        PrismObject<ShadowType> accountModelP0 = modelService.getObject(ShadowType.class, accountOidP0, null, task, result);
        display("Account shadow from model (pete0)", accountModelP0);
        assertAccountShadowModel(accountModelP0, accountOidP0, "Pete Crenshaw", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // accounts on the resource
        assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw", null, true);            // pete0
        assertDummyAccountAttribute(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "pete0");
        assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw1", null, true);            // pete
        assertDummyAccountAttribute(RESOURCE_DUMMY_FUCHSIA_NAME, "Pete Crenshaw1", DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "pete");
        assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, "peter");
    }

    // as with jupiter, but ADD instead of ASSIGN account
    @Test
    public void test290AlfredConflictNoShadowSyncBackAdd() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Make sure there is a conflicting account and NO shadow for it
        DummyAccount account = new DummyAccount("Alfred Hitchcock");
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_AD_SAM_ACCOUNT_NAME_NAME, "alfred");
        getDummyResource(RESOURCE_DUMMY_FUCHSIA_NAME).addAccount(account);

        PrismObject<UserType> userAlfred = PrismTestUtil.parseObject(USER_ALFRED_FILE);
        PrismObject<ShadowType> accountAlfred = PrismTestUtil.parseObject(ACCOUNT_ALFRED_FILE);
        ObjectReferenceType linkRef = new ObjectReferenceType();
        linkRef.asReferenceValue().setObject(accountAlfred);
        userAlfred.asObjectable().getLinkRef().add(linkRef);
        ObjectDelta<UserType> delta = DeltaFactory.Object.createAddDelta(userAlfred);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result, 2);

        PrismObject<UserType> userAlfredAfter = findUserByUsername(USER_ALFRED_NAME);        // alfred
        display("User after change execution", userAlfredAfter);
        assertUser(userAlfredAfter, null, USER_ALFRED_NAME, "Alfred Hitchcock", "Alfred", "Hitchcock", null);
        String accountOid = getSingleLinkOid(userAlfredAfter);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
        display("Account shadow from repo", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, "Alfred Hitchcock", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "Alfred Hitchcock", getDummyResourceType(RESOURCE_DUMMY_FUCHSIA_NAME));

        // Check account in dummy resource (actually, the fullname attribute does not exist but it's OK)
        assertDummyAccount(RESOURCE_DUMMY_FUCHSIA_NAME, "Alfred Hitchcock", null, true);
    }

    @Test
    public void test300JackAssignAccountDummyVioletConflicting() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // Make sure there is a conflicting account and also a shadow for it
        DummyAccount account = new DummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Jack Violet");
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Sea of Lavender");
        getDummyResource(RESOURCE_DUMMY_VIOLET_NAME).addAccount(account);
        repoAddObject(
                createRepoShadow(getDummyResourceObject(RESOURCE_DUMMY_VIOLET_NAME), ACCOUNT_JACK_DUMMY_USERNAME).getPrismObject(),
                result);

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_VIOLET_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLiveLinks(userJack, 3);
        assertAccount(userJack, RESOURCE_DUMMY_OID);
        assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
        assertAccount(userJack, RESOURCE_DUMMY_VIOLET_OID);

        String accountVioletOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_VIOLET_OID);

        // Check shadow
        var accountVioletShadow = getShadowRepo(accountVioletOid);
        assertAccountShadowRepo(accountVioletShadow, accountVioletOid, "jack.1", getDummyResourceType(RESOURCE_DUMMY_VIOLET_NAME));

        // Check account
        PrismObject<ShadowType> accountVioletModel = modelService.getObject(ShadowType.class, accountVioletOid, null, task, result);
        assertAccountShadowModel(accountVioletModel, accountVioletOid, "jack.1", getDummyResourceType(RESOURCE_DUMMY_VIOLET_NAME));

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Violet", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "jack.1", "Jack Sparrow", true);

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
    public void test350GuybrushAssignAccountDummyViolet() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_VIOLET_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
        display("User after change execution", userGuybrush);
        assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
        assertLiveLinks(userGuybrush, 3);
        assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
        assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
        assertAccount(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);

        String accountVioletOid = getLiveLinkRefOid(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);

        // Check shadow
        var accountVioletShadow = getShadowRepo(accountVioletOid);
        assertAccountShadowRepo(accountVioletShadow, accountVioletOid, "guybrush.3", getDummyResourceType(RESOURCE_DUMMY_VIOLET_NAME));

        // Check account
        PrismObject<ShadowType> accountVioletModel = modelService.getObject(ShadowType.class, accountVioletOid, null, task, result);
        assertAccountShadowModel(accountVioletModel, accountVioletOid, "guybrush.3", getDummyResourceType(RESOURCE_DUMMY_VIOLET_NAME));

        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "guybrush.3", "Guybrush Threepwood", true);

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
    public void test360HermanAssignAccountDummyViolet() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(USER_HERMAN_FILE);

        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_HERMAN_OID, RESOURCE_DUMMY_VIOLET_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userHerman = getUser(USER_HERMAN_OID);
        display("User after change execution", userHerman);
        assertUser(userHerman, USER_HERMAN_OID, "herman", "Herman Toothrot", "Herman", "Toothrot");
        assertLiveLinks(userHerman, 1);
        assertAccount(userHerman, RESOURCE_DUMMY_VIOLET_OID);

        String accountVioletOid = getLiveLinkRefOid(userHerman, RESOURCE_DUMMY_VIOLET_OID);

        // Check shadow
        var accountVioletShadow = getShadowRepo(accountVioletOid);
        assertAccountShadowRepo(accountVioletShadow, accountVioletOid, "herman.1", getDummyResourceType(RESOURCE_DUMMY_VIOLET_NAME));

        assertIteration(accountVioletShadow.getPrismObject(), 1, ".1");

        // Check account
        PrismObject<ShadowType> accountVioletModel = modelService.getObject(ShadowType.class, accountVioletOid, null, task, result);
        assertAccountShadowModel(accountVioletModel, accountVioletOid, "herman.1", getDummyResourceType(RESOURCE_DUMMY_VIOLET_NAME));

        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "herman");
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "herman.1", "Herman Toothrot", true);

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
    public void test400RenameAngelicaConflicting() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userLechuck = createUser(USER_ANGELICA_NAME, "Angelica", true);
        userLechuck.asObjectable().getAssignment().add(createAccountAssignment(RESOURCE_DUMMY_PINK_OID, null));
        addObject(userLechuck);
        String userLechuckOid = userLechuck.getOid();

        PrismObject<ShadowType> accountCharles =
                createAccount(getDummyResourceObject(RESOURCE_DUMMY_PINK_NAME), ACCOUNT_SPARROW_NAME, true);
        addObject(accountCharles);

        // preconditions
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_ANGELICA_NAME, "Angelica", true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_SPARROW_NAME, null, true);

        // WHEN
        modifyUserReplace(userLechuckOid, UserType.F_NAME, task, result, PolyString.fromOrig(ACCOUNT_SPARROW_NAME));

        // THEN
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_SPARROW_NAME, null, true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, ACCOUNT_SPARROW_NAME + "1", "Angelica", true);
        assertNoDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_ANGELICA_NAME);
    }

    /**
     * No conflict. Just make sure the iteration condition is not triggered.
     */
    @Test
    public void test500JackAssignAccountDummyMagenta() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_MAGENTA_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLiveLinks(userJack, 4);
        assertAccount(userJack, RESOURCE_DUMMY_OID);
        assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
        assertAccount(userJack, RESOURCE_DUMMY_VIOLET_OID);
        assertAccount(userJack, RESOURCE_DUMMY_MAGENTA_OID);

        String accountMagentaOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_MAGENTA_OID);

        // Check shadow
        var accountMagentaShadow = getShadowRepo(accountMagentaOid);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, "jack", getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, "jack", getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        assertIteration(accountMagentaShadow.getPrismObject(), 0, "");

        // Check account in dummy resource
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Violet", true);
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "jack.1", "Jack Sparrow", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "jack", "Jack Sparrow", true);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION, PolyString.fromOrig(DESCRIPTION_RUM));

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Conflict on quote attribute
     */
    @Test
    public void test510DrakeAssignAccountDummyMagenta() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userDrake = PrismTestUtil.parseObject(USER_DRAKE_FILE);
        userDrake.asObjectable().setDescription(DESCRIPTION_RUM);
        userDrake.asObjectable().setLocality(PrismTestUtil.createPolyStringType("Jamaica"));
        addObject(userDrake);

        dummyAuditService.clear();

        // Make sure there are some dummy accounts without quote. So if the code tries to search for null
        // it will get something and the test fails
        getDummyResourceController(RESOURCE_DUMMY_MAGENTA_NAME).addAccount("afettucini", "Alfredo Fettucini");
        getDummyResourceController(RESOURCE_DUMMY_MAGENTA_NAME).addAccount("bfettucini", "Bill Fettucini");

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_DRAKE_OID,
                RESOURCE_DUMMY_MAGENTA_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("User after change execution", userDrakeAfter);
        assertUser(userDrakeAfter, USER_DRAKE_OID, "drake", "Francis Drake", "Fancis", "Drake");
        assertLiveLinks(userDrakeAfter, 1);
        assertAccount(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);

        String accountMagentaOid = getLiveLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);

        // Check shadow
        var accountMagentaShadow = getShadowRepo(accountMagentaOid);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, "drake001", getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        assertIteration(accountMagentaShadow.getPrismObject(), 1, "001");

        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, "drake001", getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake");
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", "Francis Drake", true);

        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, DESCRIPTION_RUM + " -- Francis Drake");
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Jamaica");

        PrismAsserts.assertPropertyValue(userDrakeAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig(DESCRIPTION_RUM + " -- Francis Drake"));

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /*
     * Modify a property that has nothing to do with iteration
     */
    @Test
    public void test520DrakeModifyLocality() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        when();
        modifyUserReplace(USER_DRAKE_OID, UserType.F_LOCALITY, task, result, PolyString.fromOrig("London"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("User after change execution", userDrakeAfter);
        assertUser(userDrakeAfter, USER_DRAKE_OID, "drake", "Francis Drake", "Fancis", "Drake");
        assertLiveLinks(userDrakeAfter, 1);
        assertAccount(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);

        String accountMagentaOid = getLiveLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_MAGENTA_OID);

        // Check shadow
        var accountMagentaShadow = getShadowRepo(accountMagentaOid);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, "drake001", getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        assertIteration(accountMagentaShadow.getPrismObject(), 1, "001");

        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, "drake001", getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake");
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, "drake001", "Francis Drake", true);

        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "London");
        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, "drake001",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, DESCRIPTION_RUM + " -- Francis Drake");

        PrismAsserts.assertPropertyValue(userDrakeAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig(DESCRIPTION_RUM + " -- Francis Drake"));

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Nothing special in this test. Just plain assignment. No conflicts. It just prepares the ground for the next
     * test and also tests the normal case.
     */
    @Test
    public void test530GuybrushAssignAccountDummyMagenta() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> accountAssignmentUserDelta = createAccountAssignmentUserDelta(USER_GUYBRUSH_OID, RESOURCE_DUMMY_MAGENTA_OID, null, true);
        deltas.add(accountAssignmentUserDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
        display("User after change execution", userGuybrush);
        assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
        assertLiveLinks(userGuybrush, 4);
        assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
        assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
        assertAccount(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
        assertAccount(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);

        String accountMagentaOid = getLiveLinkRefOid(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);

        // Check shadow
        var accountMagentaShadow = getShadowRepo(accountMagentaOid);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        assertIteration(accountMagentaShadow.getPrismObject(), 0, "");

        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        // old account
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "guybrush.3", "Guybrush Threepwood", true);
        // The new account
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);

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
     * Change Guybrushe's description so it conflicts with Jack's description in magenta resource.
     * As the iterator is also bound to the account identifier (ICF NAME) the guybrushe's account will
     * also be renamed.
     */
    @Test
    public void test532GuybrushModifyDescription() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, task, result, DESCRIPTION_RUM);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userGuybrush = getUser(USER_GUYBRUSH_OID);
        display("User after change execution", userGuybrush);
        assertUser(userGuybrush, USER_GUYBRUSH_OID, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Guybrush", "Threepwood");
        assertLiveLinks(userGuybrush, 4);
        assertAccount(userGuybrush, RESOURCE_DUMMY_OID);
        assertAccount(userGuybrush, RESOURCE_DUMMY_PINK_OID);
        assertAccount(userGuybrush, RESOURCE_DUMMY_VIOLET_OID);
        assertAccount(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);

        String accountMagentaOid = getLiveLinkRefOid(userGuybrush, RESOURCE_DUMMY_MAGENTA_OID);

        // Check shadow
        var accountMagentaShadow = getShadowRepo(accountMagentaOid);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME + "001", getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, ACCOUNT_GUYBRUSH_DUMMY_USERNAME + "001", getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, "guybrush.3", "Guybrush Threepwood", true);

        // There should be no account with the "straight" name
        assertNoDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        // Renamed
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME + "001", "Guybrush Threepwood", true);

        assertDummyAccountAttribute(RESOURCE_DUMMY_MAGENTA_NAME, ACCOUNT_GUYBRUSH_DUMMY_USERNAME + "001",
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, DESCRIPTION_RUM + " -- Guybrush Threepwood");

        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_ORGANIZATION,
                PolyString.fromOrig(DESCRIPTION_RUM + " -- Guybrush Threepwood"));

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test600JackRename() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_NAME, task, result,
                PolyString.fromOrig(USER_JACK_RENAMED_NAME));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack, USER_JACK_RENAMED_NAME, "Jack Sparrow", "Jack", "Sparrow", "Caribbean");
        assertLiveLinks(userJack, 4);
        assertAccount(userJack, RESOURCE_DUMMY_OID);
        assertAccount(userJack, RESOURCE_DUMMY_PINK_OID);
        assertAccount(userJack, RESOURCE_DUMMY_VIOLET_OID);
        assertAccount(userJack, RESOURCE_DUMMY_MAGENTA_OID);

        String accountMagentaOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_MAGENTA_OID);

        // Check shadow
        var accountMagentaShadow = getShadowRepo(accountMagentaOid);
        assertAccountShadowRepo(accountMagentaShadow, accountMagentaOid, USER_JACK_RENAMED_NAME, getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        // Check account
        PrismObject<ShadowType> accountMagentaModel = modelService.getObject(ShadowType.class, accountMagentaOid, null, task, result);
        assertAccountShadowModel(accountMagentaModel, accountMagentaOid, USER_JACK_RENAMED_NAME, getDummyResourceType(RESOURCE_DUMMY_MAGENTA_NAME));

        assertIteration(accountMagentaShadow.getPrismObject(), 0, "");

        assertDefaultDummyAccount(USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_PINK_NAME, USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        // The original conflicting account should still remain
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Violet", true);
        assertDummyAccount(RESOURCE_DUMMY_VIOLET_NAME, USER_JACK_RENAMED_NAME, "Jack Sparrow", true);
        assertDummyAccount(RESOURCE_DUMMY_MAGENTA_NAME, USER_JACK_RENAMED_NAME, "Jack Sparrow", true);

        PrismAsserts.assertPropertyValue(userJack, UserType.F_ORGANIZATION,
                PolyString.fromOrig(DESCRIPTION_RUM));

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(5);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test700DarkVioletSyncTask() throws Exception {
        // WHEN
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_FILE);

        // THEN
        waitForTaskNextRun(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);
    }

    /*
     * Create account with fullname LeChuck. User with name LeChuck should be created (no conflict yet).
     */
    @Test
    public void test710DarkVioletAddLeChuck() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = new DummyAccount(ACCOUNT_LECHUCK_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LECHUCK_FULLNAME);

        // WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());
        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
    }

    /*
     * Create account with fullname LeChuck. User with name LeChuck.1 should be created (conflict).
     */
    @Test
    public void test712DarkVioletAddCharles() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = new DummyAccount(ACCOUNT_CHARLES_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LECHUCK_FULLNAME);

        // WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());
        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
        assertUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME + ".1");
    }

    /*
     * Create account with fullname LeChuck. User with name LeChuck.2 should be created (second conflict).
     */
    @Test
    public void test714DarkVioletAddShinetop() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = new DummyAccount(ACCOUNT_SHINETOP_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LECHUCK_FULLNAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");

        // WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());
        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
        assertUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME + ".1");
        assertUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME + ".2", "Melee Island");
    }

    @Test
    public void test716DarkVioletDeleteCharles() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        // WHEN
        when();

        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).deleteAccountByName(ACCOUNT_CHARLES_USERNAME);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
        assertNoUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME + ".1");
        assertUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME + ".2", "Melee Island");
    }

    @Test
    public void test720DarkVioletModifyShinetopLocation() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).getAccountByName(ACCOUNT_SHINETOP_USERNAME);

        // WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Monkey Island");

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        displayAllUsers();
        assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
        assertNoUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME + ".1");
        assertUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME + ".2", "Monkey Island");
    }

    @Test
    public void test722DarkVioletModifyShinetopFullName() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).getAccountByName(ACCOUNT_SHINETOP_USERNAME);

        // WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, CHUCKIE_FULLNAME);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        displayAllUsers();
        assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
        assertNoUserNick(ACCOUNT_CHARLES_USERNAME, LECHUCK_FULLNAME + ".1");
        assertUserNick(ACCOUNT_SHINETOP_USERNAME, CHUCKIE_FULLNAME, CHUCKIE_FULLNAME, "Monkey Island");
        assertNoUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME + ".2");
    }

    /*
     * Create account with fullname Le_Chuck. This does not conflict with the orig value,
     * but it does conflict on polystring norm value.
     * User with name le_chuck.1 should be created. This is the third conflict, but the .1
     * suffix is free, therefore it is reused.
     * MID-5199
     */
    @Test
    public void test724DarkVioletAddLe_Chuck() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = new DummyAccount(ACCOUNT_LE_CHUCK_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, LE_CHUCK_FULLNAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Melee Island");

        // WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());
        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        displayAllUsers();
        assertUserNick(ACCOUNT_LECHUCK_USERNAME, LECHUCK_FULLNAME, LECHUCK_FULLNAME);
        assertUserNick(ACCOUNT_LE_CHUCK_USERNAME, LE_CHUCK_FULLNAME, LE_CHUCK_FULLNAME + ".1", "Melee Island");
        assertUserNick(ACCOUNT_SHINETOP_USERNAME, CHUCKIE_FULLNAME, CHUCKIE_FULLNAME, "Monkey Island");
        assertNoUserNick(ACCOUNT_SHINETOP_USERNAME, LECHUCK_FULLNAME + ".2");
    }

    /*
     * Create account with fullname barbossa. But user barbossa already exists.
     *  User with name barbossa.1 should be created (conflict).
     */
    @Test
    public void test730DarkVioletAddBarbossa() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = new DummyAccount(USER_BARBOSSA_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, USER_BARBOSSA_USERNAME);

        // WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());
        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        assertUserNick(USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME + ".1");
    }

    /*
     * Create yet another account with fullname barbossa. We already have two barbossa users,
     * so the next one is barbossa.2. But there is a post-iteration condition that refuses that
     * name. It also refuses barbossa.3. So the result should be barbossa.4.
     * MID-3338
     */
    @Test
    public void test732DarkVioletAddBarbossa() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = new DummyAccount("YA" + USER_BARBOSSA_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, USER_BARBOSSA_USERNAME);

        // WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());
        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        assertUserNick(USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME + ".1");
        assertUserNick("YA" + USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_USERNAME + ".4");
    }

    @Test
    public void test750DarkVioletAddMatusalem() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        // IMPORTANT! Change of user template!
        deleteObject(ObjectTemplateType.class, USER_TEMPLATE_ITERATION_OID, task, result);
        addObject(USER_TEMPLATE_ITERATION_RANDOM_FILE);

        DummyAccount account = new DummyAccount(ACCOUNT_MATUSALEM_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RUM_FULLNAME);

        // WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());
        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
    }

    /*
     * Create account with fullname Rum. User with name Rum.xxx should be created (conflict).
     */
    @Test
    public void test752DarkVioletAddDiplomatico() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = new DummyAccount(ACCOUNT_DIPLOMATICO_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RUM_FULLNAME);

        // WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());
        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        displayAllUsers();

        assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);

        iterationTokenDiplomatico = lookupIterationTokenByAdditionalName(ACCOUNT_DIPLOMATICO_USERNAME);
        assertUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME + iterationTokenDiplomatico);
    }

    /*
     * Create account with fullname Rum. User with name Rum.yyy should be created (second conflict).
     */
    @Test
    public void test754DarkVioletAddMilionario() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = new DummyAccount(ACCOUNT_MILLONARIO_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RUM_FULLNAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Peru");

        // WHEN
        when();

        displayValue("Adding dummy account", account.debugDump());
        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
        assertUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME, RUM_FULLNAME + iterationTokenDiplomatico);

        iterationTokenMillonario = lookupIterationTokenByAdditionalName(ACCOUNT_MILLONARIO_USERNAME);
        assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME + iterationTokenMillonario, "Peru");
    }

    @Test
    public void test756DarkVioletDeleteDiplomatico() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        // WHEN
        when();

        getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).deleteAccountByName(ACCOUNT_DIPLOMATICO_USERNAME);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
        assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME + iterationTokenDiplomatico);
        assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME + iterationTokenMillonario, "Peru");
    }

    @Test
    public void test760DarkVioletModifyMillonarioLocation() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).getAccountByName(ACCOUNT_MILLONARIO_USERNAME);

        // WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Northern Peru");

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        displayAllUsers();
        assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
        assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME + iterationTokenDiplomatico);
        assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME, RUM_FULLNAME + iterationTokenMillonario, "Northern Peru");
    }

    /**
     * Rename to an identifier that is free. Empty iterationToken is expected.
     */
    @Test
    public void test762DarkVioletModifyMillonarioFullName() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).getAccountByName(ACCOUNT_MILLONARIO_USERNAME);

        // WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RON_FULLNAME);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        displayAllUsers();
        assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME, RUM_FULLNAME);
        assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME + iterationTokenDiplomatico);
        assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RON_FULLNAME, RON_FULLNAME, "Northern Peru");
        assertNoUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME + iterationTokenMillonario);
    }

    /**
     * Rename to an identifier that is taken. New random iterationToken is expected.
     */
    @Test
    public void test764DarkVioletModifyMatusalemFullName() throws Exception {
        // GIVEN
        dummyAuditService.clear();

        DummyAccount account = getDummyResource(RESOURCE_DUMMY_DARK_VIOLET_NAME).getAccountByName(ACCOUNT_MATUSALEM_USERNAME);

        // WHEN
        when();

        account.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, RON_FULLNAME);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_DARK_VIOLET_OID);

        // THEN
        then();
        displayAllUsers();
        assertNoUserNick(ACCOUNT_MATUSALEM_USERNAME, RUM_FULLNAME);
        String iterationTokenMatusalem = lookupIterationTokenByAdditionalName(ACCOUNT_MATUSALEM_USERNAME);
        assertUserNick(ACCOUNT_MATUSALEM_USERNAME, RON_FULLNAME, RON_FULLNAME + iterationTokenMatusalem);
        assertNoUserNick(ACCOUNT_DIPLOMATICO_USERNAME, RUM_FULLNAME + iterationTokenDiplomatico);
        assertUserNick(ACCOUNT_MILLONARIO_USERNAME, RON_FULLNAME, RON_FULLNAME, "Northern Peru");
        assertNoUserNick(ACCOUNT_MILLONARIO_USERNAME, RUM_FULLNAME + iterationTokenMillonario);
    }

    /**
     * MID-2887
     */
    @Test
    public void test800UniqueEmailAddUserAlfredoFettucini() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_OID);

        PrismObject<UserType> user = createUser(
                USER_ALFREDO_FETTUCINI_USERNAME, USER_ALFREDO_FETTUCINI_GIVEN_NAME,
                USER_ALFREDO_FETTUCINI_FAMILY_NAME, USER_FETTUCINI_NICKNAME, true);

        // WHEN
        when();
        addObject(user, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(user.getOid());
        display("User after change execution", userAfter);
        assertUser(userAfter, user.getOid(), USER_ALFREDO_FETTUCINI_USERNAME,
                USER_ALFREDO_FETTUCINI_GIVEN_NAME + " " + USER_ALFREDO_FETTUCINI_FAMILY_NAME,
                USER_ALFREDO_FETTUCINI_GIVEN_NAME, USER_ALFREDO_FETTUCINI_FAMILY_NAME);

        PrismAsserts.assertEqualsPolyString("Wrong " + user + " nickname", USER_FETTUCINI_NICKNAME, userAfter.asObjectable().getNickName());

        assertEquals("Wrong " + user + " emailAddress", USER_FETTUCINI_NICKNAME + EMAIL_SUFFIX, userAfter.asObjectable().getEmailAddress());
    }

    /**
     * MID-2887
     */
    @Test
    public void test802UniqueEmailAddUserBillFettucini() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_ITERATION_UNIQUE_EMAIL_OID);

        PrismObject<UserType> user = createUser(USER_BILL_FETTUCINI_USERNAME, USER_BILL_FETTUCINI_GIVEN_NAME,
                USER_BILL_FETTUCINI_FAMILY_NAME, USER_FETTUCINI_NICKNAME, true);

        // WHEN
        when();
        addObject(user, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(user.getOid());
        display("User after change execution", userAfter);
        assertUser(userAfter, user.getOid(), USER_BILL_FETTUCINI_USERNAME,
                USER_BILL_FETTUCINI_GIVEN_NAME + " " + USER_BILL_FETTUCINI_FAMILY_NAME,
                USER_BILL_FETTUCINI_GIVEN_NAME, USER_BILL_FETTUCINI_FAMILY_NAME);

        PrismAsserts.assertEqualsPolyString("Wrong " + user + " nickname", USER_FETTUCINI_NICKNAME, userAfter.asObjectable().getNickName());

        assertEquals("Wrong " + user + " emailAddress", USER_FETTUCINI_NICKNAME + ".1" + EMAIL_SUFFIX, userAfter.asObjectable().getEmailAddress());
    }

    /**
     * MID-5618: When subtype is set by inbound, wrong iterator is used.
     */
    @Test
    public void test820SubtypeSetByInbound() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, null);
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, "associate", USER_TEMPLATE_ITERATION_ASSOCIATE_OID, result);

        DummyResourceContoller associateCtl = dummyResourceCollection.get("associate");
        associateCtl.addAccount("u1", "jim");   // should be imported as jim-0
        associateCtl.addAccount("u2", "jim");   // should be imported as jim-1

        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_ASSOCIATE_OID)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .build();
        SearchResultList<PrismObject<ShadowType>> shadows = provisioningService
                .searchObjects(ShadowType.class, query, null, task, result);
        display("shadows", shadows);
        assertEquals("Wrong # of shadows", 2, shadows.size());

        // WHEN
        when();
        importFromResourceLauncher.importSingleShadow(shadows.get(0).getOid(), task, result);
        importFromResourceLauncher.importSingleShadow(shadows.get(1).getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        ObjectQuery userQuery = prismContext.queryFor(UserType.class)
                .item(UserType.F_SUBTYPE).eq("associate")
                .asc(UserType.F_NAME)
                .build();
        SearchResultList<PrismObject<UserType>> usersAfter = repositoryService
                .searchObjects(UserType.class, userQuery, null, result);
        display("users", usersAfter);
        assertEquals("Wrong # of created users", 2, usersAfter.size());
        assertEquals("Wrong name of user 1", "jim-0", usersAfter.get(0).getName().getOrig());
        assertEquals("Wrong name of user 2", "jim-1", usersAfter.get(1).getName().getOrig());
    }

    @SuppressWarnings("SameParameterValue")
    private PrismObject<UserType> createUser(String username, String givenName,
            String familyName, String nickname, boolean enabled) throws SchemaException {
        PrismObject<UserType> user = createUser(username, givenName, familyName, enabled);
        user.asObjectable().setNickName(PrismTestUtil.createPolyStringType(nickname));
        return user;
    }

    private void assertUserLargo(PrismObject<UserType> userLargo) {
        assertUser(userLargo, USER_LARGO_OID, USER_LARGO_NAME, null, "Largo", "LaGrande", null);
    }

    private void assertUserNick(String accountName, String accountFullName, String expectedUserName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        assertUserNick(accountName, accountFullName, expectedUserName, null);
    }

    private void assertUserNick(String accountName, String accountFullName, String expectedUserName, String expectedLocality) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> user = findUserByUsername(expectedUserName);
        assertNotNull("No user for " + accountName + " (" + expectedUserName + ")", user);
        display("Created user for " + accountName, user);
        assertEquals("Wrong nickname in user created for " + accountName, accountFullName, user.asObjectable().getNickName().getOrig());
        assertEquals("Wrong additionalName in user created for " + accountName, accountName, user.asObjectable().getAdditionalName().getOrig());
        PolyStringType locality = user.asObjectable().getLocality();
        if (locality == null) {
            assertNull("Wrong locality in user created for " + accountName, expectedLocality);
        } else {
            assertEquals("Wrong locality in user created for " + accountName, expectedLocality, locality.getOrig());
        }
    }

    private void assertNoUserNick(String accountName, String expectedUserName)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> user = findUserByUsername(expectedUserName);
        display("User for " + accountName, user);
        assertNull("User for " + accountName + " (" + expectedUserName + ") exists but it should be gone", user);
    }

    private String lookupIterationTokenByAdditionalName(String additionalName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = taskManager.createTaskInstance(TestIteration.class.getName() + ".lookupIterationTokenByAdditionalName");
        OperationResult result = task.getResult();
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ADDITIONAL_NAME).eq(PolyString.fromOrig(additionalName))
                .build();
        List<PrismObject<UserType>> objects = modelService.searchObjects(UserType.class, query, null, task, result);
        if (objects.isEmpty()) {
            return null;
        }
        assert objects.size() == 1 : "Too many objects found for additional name " + additionalName + ": " + objects;
        PrismObject<UserType> user = objects.iterator().next();
        return user.asObjectable().getIterationToken();
    }

    /**
     * Cases doesn't have to have unique names. We need skip constraints check for CaseType.
     * It's not direct relation with iteration, but function for skipping is in IterationHelper.
     */
    @Test
    public void test900SkippingConstraintsCheckForCaseType() throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // adding two object with same names
        addObject(new CaseType().name("test case").asPrismObject());
        String caseOid = addObject(new CaseType().name("test case").asPrismObject());

        dummyAuditService.clear();

        ObjectDelta<CaseType> modifyDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(CaseType.class, caseOid, CaseType.F_DESCRIPTION, "descriptionmodify");

        // WHEN
        when();
        executeChanges(modifyDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, CaseType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /** Simply adding (conflicting) user with an association. MID-8569. */
    @Test
    public void test910AddUserWithAssociation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setDefaultUserTemplate(USER_TEMPLATE_SIMPLE_ITERATION.oid);
        try {
            given("existing user 'Joe Smith' with CS-101 assignment");
            UserType existingJoe = new UserType()
                    .givenName("Joe")
                    .familyName("Smith")
                    .assignment(ROLE_CS_101.assignmentTo());
            addObject(existingJoe.asPrismObject(), task, result);
            assertUserWithAssociation("smith1");

            when("conflicting 'John Smith' with CS-101 assignment is added");
            UserType newJoe = new UserType()
                    .givenName("John")
                    .familyName("Smith")
                    .assignment(ROLE_CS_101.assignmentTo());
            addObject(newJoe.asPrismObject(), task, result);

            then("second user is created");
            assertUserWithAssociation("smith2");
        } finally {
            setDefaultUserTemplate(null);
        }
    }

    private void assertUserWithAssociation(String name) throws Exception {
        assertUserAfterByUsername(name)
                .withObjectResolver(createSimpleModelObjectResolver())
                .links()
                .singleLive()
                .resolveTarget()
                .associations()
                .assertSize(1);
    }

    /** Having template with `iterationSpecification` but the name does not depend on the iteration. MID-8492. */
    @Test
    public void test920IteratingWithoutIterator() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String name = "test920";

        setDefaultUserTemplate(USER_TEMPLATE_ITERATION_BUT_CONSTANT_NAME.oid);
        try {
            when("user is created");
            UserType user = new UserType()
                    .name(name);
            addObject(user.asPrismObject(), task, result);

            then("user is there");
            assertUserAfterByUsername(name);
        } finally {
            setDefaultUserTemplate(null);
        }
    }
}
