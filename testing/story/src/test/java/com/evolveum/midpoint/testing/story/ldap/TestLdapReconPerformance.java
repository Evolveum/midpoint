/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.ldap;


import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.util.LDIFException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Reconciliation performance tests.
 *
 * We want a resource that is quite real. E.g. it needs to have quite a big schema, real
 * initialization costs and so on.
 *
 * MID-5284
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapReconPerformance extends AbstractLdapTest {

    public static final File TEST_DIR = new File(LDAP_TEST_DIR, "recon-perf");

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
    private static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

    private static final File TASK_RECON_1_OPENDJ_FILE = new File(TEST_DIR, "task-reconcile-1-opendj.xml");
    private static final String TASK_RECON_1_OPENDJ_OID = "a62c53e2-6830-11e9-8592-ef14755a7258";

    private static final File TASK_RECON_4_OPENDJ_FILE = new File(TEST_DIR, "task-reconcile-4-opendj.xml");
    private static final String TASK_RECON_4_OPENDJ_OID = "b3a2fae2-6805-11e9-825d-27f67acbabae";

    protected static final int NUMBER_OF_GENERATED_USERS = 100;
    protected static final String GENERATED_USER_NAME_FORMAT = "u%06d";
    protected static final String GENERATED_USER_FULL_NAME_FORMAT = "Random J. U%06d";
    protected static final String GENERATED_USER_GIVEN_NAME_FORMAT = "Random";
    protected static final String GENERATED_USER_FAMILY_NAME_FORMAT = "U%06d";
    protected static final String GENERATED_USER_OID_FORMAT = "00000000-0000-ffff-1000-000000%06d";

    private static final String SUMMARY_LINE_FORMAT = "%20s: %5dms (%4dms/object)\n";

    private static final int RECON_TASK_WAIT_TIMEOUT = 60000;

    private PrismObject<ResourceType> resourceOpenDj;

    private Map<String,Long> durations = new LinkedHashMap<>();

    private long reconDuration1ThreadBaseline;
    private long reconDuration4ThreadBaseline;

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServerRI();
    }

    @AfterClass
    public static void stopResources() throws Exception {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Resources
        resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

//        InternalMonitor.setTrace(InternalOperationClasses.CONNECTOR_OPERATIONS, true);
    }

    @Override
    protected String getLdapResourceOid() {
        return RESOURCE_OPENDJ_OID;
    }

    @Test
    public void test010GenerateUsers() throws Exception {
        final String TEST_NAME = "test010GenerateUsers";

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        long startMillis = System.currentTimeMillis();

        // WHEN
        displayWhen(TEST_NAME);
        // Add objects using model, we also want to create LDAP accounts
         generateObjects(UserType.class, NUMBER_OF_GENERATED_USERS, GENERATED_USER_NAME_FORMAT, GENERATED_USER_OID_FORMAT,
                     (user,i) -> {
                         user
                             .fullName(String.format(GENERATED_USER_FULL_NAME_FORMAT, i))
                             .givenName(String.format(GENERATED_USER_GIVEN_NAME_FORMAT, i))
                             .familyName(String.format(GENERATED_USER_FAMILY_NAME_FORMAT, i))
                             .beginAssignment()
                                 .beginConstruction()
                                     .resourceRef(RESOURCE_OPENDJ_OID, ResourceType.COMPLEX_TYPE);
                     },
                     user -> addObject(user, task, result),
                     result);

         // THEN
         displayThen(TEST_NAME);

         long endMillis = System.currentTimeMillis();
         recordDuration(TEST_NAME, (endMillis - startMillis));

         assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);

        dumpLdap();
        assertLdapAccounts();
        assertLdapConnectorInstances(1);
    }


    /**
     * No changes for recon to fix. Single-threaded recon.
     */
    @Test
    public void test100Reconcile1ThreadLdap0() throws Exception {
        final String TEST_NAME = "test100Reconcile1ThreadLdap0";

        rememberConnectorResourceCounters();

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        addTask(TASK_RECON_1_OPENDJ_FILE);
        waitForTaskFinish(TASK_RECON_1_OPENDJ_OID, true, RECON_TASK_WAIT_TIMEOUT);

         // THEN
         displayThen(TEST_NAME);

         recordDuration(TEST_NAME,getRunDurationMillis(TASK_RECON_1_OPENDJ_OID));

         assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);

        assertLdapAccounts();
        assertLdapConnectorInstances(1);

        assertSteadyResource();
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);
    }

    @Test
    public void test110Reconcile1ThreadLdap1() throws Exception {
        reconDuration1ThreadBaseline = testReconcileLdapRestart1Thread("test110Reconcile1ThreadLdap1");
    }

    @Test
    public void test120Reconcile1ThreadLdap2() throws Exception {
        testReconcileLdapRestart1Thread("test110Reconcile1ThreadLdap2");
    }

    /**
     * No changes for recon to fix. Recon in 4 threads.
     */
    @Test
    public void test200ReconcileLdap0() throws Exception {
        final String TEST_NAME = "test200ReconcileLdap0";

        rememberConnectorResourceCounters();

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        addTask(TASK_RECON_4_OPENDJ_FILE);
        waitForTaskFinish(TASK_RECON_4_OPENDJ_OID, true, RECON_TASK_WAIT_TIMEOUT);

         // THEN
         displayThen(TEST_NAME);

         recordDuration(TEST_NAME,getRunDurationMillis(TASK_RECON_4_OPENDJ_OID));

         assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);

        assertLdapAccounts();
        assertLdapConnectorInstances(1);

        assertSteadyResource();
    }

    @Test
    public void test210ReconcileLdap1() throws Exception {
        reconDuration4ThreadBaseline = testReconcileLdapRestart("test210ReconcileLdap1");
    }

    @Test
    public void test220ReconcileLdap2() throws Exception {
        testReconcileLdapRestart("test220ReconcileLdap2");
    }

    @Test
    public void test230ReconcileLdap3() throws Exception {
        testReconcileLdapRestart("test230ReconcileLdap3");
    }

    @Test
    public void test310ReconcileLdapX1() throws Exception {
        final String TEST_NAME = "test310ReconcileLdapX1";

        Task task = taskManager.createTaskInstance(TestLdapReconPerformance.class.getName() + "." + TEST_NAME);
        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);
        display("Test connection result", testResultOpenDj);

        testReconcileLdapRestartWhen(TEST_NAME, TASK_RECON_4_OPENDJ_OID);
    }

    @Test
    public void test320ReconcileLdapX2() throws Exception {
        testReconcileLdapRestart("test320ReconcileLdapX2");
    }

    @Test
    public void test330ReconcileLdapX3() throws Exception {
        testReconcileLdapRestart("test330ReconcileLdapX3");
    }

    private long testReconcileLdapRestart1Thread(final String TEST_NAME) throws Exception {

        long duration = testReconcileLdapRestartWhen(TEST_NAME, TASK_RECON_1_OPENDJ_OID);

        assertLdapConnectorInstances(1);

        return duration;
    }

    private long testReconcileLdapRestart(final String TEST_NAME) throws Exception {

        long duration =  testReconcileLdapRestartWhen(TEST_NAME, TASK_RECON_4_OPENDJ_OID);

        assertLdapConnectorInstances();

        return duration;
    }

    private long testReconcileLdapRestartWhen(final String TEST_NAME, String taskOid) throws Exception {
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        ruinLdapAccounts();
        rememberConnectorResourceCounters();

        // WHEN
        displayWhen(TEST_NAME);

        restartTask(taskOid);
        waitForTaskFinish(taskOid, true, RECON_TASK_WAIT_TIMEOUT);

         // THEN
         displayThen(TEST_NAME);

         long duration = recordDuration(TEST_NAME, getRunDurationMillis(taskOid));

         assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);
        assertLdapAccounts();
        assertSteadyResource();
        // Re-reading modified account after the modificaiton (because context is not fresh), hence 2*NUMBER_OF_GENERATED_USERS
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1 + 1 + 2*NUMBER_OF_GENERATED_USERS);
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, NUMBER_OF_GENERATED_USERS);

        return duration;
    }

    @Test
    public void test900Summarize() throws Exception {
        final String TEST_NAME = "test900Summarize";

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entry : durations.entrySet()) {
            sb.append(summary(entry.getKey(), entry.getValue()));
        }
        display("Summary ("+NUMBER_OF_GENERATED_USERS+" users)", sb.toString());

         // THEN
         displayThen(TEST_NAME);

         if (reconDuration1ThreadBaseline < reconDuration4ThreadBaseline) {
             fail("Multi-thread recon SLOWER than single-thread! singlethread="+reconDuration1ThreadBaseline+"ms, multithread="+reconDuration4ThreadBaseline+"ms");
         }

         // TODO: more threasholds

    }

    private void rememberConnectorResourceCounters() {
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);
    }

    private void assertSteadyResource() {
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 0);
    }

    private void ruinLdapAccounts() throws DirectoryException, LDIFException, IOException {
        for (Entry entry : openDJController.search("objectclass="+OBJECTCLASS_INETORGPERSON)) {
            String cn = openDJController.getAttributeValue(entry, "cn");
            if (cn.startsWith("Random")) {
                cn = cn.replace("Random", "Broken");
                openDJController.modifyReplace(entry.getDN().toString(), "cn", cn);
//                display("Replaced", openDJController.fetchEntry(entry.getDN().toString()));
//            } else {
//                display("NOT RANDOM: "+cn, entry);
            }
        }
        dumpLdap();
    }

    protected void assertLdapAccounts() throws DirectoryException {
        List<? extends Entry> entries = openDJController.search("objectclass="+OBJECTCLASS_INETORGPERSON);
        int randoms = 0;
        for (Entry entry : openDJController.search("objectclass="+OBJECTCLASS_INETORGPERSON)) {
            String cn = openDJController.getAttributeValue(entry, "cn");
            if (cn.startsWith("Broken")) {
                fail("Broken LDAP account: "+entry);
            }
            if (cn.startsWith("Random")) {
                randoms++;
            }
        }
        assertEquals("Wrong number of Random LDAP accounts", NUMBER_OF_GENERATED_USERS, randoms);
    }

    protected void assertLdapConnectorInstances() throws NumberFormatException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, IOException, InterruptedException {
        assertLdapConnectorInstances(2,4);
    }

    private long recordDuration(String label, long duration) {
        durations.put(label, duration);
        return duration;
    }

    private Object summary(String label, long duration) {
        return String.format(SUMMARY_LINE_FORMAT, label, duration, duration / NUMBER_OF_GENERATED_USERS);
    }
}
