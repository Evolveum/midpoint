/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Tests the parallel import/reconciliation of accounts with entitlements.
 *
 * Do not use in regular tests (for now).
 *
 * See MID-5237.
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestParallelSynchronization extends AbstractInitializedModelIntegrationTest {

    // --- START of test configuration ---
    private static final SyncKind KIND = SyncKind.RECONCILIATION;
    private static final Distribution DISTRIBUTION = Distribution.PARTITIONED;
    // --- END of test configuration ---

    private static final File TEST_DIR = new File("src/test/resources/sync");

    private static final File RESOURCE_DUMMY_STEELBLUE_FILE = new File(TEST_DIR, "resource-dummy-steelblue.xml");
    private static final String RESOURCE_DUMMY_STEELBLUE_OID = "8d97261a-ef5e-4199-9700-670577441c7f";
    private static final String RESOURCE_DUMMY_STEELBLUE_NAME = "steelblue";

    private static final File TASK_IMPORT_DUMMY_STEELBLUE_MULTITHREADED_FILE = new File(TEST_DIR, "task-import-dummy-steelblue-multithreaded.xml");
    private static final String TASK_IMPORT_DUMMY_STEELBLUE_MULTITHREADED_OID = "d553eec5-0e03-4efd-80ba-18e6715c26aa";

    private static final File TASK_RECONCILE_DUMMY_STEELBLUE_MULTITHREADED_FILE = new File(TEST_DIR, "task-reconcile-dummy-steelblue-multithreaded.xml");
    private static final String TASK_RECONCILE_DUMMY_STEELBLUE_MULTITHREADED_OID = "c1351099-eabf-4ca3-b157-9a7b6c16b960";

    private static final File TASK_RECONCILE_DUMMY_STEELBLUE_PARTITIONED_FILE = new File(TEST_DIR, "task-reconcile-dummy-steelblue-partitioned.xml");
    private static final String TASK_RECONCILE_DUMMY_STEELBLUE_PARTITIONED_OID = "0e1f67e2-45b3-4fd9-b193-e1a5fea1d315";

    private DummyResource dummyResourceSteelBlue;
    private DummyResourceContoller dummyResourceCtlSteelBlue;
    private ResourceType resourceDummySteelBlueType;
    private PrismObject<ResourceType> resourceDummySteelBlue;

    private static final int NUMBER_OF_GROUPS = 1000;
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final List<String> groupNames = new ArrayList<>();

    private static final int NUMBER_OF_USERS = 100;
    private final List<String> userNames = new ArrayList<>();

    private static final double GROUP_ASSIGNMENT_PROBABILITY = 0.1;

    enum SyncKind { IMPORT, RECONCILIATION }
    enum Distribution { MULTITHREADED, PARTITIONED }

    private String getSyncTaskOid() {
        if (KIND == SyncKind.IMPORT) {
            if (DISTRIBUTION == Distribution.MULTITHREADED) {
                return TASK_IMPORT_DUMMY_STEELBLUE_MULTITHREADED_OID;
            } else {
                throw new AssertionError("unsupported");
            }
        } else {
            if (DISTRIBUTION == Distribution.MULTITHREADED) {
                return TASK_RECONCILE_DUMMY_STEELBLUE_MULTITHREADED_OID;
            } else {
                return TASK_RECONCILE_DUMMY_STEELBLUE_PARTITIONED_OID;
            }
        }
    }

    private File getSyncTaskFile() {
        if (KIND == SyncKind.IMPORT) {
            if (DISTRIBUTION == Distribution.MULTITHREADED) {
                return TASK_IMPORT_DUMMY_STEELBLUE_MULTITHREADED_FILE;
            } else {
                throw new AssertionError("unsupported");
            }
        } else {
            if (DISTRIBUTION == Distribution.MULTITHREADED) {
                return TASK_RECONCILE_DUMMY_STEELBLUE_MULTITHREADED_FILE;
            } else {
                return TASK_RECONCILE_DUMMY_STEELBLUE_PARTITIONED_FILE;
            }
        }
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceCtlSteelBlue = DummyResourceContoller.create(RESOURCE_DUMMY_STEELBLUE_NAME, resourceDummySteelBlue);
        dummyResourceCtlSteelBlue.extendSchemaPirate();
        dummyResourceSteelBlue = dummyResourceCtlSteelBlue.getDummyResource();
        resourceDummySteelBlue = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_STEELBLUE_FILE, RESOURCE_DUMMY_STEELBLUE_OID, initTask, initResult);
        resourceDummySteelBlueType = resourceDummySteelBlue.asObjectable();
        dummyResourceCtlSteelBlue.setResource(resourceDummySteelBlue);

        for (int i = 0; i < NUMBER_OF_USERS; i++) {
            String userName = String.format("user%06d", i);
            dummyResourceCtlSteelBlue.addAccount(userName);
            userNames.add(userName);
        }

        for (int i = 0; i < NUMBER_OF_GROUPS; i++) {
            String groupName = String.format("group%06d", i);
            DummyGroup group = dummyResourceCtlSteelBlue.addGroup(groupName);
            for (String name : userNames) {
                if (Math.random() < GROUP_ASSIGNMENT_PROBABILITY) {
                    group.addMember(name);
                }
            }
            groupNames.add(groupName);
        }

        InternalMonitor.reset();
        InternalMonitor.setTrace(InternalOperationClasses.SHADOW_FETCH_OPERATIONS, true);
    }

    @Override
    protected int getNumberOfUsers() {
        return super.getNumberOfUsers() + NUMBER_OF_USERS;
    }

    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Test
    public void test001SanityAzure() throws Exception {
        displayDumpable("Dummy resource azure", dummyResourceSteelBlue);

        // WHEN
        ResourceSchema resourceSchemaAzure = ResourceSchemaFactory.getRawSchema(resourceDummySteelBlueType);

        displayDumpable("Dummy azure resource schema", resourceSchemaAzure);

        // THEN
        dummyResourceCtlSteelBlue.assertDummyResourceSchemaSanityExtended(resourceSchemaAzure);
    }

    @Test
    public void test002SanityAzureRefined() throws Exception {
        // WHEN
        ResourceSchema refinedSchemaAzure = ResourceSchemaFactory.getCompleteSchema(resourceDummySteelBlueType);

        displayDumpable("Dummy azure refined schema", refinedSchemaAzure);

        // THEN
        dummyResourceCtlSteelBlue.assertRefinedSchemaSanity(refinedSchemaAzure);
    }

    @Test
    public void test100Synchronize() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // Preconditions
        List<PrismObject<UserType>> usersBefore = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users before", usersBefore);
        ObjectQuery onSteelBlueQuery = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_STEELBLUE_OID)
                .build();
        SearchResultList<PrismObject<ShadowType>> shadowsBefore = repositoryService.searchObjects(ShadowType.class, onSteelBlueQuery, null, result);
        display("Shadows before", shadowsBefore);

        loginAdministrator();

        dummyAuditService.clear();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        addObject(getSyncTaskFile(), task, result);

        // THEN
        then();

        if (DISTRIBUTION == Distribution.MULTITHREADED) {
            waitForTaskFinish(getSyncTaskOid(), 600000);
        } else {
            waitForRootActivityCompletion(getSyncTaskOid(), 600000);
        }

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        List<PrismObject<UserType>> usersAfter = modelService.searchObjects(UserType.class, null, null, task, result);
        display("Users after", usersAfter);
        SearchResultList<PrismObject<ShadowType>> shadowsAfter = repositoryService.searchObjects(ShadowType.class, onSteelBlueQuery, null, result);
        display("Shadows after", shadowsAfter);

        List<String> shadowNames = shadowsAfter.stream().map(o -> o.getName().getOrig()).collect(Collectors.toList());
        Set<String> uniqueNames = new HashSet<>();
        List<String> duplicateNames = shadowNames.stream()
                .filter(e -> !uniqueNames.add(e))
                .collect(Collectors.toList());
        System.out.println("Shadow names: " + shadowNames.size());
        System.out.println("Unique shadow names: " + uniqueNames.size());
        System.out.println("Duplicate names: " + duplicateNames);
        assertEquals("Duplicate names: " + duplicateNames, 0, duplicateNames.size());

        PrismObject<ShadowType> account0 = shadowsAfter.stream()
                .filter(o -> o.asObjectable().getName().getOrig().equals(userNames.get(0))).findFirst().orElseThrow(AssertionError::new);
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, account0.getOid(), null, task, result);
        display("user0 shadow", shadow);
    }

}
