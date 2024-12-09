/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.*;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.schema.util.ObjectQueryUtil.createResourceAndObjectClassQuery;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.PROTECTED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.SYNCHRONIZATION_NOT_NEEDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.sync.tasks.recon.ReconciliationActivityHandler;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.test.*;

import org.apache.commons.lang3.mutable.MutableInt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.sync.tasks.recon.DebugReconciliationResultListener;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 */
@SuppressWarnings("SpellCheckingInspection")
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestImportRecon extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/sync");

    private static final TestObject<UserType> USER_IMPORTER = TestObject.file(
            TEST_DIR, "user-importer.xml", "00000000-1111-1111-1111-000000000002");

    private static final String ACCOUNT_OTIS_NAME = "otis";
    private static final String ACCOUNT_OTIS_FULLNAME = "Otis";

    private static final File ACCOUNT_STAN_FILE = new File(TEST_DIR, "account-stan-dummy.xml");
    private static final String ACCOUNT_STAN_OID = "22220000-2200-0000-0000-444400004455";
    private static final String ACCOUNT_STAN_NAME = "stan";
    private static final String ACCOUNT_STAN_FULLNAME = "Stan the Salesman";

    private static final String ACCOUNT_RUM_NAME = "rum";

    private static final String ACCOUNT_MURRAY_NAME = "murray";

    private static final String ACCOUNT_CAPSIZE_NAME = "capsize";
    private static final String ACCOUNT_CAPSIZE_FULLNAME = "Kata Capsize";

    private static final String USER_AUGUSTUS_NAME = "augustus";

    private static class TestAccount extends TestObject<ShadowType> {
        private final String name;
        @SuppressWarnings({ "FieldCanBeLocal", "unused" })
        private final String fullName;

        private TestAccount(File dir, String fileName, String oid, String name, String fullName) {
            super(new FileBasedTestObjectSource(dir, fileName), oid);
            this.name = name;
            this.fullName = fullName;
        }
    }

    private static final TestAccount ACCOUNT_AUGUSTUS = new TestAccount(TEST_DIR, "account-augustus-dummy.xml", "22220000-2200-0000-0000-444400004457", "augustus", "Augustus DeWaat");
    private static final TestAccount ACCOUNT_TAUGUSTUS = new TestAccount(TEST_DIR, "account-taugustus-dummy.xml", "22220000-2200-0000-0000-444400004456", "Taugustus", "Augustus DeWaat");
    private static final TestAccount ACCOUNT_KENNY = new TestAccount(TEST_DIR, "account-kenny-dummy.xml", "22220000-2200-0000-0000-444400004461", "kenny", "Kenny Falmouth");

    private static final String USER_PALIDO_NAME = "palido";

    private static final TestAccount ACCOUNT_TPALIDO = new TestAccount(TEST_DIR, "account-tpalido-dummy.xml", "22220000-2200-0000-0000-444400004462", "Tpalido", "Palido Domingo");
    private static final TestAccount ACCOUNT_LECHIMP = new TestAccount(TEST_DIR, "account-lechimp-dummy.xml", "22220000-2200-0000-0000-444400004463", "lechimp", "Captain LeChimp");
    private static final TestAccount ACCOUNT_TLECHIMP = new TestAccount(TEST_DIR, "account-tlechimp-dummy.xml", "22220000-2200-0000-0000-444400004464", "Tlechimp", "Captain LeChimp");
    private static final TestAccount ACCOUNT_ANDRE = new TestAccount(TEST_DIR, "account-andre-dummy.xml", "22220000-2200-0000-0000-444400004465", "andre", "King Andre");
    private static final TestAccount ACCOUNT_TANDRE = new TestAccount(TEST_DIR, "account-tandre-dummy.xml", "22220000-2200-0000-0000-444400004466", "Tandre", "King Andre");

    private static final String USER_LAFOOT_NAME = "lafoot";
    private static final TestAccount ACCOUNT_TLAFOOT = new TestAccount(TEST_DIR, "account-tlafoot-dummy.xml", "22220000-2200-0000-0000-444400004467", "Tlafoot", "Effete LaFoot");
    private static final TestAccount ACCOUNT_CRUFF = new TestAccount(TEST_DIR, "account-cruff-dummy.xml", "22220000-2200-0000-0000-444400004468", "cruff", "Cruff");

    private static final String ACCOUNT_HTM_NAME = "htm";
    private static final String ACCOUNT_HTM_FULL_NAME = "Horatio Torquemada Marley";

    // AZURE resource. It disables unmatched accounts.
    // It also has several objectType definitions that are designed to confuse
    // the code that determines refined schema definitions
    private static final File RESOURCE_DUMMY_AZURE_FILE = new File(TEST_DIR, "resource-dummy-azure.xml");
    private static final String RESOURCE_DUMMY_AZURE_OID = "10000000-0000-0000-0000-00000000a204";
    private static final String RESOURCE_DUMMY_AZURE_NAME = "azure";

    // LIME dummy resource. This is a pure authoritative resource. It has only inbound mappings.
    private static final File RESOURCE_DUMMY_LIME_FILE = new File(TEST_DIR, "resource-dummy-lime.xml");
    private static final String RESOURCE_DUMMY_LIME_OID = "10000000-0000-0000-0000-000000131404";
    private static final String RESOURCE_DUMMY_LIME_NAME = "lime";

    private static final DummyTestResource RESOURCE_DUMMY_GRAVEYARD = new DummyTestResource(
            TEST_DIR, "resource-dummy-graveyard.xml", "106c2242-59c9-4ccd-afcb-557437b816da", "graveyard");
    private static final DummyTestResource RESOURCE_DUMMY_REAPING = new DummyTestResource(
            TEST_DIR, "resource-dummy-reaping.xml", "9f5842cb-74c0-434f-85d1-b2999f189500", "reaping");

    private static final QName DUMMY_LIME_ACCOUNT_OBJECT_CLASS = RI_ACCOUNT_OBJECT_CLASS;

    private static final TestObject<ObjectTemplateType> USER_TEMPLATE_LIME = TestObject.file(
            TEST_DIR, "user-template-lime.xml", "3cf43520-241d-11e6-afa5-a377b674950d");
    private static final TestObject<RoleType> ROLE_IMPORTER = TestObject.file(
            TEST_DIR, "role-importer.xml", "00000000-1111-1111-1111-000000000004");
    private static final TestObject<RoleType> ROLE_CORPSE = TestObject.file(
            TEST_DIR, "role-corpse.xml", "1c64c778-e7ac-11e5-b91a-9f44177e2359");

    private static final TestObject<ValuePolicyType> PASSWORD_POLICY_LOWER_CASE_ALPHA_AZURE = TestObject.file(
            TEST_DIR, "password-policy-azure.xml", "81818181-76e0-59e2-8888-3d4f02d3fffd");
    private static final TestObject<TaskType> TASK_RECONCILE_DUMMY_SINGLE = TestObject.file(
            TEST_DIR, "task-reconcile-dummy-single.xml", "10000000-0000-0000-5656-565600000004");
    private static final TestObject<TaskType> TASK_RECONCILE_DUMMY_FILTER = TestObject.file(
            TEST_DIR, "task-reconcile-dummy-filter.xml", "10000000-0000-0000-5656-565600000014");
    private static final TestTask TASK_RECONCILE_DUMMY_AZURE = TestTask.file(
            TEST_DIR, "task-reconcile-dummy-azure.xml", "10000000-0000-0000-5656-56560000a204");
    private static final TestObject<TaskType> TASK_RECONCILE_DUMMY_LIME = TestObject.file(
            TEST_DIR, "task-reconcile-dummy-lime.xml", "10000000-0000-0000-5656-565600131204");
    private static final TestTask TASK_RECONCILE_DUMMY_GRAVEYARD = new TestTask(
            TEST_DIR, "task-reconcile-dummy-graveyard.xml", "c2665533-bae3-4d06-966c-8e8705bc37da", 20_000);
    private static final TestTask TASK_RECONCILE_DUMMY_REAPING_DRY_RUN = new TestTask(
            TEST_DIR, "task-reconcile-dummy-reaping-dry-run.xml", "2c51a65a-84bc-4496-a34f-5e3070131da9", 20_000);
    private static final TestObject<TaskType> TASK_IMPORT_DUMMY_LIME_LIMITED_MIGRATED = TestObject.file(
            TEST_DIR, "task-import-dummy-lime-limited-migrated.xml", "4e2f83b8-5312-4924-af7e-52805ad20b3e");
    private static final TestObject<TaskType> TASK_IMPORT_DUMMY_LIME_LIMITED = TestObject.file(
            TEST_DIR, "task-import-dummy-lime-limited.xml", "db3b4438-67a8-4614-a320-382b4cbace41");
    private static final TestObject<TaskType> TASK_DELETE_DUMMY_SHADOWS = TestObject.file(
            TEST_DIR, "task-delete-dummy-shadows.xml", "abaab842-18be-11e5-9416-001e8c717e5b");
    private static final TestObject<TaskType> TASK_DELETE_DUMMY_ACCOUNTS = TestObject.file(
            TEST_DIR, "task-delete-dummy-accounts.xml", "ab28a334-2aca-11e5-afe7-001e8c717e5b");

    private static final String ATTR_EMPLOYEE_NUMBER = "employeeNumber";

    private static final TestObject<ArchetypeType> ARCHETYPE_EMPLOYEE = TestObject.file(
            TEST_DIR, "archetype-employee.xml", "e3a9a6b9-17f6-4239-b935-6f88a655b9d7");
    private static final TestObject<ArchetypeType> ARCHETYPE_OTHER = TestObject.file(
            TEST_DIR, "archetype-other.xml", "0e887a44-4862-4e27-af06-0215f04ef36f");
    private static final DummyTestResource RESOURCE_DUMMY_ARCHETYPED = new DummyTestResource(
            TEST_DIR, "resource-dummy-archetyped.xml", "e0789d4f-8748-41e0-9911-6d0938287588", "archetyped",
            controller -> controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                    ATTR_EMPLOYEE_NUMBER, String.class, false, false));
    private static final DummyTestResource RESOURCE_DUMMY_ARCHETYPED_FILTER_BASED = new DummyTestResource(
            TEST_DIR, "resource-dummy-archetyped-filter-based.xml", "ac51f575-bdf7-49ec-bb3f-5ffe9fcbb3bb",
            "archetyped-filter-based",
            controller -> controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                    ATTR_EMPLOYEE_NUMBER, String.class, false, false));

    private static final String GROUP_CORPSES_NAME = "corpses";

    private static final String ACCOUNT_CAPSIZE_PASSWORD = "is0mud01d";

    private static final int NUMBER_OF_IMPORTED_USERS = 2;

    @Autowired
    private ValuePolicyProcessor valuePolicyProcessor;

    private DummyResource dummyResourceAzure;
    private DummyResourceContoller dummyResourceCtlAzure;
    private ResourceType resourceDummyAzureType;
    private PrismObject<ResourceType> resourceDummyAzure;

    private DummyResource dummyResourceLime;
    private DummyResourceContoller dummyResourceCtlLime;
    private PrismObject<ResourceType> resourceDummyLime;

    @Autowired private ReconciliationActivityHandler reconciliationActivityHandler;

    private DebugReconciliationResultListener reconciliationResultListener;

    PrismObject<UserType> userImporter;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        reconciliationResultListener = new DebugReconciliationResultListener();
        reconciliationActivityHandler.setReconciliationResultListener(reconciliationResultListener);

        // Object templates (must be imported before resource, otherwise there are validation warnigns)
        repoAdd(USER_TEMPLATE_LIME, initResult);

        dummyResourceCtlAzure = DummyResourceContoller.create(RESOURCE_DUMMY_AZURE_NAME, resourceDummyAzure);
        dummyResourceCtlAzure.extendSchemaPirate();
        dummyResourceCtlAzure.addOrgTop();
        dummyResourceAzure = dummyResourceCtlAzure.getDummyResource();
        resourceDummyAzure = importAndGetObjectFromFile(ResourceType.class, getDummyResourceAzureFile(), RESOURCE_DUMMY_AZURE_OID, initTask, initResult);
        resourceDummyAzureType = resourceDummyAzure.asObjectable();
        dummyResourceCtlAzure.setResource(resourceDummyAzure);

        dummyResourceCtlLime = DummyResourceContoller.create(RESOURCE_DUMMY_LIME_NAME, resourceDummyLime);
        dummyResourceCtlLime.extendSchemaPirate();
        dummyResourceLime = dummyResourceCtlLime.getDummyResource();
        resourceDummyLime = importAndGetObjectFromFile(ResourceType.class, getDummyResourceLimeFile(), RESOURCE_DUMMY_LIME_OID, initTask, initResult);
        dummyResourceCtlLime.setResource(resourceDummyLime);

        initAndTestDummyResource(RESOURCE_DUMMY_GRAVEYARD, initTask, initResult);
        TASK_RECONCILE_DUMMY_GRAVEYARD.init(this, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_REAPING, initTask, initResult);
        TASK_RECONCILE_DUMMY_REAPING_DRY_RUN.init(this, initTask, initResult);

        // Create an account that midPoint does not know about yet
        getDummyResourceController().addAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, "Scabb Island");
        getDummyResource().getAccountByName(USER_RAPP_USERNAME)
                .replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The Elaine");

        dummyResourceCtlLime.addAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME, "Scabb Island");
        dummyResourceLime.getAccountByName(USER_RAPP_USERNAME)
                .replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The Elaine");
        dummyResourceCtlLime.addAccount(ACCOUNT_RUM_NAME, "Rum Rogers");
        dummyResourceCtlLime.addAccount(ACCOUNT_MURRAY_NAME, "Murray");

        addObject(ARCHETYPE_EMPLOYEE, initTask, initResult);
        addObject(ARCHETYPE_OTHER, initTask, initResult);
        RESOURCE_DUMMY_ARCHETYPED.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_ARCHETYPED_FILTER_BASED.initAndTest(this, initTask, initResult);

        // Groups
        dummyResourceCtlAzure.addGroup(GROUP_CORPSES_NAME);

        // Roles
        repoAdd(ROLE_CORPSE, initResult);
        repoAdd(ROLE_IMPORTER, initResult);

        // Password policy
        repoAddObjectFromFile(PASSWORD_POLICY_GLOBAL_FILE, initResult);
        repoAdd(PASSWORD_POLICY_LOWER_CASE_ALPHA_AZURE, initResult);

        applyPasswordPolicy(PASSWORD_POLICY_GLOBAL_OID, SECURITY_POLICY_OID, initTask, initResult);

        // Users
        userImporter = repoAdd(USER_IMPORTER, initResult);
        // And a user that will be correlated to that account
        repoAddObjectFromFile(USER_RAPP_FILE, initResult);

        PrismObject<ShadowType> accountStan = PrismTestUtil.parseObject(ACCOUNT_STAN_FILE);
        provisioningService.addObject(accountStan, null, null, initTask, initResult);

        addObject(SHADOW_GROUP_DUMMY_TESTERS_FILE, initTask, initResult);

        InternalMonitor.reset();
        InternalMonitor.setTrace(InternalOperationClasses.SHADOW_FETCH_OPERATIONS, true);

        initTestObjects(initTask, initResult,
                TASK_RECONCILE_DUMMY_AZURE); // other tasks will be migrated later
    }

    @Override
    protected int getNumberOfUsers() {
        return super.getNumberOfUsers() + NUMBER_OF_IMPORTED_USERS;
    }

    private File getDummyResourceLimeFile() {
        return RESOURCE_DUMMY_LIME_FILE;
    }

    private File getDummyResourceAzureFile() {
        return RESOURCE_DUMMY_AZURE_FILE;
    }

    protected void loginImportUser() throws CommonException {
        loginAdministrator();
    }

    @Test
    public void test001SanityAzure() throws Exception {
        displayDumpable("Dummy resource azure", dummyResourceAzure);

        // WHEN
        var resourceSchemaAzure = ResourceSchemaFactory.getBareSchema(resourceDummyAzureType);
        displayDumpable("Dummy azure resource schema", resourceSchemaAzure);

        // THEN
        dummyResourceCtlAzure.assertDummyResourceSchemaSanityExtended(resourceSchemaAzure);

        ResourceObjectClassDefinition orgOcDef =
                resourceSchemaAzure.findObjectClassDefinition(dummyResourceCtlAzure.getOrgObjectClassQName());
        assertNotNull("No org object class def in azure resource schema", orgOcDef);
    }

    @Test
    public void test002SanityAzureRefined() throws Exception {
        // WHEN
        var completeSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resourceDummyAzureType);
        displayDumpable("Dummy azure refined schema", completeSchema);

        // THEN
        dummyResourceCtlAzure.assertCompleteSchemaSanity(completeSchema);

        completeSchema.findObjectClassDefinitionRequired(dummyResourceCtlAzure.getOrgObjectClassQName());
    }

    /**
     * Single-user import.
     */
    @Test
    public void test100ImportStanFromResourceDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Preconditions
        assertUsers(getNumberOfUsers());
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        loginImportUser();

        // WHEN
        when();
        modelService.importFromResource(ACCOUNT_STAN_OID, task, result);

        // THEN
        then();
        display(result);
        assertSuccess(result);

        loginAdministrator();

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        PrismObject<UserType> userStan = assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        assertUsers(getNumberOfUsers() + 1);

        assertPasswordCompliesWithPolicy(userStan, PASSWORD_POLICY_GLOBAL_OID); // MID-4028

        // Check audit
        assertImportAuditModifications(1);
    }

    /**
     * Background import.
     */
    @Test
    public void test150ImportFromResourceDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Preconditions
        List<PrismObject<UserType>> usersBefore = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users before import", usersBefore);
        assertEquals("Unexpected number of users", getNumberOfUsers() + 1, usersBefore.size());

        PrismObject<UserType> rapp = getUser(USER_RAPP_OID);
        assertNotNull("No rapp", rapp);
        // Rapp has dummy account but it is not linked
        assertLiveLinks(rapp, 0);

        loginImportUser();

        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS, task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        loginAdministrator();

        waitForTaskFinish(task, 40000);

        // THEN
        then();
        TestUtil.assertSuccess(task.getResult());

        dumpStatistics(task);
        // @formatter:off
        assertTask(task, "task after")
                .display()
                .rootSynchronizationInformation()
                    .display()
                    .assertTransition(LINKED, LINKED, LINKED, null, 1, 0, 0) // stan
                    .assertTransition(null, LINKED, LINKED, null, 2, 0, 0) // guybrush, elaine
                    .assertTransition(null, UNLINKED, LINKED, null, 1, 0, 0) // rapp
                    .assertTransition(null, UNMATCHED, LINKED, null, 1, 0, 0) // ht
                    .assertTransition(null, null, null, PROTECTED, 0, 0, 2) // daviejones, calypso
                    .assertTransitions(5)
                    .end()
                .rootItemProcessingInformation()
                    .display()
                    .assertTotalCounts(7, 0)
                    .end()
                .assertProgress(7);
        // @formatter:on

        // Two additional fetch operations are because of the need to classify elaine's blue and red accounts.
        // (They are initially created without kind/intent.) See also PERFORMANCE_ADVISOR log entries.
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 5);

        List<PrismObject<UserType>> usersAfter = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", usersAfter);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp after", userRappAfter);
        PrismAsserts.assertPropertyValue(userRappAfter, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        assertEquals("Unexpected number of users", getNumberOfUsers() + 2, usersAfter.size());

        // Check audit
        assertImportAuditModifications(4);
    }

    /**
     * Background import.
     */
    @Test
    public void test155ImportFromResourceDummyAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        loginImportUser();

        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS, task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        loginAdministrator();

        waitForTaskFinish(task, 40000);

        // THEN
        then();
        assertSuccess(task.getResult());

        dumpStatistics(task);
        // @formatter:off
        assertTask(task, "task after")
                .display()
                .rootSynchronizationInformation()
                    .display()
                    .assertTransition(LINKED, LINKED, LINKED, null, 5, 0, 0) // stan, guybrush, elaine, rapp, ht
                    .assertTransition(null, null, null, PROTECTED, 0, 0, 2) // daviejones, calypso
                    .assertTransitions(2)
                    .end()
                .rootItemProcessingInformation()
                    .display()
                    .assertTotalCounts(7, 0)
                    .end()
                .assertProgress(7);
        // @formatter:on

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 3);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp after", userRappAfter);
        PrismAsserts.assertPropertyValue(userRappAfter, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        assertEquals("Unexpected number of users", getNumberOfUsers() + 2, users.size());

        // Check audit
        assertImportAuditModifications(0);
    }

    /**
     * Background import.
     */
    @Test
    public void test160ImportFromResourceDummyLime() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Preconditions
        assertUsers(getNumberOfUsers() + 2);
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        displayDumpable("Rapp lime account before", dummyResourceLime.getAccountByName(USER_RAPP_USERNAME));

        PrismObject<UserType> userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before", userRappBefore);
        PrismAsserts.assertPropertyValue(userRappBefore, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        loginImportUser();

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_LIME_OID, DUMMY_LIME_ACCOUNT_OBJECT_CLASS, task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        loginAdministrator();

        waitForTaskFinish(task, 40000);

        // THEN
        then();
        TestUtil.assertSuccess(task.getResult());

        dumpStatistics(task);
        // @formatter:off
        assertTask(task, "task after")
                .display()
                .rootSynchronizationInformation()
                    .display()
                    .assertTransition(null, UNLINKED, LINKED, null, 1, 0, 0) // rapp
                    .assertTransition(null, UNMATCHED, LINKED, null, 2, 0, 0) // rum, murray
                    .assertTransitions(2)
                    .end()
                .rootItemProcessingInformation()
                    .display()
                    .assertTotalCounts(3, 0)
                    .end()
                .assertProgress(3);
        // @formatter:on

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 2);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_RUM_NAME, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_MURRAY_NAME, RESOURCE_DUMMY_LIME_OID);

        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp after", userRappAfter);
        PrismAsserts.assertPropertyValue(userRappAfter, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        displayDumpable("Rapp lime account after", dummyResourceLime.getAccountByName(USER_RAPP_USERNAME));

        assertUsers(getNumberOfUsers() + 4);

        // Check audit
        assertImportAuditModifications(3);
    }

    /**
     * Import limited to single object: MID-6798. (Legacy specification migrated to the new one.)
     */
    @Test
    public void test161aImportFromResourceDummyLimeLimitedLegacy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Preconditions
        assertUsers(getNumberOfUsers() + 4);
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        loginImportUser();

        when();

        addObject(TASK_IMPORT_DUMMY_LIME_LIMITED_MIGRATED, task, result);
        loginAdministrator();
        waitForTaskFinish(TASK_IMPORT_DUMMY_LIME_LIMITED_MIGRATED.oid);

        then();

        Task importTask = taskManager.getTaskPlain(TASK_IMPORT_DUMMY_LIME_LIMITED_MIGRATED.oid, result);

        dumpStatistics(importTask);
        // @formatter:off
        assertTask(importTask, "task after")
                .display()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(1, 0)
                    .end()
                    .progress()
                        .display()
                        .assertSuccessCount(1, false)
                    .end()
                .end()
                .assertProgress(1);
        // @formatter:on
    }

    /**
     * Import limited to single object: MID-6798. (Activity-based specification.)
     */
    @Test
    public void test161bImportFromResourceDummyLimeLimited() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Preconditions
        assertUsers(getNumberOfUsers() + 4);
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        loginImportUser();

        when();

        addObject(TASK_IMPORT_DUMMY_LIME_LIMITED, task, result);
        loginAdministrator();
        waitForTaskFinish(TASK_IMPORT_DUMMY_LIME_LIMITED.oid);

        then();

        Task importTask = taskManager.getTaskPlain(TASK_IMPORT_DUMMY_LIME_LIMITED.oid, result);

        dumpStatistics(importTask);
        // @formatter:off
        assertTask(importTask, "task after")
                .display()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(1, 0)
                    .end()
                    .progress()
                        .display()
                        .assertSuccessCount(1, false)
                    .end()
                .end()
                .assertProgress(1);
        // @formatter:on
    }

    /**
     * MID-2427
     */
    @Test
    public void test162ImportFromResourceDummyLimeRappOrganizationScummBar() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount accountRappLimeBefore = dummyResourceLime.getAccountByName(USER_RAPP_USERNAME);
        accountRappLimeBefore.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                ORG_SCUMM_BAR_NAME);
        displayDumpable("Rapp lime account before", accountRappLimeBefore);

        // Preconditions

        PrismObject<UserType> userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before", userRappBefore);
        PrismAsserts.assertPropertyValue(userRappBefore, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));
        assertNoAssignments(userRappBefore);

        assertUsers(getNumberOfUsers() + 4);

        loginImportUser();

        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_LIME_OID, DUMMY_LIME_ACCOUNT_OBJECT_CLASS, task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        loginAdministrator();

        waitForTaskFinish(task, 40000);

        // THEN
        then();
        TestUtil.assertSuccess(task.getResult());

        dumpStatistics(task);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 2);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_RUM_NAME, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_MURRAY_NAME, RESOURCE_DUMMY_LIME_OID);

        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp after", userRappAfter);
        PrismAsserts.assertPropertyValue(userRappAfter, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));
        PrismAsserts.assertPropertyValue(userRappAfter, UserType.F_ORGANIZATION,
                PolyString.fromOrig(ORG_SCUMM_BAR_NAME));

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        DummyAccount accountRappLimeAfter = dummyResourceLime.getAccountByName(USER_RAPP_USERNAME);
        displayDumpable("Rapp lime account after", accountRappLimeAfter);
        assertAssignedOrg(userRappAfter, ORG_SCUMM_BAR_OID);
        assertAssignments(userRappAfter, 1);

        assertUsers(getNumberOfUsers() + 4);

        // Check audit
        assertImportAuditModifications(1);
    }

    /**
     * MID-2427
     */
    @Test
    public void test164ImportFromResourceDummyLimeRappOrganizationNull() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount accountRappLimeBefore = dummyResourceLime.getAccountByName(USER_RAPP_USERNAME);
        accountRappLimeBefore.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME /* no value */);
        displayDumpable("Rapp lime account before", accountRappLimeBefore);

        // Preconditions

        PrismObject<UserType> userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before", userRappBefore);
        PrismAsserts.assertPropertyValue(userRappBefore, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));
        PrismAsserts.assertPropertyValue(userRappBefore, UserType.F_ORGANIZATION,
                PolyString.fromOrig(ORG_SCUMM_BAR_NAME));
        assertAssignedOrg(userRappBefore, ORG_SCUMM_BAR_OID);
        assertAssignments(userRappBefore, 1);

        assertUsers(getNumberOfUsers() + 4);

        loginImportUser();

        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_LIME_OID, DUMMY_LIME_ACCOUNT_OBJECT_CLASS, task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        loginAdministrator();

        waitForTaskFinish(task, 40000);

        // THEN
        then();
        assertSuccess(task.getResult());

        dumpStatistics(task);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 2);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_RUM_NAME, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_MURRAY_NAME, RESOURCE_DUMMY_LIME_OID);

        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp after", userRappAfter);
        PrismAsserts.assertPropertyValue(userRappAfter, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));
        PrismAsserts.assertNoItem(userRappAfter, UserType.F_ORGANIZATION);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        DummyAccount accountRappLimeAfter = dummyResourceLime.getAccountByName(USER_RAPP_USERNAME);
        displayDumpable("Rapp lime account after", accountRappLimeAfter);
        assertNoAssignments(userRappAfter);

        assertUsers(getNumberOfUsers() + 4);

        // Check audit
        assertImportAuditModifications(1);
    }

    @Test
    public void test200ReconcileDummy() throws Exception {
        // GIVEN
        loginAdministrator();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        given("Lets do some local changes on dummy resource");
        DummyAccount guybrushDummyAccount = getDummyResource().getAccountByName(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        // fullname has a normal outbound mapping, this change should NOT be corrected
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Dubrish Freepweed");

        // location has strong outbound mapping, this change should be corrected
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "The Forbidded Dodecahedron");

        // Weapon has a weak mapping, this change should be left as it is
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Feather duster");

        // Drink is not tolerant. The extra values should be removed
        guybrushDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water");

        // Quote is tolerant. The extra values should stay as they are
        guybrushDummyAccount.addAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "I want to be a pirate!");

        // Calypso is protected, this should not reconcile
        DummyAccount calypsoDummyAccount = getDummyResource().getAccountByName(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        calypsoDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Calypso");

        PrismObject<UserType> userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before", userRappBefore);
        PrismAsserts.assertPropertyValue(userRappBefore, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        reconciliationResultListener.clear();

        when("reconciliation task is run");
        importObject(TASK_RECONCILE_DUMMY_SINGLE, task, result);

        then("task should be OK");

        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_DUMMY_OID);
        dumpStatistics(taskAfter);

        // @formatter:off
        assertTask(taskAfter, "task after")
                .display()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(LINKED, LINKED, LINKED, null, 5, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, 2)
                        .assertTransitions(2)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(5, 0, 2)
                    .end()
                    .progress()
                        .display()
                        .assertCommitted(5, 0, 2)
                        .assertNoUncommitted()
                    .end()
                .end()
                .activityState(RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(0, 0, 0)
                    .end()
                    .progress()
                        .display()
                        .assertNoCommitted()
                        .assertNoUncommitted()
                    .end()
                .end()
                .assertProgress(7);
        // @formatter:on

        and("given number of fetch operations should be there");
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 4);

        and("reconciliation result listener should contain correct counters");
        reconciliationResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 0, 0);

        and("users should be as expected");
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);

        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);

        and("attributes on the resource should be as expected");
        // Guybrushes fullname should NOT be corrected back to real fullname
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Dubrish Freepweed");
        // Guybrushes location should be corrected back to real value
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                "Melee Island");
        // Guybrushes weapon should be left untouched
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME,
                "Feather duster");
        // Guybrushes drink should be corrected
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                "rum");
        // Guybrushes quotes should be left untouched
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                "Arr!", "I want to be a pirate!");

        and("user rapp should be as expected");
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp after", userRappAfter);
        PrismAsserts.assertPropertyValue(userRappAfter, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        and("herman should be there");
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);

        and("users for protected accounts should NOT be imported nor touched");
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        and("here we display misc things");
        assertEquals("Unexpected number of users", getNumberOfUsers() + 4, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        and("appropriate scripts should be executed");
        display("Script history", getDummyResource().getScriptHistory());

        ArrayList<ProvisioningScriptSpec> scripts = new ArrayList<>();
        addReconScripts(scripts, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", false);
        addReconScripts(scripts, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        addReconScripts(scripts, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", false);
        addReconScripts(scripts, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, false);
        addReconScripts(scripts, ACCOUNT_STAN_NAME, ACCOUNT_STAN_FULLNAME, false);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), scripts.toArray(new ProvisioningScriptSpec[0]));

        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_OID);

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
    }

    @Test
    public void test210ReconcileDummyBroken() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Lets do some local changes on dummy resource ...
        DummyAccount guybrushDummyAccount = getDummyResource().getAccountByName(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        // location has strong outbound mapping, this change should be corrected
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Phatt Island");

        // BREAK it!
        getDummyResource().setBreakMode(BreakMode.NETWORK);

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        // WHEN
        when();
        restartTask(TASK_RECONCILE_DUMMY_OID);
        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, DEFAULT_TASK_WAIT_TIMEOUT, true);

        // THEN
        then();

        dumpStatistics(taskAfter);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconciliation (broken resource)", users);

        // Total error in the recon process. No reasonable result here.
