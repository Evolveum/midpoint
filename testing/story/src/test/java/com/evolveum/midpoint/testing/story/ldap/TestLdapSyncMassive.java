/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.ldap;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Testing sync, with lot of sync cycles. The goal is to test thread pooling and memory
 * management related to sync (e.g. MID-5099)
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapSyncMassive extends AbstractLdapTest {

    public static final File TEST_DIR = new File(LDAP_TEST_DIR, "sync-massive");

    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    private static final File RESOURCE_OPENDJ_FILE_BAD = new File(TEST_DIR, "resource-opendj-bad.xml");

    private static final File TASK_LIVE_SYNC_FILE = new File(TEST_DIR, "task-live-sync.xml");
    private static final String TASK_LIVE_SYNC_OID = "eba4a816-2a05-11e9-9123-03a2334b9b4c";

    private static final File ACCOUNT_WILL_LDIF_FILE = new File(TEST_DIR, "will.ldif");
    private static final String ACCOUNT_WILL_LDAP_UID = "will";
    private static final String ACCOUNT_WILL_LDAP_CN = "Will Turner";

    private static final File ACCOUNT_KRAKEN_LDIF_FILE = new File(TEST_DIR, "kraken.ldif");

    private static final int THREAD_COUNT_TOLERANCE = 10;
    private static final int THREAD_COUNT_TOLERANCE_BIG = 20;

    private static final int SYNC_ADD_ATTEMPTS = 30;
    private static final int NUMBER_OF_GOBLINS = 50;

    private static final int NUMBER_OF_TEST_THREADS = 5;
    private static final Integer TEST_THREADS_RANDOM_START_RANGE = 10;
    private static final long PARALLEL_TEST_TIMEOUT = 60000L;

    /**
     * Hypothesis why we experience three (not only two) connector instances e.g. for test150AddGoblins:
     * 1. instance is for main thread when creating goblins
     * 2. instance is for some internal connector thread for live sync query (this thread seems to run asynchronously even after sync() returns)
     * 3. instance is for worker thread of LiveSync task when it's starting (it does so each second)
     */
    private static final int INSTANCES_MAX = 3;

    private Integer lastSyncToken;
    private int threadCountBaseline;

    private File getTestDir() {
        return TEST_DIR;
    }

    private File getResourceOpenDjFile() {
        return new File(getTestDir(), "resource-opendj.xml");
    }

    @Override
    protected String getLdapResourceOid() {
        return RESOURCE_OPENDJ_OID;
    }

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Resources
        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, getResourceOpenDjFile(), RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        assertLdapConnectorInstances(1);

        dumpLdap();
    }

    @Test
    public void test080ImportSyncTask() throws Exception {
        // WHEN
        when();

        importObjectFromFile(TASK_LIVE_SYNC_FILE);

        // THEN
        then();

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_OID, true);

        PrismObject<TaskType> syncTask = getTask(TASK_LIVE_SYNC_OID);
        lastSyncToken = ObjectTypeUtil.getExtensionItemRealValue(syncTask, SchemaConstants.SYNC_TOKEN);
        displayValue("Initial sync token", lastSyncToken);
        assertNotNull("Null sync token", lastSyncToken);

        assertLdapConnectorInstances(1);

        threadCountBaseline = Thread.activeCount();
        displayValue("Thread count baseline", threadCountBaseline);

        dumpLdap();
    }

    /**
     * Add a single LDAP account. This goal is to test whether we have good configuration.
     */
    @Test
    public void test110SyncAddWill() throws Exception {
        Entry entry = openDJController.addEntryFromLdifFile(ACCOUNT_WILL_LDIF_FILE);
        display("Entry from LDIF", entry);

        // WHEN
        when();

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_OID, true);

        // THEN
        then();

        assertSyncTokenIncrement(1);

        assertLdapConnectorInstances(1);

        assertUserAfterByUsername(ACCOUNT_WILL_LDAP_UID)
                .assertFullName(ACCOUNT_WILL_LDAP_CN);

        assertThreadCount();

        // just to make sure we are stable

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_OID, true);

        assertSyncTokenIncrement(0);
        assertLdapConnectorInstances(1);
        assertThreadCount();

        dumpLdap();

    }

    /**
     * "Good run". This is a run with more sync cycles, but without
     * any effort to trigger problems. This is here to make sure we
     * have the right "baseline", e.g. thread count tolerance.
     */
    @Test
    public void test112SyncAddGoods() throws Exception {
        // WHEN
        when();

        for (int i = 0; i < SYNC_ADD_ATTEMPTS; i++) {
            syncAddAttemptGood("good", i);
        }

        // THEN
        then();

        dumpLdap();

    }

    /**
     * Add "goblin" users, each with an LDAP account.
     * We do not really needs them now. But these will make
     * subsequent tests more massive.
     * Adding them in this way is much faster then adding
     * them in sync one by one.
     * And we need to add them while the resource still
     * works OK.
     */
    @Test
    public void test150AddGoblins() throws Exception {
        // WHEN
        when();

        for (int i = 0; i < NUMBER_OF_GOBLINS; i++) {
            String username = goblinUsername(i);
            PrismObject<UserType> goblin = createUser(username, "Goblin", Integer.toString(i), true);
            goblin.asObjectable().
                    beginAssignment()
                    .beginConstruction()
                    .resourceRef(RESOURCE_OPENDJ_OID, ResourceType.COMPLEX_TYPE);
            addObject(goblin);
        }

        // THEN
        then();

        dumpLdap();
        assertLdapConnectorInstances(1, INSTANCES_MAX);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_OID, true);

        assertLdapConnectorInstances(1, INSTANCES_MAX);
        assertSyncTokenIncrement(NUMBER_OF_GOBLINS);
        assertThreadCount();

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_OID, true);

        assertLdapConnectorInstances(1, INSTANCES_MAX);
        assertSyncTokenIncrement(0);
        assertThreadCount();

    }

    private String goblinUsername(int i) {
        return String.format("goblin%05d", i);
    }

    /**
     * Overwrite the resource with a bad configuration.
     * Now we are going to make some trouble.
     */
    @Test
    public void test200SyncAddKraken() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ImportOptionsType options = new ImportOptionsType()
                .overwrite(true);
        importObjectFromFile(RESOURCE_OPENDJ_FILE_BAD, options, task, result);

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        display("Test resource result", testResultOpenDj);
        TestUtil.assertSuccess(testResultOpenDj);

        PrismObject<ResourceType> resourceAfter = modelService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, task, result);
        assertResource(resourceAfter, "after")
                .assertHasSchema();

        assertLdapConnectorInstances(1, INSTANCES_MAX);
    }

    /**
     * Just make first attempt with bad configuration.
     * This is here mostly to make sure we really have a bad configuration.
     */
    @Test
    public void test210SyncAddKraken() throws Exception {
        Entry entry = openDJController.addEntryFromLdifFile(ACCOUNT_KRAKEN_LDIF_FILE);
        display("Entry from LDIF", entry);

        // WHEN
        when();

        OperationResult taskResult = waitForTaskNextRun(TASK_LIVE_SYNC_OID);

        // THEN
        then();
        assertPartialError(taskResult);

        assertSyncTokenIncrement(0);
        assertLdapConnectorInstances(1, INSTANCES_MAX);
        assertThreadCount();

        // just to make sure we are stable
        // in fact, it is "FUBAR, but stable"

        taskResult = waitForTaskNextRun(TASK_LIVE_SYNC_OID);
        assertPartialError(taskResult);

        assertSyncTokenIncrement(0);
        assertLdapConnectorInstances(1, INSTANCES_MAX);
        assertThreadCount();

        dumpLdap();

    }

    /**
     * "Bad run".
     * MID-5099: cannot reproduce
     */
    @Test
    public void test212SyncAddBads() throws Exception {
        // WHEN
        when();

        for (int i = 0; i < SYNC_ADD_ATTEMPTS; i++) {
            syncAddAttemptBad("bad", i);
        }

        // THEN
        then();

        dumpLdap();

    }

    /**
     * Suspend sync task. We do not want that to mess the results of subsequent
     * tests (e.g. mess the number of connector instances).
     */
    @Test
    public void test219StopSyncTask() throws Exception {
        // WHEN
        when();

        suspendTask(TASK_LIVE_SYNC_OID);

        // THEN
        then();

        assertSyncTokenIncrement(0);
        assertLdapConnectorInstances(1, INSTANCES_MAX);
        assertThreadCount();

    }

    // this runs for ~20+ minutes
    @Test
    public void test230UserRecomputeSequential() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        SearchResultList<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);

        // WHEN
        when();

        for (PrismObject<UserType> user : users) {
            reconcile(user);
        }

        // THEN
        then();

        assertLdapConnectorInstances(1, INSTANCES_MAX);
        assertThreadCount();
    }

    @Test
    public void test232UserRecomputeParallel() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        SearchResultList<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);

        // WHEN
        when();

        int segmentSize = users.size() / NUMBER_OF_TEST_THREADS;
        ParallelTestThread[] threads = multithread(
                (threadIndex) -> {
                    for (int i = segmentSize * threadIndex; i < segmentSize * threadIndex + segmentSize; i++) {
                        PrismObject<UserType> user = users.get(i);
                        reconcile(user);
                    }

                }, NUMBER_OF_TEST_THREADS, TEST_THREADS_RANDOM_START_RANGE);

        // THEN
        then();
        waitForThreads(threads, PARALLEL_TEST_TIMEOUT);

        // When system is put under load, this means more threads. But not huge number of threads.
        assertThreadCount(THREAD_COUNT_TOLERANCE_BIG);
        assertLdapConnectorInstances(1, NUMBER_OF_TEST_THREADS);
    }

    private void reconcile(PrismObject<UserType> user)
            throws SchemaException, ObjectAlreadyExistsException, ExpressionEvaluationException,
            PolicyViolationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        user.getName();
        Task task = createTask("user." + user.getName());
        OperationResult result = task.getResult();

        reconcileUser(user.getOid(), task, result);

        // We do not bother to check result. Even though the
        // timeout is small, the operation may succeed occasionally.
        // This annoying success cout cause the tests to fail.
    }

    private void syncAddAttemptGood(String prefix, int index) throws Exception {

        String uid = String.format("%s%05d", prefix, index);
        String cn = prefix + " " + index;
        addAttemptEntry(uid, cn, Integer.toString(index));

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_OID, true);

        assertSyncTokenIncrement(1);

        assertUserAfterByUsername(uid)
                .assertFullName(cn);

        assertThreadCount();
    }

    private void syncAddAttemptBad(String prefix, int index) throws Exception {

        String uid = String.format("%s%05d", prefix, index);
        String cn = prefix + " " + index;
        addAttemptEntry(uid, cn, Integer.toString(index));

        OperationResult taskResult = waitForTaskNextRun(TASK_LIVE_SYNC_OID);

        assertPartialError(taskResult);
        assertSyncTokenIncrement(0);
        assertLdapConnectorInstances(1);
        assertThreadCount();
    }

    private void addAttemptEntry(String uid, String cn, String sn) throws Exception {
        Entry entry = openDJController.addEntry(
                "dn: uid=" + uid + ",ou=People,dc=example,dc=com\n" +
                        "uid: " + uid + "\n" +
                        "cn: " + cn + "\n" +
                        "sn: " + sn + "\n" +
                        "givenname: " + uid + "\n" +
                        "objectclass: top\n" +
                        "objectclass: person\n" +
                        "objectclass: organizationalPerson\n" +
                        "objectclass: inetOrgPerson"
        );
        display("Added generated entry", entry);
    }

    private void assertThreadCount() {
        assertThreadCount(THREAD_COUNT_TOLERANCE);
    }

    private void assertThreadCount(int tolerance) {
        int currentThreadCount = Thread.activeCount();
        if (!isWithinTolerance(threadCountBaseline, currentThreadCount, tolerance)) {
            fail("Thread count out of tolerance: " + currentThreadCount + " (" + (currentThreadCount - threadCountBaseline) + ")");
        }
    }

    private boolean isWithinTolerance(int baseline, int currentCount, int tolerance) {
        return currentCount <= baseline + tolerance
                && currentCount >= baseline - tolerance;
    }

    private void assertSyncTokenIncrement(int expectedIncrement) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<TaskType> syncTask = getTask(TASK_LIVE_SYNC_OID);
        Integer currentSyncToken = ObjectTypeUtil.getExtensionItemRealValue(syncTask, SchemaConstants.SYNC_TOKEN);
        display("Sync token, last=" + lastSyncToken + ", current=" + currentSyncToken + ", expectedIncrement=" + expectedIncrement);
        if (currentSyncToken != lastSyncToken + expectedIncrement) {
            fail("Expected sync token increment " + expectedIncrement + ", but it was " + (currentSyncToken - lastSyncToken));
        }
        lastSyncToken = currentSyncToken;
    }

    @Override
    protected void dumpLdap() throws DirectoryException {
        displayValue("LDAP server tree", openDJController.dumpTree());
    }
}
