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
import com.evolveum.midpoint.tools.testng.PerformanceTestMixin;
import com.evolveum.midpoint.tools.testng.TestMonitor;
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
        implements PerformanceTestMixin {

    public static final int RESOURCE_COUNT = 10;
    public static final int BASE_USER_COUNT = 1000;
    public static final int MORE_USER_COUNT = 2000;
    public static final int PEAK_USER_COUNT = 1000;

    public static final int FIND_COUNT = 1000;

    private static final Random RND = new Random();

    private final TestMonitor testMonitor = new TestMonitor();

    // maps name -> oid
    private final Map<String, String> resources = new LinkedHashMap<>();
    private final Map<String, String> users = new LinkedHashMap<>();

    private final List<String> memInfo = new ArrayList<>();

    @BeforeMethod
    public void reportBeforeTest() {
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
        Stopwatch stopwatch = stopwatch("resource-add");
        for (int resourceIndex = 1; resourceIndex <= RESOURCE_COUNT; resourceIndex++) {
            ResourceType resourceType = new ResourceType(prismContext);
            String name = String.format("resource-%03d", resourceIndex);
            resourceType.setName(PolyStringType.fromOrig(name));
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
        Stopwatch stopwatch = stopwatch("user-add");
        for (int userIndex = 1; userIndex <= BASE_USER_COUNT; userIndex++) {
            UserType userType = new UserType(prismContext);
            String name = String.format("user-%07d", userIndex);
            userType.setName(PolyStringType.fromOrig(name));
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
        Stopwatch stopwatch = stopwatch("shadow-add");
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
        ShadowType shadowType = new ShadowType(prismContext);
        shadowType.setName(PolyStringType.fromOrig(shadowName));
        shadowType.setResourceRef(MiscSchemaUtil.createObjectReference(
                resourceOid, ResourceType.COMPLEX_TYPE));
        return shadowType;
    }

    @Test
    public void test110GetUser() throws SchemaException, ObjectNotFoundException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user-get1");
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
        Stopwatch stopwatch = stopwatch("user-add-more");
        for (int userIndex = 1; userIndex <= MORE_USER_COUNT; userIndex++) {
            UserType userType = new UserType(prismContext);
            String name = String.format("user-more-%07d", userIndex);
            userType.setName(PolyStringType.fromOrig(name));
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
        Stopwatch stopwatch = stopwatch("shadow-add-more");
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
    public void test610PeakMoreUsers() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user-add-peak");
        for (int userIndex = 1; userIndex <= PEAK_USER_COUNT; userIndex++) {
            UserType userType = new UserType(prismContext);
            String name = String.format("user-peak-%07d", userIndex);
            userType.setName(PolyStringType.fromOrig(name));
            if (userIndex == PEAK_USER_COUNT) {
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
    public void test710GetUserMore() throws SchemaException, ObjectNotFoundException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = stopwatch("user-get2");
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

    public TestMonitor getTestMonitor() {
        return testMonitor;
    }
}