//        reconciliationTaskResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 1, 0);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        assertEquals("Unexpected number of users", getNumberOfUsers() + 4, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        display("Script history", getDummyResource().getScriptHistory());

        // no scripts
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory());

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertFailure(reconTaskResult);

        // Check audit
        displayDumpable("Audit", dummyAuditService);

        dummyAuditService.assertRecords(0);
    }

    /**
     * Simply re-run recon after the resource is fixed. This should correct the data.
     */
    @Test
    public void test219ReconcileDummyFixed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Fix it!
        getDummyResource().setBreakMode(BreakMode.NONE);

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        restartTask(TASK_RECONCILE_DUMMY_OID);
        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, DEFAULT_TASK_WAIT_TIMEOUT, true);

        // THEN
        then();

        dumpStatistics(taskAfter);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 4);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 0, 0);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Dubrish Freepweed");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                "Melee Island");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME,
                "Feather duster");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                "rum");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                "Arr!", "I want to be a pirate!");

        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertEquals("Unexpected number of users", getNumberOfUsers() + 4, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        display("Script history", getDummyResource().getScriptHistory());

        ArrayList<ProvisioningScriptSpec> scripts = new ArrayList<>();
        addReconScripts(scripts, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", false);
        addReconScripts(scripts, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        addReconScripts(scripts, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", false);
        addReconScripts(scripts, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, false);
        addReconScripts(scripts, ACCOUNT_STAN_NAME, ACCOUNT_STAN_FULLNAME, false);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), scripts.toArray(new ProvisioningScriptSpec[0]));

        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_OID);

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
    }

    /**
     * The resource itself works, just the guybrush account is broken.
     */
    @Test
    public void test220ReconcileDummyBrokenGuybrush() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Lets do some local changes on dummy resource ...
        DummyAccount guybrushDummyAccount = getDummyResource().getAccountByName(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);

        // location has strong outbound mapping, this change should be corrected
        guybrushDummyAccount.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Forbidden Dodecahedron");

        // BREAK it!
        getDummyResource().setBreakMode(BreakMode.NONE);
        guybrushDummyAccount.setModifyBreakMode(BreakMode.NETWORK);

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        // WHEN
        when();
        restartTask(TASK_RECONCILE_DUMMY_OID);
        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, DEFAULT_TASK_WAIT_TIMEOUT, true);

        // THEN
        then();

        dumpStatistics(taskAfter);
        // @formatter:off
        assertTask(taskAfter, "task after")
                .display()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .display()
                    .synchronizationStatistics()
                        .assertTransition(LINKED, LINKED, LINKED, null, 4, 1, 0) // error is guybrush
                        .assertTransition(null, null, null, PROTECTED, 0, 0, 2)
                        .assertTransitions(2)
                        .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(6, 1)
                    .end()
                    .progress()
                        .display() // TODO asserts
                    .end()
                ;
                //.assertProgress(7); // TODO - specify meaning of progress for reconciliation tasks
        // @formatter:on

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconciliation (broken resource account)", users);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 1, 0);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        assertEquals("Unexpected number of users", getNumberOfUsers() + 4, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        display("Script history", getDummyResource().getScriptHistory());
        ArrayList<ProvisioningScriptSpec> scripts = new ArrayList<>();
        addReconScripts(scripts, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", false);
        // Guybrush is broken.
        addReconScripts(scripts, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true, false);
        addReconScripts(scripts, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", false);
        addReconScripts(scripts, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, false);
        addReconScripts(scripts, ACCOUNT_STAN_NAME, ACCOUNT_STAN_FULLNAME, false);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), scripts.toArray(new ProvisioningScriptSpec[0]));

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertStatus(reconTaskResult, OperationResultStatusType.PARTIAL_ERROR);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_OID);
    }

    /**
     * Simply re-run recon after the resource is fixed. This should correct the data.
     */
    @Test
    public void test229ReconcileDummyFixed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Fix it!
        getDummyResource().setBreakMode(BreakMode.NONE);
        getDummyResource().getAccountByName(ACCOUNT_GUYBRUSH_DUMMY_USERNAME).setModifyBreakMode(BreakMode.NONE);

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        reconciliationResultListener.clear();

        // WHEN
        when();
        restartTask(TASK_RECONCILE_DUMMY_OID);
        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, DEFAULT_TASK_WAIT_TIMEOUT, true);

        // THEN
        then();

        dumpStatistics(taskAfter);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 4);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 0, 0);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Dubrish Freepweed");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                "Melee Island");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME,
                "Feather duster");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                "rum");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                "Arr!", "I want to be a pirate!");

        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp after", userRappAfter);
        PrismAsserts.assertPropertyValue(userRappAfter, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME, RESOURCE_DUMMY_OID);
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertEquals("Unexpected number of users", getNumberOfUsers() + 4, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        display("Script history", getDummyResource().getScriptHistory());

        ArrayList<ProvisioningScriptSpec> scripts = new ArrayList<>();
        addReconScripts(scripts, ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", false);
        addReconScripts(scripts, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", true);
        addReconScripts(scripts, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", false);
        addReconScripts(scripts, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, false);
        addReconScripts(scripts, ACCOUNT_STAN_NAME, ACCOUNT_STAN_FULLNAME, false);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), scripts.toArray(new ProvisioningScriptSpec[0]));

        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_OID);

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);
        TestUtil.assertSuccess(reconTaskResult);
    }

    @Test
    public void test230ReconcileDummyRename() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        getDummyResource().setBreakMode(BreakMode.NONE);
        getDummyResource().getAccountByName(ACCOUNT_GUYBRUSH_DUMMY_USERNAME).setModifyBreakMode(BreakMode.NONE);

        PrismObject<UserType> userHerman = findUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        String hermanShadowOid = getSingleLinkOid(userHerman);

        assertShadows(14);

        getDummyResource().renameAccount(ACCOUNT_HERMAN_DUMMY_USERNAME, ACCOUNT_HERMAN_DUMMY_USERNAME, ACCOUNT_HTM_NAME);
        DummyAccount dummyAccountHtm = getDummyAccount(null, ACCOUNT_HTM_NAME);
        dummyAccountHtm.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_HTM_FULL_NAME);

        getDummyResource().purgeScriptHistory();
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);
        reconciliationResultListener.clear();

        // WHEN
        when();
        restartTask(TASK_RECONCILE_DUMMY_OID);
        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_DUMMY_OID, DEFAULT_TASK_WAIT_TIMEOUT, true);

        // THEN
        then();

        dumpStatistics(taskAfter);

        // @formatter:off
        assertTask(taskAfter, "task after")
                .display()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(LINKED, LINKED, LINKED, null, 4, 0, 0) // guybrush, elaine, rapp, stan
                        .assertTransition(null, UNMATCHED, LINKED, null, 1, 0, 0) // htm (new name for ht)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, 2) // daviejones, calypso
                        .assertTransitions(3)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(5, 0, 2)
                    .end()
                    .progress()
                        .display()
                        .assertCommitted(5, 0, 2)
                        .assertNoUncommitted()
                    .end()
                .end()
                .activityState(RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .synchronizationStatistics()
                        .display()
                        // for ht (old name for htm)
                        .assertTransition(LINKED, DELETED, DELETED, null, 1, 0, 0)
                        // two protected accounts (daviejones, calypso)
                        .assertTransitions(1)
                    .end()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(1, 0, 0) // 1 renamed
                    .end()
                    .progress()
                        .display()
                        .assertCommitted(1, 0, 0)
                        .assertNoUncommitted()
                    .end()
                .end()
                .assertProgress(8);
        // @formatter:on

        dumpShadowSituations(RESOURCE_DUMMY_OID, result);

        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 4);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_OID, 0, 7, 0, 1);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after import", users);

        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME); // not deleted. reaction=unlink

        assertRepoShadow(hermanShadowOid)
                .assertTombstone();

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Dubrish Freepweed");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME,
                "Melee Island");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME,
                "Feather duster");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                "rum");
        assertDummyAccountAttribute(null, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME,
                "Arr!", "I want to be a pirate!");

        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertEquals("Unexpected number of users", getNumberOfUsers() + 5, users.size());

        displayValue("Dummy resource", getDummyResource().debugDump());

        display("Script history", getDummyResource().getScriptHistory());

        ArrayList<ProvisioningScriptSpec> scripts = new ArrayList<>();
        addReconScripts(scripts, ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", false);
        addReconScripts(scripts, ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", false);
        addReconScripts(scripts, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, false);
        addReconScripts(scripts, ACCOUNT_STAN_NAME, ACCOUNT_STAN_FULLNAME, false);
        addReconScripts(scripts, ACCOUNT_HTM_NAME, ACCOUNT_HTM_FULL_NAME, true);
        IntegrationTestTools.assertScripts(getDummyResource().getScriptHistory(), scripts.toArray(new ProvisioningScriptSpec[0]));

        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_OID);

        assertShadows(15);

        // Task result
        PrismObject<TaskType> reconTaskAfter = getTask(TASK_RECONCILE_DUMMY_OID);
        OperationResultType reconTaskResult = reconTaskAfter.asObjectable().getResult();
        display("Recon task result", reconTaskResult);

        // There's (expected) "object not found" error related to ht that was renamed.
        TestUtil.assertSuccess("reconciliation", reconTaskResult, 4);
    }

    private void addReconScripts(Collection<ProvisioningScriptSpec> scripts, String username, String fullName, boolean modified) {
        addReconScripts(scripts, username, fullName, modified, true);
    }

    private void addReconScripts(Collection<ProvisioningScriptSpec> scripts, String username, String fullName,
            boolean modified, boolean afterRecon) {
        // before recon
        ProvisioningScriptSpec script = new ProvisioningScriptSpec("The vorpal blade went snicker-snack!");
        script.addArgSingle("who", username);
        scripts.add(script);

        if (modified) {
            script = new ProvisioningScriptSpec("Beware the Jabberwock, my son!");
            script.addArgSingle("howMuch", null);
            script.addArgSingle("howLong", "from here to there");
            script.addArgSingle("who", username);
            script.addArgSingle("whatchacallit", fullName);
            scripts.add(script);
        }

        if (afterRecon) {
            // after recon
            script = new ProvisioningScriptSpec("He left it dead, and with its head");
            script.addArgSingle("how", "enabled");
            scripts.add(script);
        }
    }

    /**
     * Create illegal (non-correlable) account. See that it is disabled.
     */
    @Test
    public void test300ReconcileDummyAzureAddAccountOtis() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        getDummyResource().setBreakMode(BreakMode.NONE);
        dummyResourceAzure.setBreakMode(BreakMode.NONE);

        // Create some illegal account
        dummyResourceCtlAzure.addAccount(ACCOUNT_OTIS_NAME, ACCOUNT_OTIS_FULLNAME);
        displayDumpable("Otis account before", dummyResourceAzure.getAccountByName(ACCOUNT_OTIS_NAME));

        dummyResourceAzure.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        when();
        TASK_RECONCILE_DUMMY_AZURE.rerun(result);

        then();
        dumpStatistics(getTask(TASK_RECONCILE_DUMMY_AZURE.oid));

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_AZURE_OID, 0, 1, 0, 0);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);

        // Otis
        assertNoImportedUserByUsername(ACCOUNT_OTIS_NAME);
        displayDumpable("Otis account after", dummyResourceAzure.getAccountByName(ACCOUNT_OTIS_NAME));
        assertDummyAccount(RESOURCE_DUMMY_AZURE_NAME, ACCOUNT_OTIS_NAME, ACCOUNT_OTIS_FULLNAME, false);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertEquals("Unexpected number of users", getNumberOfUsers() + 5, users.size());

        displayValue("Dummy resource (azure)", dummyResourceAzure.debugDump());

        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_AZURE.oid);

        assertShadows(17);
    }

    @Test
    public void test310ReconcileDummyAzureAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        getDummyResource().setBreakMode(BreakMode.NONE);
        dummyResourceAzure.setBreakMode(BreakMode.NONE);

        PrismObject<TaskType> reconTask = getTask(TASK_RECONCILE_DUMMY_AZURE.oid);
        display("Recon task", reconTask);

        dummyResourceAzure.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        when();
        TASK_RECONCILE_DUMMY_AZURE.rerun(result);

        then();
        dumpStatistics(getTask(TASK_RECONCILE_DUMMY_AZURE.oid));

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_AZURE_OID, 0, 1, 0, 0);

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);

        // Otis
        assertNoImportedUserByUsername(ACCOUNT_OTIS_NAME);
        assertDummyAccount(RESOURCE_DUMMY_AZURE_NAME, ACCOUNT_OTIS_NAME, ACCOUNT_OTIS_FULLNAME, false);

        PrismObject<UserType> userRappAfter = getUser(USER_RAPP_OID);
        display("User rapp after", userRappAfter);
        PrismAsserts.assertPropertyValue(userRappAfter, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertEquals("Unexpected number of users", getNumberOfUsers() + 5, users.size());

        displayValue("Dummy resource (azure)", dummyResourceAzure.debugDump());

        assertReconAuditModifications(0, TASK_RECONCILE_DUMMY_AZURE.oid);

        assertShadows(17);
    }

    @Test
    public void test320ReconcileDummyAzureDeleteOtis() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        getDummyResource().setBreakMode(BreakMode.NONE);
        dummyResourceAzure.setBreakMode(BreakMode.NONE);

        assertShadows(17);

        PrismObject<ShadowType> otisShadow = findShadowByName(
                ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT, ACCOUNT_OTIS_NAME, resourceDummyAzure, result);

        dummyResourceAzure.deleteAccountByName(ACCOUNT_OTIS_NAME);

        dummyResourceAzure.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        when();
        TASK_RECONCILE_DUMMY_AZURE.rerun(result);

        then();
        dumpStatistics(getTask(TASK_RECONCILE_DUMMY_AZURE.oid));

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_AZURE_OID, 0, 0, 0, 1);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);

        // Otis
        assertNoImportedUserByUsername(ACCOUNT_OTIS_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_AZURE_NAME, ACCOUNT_OTIS_NAME);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertRepoShadow(otisShadow.getOid())
                .assertTombstone();

        assertShadows(17);

        assertEquals("Unexpected number of users", getNumberOfUsers() + 5, users.size());

        displayValue("Dummy resource (azure)", dummyResourceAzure.debugDump());

        assertReconAuditModifications(0, TASK_RECONCILE_DUMMY_AZURE.oid);
    }

    /**
     * Create account that will correlate to existing user.
     * See that it is linked and modified.
     * MID-4997
     */
    @Test
    public void test330ReconcileDummyAzureAddAccountRapp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        getDummyResource().setBreakMode(BreakMode.NONE);
        dummyResourceAzure.setBreakMode(BreakMode.NONE);

        dummyResourceCtlAzure.addAccount(USER_RAPP_USERNAME, USER_RAPP_FULLNAME);
        displayDumpable("Rapp azure account before", dummyResourceAzure.getAccountByName(USER_RAPP_USERNAME));

        PrismObject<UserType> userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before", userRappBefore);
        PrismAsserts.assertPropertyValue(userRappBefore, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        dummyResourceAzure.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        when();
        TASK_RECONCILE_DUMMY_AZURE.rerun(result);

        then();
        dumpStatistics(getTask(TASK_RECONCILE_DUMMY_AZURE.oid));

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_AZURE_OID, 0, 1, 0, 1);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);

        // Rapp
        displayDumpable("Rapp azure account after", dummyResourceAzure.getAccountByName(USER_RAPP_USERNAME));
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID, RESOURCE_DUMMY_AZURE_OID);
        assertDummyAccount(RESOURCE_DUMMY_AZURE_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_AZURE_NAME, USER_RAPP_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Elaine");

        //Checking password policy
        PrismObject<UserType> userRapp = findUserByUsername(USER_RAPP_USERNAME);
        assertNotNull("No user Rapp", userRapp);
        UserType userTypeRapp = userRapp.asObjectable();

        assertNotNull("User Rapp has no credentials", userTypeRapp.getCredentials());
        PasswordType password = userTypeRapp.getCredentials().getPassword();
        assertNotNull("User Rapp has no password", password);

        ProtectedStringType passwordType = password.getValue();

        String stringPassword = null;
        if (passwordType.getClearValue() == null) {
            stringPassword = protector.decryptString(passwordType);
        }

        assertNotNull("No clear text password", stringPassword);
        assertTrue("Rapp's password is supposed to contain letter a: " + stringPassword, stringPassword.contains("a"));

        PrismObject<ValuePolicyType> passwordPolicy = getObjectViaRepo(ValuePolicyType.class, PASSWORD_POLICY_LOWER_CASE_ALPHA_AZURE.oid);

        valuePolicyProcessor.validateValue(
                stringPassword, passwordPolicy.asObjectable(),
                createUserOriginResolver(userRapp), getTestNameShort(), task, result);
        boolean isPasswordValid = result.isAcceptable();
        assertTrue("Password doesn't satisfy password policy, generated password: " + stringPassword, isPasswordValid);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertEquals("Unexpected number of users", getNumberOfUsers() + 5, users.size());

        displayValue("Dummy resource (azure)", dummyResourceAzure.debugDump());

        assertReconAuditModifications(2, TASK_RECONCILE_DUMMY_AZURE.oid); // password via inbounds is generated twice
    }

    /**
     * Make a repository modification of the user Rapp. Run recon. See that the
     * account is modified.
     */
    @Test
    public void test332ModifyUserRappAndReconcileDummyAzure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        getDummyResource().setBreakMode(BreakMode.NONE);
        dummyResourceAzure.setBreakMode(BreakMode.NONE);

        displayDumpable("Rapp azure account before", dummyResourceAzure.getAccountByName(USER_RAPP_USERNAME));
        assertDummyAccountAttribute(RESOURCE_DUMMY_AZURE_NAME, USER_RAPP_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Elaine");

        PrismObject<UserType> userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before", userRappBefore);
        PrismAsserts.assertPropertyValue(userRappBefore, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of The Elaine"));

        ObjectDelta<UserType> userRappDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, USER_RAPP_OID,
                        UserType.F_ORGANIZATIONAL_UNIT, PolyString.fromOrig("The six feet under crew"));
        repositoryService.modifyObject(UserType.class, USER_RAPP_OID, userRappDelta.getModifications(), result);

        userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before (modified)", userRappBefore);
        PrismAsserts.assertPropertyValue(userRappBefore, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The six feet under crew"));

        dummyResourceAzure.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        when();
        TASK_RECONCILE_DUMMY_AZURE.rerun(result);

        then();
        dumpStatistics(getTask(TASK_RECONCILE_DUMMY_AZURE.oid));

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_AZURE_OID, 0, 1, 0, 1);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);

        // Rapp
        displayDumpable("Rapp azure account after", dummyResourceAzure.getAccountByName(USER_RAPP_USERNAME));
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID, RESOURCE_DUMMY_AZURE_OID);
        assertDummyAccount(RESOURCE_DUMMY_AZURE_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_AZURE_NAME, USER_RAPP_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The six feet under crew");

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertEquals("Unexpected number of users", getNumberOfUsers() + 5, users.size());

        displayValue("Dummy resource (azure)", dummyResourceAzure.debugDump());

        assertReconAuditModifications(2, TASK_RECONCILE_DUMMY_AZURE.oid);
    }

    /**
     * Make a repository modification of the user Rapp: assign role corpse.
     * Run recon. See that the account is modified (added to group).
     * There is associationTargetSearch expression in the role. Make sure that the
     * search is done properly (has baseContext).
     */
    @Test
    public void test334AssignRoleCorpseToRappAndReconcileDummyAzure() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
        getDummyResource().setBreakMode(BreakMode.NONE);
        dummyResourceAzure.setBreakMode(BreakMode.NONE);

        displayDumpable("Rapp azure account before", dummyResourceAzure.getAccountByName(USER_RAPP_USERNAME));
        assertNoDummyGroupMember(RESOURCE_DUMMY_AZURE_NAME, GROUP_CORPSES_NAME, USER_RAPP_USERNAME);

        ObjectDelta<UserType> userRappDelta = createAssignmentUserDelta(USER_RAPP_OID, ROLE_CORPSE.oid,
                RoleType.COMPLEX_TYPE, null, null, true);
        repositoryService.modifyObject(UserType.class, USER_RAPP_OID, userRappDelta.getModifications(), result);

        PrismObject<UserType> userRappBefore = getUser(USER_RAPP_OID);
        display("User rapp before (modified)", userRappBefore);

        dummyResourceAzure.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        when();

        TASK_RECONCILE_DUMMY_AZURE.rerun(result);

        dumpStatistics(getTask(TASK_RECONCILE_DUMMY_AZURE.oid));

        then();

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_AZURE_OID, 0, 1, 0, 1);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);

        // Rapp
        displayDumpable("Rapp azure account after", dummyResourceAzure.getAccountByName(USER_RAPP_USERNAME));
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID, RESOURCE_DUMMY_AZURE_OID);
        assertDummyAccount(RESOURCE_DUMMY_AZURE_NAME, USER_RAPP_USERNAME, USER_RAPP_FULLNAME, true);
        assertDummyGroupMember(RESOURCE_DUMMY_AZURE_NAME, GROUP_CORPSES_NAME, USER_RAPP_USERNAME);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertEquals("Unexpected number of users", getNumberOfUsers() + 5, users.size());

        displayValue("Dummy resource (azure)", dummyResourceAzure.debugDump());

        assertReconAuditModifications(2, TASK_RECONCILE_DUMMY_AZURE.oid); // password via inbounds is generated twice
    }

    /**
     * Account `rapp` is deleted.
     *
     * Among other things, the shadow transition to DELETED sync state should be reported (MID-7724).
     *
     * In the third reconciliation stage (remainingShadows) two accounts are processed:
     *
     * 1. dead `otis` account (deleted in {@link #test320ReconcileDummyAzureDeleteOtis()})
     *     (actually it is questionable if we should process dead shadows in that stage!)
     * 2. now-deleted `rapp` account
     */
    @Test
    public void test339ReconcileDummyAzureDeleteRapp() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        getDummyResource().setBreakMode(BreakMode.NONE);
        dummyResourceAzure.setBreakMode(BreakMode.NONE);

        assertShadows(19);

        // Remove the assignment. It may do bad things later.
        ObjectDelta<UserType> userRappDelta = createAssignmentUserDelta(USER_RAPP_OID, ROLE_CORPSE.oid,
                RoleType.COMPLEX_TYPE, null, null, false);
        repositoryService.modifyObject(UserType.class, USER_RAPP_OID, userRappDelta.getModifications(), result);

        PrismObject<ShadowType> rappShadow = findShadowByName(
                ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT, USER_RAPP_USERNAME, resourceDummyAzure, result);

        dummyResourceAzure.deleteAccountByName(USER_RAPP_USERNAME);

        dummyResourceAzure.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        when();
        TASK_RECONCILE_DUMMY_AZURE.rerun(result);

        then();
        dumpStatistics(getTask(TASK_RECONCILE_DUMMY_AZURE.oid));

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_AZURE_OID, 0, 0, 0, 2);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);

        // Rapp
        assertNoImportedUserByUsername(ACCOUNT_OTIS_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_AZURE_NAME, USER_RAPP_USERNAME);

        assertNoDummyAccount(RESOURCE_DUMMY_AZURE_NAME, ACCOUNT_OTIS_NAME);
        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);
        // Calypso is protected account. Reconciliation should not touch it
        assertDummyAccountAttribute(null, ACCOUNT_CALYPSO_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME,
                "Calypso");

        assertRepoShadow(rappShadow.getOid())
                .assertTombstone();

        assertShadows(19);

        assertEquals("Unexpected number of users", getNumberOfUsers() + 5, users.size());

        displayValue("Dummy resource (azure)", dummyResourceAzure.debugDump());

        // deleting linkRef
        assertReconAuditModifications(1, TASK_RECONCILE_DUMMY_AZURE.oid);

        // @formatter:off
        assertTask(TASK_RECONCILE_DUMMY_AZURE.oid, "after")
                .display()
                .synchronizationInformation(RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .display()
                    // otis (dead shadow) - maybe in the future we won't process such accounts here
                    .assertTransition(DELETED, null, null, SYNCHRONIZATION_NOT_NEEDED, 0, 0, 1)
                    .assertTransition(LINKED, DELETED, DELETED, null, 1, 0, 0) // rapp
                    .assertTransitions(2)
                .end();
        // @formatter:on
    }

    @Test
    public void test400ReconcileDummyLimeAddAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Create some illegal account
        DummyAccount accountKate = dummyResourceCtlLime.addAccount(ACCOUNT_CAPSIZE_NAME, ACCOUNT_CAPSIZE_FULLNAME);
        accountKate.setPassword(ACCOUNT_CAPSIZE_PASSWORD);

        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        // WHEN
        when();
        importObject(TASK_RECONCILE_DUMMY_LIME, task, result);

        // THEN
        then();

        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_DUMMY_LIME.oid);

        dumpStatistics(taskAfter);

        then();

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_LIME_OID, 0, 4, 0, 0);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);

        // Kate Capsize: user should be created
        assertImportedUserByUsername(ACCOUNT_CAPSIZE_NAME, RESOURCE_DUMMY_LIME_OID);
        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_CAPSIZE_NAME);
        assertPassword(userAfter, ACCOUNT_CAPSIZE_PASSWORD);

        assertEquals("Unexpected number of users", getNumberOfUsers() + 6, users.size());

        displayValue("Dummy resource (lime)", dummyResourceLime.debugDump());

        // Audit record structure is somehow complex here.
        // I am not sure about the correct number of mods, but 3 looks good.
        assertReconAuditModifications(3, TASK_RECONCILE_DUMMY_LIME.oid);
    }

    @Test
    public void test401ReconcileDummyLimeKateOnlyEmpty() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount accountKate = dummyResourceLime.getAccountByName(ACCOUNT_CAPSIZE_NAME);
        accountKate.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "");

        PrismObject<UserType> userBefore = findUserByUsername(ACCOUNT_CAPSIZE_NAME);
        PrismAsserts.assertNoItem(userBefore, UserType.F_COST_CENTER);

        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIME_OID);

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // TODO check cached password
//        assertUserAfterByUsername(ACCOUNT_CAPSIZE_NAME)
//                .assertCostCenter("")
//                .links()
//                .by().resourceOid(RESOURCE_DUMMY_LIME_OID).find()
//                .resolveTarget()
//                .display();

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        PrismAsserts.assertModifications(dummyAuditService.getExecutionDelta(0).getObjectDelta(), 7);
        dummyAuditService.assertTarget(userBefore.getOid());
        dummyAuditService.assertExecutionSuccess();

        assertUsers(getNumberOfUsers() + 6);

        displayValue("Dummy resource (lime)", dummyResourceLime.debugDump());
    }

    @Test
    public void test402ReconcileDummyLimeKateOnlyGrog() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount accountKate = dummyResourceLime.getAccountByName(ACCOUNT_CAPSIZE_NAME);
        accountKate.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "grog");

        PrismObject<UserType> userBefore = findUserByUsername(ACCOUNT_CAPSIZE_NAME);

        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIME_OID);

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_CAPSIZE_NAME);
        display("User after reconcile", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_COST_CENTER, "grog");

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        PrismAsserts.assertModifications(dummyAuditService.getExecutionDelta(0).getObjectDelta(), 7);
        dummyAuditService.assertTarget(userBefore.getOid());
        dummyAuditService.assertExecutionSuccess();

        assertUsers(getNumberOfUsers() + 6);

        displayValue("Dummy resource (lime)", dummyResourceLime.debugDump());
    }

    @Test
    public void test403ReconcileDummyLimeKateOnlyNoValue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount accountKate = dummyResourceLime.getAccountByName(ACCOUNT_CAPSIZE_NAME);
        accountKate.replaceAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
        displayValue("Dummy resource (lime)", dummyResourceLime.debugDump());

        PrismObject<UserType> userBefore = findUserByUsername(ACCOUNT_CAPSIZE_NAME);

        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIME_OID);

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_CAPSIZE_NAME);
        display("User after reconcile", userAfter);

        PrismAsserts.assertNoItem(userAfter, UserType.F_COST_CENTER);

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        PrismAsserts.assertModifications(dummyAuditService.getExecutionDelta(0).getObjectDelta(), 7);
        dummyAuditService.assertTarget(userBefore.getOid());
        dummyAuditService.assertExecutionSuccess();

        assertUsers(getNumberOfUsers() + 6);
    }

    @Test
    public void test404ReconcileDummyLimeKateOnlyRum() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount accountKate = dummyResourceLime.getAccountByName(ACCOUNT_CAPSIZE_NAME);
        accountKate.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");

        PrismObject<UserType> userBefore = findUserByUsername(ACCOUNT_CAPSIZE_NAME);

        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIME_OID);

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_CAPSIZE_NAME);
        display("User after reconcile", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_COST_CENTER, "rum");

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        PrismAsserts.assertModifications(dummyAuditService.getExecutionDelta(0).getObjectDelta(), 7);
        dummyAuditService.assertTarget(userBefore.getOid());
        dummyAuditService.assertExecutionSuccess();

        assertUsers(getNumberOfUsers() + 6);

        displayValue("Dummy resource (lime)", dummyResourceLime.debugDump());
    }

    @Test
    public void test405ReconcileDummyLimeKateOnlyEmpty() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount accountKate = dummyResourceLime.getAccountByName(ACCOUNT_CAPSIZE_NAME);
        accountKate.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "");

        PrismObject<UserType> userBefore = findUserByUsername(ACCOUNT_CAPSIZE_NAME);

        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIME_OID);

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_CAPSIZE_NAME);
        display("User after reconcile", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_COST_CENTER, "");

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        PrismAsserts.assertModifications(dummyAuditService.getExecutionDelta(0).getObjectDelta(), 7);
        dummyAuditService.assertTarget(userBefore.getOid());
        dummyAuditService.assertExecutionSuccess();

        assertUsers(getNumberOfUsers() + 6);

        displayValue("Dummy resource (lime)", dummyResourceLime.debugDump());
    }

    @Test
    public void test406ReconcileDummyLimeKateOnlyEmptyAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<UserType> userBefore = findUserByUsername(ACCOUNT_CAPSIZE_NAME);

        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIME_OID);

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_CAPSIZE_NAME);
        display("User after reconcile", userAfter);

        PrismAsserts.assertPropertyValue(userAfter, UserType.F_COST_CENTER, "");

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0);
        dummyAuditService.assertTarget(userBefore.getOid());
        dummyAuditService.assertExecutionSuccess();

        assertUsers(getNumberOfUsers() + 6);

        displayValue("Dummy resource (lime)", dummyResourceLime.debugDump());
    }

    @Test
    public void test410ReconcileDummyLimeKatePassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        DummyAccount accountKate = dummyResourceLime.getAccountByName(ACCOUNT_CAPSIZE_NAME);
        accountKate.setPassword("d0d3c4h3dr0n");

        PrismObject<UserType> userBefore = findUserByUsername(ACCOUNT_CAPSIZE_NAME);

        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIME_OID);

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(ACCOUNT_CAPSIZE_NAME);
        display("User after reconcile", userAfter);

        assertPassword(userAfter, "d0d3c4h3dr0n");

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(1);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        PrismAsserts.assertModifications(dummyAuditService.getExecutionDelta(0).getObjectDelta(), 11);
        dummyAuditService.assertTarget(userBefore.getOid());
        dummyAuditService.assertExecutionSuccess();

        assertUsers(getNumberOfUsers() + 6);

        displayValue("Dummy resource (lime)", dummyResourceLime.debugDump());
    }

    @Test
    public void test420ReconcileDummyLimeDeleteLinkedAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Create some illegal account
        dummyResourceLime.deleteAccountByName(ACCOUNT_CAPSIZE_NAME);

        dummyResourceLime.purgeScriptHistory();
        dummyAuditService.clear();
        reconciliationResultListener.clear();

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_LIME_OID);

        // WHEN
        when();
        restartTask(TASK_RECONCILE_DUMMY_LIME.oid);

        // THEN
        then();

        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_DUMMY_LIME.oid);

        dumpStatistics(taskAfter);

        then();

        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after reconcile", users);

        reconciliationResultListener.assertResult(RESOURCE_DUMMY_LIME_OID, 0, 3, 0, 1);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);

        // Kate Capsize: user should be gone
        assertNoImportedUserByUsername(ACCOUNT_CAPSIZE_NAME);

        assertEquals("Unexpected number of users", getNumberOfUsers() + 5, users.size());

        displayValue("Dummy resource (lime)", dummyResourceLime.debugDump());

        // Audit record structure is somehow complex here.
        // I am not sure about the correct number of mods, but 3 looks good.
        assertReconAuditModifications(3, TASK_RECONCILE_DUMMY_LIME.oid);
    }

    /**
     * Imports a testing account (Taugustus)
     */
    @Test
    public void test500ImportTAugustusFromResourceDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        provisioningService.addObject(ACCOUNT_TAUGUSTUS.getFresh(), null, null, task, result);

        // Preconditions
        assertUsers(getNumberOfUsers() + 5);

        loginImportUser();

        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        modelService.importFromResource(ACCOUNT_TAUGUSTUS.oid, task, result);

        // THEN
        then();
        assertSuccess(result);

        loginAdministrator();

        // First fetch: import handler reading the account
        // Second fetch: fetchback to correctly process inbound (import changes the account).
