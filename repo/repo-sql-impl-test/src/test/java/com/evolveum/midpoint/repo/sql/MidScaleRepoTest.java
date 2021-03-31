/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;

import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.tools.testng.PerformanceTestClassMixin;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * The test is not part of automatically run tests (it is not mentioned in suite XMLs).
 * This tests creates data in the repository and then tries various queries.
 * Data doesn't need to be business-realistic, full object representation can be dummy.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MidScaleRepoTest extends BaseSQLRepoTest
        implements PerformanceTestClassMixin {

    public static final int RESOURCE_COUNT = 10;
    public static final int BASE_USER_COUNT = 1000;
    public static final int MORE_USER_COUNT = 10000;
    public static final int PEAK_USER_COUNT = 1000; // added both with and without assigned OID

    public static final int FIND_COUNT = 1000;

    private static final Random RND = new Random();

    // maps name -> oid
    private final Map<String, String> resources = new LinkedHashMap<>();
    private final Map<String, String> users = new LinkedHashMap<>();

    private final List<String> memInfo = new ArrayList<>();

    @BeforeMethod
    public void reportBeforeTest() {
        Runtime.getRuntime().gc();
        memInfo.add(String.format("%-40.40s before: %,15d",
                contextName(), Runtime.getRuntime().totalMemory()));
        queryListener.clear();
    }

    @AfterMethod
    public void reportAfterTest() {
        memInfo.add(String.format("%-40.40s  after: %,15d",
                contextName(), Runtime.getRuntime().totalMemory()));
    }

    @Test
    public void test010InitResources() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("resource.add", "Repository addObject(resource)");
        for (int resourceIndex = 1; resourceIndex <= RESOURCE_COUNT; resourceIndex++) {
            String name = String.format("resource-%03d", resourceIndex);
            ResourceType resourceType = new ResourceType(prismContext)
                    .name(PolyStringType.fromOrig(name));
            if (resourceIndex == RESOURCE_COUNT) {
                queryListener.start();
            }
            try (Split ignored = stopwatch.start()) {
                repositoryService.addObject(resourceType.asPrismObject(), null, operationResult);
            }
            resources.put(name, resourceType.getOid());
        }
        queryListener.dumpAndStop();
    }

    @Test
    public void test020AddBaseUsers() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user.add", "Repository addObject(user) - 1st batch");
        for (int userIndex = 1; userIndex <= BASE_USER_COUNT; userIndex++) {
            String name = String.format("user-%07d", userIndex);
            UserType userType = new UserType(prismContext)
                    .name(PolyStringType.fromOrig(name));
            if (userIndex == BASE_USER_COUNT) {
                queryListener.start();
            }
            try (Split ignored = stopwatch.start()) {
                repositoryService.addObject(userType.asPrismObject(), null, operationResult);
            }
            users.put(name, userType.getOid());
        }
        queryListener.dumpAndStop();
    }

    @Test
    public void test030AddBaseShadows() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("shadow.add", "Repository addObject(shadow) - 1st batch");
        for (int userIndex = 1; userIndex <= BASE_USER_COUNT; userIndex++) {
            for (Map.Entry<String, String> resourceEntry : resources.entrySet()) {
                String name = String.format("shadow-%07d-at-%s", userIndex, resourceEntry.getKey());
                ShadowType shadowType = createShadow(name, resourceEntry.getValue());
                // for the last user, but only once for a single resource
                if (userIndex == BASE_USER_COUNT && queryListener.hasNoEntries()) {
                    queryListener.start();
                }
                try (Split ignored = stopwatch.start()) {
                    repositoryService.addObject(shadowType.asPrismObject(), null, operationResult);
                }
                if (queryListener.isStarted()) {
                    // stop does not clear entries, so it will not be started again
                    queryListener.stop();
                }
            }
        }
        queryListener.dumpAndStop();
    }

    @NotNull
    private ShadowType createShadow(String shadowName, String resourceOid) {
        return new ShadowType(prismContext)
                .name(PolyStringType.fromOrig(shadowName))
                .resourceRef(MiscSchemaUtil.createObjectReference(
                        resourceOid, ResourceType.COMPLEX_TYPE));
    }

    @Test
    public void test110GetUser() throws SchemaException, ObjectNotFoundException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user.get1", "Repository getObject() -> user, 1st test");
        for (int i = 1; i <= FIND_COUNT; i++) {
            String randomName = String.format("user-%07d", RND.nextInt(BASE_USER_COUNT) + 1);
            if (i == FIND_COUNT) {
                queryListener.start();
            }
            try (Split ignored = stopwatch.start()) {
                assertThat(repositoryService.getObject(
                        UserType.class, users.get(randomName), null, operationResult))
                        .isNotNull();
            }
        }
        queryListener.dumpAndStop();
    }

    @Test
    public void test210AddMoreUsers() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user.addMore", "Repository addObject(user) - 2nd batch");
        for (int userIndex = 1; userIndex <= MORE_USER_COUNT; userIndex++) {
            String name = String.format("user-more-%07d", userIndex);
            UserType userType = new UserType(prismContext)
                    .name(PolyStringType.fromOrig(name));
            if (userIndex == MORE_USER_COUNT) {
                queryListener.start();
            }
            try (Split ignored = stopwatch.start()) {
                repositoryService.addObject(userType.asPrismObject(), null, operationResult);
            }
            users.put(name, userType.getOid());
        }
        queryListener.dumpAndStop();
    }

    @Test
    public void test230AddMoreShadows() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("shadow.addMore", "Repository addObject(shadow) - 2nd batch");
        for (int userIndex = 1; userIndex <= MORE_USER_COUNT; userIndex++) {
            for (Map.Entry<String, String> resourceEntry : resources.entrySet()) {
                String name = String.format("shadow-more-%07d-at-%s", userIndex, resourceEntry.getKey());
                ShadowType shadowType = createShadow(name, resourceEntry.getValue());
                // for the last user, but only once for a single resource
                if (userIndex == MORE_USER_COUNT && queryListener.hasNoEntries()) {
                    queryListener.start();
                }
                try (Split ignored = stopwatch.start()) {
                    repositoryService.addObject(shadowType.asPrismObject(), null, operationResult);
                }
                if (queryListener.isStarted()) {
                    queryListener.stop();
                }
            }
        }
        queryListener.dumpAndStop();
    }

    @Test
    public void test610AddPeakUsers() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user.addPeak", "Repository addObject(user) - 3rd batch");
        for (int userIndex = 1; userIndex <= PEAK_USER_COUNT; userIndex++) {
            String name = String.format("user-peak-%07d", userIndex);
            UserType userType = new UserType(prismContext)
                    .name(PolyStringType.fromOrig(name));
            try (Split ignored = stopwatch.start()) {
                repositoryService.addObject(userType.asPrismObject(), null, operationResult);
            }
            users.put(name, userType.getOid());
        }
        // no query listener in this test
    }

    @Test
    public void test611AddPeakShadows() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("shadow.addPeak", "Repository addObject(shadow) - 3rd batch");
        for (int userIndex = 1; userIndex <= PEAK_USER_COUNT; userIndex++) {
            for (Map.Entry<String, String> resourceEntry : resources.entrySet()) {
                String name = String.format("shadow-peak-%07d-at-%s", userIndex, resourceEntry.getKey());
                ShadowType shadowType = createShadow(name, resourceEntry.getValue());
                try (Split ignored = stopwatch.start()) {
                    repositoryService.addObject(shadowType.asPrismObject(), null, operationResult);
                }
            }
        }
        // no query listener in this test
    }

    @Test
    public void test615AddPeakUsersWithOid() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user.addPeakWithOid", "Repository addObject(user) - 4th batch");
        for (int userIndex = 1; userIndex <= PEAK_USER_COUNT; userIndex++) {
            String name = String.format("user-peak-oid-%07d", userIndex);
            UserType userType = new UserType(prismContext)
                    // (not) assigning OID makes little/no difference for old repo
                    .oid(UUID.randomUUID().toString())
                    .name(PolyStringType.fromOrig(name));
            try (Split ignored = stopwatch.start()) {
                repositoryService.addObject(userType.asPrismObject(), null, operationResult);
            }
            users.put(name, userType.getOid());
        }
        // no query listener in this test
    }

    @Test
    public void test616AddPeakShadowsWithOid() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("shadow.addPeakWithOid", "Repository addObject(shadow) - 4th batch");
        for (int userIndex = 1; userIndex <= PEAK_USER_COUNT; userIndex++) {
            for (Map.Entry<String, String> resourceEntry : resources.entrySet()) {
                String name = String.format("shadow-peak-oid-%07d-at-%s", userIndex, resourceEntry.getKey());
                ShadowType shadowType = createShadow(name, resourceEntry.getValue())
                        .oid(UUID.randomUUID().toString());
                try (Split ignored = stopwatch.start()) {
                    repositoryService.addObject(shadowType.asPrismObject(), null, operationResult);
                }
            }
        }
        // no query listener in this test
    }

    @Test
    public void test710GetUserMore() throws SchemaException, ObjectNotFoundException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user.get2", "Repository getObject() -> user, 2nd test");
        for (int i = 1; i <= FIND_COUNT; i++) {
            String randomName = String.format("user-more-%07d", RND.nextInt(MORE_USER_COUNT) + 1);
            if (i == FIND_COUNT) {
                queryListener.start();
            }
            try (Split ignored = stopwatch.start()) {
                assertThat(repositoryService.getObject(
                        UserType.class, users.get(randomName), null, operationResult))
                        .isNotNull();
            }
        }
        queryListener.dumpAndStop();
    }

    @Test
    public void test900PrintObjects() {
        System.out.println("resources = " + resources.size());
        System.out.println("users = " + users.size());
        // WIP: memInfo is not serious yet
        memInfo.forEach(System.out::println);
    }
}
