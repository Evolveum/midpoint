/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.scripting;

import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ScriptExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.ModelWebService;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestScriptingBasic extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/scripting");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final File LOG_FILE = new File(TEST_DIR, "log.xml");
    private static final File SEARCH_FOR_USERS_FILE = new File(TEST_DIR, "search-for-users.xml");
    private static final File SEARCH_FOR_USERS_WITH_EXPRESSIONS_FILE = new File(TEST_DIR, "search-for-users-with-expressions.xml");
    private static final File SEARCH_FOR_USERS_RESOLVE_NAMES_FOR_ROLE_MEMBERSHIP_REF_FILE = new File(TEST_DIR, "search-for-users-resolve-names-for-roleMembershipRef.xml");
    private static final File SEARCH_FOR_USERS_RESOLVE_ROLE_MEMBERSHIP_REF_FILE = new File(TEST_DIR, "search-for-users-resolve-roleMembershipRef.xml");
    private static final File SEARCH_FOR_SHADOWS_FILE = new File(TEST_DIR, "search-for-shadows.xml");
    private static final File SEARCH_FOR_SHADOWS_NOFETCH_FILE = new File(TEST_DIR, "search-for-shadows-nofetch.xml");
    private static final File SEARCH_FOR_RESOURCES_FILE = new File(TEST_DIR, "search-for-resources.xml");
    private static final File SEARCH_FOR_ROLES_FILE = new File(TEST_DIR, "search-for-roles.xml");
    private static final File SEARCH_FOR_USERS_ACCOUNTS_FILE = new File(TEST_DIR, "search-for-users-accounts.xml");
    private static final File SEARCH_FOR_USERS_ACCOUNTS_NOFETCH_FILE = new File(TEST_DIR, "search-for-users-accounts-nofetch.xml");
    private static final File DISABLE_JACK_FILE = new File(TEST_DIR, "disable-jack.xml");
    private static final File ENABLE_JACK_FILE = new File(TEST_DIR, "enable-jack.xml");
    private static final File DELETE_AND_ADD_JACK_FILE = new File(TEST_DIR, "delete-and-add-jack.xml");
    private static final File MODIFY_JACK_FILE = new File(TEST_DIR, "modify-jack.xml");
    private static final File MODIFY_JACK_BACK_FILE = new File(TEST_DIR, "modify-jack-back.xml");
    private static final File MODIFY_JACK_PASSWORD_FILE = new File(TEST_DIR, "modify-jack-password.xml");
    private static final File MODIFY_JACK_PASSWORD_TASK_FILE = new File(TEST_DIR, "modify-jack-password-task.xml");
    private static final String MODIFY_JACK_PASSWORD_TASK_OID = "9de76345-0f02-48de-86bf-e7a887cb374a";
    private static final File RECOMPUTE_JACK_FILE = new File(TEST_DIR, "recompute-jack.xml");
    private static final File ASSIGN_TO_JACK_FILE = new File(TEST_DIR, "assign-to-jack.xml");
    private static final File ASSIGN_TO_JACK_DRY_AND_RAW_FILE = new File(TEST_DIR, "assign-to-jack-dry-and-raw.xml");
    private static final File ASSIGN_TO_JACK_2_FILE = new File(TEST_DIR, "assign-to-jack-2.xml");
    private static final File UNASSIGN_FROM_WILL_FILE = new File(TEST_DIR, "unassign-from-will.xml");
    private static final File UNASSIGN_FROM_WILL_2_FILE = new File(TEST_DIR, "unassign-from-will-2.xml");
    private static final File UNASSIGN_FROM_WILL_3_FILE = new File(TEST_DIR, "unassign-from-will-3.xml");
    private static final File ASSIGN_TO_WILL_FILE = new File(TEST_DIR, "assign-to-will.xml");
    private static final File ASSIGN_TO_WILL_2_FILE = new File(TEST_DIR, "assign-to-will-2.xml");
    private static final File PURGE_DUMMY_BLACK_SCHEMA_FILE = new File(TEST_DIR, "purge-dummy-black-schema.xml");
    private static final File TEST_DUMMY_RESOURCE_FILE = new File(TEST_DIR, "test-dummy-resource.xml");
    private static final File NOTIFICATION_ABOUT_JACK_FILE = new File(TEST_DIR, "notification-about-jack.xml");
    private static final File NOTIFICATION_ABOUT_JACK_TYPE2_FILE = new File(TEST_DIR, "notification-about-jack-type2.xml");
    private static final File SCRIPTING_USERS_FILE = new File(TEST_DIR, "scripting-users.xml");
    private static final File SCRIPTING_USERS_IN_BACKGROUND_FILE = new File(TEST_DIR, "scripting-users-in-background.xml");
    private static final File SCRIPTING_USERS_IN_BACKGROUND_ASSIGN_FILE = new File(TEST_DIR, "scripting-users-in-background-assign.xml");
    private static final File SCRIPTING_USERS_IN_BACKGROUND_TASK_FILE = new File(TEST_DIR, "scripting-users-in-background-task.xml");
    private static final File SCRIPTING_USERS_IN_BACKGROUND_ITERATIVE_TASK_FILE = new File(TEST_DIR, "scripting-users-in-background-iterative-task.xml");
    private static final File START_TASKS_FROM_TEMPLATE_FILE = new File(TEST_DIR, "start-tasks-from-template.xml");
    private static final File GENERATE_PASSWORDS_FILE = new File(TEST_DIR, "generate-passwords.xml");
    private static final File GENERATE_PASSWORDS_2_FILE = new File(TEST_DIR, "generate-passwords-2.xml");
    private static final File GENERATE_PASSWORDS_3_FILE = new File(TEST_DIR, "generate-passwords-3.xml");
    private static final File ECHO_FILE = new File(TEST_DIR, "echo.xml");
    private static final File USE_VARIABLES_FILE = new File(TEST_DIR, "use-variables.xml");
    private static final File TASK_TO_RESUME_FILE = new File(TEST_DIR, "task-to-resume.xml");
    private static final File TASK_TO_KEEP_SUSPENDED_FILE = new File(TEST_DIR, "task-to-keep-suspended.xml");
    private static final File RESUME_SUSPENDED_TASKS_FILE = new File(TEST_DIR, "resume-suspended-tasks.xml");
    private static final ItemName USER_NAME_TASK_EXTENSION_PROPERTY = new ItemName("http://midpoint.evolveum.com/xml/ns/samples/piracy", "userName");
    private static final ItemName USER_DESCRIPTION_TASK_EXTENSION_PROPERTY = new ItemName("http://midpoint.evolveum.com/xml/ns/samples/piracy", "userDescription");
    private static final ItemName STUDY_GROUP_TASK_EXTENSION_PROPERTY = new ItemName("http://midpoint.evolveum.com/xml/ns/samples/piracy", "studyGroup");

    private static final String PASSWORD_PLAINTEXT_FRAGMENT = "pass1234wor";
    private static final String PASSWORD_PLAINTEXT_1 = "pass1234wor1";
    private static final String PASSWORD_PLAINTEXT_2 = "pass1234wor2";
    private static final String PASSWORD_PLAINTEXT_3 = "pass1234wor3";

    @Autowired
    private ScriptingExpressionEvaluator scriptingExpressionEvaluator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);
        InternalMonitor.reset();