//        assertShadowFetchOperationCountIncrement(2);

        // WHY???
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        PrismObject<UserType> userAugustusAfter = assertImportedUserByUsername(USER_AUGUSTUS_NAME, RESOURCE_DUMMY_OID);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        assertUsers(getNumberOfUsers() + 6);

        assertShadowKindIntent(ACCOUNT_TAUGUSTUS.oid, ShadowKindType.ACCOUNT, INTENT_TEST);

        display("User augustus after", userAugustusAfter);
        assertLiveLinks(userAugustusAfter, 1);
        PrismAsserts.assertPropertyValue(userAugustusAfter, UserType.F_ORGANIZATIONAL_UNIT,
                PolyString.fromOrig("The crew of Titanicum Augusticum"));

        assertImportAuditModifications(1);
    }

    /**
     * Imports a default account (augustus), it should be linked
     */
    @SuppressWarnings("CommentedOutCode")
    @Test
    public void test502ImportAugustusFromResourceDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        PrismObject<UserType> userAugustusBefore = findUserByUsername(USER_AUGUSTUS_NAME);
        display("User augustus before", userAugustusBefore);

        PrismObject<ShadowType> account = ACCOUNT_AUGUSTUS.getFresh();
        provisioningService.addObject(account, null, null, task, result);
        display("Account augustus before", account);

        // Preconditions
        assertUsers(getNumberOfUsers() + 6);

        loginImportUser();

        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        modelService.importFromResource(ACCOUNT_AUGUSTUS.oid, task, result);

        // THEN
        then();
        assertSuccess(result);

        loginAdministrator();

        // First fetch: import handler reading the account
        // Second fetch: fetchback to correctly process inbound (import changes the account).
