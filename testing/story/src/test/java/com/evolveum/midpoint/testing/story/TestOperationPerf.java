/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.util.exception.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.test.ProfilingModelInspectorManager;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ExtensionValueGenerator;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

/**
 * Originally, this class checks number of selected infra operations needed to execute "object add" operation.
 *
 * It is now extended to cover other performance aspects, e.g. MID-8479.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOperationPerf extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "operation-perf");

    public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final File USER_ALICE_FILE = new File(TEST_DIR, "user-alice.xml");
    private static final String USER_ALICE_OID = "a077357a-1c5f-11e8-ad16-af1b03cecee9";

    private static final File USER_BOB_FILE = new File(TEST_DIR, "user-bob.xml");
    private static final String USER_BOB_OID = "ab43445c-1c83-11e8-a669-331e1f2cbbac";

    private static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
    protected static final String OBJECT_TEMPLATE_USER_OID = "995aa1a6-1c5e-11e8-8d2f-6784dbc320a9";

    private static final int NUMBER_OF_ORDINARY_ROLES = 1; // including superuser role
    private static final int NUMBER_OF_GENERATED_EMPTY_ROLES = 200;
    private static final String GENERATED_EMPTY_ROLE_OID_FORMAT = "00000000-0000-ffff-2000-e0000000%04d";
    private static final String GENERATED_EMPTY_ROLE_NAME_FORMAT = "Empty Role %04d";

    private static final int GET_ITERATIONS = 2_000_000;

    private final ExtensionValueGenerator extensionValueGenerator = ExtensionValueGenerator.withDefaults();

    @Autowired ClockworkMedic clockworkMedic;
    @Autowired RepositoryCache repositoryCache;
    @Autowired protected MappingFactory mappingFactory;

    private CountingInspector internalInspector;
    private ProfilingModelInspectorManager profilingModelInspectorManager;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        generateRoles(NUMBER_OF_GENERATED_EMPTY_ROLES, GENERATED_EMPTY_ROLE_NAME_FORMAT, GENERATED_EMPTY_ROLE_OID_FORMAT, null, initResult);

        repoAddObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, OBJECT_TEMPLATE_USER_OID, initResult);

        internalInspector = new CountingInspector();
        InternalMonitor.setInspector(internalInspector);

        mappingFactory.setProfiling(true);
        profilingModelInspectorManager = new ProfilingModelInspectorManager();
        clockworkMedic.setDiagnosticContextManager(profilingModelInspectorManager);

        InternalMonitor.setCloneTimingEnabled(true);
        InternalsConfig.reset(); // We want to measure performance, after all.
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false;
    }

    @Override
    protected void importSystemTasks(OperationResult initResult) throws FileNotFoundException {
        // we don't want any
    }

    @Test
    public void test000Sanity() throws Exception {
        assertObjects(RoleType.class, NUMBER_OF_GENERATED_EMPTY_ROLES + NUMBER_OF_ORDINARY_ROLES);

        displayValue("Repo reads", InternalMonitor.getCount(InternalCounters.REPOSITORY_READ_COUNT));
        displayValue("Object compares", InternalMonitor.getCount(InternalCounters.PRISM_OBJECT_COMPARE_COUNT));
    }

    /**
     * - `alice` is "basic user" - 10 extension properties, 3 assignments.
     * - `bob` is "heavy user" - "Heavy user" - 50 properties, 40 assignments.
     * */
    @Test
    public void test100AddAndGet() throws Exception {
        when("creating users");
        testAddUser(USER_ALICE_FILE, USER_ALICE_OID, 10, 3);
        testAddUser(USER_BOB_FILE, USER_BOB_OID, 50, 40);

        and("getting users");
        testGetUser("alice", USER_ALICE_OID);
        testGetUser("bob", USER_BOB_OID);
        testGetUser("alice", USER_ALICE_OID);
        testGetUser("bob", USER_BOB_OID);
        testGetUser("alice", USER_ALICE_OID);
        testGetUser("bob", USER_BOB_OID);
        testGetUser("alice", USER_ALICE_OID);
        testGetUser("bob", USER_BOB_OID);
        testGetUser("alice", USER_ALICE_OID);
        testGetUser("bob", USER_BOB_OID);
    }

    private void testAddUser(File userFile, String userOid, int extProperties, int roles) throws Exception {

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = parseObject(userFile);
        extensionValueGenerator.populateExtension(userBefore, extProperties);
        setRandomOrganizations(userBefore, roles);
        display("User before", userBefore);

        internalInspector.reset();
        profilingModelInspectorManager.reset();
        InternalMonitor.reset();
        rememberCounter(InternalCounters.PRISM_OBJECT_COMPARE_COUNT);
        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);
        long startMillis = System.currentTimeMillis();

        // WHEN
        when();

        addObject(userBefore, task, result);

        // THEN
        then();
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);

        display("Added user in "+(endMillis - startMillis)+" ms");

        displayDumpable("Model diagnostics", profilingModelInspectorManager);
        displayDumpable("Internal inspector", internalInspector);
        displayValue("Internal counters", InternalMonitor.debugDumpStatic(1));

        PrismObject<UserType> userAfter = getUser(userOid);
        display("User after", userAfter);
        assertAssignments(userAfter, roles);

        displayValue("Repo reads", InternalMonitor.getCount(InternalCounters.REPOSITORY_READ_COUNT));
        displayValue("Object compares", InternalMonitor.getCount(InternalCounters.PRISM_OBJECT_COMPARE_COUNT));
        displayValue("Object clones", InternalMonitor.getCount(InternalCounters.PRISM_OBJECT_CLONE_COUNT));

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
    }

    private void setRandomOrganizations(PrismObject<UserType> userBefore, int numberOfOrganizations) {
        assert numberOfOrganizations <= NUMBER_OF_GENERATED_EMPTY_ROLES/2; // to avoid long random generation
        Set<Integer> selectedOrganizationNumbers = new HashSet<>();
        while (selectedOrganizationNumbers.size() < numberOfOrganizations) {
            selectedOrganizationNumbers.add(RND.nextInt(NUMBER_OF_GENERATED_EMPTY_ROLES));
        }
        List<PolyStringType> userOrganizations = userBefore.asObjectable().getOrganization();
        selectedOrganizationNumbers.stream()
                .map(number -> createPolyStringType(String.format(GENERATED_EMPTY_ROLE_NAME_FORMAT, number)))
                .forEach(userOrganizations::add);
    }

    /** MID-8479 */
    private void testGetUser(String name, String oid) throws CommonException {
        executeGetUserIterations(oid, GET_ITERATIONS / 10); // heating up

        long start = System.currentTimeMillis();
        executeGetUserIterations(oid, GET_ITERATIONS);
        long duration = System.currentTimeMillis() - start;

        display(String.format("Retrieved %s in %,.3f µs", name, 1000.0 * duration / GET_ITERATIONS));
    }

    private void executeGetUserIterations(String oid, int iterations) throws CommonException {
        var task = getTestTask();
        var options = readOnly();
        OperationResult result1 = null;
        for (int i = 0; i < iterations; i++) {
            if (i % 100 == 0) {
                result1 = new OperationResult("dummy"); // to avoid operation result aggregation
            }
            modelService.getObject(UserType.class, oid, options, task, result1);
        }
    }
}