//        InternalMonitor.setTraceShadowFetchOperation(true);
//        InternalMonitor.setTraceResourceSchemaOperations(true);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100EmptySequence() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ExpressionSequenceType sequence = new ExpressionSequenceType();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(sequence, task, result);

        // THEN
        dumpOutput(output, result);
        assertNoOutputData(output);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test110EmptyPipeline() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ExpressionPipelineType pipeline = new ExpressionPipelineType();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(pipeline, task, result);

        // THEN
        dumpOutput(output, result);
        assertNoOutputData(output);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test112Echo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ExecuteScriptType executeScript = parseExecuteScript(ECHO_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(executeScript, VariablesMap.emptyMap(), false, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        PipelineData data = output.getFinalOutput();
        assertEquals("Unexpected # of items in output", 4, data.getData().size());

        // TODO check correct serialization
    }

    @Test
    public void test120Log() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType logAction = parseScriptingExpression(LOG_FILE);

        LogfileTestTailer tailer = new LogfileTestTailer(LoggingConfigurationManager.AUDIT_LOGGER_NAME);
        tailer.tail();
        tailer.setExpecteMessage("Custom message:");

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(logAction, task, result);

        // THEN
        dumpOutput(output, result);
        assertNoOutputData(output);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        tailer.tail();
        tailer.assertExpectedMessage();
    }

    private ScriptingExpressionType parseScriptingExpression(File file) throws IOException, SchemaException {
        // we cannot specify explicit type parameter here, as the parsed files contain subtypes of ScriptingExpressionType
        return prismContext.parserFor(file).parseRealValue();
    }

    private ExecuteScriptType parseExecuteScript(File file) throws IOException, SchemaException {
        return prismContext.parserFor(file).parseRealValue(ExecuteScriptType.class);
    }

    @Test
    public void test200SearchUser() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SEARCH_FOR_USERS_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(2, output.getFinalOutput().getData().size());
    }

    @Test
    public void test202SearchUserWithExpressions() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ExecuteScriptType executeScript = prismContext.parserFor(SEARCH_FOR_USERS_WITH_EXPRESSIONS_FILE).parseRealValue();
        VariablesMap variables = new VariablesMap();
        variables.put("value1", "administrator", String.class);
        variables.put("value2", "jack", String.class);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(executeScript, variables, false, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(2, output.getFinalOutput().getData().size());
        assertEquals(new HashSet<>(Arrays.asList("administrator", "jack")),
                output.getFinalOutput().getData().stream()
                        .map(i -> ((PrismObjectValue<?>) i.getValue()).getName().getOrig())
                        .collect(Collectors.toSet()));
    }

    @Test
    public void test205SearchForResources() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SEARCH_FOR_RESOURCES_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(13, output.getFinalOutput().getData().size());
    }

    @Test
    public void test206SearchForRoles() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SEARCH_FOR_ROLES_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test210SearchForShadows() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SEARCH_FOR_SHADOWS_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(5, output.getFinalOutput().getData().size());
        assertAttributesFetched(output.getFinalOutput().getData());
    }

    @Test
    public void test215SearchForShadowsNoFetch() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SEARCH_FOR_SHADOWS_NOFETCH_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(5, output.getFinalOutput().getData().size());
        assertAttributesNotFetched(output.getFinalOutput().getData());
    }

    @Test
    public void test220SearchForUsersAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SEARCH_FOR_USERS_ACCOUNTS_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(4, output.getFinalOutput().getData().size());
        assertAttributesFetched(output.getFinalOutput().getData());
    }

    @Test
    public void test225SearchForUsersAccountsNoFetch() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SEARCH_FOR_USERS_ACCOUNTS_NOFETCH_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(4, output.getFinalOutput().getData().size());
        assertAttributesNotFetched(output.getFinalOutput().getData());
    }

    @Test
    public void test300DisableJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(DISABLE_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        assertEquals("Disabled user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertAdministrativeStatusDisabled(searchObjectByName(UserType.class, "jack"));
    }

    @Test
    public void test310EnableJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(ENABLE_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Enabled user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertAdministrativeStatusEnabled(searchObjectByName(UserType.class, "jack"));
    }

    @Test
    public void test320DeleteAndAddJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(DELETE_AND_ADD_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Deleted user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\nAdded user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertAdministrativeStatusEnabled(searchObjectByName(UserType.class, "jack"));
    }

    @Test
    public void test330ModifyJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(MODIFY_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Modified user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertEquals("Nowhere", searchObjectByName(UserType.class, "jack").asObjectable().getLocality().getOrig());
    }

    @Test
    public void test340ModifyJackBack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(MODIFY_JACK_BACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Modified user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        assertEquals("Caribbean", searchObjectByName(UserType.class, "jack").asObjectable().getLocality().getOrig());
    }

    @Test
    public void test350RecomputeJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(RECOMPUTE_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Recomputed user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
    }

    @Test
    public void test360AssignToJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(ASSIGN_TO_JACK_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        //assertEquals("Recomputed user:c0c010c0-d34d-b33f-f00d-111111111111(jack)\n", output.getConsoleOutput());
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("jack after assignments creation", jack);
        assertAssignedAccount(jack, "10000000-0000-0000-0000-000000000104");
        assertAssignedRole(jack, "12345678-d34d-b33f-f00d-55555555cccc");
    }

    /**
     * MID-6141
     */
    @Test
    public void test365AssignToJackDryAndRaw() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(ASSIGN_TO_JACK_DRY_AND_RAW_FILE);

        when();
        try {
            scriptingExpressionEvaluator.evaluateExpression(expression, task, result);
            fail("unexpected success");
        } catch (ScriptExecutionException e) {
            displayExpectedException(e);
            assertThat(e).hasMessageContaining("previewChanges is not supported in raw mode");
        }
    }

    @Test
    public void test370AssignToJackInBackground() throws Exception {
        // GIVEN
        OperationResult result = getTestOperationResult();
        ScriptingExpressionType expression = parseScriptingExpression(ASSIGN_TO_JACK_2_FILE);

        // WHEN
        Task task = taskManager.createTaskInstance();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        scriptingExpressionEvaluator.evaluateExpressionInBackground(expression, task, result);
        waitForTaskFinish(task.getOid(), false);
        task.refresh(result);

        // THEN
        display(task.getResult());
        TestUtil.assertSuccess(task.getResult());
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("jack after assignment creation", jack);
        assertAssignedRole(jack, "12345678-d34d-b33f-f00d-555555556677");
    }

    @Test
    public void test390AssignToWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(ASSIGN_TO_WILL_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> will = getUser(USER_WILL_OID);
        display("will after assignments creation", will);
        MidPointAsserts.assertAssigned(will, "12345678-d34d-b33f-f00d-555555556666", RoleType.COMPLEX_TYPE, RelationTypes.MANAGER.getRelation());
    }

    @Test
    public void test391UnassignFromWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(UNASSIGN_FROM_WILL_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> will = getUser(USER_WILL_OID);
        display("will after unassign assignment", will);
        MidPointAsserts.assertNotAssigned(will, "12345678-d34d-b33f-f00d-555555556666", RoleType.COMPLEX_TYPE, RelationTypes.MEMBER.getRelation());
        MidPointAsserts.assertAssigned(will, "12345678-d34d-b33f-f00d-555555556666", RoleType.COMPLEX_TYPE, RelationTypes.MANAGER.getRelation());
        MidPointAsserts.assertAssignedResource(will, "10000000-0000-0000-0000-000000000004");
    }

    @Test
    public void test392UnassignFromWill2() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(UNASSIGN_FROM_WILL_2_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> will = getUser(USER_WILL_OID);
        display("will after unassign assignment", will);
        MidPointAsserts.assertNotAssigned(will, "12345678-d34d-b33f-f00d-555555556666", RoleType.COMPLEX_TYPE, RelationTypes.MANAGER.getRelation());
        MidPointAsserts.assertNotAssigned(will, "12345678-d34d-b33f-f00d-555555556666", RoleType.COMPLEX_TYPE, RelationTypes.OWNER.getRelation());
        MidPointAsserts.assertAssignedResource(will, "10000000-0000-0000-0000-000000000004");
    }

    @Test
    public void test393UnassignFromWill3() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(UNASSIGN_FROM_WILL_3_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> will = getUser(USER_WILL_OID);
        display("will after unassign assignment", will);
        MidPointAsserts.assertNotAssigned(will, "12345678-d34d-b33f-f00d-555555556666", RoleType.COMPLEX_TYPE, RelationTypes.MEMBER.getRelation());
        MidPointAsserts.assertNotAssigned(will, "12345678-d34d-b33f-f00d-555555556666", RoleType.COMPLEX_TYPE, RelationTypes.MANAGER.getRelation());
        MidPointAsserts.assertNotAssignedResource(will, "10000000-0000-0000-0000-000000000004");
    }

    @Test
    public void test394AssignToWill2() throws Exception {
        // GIVEN
        QName customRelation = new QName("http://midpoint.evolveum.com/xml/ns/samples/piracy", "captain");

        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(ASSIGN_TO_WILL_2_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> will = getUser(USER_WILL_OID);
        display("will after assignments creation", will);
        MidPointAsserts.assertAssigned(will, "12345678-d34d-b33f-f00d-555555556666", RoleType.COMPLEX_TYPE, customRelation);
    }

    @Test
    public void test400PurgeSchema() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(PURGE_DUMMY_BLACK_SCHEMA_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(1, output.getFinalOutput().getData().size());

        //AssertJUnit.assertNull("Schema is still present", dummy.getSchema());
        // actually, schema gets downloaded just after purging it
        assertEquals("Purged schema information from resource:10000000-0000-0000-0000-000000000305(Dummy Resource Black)\n", output.getConsoleOutput());
    }

    @Test
    public void test410TestResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(TEST_DUMMY_RESOURCE_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        ResourceType dummy = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, taskManager.createTaskInstance(), result).asObjectable();
        display("dummy resource after test connection", dummy.asPrismObject());
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(1, output.getFinalOutput().getData().size());
        assertEquals("Tested resource:10000000-0000-0000-0000-000000000004(Dummy Resource): SUCCESS\n", output.getConsoleOutput());
    }

    @Test
    public void test420NotificationAboutJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(NOTIFICATION_ABOUT_JACK_FILE);
        prepareNotifications();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        assertEquals("Produced 1 event(s)\n", output.getConsoleOutput());

        displayDumpable("Dummy transport", dummyTransport);
        checkDummyTransportMessages("Custom", 1);
        Message m = dummyTransport.getMessages("dummy:Custom").get(0);
        assertEquals("Wrong message body", "jack/" + USER_JACK_OID, m.getBody());
        assertEquals("Wrong message subject", "Ad hoc notification", m.getSubject());
    }

    @Test
    public void test430NotificationAboutJackType2() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(NOTIFICATION_ABOUT_JACK_TYPE2_FILE);
        prepareNotifications();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);
        assertEquals("Produced 1 event(s)\n", output.getConsoleOutput());

        displayDumpable("Dummy transport", dummyTransport);
        checkDummyTransportMessages("Custom", 1);
        Message m = dummyTransport.getMessages("dummy:Custom").get(0);
        assertEquals("Wrong message body", "1", m.getBody());
        assertEquals("Wrong message subject", "Ad hoc notification 2", m.getSubject());

        checkDummyTransportMessages("CustomType2", 1);
        m = dummyTransport.getMessages("dummy:CustomType2").get(0);
        assertEquals("Wrong message body", "POV:user:c0c010c0-d34d-b33f-f00d-111111111111(jack)", m.getBody());
        assertEquals("Wrong message subject", "Failure notification of type 2", m.getSubject());
    }

    @Test
    public void test500ScriptingUsers() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SCRIPTING_USERS_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PipelineData data = output.getFinalOutput();
        assertEquals("Unexpected # of items in output", 6, data.getData().size());
        Set<String> realOids = new HashSet<>();
        for (PipelineItem item : data.getData()) {
            PrismValue value = item.getValue();
            //noinspection unchecked
            UserType user = ((PrismObjectValue<UserType>) value).asObjectable();
            assertEquals("Description not set", "Test", user.getDescription());
            realOids.add(user.getOid());
            assertSuccess(item.getResult());
        }
        assertEquals("Unexpected OIDs in output",
                Sets.newHashSet(Arrays.asList(USER_ADMINISTRATOR_OID, USER_JACK_OID, USER_BARBOSSA_OID, USER_GUYBRUSH_OID, USER_ELAINE_OID, USER_WILL_OID)),
                realOids);
    }

    @Test
    public void test505ScriptingUsersInBackground() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        ExecuteScriptType exec = prismContext.parserFor(SCRIPTING_USERS_IN_BACKGROUND_FILE).parseRealValue();

        // WHEN

        task.setExtensionPropertyValue(SchemaConstants.SE_EXECUTE_SCRIPT, exec);
        task.getExtensionOrClone()
                .findOrCreateProperty(USER_NAME_TASK_EXTENSION_PROPERTY)
                .addRealValue(USER_ADMINISTRATOR_USERNAME);
        task.getExtensionOrClone()
                .findOrCreateProperty(USER_DESCRIPTION_TASK_EXTENSION_PROPERTY)
                .addRealValue("admin description");
        task.setHandlerUri(ModelPublicConstants.SCRIPT_EXECUTION_TASK_HANDLER_URI);

        dummyTransport.clearMessages();
        boolean notificationsDisabled = notificationManager.isDisabled();
        notificationManager.setDisabled(false);

        taskManager.switchToBackground(task, result);

        waitForTaskFinish(task.getOid(), false);
        task.refresh(result);

        // THEN
        display(task.getResult());
        TestUtil.assertSuccess(task.getResult());
        PrismObject<UserType> admin = getUser(USER_ADMINISTRATOR_OID);
        display("admin after operation", admin);
        assertEquals("Wrong description", "admin description", admin.asObjectable().getDescription());

        displayDumpable("dummy transport", dummyTransport);
        notificationManager.setDisabled(notificationsDisabled);

        assertEquals("Wrong # of messages in dummy transport", 1,
                emptyIfNull(dummyTransport.getMessages("dummy:simpleUserNotifier")).size());
    }

    @Test
    public void test507ScriptingUsersInBackgroundAssign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        ExecuteScriptType exec = prismContext.parserFor(SCRIPTING_USERS_IN_BACKGROUND_ASSIGN_FILE).parseRealValue();

        // WHEN

        task.setExtensionPropertyValue(SchemaConstants.SE_EXECUTE_SCRIPT, exec);
        task.setHandlerUri(ModelPublicConstants.SCRIPT_EXECUTION_TASK_HANDLER_URI);

        dummyTransport.clearMessages();
        boolean notificationsDisabled = notificationManager.isDisabled();
        notificationManager.setDisabled(false);

        taskManager.switchToBackground(task, result);

        waitForTaskFinish(task.getOid(), false);
        task.refresh(result);

        // THEN
        display(task.getResult());
        TestUtil.assertSuccess(task.getResult());
        PrismObject<UserType> admin = getUser(USER_ADMINISTRATOR_OID);
        display("admin after operation", admin);
        assertAssignedRole(admin, ROLE_EMPTY_OID);

        displayDumpable("dummy transport", dummyTransport);
        notificationManager.setDisabled(notificationsDisabled);

        assertEquals("Wrong # of messages in dummy transport", 1,
                emptyIfNull(dummyTransport.getMessages("dummy:simpleUserNotifier")).size());
    }

    @Test
    public void test510GeneratePasswords() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(GENERATE_PASSWORDS_FILE);

        addObject(PASSWORD_POLICY_GLOBAL_FILE);

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(SecurityPolicyType.class)
                .item(SecurityPolicyType.F_CREDENTIALS, CredentialsPolicyType.F_PASSWORD,
                        PasswordCredentialsPolicyType.F_VALUE_POLICY_REF)
                .add(itemFactory().createReferenceValue(PASSWORD_POLICY_GLOBAL_OID))
                .asItemDeltas();
        modifySystemObjectInRepo(SecurityPolicyType.class, SECURITY_POLICY_OID, itemDeltas, result);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PipelineData data = output.getFinalOutput();
        assertEquals("Unexpected # of items in output", 6, data.getData().size());
        Set<String> realOids = new HashSet<>();
        for (PipelineItem item : data.getData()) {
            PrismValue value = item.getValue();
            //noinspection unchecked
            UserType user = ((PrismObjectValue<UserType>) value).asObjectable();
            ProtectedStringType passwordValue = user.getCredentials().getPassword().getValue();
            assertNotNull("clearValue for password not set", passwordValue.getClearValue());
            realOids.add(user.getOid());
        }
        assertEquals("Unexpected OIDs in output",
                Sets.newHashSet(Arrays.asList(USER_ADMINISTRATOR_OID, USER_JACK_OID, USER_BARBOSSA_OID, USER_GUYBRUSH_OID, USER_ELAINE_OID, USER_WILL_OID)),
                realOids);
    }

    @Test
    public void test520GeneratePasswordsFullInput() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ExecuteScriptType executeScript = parseExecuteScript(GENERATE_PASSWORDS_2_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(executeScript, VariablesMap.emptyMap(), false, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        //TestUtil.assertSuccess(result);
        PipelineData data = output.getFinalOutput();
        List<PipelineItem> items = data.getData();
        assertEquals("Unexpected # of items in output", 4, items.size());
        assertSuccess(items.get(0).getResult());
        assertFailure(items.get(1).getResult());
        assertSuccess(items.get(2).getResult());
        assertSuccess(items.get(3).getResult());
    }

    @Test
    public void test530GeneratePasswordsReally() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ExecuteScriptType executeScript = parseExecuteScript(GENERATE_PASSWORDS_3_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(executeScript, VariablesMap.emptyMap(), false, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        PipelineData data = output.getFinalOutput();
        List<PipelineItem> items = data.getData();
        assertEquals("Unexpected # of items in output", 3, items.size());
        assertFailure(items.get(0).getResult());
        assertSuccess(items.get(1).getResult());
        assertSuccess(items.get(2).getResult());

        checkPassword(items.get(1), USER_GUYBRUSH_OID);
        checkPassword(items.get(2), USER_ELAINE_OID);
    }

    @SuppressWarnings("unchecked")
    private void checkPassword(PipelineItem item, String userOid)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, EncryptionException {
        PrismProperty<ProtectedStringType> returnedPassword = (PrismProperty<ProtectedStringType>)
                item.getValue().find(SchemaConstants.PATH_PASSWORD_VALUE);
        ProtectedStringType returnedRealValue = returnedPassword.getRealValue();
        PrismObject<UserType> user = getUser(userOid);
        ProtectedStringType repoRealValue = user.asObjectable().getCredentials().getPassword().getValue();
        String returnedClearValue = protector.decryptString(returnedRealValue);
        String repoClearValue = protector.decryptString(repoRealValue);
        System.out.println("Returned password = " + returnedClearValue + ", repo password = " + repoClearValue);
        assertEquals("Wrong password stored in repository", returnedClearValue, repoClearValue);
    }

    @Test
    public void test540SearchUserResolveNamesForRoleMembershipRef() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SEARCH_FOR_USERS_RESOLVE_NAMES_FOR_ROLE_MEMBERSHIP_REF_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(2, output.getFinalOutput().getData().size());
        //assertEquals("administrator", ((PrismObject<UserType>) output.getData().get(0)).asObjectable().getName().getOrig());

        for (PipelineItem item : output.getFinalOutput().getData()) {
            PrismAsserts.assertHasTargetName((PrismContainerValue<?>) item.getValue(), UserType.F_ROLE_MEMBERSHIP_REF);
            PrismAsserts.assertHasNoTargetName((PrismContainerValue<?>) item.getValue(), UserType.F_LINK_REF);
        }
    }

    @Test
    public void test545SearchUserResolveRoleMembershipRef() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ScriptingExpressionType expression = parseScriptingExpression(SEARCH_FOR_USERS_RESOLVE_ROLE_MEMBERSHIP_REF_FILE);

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(expression, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals(2, output.getFinalOutput().getData().size());

        for (PipelineItem item : output.getFinalOutput().getData()) {
            PrismAsserts.assertHasObject((PrismContainerValue<?>) item.getValue(), UserType.F_ROLE_MEMBERSHIP_REF);
            PrismAsserts.assertHasNoObject((PrismContainerValue<?>) item.getValue(), UserType.F_LINK_REF);
        }
    }

    @Test
    public void test550UseVariables() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ExecuteScriptType executeScript = parseExecuteScript(USE_VARIABLES_FILE);

        PrismContainer<? extends ExtensionType> taskExtension = task.getOrCreateExtension();
        taskExtension
                .findOrCreateProperty(USER_NAME_TASK_EXTENSION_PROPERTY)
                .addRealValue("user1");
        taskExtension
                .findOrCreateProperty(STUDY_GROUP_TASK_EXTENSION_PROPERTY)
                .addRealValues("group1", "group2", "group3");

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(executeScript, VariablesMap.emptyMap(), false, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        PipelineData data = output.getFinalOutput();
        assertEquals("Unexpected # of items in output", 1, data.getData().size());

        String returned = data.getData().get(0).getValue().getRealValue();
        assertEquals("Wrong returned status", "ok", returned);
    }

    @Test
    public void test560StartTaskFromTemplate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();
        repoAddObjectFromFile(SCRIPTING_USERS_IN_BACKGROUND_TASK_FILE, result);
        ExecuteScriptType exec = prismContext.parserFor(START_TASKS_FROM_TEMPLATE_FILE).parseRealValue();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(exec, VariablesMap.emptyMap(), false, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        PipelineData data = output.getFinalOutput();
        assertEquals("Unexpected # of items in output", 2, data.getData().size());

        String oid1 = ((PrismObjectValue<?>) data.getData().get(0).getValue()).getOid();
        String oid2 = ((PrismObjectValue<?>) data.getData().get(1).getValue()).getOid();

        waitForTaskCloseOrSuspend(oid1, 20000);
        waitForTaskCloseOrSuspend(oid2, 20000);

        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        PrismObject<UserType> administrator = getUser(USER_ADMINISTRATOR_OID);
        display("jack", jack);
        display("administrator", administrator);
        assertEquals("Wrong jack description", "new desc jack", jack.asObjectable().getDescription());
        assertEquals("Wrong administrator description", "new desc admin", administrator.asObjectable().getDescription());

        // cleaning up the tasks

        Thread.sleep(5000L);            // cleanup is set to 1 second after completion

        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);

        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, false);
        waitForTaskFinish(TASK_TRIGGER_SCANNER_OID, true);

        assertNoObject(TaskType.class, oid1, task, result);
        assertNoObject(TaskType.class, oid2, task, result);

        taskManager.suspendTasks(singleton(TASK_TRIGGER_SCANNER_OID), 10000L, result);
    }

    @Test
    public void test570IterativeScriptingTask() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        String taskOid = repoAddObjectFromFile(SCRIPTING_USERS_IN_BACKGROUND_ITERATIVE_TASK_FILE, result).getOid();

        // WHEN
        waitForTaskFinish(taskOid, false);

        // THEN
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        PrismObject<UserType> administrator = getUser(USER_ADMINISTRATOR_OID);
        display("jack", jack);
        display("administrator", administrator);
        assertEquals("Wrong jack description", "hello jack", jack.asObjectable().getDescription());
        assertEquals("Wrong administrator description", "hello administrator", administrator.asObjectable().getDescription());
    }

    @Test(enabled = false)      // probably obsolete
    public void test575ResumeTask() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = task.getResult();

        addObject(TASK_TO_KEEP_SUSPENDED_FILE);

        PrismObject<TaskType> taskToResume = prismContext.parseObject(TASK_TO_RESUME_FILE);
        //TODO deal with this
        //taskToResume.asObjectable().getApprovalContext().setEndTimestamp(fromNow(createDuration(-1000L)));
        addObject(taskToResume);
        display("task to resume", taskToResume);

        ExecuteScriptType exec = prismContext.parserFor(RESUME_SUSPENDED_TASKS_FILE).parseRealValue();

        // WHEN
        ExecutionContext output = scriptingExpressionEvaluator.evaluateExpression(exec, VariablesMap.emptyMap(), false, task, result);

        // THEN
        dumpOutput(output, result);
        result.computeStatus();
        // the task should be there
        assertEquals("Unexpected # of items in output", 1, output.getFinalOutput().getData().size());

        PrismObject<TaskType> taskAfter = getObject(TaskType.class, taskToResume.getOid());
        assertNotSame("Task is still suspended", taskAfter.asObjectable().getExecutionStatus(), TaskExecutionStatusType.SUSPENDED);
    }

    // MID-5359
    @Test
    public void test600ModifyJackPasswordInBackground() throws Exception {
        // GIVEN
        OperationResult result = getTestOperationResult();
        ScriptingExpressionType expression = parseScriptingExpression(MODIFY_JACK_PASSWORD_FILE);

        prepareNotifications();
        dummyAuditService.clear();

        // WHEN
        Task task = taskManager.createTaskInstance();
        task.setOwner(getUser(USER_ADMINISTRATOR_OID));
        scriptingExpressionEvaluator.evaluateExpressionInBackground(expression, task, result);
        waitForTaskFinish(task.getOid(), false);
        task.refresh(result);

        // THEN
        display(task.getResult());
        TestUtil.assertSuccess(task.getResult());
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("jack after password change", jack);
        assertEncryptedUserPassword(jack, PASSWORD_PLAINTEXT_1);

        String xml = prismContext.xmlSerializer().serialize(task.getUpdatedTaskObject());
        displayValue("task", xml);
        assertFalse("Plaintext password is present in the task", xml.contains(PASSWORD_PLAINTEXT_FRAGMENT));

        displayDumpable("Dummy transport", dummyTransport);
        displayDumpable("Audit", dummyAuditService);
    }

    // MID-5359
    @Test
    public void test610ModifyJackPasswordImportingTask() throws Exception {
        // GIVEN
        Task opTask = getTestTask();
        opTask.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = opTask.getResult();

        prepareNotifications();
        dummyAuditService.clear();

        // WHEN
        FileInputStream stream = new FileInputStream(MODIFY_JACK_PASSWORD_TASK_FILE);
        modelService.importObjectsFromStream(stream, PrismContext.LANG_XML, null, opTask, result);
        stream.close();

        result.computeStatus();
        assertSuccess(result);

        Task task = waitForTaskFinish(MODIFY_JACK_PASSWORD_TASK_OID, false);

        // THEN
        display(task.getResult());
        TestUtil.assertSuccess(task.getResult());
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("jack after password change", jack);
        assertEncryptedUserPassword(jack, PASSWORD_PLAINTEXT_2);

        String xml = prismContext.xmlSerializer().serialize(task.getUpdatedTaskObject());
        displayValue("task", xml);
        assertFalse("Plaintext password is present in the task", xml.contains(PASSWORD_PLAINTEXT_FRAGMENT));

        displayDumpable("Dummy transport", dummyTransport);
        displayDumpable("Audit", dummyAuditService);
    }

    // not using scripting as such, but related... MID-5359
    @Test
    public void test620ModifyJackPasswordViaExecuteChangesAsynchronously() throws Exception {
        // GIVEN
        Task opTask = getTestTask();
        opTask.setOwner(getUser(USER_ADMINISTRATOR_OID));
        OperationResult result = opTask.getResult();

        prepareNotifications();
        dummyAuditService.clear();

        // WHEN
        ProtectedStringType password = new ProtectedStringType();
        password.setClearValue(PASSWORD_PLAINTEXT_3);

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE)
                .replace(password)
                .asObjectDelta(USER_JACK_OID);
        TaskType newTask = libraryMidpointFunctions.executeChangesAsynchronously(singleton(delta), null, null, opTask, result);

        result.computeStatus();
        assertSuccess(result);

        Task task = waitForTaskFinish(newTask.getOid(), false);

        // THEN
        display(task.getResult());
        TestUtil.assertSuccess(task.getResult());
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("jack after password change", jack);
        assertEncryptedUserPassword(jack, PASSWORD_PLAINTEXT_3);

        String xml = prismContext.xmlSerializer().serialize(task.getUpdatedTaskObject());
        displayValue("task", xml);
        assertFalse("Plaintext password is present in the task", xml.contains(PASSWORD_PLAINTEXT_FRAGMENT));

        displayDumpable("Dummy transport", dummyTransport);
        displayDumpable("Audit", dummyAuditService);
    }

    private void assertNoOutputData(ExecutionContext output) {
        assertTrue("Script returned unexpected data", output.getFinalOutput() == null || output.getFinalOutput().getData().isEmpty());
    }

    @SuppressWarnings("SameParameterValue")
    private void assertOutputData(ExecutionContext output, int size, OperationResultStatus status) {
        assertEquals("Wrong # of output items", size, output.getFinalOutput().getData().size());
        for (PipelineItem item : output.getFinalOutput().getData()) {
            assertEquals("Wrong op result status", status, item.getResult().getStatus());
        }
    }

    // the following tests are a bit crude but for now it should be OK

    private void assertAttributesNotFetched(List<PipelineItem> data) {
        for (PipelineItem item : data) {
            PrismValue value = item.getValue();
            //noinspection unchecked
            if (((PrismObjectValue<ShadowType>) value).asObjectable().getAttributes().getAny().size() > 2) {
                throw new AssertionError("There are some unexpected attributes present in " + value.debugDump());
            }
        }
    }

    private void assertAttributesFetched(List<PipelineItem> data) {
        for (PipelineItem item : data) {
            PrismValue value = item.getValue();
            //noinspection unchecked
            if (((PrismObjectValue<ShadowType>) value).asObjectable().getAttributes().getAny().size() <= 2) {
                throw new AssertionError("There are no attributes present in " + value.debugDump());
            }
        }
    }

    private void dumpOutput(ExecutionContext output, OperationResult result) throws JAXBException, SchemaException {
        displayDumpable("output", output.getFinalOutput());
        displayValue("stdout", output.getConsoleOutput());
        display(result);
        if (output.getFinalOutput() != null) {
            PipelineDataType bean = ModelWebService.prepareXmlData(output.getFinalOutput().getData(), null);
            displayValue("output in XML", prismContext.xmlSerializer().root(new QName("output")).serializeRealValue(bean));
        }
    }

}
