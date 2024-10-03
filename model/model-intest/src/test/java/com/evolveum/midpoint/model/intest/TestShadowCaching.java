/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.statistics.ProvisioningStatistics;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.test.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.intest.dummys.DummyHrScenarioExtended;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;

/**
 * Comprehensive test for shadow caching. Its main goal is to ensure that caching works both ways:
 *
 * 1. What is cached is not retrieved needlessly.
 * 2. What is not cached (or expired) is retrieved when needed.
 *
 * Test methods depend on each other.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestShadowCaching extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/caching");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "c4b26c78-b7ac-42b8-9a7c-9bdb0c1e2bbd");
    private static final TestObject<ArchetypeType> OBJECT_TEMPLATE_PERSON = TestObject.file(
            TEST_DIR, "object-template-person.xml", "62d94990-fb9d-4723-a101-cd91384795d5");

    private static final TestTask TASK_RECOMPUTE_PERSONS = TestTask.file(
            TEST_DIR, "task-recompute-persons.xml", "d791f072-1be3-4da0-a0d5-9da2ae5589c6");
    private static final TestTask TASK_RECONCILE_HR_PERSONS = TestTask.file(
            TEST_DIR, "task-reconcile-hr-persons.xml", "007c5ef2-3d1f-4688-a799-b735bbb9d934");
    private static final TestTask TASK_RECONCILE_TARGET_ACCOUNTS = TestTask.file(
            TEST_DIR, "task-reconcile-target-accounts.xml", "3b5cf882-65ae-4482-9b29-354d35599614");

    private static final String INTENT_PERSON = "person";

    private static final String ORG_SCIENCES_NAME = "sciences";
    private static final String ORG_LAW_NAME = "law";
    private static final String ORG_MEDICINE_NAME = "medicine";
    private static final int INITIAL_ORGS_COUNT = 3;

    private static final String PERSON_JOHN_NAME = "john";
    private static final String PERSON_ALEX_NAME = "alex";
    private static final String PERSON_BOB_NAME = "bob";
    private static final int INITIAL_PERSONS_COUNT = 2; // Alex is not counted here

    private static final String JOHN_SCIENCES_CONTRACT_ID = "10703321";
    private static final String JOHN_SCIENCES_CONTRACT_ASSIGNMENT_ID = "contract:" + JOHN_SCIENCES_CONTRACT_ID;
    private static final String JOHN_LAW_CONTRACT_ID = "10409314";
    private static final String JOHN_LAW_CONTRACT_ASSIGNMENT_ID = "contract:" + JOHN_LAW_CONTRACT_ID;
    private static final String JOHN_MEDICINE_CONTRACT_ID = "10104921";
    private static final String JOHN_MEDICINE_CONTRACT_ASSIGNMENT_ID = "contract:" + JOHN_MEDICINE_CONTRACT_ID;

    /** Cache TTL is a couple of minutes. So one day is much larger than that. */
    private static final String EXPIRY_INTERVAL = "-P1D";

    private static DummyHrScenario hrScenario;
    private static DummyDefaultScenario targetScenario;

    private static final DummyTestResource RESOURCE_DUMMY_HR = new DummyTestResource(
            TEST_DIR, "resource-dummy-hr.xml", "c37ff87e-42f1-46d2-8c6f-36c780cd1193", "hr",
            c -> hrScenario = DummyHrScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_TARGET = new DummyTestResource(
            TEST_DIR, "resource-dummy-target.xml", "e26d279d-6552-45e6-a3ca-a288910c885a", "target",
            c -> targetScenario = DummyDefaultScenario.on(c).initialize());

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                OBJECT_TEMPLATE_PERSON, ARCHETYPE_PERSON,
                TASK_RECOMPUTE_PERSONS, TASK_RECONCILE_HR_PERSONS, TASK_RECONCILE_TARGET_ACCOUNTS);

        RESOURCE_DUMMY_HR.initAndTest(this, initTask, initResult);
        createCommonHrObjects();
    }

    private void createCommonHrObjects() throws Exception {
        DummyObject sciences = hrScenario.orgUnit.add(ORG_SCIENCES_NAME)
                .addAttributeValues(DummyHrScenarioExtended.OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Sciences");
        DummyObject law = hrScenario.orgUnit.add(ORG_LAW_NAME)
                .addAttributeValues(DummyHrScenarioExtended.OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Law");
        hrScenario.orgUnit.add(ORG_MEDICINE_NAME)
                .addAttributeValues(DummyHrScenarioExtended.OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Medicine");

        DummyObject john = hrScenario.person.add(PERSON_JOHN_NAME)
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Doe")
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.TITLE.local(), "Bc.");

        DummyObject johnContractSciences = hrScenario.contract.add(JOHN_SCIENCES_CONTRACT_ID);
        DummyObject johnContractLaw = hrScenario.contract.add(JOHN_LAW_CONTRACT_ID);

        hrScenario.personContract.add(john, johnContractSciences);
        hrScenario.contractOrgUnit.add(johnContractSciences, sciences);

        hrScenario.personContract.add(john, johnContractLaw);
        hrScenario.contractOrgUnit.add(johnContractLaw, law);

        // Alex will be deleted soon after the import
        hrScenario.person.add(PERSON_ALEX_NAME)
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.FIRST_NAME.local(), "Alex")
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Foo");

        // Bob will be deleted, but later on
        hrScenario.person.add(PERSON_BOB_NAME)
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.FIRST_NAME.local(), "Bob")
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Red");
    }

    /**
     * Searches through all HR objects, filling-in the shadow caches.
     */
    @Test
    public void test100InitializeCaches() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("searching for org units in HR");
        var orgUnits = refreshHrOrgUnits(task, result);
        assertThat(orgUnits).as("org units").hasSize(INITIAL_ORGS_COUNT);

        when("searching for persons in HR (with Alex; simulating an earlier time)");
        clock.overrideDuration(EXPIRY_INTERVAL);

        var persons = refreshHrPersons(task, result);
        assertThat(persons).as("persons").hasSize(INITIAL_PERSONS_COUNT + 1);

        and("deleting Alex");
        hrScenario.person.deleteById(
                hrScenario.person.getByNameRequired(PERSON_ALEX_NAME).getId());

        when("searching for persons in HR (without Alex, in current time)");
        clock.resetOverride();
        var personsAfter = refreshHrPersons(task, result);
        assertThat(personsAfter).as("persons").hasSize(INITIAL_PERSONS_COUNT);

        then("everything is cached in the repository");
        dumpCachedShadows(result);
    }

    private void dumpCachedShadows(OperationResult result) throws SchemaException {
        var repoShadows = repositoryService.searchObjects(ShadowType.class, null, null, result);
        display("repo shadows", repoShadows);
    }

    /** Checks that caching works for the initial import. */
    @Test
    public void test110DoingInitialImport() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        rememberShadowOperations();

        var usersBefore = getObjectCount(UserType.class);

        when("persons are imported");
        var taskOid = importHrPersons(result);

        then("orgs are there (they were created on demand)");
        var orgSciencesOid = assertOrgByName(ORG_SCIENCES_NAME, "after").getOid();
        var orgLawOid = assertOrgByName(ORG_LAW_NAME, "after").getOid();

        and("john is found");
        // @formatter:off
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertGivenName("John")
                .assertFamilyName("Doe")
                .assertHonorificPrefix("Bc.")
                .assignments()
                    .assertAssignments(3)
                    .by().targetType(ArchetypeType.COMPLEX_TYPE).find()
                        .assertTargetOid(ARCHETYPE_PERSON.oid)
                    .end()
                    .by().identifier(JOHN_SCIENCES_CONTRACT_ASSIGNMENT_ID).find()
                        .assertTargetRef(orgSciencesOid, OrgType.COMPLEX_TYPE)
                    .end()
                    .by().identifier(JOHN_LAW_CONTRACT_ASSIGNMENT_ID).find()
                        .assertTargetRef(orgLawOid, OrgType.COMPLEX_TYPE)
                    .end()
                .end();
        // @formatter:on

        assertUserAfterByUsername(PERSON_BOB_NAME)
                .assertGivenName("Bob")
                .assertFamilyName("Red");

        and("alex is not there (as well as no other persons)");
        assertNoUserByUsername(PERSON_ALEX_NAME);

        assertThat(getObjectCount(UserType.class) - usersBefore)
                .as("users created")
                .isEqualTo(INITIAL_PERSONS_COUNT);

        and("there were no shadow fetch operations");
        var provisioningStatistics = dumpProvisioningStatistics(taskOid);
        assertThat(provisioningStatistics.getEntry()).as("provisioning statistics entry").isEmpty();

        assertNoShadowFetchOperations();
    }

    /** We modify the user, and re-run the import. */
    @Test
    public void test120ImportingChangedData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("john's data are changed and the cache is refreshed");

        hrScenario.person.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "DOE")
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.TITLE.local(), "Ing.");

        refreshHrPersons(task, result);

        rememberShadowOperations();

        when("persons are re-imported");
        importHrPersons(result);

        then("data are changed in midPoint");

        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertGivenName("John")
                .assertFamilyName("DOE")
                .assertHonorificPrefix("Ing.")
                .assertAssignments(3);

        and("there were no shadow fetch operations");
        assertNoShadowFetchOperations();
    }

    /**
     * Bob is modified, retrieved, and deleted right after that. The modified data will get into the cache.
     * We make sure that the import will *not* process Bob.
     */
    @Test
    public void test130ModifyAndDeleteBobAndImport() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("bob is modified");
        hrScenario.person.getByNameRequired(PERSON_BOB_NAME)
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "RED");

        refreshHrPersons(task, result);

        rememberShadowOperations();

        and("bob is deleted and his account made expired");

        hrScenario.person.deleteById(
                hrScenario.person.getByNameRequired(PERSON_BOB_NAME).getId());

        var bobShadow = findShadowByPrismName(PERSON_BOB_NAME, RESOURCE_DUMMY_HR.get(), result);
        expireShadow(bobShadow, result);

        and("HR persons are imported");
        importHrPersons(result);

        then("bob is unchanged");
        assertUserAfterByUsername(PERSON_BOB_NAME)
                .assertFamilyName("Red");

        and("there were no shadow fetch operations");
        assertNoShadowFetchOperations();
    }

    /** We modify john, and now run the recomputation task. */
    @Test
    public void test140RecomputingWithChangedData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("john's data are changed and the cache is refreshed");

        hrScenario.person.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Big Doe")
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.TITLE.local(), "Ing. Mgr.");
        hrScenario.contract.deleteById(
                hrScenario.contract.getByNameRequired(JOHN_LAW_CONTRACT_ID).getId());

        refreshHrPersons(task, result);

        rememberShadowOperations();

        when("users are recomputed");

        TASK_RECOMPUTE_PERSONS.rerun(result);

        then("john's data are changed in midPoint");
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertGivenName("John")
                .assertFamilyName("Big Doe")
                .assertHonorificPrefix("Ing. Mgr.")
                .assertAssignments(2);

        and("but bob's data are not changed");
        assertUserAfterByUsername(PERSON_BOB_NAME)
                .assertFamilyName("Red");

        and("there were no shadow fetch operations");
        assertNoShadowFetchOperations();
    }

    /** We modify the user, and now run the reconciliation task. */
    @Test
    public void test150ReconcilingWithChangedData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("john's data are changed and the cache is refreshed");

        var newName = "Doe-150";

        hrScenario.person.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), newName);

        refreshHrPersons(task, result);

        rememberShadowOperations();

        when("users are reconciled");

        TASK_RECONCILE_HR_PERSONS.rerun(result);

        then("john's data are changed in midPoint");

        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertGivenName("John")
                .assertFamilyName(newName)
                .assertHonorificPrefix("Ing. Mgr.")
                .assertAssignments(2);

        and("bob is gone");
        assertNoUserByUsername(PERSON_BOB_NAME);

        and("there were two shadow fetch operations (checking for non-existent accounts)");
        assertShadowFetchOperations(2);

        dumpProvisioningStatistics(TASK_RECONCILE_HR_PERSONS.oid);
    }

    @Test
    public void test200AddTargetResourceAndCreateAccounts() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        RESOURCE_DUMMY_TARGET.initAndTest(this, task, result);

        TASK_RECOMPUTE_PERSONS.rerun(result);

        // We don't care much here, just dumping this for information
        dumpProvisioningStatistics(TASK_RECOMPUTE_PERSONS.oid);

        displayDumpable("target", RESOURCE_DUMMY_TARGET.getDummyResource());

        RESOURCE_DUMMY_TARGET.controller.assertAccountByUsername(PERSON_JOHN_NAME)
                .display()
                .assertFullName("John Doe-150");
    }

    /** We modify john on HR and on target, and now run the recomputation task. */
    @Test
    public void test210RecomputingWithChangedData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("john's data are changed and the cache is refreshed");

        hrScenario.person.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Doe-210");
        targetScenario.account.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyDefaultScenario.Account.AttributeNames.DESCRIPTION.local(), "Desc-210");

        refreshHrPersons(task, result);
        refreshTargetAccounts(task, result);

        rememberShadowOperations();

        when("users are recomputed");

        TASK_RECOMPUTE_PERSONS.rerun(result);

        then("john's data are changed in midPoint");
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertFamilyName("Doe-210") // from HR
                .assertDescription("Desc-210"); // from target

        and("john's data are changed on target");
        RESOURCE_DUMMY_TARGET.controller.assertAccountByUsername(PERSON_JOHN_NAME)
                .assertFullName("John Doe-210");

        and("there were no shadow fetch operations");
        assertNoShadowFetchOperations();
    }

    /** We modify john on HR and on target, and now run the HR reconciliation task. */
    @Test
    public void test220ReconcilingWithHrWithChangedData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("john's data are changed and the cache is refreshed");

        hrScenario.person.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Doe-220");
        targetScenario.account.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyDefaultScenario.Account.AttributeNames.DESCRIPTION.local(), "Desc-220");

        refreshHrPersons(task, result);
        refreshTargetAccounts(task, result);

        rememberShadowOperations();

        when("HR is reconciled");

        TASK_RECONCILE_HR_PERSONS.rerun(result);

        then("john's data are changed in midPoint");
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertFamilyName("Doe-220") // from HR
                .assertDescription("Desc-220"); // from target

        and("john's data are changed on target");
        RESOURCE_DUMMY_TARGET.controller.assertAccountByUsername(PERSON_JOHN_NAME)
                .assertFullName("John Doe-220");

        and("there were no shadow fetch operations");
        assertNoShadowFetchOperations();
    }

    /** We modify john on HR and on target, and now run the target reconciliation task. */
    @Test
    public void test230ReconcilingWithTargetWithChangedData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("john's data are changed and the cache is refreshed");

        hrScenario.person.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Doe-230");
        targetScenario.account.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyDefaultScenario.Account.AttributeNames.DESCRIPTION.local(), "Desc-230");

