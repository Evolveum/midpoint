/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;

import org.javasimon.EnabledManager;
import org.javasimon.Manager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * The test is not part of automatically run tests (it is not mentioned in suite XMLs).
 * This tests creates data in the repository and then tries various queries.
 * Data doesn't need to be business-realistic, full object representation can be dummy.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MidScaleRepoTest extends BaseSQLRepoTest {

    public static final int RESOURCE_COUNT = 10;
    public static final int BASE_USER_COUNT = 10000;
    public static final int MORE_USER_COUNT = 20000;
    public static final int PEAK_USER_COUNT = 1000;

    public static final int FIND_COUNT = 1000;

    private static final Random RND = new Random();

    private final Manager simonManager = new EnabledManager();

    // maps name -> oid
    private final Map<String, String> resources = new LinkedHashMap<>();
    private final Map<String, String> users = new LinkedHashMap<>();

    private final List<String> memInfo = new ArrayList<>();

    @BeforeMethod
    public void memInfoBefore() {
        memInfo.add(String.format("%-40.40s before: %,15d",
                contextName(), Runtime.getRuntime().totalMemory()));
    }

    @AfterMethod
    public void memInfoAfter() {
        memInfo.add(String.format("%-40.40s  after: %,15d",
                contextName(), Runtime.getRuntime().totalMemory()));
    }

    @Test
    public void test010InitResources() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = simonManager.getStopwatch("resource-add");
        for (int i = 1; i <= RESOURCE_COUNT; i++) {
            ResourceType resourceType = new ResourceType(prismContext);
            String name = String.format("resource-%03d", i);
            resourceType.setName(PolyStringType.fromOrig(name));
            try (Split ignored = stopwatch.start()) {
                repositoryService.addObject(resourceType.asPrismObject(), null, operationResult);
            }
            resources.put(name, resourceType.getOid());
        }
    }

    @Test
    public void test020AddBaseUsers() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = simonManager.getStopwatch("user-add");
        for (int i = 1; i <= BASE_USER_COUNT; i++) {
            UserType userType = new UserType(prismContext);
            String name = String.format("user-%07d", i);
            userType.setName(PolyStringType.fromOrig(name));
            try (Split ignored = stopwatch.start()) {
                repositoryService.addObject(userType.asPrismObject(), null, operationResult);
            }
            users.put(name, userType.getOid());
        }
    }

    @Test
    public void test110FindUsers() throws SchemaException, ObjectNotFoundException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = simonManager.getStopwatch("user-find1");
        for (int i = 1; i <= FIND_COUNT; i++) {
            String randomName = String.format("user-%07d", RND.nextInt(BASE_USER_COUNT) + 1);
            try (Split ignored = stopwatch.start()) {
                assertThat(repositoryService.getObject(
                        UserType.class, users.get(randomName), null, operationResult))
                        .isNotNull();
            }
        }
    }

    @Test
    public void test210AddMoreUsers() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = simonManager.getStopwatch("user-add-more");
        for (int i = 1; i <= MORE_USER_COUNT; i++) {
            UserType userType = new UserType(prismContext);
            try (Split ignored = stopwatch.start()) {
                String name = String.format("user-more-%07d", i);
                userType.setName(PolyStringType.fromOrig(name));
                repositoryService.addObject(userType.asPrismObject(), null, operationResult);
                users.put(name, userType.getOid());
            }
        }
    }

    @Test
    public void test610PeakMoreUsers() throws ObjectAlreadyExistsException, SchemaException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = simonManager.getStopwatch("user-add-peak");
        for (int i = 1; i <= PEAK_USER_COUNT; i++) {
            UserType userType = new UserType(prismContext);
            try (Split ignored = stopwatch.start()) {
                String name = String.format("user-peak-%07d", i);
                userType.setName(PolyStringType.fromOrig(name));
                repositoryService.addObject(userType.asPrismObject(), null, operationResult);
                users.put(name, userType.getOid());
            }
        }
    }

    @Test
    public void test710FindUsers() throws SchemaException, ObjectNotFoundException {
        OperationResult operationResult = createOperationResult();
        Stopwatch stopwatch = simonManager.getStopwatch("user-find2");
        for (int i = 1; i <= FIND_COUNT; i++) {
            String randomName = String.format("user-more-%07d", RND.nextInt(MORE_USER_COUNT) + 1);
            try (Split ignored = stopwatch.start()) {
                assertThat(repositoryService.getObject(
                        UserType.class, users.get(randomName), null, operationResult))
                        .isNotNull();
            }
        }
    }

    @Test
    public void test900PrintObjects() {
        System.out.println("resources = " + resources.size());
        System.out.println("users = " + users.size());
        memInfo.forEach(System.out::println);
        simonManager.getSimons(null).stream().sorted(Comparator.comparing(s -> s.getName()))
                .forEach(System.out::println);
    }
}
