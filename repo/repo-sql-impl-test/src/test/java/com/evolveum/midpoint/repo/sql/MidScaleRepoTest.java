/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.javasimon.EnabledManager;
import org.javasimon.Manager;
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
public class MidScaleRepoTest extends BaseSQLRepoTest {

    public static final int RESOURCE_COUNT = 10;
    public static final int BASE_USER_COUNT = 1000;
    public static final int MORE_USER_COUNT = 2000;
    public static final int PEAK_USER_COUNT = 1000;

    public static final int FIND_COUNT = 1000;

    private static final Random RND = new Random();

    //    private final Manager simonManager = new EnabledManager();
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
        Stopwatch stopwatch = testMonitor.stopwatch("resource-add");
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
        Stopwatch stopwatch = testMonitor.stopwatch("user-add");
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
        Stopwatch stopwatch = testMonitor.stopwatch("shadow-add");
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
        Stopwatch stopwatch = testMonitor.stopwatch("user-get1");
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
        Stopwatch stopwatch = testMonitor.stopwatch("user-add-more");
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
        Stopwatch stopwatch = testMonitor.stopwatch("shadow-add-more");
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
        Stopwatch stopwatch = testMonitor.stopwatch("user-add-peak");
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
        Stopwatch stopwatch = testMonitor.stopwatch("user-get2");
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
        memInfo.forEach(System.out::println);
        testMonitor.dumpReport(getClass().getSimpleName());
//        simonManager.getSimons(null).stream().sorted(Comparator.comparing(s -> s.getName()))
//                .forEach(System.out::println);
    }
}

/**
 * Object can hold various monitors (stopwatches, counters) and dump a report for them.
 * Use for a single report unit, e.g. a test class, then throw away.
 */
class TestMonitor {

    /**
     * Order of creation/addition is the order in the final report.
     * Stopwatches are stored under specific names, but their names are null, always use the key.
     */
    private final Map<String, Stopwatch> stopwatches = new LinkedHashMap<>();

    /** Simon manager used for monitor creations, otherwise ignored. */
    private final Manager simonManager = new EnabledManager();

    /** If you want to register already existing Stopwatch, e.g. from production code. */
    public synchronized void register(Stopwatch stopwatch) {
        stopwatches.put(stopwatch.getName(), stopwatch);
    }

    /**
     * Returns {@link Stopwatch} for specified name, registers a new one if needed.
     */
    public synchronized Stopwatch stopwatch(String name) {
        Stopwatch stopwatch = stopwatches.get(name);
        if (stopwatch == null) {
            // internally the stopwatch is not named to avoid registration with Simon Manager
            stopwatch = simonManager.getStopwatch(null);
            stopwatches.put(name, stopwatch);
        }
        return stopwatch;
    }

    /**
     * Starts measurement (represented by {@link Split}) on a stopwatch with specified name.
     * Stopwatch is created and registered with this object if needed (no Simon manager is used).
     * Returned {@link Split} is {@link AutoCloseable}, so it can be used in try-with-resource
     * and the actual variable can be completely ignored.
     */
    public Split stopwatchStart(String name) {
        return stopwatch(name).start();
    }

    // TODO not sure what "testName" is, it's some kind of "bundle of monitors".
    //  Currently it is tied to the dump call, so it can't be test method name unless it's called
    //  in each method. Scoping/grouping of monitors is to be determined yet.
    public void dumpReport(String testName) {
        dumpReport(testName, System.out);
    }

    public void dumpReport(String testName, PrintStream out) {
        // millis are more practical, but sometimes too big for avg and min and we don't wanna mix ms/us
        out.println("test|name|count|total(us)|avg(us)|min(us)|max(us)");
        for (Map.Entry<String, Stopwatch> stopwatchEntry : stopwatches.entrySet()) {
            String name = stopwatchEntry.getKey();
            Stopwatch stopwatch = stopwatchEntry.getValue();
            out.printf("%s|%s|%d|%d|%d|%d|%d\n", testName, name,
                    stopwatch.getCounter(),
                    TimeUnit.NANOSECONDS.toMicros(stopwatch.getTotal()),
                    TimeUnit.NANOSECONDS.toMicros((long) stopwatch.getMean()),
                    TimeUnit.NANOSECONDS.toMicros(stopwatch.getMin()),
                    TimeUnit.NANOSECONDS.toMicros(stopwatch.getMax()));
        }
    }
}