//        executeChanges(
//                deltaFor(ResourceType.class)
//                        .item(ResourceType.F_CACHING, ShadowCachingPolicyType.F_DEFAULT_CACHE_USE)
//                        .replace(CachedShadowsUseType.USE_FRESH)
//                        .asObjectDelta(RESOURCE_DUMMY_HR.oid),
//                null, task, result);

        refreshHrPersons(task, result);
        refreshTargetAccounts(task, result);

        rememberShadowOperations();

        when("target is reconciled");

        TASK_RECONCILE_TARGET_ACCOUNTS.rerun(result);

        // Note that unlike HR reconciliation (which executes inbounds from the target), this one ignores HR data.
        // The reason is that in the former case, there is a priori delta for target, that causes the inbounds to be executed.
        // (From cached data, but anyway.) In this case, there is no such a priori delta for HR, so that inbounds for HR are
        // not executed.

        then("john's data from the target are changed in midPoint");
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertFamilyName("Doe-220") // from HR - unchanged
                .assertDescription("Desc-230"); // from target

        and("john's data are NOT changed on target");
        RESOURCE_DUMMY_TARGET.controller.assertAccountByUsername(PERSON_JOHN_NAME)
                .assertFullName("John Doe-220"); // indirectly from HR - unchanged

        and("there were no shadow fetch operations");
        assertNoShadowFetchOperations();
    }

    private @NotNull SearchResultList<? extends AbstractShadow> refreshHrPersons(Task task, OperationResult result)
            throws CommonException {
        return provisioningService.searchShadows(
                Resource.of(RESOURCE_DUMMY_HR.get())
                        .queryFor(DummyHrScenario.Person.OBJECT_CLASS_NAME.xsd())
                        .build(),
                null, task, result);
    }

    private @NotNull SearchResultList<? extends AbstractShadow> refreshHrOrgUnits(Task task, OperationResult result)
            throws CommonException {
        return provisioningService.searchShadows(
                Resource.of(RESOURCE_DUMMY_HR.get())
                        .queryFor(DummyHrScenario.OrgUnit.OBJECT_CLASS_NAME.xsd())
                        .build(),
                null, task, result);
    }

    private @NotNull SearchResultList<? extends AbstractShadow> refreshTargetAccounts(Task task, OperationResult result)
            throws CommonException {
        return provisioningService.searchShadows(
                Resource.of(RESOURCE_DUMMY_TARGET.get())
                        .queryFor(DummyDefaultScenario.Account.OBJECT_CLASS_NAME.xsd())
                        .build(),
                null, task, result);
    }

    private String importHrPersons(OperationResult result) throws CommonException, IOException {
        return importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_PERSON))
                .withProcessingAllAccounts()
                .withNoFetchWhenSynchronizing()
                .execute(result);
    }

    private void expireShadow(PrismObject<ShadowType> bobShadow, OperationResult result) throws CommonException {
        repositoryService.modifyObject(
                ShadowType.class,
                bobShadow.getOid(),
                deltaFor(ShadowType.class)
                        .item(ShadowType.F_CACHING_METADATA)
                        .replace(
                                bobShadow.asObjectable().getCachingMetadata().clone()
                                        .retrievalTimestamp(XmlTypeConverter.fromNow(EXPIRY_INTERVAL)))
                        .asItemDeltas(),
                result);
    }

    private ProvisioningStatisticsType dumpProvisioningStatistics(String taskOid) throws CommonException {
        var provisioningStatistics = assertTask(taskOid, "")
                .getObjectable()
                .getOperationStats()
                .getEnvironmentalPerformanceInformation()
                .getProvisioningStatistics();
        displayValue("Provisioning statistics", ProvisioningStatistics.format(provisioningStatistics));
        return provisioningStatistics;
    }
}
