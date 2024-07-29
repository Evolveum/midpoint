/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.statistics.ProvisioningStatistics;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.test.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.intest.associations.DummyHrScenarioExtended;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

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

    private static final TestTask TASK_RECOMPUTE_PERSONS = TestTask.file(
            TEST_DIR, "task-recompute-persons.xml", "d791f072-1be3-4da0-a0d5-9da2ae5589c6");

    private static final String INTENT_PERSON = "person";

    private static final String ORG_SCIENCES_NAME = "sciences";
    private static final String ORG_LAW_NAME = "law";
    private static final String ORG_MEDICINE_NAME = "medicine";

    private static final String PERSON_JOHN_NAME = "john";

    private static final String JOHN_SCIENCES_CONTRACT_ID = "10703321";
    private static final String JOHN_SCIENCES_CONTRACT_ASSIGNMENT_ID = "contract:" + JOHN_SCIENCES_CONTRACT_ID;
    private static final String JOHN_LAW_CONTRACT_ID = "10409314";
    private static final String JOHN_LAW_CONTRACT_ASSIGNMENT_ID = "contract:" + JOHN_LAW_CONTRACT_ID;
    private static final String JOHN_MEDICINE_CONTRACT_ID = "10104921";
    private static final String JOHN_MEDICINE_CONTRACT_ASSIGNMENT_ID = "contract:" + JOHN_MEDICINE_CONTRACT_ID;

    private static DummyHrScenario hrScenario;
    private static DummyDefaultScenario targetScenario;

    private static final DummyTestResource RESOURCE_DUMMY_HR = new DummyTestResource(
            TEST_DIR, "resource-dummy-hr.xml", "c37ff87e-42f1-46d2-8c6f-36c780cd1193", "hr",
            c -> hrScenario = DummyHrScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_AD = new DummyTestResource(
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
                ARCHETYPE_PERSON, TASK_RECOMPUTE_PERSONS);

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
    }

    /**
     * Searches through all HR objects, filling-in the shadow caches.
     */
    @Test
    public void test100InitializeCaches() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("searching for all HR objects");
        var orgUnits = refreshHrOrgUnits(task, result);
        assertThat(orgUnits).as("org units").hasSize(3);

        var persons = refreshHrPersons(task, result);
        assertThat(persons).as("persons").hasSize(1);

        then("they are cached in the repository");
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

        when("persons are imported");
        var taskOid = importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_PERSON))
                .withProcessingAllAccounts()
                .withNoFetchWhenSynchronizing()
                .execute(result);

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

        and("there were no shadow fetch operations");
        var taskProvisioningStats = assertTask(taskOid, "import task after")
                .display()
                .getObjectable()
                .getOperationStats()
                .getEnvironmentalPerformanceInformation()
                .getProvisioningStatistics();

        displayValue("Provisioning statistics", ProvisioningStatistics.format(taskProvisioningStats));
        assertThat(taskProvisioningStats.getEntry()).as("provisioning statistics entry").isEmpty();

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
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_PERSON))
                .withProcessingAllAccounts()
                .withNoFetchWhenSynchronizing()
                .withTracing()
                .execute(result);

        then("data are changed in midPoint");

        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertGivenName("John")
                .assertFamilyName("DOE")
                .assertHonorificPrefix("Ing.")
                .assertAssignments(3);

        and("there were no shadow fetch operations");
        assertNoShadowFetchOperations();
    }

    /** We modify the user, and now run the recomputation task. */
    @Test
    public void test130RecomputingWithChangedData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("john's data are changed and the cache is refreshed");

        hrScenario.person.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Big Doe")
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.TITLE.local(), "Ing. Mgr.");

        refreshHrPersons(task, result);

        rememberShadowOperations();

        when("users are recomputed");

        TASK_RECOMPUTE_PERSONS.rerun(result);

        then("data are changed in midPoint");

        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertGivenName("John")
                .assertFamilyName("Big Doe")
                .assertHonorificPrefix("Ing. Mgr.")
                .assertAssignments(3);

        and("there were no shadow fetch operations");
        assertNoShadowFetchOperations();
    }

    /** We modify the user, and now run the reconciliation task. */
    @Test(enabled = false) // not ready yet
    public void test140ReconcilingWithChangedData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("john's data are changed and the cache is refreshed");

        hrScenario.person.getByNameRequired(PERSON_JOHN_NAME)
                .replaceAttributeValues(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Doe-140");

        refreshHrPersons(task, result);

        rememberShadowOperations();

        when("users are reconciled");

        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_PERSON))
                .withProcessingAllAccounts()
                .withNoFetchWhenSynchronizing()
                .execute(result);

        then("data are changed in midPoint");

        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assertGivenName("John")
                .assertFamilyName("Doe-140")
                .assertHonorificPrefix("Ing. Mgr.")
                .assertAssignments(3);

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
}
