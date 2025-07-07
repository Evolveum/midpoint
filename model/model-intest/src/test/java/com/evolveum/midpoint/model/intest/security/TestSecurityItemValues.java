/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import java.util.function.Consumer;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.CaseWorkItemAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the security functions related to sub-object structures, like:
 *
 * . operations on individual items (e.g., case work items),
 * . visibility of individual items.
 *
 * For example, there may be a case with three work items with different operations and visibility because of different
 * relations to the current principal.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityItemValues extends AbstractInitializedSecurityTest {

    private static final TestObject<RoleType> ROLE_CASE_WORK_ITEMS_ASSIGNEE_SELF_READ = TestObject.file(
            TEST_DIR, "role-case-work-items-assignee-self-read.xml", "efa5620b-0cf1-492e-8aab-9de3358bb525");
    private static final TestObject<RoleType> ROLE_CASE_WORK_ITEMS_EVENT_APPROVED_READ = TestObject.file(
            TEST_DIR, "role-case-work-items-event-approved-read.xml", "6b2af0ae-36a6-4e90-8cff-1f2172566956");
    private static final TestObject<RoleType> ROLE_ACC_CERT_CASE_WORK_ITEMS_ASSIGNEE_SELF_READ = TestObject.file(
            TEST_DIR, "role-acc-cert-case-work-items-assignee-self-read.xml", "7090f927-d393-4dc6-aefb-d3c83408978b");
    private static final TestObject<RoleType> ROLE_ACC_CERT_CAMPAIGN_COMPLEX_READ = TestObject.file(
            TEST_DIR, "role-acc-cert-campaign-complex-read.xml", "2ce049c4-fd83-44d5-b5eb-19b4a156c262");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                ROLE_CASE_WORK_ITEMS_ASSIGNEE_SELF_READ,
                ROLE_CASE_WORK_ITEMS_EVENT_APPROVED_READ,
                ROLE_ACC_CERT_CASE_WORK_ITEMS_ASSIGNEE_SELF_READ,
                ROLE_ACC_CERT_CAMPAIGN_COMPLEX_READ);
    }

    @Test
    public void test100CaseWorkItemsAssigneeSelfRead() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_CASE_WORK_ITEMS_ASSIGNEE_SELF_READ.oid);
        assignRole(USER_JACK_OID, ROLE_CASE_WORK_ITEMS_EVENT_APPROVED_READ.oid);
        login(USER_JACK_USERNAME);

        then("can see all cases (because of 'all cases' object selector)");
        assertSearchCases(CASE1.oid, CASE2.oid, CASE3.oid, CASE4.oid);

        Consumer<CaseWorkItemAsserter<?,CaseWorkItemType>> workItemAsserter =
                a -> a.assertAssignees(USER_JACK_OID)
                        .assertItemsPresent(CaseWorkItemType.F_STAGE_NUMBER) // just one of the expected items
                        .assertNoItem(CaseWorkItemType.F_CREATE_TIMESTAMP); // this one is specifically excluded

        and("but not all their work items");
        // @formatter:off
        assertCase(CASE1.oid, "after")
                .workItems()
                    .assertNone() // none assigned to jack
                .end()
                .events()
                    .assertEvents(1); // only single is approved
        assertCase(CASE2.oid, "after")
                .workItems()
                    .assertNone() // none assigned to jack
                .end()
                .events()
                    .assertEvents(1); // only single is approved
        assertCase(CASE3.oid, "after")
                .workItems()
                    .single(workItemAsserter)
                .end()
                .events()
                    .assertEvents(1); // only single is approved
        assertCase(CASE4.oid, "after")
                .workItems()
                    .single(workItemAsserter)
                .end()
                .events()
                    .assertNone(); // none is approved
        // @formatter:on

        and("only allowed items are there");
        assertCaseAfter(CASE1.oid)
                .assertItems(CaseType.F_NAME, CaseType.F_EVENT);
        assertCaseAfter(CASE2.oid)
                .assertItems(CaseType.F_NAME, CaseType.F_EVENT);
        assertCaseAfter(CASE3.oid)
                .assertItems(CaseType.F_NAME, CaseType.F_WORK_ITEM, CaseType.F_EVENT);
        assertCaseAfter(CASE4.oid)
                .assertItems(CaseType.F_NAME, CaseType.F_WORK_ITEM);

        and("only allowed work items are returned by the search");
        var workItems = assertSearchWorkItems(2);
        assertWorkItems(workItems, "after")
                .by().workItemId(4L).find(workItemAsserter) // in case-3
                .by().workItemId(5L).find(workItemAsserter); // in case-4
    }

    @Test
    public void test110AccCertCaseWorkItemsAssigneeSelfRead() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ACC_CERT_CASE_WORK_ITEMS_ASSIGNEE_SELF_READ.oid);

        when();
        login(USER_JACK_USERNAME);

        then("can see all cert campaigns (because of 'all cases' object selector)");
        assertSearchCampaigns(CAMPAIGN1.oid, CAMPAIGN2.oid, CAMPAIGN3.oid, CAMPAIGN4.oid);

        and("but only allowed items are visible");
        // @formatter:off
        assertCampaignFullAfter(CAMPAIGN3.oid)
                .assertItems(AccessCertificationCampaignType.F_NAME, AccessCertificationCampaignType.F_CASE)
                .container(AccessCertificationCampaignType.F_CASE)
                    .valueWithId(6)
                        .assertItemsExactly(AccessCertificationCaseType.F_WORK_ITEM)
                        .containerSingle(AccessCertificationCaseType.F_WORK_ITEM)
                            .assertId(1L)
                            .assertItemsExactly(
                                    AccessCertificationWorkItemType.F_STAGE_NUMBER,
                                    AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                        .end()
                    .end()
                    .valueWithId(7)
                        .assertItemsExactly(AccessCertificationCaseType.F_WORK_ITEM)
                        .containerSingle(AccessCertificationCaseType.F_WORK_ITEM)
                            .assertId(2L)
                            .assertItemsExactly(
                                    AccessCertificationWorkItemType.F_NAME,
                                    AccessCertificationWorkItemType.F_STAGE_NUMBER,
                                    AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                        .end()
                    .end()
                    .assertSize(2); // no others!
        // @formatter:on
    }

    /**
     * Authorizations:
     *
     * - `campaign-name-and-case-outcome` - allows `name` and `case/outcome` for all cases i.e. 1, 2, 3, 4, 5, 6, 7, 8
     * - `campaign-case-iteration-for-object-ann` - allows `case/iteration` for cases 5, 6, 7, 8
     * - `no-campaign-case-outcome-and-iteration-for-target-approver` - denies `case/outcome` and `case/iteration` for case 6
     * - `no-campaign-case-for-object-administrator` - denies the whole case 1
     * - `campaign-work-items-assigned-to-administrator` - allows `case/workItem/assigneeRef` for work items
     *    1.1, 2.1, 3.1, 4.1, 5.1, 6.2, 7.1, 8.1; not apples to work items 6.1, 7.2
     * - `campaign-extra-work-item` - allows `case/workItem/*` (except for `stageNumber`) for work item 8.1
     */
    @Test
    public void test120AccCertCampaignComplexRead() throws Exception {
        given();
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ACC_CERT_CAMPAIGN_COMPLEX_READ.oid);

        when();
        login(USER_JACK_USERNAME);

        then("can see all cert campaigns (because of 'all cases' object selector)");
        assertSearchCampaigns(CAMPAIGN1.oid, CAMPAIGN2.oid, CAMPAIGN3.oid, CAMPAIGN4.oid);

        and("but only allowed items are visible");
        // @formatter:off
        assertCampaignFullAfter(CAMPAIGN3.oid)
                .assertItems(AccessCertificationCampaignType.F_NAME, AccessCertificationCampaignType.F_CASE)
                .container(AccessCertificationCampaignType.F_CASE)
                    .assertNoValueWithId(1L) // explicitly denied by "no-campaign-case-for-object-administrator"
                    .valueWithId(2L)
                        // allowed by "campaign-name-and-case-outcome", "campaign-work-items-assigned-to-administrator"
                        .assertItemsExactly(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationCaseType.F_OUTCOME)
                        .containerSingle(AccessCertificationCaseType.F_WORK_ITEM)
                            .assertId(1L)
                            .assertItemsExactly(AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                        .end()
                    .end()
                    .valueWithId(3) // the same as #2
                        .assertItemsExactly(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationCaseType.F_OUTCOME)
                        .containerSingle(AccessCertificationCaseType.F_WORK_ITEM)
                            .assertId(1L)
                            .assertItemsExactly(AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                        .end()
                    .end()
                    .valueWithId(4) // the same as #2
                        .assertItemsExactly(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationCaseType.F_OUTCOME)
                        .containerSingle(AccessCertificationCaseType.F_WORK_ITEM)
                            .assertId(1L)
                            .assertItemsExactly(AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                        .end()
                    .end()
                    .valueWithId(5)
                        // allowed by "campaign-name-and-case-outcome", "campaign-work-items-assigned-to-administrator"
                        // and "campaign-case-iteration-for-object-ann"
                        .assertItemsExactly(
                                AccessCertificationCaseType.F_WORK_ITEM,
                                AccessCertificationCaseType.F_OUTCOME,
                                AccessCertificationCaseType.F_ITERATION)
                        .containerSingle(AccessCertificationCaseType.F_WORK_ITEM)
                            .assertId(1L)
                            .assertItemsExactly(AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                        .end()
                    .end()
                    .valueWithId(6)
                        // as #5 but outcome and iteration denied by "no-campaign-case-outcome-and-iteration-for-target-approver"
                        .assertItemsExactly(AccessCertificationCaseType.F_WORK_ITEM)
                        .containerSingle(AccessCertificationCaseType.F_WORK_ITEM)
                            .assertId(2L)
                            .assertItemsExactly(AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                        .end()
                    .end()
                    .valueWithId(7)
                        // as #5
                        .assertItemsExactly(
                                AccessCertificationCaseType.F_WORK_ITEM,
                                AccessCertificationCaseType.F_OUTCOME,
                                AccessCertificationCaseType.F_ITERATION)
                        .containerSingle(AccessCertificationCaseType.F_WORK_ITEM)
                            .assertId(1L)
                            .assertItemsExactly(AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                        .end()
                    .end()
                    .valueWithId(8)
                        // as #5 but the work item is visible almost fully
                        .assertItemsExactly(
                                AccessCertificationCaseType.F_WORK_ITEM,
                                AccessCertificationCaseType.F_OUTCOME,
                                AccessCertificationCaseType.F_ITERATION)
                        .containerSingle(AccessCertificationCaseType.F_WORK_ITEM)
                            .assertId(1L)
                            .assertItemsExactly(
                                    AccessCertificationWorkItemType.F_NAME,
                                    AccessCertificationWorkItemType.F_ORIGINAL_ASSIGNEE_REF,
                                    AccessCertificationWorkItemType.F_ASSIGNEE_REF,
                                    AccessCertificationWorkItemType.F_ITERATION)
                        .end()
                    .end();
        // @formatter:on
    }
}