//        assertShadowFetchOperationCountIncrement(2);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        PrismObject<UserType> userAugustusAfter = assertImportedUserByUsername(USER_AUGUSTUS_NAME, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_OID);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        assertUsers(getNumberOfUsers() + 6);

        assertShadowKindIntent(ACCOUNT_AUGUSTUS.oid, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
        assertShadowKindIntent(ACCOUNT_TAUGUSTUS.oid, ShadowKindType.ACCOUNT, INTENT_TEST);

        display("User augustus after", userAugustusAfter);
        assertLiveLinks(userAugustusAfter, 2);
        // Gives wrong results now. See MID-2532
//        PrismAsserts.assertPropertyValue(userAugustusAfter, UserType.F_ORGANIZATIONAL_UNIT,
//                createPolyString("The crew of Titanicum Augusticum"),
//                createPolyString("The crew of Boatum Mailum"));

        assertImportAuditModifications(1);
    }

    /**
     * This should import all the intents in the object class
     */
    @Test
    public void test510ImportFromResourceDummy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        provisioningService.addObject(ACCOUNT_KENNY.getFresh(), null, null, task, result);
        provisioningService.addObject(ACCOUNT_TPALIDO.getFresh(), null, null, task, result);
        provisioningService.addObject(ACCOUNT_LECHIMP.getFresh(), null, null, task, result);
        provisioningService.addObject(ACCOUNT_TLECHIMP.getFresh(), null, null, task, result);
        provisioningService.addObject(ACCOUNT_ANDRE.getFresh(), null, null, task, result);
        provisioningService.addObject(ACCOUNT_TANDRE.getFresh(), null, null, task, result);
        provisioningService.addObject(ACCOUNT_TLAFOOT.getFresh(), null, null, task, result);
        provisioningService.addObject(ACCOUNT_CRUFF.getFresh(), null, null, task, result);

        // Preconditions
        assertUsers(getNumberOfUsers() + 6);

        loginImportUser();

        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        modelService.importFromResource(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS, task, result);

        // THEN
        then();
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);

        loginAdministrator();

        waitForTaskFinish(task, 40000);

        dumpStatistics(task);

        // THEN
        then();
        TestUtil.assertSuccess(task.getResult());

        // First fetch: search in import handler
        // 6 fetches: fetchback to correctly process inbound (import changes the account).
        // The accounts are modified during import as there are also outbound mappings in
        // ther dummy resource. As the import is in fact just a recon the "fetchbacks" happens.
        // One is because of counting resource objects before importing them.
