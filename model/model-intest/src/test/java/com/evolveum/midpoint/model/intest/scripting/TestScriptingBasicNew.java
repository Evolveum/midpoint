/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.scripting;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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

    private static final TestObject<TaskType> TASK_DELETE_SHADOWS_MULTINODE = TestObject.file(TEST_DIR, "task-delete-shadows-multinode.xml", "931e34be-5cf0-46c6-8cc1-90812a66d5cb");
    private static final TestObject<TaskType> TASK_MIGRATE_PLAINTEXT_PASSWORD_HINTS = TestObject.file(TEST_DIR, "task-migrate-plaintext-password-hints.xml", "98efdb82-1470-4636-b1d0-121110000001");

    private static final String PASSWORD_HINT_MIGRATION_TEST_USER_NAME = "test905-migrate-password-hint-clear";
    private static final String PASSWORD_HINT_MIGRATION_SKIPPED_USER_NAME = "test905-migrate-password-hint-encrypted";
    private static final String PASSWORD_HINT_MIGRATION_CLEAR_VALUE = "plain text hint";
    private static final String PASSWORD_HINT_MIGRATION_UNRELATED_VALUE = "unchanged password value";
    private static final String LEGACY_PASSWORD_HINT_USER_NAME_PREFIX = "legacy-password-hint-";

    @Override
    String getSuffix() {
        return "";
    }

    @Override
    boolean isNew() {
        return true;
    }

    @Test
    public void test352RecomputeJackTriggerDirect() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ScriptingExpressionType expression = parseScriptingExpression(RECOMPUTE_JACK_NEW_TRIGGER_DIRECT_FILE);

        when();
        evaluateExpression(expression, task, result);
        Thread.sleep(20);
        evaluateExpression(expression, task, result);
        Thread.sleep(20);
        ExecutionContext output = evaluateExpression(expression, task, result);

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
        evaluateExpression(expression, task, result);
        Thread.sleep(20);
        evaluateExpression(expression, task, result);
        Thread.sleep(20);
        ExecutionContext output = evaluateExpression(expression, task, result);

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
        ExecutionContext output = evaluateExpression(expression, task, result);

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
        ExecutionContext output = evaluateExpression(expression, task, result);

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
        ExecutionContext output = evaluateExpression(expression, task, result);

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
        ExecutionContext output = evaluateExpression(executeScript, task, result);

        then();
        dumpOutput(output, result);
        assertOutputData(output, 1, OperationResultStatus.SUCCESS);

        assertSuccess(result);
        assertUserAfterByUsername(USER_JACK_USERNAME)
                .assertAssignments(1)
                .assignments()
                    .assertRole(ROLE_SUPERUSER.oid);
    }

    /**
     * Verifies that a legacy clear-text password hint is left untouched (and does not break the operation)
     * when an unrelated modification is executed on the user.
     */
    @Test
    public void test901LegacyPasswordHintUntouchedOnUnrelatedModification() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ProtectedStringType passwordValue = protector.encryptString(PASSWORD_HINT_MIGRATION_UNRELATED_VALUE);

        String userOid = addPasswordHintUser(
                LEGACY_PASSWORD_HINT_USER_NAME_PREFIX + "unrelated-modification",
                ProtectedStringType.fromClearValue(PASSWORD_HINT_MIGRATION_CLEAR_VALUE),
                passwordValue.clone(),
                RepoAddOptions.createAllowUnencryptedValues(),
                result);

        when();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_DESCRIPTION)
                        .replace("unrelated edit")
                        .asObjectDelta(userOid),
                null,
                task,
                result);

        then();
        UserType userAfter = getUser(userOid).asObjectable();

        assertThat(userAfter.getDescription()).isEqualTo("unrelated edit");

        ProtectedStringType hintAfter = userAfter.getCredentials().getPassword().getHint();

        assertThat(hintAfter.getClearValue()).isEqualTo(PASSWORD_HINT_MIGRATION_CLEAR_VALUE);
        assertThat(hintAfter.isEncrypted()).isFalse();

        ProtectedStringType passwordValueAfter = userAfter.getCredentials().getPassword().getValue();

        assertThat(passwordValueAfter.getEncryptedDataType()).isEqualTo(passwordValue.getEncryptedDataType());
    }

    /**
     * Verifies that an explicitly modified password hint is encrypted by the normal model encryption path.
     */
    @Test
    public void test902DirectPasswordHintModification() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String userOid = addPasswordHintUser(
                LEGACY_PASSWORD_HINT_USER_NAME_PREFIX + "direct-edit",
                ProtectedStringType.fromClearValue("old hint"),
                null,
                RepoAddOptions.createAllowUnencryptedValues(),
                result);

        when();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(
                                UserType.F_CREDENTIALS,
                                CredentialsType.F_PASSWORD,
                                PasswordType.F_HINT)
                        .replace(ProtectedStringType.fromClearValue("new hint"))
                        .asObjectDelta(userOid),
                null,
                task,
                result);

        then();
        ProtectedStringType hintAfter = getUser(userOid)
                .asObjectable()
                .getCredentials()
                .getPassword()
                .getHint();

        assertThat(hintAfter.getClearValue()).isNull();
        assertThat(hintAfter.isEncrypted()).isTrue();
        assertThat(protector.decryptString(hintAfter)).isEqualTo("new hint");
    }

    /**
     * Verifies that an already encrypted password hint is not modified during
     * an unrelated user modification.
     */
    @Test
    public void test903AlreadyEncryptedPasswordHintIsUnchanged() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ProtectedStringType encryptedHint = protector.encryptString(PASSWORD_HINT_MIGRATION_CLEAR_VALUE);

        String userOid = addPasswordHintUser(
                LEGACY_PASSWORD_HINT_USER_NAME_PREFIX + "already-encrypted",
                encryptedHint.clone(),
                null,
                null,
                result);

        when();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_DESCRIPTION)
                        .replace("unrelated edit")
                        .asObjectDelta(userOid),
                null,
                task,
                result);

        then();
        ProtectedStringType hintAfter = getUser(userOid)
                .asObjectable()
                .getCredentials()
                .getPassword()
                .getHint();

        assertThat(hintAfter.getEncryptedDataType()).isEqualTo(encryptedHint.getEncryptedDataType());
    }

    /**
     * Verifies that an operation with noCrypt works on a user with a legacy clear-text password hint
     * and leaves the hint untouched.
     */
    @Test
    public void test904NoCryptLeavesLegacyPasswordHintUntouched() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String userOid = addPasswordHintUser(
                LEGACY_PASSWORD_HINT_USER_NAME_PREFIX + "no-crypt",
                ProtectedStringType.fromClearValue(PASSWORD_HINT_MIGRATION_CLEAR_VALUE),
                null,
                RepoAddOptions.createAllowUnencryptedValues(),
                result);

        when();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_DESCRIPTION)
                        .replace("unrelated edit")
                        .asObjectDelta(userOid),
                ModelExecuteOptions.create().noCrypt(true),
                task,
                result);

        then();

        // Read directly from repository because the hint is intentionally left unencrypted.
        ProtectedStringType hintAfter = repositoryService
                .getObject(UserType.class, userOid, null, result)
                .asObjectable()
                .getCredentials()
                .getPassword()
                .getHint();

        assertThat(hintAfter.getClearValue()).isEqualTo(PASSWORD_HINT_MIGRATION_CLEAR_VALUE);
        assertThat(hintAfter.isEncrypted()).isFalse();
    }

    /**
     * Verifies that a legacy clear-text password hint is left untouched by a recompute, which starts
     * with a focus object set directly into the context, without loading it through the context loader.
     */
    @Test
    public void test906LegacyPasswordHintUntouchedOnRecompute() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String userOid = addPasswordHintUser(
                LEGACY_PASSWORD_HINT_USER_NAME_PREFIX + "recompute",
                ProtectedStringType.fromClearValue(PASSWORD_HINT_MIGRATION_CLEAR_VALUE),
                null,
                RepoAddOptions.createAllowUnencryptedValues(),
                result);

        when();
        recomputeUser(userOid, task, result);

        then();
        ProtectedStringType hintAfter = getUser(userOid)
                .asObjectable()
                .getCredentials()
                .getPassword()
                .getHint();

        assertThat(hintAfter.getClearValue()).isEqualTo(PASSWORD_HINT_MIGRATION_CLEAR_VALUE);
        assertThat(hintAfter.isEncrypted()).isFalse();
    }

    /**
     * Verifies that the password-hint migration encrypts only legacy clear-text hints,
     * leaves already encrypted and unrelated protected values unchanged, and is idempotent.
     */
    @Test
    public void test905MigrateLegacyPlaintextPasswordHints() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ProtectedStringType unrelatedValue =
                protector.encryptString(PASSWORD_HINT_MIGRATION_UNRELATED_VALUE);

        var userWithClearHint = new UserType()
                .name(PASSWORD_HINT_MIGRATION_TEST_USER_NAME)
                .credentials(new CredentialsType()
                        .password(new PasswordType()
                                .value(unrelatedValue.clone())
                                .hint(ProtectedStringType.fromClearValue(
                                        PASSWORD_HINT_MIGRATION_CLEAR_VALUE))));

        String userWithClearHintOid = repositoryService.addObject(
                userWithClearHint.asPrismObject(),
                RepoAddOptions.createAllowUnencryptedValues(),
                result);

        ProtectedStringType encryptedHint =
                protector.encryptString(PASSWORD_HINT_MIGRATION_CLEAR_VALUE);

        var userWithEncryptedHint = new UserType()
                .name(PASSWORD_HINT_MIGRATION_SKIPPED_USER_NAME)
                .credentials(new CredentialsType()
                        .password(new PasswordType()
                                .hint(encryptedHint.clone())));

        String userWithEncryptedHintOid = repositoryService.addObject(
                userWithEncryptedHint.asPrismObject(),
                null,
                result);

        // Read the intentionally unencrypted legacy object directly from repository.
        PrismObject<UserType> userBeforeMigration = repositoryService.getObject(
                UserType.class,
                userWithClearHintOid,
                null,
                result);

        ProtectedStringType unrelatedValueBefore = userBeforeMigration
                .asObjectable()
                .getCredentials()
                .getPassword()
                .getValue()
                .clone();

        ProtectedStringType encryptedHintBefore = getUser(userWithEncryptedHintOid)
                .asObjectable()
                .getCredentials()
                .getPassword()
                .getHint()
                .clone();

        when();
        addObject(TASK_MIGRATE_PLAINTEXT_PASSWORD_HINTS, task, result);
        Task taskAfterFirstRun =
                waitForTaskFinish(TASK_MIGRATE_PLAINTEXT_PASSWORD_HINTS.oid);

        then();
        assertTask(taskAfterFirstRun, "after first run")
                .assertSuccess()
                .assertClosed();

        var migratedUser = getUser(userWithClearHintOid).asObjectable();
        ProtectedStringType migratedHint = migratedUser.getCredentials().getPassword().getHint();

        assertThat(migratedHint.getClearValue()).isNull();
        assertThat(migratedHint.isEncrypted()).isTrue();
        assertThat(protector.decryptString(migratedHint))
                .isEqualTo(PASSWORD_HINT_MIGRATION_CLEAR_VALUE);

        ProtectedStringType unrelatedValueAfter = migratedUser.getCredentials().getPassword().getValue();

        assertThat(unrelatedValueAfter.getClearValue()).isNull();
        assertThat(unrelatedValueAfter.getEncryptedDataType())
                .isEqualTo(unrelatedValueBefore.getEncryptedDataType());
        assertThat(protector.decryptString(unrelatedValueAfter))
                .isEqualTo(PASSWORD_HINT_MIGRATION_UNRELATED_VALUE);

        ProtectedStringType skippedHintAfterFirstRun = getUser(userWithEncryptedHintOid)
                .asObjectable()
                .getCredentials()
                .getPassword()
                .getHint();

        assertThat(skippedHintAfterFirstRun.getEncryptedDataType())
                .isEqualTo(encryptedHintBefore.getEncryptedDataType());

        when("second run");
        Task taskAfterSecondRun = rerunTask(TASK_MIGRATE_PLAINTEXT_PASSWORD_HINTS.oid, result);

        then("second run");
        assertTask(taskAfterSecondRun, "after second run")
                .assertSuccess()
                .assertClosed();

        ProtectedStringType migratedHintAfterSecondRun = getUser(userWithClearHintOid)
                .asObjectable()
                .getCredentials()
                .getPassword()
                .getHint();

        assertThat(migratedHintAfterSecondRun.getEncryptedDataType())
                .isEqualTo(migratedHint.getEncryptedDataType());
        assertThat(protector.decryptString(migratedHintAfterSecondRun))
                .isEqualTo(PASSWORD_HINT_MIGRATION_CLEAR_VALUE);
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

    private String addPasswordHintUser(String name, ProtectedStringType hint, ProtectedStringType passwordValue,
            RepoAddOptions options, OperationResult result) throws Exception {

        var password = new PasswordType().hint(hint);

        if (passwordValue != null) {
            password.value(passwordValue);
        }

        var user = new UserType()
                .name(name)
                .credentials(new CredentialsType()
                        .password(password));

        return repositoryService.addObject(
                user.asPrismObject(),
                options,
                result);
    }
}
