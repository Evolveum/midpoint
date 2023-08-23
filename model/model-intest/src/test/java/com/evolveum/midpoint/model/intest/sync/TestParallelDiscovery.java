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
 * Tests the parallel discovery of accounts with entitlements.
 *
 * Do not use in regular tests (for now).
 *
 * See MID-5237.
 *
 * ------------------------------------------------------------------------------
 *
 * Setup:
 *
 *  - synchronizing accounts from SteelBlue to SteelGrey (using user template that assigns a role for SteelGrey)
 *  - starting with N user accounts on SteelBlue and SteelGrey; M groups on SteelGrey
 *  - during SteelGrey account creation, account+groups are discovered there
 *  - because that occurs in multiple threads, we expect that duplicate group shadows are created
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestParallelDiscovery extends AbstractInitializedModelIntegrationTest {

    // --- START of test configuration ---
    private static final SyncKind KIND = SyncKind.IMPORT;
    private static final Distribution DISTRIBUTION = Distribution.MULTITHREADED;
    // --- END of test configuration ---

    private static final File TEST_DIR = new File("src/test/resources/sync");

    private static final File ROLE_STEELGREY_FILE = new File(TEST_DIR, "role-steelgrey.xml");
    private static final File USER_TEMPLATE_STEELGREY_FILE = new File(TEST_DIR, "user-template-steelgrey.xml");
    private static final String USER_TEMPLATE_STEELGREY_OID = "a7745154-c9c4-43d3-89c1-8de89e615baf";

    private static final File RESOURCE_DUMMY_STEELBLUE_FILE = new File(TEST_DIR, "resource-dummy-steelblue.xml");
    private static final String RESOURCE_DUMMY_STEELBLUE_OID = "8d97261a-ef5e-4199-9700-670577441c7f";
    private static final String RESOURCE_DUMMY_STEELBLUE_NAME = "steelblue";

    private static final File RESOURCE_DUMMY_STEELGREY_FILE = new File(TEST_DIR, "resource-dummy-steelgrey.xml");
    private static final String RESOURCE_DUMMY_STEELGREY_OID = "ff5899c7-8b02-4a0e-8f7d-74c154721d5d";
    private static final String RESOURCE_DUMMY_STEELGREY_NAME = "steelgrey";

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

    private DummyResource dummyResourceSteelGrey;
    private DummyResourceContoller dummyResourceCtlSteelGrey;
    private ResourceType resourceDummySteelGreyType;
    private PrismObject<ResourceType> resourceDummySteelGrey;

    private static final int NUMBER_OF_GROUPS = 10;

    private static final int NUMBER_OF_USERS = 20;
    private final List<String> userNames = new ArrayList<>();

    private static final double GROUP_ASSIGNMENT_PROBABILITY = 0.3;

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

        repoAddObjectFromFile(ROLE_STEELGREY_FILE, initResult);
        repoAddObjectFromFile(USER_TEMPLATE_STEELGREY_FILE, initResult);
        setDefaultUserTemplate(USER_TEMPLATE_STEELGREY_OID);

        dummyResourceCtlSteelBlue = DummyResourceContoller.create(RESOURCE_DUMMY_STEELBLUE_NAME, resourceDummySteelBlue);
        dummyResourceCtlSteelBlue.extendSchemaPirate();
        dummyResourceSteelBlue = dummyResourceCtlSteelBlue.getDummyResource();
        resourceDummySteelBlue = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_STEELBLUE_FILE, RESOURCE_DUMMY_STEELBLUE_OID, initTask, initResult);
        resourceDummySteelBlueType = resourceDummySteelBlue.asObjectable();
        dummyResourceCtlSteelBlue.setResource(resourceDummySteelBlue);

        dummyResourceCtlSteelGrey = DummyResourceContoller.create(RESOURCE_DUMMY_STEELGREY_NAME, resourceDummySteelGrey);
        dummyResourceCtlSteelGrey.extendSchemaPirate();
        dummyResourceSteelGrey = dummyResourceCtlSteelGrey.getDummyResource();
        resourceDummySteelGrey = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_STEELGREY_FILE, RESOURCE_DUMMY_STEELGREY_OID, initTask, initResult);
        resourceDummySteelGreyType = resourceDummySteelGrey.asObjectable();
        dummyResourceCtlSteelGrey.setResource(resourceDummySteelGrey);

        for (int i = 0; i < NUMBER_OF_USERS; i++) {
            String userName = String.format("user%06d", i);
            dummyResourceCtlSteelBlue.addAccount(userName);
            dummyResourceCtlSteelGrey.addAccount(userName);
            userNames.add(userName);
        }

        for (int i = 0; i < NUMBER_OF_GROUPS; i++) {
            String groupName = String.format("group%06d", i);
            DummyGroup group = dummyResourceCtlSteelGrey.addGroup(groupName);
            for (String name : userNames) {
                if (Math.random() < GROUP_ASSIGNMENT_PROBABILITY) {
                    group.addMember(name);
                }
            }
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
    public void test001Sanity() throws Exception {
        displayDumpable("Dummy resource azure", dummyResourceSteelBlue);

        // WHEN
        ResourceSchema resourceSchemaSteelBlue = ResourceSchemaFactory.getRawSchema(resourceDummySteelBlueType);
        ResourceSchema resourceSchemaSteelGrey = ResourceSchemaFactory.getRawSchema(resourceDummySteelGreyType);

        displayDumpable("Dummy steel blue resource schema", resourceSchemaSteelBlue);
        displayDumpable("Dummy steel grey resource schema", resourceSchemaSteelGrey);

        // THEN
        dummyResourceCtlSteelBlue.assertDummyResourceSchemaSanityExtended(resourceSchemaSteelBlue);
        dummyResourceCtlSteelGrey.assertDummyResourceSchemaSanityExtended(resourceSchemaSteelGrey);
    }

    @Test
    public void test002SanityRefined() throws Exception {
        // WHEN
        ResourceSchema refinedSchemaSteelBlue = ResourceSchemaFactory.getCompleteSchema(resourceDummySteelBlueType);
        ResourceSchema refinedSchemaSteelGrey = ResourceSchemaFactory.getCompleteSchema(resourceDummySteelGreyType);

        displayDumpable("Dummy steel blue refined schema", refinedSchemaSteelBlue);
        displayDumpable("Dummy steel grey refined schema", refinedSchemaSteelGrey);

        // THEN
        dummyResourceCtlSteelBlue.assertRefinedSchemaSanity(refinedSchemaSteelBlue);
        dummyResourceCtlSteelGrey.assertRefinedSchemaSanity(refinedSchemaSteelGrey);
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
        ObjectQuery onSteelGreyQuery = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_STEELGREY_OID)
                .build();
        SearchResultList<PrismObject<ShadowType>> shadowsBlueBefore = repositoryService.searchObjects(ShadowType.class, onSteelBlueQuery, null, result);
        display("Shadows on blue before", shadowsBlueBefore);
        SearchResultList<PrismObject<ShadowType>> shadowsGreyBefore = repositoryService.searchObjects(ShadowType.class, onSteelGreyQuery, null, result);
        display("Shadows on grey before", shadowsGreyBefore);

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
        SearchResultList<PrismObject<ShadowType>> shadowsBlueAfter = repositoryService.searchObjects(ShadowType.class, onSteelBlueQuery, null, result);
        display("Shadows on blue after", shadowsBlueAfter);
        SearchResultList<PrismObject<ShadowType>> shadowsGreyAfter = repositoryService.searchObjects(ShadowType.class, onSteelGreyQuery, null, result);
        display("Shadows on grey after", shadowsGreyAfter);

        List<String> shadowNames = shadowsGreyAfter.stream().map(o -> o.getName().getOrig()).collect(Collectors.toList());
        Set<String> uniqueNames = new HashSet<>();
        List<String> duplicateNames = shadowNames.stream()
                .filter(e -> !uniqueNames.add(e))
                .collect(Collectors.toList());
        System.out.println("Shadow (grey) names: " + shadowNames.size());
        System.out.println("Unique (grey) shadow names: " + uniqueNames.size());
        System.out.println("Duplicate (grey) names: " + duplicateNames);
        assertEquals("Duplicate (grey) names: " + duplicateNames, 0, duplicateNames.size());

        PrismObject<ShadowType> account0 = shadowsGreyAfter.stream()
                .filter(o -> o.asObjectable().getName().getOrig().equals(userNames.get(0))).findFirst().orElseThrow(AssertionError::new);
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, account0.getOid(), null, task, result);
        display("user0 shadow", shadow);
    }

}