//        assertShadowFetchOperationCountIncrement(8);

        // WHY????
//        assertShadowFetchOperationCountIncrement(4);

        assertImportedUserByOid(USER_ADMINISTRATOR_OID);
        assertImportedUserByOid(USER_JACK_OID);
        assertImportedUserByOid(USER_BARBOSSA_OID);
        assertImportedUserByOid(USER_GUYBRUSH_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByOid(USER_RAPP_OID, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_LIME_OID);
        assertImportedUserByUsername(ACCOUNT_HERMAN_DUMMY_USERNAME);
        assertImportedUserByUsername(ACCOUNT_HTM_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_STAN_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(USER_AUGUSTUS_NAME, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_KENNY.name, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(USER_PALIDO_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_LECHIMP.name, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_CRUFF.name, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(USER_LAFOOT_NAME, RESOURCE_DUMMY_OID);
        assertImportedUserByUsername(ACCOUNT_ANDRE.name, RESOURCE_DUMMY_OID, RESOURCE_DUMMY_OID);

        // These are protected accounts, they should not be imported
        assertNoImportedUserByUsername(ACCOUNT_DAVIEJONES_DUMMY_USERNAME);
        assertNoImportedUserByUsername(ACCOUNT_CALYPSO_DUMMY_USERNAME);

        assertUsers(getNumberOfUsers() + 12);

        assertShadowKindIntent(ACCOUNT_AUGUSTUS.oid, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
        assertShadowKindIntent(ACCOUNT_TAUGUSTUS.oid, ShadowKindType.ACCOUNT, INTENT_TEST);
    }

    /**
     * This should reconcile only accounts matched by filter
     */
    @Test
    public void test520ReconResourceDummyFilter() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // Preconditions
        assertUsers(getNumberOfUsers() + 12);

        loginImportUser();

        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        // runPrivileged is necessary for TestImportReconAuthorizations as importObjectFromFile() is using raw operations
        runPrivileged(() -> {
            try {
                importObject(TASK_RECONCILE_DUMMY_FILTER, task, result);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return null;
        });

        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_DUMMY_FILTER.oid, 40000);
        dumpStatistics(taskAfter);
        // @formatter:off
        assertTask(taskAfter, "after")
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .synchronizationStatistics()
                        .assertTransition(LINKED, LINKED, LINKED, null, 12, 0, 0)
                        .assertTransition(null, null, null, PROTECTED, 0, 0, 2)
                        .assertTransitions(2);
        // @formatter:on
    }

    @Test
    public void test600SearchAllDummyAccounts() throws Exception {
        // GIVEN
        loginAdministrator();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> objects = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Found", objects);

        assertEquals("Wrong number of objects found", 17, objects.size());
    }

    @Test
    public void test610SearchDummyAccountsNameSubstring() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query =
                ObjectQueryUtil.createResourceAndObjectClassFilterPrefix(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS)
                        .and().item(
                                SchemaConstants.ICFS_NAME_PATH,
                                ObjectFactory.createSimpleAttributeDefinition(SchemaConstants.ICFS_NAME, DOMUtil.XSD_STRING))
                        .contains("s")
                        .build();

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> objects = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("Found", objects);

        assertEquals("Wrong number of objects found", 6, objects.size());
    }

    /**
     * Imports new (unmatched) user from "archetyped" resource.
     */
    @Test
    public void test650ImportNewArchetypedUser() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String empNo = "650";
        String name = "test650";

        given("there is a new account");
        DummyAccount account = RESOURCE_DUMMY_ARCHETYPED.controller.addAccount(name);
        account.addAttributeValue(ATTR_EMPLOYEE_NUMBER, empNo);

        when("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ARCHETYPED.oid)
                .withNameValue(name)
                .execute(result);

        then("user is created with archetype");
        assertUserAfterByUsername(name)
                .assertArchetypeRef(ARCHETYPE_EMPLOYEE.oid);
    }

    /**
     * Imports an account to be linked to existing user: Tests whether the specified archetype
     * is correctly searched-for during correlation (using items correlator).
     */
    @Test
    public void test660LinkArchetypedUser() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String empNo = "660";
        String name = "test660";
        String unrelatedName = "test660-wrong";
        String relatedName = "test660-ok";

        given("there is a new account");
        DummyAccount account = RESOURCE_DUMMY_ARCHETYPED.controller.addAccount(name);
        account.addAttributeValue(ATTR_EMPLOYEE_NUMBER, empNo);

        and("there is a user with corresponding empno, but without the archetype");
        addObject(
                new UserType()
                        .name(unrelatedName)
                        .employeeNumber(empNo)
                        .asPrismObject(),
                task,
                result);

        and("there is a user with corresponding empno, and with the archetype");
        addObject(
                new UserType()
                        .name(relatedName)
                        .employeeNumber(empNo)
                        .assignment(new AssignmentType()
                                .targetRef(ARCHETYPE_EMPLOYEE.oid, ArchetypeType.COMPLEX_TYPE))
                        .asPrismObject(),
                task,
                result);

        when("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ARCHETYPED.oid)
                .withNameValue(name)
                .execute(result);

        then("archetyped user is linked");
        assertUserAfterByUsername(name) // name is updated by inbound mapping
                .assertLiveLinks(1); // and the account is linked

        and("non-archetyped user (test660-wrong) is ignored");
        assertUserAfterByUsername(unrelatedName)
                .assertLiveLinks(0);
    }

    /**
     * The same as {@link #test660LinkArchetypedUser()} but using traditional filter-based correlator.
     */
    @Test
    public void test670LinkArchetypedUserViaFilter() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String empNo = "670";
        String name = "test670";
        String relatedName = "test670-ok";
        String unrelatedName = "test670-wrong";

        given("there is a new account");
        DummyAccount account = RESOURCE_DUMMY_ARCHETYPED_FILTER_BASED.controller.addAccount(name);
        account.addAttributeValue(ATTR_EMPLOYEE_NUMBER, empNo);

        and("there is a user with corresponding empno, but without the archetype");
        addObject(
                new UserType()
                        .name(unrelatedName)
                        .employeeNumber(empNo)
                        .asPrismObject(),
                task,
                result);

        and("there is a user with corresponding empno, and with the archetype");
        addObject(
                new UserType()
                        .name(relatedName)
                        .employeeNumber(empNo)
                        .assignment(new AssignmentType()
                                .targetRef(ARCHETYPE_EMPLOYEE.oid, ArchetypeType.COMPLEX_TYPE))
                        .asPrismObject(),
                task,
                result);

        when("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ARCHETYPED_FILTER_BASED.oid)
                .withNameValue(name)
                .execute(result);

        then("archetyped user is linked");
        assertUserAfterByUsername(name) // name is updated by inbound mapping
                .assertLiveLinks(1); // and the account is linked

        and("non-archetyped user (test670-wrong) is ignored");
        assertUserAfterByUsername(unrelatedName)
                .assertLiveLinks(0);
    }

    /**
     * Having resource object type with declared focus archetype, we run the synchronization against a user that
     * has no archetype.
     */
    @Test
    public void test680SynchronizeUserWithoutArchetype() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String empNo = "680";
        String name = "test680";

        given("there is a new account, without corresponding user");
        DummyAccount account = RESOURCE_DUMMY_ARCHETYPED.controller.addAccount(name);
        account.addAttributeValue(ATTR_EMPLOYEE_NUMBER, empNo);

        and("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ARCHETYPED.oid)
                .withNameValue(name)
                .execute(result);
        var userOid = assertUserByUsername(name, "after initial creation")
                .assertArchetypeRef(ARCHETYPE_EMPLOYEE.oid)
                .getOid();

        and("the archetype is removed from the owner (in raw mode)");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).replace()
                        .item(UserType.F_ARCHETYPE_REF).replace()
                        .asObjectDelta(userOid),
                ModelExecuteOptions.create().raw(),
                task,
                result);
        assertUser(userOid, "after archetype removal")
                .assertNoArchetypeRef();

        when("the account is imported again");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ARCHETYPED.oid)
                .withNameValue(name)
                .execute(result);

        then("user has archetype");
        assertUserAfter(userOid)
                .assertArchetypeRef(ARCHETYPE_EMPLOYEE.oid);
    }

    /**
     * Having resource object type with declared focus archetype, we run the synchronization against a user that
     * has a conflicting archetype.
     */
    @Test
    public void test690SynchronizeUserWithConflictingArchetype() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String empNo = "690";
        String name = "test690";

        given("there is a new account, without corresponding user");
        DummyAccount account = RESOURCE_DUMMY_ARCHETYPED.controller.addAccount(name);
        account.addAttributeValue(ATTR_EMPLOYEE_NUMBER, empNo);

        and("the account is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ARCHETYPED.oid)
                .withNameValue(name)
                .execute(result);
        var userOid = assertUserByUsername(name, "after initial creation")
                .assertArchetypeRef(ARCHETYPE_EMPLOYEE.oid)
                .getOid();

        and("the archetype of the new owner is changed");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .replace(
                                new AssignmentType()
                                        .targetRef(ARCHETYPE_OTHER.oid, ArchetypeType.COMPLEX_TYPE))
                        .item(UserType.F_ARCHETYPE_REF)
                        .replace(
                                new ObjectReferenceType()
                                        .oid(ARCHETYPE_OTHER.oid)
                                        .type(ArchetypeType.COMPLEX_TYPE))
                        .asObjectDelta(userOid),
                ModelExecuteOptions.create().raw(),
                task,
                result);
        assertUser(userOid, "after archetype change")
                .assertArchetypeRef(ARCHETYPE_OTHER.oid);

        when("the account is imported again");
        var taskOid = importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ARCHETYPED.oid)
                .withNameValue(name)
                .withNotAssertingSuccess()
                .execute(result);

        then("the task has failed");
        assertTask(taskOid, "after")
                .display()
                .assertPartialError()
                .getObjectable();
    }

    /**
     * We check that creating "archetyped" projection leads to the propagation of the archetype to the user.
     *
     * Also, we check that the archetype cannot be changed by explicit delta.
     */
    @Test
    public void test700SetArchetypeFromProjection() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String name = "test700";

        given("assignment policy is RELATIVE");
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        when("a user with archetyped account is created");
        UserType user = new UserType()
                .name(name)
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(RESOURCE_DUMMY_ARCHETYPED.oid, ResourceType.COMPLEX_TYPE)));
        String oid = addObject(user, task, result);

        then("user gets the archetype");
        assertUserAfter(oid)
                .assertArchetypeRef(ARCHETYPE_EMPLOYEE.oid)
                .assertLiveLinks(1); // just to be sure the account was created

        and("an attempt to change the archetype fails");
        try {
            executeChanges(
                    deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT)
                            .replace(
                                    new AssignmentType()
                                            .targetRef(ARCHETYPE_OTHER.oid, ArchetypeType.COMPLEX_TYPE))
                            .asObjectDelta(oid),
                    null,
                    task,
                    result);
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
            assertThat(e.getMessage()).
                    as("exception message")
                    .contains("Trying to enforce archetype:")
                    .contains("but the object has already a different structural archetype");
        }
    }

    /**
     * Reconciles a dead account. See MID-7956.
     */
    @Test
    public void test710ReconcileDeadAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String accountName = "test710";

        given("an account + a shadow exist");
        RESOURCE_DUMMY_GRAVEYARD.controller.addAccount(accountName);
        ObjectQuery query = createResourceAndObjectClassQuery(
                RESOURCE_DUMMY_GRAVEYARD.oid, RI_ACCOUNT_OBJECT_CLASS);
        List<PrismObject<ShadowType>> shadows =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);
        assertThat(shadows).as("shadows").hasSize(1);
        String shadowOid = shadows.get(0).getOid();

        and("the account is deleted, and shadow is marked as dead");
        RESOURCE_DUMMY_GRAVEYARD.controller.deleteAccount(accountName);
        PrismObject<ShadowType> shadowAfterDeletion =
                provisioningService.getObject(ShadowType.class, shadowOid, null, task, result);
        assertShadow(shadowAfterDeletion, "after deletion")
                .display()
                .assertIsDead(true);

        and("time is +20 minutes (after dead shadow retention period)");
        clock.overrideDuration("PT20M");

        when("reconciliation is run");
        TASK_RECONCILE_DUMMY_GRAVEYARD.rerun(result);

        then("reconciliation is OK");
        // @formatter:off
        TASK_RECONCILE_DUMMY_GRAVEYARD.assertAfter()
                .assertClosed()
                .assertSuccess()
                .activityState(RECONCILIATION_OPERATION_COMPLETION_PATH)
                    .progress()
                        .assertCommitted(0, 0, 0)
                    .end()
                .end()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .progress()
                        .assertCommitted(0, 0, 0)
                    .end()
                .end()
                .activityState(RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .progress()
                        .assertCommitted(0, 0, 1) // "success" state would be probably also OK
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Dry-run-reconciling an account that has been deleted. See MID-7927.
     */
    @Test
    public void test720DryRunReconcileDeletedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String accountName = "test720";
        clock.resetOverride();

        given("an account + a shadow exist");
        RESOURCE_DUMMY_REAPING.controller.addAccount(accountName);
        ObjectQuery query = createResourceAndObjectClassQuery(RESOURCE_DUMMY_REAPING.oid, RI_ACCOUNT_OBJECT_CLASS);
        List<PrismObject<ShadowType>> shadows =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);
        assertThat(shadows).as("shadows").hasSize(1);
        String shadowOid = shadows.get(0).getOid();

        and("the account is deleted (midPoint does not know)");
        RESOURCE_DUMMY_REAPING.controller.deleteAccount(accountName);

        when("dry-run reconciliation is run");
        TASK_RECONCILE_DUMMY_REAPING_DRY_RUN.rerun(result);

        then("reconciliation is OK");
        // @formatter:off
        TASK_RECONCILE_DUMMY_REAPING_DRY_RUN.assertAfter()
                .assertClosed()
                .assertSuccess()
                .activityState(RECONCILIATION_OPERATION_COMPLETION_PATH)
                    .progress()
                        .assertCommitted(0, 0, 0)
                    .end()
                .end()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .progress()
                        .assertCommitted(0, 0, 0)
                    .end()
                .end()
                .activityState(RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .progress()
                        .assertCommitted(1, 0, 0)
                    .end()
                .end();
        // @formatter:on

        and("the shadow should be gone");
        assertNoRepoShadow(shadowOid);
    }

    /**
     * Dry-run-reconciling an account that is already dead. See MID-7927.
     */
    @Test
    public void test730DryRunReconcileDeadAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String accountName = "test730";
        clock.resetOverride();

        given("an account + a shadow exist");
        RESOURCE_DUMMY_REAPING.controller.addAccount(accountName);
        ObjectQuery query = createResourceAndObjectClassQuery(RESOURCE_DUMMY_REAPING.oid, RI_ACCOUNT_OBJECT_CLASS);
        List<PrismObject<ShadowType>> shadows =
                provisioningService.searchObjects(ShadowType.class, query, null, task, result);
        assertThat(shadows).as("shadows").hasSize(1);
        String shadowOid = shadows.get(0).getOid();

        and("the account is deleted (midPoint does not know)");
        RESOURCE_DUMMY_REAPING.controller.deleteAccount(accountName);

        and("the account is discovered to be missing (midPoint marks it as dead)");
        PrismObject<ShadowType> shadowAfterDeletion =
                provisioningService.getObject(ShadowType.class, shadowOid, null, task, result);
        assertShadow(shadowAfterDeletion, "after deletion")
                .assertDead()
                .assertIsNotExists();

        when("dry-run reconciliation is run");
        TASK_RECONCILE_DUMMY_REAPING_DRY_RUN.rerun(result);

        then("reconciliation is OK");
        // @formatter:off
        TASK_RECONCILE_DUMMY_REAPING_DRY_RUN.assertAfter()
                .assertClosed()
                .assertSuccess()
                .activityState(RECONCILIATION_OPERATION_COMPLETION_PATH)
                    .progress()
                        .assertCommitted(0, 0, 0)
                    .end()
                .end()
                .activityState(RECONCILIATION_RESOURCE_OBJECTS_PATH)
                    .progress()
                        .assertCommitted(0, 0, 0)
                    .end()
                .end()
                .activityState(RECONCILIATION_REMAINING_SHADOWS_PATH)
                    .progress()
                        .assertCommitted(0, 0, 1) // "not needed" (but can be success as well)
                    .end()
                .end();
        // @formatter:on

        and("the shadow should be gone");
        assertNoRepoShadow(shadowOid);
    }

    /** Reconciling an account that was deleted on the resource. Nothing special for now. MID-10195. */
    @Test
    public void test740ReconcileDeletedAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();

        given("an account imported from the green resource");
        var userOid = createAndImportGreenAccount(accountName, result);

        when("the account is deleted on the resource and reconciliation is run");
        deleteGreenAccount(accountName);
        reconcileAllGreenAccounts(result);

        then("the focus is gone");
        assertNoObject(UserType.class, userOid);
    }

    private void deleteGreenAccount(String accountName) throws Exception {
        getDummyResourceController(RESOURCE_DUMMY_GREEN_NAME).deleteAccount(accountName);
    }

    private String createAndImportGreenAccount(String accountName, OperationResult result) throws Exception {
        getDummyResourceController(RESOURCE_DUMMY_GREEN_NAME).addAccount(accountName);
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_GREEN_OID)
                .withNameValue(accountName)
                .executeOnForeground(result);
        return assertUserBeforeByUsername(accountName)
                .assertLiveLinks(1)
                .getOid();
    }

    private void reconcileAllGreenAccounts(OperationResult result) throws CommonException, IOException {
        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_GREEN_OID)
                .withProcessingAllAccounts()
                .execute(result);
    }

    /** Recompute user with an account that was deleted on the resource (with reconciliation). MID-10195. */
    @Test
    public void test750RecomputeUserWithDeletedAccountWithReconciliation() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();

        given("an account imported from the green resource");
        var userOid = createAndImportGreenAccount(accountName, result);

        when("the account is deleted on the resource and user is recomputed (with reconciliation)");
        deleteGreenAccount(accountName);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_GREEN_OID);

        recomputeUser(userOid, ModelExecuteOptions.create().reconcile(), task, result);

        then("the focus is gone");
        assertWarning(result);
        assertNoObject(UserType.class, userOid);
    }

    /** Recompute user with an account that was deleted on the resource (without reconciliation). MID-10195. */
    @Test
    public void test755RecomputeUserWithDeletedAccountWithoutReconciliation() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();

        given("an account imported from the green resource");
        var userOid = createAndImportGreenAccount(accountName, result);

        when("the account is deleted on the resource and user is recomputed (without reconciliation)");
        deleteGreenAccount(accountName);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_GREEN_OID);

        recomputeUser(userOid, task, result);

        then("the focus is gone");
        assertWarning(result);
        assertNoObject(UserType.class, userOid);
    }

    /** Learns about deleted account (with discovery). MID-10195. */
    @Test
    public void test760GetDeletedAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();

        given("an account imported from the green resource");
        var userOid = createAndImportGreenAccount(accountName, result);
        var shadowOid = assertUser(userOid, "").singleLink().getOid();

        when("the account is deleted on the resource and fetched by midPoint");
        deleteGreenAccount(accountName);

        var shadow = modelService.getObject(ShadowType.class, shadowOid, null, task, result);
        assertShadow(shadow, "")
                .assertDead();

        then("the focus is gone");
        assertNoObject(UserType.class, userOid);
    }

    /** Learns about deleted account (NO discovery) and then runs reconciliation. MID-10195. */
    @Test
    public void test765GetDeletedAccountNoDiscoveryAndReconcile() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();

        given("an account imported from the green resource");
        var userOid = createAndImportGreenAccount(accountName, result);
        var shadowOid = assertUser(userOid, "").singleLink().getOid();

        when("the account is deleted on the resource and fetched by midPoint");
        deleteGreenAccount(accountName);

        var shadow = modelService.getObject(
                ShadowType.class, shadowOid,
                GetOperationOptionsBuilder.create().doNotDiscovery().build(),
                task, result);

        assertShadow(shadow, "")
                .assertDead();

        and("reconciliation is run");
        reconcileAllGreenAccounts(result);

        then("the focus is gone");
        assertNoObject(UserType.class, userOid);
    }

    /**
     * Deleting dummy shadows in raw mode: searching in repo, and deleting from the repo.
     */
    @Test
    public void test900DeleteDummyShadows() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Preconditions
        assertUsers(getNumberOfUsers() + 20);
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        importObject(TASK_DELETE_DUMMY_SHADOWS, task, result);

        // THEN
        then();

        Task taskAfter = waitForTaskFinish(TASK_DELETE_DUMMY_SHADOWS.oid, 20000);
        dumpStatistics(taskAfter);

        // THEN
        then();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        // @formatter:off
        assertTask(TASK_DELETE_DUMMY_SHADOWS.oid, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(18, 0, 0); // deleted also protected shadows
        // @formatter:on

        // Checking operation result internal structure.
        PrismObject<TaskType> deleteTask = getTask(TASK_DELETE_DUMMY_SHADOWS.oid);
        OperationResultType deleteTaskResultBean = deleteTask.asObjectable().getResult();
        display("Final delete task result", deleteTaskResultBean);

        TestUtil.assertSuccess(deleteTaskResultBean);
        OperationResult deleteTaskResult = OperationResult.createOperationResult(deleteTaskResultBean);
        TestUtil.assertSuccess(deleteTaskResult);

        String opProcess = "com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingGatekeeper.process";
        List<OperationResult> processSearchResults = deleteTaskResult.findSubresultsDeeply(opProcess);
        assertThat(processSearchResults).as("'process item' operation results").hasSize(11);
        assertThat(processSearchResults.get(processSearchResults.size() - 1).getHiddenRecordsCount())
                .as("hidden operation results")
                .isEqualTo(8);

        assertUsers(getNumberOfUsers() + 20);

        assertDummyAccountShadows(0, true, task, result);
        assertDummyAccountShadows(17, false, task, result);
    }

    /**
     * Deleting dummy _accounts_ i.e. in non-raw mode: searching on resource, and deleting from the resource.
     */
    @Test
    public void test910DeleteDummyAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Preconditions
        assertUsers(getNumberOfUsers() + 20);
        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        importObject(TASK_DELETE_DUMMY_ACCOUNTS, task, result);

        // THEN
        then();

        Task taskAfter = waitForTaskFinish(TASK_DELETE_DUMMY_ACCOUNTS.oid, 20000);
        dumpStatistics(taskAfter);

        // THEN
        then();
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1); // One search operation

        // @formatter:off
        assertTask(TASK_DELETE_DUMMY_ACCOUNTS.oid, "after")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                        .assertTotalCounts(15, 0, 2); // two protected accounts not deleted
        // @formatter:on

        // Operation result structure is currently not as neat as when pure repo access is used.
        // So let's skip these tests for now.

        assertUsers(getNumberOfUsers() + 20);

        assertDummyAccountShadows(2, true, task, result); // two protected accounts
        assertDummyAccountShadows(2, false, task, result);
    }

    private void assertDummyAccountShadows(int expected, boolean raw, Task task, OperationResult result) throws CommonException {
        ObjectQuery query = createResourceAndObjectClassQuery(RESOURCE_DUMMY_OID, RI_ACCOUNT_OBJECT_CLASS);

        final MutableInt count = new MutableInt(0);
        ResultHandler<ShadowType> handler = (shadow, parentResult) -> {
            count.increment();
            display("Found", shadow);
            return true;
        };
        Collection<SelectorOptions<GetOperationOptions>> options = null;
        if (raw) {
            options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
        }
        modelService.searchObjectsIterative(ShadowType.class, query, handler, options, task, result);
        assertEquals("Unexpected number of search results (raw=" + raw + ")", expected, (int) count.getValue());
    }

    private void assertImportAuditModifications(int expectedModifications) {
        displayDumpable("Audit", dummyAuditService);

        List<AuditEventRecord> auditRecords = dummyAuditService.getRecords();

        int i = 0;
        int modifications = 0;
        for (; i < auditRecords.size() - 1; i += 2) {
            AuditEventRecord requestRecord = auditRecords.get(i);
            assertNotNull("No request audit record (" + i + ")", requestRecord);
            assertEquals("Got this instead of request audit record (" + i + "): " + requestRecord, AuditEventStage.REQUEST, requestRecord.getEventStage());
            Collection<ObjectDeltaOperation<? extends ObjectType>> requestDeltas = requestRecord.getDeltas();
            assertTrue("Unexpected delta in request audit record " + requestRecord,
                    requestDeltas.isEmpty() || requestDeltas.size() == 1 && requestDeltas.iterator().next().getObjectDelta().isAdd());

            AuditEventRecord executionRecord = auditRecords.get(i + 1);
            assertNotNull("No execution audit record (" + i + ")", executionRecord);
            assertEquals("Got this instead of execution audit record (" + i + "): " + executionRecord, AuditEventStage.EXECUTION, executionRecord.getEventStage());

            assertThat(executionRecord.getDeltas())
                    .withFailMessage("Empty deltas in execution audit record " + executionRecord)
                    .isNotEmpty();
            modifications++;

            // check next records
            while (i < auditRecords.size() - 2) {
                AuditEventRecord nextRecord = auditRecords.get(i + 2);
                if (nextRecord.getEventStage() == AuditEventStage.EXECUTION) {
                    // more than one execution record is OK
                    i++;
                } else {
                    break;
                }
            }

        }
        assertEquals("Unexpected number of audit modifications", expectedModifications, modifications);
    }

    private void assertReconAuditModifications(int expectedModifications, String taskOid) {
        displayDumpable("Audit (all records)", dummyAuditService);

        List<AuditEventRecord> auditRecords = dummyAuditService.getRecords();

        // Skip unrelated records (other tasks)
        auditRecords.removeIf(
                record -> record.getTaskOid() != null && !record.getTaskOid().equals(taskOid));

        // Skip unrelated records (raw changes)
        auditRecords.removeIf(
                record -> record.getEventType() == AuditEventType.EXECUTE_CHANGES_RAW);

        // Skip request-stage records
        auditRecords.removeIf(
                record -> record.getEventStage() == AuditEventStage.REQUEST);

        displayDumpable("Audit (relevant modifications)", dummyAuditService);

        for (AuditEventRecord record : auditRecords) {
            assertEquals("Non-execution audit record sneaked in: " + record,
                    AuditEventStage.EXECUTION, record.getEventStage());
            assertThat(record.getDeltas())
                    .withFailMessage("Empty deltas in execution audit record " + record)
                    .isNotEmpty();
        }
        assertEquals("Unexpected number of audit modifications", expectedModifications, auditRecords.size());
    }

    private void assertNoImportedUserByUsername(String username) throws CommonException {
        PrismObject<UserType> user = findUserByUsername(username);
        assertNull("User " + username + " sneaked in", user);
    }

    private void assertImportedUserByOid(String userOid, String... resourceOids) throws CommonException {
        PrismObject<UserType> user = getUser(userOid);
        assertNotNull("No user " + userOid, user);
        assertImportedUser(user, resourceOids);
    }

    private PrismObject<UserType> assertImportedUserByUsername(String username, String... resourceOids) throws CommonException {
        PrismObject<UserType> user = findUserByUsername(username);
        assertNotNull("No user " + username, user);
        assertImportedUser(user, resourceOids);
        return user;
    }

    private void assertImportedUser(PrismObject<UserType> user, String... resourceOids) throws CommonException {
        display("Imported user", user);
        assertLiveLinks(user, resourceOids.length);
        for (String resourceOid : resourceOids) {
            assertAccount(user, resourceOid);
        }
        assertAdministrativeStatusEnabled(user);
    }
}
