/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Objects;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Test of Model Audit Service.
 * <p>
 * Two users, interlaced events: jack and herman.
 * <p>
 * Jack already exists, but there is no create audit record for him. This simulates trimmed
 * audit log. There are several fresh modify operations.
 * <p>
 * Herman is properly created and modified. We have all the audit records.
 * <p>
 * The tests check if audit records are created and that they can be listed.
 * The other tests check that the state can be reconstructed by the time machine (object history).
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAudit extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/audit");

    public static final int INITIAL_NUMBER_OF_AUDIT_RECORDS = 26;

    private XMLGregorianCalendar initialTs;
    private XMLGregorianCalendar jackKidTs;
    private String jackKidEid;
    private XMLGregorianCalendar jackSailorTs;
    private String jackSailorEid;
    private XMLGregorianCalendar jackCaptainTs;
    private String jackCaptainEid;
    private XMLGregorianCalendar hermanInitialTs;
    private XMLGregorianCalendar hermanCreatedTs;
    private String hermanCreatedEid;
    private XMLGregorianCalendar hermanMaroonedTs;
    private String hermanMaroonedEid;
    private XMLGregorianCalendar hermanHermitTs;
    private String hermanHermitEid;
    private XMLGregorianCalendar hermanCivilisedHermitTs;
    private String hermanCivilisedHermitEid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        assertTrue(modelAuditService.supportsRetrieval());

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        List<AuditEventRecord> allRecords = modelAuditService.listRecords("select * from m_audit_event as aer where 1=1 ",
                new HashMap<>(), task, result);

        // THEN
        display("all records", allRecords);

        assertEquals("Wrong initial number of audit records", INITIAL_NUMBER_OF_AUDIT_RECORDS, allRecords.size());
    }

    @Test
    public void test010SanityJack() throws Exception {
        // WHEN
        List<AuditEventRecord> auditRecords = getObjectAuditRecords(USER_JACK_OID);

        // THEN
        display("Jack records", auditRecords);

        assertEquals("Wrong initial number of jack audit records", 0, auditRecords.size());
    }

    @Test
    public void test100ModifyUserJackKid() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        initialTs = getTimeSafely();

        // WHEN
        when();

        modifyUserReplace(USER_JACK_OID, UserType.F_TITLE, task, result, createPolyString("Kid"));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        jackKidTs = getTimeSafely();
        jackKidEid = assertObjectAuditRecords(USER_JACK_OID, 2);
        assertRecordsFromInitial(jackKidTs, 2);
    }

    /**
     * Let's interlace the history of two objects. So we make sure that the filtering
     * in the time machine works well.
     */
    @Test
    public void test105CreateUserHerman() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userHermanBefore = PrismTestUtil.parseObject(USER_HERMAN_FILE);
        userHermanBefore.asObjectable().setDescription("Unknown");
        userHermanBefore.asObjectable().setNickName(createPolyStringType("HT"));

        hermanInitialTs = getTimeSafely();

        // WHEN
        when();

        addObject(userHermanBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("Herman (created)", user);

        hermanCreatedTs = getTimeSafely();
        hermanCreatedEid = assertObjectAuditRecords(USER_HERMAN_OID, 2);
        assertRecordsFromInitial(hermanCreatedTs, 4);
    }

    @Test
    public void test110ModifyUserJackSailor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_TITLE,
                createPolyString("Sailor"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Hermit on Monkey Island");
        objectDelta.addModificationReplaceProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ActivationStatusType.DISABLED);

        // WHEN
        when();

        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        jackSailorTs = getTimeSafely();
        jackSailorEid = assertObjectAuditRecords(USER_JACK_OID, 4);

        assertRecordsFromPrevious(hermanCreatedTs, jackSailorTs, 2);
        assertRecordsFromPrevious(jackKidTs, jackSailorTs, 4);
        assertRecordsFromInitial(jackSailorTs, 6);
    }

    @Test
    public void test115ModifyUserHermanMarooned() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_HERMAN_OID, UserType.F_TITLE,
                createPolyString("Marooned"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Marooned on Monkey Island");
        objectDelta.addModification(createAssignmentModification(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, true));

        // WHEN
        when();

        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("Herman (marooned)", user);

        hermanMaroonedTs = getTimeSafely();
        hermanMaroonedEid = assertObjectAuditRecords(USER_HERMAN_OID, 4);
        assertRecordsFromInitial(hermanMaroonedTs, 8);
    }

    @Test
    public void test120ModifyUserJackCaptain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_JACK_OID, UserType.F_TITLE,
                createPolyString("Captain"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Hermit on Monkey Island");
        objectDelta.addModificationReplaceProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ActivationStatusType.ENABLED);

        // WHEN
        when();

        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        jackCaptainTs = getTimeSafely();
        jackCaptainEid = assertObjectAuditRecords(USER_JACK_OID, 6);

        assertRecordsFromPrevious(hermanMaroonedTs, jackCaptainTs, 2);
        assertRecordsFromPrevious(jackSailorTs, jackCaptainTs, 4);
        assertRecordsFromInitial(jackCaptainTs, 10);
    }

    @Test
    public void test125ModifyUserHermanHermit() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_HERMAN_OID, UserType.F_TITLE,
                createPolyString("Hermit"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Hermit on Monkey Island");
        objectDelta.addModificationReplaceProperty(UserType.F_HONORIFIC_PREFIX, createPolyString("His Loneliness"));
        objectDelta.addModificationReplaceProperty(UserType.F_NICK_NAME);
        objectDelta.addModification(createAssignmentModification(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, false));
        objectDelta.addModification(createAssignmentModification(ROLE_JUDGE_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true));
        objectDelta.addModification(createAssignmentModification(ROLE_RED_SAILOR_OID, RoleType.COMPLEX_TYPE,
                null, null, null, true));

        // WHEN
        when();

        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("Herman (hermit)", user);

        hermanHermitTs = getTimeSafely();
        hermanHermitEid = assertObjectAuditRecords(USER_HERMAN_OID, 6);
        assertRecordsFromInitial(hermanHermitTs, 12);
    }

    @Test
    public void test135ModifyUserHermanCivilisedHermit() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_HERMAN_OID, UserType.F_TITLE,
                createPolyString("Civilised Hermit"));
        objectDelta.addModificationReplaceProperty(UserType.F_DESCRIPTION, "Civilised Hermit on Monkey Island");
        objectDelta.addModification(createAssignmentModification(ROLE_RED_SAILOR_OID, RoleType.COMPLEX_TYPE,
                null, null, null, false));

        // WHEN
        when();

        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("Herman (civilised hermit)", user);

        hermanCivilisedHermitTs = getTimeSafely();
        hermanCivilisedHermitEid = assertObjectAuditRecords(USER_HERMAN_OID, 8);
        assertRecordsFromInitial(hermanCivilisedHermitTs, 14);
    }

    @Test
    public void test200ReconstructJackSailor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        // precondition
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_TITLE, createPolyString("Captain"));

        // WHEN
        when();

        PrismObject<UserType> jackReconstructed = modelAuditService.reconstructObject(UserType.class, USER_JACK_OID,
                jackSailorEid, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Reconstructed jack", jackReconstructed);

        PrismAsserts.assertPropertyValue(jackReconstructed, UserType.F_TITLE, createPolyString("Sailor"));
        assertAdministrativeStatusDisabled(jackReconstructed);

        // TODO
    }

    @Test
    public void test210ReconstructJackKid() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        // precondition
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_TITLE, createPolyString("Captain"));

        // WHEN
        when();

        PrismObject<UserType> jackReconstructed = modelAuditService.reconstructObject(UserType.class, USER_JACK_OID,
                jackKidEid, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Reconstructed jack", jackReconstructed);

        PrismAsserts.assertPropertyValue(jackReconstructed, UserType.F_TITLE, createPolyString("Kid"));

        // TODO
    }

    /**
     * This is supposed to get the objectToAdd directly from the create delta.
     */
    @Test
    public void test250ReconstructHermanCreated() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_HERMAN_OID);
        display("User before", userBefore);
        // precondition
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_TITLE, createPolyString("Civilised Hermit"));
        PrismAsserts.assertPropertyValue(userBefore, UserType.F_HONORIFIC_PREFIX, createPolyString("His Loneliness"));
        PrismAsserts.assertNoItem(userBefore, UserType.F_NICK_NAME);
        assertAssignments(userBefore, 1);

        // WHEN
        when();

        PrismObject<UserType> hermanReconstructed = modelAuditService.reconstructObject(UserType.class, USER_HERMAN_OID,
                hermanCreatedEid, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Reconstructed herman", hermanReconstructed);

        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_DESCRIPTION, "Unknown");
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_NICK_NAME, createPolyString("HT"));
        PrismAsserts.assertNoItem(hermanReconstructed, UserType.F_TITLE);
        PrismAsserts.assertNoItem(hermanReconstructed, UserType.F_HONORIFIC_PREFIX);
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_LOCALITY, createPolyString("Monkey Island"));

        assertNoAssignments(hermanReconstructed);
    }

    /**
     * Rolling back the deltas from the current state.
     */
    @Test
    public void test252ReconstructHermanMarooned() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        PrismObject<UserType> hermanReconstructed = modelAuditService.reconstructObject(UserType.class, USER_HERMAN_OID,
                hermanMaroonedEid, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Reconstructed herman", hermanReconstructed);

        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_TITLE, createPolyString("Marooned"));
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_DESCRIPTION, "Marooned on Monkey Island");
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_NICK_NAME, createPolyString("HT"));
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_LOCALITY, createPolyString("Monkey Island"));
        PrismAsserts.assertNoItem(hermanReconstructed, UserType.F_HONORIFIC_PREFIX);

        assertAssignedAccount(hermanReconstructed, RESOURCE_DUMMY_OID);
        assertAssignments(hermanReconstructed, 1);
    }

    /**
     * Rolling back the deltas from the current state.
     */
    @Test
    public void test254ReconstructHermanHermit() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        PrismObject<UserType> hermanReconstructed = modelAuditService.reconstructObject(UserType.class, USER_HERMAN_OID,
                hermanHermitEid, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Reconstructed herman", hermanReconstructed);

        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_TITLE, createPolyString("Hermit"));
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_DESCRIPTION, "Hermit on Monkey Island");
        PrismAsserts.assertNoItem(hermanReconstructed, UserType.F_NICK_NAME);
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_LOCALITY, createPolyString("Monkey Island"));
        PrismAsserts.assertPropertyValue(hermanReconstructed, UserType.F_HONORIFIC_PREFIX, createPolyString("His Loneliness"));

        assertAssignedRole(hermanReconstructed, ROLE_RED_SAILOR_OID);
        assertAssignedRole(hermanReconstructed, ROLE_JUDGE_OID);
        assertAssignments(hermanReconstructed, 2);
    }

    @Test
    public void test290QueryUnknown() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        AuditEventRecord record = new AuditEventRecord(AuditEventType.SYNCHRONIZATION, AuditEventStage.EXECUTION);
        record.setOutcome(OperationResultStatus.UNKNOWN);
        modelAuditService.audit(record, task, result);

        HashMap<String, Object> params = new HashMap<>();
        params.put("outcome", OperationResultStatusType.UNKNOWN);
        List<AuditEventRecord> records = modelAuditService.listRecords("select * from m_audit_event as aer where aer.outcome = :outcome", params, task, result);

        // THEN
        then();
        display("records", records);
        assertEquals("Wrong # of records", 1, records.size());

        // THEN
        assertSuccess(result);
    }

    private String assertObjectAuditRecords(String oid, int expectedNumberOfRecords) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        List<AuditEventRecord> auditRecords = getObjectAuditRecords(oid);
        display("Object records", auditRecords);
        assertEquals("Wrong number of jack audit records", expectedNumberOfRecords, auditRecords.size());
        return auditRecords.get(auditRecords.size() - 1).getEventIdentifier();
    }

    private void assertRecordsFromPrevious(XMLGregorianCalendar from, XMLGregorianCalendar to,
            int expectedNumberOfRecords) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        List<AuditEventRecord> auditRecordsSincePrevious = getAuditRecordsFromTo(from, to, task, result);
        display("From/to records (previous)", auditRecordsSincePrevious);
        assertEquals("Wrong number of audit records (previous)", expectedNumberOfRecords, auditRecordsSincePrevious.size());
    }

    private void assertRecordsFromInitial(XMLGregorianCalendar to, int expectedNumberOfRecords) throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        List<AuditEventRecord> auditRecordsSincePrevious = getAuditRecordsFromTo(initialTs, to, task, result);
        display("From/to records (initial)", auditRecordsSincePrevious);
        assertEquals("Wrong number of audit records (initial)", expectedNumberOfRecords, auditRecordsSincePrevious.size());
    }

    /**
     * We make concurrent modify operations to test audit service under higher load
     */
    @Test
    public void test300ConcurrentAudits() throws Exception {
        final int NUM_THREADS = 2;
        final int ITERATIONS = 300;
        final long TIMEOUT = 600_000;

//        skipTestIf(isH2(), "because of H2 database");

        // creating objects
        List<String> oids = new ArrayList<>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            UserType user = new UserType(prismContext);
            user.setName(PolyStringType.fromOrig("user-" + i));
            addObject(user.asPrismObject());
            oids.add(user.getOid());
        }
        display("OIDs", oids);

        // creating threads + starting them
        List<Thread> threads = new ArrayList<>(NUM_THREADS);
        List<Throwable> results = new ArrayList<>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                try {
                    login(userAdministrator.clone());
                    Task threadTask = createTask();
                    OperationResult threadResult = threadTask.getResult();
                    for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                        display("Executing iteration " + iteration + " on user " + index);
                        ObjectDelta delta = prismContext.deltaFor(UserType.class)
                                .item(UserType.F_FULL_NAME).replace(PolyString.fromOrig("User " + index + " iteration " + iteration))
                                .asObjectDelta(oids.get(index));
                        executeChangesAssertSuccess(delta, null, threadTask, threadResult);
                    }
                    results.set(index, null);
                } catch (Throwable t) {
                    display("Thread " + index + " got an exception ", t);
                    results.set(index, t);
                }
            });
            //thread.setName("Worker " + i);
            threads.add(thread);
            results.add(new IllegalStateException("Thread not finished"));    // cleared on successful finish
        }
        threads.forEach(Thread::start);

        // waiting for threads
        long deadline = System.currentTimeMillis() + TIMEOUT;
        for (int i = 0; i < NUM_THREADS; i++) {
            long waitTime = deadline - System.currentTimeMillis();
            if (waitTime > 0) {
                threads.get(i).join(waitTime);
            }
        }

        // checking results
        int fails = 0;
        for (int i = 0; i < NUM_THREADS; i++) {
            if (results.get(i) != null) {
                fails++;
                display("Thread " + i + " produced an exception: " + results.get(i));
            }
        }
        if (fails > 0) {
            fail(fails + " thread(s) failed: " + results.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }

        // TODO check audit correctness
    }

    /**
     * Pure audit attempts (TODO move to some other test class in lower levels)
     */
    @Test
    public void test310ConcurrentAuditsRaw() throws Exception {
        final int NUM_THREADS = 2;
        final int ITERATIONS = 300;
        final long TIMEOUT = 600_000;

        // Originally we wanted to skip this because of possible concurrency issues on H2, but we'll try it for a while
//        skipTestIf(isH2(), "H2 database can have MVCC issues");

        final AtomicBoolean failed = new AtomicBoolean(false);        // signal to kill other threads after a failure

        // creating threads + starting them
        List<Thread> threads = new ArrayList<>(NUM_THREADS);
        List<Throwable> results = new ArrayList<>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                try {
                    login(userAdministrator.clone());
                    Task threadTask = createTask();
                    OperationResult threadResult = threadTask.getResult();
                    for (int iteration = 0; iteration < ITERATIONS; iteration++) {
                        display("Executing iteration " + iteration + " in worker " + index);
                        AuditEventRecord record = new AuditEventRecord(AuditEventType.MODIFY_OBJECT, AuditEventStage.EXECUTION);
                        record.setEventIdentifier(
                                iteration + ":" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1_000_000));
                        ObjectDelta<?> delta = prismContext.deltaFor(UserType.class)
                                .item(UserType.F_FULL_NAME).replace(PolyString.fromOrig("Hi" + iteration))
                                .item(UserType.F_METADATA, MetadataType.F_MODIFY_TIMESTAMP).replace(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                                .asObjectDelta("oid" + index);
                        record.getDeltas().add(new ObjectDeltaOperation(delta));
                        modelAuditService.audit(record, threadTask, threadResult);
                        if (failed.get()) {
                            results.set(index, new IllegalStateException("Some other thread failed"));
                            return;
                        }
                    }
                    results.set(index, null);
                } catch (Throwable t) {
                    System.err.println("Thread " + index + " got an exception " + t);
                    LoggingUtils.logUnexpectedException(logger, "Thread {} got an exception", t, index);
                    results.set(index, t);
                    failed.set(true);
                }
            });
            thread.setName("Worker " + i);
            threads.add(thread);
            results.add(new IllegalStateException("Thread not finished"));    // cleared on successful finish
        }
        threads.forEach(Thread::start);

        // waiting for threads
        long deadline = System.currentTimeMillis() + TIMEOUT;
        for (int i = 0; i < NUM_THREADS; i++) {
            long waitTime = deadline - System.currentTimeMillis();
            if (waitTime > 0) {
                threads.get(i).join(waitTime);
            }
        }

        // checking results
        int fails = 0;
        for (int i = 0; i < NUM_THREADS; i++) {
            if (results.get(i) != null) {
                fails++;
                display("Thread " + i + " produced an exception: " + results.get(i));
            }
        }
        if (fails > 0) {
            fail(fails + " thread(s) failed: " + results.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        }

        // TODO check audit correctness
    }

    // If a timestamp is retrieved at the same time an operation started/finished, wrong results might be assumed
    // (audit record could be mistakenly included in the time interval bounded by the timestamp).
    // As I am currently too lazy to think out all possible failure modes, the safest way is to separate
    // timestamp retrieval from any audited actions by small amount of time.
    private XMLGregorianCalendar getTimeSafely() {
        sleep(50);
        XMLGregorianCalendar time = clock.currentTimeXMLGregorianCalendar();
        sleep(50);
        return time;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // no problem here
        }
    }

}
