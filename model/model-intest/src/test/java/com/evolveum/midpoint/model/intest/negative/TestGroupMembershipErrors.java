/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.negative;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.intest.dummys.DummyAdTrivialScenario;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test for #11979
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestGroupMembershipErrors extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/negative/group-membership-errors");

    private static final String INTENT_DEFAULT = "default";

    private static final long RECONCILE_TIMEOUT_MS = 30_000;

    private static final String ROLE_TESTERS_NAME = "testers";

    private static DummyAdTrivialScenario scenario;

    private static final TestObject<ArchetypeType> ARCHETYPE_GROUP_ROLE = TestObject.file(
            TEST_DIR, "archetype-group-role.xml", "3cfd094d-dabb-4a44-a52e-a4e519cb2a94");

    private static final DummyTestResource RESOURCE_DUMMY_MEMBERSHIP = new DummyTestResource(
            TEST_DIR, "resource-dummy-membership.xml", "b41f7dcc-a413-4b43-a54e-7ee1fbf85722", "membership",
            c -> scenario = DummyAdTrivialScenario.on(c).initialize());

    /** Role named {@link #ROLE_TESTERS_NAME}; owns the group of the same name on the resource. */
    private String roleTestersOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult, ARCHETYPE_GROUP_ROLE);
        RESOURCE_DUMMY_MEMBERSHIP.initAndTest(this, initTask, initResult);

        var role = new RoleType()
                .name(ROLE_TESTERS_NAME)
                .assignment(ARCHETYPE_GROUP_ROLE.assignmentTo());
        roleTestersOid = addObject(role, initTask, initResult);
    }

    @Test
    public void test100PendingOperationsAccumulateOnPersistentConflict() throws Exception {
        loginAdministrator();

        var task = getTestTask();
        var result = task.getResult();

        given("a user whose membership addition is parked as a pending operation (network error)");
        var user = new UserType().name("dave");
        addObject(user, task, result);
        assignAccountToUser(user.getOid(), RESOURCE_DUMMY_MEMBERSHIP.oid, INTENT_DEFAULT, task, result);
        accountOf("dave").setModifyBreakMode(BreakMode.NETWORK);
        try {
            assignRole(user.getOid(), roleTestersOid, task, result);
        } catch (Exception e) {
            displayExpectedException(e);
        }
        var shadowOid = assertUserAfter(user.getOid())
                .singleLink()
                .getOid();
        assertRepoShadow(shadowOid)
                .pendingOperations()
                .assertOperations(1);

        and("the resource keeps answering AlreadyExists, while the membership is still missing there");
        accountOf("dave").setModifyBreakMode(BreakMode.CONFLICT);

        when("the user is repeatedly processed as the retry intervals elapse");
        int rounds = 4; // 1 round is enough technically, there's recursion [vilo]
        int[] totalCounts = new int[rounds];
        int[] openCounts = new int[rounds];
        for (int i = 0; i < rounds; i++) {
            clockForward("PT1H");
            reconcileUserWithTimeout(user.getOid(), task, shadowOid, i + 1);
            var pendingOperations = getShadowRepo(shadowOid).getBean().getPendingOperation();
            totalCounts[i] = pendingOperations.size();
            openCounts[i] = (int) pendingOperations.stream()
                    .filter(op -> op.getCompletionTimestamp() == null)
                    .count();
            displayValue("pending operations after round " + (i + 1),
                    totalCounts[i] + " total, " + openCounts[i] + " open");
        }

        then("the pending operations converge instead of accumulating");
        displayValue("total pending operations per round", Arrays.toString(totalCounts));
        displayValue("open pending operations per round", Arrays.toString(openCounts));
        assertRepoShadow(shadowOid)
                .display();
        assertThat(totalCounts[rounds - 1])
                .as("pending operations after round %d (vs. %d after round %d)",
                        rounds, totalCounts[rounds - 2], rounds - 1)
                .isEqualTo(totalCounts[rounds - 2]);
        // TODO assert open operations count and the final shadow/membership state

        and("cleanup: resetting break mode");
        accountOf("dave").setModifyBreakMode(BreakMode.NONE);
    }

    /**
     * Runs the reconciliation in a separate (daemon) thread with a timeout.
     */
    private void reconcileUserWithTimeout(String userOid, Task task, String shadowOid, int round) throws Exception {
        var unexpected = new AtomicReference<Throwable>();
        var thread = new Thread(() -> {
            try {
                login(userAdministrator);
                var roundResult = createOperationResult();
                try {
                    reconcileUser(userOid, task, roundResult);
                } catch (CommonException | RuntimeException e) {
                    displayExpectedException(e);
                }
            } catch (Throwable t) {
                unexpected.set(t);
            }
        }, "reconcile-round-" + round);
        thread.setDaemon(true);
        thread.start();
        thread.join(RECONCILE_TIMEOUT_MS);
        if (thread.isAlive()) {
            thread.interrupt();
            displayValue("pending operations when the timeout hit",
                    getShadowRepo(shadowOid).getBean().getPendingOperation().size());
            fail(("Reconciliation in round %d did not finish within %d ms").formatted(round, RECONCILE_TIMEOUT_MS));
        }
        if (unexpected.get() != null) {
            throw new AssertionError("Unexpected failure in reconciliation round " + round, unexpected.get());
        }
    }

    private DummyObject accountOf(String userName) throws Exception {
        return scenario.account.getByNameRequired(userName);
    }
}
