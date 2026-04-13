/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.core.Authentication;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Draft reproducer for MID-10834 based strictly on mappings from the ticket.
 *
 * Idea:
 *  - create users + services/orgs with values that should match 1:1
 *  - run repeated recompute from a multithreaded task
 *  - after each wave, assert that each user has exactly one expected assignment
 *  - cleanup raw assignments and membership refs to allow next iteration to start from scratch
 *
 * Notes:
 *  - known bad symptoms to watch for are: all matching targets assigned, wrong target selected,
 *    one part of the MQL ignored, variable leakage between mappings, and expression errors like
 *    "No variable with name ..."
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMqlTicket10834 extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/mql-ticket-10834");
    private static final String NS_MID_10834 = "http://midpoint.evolveum.com/xml/ns/test/mql-ticket-10834";
    private static final String ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
    private static final String ROLE_CARRIER_OID = "33333333-3333-3333-3333-333333333333";
    private static final String ROLE_ARCHETYPE_OID = "879e6e8a-ba0a-449d-9795-4f089c52aaaa";
    private static final String ROLE_DISTRACTOR_ARCHETYPE_OID = "979e6e8a-ba0a-449d-9795-4f089c52aaab";
    private static final QName INITIAL_OWNER_NAME = new QName(NS_MID_10834, "initialOwnerName");
    private static final QName ID_WORKPLACE = new QName(NS_MID_10834, "idWorkplace");
    private static final QName ADMIN_PERSONAL_NUMBER = new QName(NS_MID_10834, "adminPersonalNumber");
    private static final QName EX_ROLE_SAP_CODE1 = new QName(NS_MID_10834, "exRoleSapCode1");
    private static final QName EX_ROLE_SAP_CODE = new QName(NS_MID_10834, "exRoleSapCode");
    private static final QName EX_ROLE_APPLICATION_INSTANCE_NAME =
            new QName(NS_MID_10834, "exRoleApplicationInstanceName");

    private static final int USERS = Integer.getInteger("midpoint.mid10834.users", 100);
    private static final int SERVICES = Integer.getInteger("midpoint.mid10834.services", USERS);
    private static final int ORGS = Integer.getInteger("midpoint.mid10834.orgs", 20);
    private static final int WAVES = Integer.getInteger("midpoint.mid10834.waves", 50);
    private static final int WORKER_THREADS = Integer.getInteger("midpoint.mid10834.workerThreads", 16);
    public final TestObject<ObjectTemplateType> objectTemplateUserService = TestObject.file(TEST_DIR, "object-template-user-service.xml", "11111111-1111-1111-1111-111111111111");
    public final TestObject<ObjectTemplateType> objectTemplateUserOrg = TestObject.file(TEST_DIR, "object-template-user-org.xml", "22222222-2222-2222-2222-222222222222");
    public final TestObject<RoleType> roleCarrier = TestObject.file(TEST_DIR, "role-user-role-carrier.xml", ROLE_CARRIER_OID);

    @Override
    public boolean isNativeRepository() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        Task cleanupTask = createPlainTask("cleanupMqlTicket10834TaskRecompute");
        OperationResult cleanupResult = cleanupTask.getResult();
        cleanupTestData(cleanupTask, cleanupResult);
        addObject(objectTemplateUserService, initTask, initResult);
        addObject(objectTemplateUserOrg, initTask, initResult);
        addObject(roleCarrier, initTask, initResult);

        createRoleArchetypes(initTask, initResult);
        createServices(initTask, initResult);
        createOrgs(initTask, initResult);
        createRoles(initTask, initResult);
        createUsers(initTask, initResult);
    }

    private void cleanupTestData(Task task, OperationResult result) throws Exception {
        cleanupDelete(ObjectTemplateType.class, objectTemplateUserService.oid, task, result);
        cleanupDelete(ObjectTemplateType.class, objectTemplateUserOrg.oid, task, result);
        cleanupDelete(RoleType.class, roleCarrier.oid, task, result);

        for (int i = 0; i < USERS; i++) {
            cleanupDelete(UserType.class, userOid(i), task, result);
        }
        for (int i = 0; i < SERVICES; i++) {
            cleanupDelete(ServiceType.class, serviceOid(i), task, result);
        }
        for (int i = 0; i < ORGS; i++) {
            cleanupDelete(OrgType.class, orgOid(i), task, result);
        }
        for (int i = 0; i < USERS; i++) {
            cleanupDelete(RoleType.class, roleMatchOid(i), task, result);
            cleanupDelete(RoleType.class, roleWrongSapCodeOid(i), task, result);
            cleanupDelete(RoleType.class, roleWrongApplicationOid(i), task, result);
            cleanupDelete(RoleType.class, roleWrongArchetypeOid(i), task, result);
        }
        cleanupDelete(ArchetypeType.class, ROLE_ARCHETYPE_OID, task, result);
        cleanupDelete(ArchetypeType.class, ROLE_DISTRACTOR_ARCHETYPE_OID, task, result);
    }

    private <O extends ObjectType> void cleanupDelete(Class<O> type, String oid, Task task, OperationResult result)
            throws Exception {
        try {
            getObject(type, oid);
        } catch (ObjectNotFoundException ignored) {
            // Best-effort cleanup for persistent test repositories.
            return;
        }
        deleteObject(type, oid, task, result);
    }

    /**
     * Service scenario reproducing the original "Add initial service owner" mapping from MID-10834.
     *
     * Purpose:
     *  - Tests scripted MQL inside q:text where the right-hand side is provided by a script block.
     *  - Matches users to services via extension/initialOwnerName.
     *
     * Why this matters:
     *  - This is the closest 1:1 reproduction of the original ticket mapping.
     *  - Reported issue: MQL + script intermittently fails under concurrency, potentially assigning
     *    wrong service or multiple services.
     *
     * Test behavior:
     *  - Runs concurrent recompute for all users.
     *  - Each user must get exactly ONE correct service assignment.
     *  - Any deviation (0, >1, or wrong target) indicates MQL evaluation inconsistency.
     */
    @Test
    public void test100RecomputeUsersInMultithreadedTask_serviceOwnerMapping() throws Exception {
        OperationResult result = getTestOperationResult();

        setDefaultUserTemplate("11111111-1111-1111-1111-111111111111", result);

        runConcurrentServiceScenario(WAVES, "test100");
    }

    /**
     * Organization scenario reproducing variable-based MQL filters from MID-10834.
     *
     * Purpose:
     *  - Tests MQL expressions that depend on mapping variables (e.g. $idWorkplace, $personalNumber).
     *  - Includes both member and manager assignments using different attributes.
     *
     * Why this matters:
     *  - Ticket reports random failures such as:
     *      "No variable with name personalNumber"
     *  - This suggests variable binding / leakage issues under concurrency.
     *
     * Test behavior:
     *  - Runs concurrent recompute for all users.
     *  - Each user must get exactly ONE org member and ONE org manager assignment.
     *  - Detects:
     *      - missing assignments
     *      - incorrect target org
     *      - variable resolution failures
     */
    @Test
    public void test110RecomputeUsersInMultithreadedTask_orgMappings() throws Exception {
        OperationResult result = getTestOperationResult();

        setDefaultUserTemplate("22222222-2222-2222-2222-222222222222", result);

        runConcurrentOrgScenario(WAVES, "test110");
    }

    /**
     * Role scenario reproducing multi-clause MQL filter from MID-10834 using inducement.
     *
     * Purpose:
     *  - Tests complex q:text filter with multiple AND clauses:
     *      - dynamic expression (backtick script)
     *      - fixed literals
     *      - archetype matching
     *  - Uses inducement + targetRef + resolutionTime=run to match original ticket structure.
     *
     * Why this matters:
     *  - Ticket reports that MQL may randomly ignore parts of the filter (e.g. one clause skipped),
     *    resulting in too many or incorrect role assignments.
     *
     * Test behavior:
     *  - Each user is assigned a carrier role that induces exactly one target role via MQL.
     *  - Dataset includes:
     *      - one correct role
     *      - multiple distractor roles (wrong code, app, or archetype)
     *  - After recompute:
     *      - exactly one induced role must be resolved
     *      - wrong or multiple matches indicate broken MQL clause evaluation
     */
    @Test
    public void test120RecomputeUsersInMultithreadedTask_roleMultiClauseMql() throws Exception {
        OperationResult result = getTestOperationResult();

        setDefaultUserTemplate(null, result);

        runConcurrentRoleScenario(WAVES, "test120");
    }

    private void runConcurrentServiceScenario(int waves, String label) throws Exception {
        for (int wave = 0; wave < waves; wave++) {
            long waveStart = System.currentTimeMillis();
            System.out.println(label + " service wave " + wave + " starting");
            displayTestTitle(label + " service wave " + wave);
            runConcurrentWave(wave, "service", label, this::processSingleUserServiceScenario);
            assertUsersClean(label + " service wave " + wave + " after per-user cleanup");
            System.out.println(label + " service wave " + wave + " finished in " + (System.currentTimeMillis() - waveStart) + " ms");
        }
    }

    private void runConcurrentOrgScenario(int waves, String label) throws Exception {
        for (int wave = 0; wave < waves; wave++) {
            long waveStart = System.currentTimeMillis();
            System.out.println(label + " org wave " + wave + " starting");
            displayTestTitle(label + " org wave " + wave);
            runConcurrentWave(wave, "org", label, this::processSingleUserOrgScenario);
            assertUsersClean(label + " org wave " + wave + " after per-user cleanup");
            System.out.println(label + " org wave " + wave + " finished in " + (System.currentTimeMillis() - waveStart) + " ms");
        }
    }

    private void runConcurrentRoleScenario(int waves, String label) throws Exception {
        for (int wave = 0; wave < waves; wave++) {
            long waveStart = System.currentTimeMillis();
            System.out.println(label + " role wave " + wave + " starting");
            displayTestTitle(label + " role wave " + wave);
            runConcurrentWave(wave, "role", label, this::processSingleUserRoleScenario);
            assertUsersClean(label + " role wave " + wave + " after per-user cleanup");
            System.out.println(label + " role wave " + wave + " finished in " + (System.currentTimeMillis() - waveStart) + " ms");
        }
    }

    private void runConcurrentWave(int wave, String scenario, String label, UserScenarioProcessor processor) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS, workerThreadFactory(label + "-" + scenario + "-wave-" + wave));
        try {
            List<Callable<Void>> jobs = new ArrayList<>();
            for (int i = 0; i < USERS; i++) {
                final int userIndex = i;
                jobs.add(() -> {
                    processor.process(userIndex, wave);
                    return null;
                });
            }

            List<Throwable> failures = new ArrayList<>();
            List<Future<Void>> futures = pool.invokeAll(jobs);
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    failures.add(e.getCause() != null ? e.getCause() : e);
                }
            }
            if (!failures.isEmpty()) {
                AssertionError error = new AssertionError("MID-10834 " + scenario + " wave " + wave
                        + " had " + failures.size() + " worker failures");
                failures.forEach(error::addSuppressed);
                throw error;
            }
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private ThreadFactory workerThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + "-worker-" + counter.incrementAndGet());
            return thread;
        };
    }

    private void processSingleUserServiceScenario(int userIndex, int wave) throws Exception {
        String userOid = userOid(userIndex);
        String expectedServiceOid = serviceOid(userIndex % SERVICES);
        Task localTask = createPlainTask("mid10834-service-wave-" + wave + "-user-" + userIndex);
        OperationResult result = localTask.getResult();
        try {
            runAsAdministrator(result, () -> {
                modelService.recompute(UserType.class, userOid, null, localTask, result);
                result.computeStatus();
                assertSuccess(result);
                assertSingleExpectedServiceAssignment(userIndex, wave, expectedServiceOid, result);
                rawCleanupUser(userOid, localTask);
            });
        } catch (Throwable t) {
            throw contextualFailure("service", wave, userIndex, userOid, expectedServiceOid, t, result);
        }
    }

    private void processSingleUserOrgScenario(int userIndex, int wave) throws Exception {
        String userOid = userOid(userIndex);
        String expectedOrgOid = orgOid(userIndex % ORGS);
        Task localTask = createPlainTask("mid10834-org-wave-" + wave + "-user-" + userIndex);
        OperationResult result = localTask.getResult();
        try {
            runAsAdministrator(result, () -> {
                modelService.recompute(UserType.class, userOid, null, localTask, result);
                result.computeStatus();
                assertSuccess(result);
                assertSingleExpectedOrgAssignments(userIndex, wave, expectedOrgOid, result);
                rawCleanupUser(userOid, localTask);
            });
        } catch (Throwable t) {
            throw contextualFailure("org", wave, userIndex, userOid, expectedOrgOid, t, result);
        }
    }

    private void processSingleUserRoleScenario(int userIndex, int wave) throws Exception {
        String userOid = userOid(userIndex);
        String expectedRoleOid = roleMatchOid(userIndex);
        Task localTask = createPlainTask("mid10834-role-wave-" + wave + "-user-" + userIndex);
        OperationResult result = localTask.getResult();
        try {
            runAsAdministrator(result, () -> {
                ensureRoleCarrierAssigned(userOid, localTask);
                modelService.recompute(UserType.class, userOid, null, localTask, result);
                result.computeStatus();
                assertSuccess(result);
                assertSingleExpectedRoleAssignment(userIndex, wave, expectedRoleOid, result);
                rawCleanupUser(userOid, localTask);
            });
        } catch (Throwable t) {
            throw contextualFailure("role", wave, userIndex, userOid, expectedRoleOid, t, result);
        }
    }

    private void runAsAdministrator(OperationResult result, WorkerOperation operation) throws Exception {
        Authentication originalAuthentication = securityContextManager.getAuthentication();
        try {
            PrismObject<UserType> administrator =
                    repositoryService.getObject(UserType.class, ADMINISTRATOR_OID, null, result);
            securityContextManager.setupPreAuthenticatedSecurityContext(administrator, result);
            operation.run();
        } finally {
            securityContextManager.setupPreAuthenticatedSecurityContext(originalAuthentication);
        }
    }

    private void assertSingleExpectedServiceAssignment(int userIndex, int wave, String expectedServiceOid,
            OperationResult result) throws Exception {
        PrismObject<UserType> user = getUserRepo(userOid(userIndex), result);
        UserType userType = user.asObjectable();

        List<AssignmentType> ownerAssignments = new ArrayList<>();
        for (AssignmentType assignment : userType.getAssignment()) {
            if (assignment.getTargetRef() == null) {
                continue;
            }
            if (!ServiceType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                continue;
            }
            if (!SchemaConstants.ORG_OWNER.equals(assignment.getTargetRef().getRelation())) {
                continue;
            }
            ownerAssignments.add(assignment);
        }

        assertThat(ownerAssignments)
                .withFailMessage("service wave %s user %s expected exactly one owner service assignment, actual=%s",
                        wave, userType.getName(), describeAssignments(userType))
                .hasSize(1);
        assertThat(ownerAssignments.get(0).getTargetRef().getOid())
                .withFailMessage("service wave %s user %s expected service oid %s, actual assignments=%s",
                        wave, userType.getName(), expectedServiceOid, describeAssignments(userType))
                .isEqualTo(expectedServiceOid);
    }

    private void assertSingleExpectedOrgAssignments(int userIndex, int wave, String expectedOrgOid,
            OperationResult result) throws Exception {
        PrismObject<UserType> user = getUserRepo(userOid(userIndex), result);
        UserType userType = user.asObjectable();

        List<AssignmentType> memberAssignments = userType.getAssignment().stream()
                .filter(a -> a.getTargetRef() != null)
                .filter(a -> OrgType.COMPLEX_TYPE.equals(a.getTargetRef().getType()))
                .filter(a -> SchemaConstants.ORG_DEFAULT.equals(a.getTargetRef().getRelation()))
                .toList();

        List<AssignmentType> managerAssignments = userType.getAssignment().stream()
                .filter(a -> a.getTargetRef() != null)
                .filter(a -> OrgType.COMPLEX_TYPE.equals(a.getTargetRef().getType()))
                .filter(a -> SchemaConstants.ORG_MANAGER.equals(a.getTargetRef().getRelation()))
                .toList();

        assertThat(memberAssignments)
                .withFailMessage("org wave %s user %s expected exactly one member org assignment, actual=%s",
                        wave, userType.getName(), describeAssignments(userType))
                .hasSize(1);
        assertThat(managerAssignments)
                .withFailMessage("org wave %s user %s expected exactly one manager org assignment, actual=%s",
                        wave, userType.getName(), describeAssignments(userType))
                .hasSize(1);
        assertThat(memberAssignments.get(0).getTargetRef().getOid())
                .withFailMessage("org wave %s user %s expected member org oid %s, actual=%s",
                        wave, userType.getName(), expectedOrgOid, describeAssignments(userType))
                .isEqualTo(expectedOrgOid);
        assertThat(managerAssignments.get(0).getTargetRef().getOid())
                .withFailMessage("org wave %s user %s expected manager org oid %s, actual=%s",
                        wave, userType.getName(), expectedOrgOid, describeAssignments(userType))
                .isEqualTo(expectedOrgOid);
    }

    private void assertSingleExpectedRoleAssignment(int userIndex, int wave, String expectedRoleOid,
            OperationResult result) throws Exception {
        PrismObject<UserType> user = getUserRepo(userOid(userIndex), result);
        UserType userType = user.asObjectable();

        List<AssignmentType> directRoleAssignments = userType.getAssignment().stream()
                .filter(a -> a.getTargetRef() != null)
                .filter(a -> RoleType.COMPLEX_TYPE.equals(a.getTargetRef().getType()))
                .toList();
        List<String> roleMembershipOids = userType.getRoleMembershipRef().stream()
                .map(ref -> ref.getOid())
                .filter(Objects::nonNull)
                .toList();

        assertThat(directRoleAssignments)
                .withFailMessage("role wave %s user %s expected exactly one direct role assignment, actual=%s",
                        wave, userType.getName(), describeAssignments(userType))
                .hasSize(1);
        assertThat(directRoleAssignments.get(0).getTargetRef().getOid())
                .withFailMessage("role wave %s user %s expected carrier role oid %s, actual=%s",
                        wave, userType.getName(), ROLE_CARRIER_OID, describeAssignments(userType))
                .isEqualTo(ROLE_CARRIER_OID);
        assertThat(roleMembershipOids)
                .withFailMessage("role wave %s user %s expected role memberships [%s, %s], actual=%s assignments=%s",
                        wave, userType.getName(), ROLE_CARRIER_OID, expectedRoleOid,
                        roleMembershipOids, describeAssignments(userType))
                .containsExactlyInAnyOrder(ROLE_CARRIER_OID, expectedRoleOid);
    }

    private void rawCleanupUser(String userOid, Task task) throws Exception {
        OperationResult result = task.getResult().createSubresult("mid10834RawCleanup");
        ModelExecuteOptions raw = ModelExecuteOptions.createRaw();
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).replace()
                .asObjectDelta(userOid);
        executeChanges(delta, raw, task, result);
        ObjectDelta<UserType> membershipDelta = deltaFor(UserType.class)
                .item(UserType.F_ROLE_MEMBERSHIP_REF).replace()
                .asObjectDelta(userOid);
        executeChanges(membershipDelta, raw, task, result);
        result.computeStatus();
        assertSuccess(result);
    }

    private void ensureRoleCarrierAssigned(String userOid, Task task) throws Exception {
        OperationResult result = task.getResult().createSubresult("mid10834EnsureRoleCarrier");
        ModelExecuteOptions raw = ModelExecuteOptions.createRaw();
        AssignmentType assignment = new AssignmentType();

        assignment.setDescription(userOid);
        assignment.beginTargetRef().oid(ROLE_CARRIER_OID).type(RoleType.COMPLEX_TYPE).end();
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(assignment)
                .asObjectDelta(userOid);
        executeChanges(delta, raw, task, result);
        result.computeStatus();
        assertSuccess(result);
    }

    private void assertUsersClean(String stage) throws Exception {
        OperationResult result = getTestOperationResult();
        for (int i = 0; i < USERS; i++) {
            PrismObject<UserType> user = getUserRepo(userOid(i), result);
            assertThat(user.asObjectable().getAssignment())
                    .withFailMessage("%s: user %s still has assignments %s", stage, user.asObjectable().getName(),
                            describeAssignments(user.asObjectable()))
                    .isEmpty();
            assertThat(user.asObjectable().getRoleMembershipRef())
                    .withFailMessage("%s: user %s still has roleMembershipRef values %s", stage, user.asObjectable().getName(),
                            user.asObjectable().getRoleMembershipRef())
                    .isEmpty();
        }
    }

    private String describeAssignments(UserType user) {
        return user.getAssignment().stream()
                .map(a -> {
                    if (a.getTargetRef() == null) {
                        return "null-target";
                    }
                    return a.getTargetRef().getType().getLocalPart() + ":" + a.getTargetRef().getRelation() + ":" + a.getTargetRef().getOid();
                })
                .toList()
                + " roleMembershipRef=" + user.getRoleMembershipRef();
    }

    private AssertionError contextualFailure(String scenario, int wave, int userIndex, String userOid, String expectedTargetOid,
            Throwable cause, OperationResult result) throws Exception {
        UserType user = getUserRepo(userOid, result).asObjectable();
        AssertionError error = new AssertionError(
                "MID-10834 " + scenario
                        + " failure: wave=" + wave
                        + ", thread=" + Thread.currentThread().getName()
                        + ", userIndex=" + userIndex
                        + ", userOid=" + userOid
                        + ", expectedTargetOid=" + expectedTargetOid
                        + ", actualAssignments=" + describeAssignments(user));
        error.initCause(cause);
        return error;
    }

    private PrismObject<UserType> getUserRepo(String oid, OperationResult result) throws Exception {
        return repositoryService.getObject(UserType.class, oid, null, result);
    }

    @FunctionalInterface
    private interface UserScenarioProcessor {
        void process(int userIndex, int wave) throws Exception;
    }

    @FunctionalInterface
    private interface WorkerOperation {
        void run() throws Exception;
    }

    private void createServices(Task task, OperationResult result) throws Exception {
        for (int i = 0; i < SERVICES; i++) {
            ServiceType service = new ServiceType(prismContext);
            service.setOid(serviceOid(i));
            service.setName(PolyStringType.fromOrig("service-" + String.format("%03d", i)));
            service.asPrismObject().getOrCreateExtension().setPropertyRealValue(INITIAL_OWNER_NAME, ownerName(i));
            addObject(service.asPrismObject(), task, result);
        }
    }

    private void createRoleArchetypes(Task task, OperationResult result) throws Exception {
        ArchetypeType matchingArchetype = new ArchetypeType(prismContext);
        matchingArchetype.setOid(ROLE_ARCHETYPE_OID);
        matchingArchetype.setName(PolyStringType.fromOrig("mid10834-role-matching"));
        addObject(matchingArchetype.asPrismObject(), ModelExecuteOptions.create().raw(), task, result);

        ArchetypeType distractorArchetype = new ArchetypeType(prismContext);
        distractorArchetype.setOid(ROLE_DISTRACTOR_ARCHETYPE_OID);
        distractorArchetype.setName(PolyStringType.fromOrig("mid10834-role-distractor"));
        addObject(distractorArchetype.asPrismObject(), ModelExecuteOptions.create().raw(), task, result);
    }

    private void createOrgs(Task task, OperationResult result) throws Exception {
        for (int i = 0; i < ORGS; i++) {
            OrgType org = new OrgType(prismContext);
            org.setOid(orgOid(i));
            org.setName(PolyStringType.fromOrig("org-" + i));
            org.setIdentifier("PM-" + i);
            org.beginArchetypeRef().oid("bcbcbcbc-901e-454b-ba1b-58d67232dc4a").type(com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType.COMPLEX_TYPE).end();
            org.asPrismObject().getOrCreateExtension().setPropertyRealValue(ADMIN_PERSONAL_NUMBER, personalNumber(i));
            addObject(org.asPrismObject(), ModelExecuteOptions.create().raw(), task, result);
        }
    }

    private void createRoles(Task task, OperationResult result) throws Exception {
        for (int i = 0; i < USERS; i++) {
            addObject(createRole(roleMatchOid(i), "role-match-" + i, userOid(i), "yyy", "FSD", ROLE_ARCHETYPE_OID),
                    ModelExecuteOptions.create().raw(), task, result);
            addObject(createRole(roleWrongSapCodeOid(i), "role-wrong-code-" + i, userOid(i), "zzz", "FSD",
                    ROLE_ARCHETYPE_OID), ModelExecuteOptions.create().raw(), task, result);
            addObject(createRole(roleWrongApplicationOid(i), "role-wrong-app-" + i, userOid(i), "yyy", "OTHER",
                    ROLE_ARCHETYPE_OID), ModelExecuteOptions.create().raw(), task, result);
            addObject(createRole(roleWrongArchetypeOid(i), "role-wrong-archetype-" + i, userOid(i), "yyy", "FSD",
                    ROLE_DISTRACTOR_ARCHETYPE_OID), ModelExecuteOptions.create().raw(), task, result);
        }
    }

    private PrismObject<RoleType> createRole(String oid, String name, String sapCode1, String sapCode,
            String applicationInstanceName, String archetypeOid) throws SchemaException {
        RoleType role = new RoleType(prismContext);
        role.setOid(oid);
        role.setName(PolyStringType.fromOrig(name));
        role.asPrismObject().getOrCreateExtension().setPropertyRealValue(EX_ROLE_SAP_CODE1, sapCode1);
        role.asPrismObject().getOrCreateExtension().setPropertyRealValue(EX_ROLE_SAP_CODE, sapCode);
        role.asPrismObject().getOrCreateExtension().setPropertyRealValue(
                EX_ROLE_APPLICATION_INSTANCE_NAME, applicationInstanceName);
        role.beginArchetypeRef().oid(archetypeOid).type(ArchetypeType.COMPLEX_TYPE).end();
        return role.asPrismObject();
    }

    private void createUsers(Task task, OperationResult result) throws Exception {
        for (int i = 0; i < USERS; i++) {
            UserType user = new UserType(prismContext);
            user.setOid(userOid(i));
            user.setName(PolyStringType.fromOrig(ownerName(i % SERVICES)));
            user.setPersonalNumber(personalNumber(i % ORGS));
            user.asPrismObject().getOrCreateExtension().setPropertyRealValue(ID_WORKPLACE, "PM-" + (i % ORGS));
            addObject(user.asPrismObject(), task, result);
        }
    }

    private void setDefaultUserTemplate(String templateOid, OperationResult result) throws CommonException {
        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, templateOid, result);
    }

    private String ownerName(int i) {
        return "neg" + String.format("%03d", i);
    }

    private String personalNumber(int i) {
        return "PN-" + String.format("%03d", i);
    }

    private String userOid(int i) {
        return String.format("aaaaaaa0-0000-0000-0000-%012d", i);
    }

    private String serviceOid(int i) {
        return String.format("bbbbbbb0-0000-0000-0000-%012d", i);
    }

    private String orgOid(int i) {
        return String.format("ccccccc0-0000-0000-0000-%012d", i);
    }

    private String roleMatchOid(int i) {
        return String.format("ddddddd0-0000-0000-0000-%012d", i);
    }

    private String roleWrongSapCodeOid(int i) {
        return String.format("ddddddd1-0000-0000-0000-%012d", i);
    }

    private String roleWrongApplicationOid(int i) {
        return String.format("ddddddd2-0000-0000-0000-%012d", i);
    }

    private String roleWrongArchetypeOid(int i) {
        return String.format("ddddddd3-0000-0000-0000-%012d", i);
    }
}
