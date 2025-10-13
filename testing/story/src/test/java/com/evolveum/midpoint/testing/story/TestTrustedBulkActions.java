/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ActivityCustomization;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.RunFlag;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the execution of (powerful) trusted bulk actions by unprivileged users, for example in task templates or policy rules.
 *
 * Related to `TestExpressionProfiles` in `model-intest`. However, tests here are more story-oriented.
 *
 * See MID-6913 and MID-7831.
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestTrustedBulkActions extends AbstractStoryTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "trusted-bulk-actions");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    public static final RunFlag FLAG = new RunFlag();
    public static final RunFlag FLAG2 = new RunFlag();

    private static final TestObject<FunctionLibraryType> FUNCTION_LIBRARY_ONE = TestObject.file(
            TEST_DIR, "function-library-one.xml", "17b5b255-c71e-4a67-8e42-349862e295ac");
    private static final TestObject<ArchetypeType> ARCHETYPE_TRUSTED_TASK = TestObject.file(
            TEST_DIR, "archetype-trusted-task.xml", "2179963e-d1cb-4195-b763-d4aa2bb518d7");
    private static final TestObject<ArchetypeType> ARCHETYPE_LITTLE_TRUSTED_TASK = TestObject.file(
            TEST_DIR, "archetype-little-trusted-task.xml", "655a32e0-aaa8-4163-9f4d-eee68a84a25d");
    private static final TestObject<ArchetypeType> ARCHETYPE_TRUSTED_ROLE = TestObject.file(
            TEST_DIR, "archetype-trusted-role.xml", "988c28d2-f879-4e07-a3cb-5ea7ad206146");

    private static final TestTask TASK_TEMPLATE_SCRIPTING_NO_PROFILE = new TestTask(
            TEST_DIR, "task-template-scripting-no-profile.xml", "f38b71ad-e212-4c46-8594-032202e0e9b9");
    private static final TestTask TASK_TEMPLATE_SCRIPTING_NO_PROFILE_NON_ITERATIVE = new TestTask(
            TEST_DIR, "task-template-scripting-no-profile-non-iterative.xml", "bf575791-eabd-438e-855e-09824a99b088");
    private static final TestTask TASK_TEMPLATE_SCRIPTING_TRUSTED = new TestTask(
            TEST_DIR, "task-template-scripting-trusted.xml", "a801db00-cd4b-4998-a08b-3b964b9d7cf1");
    private static final TestTask TASK_TEMPLATE_SCRIPTING_LITTLE_TRUSTED = new TestTask(
            TEST_DIR, "task-template-scripting-little-trusted.xml", "472ffb27-5b99-43e7-9c5c-fc0f453b3e89");
    private static final TestTask TASK_TEMPLATE_SCRIPTING_LITTLE_TRUSTED_RUN_PRIVILEGED = new TestTask(
            TEST_DIR, "task-template-scripting-little-trusted-run-privileged.xml", "8a953d7f-49ad-42a5-8ef3-c0e2dcf64819");

    private static final TestObject<RoleType> ROLE_WITH_SCRIPTING_ACTION = TestObject.file(
            TEST_DIR, "role-with-scripting-action.xml", "128a5458-bcd4-4bf4-b110-664677e73aa4");

    private static final TestObject<RoleType> ROLE_UNPRIVILEGED = TestObject.file(
            TEST_DIR, "role-unprivileged.xml", "4cbaf4a6-bf40-4b86-9e77-98398c36d383");
    private static final TestObject<UserType> USER_JOE = TestObject.file(
            TEST_DIR, "user-joe.xml", "aacd6290-7aab-4192-afdd-9c10616ac0bb");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        ROLE_WITH_SCRIPTING_ACTION.initRaw(this, initResult);

        initTestObjects(initTask, initResult,
                FUNCTION_LIBRARY_ONE,
                ARCHETYPE_TRUSTED_TASK,
                ARCHETYPE_LITTLE_TRUSTED_TASK,
                ARCHETYPE_TRUSTED_ROLE,
                TASK_TEMPLATE_SCRIPTING_NO_PROFILE,
                TASK_TEMPLATE_SCRIPTING_NO_PROFILE_NON_ITERATIVE,
                TASK_TEMPLATE_SCRIPTING_TRUSTED,
                TASK_TEMPLATE_SCRIPTING_LITTLE_TRUSTED,
                TASK_TEMPLATE_SCRIPTING_LITTLE_TRUSTED_RUN_PRIVILEGED,
                ROLE_UNPRIVILEGED, USER_JOE);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    /** Baseline: trying to run script from within task template without an expression profile. It should fail. */
    @Test
    public void test100TemplateWithoutProfile() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        login(USER_JOE.getNameOrig());
        FLAG.reset();

        when("task template is instantiated");
        var taskOid = modelInteractionService.submitTaskFromTemplate(
                TASK_TEMPLATE_SCRIPTING_NO_PROFILE.oid,
                ActivityCustomization.forOids(List.of(USER_JOE.oid)),
                task, result);
        waitForTaskCloseOrSuspend(taskOid, 30000L);

        then("the execution is not successful");
        assertTask(taskOid, "after")
                .display()
                .assertPartialError()
                .assertResultMessageContains("Access to script expression evaluator not allowed");

        FLAG.assertNotSet();
    }

    /** Baseline: the same as above for non-iterative task. It should fail. */
    @Test
    public void test105TemplateWithoutProfileNonIterative() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        login(USER_JOE.getNameOrig());
        FLAG.reset();

        when("task template is instantiated");
        var taskOid = modelInteractionService.submitTaskFromTemplate(
                TASK_TEMPLATE_SCRIPTING_NO_PROFILE_NON_ITERATIVE.oid,
                ActivityCustomization.none(),
                task, result);
        waitForTaskCloseOrSuspend(taskOid, 30000L);

        then("the execution is not successful");
        assertTask(taskOid, "after")
                .display()
                .assertFatalError()
                .assertResultMessageContains("Access to script expression evaluator not allowed");

        FLAG.assertNotSet();
    }

    /** Now trying to run script from within task template with `trusted` expression profile. It should work. */
    @Test
    public void test110TemplateWithTrustedProfile() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        login(USER_JOE.getNameOrig());
        FLAG.reset();

        when("task template is instantiated");
        var taskOid = modelInteractionService.submitTaskFromTemplate(
                TASK_TEMPLATE_SCRIPTING_TRUSTED.oid,
                ActivityCustomization.forOids(List.of(USER_JOE.oid)),
                task, result);
        waitForTaskCloseOrSuspend(taskOid, 30000L);

        then("the execution is successful");
        assertTask(taskOid, "after")
                .display()
                .assertSuccess();

        FLAG.assertSet();
    }

    /** Now trying to run script from within task template with `little-trusted` expression profile. It should work. */
    @Test
    public void test120TemplateWithLittleTrustedProfile() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        login(USER_JOE.getNameOrig());
        FLAG.reset();

        when("task template is instantiated");
        var taskOid = modelInteractionService.submitTaskFromTemplate(
                TASK_TEMPLATE_SCRIPTING_LITTLE_TRUSTED.oid,
                ActivityCustomization.forOids(List.of(USER_JOE.oid)),
                task, result);
        waitForTaskCloseOrSuspend(taskOid, 30000L);

        then("the execution is successful");
        assertTask(taskOid, "after")
                .display()
                .assertSuccess();

        FLAG.assertSet();
    }

    /**
     * Now trying to run script from within task template with `little-trusted` expression profile.
     * It uses a library function with `runPrivileged` flag set.
     * It should work.
     */
    @Test
    public void test130TemplateWithLittleTrustedProfileRunPrivileged() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        login(USER_JOE.getNameOrig());
        FLAG.reset();

        when("task template is instantiated");
        var taskOid = modelInteractionService.submitTaskFromTemplate(
                TASK_TEMPLATE_SCRIPTING_LITTLE_TRUSTED_RUN_PRIVILEGED.oid,
                ActivityCustomization.forOids(List.of(USER_JOE.oid)),
                task, result);
        waitForTaskCloseOrSuspend(taskOid, 30000L);

        then("the execution is successful");
        assertTask(taskOid, "after")
                .display()
                .assertSuccess();

        FLAG.assertSet();
    }

    /**
     * Touching a role with attached bulk-action-based policy rule (regarding constraints and action).
     */
    @Test
    public void test200PolicyRule() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        login(USER_JOE.getNameOrig());
        FLAG.reset();
        FLAG2.reset();

        when("role with actions is recomputed");
        recomputeFocus(RoleType.class, ROLE_WITH_SCRIPTING_ACTION.oid, task, result);

        then("the execution is successful");
        assertSuccess(result);

        FLAG.assertSet(); // set by constraint
        FLAG2.assertSet(); // set by action
    }
}
