/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.DefinitionUpdateOption;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.internals.InternalsConfig;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractEmptyInternalModelTest;
import com.evolveum.midpoint.model.impl.controller.SchemaTransformer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ParsedGetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ExtensionValueGenerator;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.AssignmentGenerator;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.CheckedConsumer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the performance of various `model-impl` components.
 *
 * First, it checks the `applySchemasAndSecurity` method.
 *
 * Later, it may be extended or split into smaller tests. The time will tell.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPerformance extends AbstractEmptyInternalModelTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "perf");

    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_PERSON = TestObject.file(
            TEST_DIR, "object-template-person.xml", "202c9a5e-b876-4009-8b87-25688b12f7b8");
    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "3bb58e36-04b3-4601-a57a-4aaaa3b64458");
    private static final TestObject<RoleType> ROLE_CAN_READ_ALL = TestObject.file(
            TEST_DIR, "role-can-read-all.xml", "8d79c980-0999-49f7-ba11-6776dad41770");
    private static final TestObject<UserType> USER_CAN_READ_ALL = TestObject.file(
            TEST_DIR, "user-can-read-all.xml", "564261c3-efe8-4f35-845e-f928395d2cf1");
    private static final TestObject<RoleType> ROLE_CAN_READ_ALMOST_ALL = TestObject.file(
            TEST_DIR, "role-can-read-almost-all.xml", "b6774d03-b2c5-4b1b-a175-6deacbdd0115");
    private static final TestObject<UserType> USER_CAN_READ_ALMOST_ALL = TestObject.file(
            TEST_DIR, "user-can-read-almost-all.xml", "78eaaa5c-b8f1-4959-b356-6c41c04d613e");
    private static final TestObject<RoleType> ROLE_CAN_READ_FEW = TestObject.file(
            TEST_DIR, "role-can-read-few.xml", "46302f20-2197-4345-9d4b-ab183a028aa9");
    private static final TestObject<UserType> USER_CAN_READ_FEW = TestObject.file(
            TEST_DIR, "user-can-read-few.xml", "a17e2af6-7b60-4cf3-bebf-513af4a61b16");

    private static final String AUTZ_FULL = "full";
    private static final String AUTZ_READ_ALL = "read all";
    private static final String AUTZ_READ_ALMOST_ALL = "read almost all";
    private static final String AUTZ_READ_FEW = "read few";

    private static final Set<String> FAST = Set.of(AUTZ_FULL, AUTZ_READ_ALL); // these get more iterations to be measurable

    @Autowired private SchemaTransformer schemaTransformer;

    private final ExtensionValueGenerator extensionValueGenerator = ExtensionValueGenerator.withDefaults();
    private final AssignmentGenerator assignmentGenerator = AssignmentGenerator.withDefaults();

    private static final Set<DefinitionUpdateOption> OPTIONS_TESTED = Set.of(DefinitionUpdateOption.NONE);

    private static final int HEAT_UP_ITERATIONS = 2;
    private static final int TEST_ITERATIONS = 10;
    private static final int TEST_ITERATIONS_FOR_FAST_EXEC = 200;
    private static final int NUMBER_OF_USERS = 4000;

    private SearchResultList<PrismObject<UserType>> users;
    private final List<PerformanceResult> performanceResults = new ArrayList<>();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                OBJECT_TEMPLATE_PERSON, ARCHETYPE_PERSON,
                ROLE_CAN_READ_ALL, USER_CAN_READ_ALL,
                ROLE_CAN_READ_ALMOST_ALL, USER_CAN_READ_ALMOST_ALL,
                ROLE_CAN_READ_FEW, USER_CAN_READ_FEW);
        InternalsConfig.reset(); // We want to measure performance, so no consistency checking and the like.
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false;
    }

    /** Tests schema/security application for principals with varying authorizations. */
    @Test
    public void test100ApplySchemasAndSecurity() throws CommonException, IOException {
        users = generateUsers();

        when("execution #1");
        executeAutzTestFull();
        executeAutzTestReadAll();
        executeAutzTestReadAlmostAll();
        executeAutzTestReadFew();

        when("execution #2");
        executeAutzTestReadAll();
        executeAutzTestReadAlmostAll();
        executeAutzTestReadFew();
        executeAutzTestFull();

        when("execution #3");
        executeAutzTestReadAlmostAll();
        executeAutzTestReadFew();
        executeAutzTestFull();
        executeAutzTestReadAll();

        when("execution #4");
        executeAutzTestReadFew();
        executeAutzTestFull();
        executeAutzTestReadAll();
        executeAutzTestReadAlmostAll();

        try (PrintStream ps = new PrintStream(
                new FileOutputStream("target/results-" + System.currentTimeMillis() + ".csv"))) {
            dumpResults(System.out);
            dumpResults(ps);
        }
    }

    private void dumpResults(PrintStream stream) {
        stream.println("Option;Mode;E1;E2;E3;E4;Avg;Avg2");
        performanceResults.forEach(result -> stream.println(result.dump()));
    }

    private void executeAutzTestFull() throws CommonException {
        executeAutzTestForAuthorizations(userAdministrator, AUTZ_FULL, user -> assertReadAll(user));
    }

    private void executeAutzTestReadAll() throws CommonException {
        executeAutzTestForAuthorizations(USER_CAN_READ_ALL.get(), AUTZ_READ_ALL, user -> assertReadAll(user));
    }

    private void assertReadAll(UserType user) {
        assertUser(user.asPrismObject(), "after")
                .extension()
                .assertSize(50)
                .end();
        var assignment = user.getAssignment().get(0);
        assertThat(assignment.getTargetRef()).isNotNull();
        assertThat(assignment.getDescription()).isNotNull();
    }

    private void executeAutzTestReadAlmostAll() throws CommonException {
        executeAutzTestForAuthorizations(
                USER_CAN_READ_ALMOST_ALL.get(), AUTZ_READ_ALMOST_ALL, user -> assertReadAlmostAll(user));
    }

    private void assertReadAlmostAll(UserType user) {
        assertUser(user.asPrismObject(), "after")
                .extension()
                .assertSize(49)
                .end();
        var assignment = user.getAssignment().get(0);
        assertThat(assignment.getTargetRef()).isNotNull();
        assertThat(assignment.getDescription()).isNotNull();
    }

    private void executeAutzTestReadFew() throws CommonException {
        executeAutzTestForAuthorizations(USER_CAN_READ_FEW.get(), AUTZ_READ_FEW, user -> assertReadFew(user));
    }

    private void assertReadFew(UserType user) {
        assertUser(user.asPrismObject(), "after")
                .extension()
                .assertSize(1)
                .end();
        var assignment = user.getAssignment().get(0);
        assertThat(assignment.getTargetRef()).isNull();
        assertThat(assignment.getDescription()).isNotNull();
    }

    private void executeAutzTestForAuthorizations(
            PrismObject<UserType> principal, String autzLabel, CheckedConsumer<UserType> autzAsserter)
            throws CommonException {
        login(principal);
        executeAutzTestForOption(autzLabel, DefinitionUpdateOption.DEEP, autzAsserter);
        executeAutzTestForOption(autzLabel, DefinitionUpdateOption.ROOT_ONLY, autzAsserter);
        executeAutzTestForOption(autzLabel, DefinitionUpdateOption.NONE, autzAsserter);
    }

    private void executeAutzTestForOption(
            String autzLabel, DefinitionUpdateOption definitionUpdateOption, CheckedConsumer<UserType> autzAsserter)
            throws CommonException {

        if (!OPTIONS_TESTED.contains(definitionUpdateOption)) {
            return;
        }

        Task task = getTestTask();
        OperationResult result = task.getResult();

        String label = autzLabel + "/" + definitionUpdateOption;

        when("heating up: " + label);
        long heatUpDuration = executeSingleAutzTestIterations(
                HEAT_UP_ITERATIONS,
                definitionUpdateOption,
                user -> {
                    if (autzAsserter != null) {
                        autzAsserter.accept(user);
                    }
                    checkDefinition(user, definitionUpdateOption);
                }, task);

        int heatUpExecutions = users.size() * HEAT_UP_ITERATIONS;
        display(String.format(
                "Average processing time for a user is %,.1f µs during heat-up for %s (%,d executions)",
                1000.0 * heatUpDuration / heatUpExecutions, label, heatUpExecutions));

        when("testing: " + label);
        int iterations = FAST.contains(autzLabel) ? TEST_ITERATIONS_FOR_FAST_EXEC : TEST_ITERATIONS;
        long duration = executeSingleAutzTestIterations(iterations, definitionUpdateOption, null, task);
        int executions = users.size() * iterations;
        double averageDuration = 1000.0 * duration / executions;
        display(String.format(
                "Testing %s: Applied to a single user in %,.1f µs (%,d ms total, %,d executions)",
                label, averageDuration, duration, executions));
        PerformanceResult.record(performanceResults, autzLabel, definitionUpdateOption, averageDuration);
    }

    private long executeSingleAutzTestIterations(
            int iterations,
            DefinitionUpdateOption definitionUpdateOption,
            CheckedConsumer<UserType> asserter,
            Task task)
            throws CommonException {

        var parsedOptions = ParsedGetOperationOptions.of(
                GetOperationOptions.createReadOnly()
                        .definitionUpdate(definitionUpdateOption));

        List<PrismObject<UserType>> usersAfter = new ArrayList<>();
        long start = System.currentTimeMillis();
        OperationResult result1 = null;
        for (int i = 0; i < iterations; i++) {
            if (i % 100 == 0) {
                result1 = new OperationResult("dummy"); // to avoid operation result aggregation
            }
            usersAfter = schemaTransformer.applySchemasAndSecurityToObjects(users, parsedOptions, task, result1);
        }
        long duration = System.currentTimeMillis() - start;

        if (asserter != null) {
            asserter.accept(usersAfter.get(0).asObjectable());
        }

        return duration;
    }

    private @NotNull SearchResultList<PrismObject<UserType>> generateUsers() throws SchemaException {
        SearchResultList<PrismObject<UserType>> users = new SearchResultList<>();
        for (int i = 0; i < NUMBER_OF_USERS; i++) {
            UserType user = new UserType()
                    .name("user " + i)
                    .assignment(ARCHETYPE_PERSON.assignmentTo()
                            .description("just-to-make-test-happy"));
            PrismObject<UserType> userPrismObject = user.asPrismObject();
            extensionValueGenerator.populateExtension(userPrismObject, 50);
            assignmentGenerator.populateAssignments(user, 50);
            assignmentGenerator.createRoleRefs(user);
            userPrismObject.freeze();
            users.add(userPrismObject);
        }
        users.freeze();
        return users;
    }

    private void checkDefinition(UserType user, DefinitionUpdateOption option) {
        PrismObjectDefinition<UserType> rootDef = user.asPrismObject().getDefinition();
        assertThat(rootDef.findPropertyDefinition(UserType.F_DESCRIPTION).getDisplayName())
                .as("description displayName in root def")
                .isEqualTo(option == DefinitionUpdateOption.NONE ? "ObjectType.description" : "X-DESCRIPTION");
        if (user.getDescription() != null) {
            var localDef = user.asPrismObject().findProperty(UserType.F_DESCRIPTION).getDefinition();
            assertThat(localDef.getDisplayName())
                    .as("description displayName (local)")
                    .isEqualTo(option != DefinitionUpdateOption.DEEP ? "ObjectType.description" : "X-DESCRIPTION");
        }
    }

    private static class PerformanceResult {
        @NotNull private final String autzLabel;
        @NotNull private final DefinitionUpdateOption definitionUpdateOption;
        @NotNull private final List<Double> values = new ArrayList<>();

        private PerformanceResult(@NotNull String autzLabel, @NotNull DefinitionUpdateOption definitionUpdateOption) {
            this.autzLabel = autzLabel;
            this.definitionUpdateOption = definitionUpdateOption;
        }

        static void record(
                List<PerformanceResult> performanceResults,
                String autzLabel,
                DefinitionUpdateOption definitionUpdateOption,
                double averageDuration) {
            for (PerformanceResult result : performanceResults) {
                if (result.matches(autzLabel, definitionUpdateOption)) {
                    result.record(averageDuration);
                    return;
                }
            }
            performanceResults.add(
                    new PerformanceResult(autzLabel, definitionUpdateOption)
                            .record(averageDuration));
        }

        private boolean matches(String autzLabel, DefinitionUpdateOption definitionUpdateOption) {
            return this.autzLabel.equals(autzLabel) && this.definitionUpdateOption == definitionUpdateOption;
        }

        private PerformanceResult record(double newValue) {
            values.add(newValue);
            return this;
        }

        String dump() {
            StringBuilder sb = new StringBuilder();
            sb.append(definitionUpdateOption).append(";").append(autzLabel);
            values.forEach(val -> sb.append(";").append(val));
            sb.append(";").append(getAverage(values));
            sb.append(";").append(getAverage(getValuesExceptHighest()));
            return sb.toString();
        }

        private static double getAverage(List<Double> values) {
            return values.stream().mapToDouble(value -> value).sum() / values.size();
        }

        private List<Double> getValuesExceptHighest() {
            List<Double> copy = new ArrayList<>(values);
            copy.sort(Comparator.naturalOrder());
            return copy.subList(0, values.size() - 1);
        }
    }
}
