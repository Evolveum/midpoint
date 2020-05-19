/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.scripting;

import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Tests new ("static") versions of scripting expressions.
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestScriptingBasicNew extends AbstractBasicScriptingTest {

    private static final File RECOMPUTE_JACK_NEW_TRIGGER_DIRECT_FILE = new File(TEST_DIR, "recompute-jack-new-trigger-direct.xml");
    private static final File RECOMPUTE_JACK_NEW_TRIGGER_OPTIMIZED_FILE = new File(TEST_DIR, "recompute-jack-new-trigger-optimized.xml");

    private static final File UNASSIGN_CAPTAIN_FROM_JACK_FILE = new File(TEST_DIR, "unassign-captain-from-jack.xml");
    private static final File ASSIGN_CAPTAIN_BY_NAME_TO_JACK_FILE = new File(TEST_DIR, "assign-captain-by-name-to-jack.xml");
    private static final File UNASSIGN_ALL_FROM_JACK_FILE = new File(TEST_DIR, "unassign-all-from-jack.xml");

    @Override
    String getSuffix() {
        return "";
    }

    @Test
    public void test352RecomputeJackTriggerDirect() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ScriptingExpressionType expression = parseScriptingExpression(RECOMPUTE_JACK_NEW_TRIGGER_DIRECT_FILE);

        when();
        evaluator.evaluateExpression(expression, task, result);
        Thread.sleep(20);
        evaluator.evaluateExpression(expression, task, result);
        Thread.sleep(20);
        ExecutionContext output = evaluator.evaluateExpression(expression, task, result);

        then();
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        assertSuccess(result);
        assertEquals("Triggered recompute of user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());

        assertUserAfter(USER_JACK_OID)
                .triggers()
                .assertTriggers(3);
    }

    @Test
    public void test353RecomputeJackTriggerOptimized() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(RECOMPUTE_JACK_NEW_TRIGGER_OPTIMIZED_FILE);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_TRIGGER).replace()
                .asObjectDelta(USER_JACK_OID);
        executeChanges(delta, null, task, result);

        assertUserBefore(USER_JACK_OID)
                .triggers()
                .assertTriggers(0);

        when();
        evaluator.evaluateExpression(expression, task, result);
        Thread.sleep(20);
        evaluator.evaluateExpression(expression, task, result);
        Thread.sleep(20);
        ExecutionContext output = evaluator.evaluateExpression(expression, task, result);

        then();
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        assertSuccess(result);
        assertEquals("Skipped triggering recompute of user:c0c010c0-d34d-b33f-f00d-111111111111(jack) because a trigger was already present\n", output.getConsoleOutput());

        assertUserAfter(USER_JACK_OID)
                .triggers()
                .assertTriggers(1);
    }

    @Test
    public void test361UnassignCaptainFromJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(UNASSIGN_CAPTAIN_FROM_JACK_FILE);

        when();
        ExecutionContext output = evaluator.evaluateExpression(expression, task, result);

        then();
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);

        assertSuccess(result);
        //assertEquals("Recomputed user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertUserAfterByUsername(USER_JACK_USERNAME)
                .assignments()
                .single()
                .assertResource(RESOURCE_DUMMY_RED_OID);
    }

    @Test
    public void test363AssignCaptainByNameToJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(ASSIGN_CAPTAIN_BY_NAME_TO_JACK_FILE);

        when();
        ExecutionContext output = evaluator.evaluateExpression(expression, task, result);

        then();
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);

        assertSuccess(result);
        //assertEquals("Recomputed user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertUserAfterByUsername(USER_JACK_USERNAME)
                .assignments()
                .assertAssignments(2)
                .by()
                .targetOid(ROLE_CAPTAIN_OID)
                .find()
                .end()
                .by()
                .resourceOid(RESOURCE_DUMMY_RED_OID)
                .find()
                .end();
    }

    @Test
    public void test364UnassignAllFromJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(UNASSIGN_ALL_FROM_JACK_FILE);

        when();
        ExecutionContext output = evaluator.evaluateExpression(expression, task, result);

        then();
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);

        assertSuccess(result);
        //assertEquals("Recomputed user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertUserAfterByUsername(USER_JACK_USERNAME)
                .assignments()
                .assertNone();
    }

}
