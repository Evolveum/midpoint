/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import java.io.File;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.test.DummyTestResource;

import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests mapping strength (strong, normal/relativistic, weak) in various scenarios.
 *
 * Not a comprehensive test! Just some random scenarios to diagnose data "flips" in #10980.
 *
 * Later, we may extend this test as needed.
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMappingStrength extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "mapping-strength");

    private static final DummyTestResource DUMMY_RESOURCE_NORMAL = new DummyTestResource(
            TEST_DIR, "resource-dummy-normal.xml", "170709ab-ab58-4685-ad1c-6739ecf83b35", "normal");
    private static final DummyTestResource DUMMY_RESOURCE_STRONG = new DummyTestResource(
            TEST_DIR, "resource-dummy-strong.xml", "a9e934ec-1b49-4cd0-89da-d7f5e55858a4", "strong");
    private static final TestTask TASK_LIVE_SYNC_STRONG = new TestTask(
            TEST_DIR, "task-live-sync-strong.xml", "728b230b-a923-4459-b07e-e7622ac6b8b0");
    private static final String NORMAL_FULLNAME = "Jim Normal";
    private static final String STRONG_FULLNAME = "Jim Strong";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initAndTestDummyResource(DUMMY_RESOURCE_NORMAL, initTask, initResult);
        initAndTestDummyResource(DUMMY_RESOURCE_STRONG, initTask, initResult);

        DUMMY_RESOURCE_STRONG.getDummyResource().setSyncStyle(DummySyncStyle.SMART); // we are going to use live sync here

        initTestObjects(initTask, initResult, TASK_LIVE_SYNC_STRONG);
    }

    /**
     * Recomputes a user that has accounts on both `normal` and `strong` resources, with conflicting `fullname` attribute values.
     *
     * == Background and overview of the scenario
     *
     * . The `normal` resource has a normal (relativistic) mapping for `ri:fullname` attribute to `fullName` property on the user.
     * There is an account on this resource with the `fullname` attribute set to `Jim Normal`.
     * . The `strong` resource has a strong mapping for `ri:fullname` attribute to `fullName` property on the user.
     * There is an account on this resource with the `fullname` attribute set to `Jim Strong`.
     * . When recomputing a user with accounts on both resources, the `fullName` property "flips" between the two values
     * (`Jim Normal` and `Jim Strong`) on each recomputation.
     *
     * ---
     *
     * == More details
     *
     * There are two flips of `fullName` property (on the user) observed during the recomputation:
     *
     * . Initial value is `Jim Normal` (it is set explicitly to have a known starting point).
     * . The recompute changes it to `Jim Strong` during the first `SECONDARY` clockwork click (number 3 globally).
     * . It is changed back to `Jim Normal` in the second `SECONDARY` clockwork click (number 4 globally).
     *
     * The reason for the two flips is that:
     *
     * . In the first Projector run (the clockwork click number 1):
     * .. Both mappings are executed, regardless of whether they are strong or normal.
     * .. Each of them produces the respective value (`Jim Strong` or `Jim Normal`).
     * .. What follows is the consolidation that tries to produce a delta from the output of the mappings, taking existing value
     * into account. The input to the consolidation is:
     * ... Existing value on the user: `Jim Normal`
     * ... Values from the mappings: `Jim Strong` and `Jim Normal` (doesn't matter which is from what mapping strength)
     * ... The consolidation code then decides to ADD the value that is not present on the user yet, as is logical especially
     * for multivalued properties. So the resulting delta is to ADD `Jim Strong`.
     * .. The delta is applied (in the clockwork click number 3), changing the value on the user to `Jim Strong`.
     * The original value is rewritten, as this is the defined behavior for adding values to single-valued properties.
     * This is the first flip.
     * . In the second Projector run (the clockwork click number 4):
     * .. the mappings are executed again (resulting in the same values as before),
     * .. the consolidation is performed again (this time adds `Jim Normal` because it is NOT in the user)
     * .. the delta is applied, overwriting the value on the user back to `Jim Normal`.
     *
     * The behavior is not ideal, but it is there for a long time.
     */
    @Test
    public void test100RecomputationWithConflictingDataWithJimNormalStartingPoint() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var name = getTestNameShort();

        var user = initializeAccountsAndUser(name, result);

        when("fullname is reset to 'normal' to provide a known starting point");
        setFullNameRaw(user.getOid(), NORMAL_FULLNAME, result);

        and("user is recomputed (with reconciliation)");
        dummyAuditService.clear();
        reconcileUser(user.getOid(), task, result);

        then("username is 'normal' (after two flips)");
        assertUserAfter(user.getOid())
                .assertFullName(NORMAL_FULLNAME);

        and("there is the 'flip' visible in audit records");
        assertFlip();
    }

    /**
     * The same as {@link #test100RecomputationWithConflictingDataWithJimNormalStartingPoint()} but using a different
     * starting value for `fullName`. This demonstrates that the behavior is consistent regardless of the starting
     * point and strong vs normal mapping strength.
     */
    @Test
    public void test110RecomputationWithConflictingDataWithJimStrongStartingPoint() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var name = getTestNameShort();

        var user = initializeAccountsAndUser(name, result);

        when("fullname is reset to 'strong' to provide a known starting point");
        setFullNameRaw(user.getOid(), STRONG_FULLNAME, result);

        and("user is recomputed (with reconciliation)");
        dummyAuditService.clear();
        reconcileUser(user.getOid(), task, result);

        then("username is 'normal' (after two flips)");
        assertUserAfter(user.getOid())
                .assertFullName(STRONG_FULLNAME);

        and("there is the 'flip' visible in audit records");
        assertFlip();
    }

    /**
     * The same as {@link #test100RecomputationWithConflictingDataWithJimNormalStartingPoint()} but using a starting value
     * for `fullName` different from both `Jim Normal` and `Jim Strong`. Now the consolidation cannot produce a delta, because
     * it would have to create ADD delta with two values for a single-valued property. This results in a {@link SchemaException}.
     */
    @Test
    public void test120RecomputationWithConflictingDataWithDifferentStartingPoint() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var name = getTestNameShort();

        var user = initializeAccountsAndUser(name, result);

        when("fullname is reset to 'strong' to provide a known starting point");
        setFullNameRaw(user.getOid(), "different", result);

        and("user is recomputed (with reconciliation)");
        try {
            reconcileUser(user.getOid(), task, result);
            fail("SchemaException was expected but not thrown");
        } catch (SchemaException e) {
            assertExpectedException(e)
                    .hasMessageContaining("Attempt to put more than one value to single-valued item");
        }
    }

    /**
     * As {@link #test100RecomputationWithConflictingDataWithJimNormalStartingPoint()} but using import instead of recompute.
     *
     * In order to get the flipping behavior, we must make sure both accounts are fully loaded.
     * This is achieved by having strong outbound mappings (for `icfs:name` attribute) on both.
     *
     * Here the setup is crucial:
     *
     * . We start with `fullName` set to `Jim Normal`.
     * . We import the account from `strong` resource.
     * . The first Projector run (clockwork click number 1) creates ADD delta to `Jim Strong` and, at the same time, loads
     * the account from `normal` resource fully (because of the strong outbound mapping).
     * . The second Projector run then creates ADD delta to `Jim Normal` (because now it sees the fully loaded account
     * from `normal` resource).
     *
     * If we imported from the `normal` resource first, the behavior would be different. The first projector run would
     * not switch the name to `Jim Strong`, because the account from the strong resource is not loaded at that time.
     */
    @Test
    public void test130ImportWithConflictingData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var name = getTestNameShort();

        var user = initializeAccountsAndUser(name, result);

        when("fullname is reset to 'normal' to provide a known starting point");
        setFullNameRaw(user.getOid(), NORMAL_FULLNAME, result);

        and("account from 'strong' resource is re-imported");
        dummyAuditService.clear();
        importAccountsRequest()
                .withResourceOid(DUMMY_RESOURCE_STRONG.oid)
                .withDefaultAccountType()
                .withNameValue(name)
                .executeOnForeground(result);

        then("username is 'normal' (after two flips)");
        assertUserAfter(user.getOid())
                .assertFullName(NORMAL_FULLNAME);

        and("there is the 'flip' visible in audit records");
        assertFlip();
    }

    /**
     * As {@link #test130ImportWithConflictingData()} but using live sync instead of import.
     */
    @Test
    public void test140LiveSyncWithConflictingData() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var name = getTestNameShort();

        var user = initializeAccountsAndUser(name, result);

        when("fullname is reset to 'normal' to provide a known starting point");
        setFullNameRaw(user.getOid(), NORMAL_FULLNAME, result);

        and("account from 'strong' resource is live synced");
        TASK_LIVE_SYNC_STRONG.rerun(result); // initial run: obtaining the sync state (token)
        DUMMY_RESOURCE_STRONG.getDummyResource().getAccountByName(name)
                .addAttributeValue(DummyAccount.ATTR_PRIVILEGES_NAME, "whatever"); // trigger live sync event
        dummyAuditService.clear();
        TASK_LIVE_SYNC_STRONG.rerun(result); // obtaining the change

        then("username is 'normal' (after two flips)");
        assertUserAfter(user.getOid())
                .assertFullName(NORMAL_FULLNAME);

        and("there is the 'flip' visible in audit records");
        assertFlip();
    }

    /**
     * Creates a user with two accounts: one on normal resource, one on strong resource.
     * (Technically, accounts are created first and then imported into midPoint.)
     */
    private PrismObject<UserType> initializeAccountsAndUser(String name, OperationResult result) throws Exception {
        given("accounts with conflicting fullname");
        DUMMY_RESOURCE_NORMAL.addAccount(name)
                .addAttributeValue(DummyAccount.ATTR_FULLNAME_NAME, NORMAL_FULLNAME);
        DUMMY_RESOURCE_STRONG.addAccount(name)
                .addAttributeValue(DummyAccount.ATTR_FULLNAME_NAME, STRONG_FULLNAME);

        when("accounts are imported");
        importAccountsRequest()
                .withResourceOid(DUMMY_RESOURCE_NORMAL.oid)
                .withDefaultAccountType()
                .withNameValue(name)
                .executeOnForeground(result);
        importAccountsRequest()
                .withResourceOid(DUMMY_RESOURCE_STRONG.oid)
                .withDefaultAccountType()
                .withNameValue(name)
                .executeOnForeground(result);
        return assertUserByUsername(name, "after import")
                .display()
                .getObject();
    }

    /** We set the full name in raw mode, to avoid executing any mappings. */
    private void setFullNameRaw(String userOid, String value, OperationResult result) throws CommonException {
        repositoryService.modifyObject(
                UserType.class,
                userOid,
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_FULL_NAME).replace(new PolyString(value))
                        .asItemDeltas(),
                null,
                result);
    }

    private void assertFlip() {
        displayDumpable("audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(2);
        // TODO assert actual flip in the records
    }
}
