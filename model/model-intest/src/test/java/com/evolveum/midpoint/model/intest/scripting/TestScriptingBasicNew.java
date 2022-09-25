/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.scripting;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static org.assertj.core.api.Assertions.assertThat;
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
    private static final File EXECUTE_CUSTOM_DELTA = new File(TEST_DIR, "execute-custom-delta.xml");

    private static final TestResource<TaskType> TASK_DELETE_SHADOWS_MULTINODE = new TestResource<>(TEST_DIR, "task-delete-shadows-multinode.xml", "931e34be-5cf0-46c6-8cc1-90812a66d5cb");

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

    @Test
    public void test900ExecuteCustomDelta() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ExecuteScriptType executeScript = parseExecuteScript(EXECUTE_CUSTOM_DELTA);

        unassignAllRoles(USER_JACK_OID);

        when();
        ExecutionContext output = evaluator.evaluateExpression(executeScript, VariablesMap.emptyMap(), false, task, result);

        then();
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);

        assertSuccess(result);
        assertUserAfterByUsername(USER_JACK_USERNAME)
                .assertAssignments(1)
                .assignments()
                    .assertRole(ROLE_SUPERUSER_OID);
    }

    /**
     * Deletes shadows while searching for them using noFetch option. (Tests for correct options application by tasks: MID-6717).
     *
     * Also check correct task OID in audit messages: MID-6713.
     */
    @Test
    public void test910DeleteShadowsMultinode() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType user = new UserType(prismContext)
                .name("test910")
                .beginAssignment()
                    .beginConstruction()
                        .resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
                    .<AssignmentType>end()
                .end();
        addObject(user, task, result);

        String shadowOid = assertUser(user.getOid(), "after creation")
                .display()
                .links()
                    .singleLive()
                        .getOid();

        int before = countDummyAccountShadows(result);
        displayValue("account shadows before", before);
        assertThat(before).isGreaterThan(0);

        dummyAuditService.clear();

        when();

        addObject(TASK_DELETE_SHADOWS_MULTINODE, task, result);
        runTaskTreeAndWaitForFinish(TASK_DELETE_SHADOWS_MULTINODE.oid, 15000);

        then();

        dumpTaskTree(TASK_DELETE_SHADOWS_MULTINODE.oid, result);

        int after = countDummyAccountShadows(result);
        displayValue("account shadows after", after);
        assertThat(after).isEqualTo(0);

        displayDumpable("Audit", dummyAuditService);
        List<AuditEventRecord> records = dummyAuditService.getRecords().stream()
                .filter(record -> record.getEventStage() == AuditEventStage.EXECUTION)
                .filter(record -> record.getTargetRef() != null && shadowOid.equals(record.getTargetRef().getOid()))
                .collect(Collectors.toList());
        assertThat(records).as("Shadow " + shadowOid + " deletion records").hasSize(1);
        AuditEventRecord record = records.get(0);
        assertThat(record.getTaskOid()).as("task OID in audit record").isEqualTo(TASK_DELETE_SHADOWS_MULTINODE.oid);
    }

    private int countDummyAccountShadows(OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(RI_ACCOUNT_OBJECT_CLASS)
                .build();
        displayValue("objects",
                DebugUtil.debugDump(repositoryService.searchObjects(ShadowType.class, query, null, result)));
        return repositoryService.countObjects(ShadowType.class, query, null, result);
    }
}
