/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.scripting;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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

    private static final File SEARCH_WITH_SCRIPT_OK = new File(TEST_DIR, "search-with-script-ok.xml");
    private static final File SEARCH_WITH_SCRIPT_BAD = new File(TEST_DIR, "search-with-script-bad.xml");
    private static final File UNASSIGN_WITH_SCRIPT_BAD = new File(TEST_DIR, "unassign-with-script-bad.xml");

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
        addObject(user.asPrismObject(), task, result);

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
                .and().item(ShadowType.F_OBJECT_CLASS).eq(dummyResourceCtl.getAccountObjectClass())
                .build();
        displayValue("objects",
                DebugUtil.debugDump(repositoryService.searchObjects(ShadowType.class, query, null, result)));
        return repositoryService.countObjects(ShadowType.class, query, null, result);
    }

    /**
     * Check for execution of groovy script as part of search filter at various places.
     */
    @Test
    public void test920CheckScriptExecution() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user able to run bulk actions");
        RoleType role = new RoleType()
                .name(getTestNameShort())
                .authorization(new AuthorizationType()
                        .action(ModelAuthorizationAction.EXECUTE_SCRIPT.getUrl()))
                .authorization(new AuthorizationType()
                        .action(ModelAuthorizationAction.READ.getUrl()));
        addObject(role.asPrismObject(), task, result);

        String regularUserName = getTestNameShort();
        UserType user = new UserType()
                .name(regularUserName)
                .assignment(ObjectTypeUtil.createAssignmentTo(role, SchemaConstants.ORG_DEFAULT));
        addObject(user.asPrismObject(), task, result);

        checkSearchWithScriptExecuted(regularUserName, SEARCH_WITH_SCRIPT_OK);
        checkSearchWithScriptNotExecuted(regularUserName, SEARCH_WITH_SCRIPT_BAD);
        checkSearchWithScriptExecuted(USER_ADMINISTRATOR_USERNAME, SEARCH_WITH_SCRIPT_OK);
        checkSearchWithScriptExecuted(USER_ADMINISTRATOR_USERNAME, SEARCH_WITH_SCRIPT_BAD);

        checkUnassignWithScriptNotExecuted(regularUserName, UNASSIGN_WITH_SCRIPT_BAD);
    }

    private void checkSearchWithScriptExecuted(String userName, File file) throws CommonException, IOException {
        ExecutionContext output = execute(userName, file);
        int objects = output.getFinalOutput().getData().size();
        assertThat(objects).as("Objects found").isEqualTo(1);
    }

    private ExecutionContext execute(String userName, File file) throws CommonException, IOException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("logged in as " + userName);
        login(userName);

        and("executing " + file);
        ExecuteScriptType executeScriptBean = parseExecuteScript(file);
        return evaluator.evaluateExpression(
                executeScriptBean, VariablesMap.emptyMap(), false, task, result);
    }

    @SuppressWarnings("SameParameterValue")
    private void checkSearchWithScriptNotExecuted(String userName, File file) throws CommonException, IOException {
        try {
            execute(userName, file);
            fail("unexpected success");
        } catch (ScriptExecutionException e) {
            assertNoScriptsAllowed(e);
        }
    }

    private void assertNoScriptsAllowed(ScriptExecutionException e) {
        SecurityViolationException cause = ExceptionUtil.findCause(e, SecurityViolationException.class);
        displayExpectedException(cause);
        assertThat(cause)
                .hasMessageContaining("Access to script expression evaluator not allowed");
    }

    @SuppressWarnings("SameParameterValue")
    private void checkUnassignWithScriptNotExecuted(String userName, File file) throws CommonException, IOException {
        try {
            execute(userName, file);
            fail("unexpected success");
        } catch (ScriptExecutionException e) {
            assertNoScriptsAllowed(e);
        }
    }
}
