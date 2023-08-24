/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.test;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.*;
import java.util.function.Consumer;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CLOSE_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

@ContextConfiguration(locations = {"classpath:ctx-certification-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestRoleInducementCertification extends AbstractCertificationTest {

    protected AccessCertificationDefinitionType certificationDefinition;

    private String campaignOid;

    @Test
    public void test010CreateCampaign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        certificationDefinition = repoAddObjectFromFile(ROLE_INDUCEMENT_CERT_DEF_FILE,
                AccessCertificationDefinitionType.class, result).asObjectable();

        // WHEN
        when();
        AccessCertificationCampaignType campaign =
                certificationManager.createCampaign(certificationDefinition.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getCampaignWithCases(campaignOid);
        display("campaign", campaign);
        assertSanityAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentCompleteAll(campaign, 100, 100, 100);
        assertPercentCompleteCurrent(campaign, 100, 100, 100);
    }

    @Test
    public void test012SearchAllCases() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }

    @Test
    public void test014Statistics() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(0, stat.getMarkedAsAccept());
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }

    @Test
    public void test020OpenFirstStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationManager.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);
        assertSanityAfterCampaignStart(campaign, certificationDefinition, 5);
        checkAllCasesSanity(campaign.getCase());

        List<AccessCertificationCaseType> caseList = campaign.getCase();
        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, null);            // 1 work item
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, null);            // 1 work item
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, ACCEPT, ACCEPT, null);      // 1 work item
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, null);            // 1 work item
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, null);      // no work items

        // no-work-items case is complete; others are not (=> 20%)
        // all are decided because of the default (=> 100%)
        // decisions done is 0 of 4
        assertPercentCompleteCurrent(campaign, 20, 100, 0);
        assertPercentCompleteCurrentIteration(campaign, 20, 100, 0);
        assertPercentCompleteAll(campaign, 20, 100, 0);
    }

    protected void checkAllCasesSanity(Collection<AccessCertificationCaseType> caseList) {
        assertEquals("Wrong number of certification cases", 5, caseList.size());
        checkCaseSanity(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, roleCeo);
        checkCaseSanity(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, roleCoo);
        checkCaseSanity(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, roleCoo);
        checkCaseSanity(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, roleCoo);
        checkCaseSanity(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, roleSuperuser);
    }

    @Test
    public void test024Statistics() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // all cases are marked as ACCEPT, because the outcome strategy is acceptedIfNotDenied, with a default of Accept
        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(5, stat.getMarkedAsAccept());
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }

    @Test
    public void test030SearchAllCases() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCasesSanity(caseList);
        AccessCertificationCaseType _case = checkCaseSanity(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, roleSuperuser);
        assertEquals("Unexpected number of reviewers in superuser case", 0, CertCampaignTypeUtil.getCurrentReviewers(_case).size());
    }

    @Test
    public void test050SearchDecisionsAdministrator() throws Exception {
        // GIVEN
        login(userAdministrator.asPrismObject());

        // Expected cases - phase 1:
        //
        //   COO-Dummy:                administrator
        //   COO-DummyBlack:           administrator
        //   COO-Superuser:            administrator

        executeWorkItemsSearchTest(workItems -> {
            display("workItems", workItems);
            assertEquals("Wrong number of certification work items", 3, workItems.size());
            checkWorkItemSanity(workItems, ROLE_COO_OID, RESOURCE_DUMMY_OID, roleCoo);
            checkWorkItemSanity(workItems, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, roleCoo);
            checkWorkItemSanity(workItems, ROLE_COO_OID, ROLE_SUPERUSER_OID, roleCoo);
        });
    }

    private void executeWorkItemsSearchTest(Consumer<Collection<AccessCertificationWorkItemType>> workItemsChecker)
            throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("searching for work items via certification service");
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(null, false, null, task, result);

        then("result is OK");
        assertSuccess(result);
        workItemsChecker.accept(workItems);

        when("searching for work items via model API");
        List<AccessCertificationWorkItemType> workItems2 =
                modelService.searchContainers(
                        AccessCertificationWorkItemType.class,
                        QueryUtils.filterForCertificationAssignees(
                                        prismContext.queryFor(AccessCertificationWorkItemType.class),
                                        SecurityUtil.getPrincipalRequired())
                                .and().item(F_CLOSE_TIMESTAMP).isNull()
                                .build(),
                        null, task, result);

        then("result is OK");
        result.recomputeStatus();
        assertSuccess(result);
        workItemsChecker.accept(workItems2);
    }

    @Test
    public void test051SearchDecisionsElaine() throws Exception {
        login(userElaine.asPrismObject());

        // Expected cases - phase 1:
        //
        // CEO-Dummy:                elaine

        executeWorkItemsSearchTest(workItems -> {
            display("workItems", workItems);
            assertEquals("Wrong number of work items", 1, workItems.size());
            checkWorkItemSanity(workItems, ROLE_CEO_OID, RESOURCE_DUMMY_OID, roleCeo);        });
    }

    @Test
    public void test052SearchDecisionsJack() throws Exception {
        login(userJack.asPrismObject());

        // Expected cases - phase 1: NONE

        executeWorkItemsSearchTest(workItems -> {
            display("workItems", workItems);
            assertEquals("Wrong number of certification work items", 0, workItems.size());
        });
    }

    /*
    Entering decisions:

    CEO-Dummy:                elaine          REVOKE
    COO-Dummy:                administrator   REVOKE
    COO-DummyBlack:           administrator   ACCEPT
    COO-Superuser:            administrator   NO-DECISION
    Superuser-Dummy:          -

     */

    @Test
    public void test100RecordDecisions() throws Exception {
        // GIVEN
        login(userAdministrator.asPrismObject());
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // WHEN
        when();

        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);

        recordDecision(campaignOid, ceoDummyCase, REVOKE, "no way", USER_ELAINE_OID, task, result);
        recordDecision(campaignOid, cooDummyCase, REVOKE, null, null, task, result);          // default reviewer - administrator
        recordDecision(campaignOid, cooDummyBlackCase, ACCEPT, "OK", USER_ADMINISTRATOR_OID, task, result);
        recordDecision(campaignOid, cooSuperuserCase, NOT_DECIDED, "I'm so procrastinative...", null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);

        assertSingleDecision(ceoDummyCase, REVOKE, "no way", 1, 1, USER_ELAINE_OID, REVOKE, false);
        assertSingleDecision(cooDummyCase, REVOKE, null, 1, 1, USER_ADMINISTRATOR_OID, REVOKE, false);
        assertSingleDecision(cooDummyBlackCase, ACCEPT, "OK", 1, 1, USER_ADMINISTRATOR_OID, ACCEPT, false);
        assertSingleDecision(cooSuperuserCase, NOT_DECIDED, "I'm so procrastinative...", 1, 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, null);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        // complete: 4 with WIs, 1 without WI (even NOT_DECIDED is an answer)
        // decided: all of them because of the strategy
        // decisions done: 4 of 4
        assertPercentCompleteCurrent(campaign, 100, 100, 100);
        assertPercentCompleteCurrentIteration(campaign, 100, 100, 100);
        assertPercentCompleteAll(campaign, 100, 100, 100);
    }

    @Test
    public void test110Statistics() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(3, stat.getMarkedAsAccept());
        assertEquals(2, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }


    @Test
    public void test150CloseFirstStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationManager.closeCurrentStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);
        assertSanityAfterStageClose(campaign, certificationDefinition, 1);
        checkAllCasesSanity(campaign.getCase());

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        assertSingleDecision(ceoDummyCase, REVOKE, "no way", 1, 1, USER_ELAINE_OID, REVOKE, true);
        assertSingleDecision(cooDummyCase, REVOKE, null, 1, 1, USER_ADMINISTRATOR_OID, REVOKE, true);
        assertSingleDecision(cooDummyBlackCase, ACCEPT, "OK", 1, 1, USER_ADMINISTRATOR_OID, ACCEPT, true);
        assertSingleDecision(cooSuperuserCase, NOT_DECIDED, "I'm so procrastinative...", 1, 1, USER_ADMINISTRATOR_OID, ACCEPT, true);
        assertNoDecision(superuserDummyCase, 1, 1, ACCEPT, true);

        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, 1);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, 1);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, 1);

        assertPercentCompleteCurrent(campaign, 100, 100, 100);
        assertPercentCompleteCurrentIteration(campaign, 100, 100, 100);
        assertPercentCompleteAll(campaign, 100, 100, 100);
    }

    @Test
    public void test160Statistics() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(3, stat.getMarkedAsAccept());
        assertEquals(2, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }

    @Test
    public void test200OpenSecondStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationManager.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 2", campaign);
        assertSanityAfterStageOpen(campaign, certificationDefinition, 2);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("Wrong number of certification cases", 5, caseList.size());
        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        assertCaseReviewers(ceoDummyCase, REVOKE, 1, singletonList(USER_ELAINE_OID));
        assertCaseReviewers(cooDummyCase, REVOKE, 1, singletonList(USER_ADMINISTRATOR_OID));
        assertCaseReviewers(cooDummyBlackCase, NO_RESPONSE, 2, Arrays.asList(USER_ADMINISTRATOR_OID, USER_ELAINE_OID));
        assertCaseReviewers(cooSuperuserCase, NO_RESPONSE, 2, singletonList(USER_ADMINISTRATOR_OID));
        assertCaseReviewers(superuserDummyCase, NO_RESPONSE, 2, Arrays.asList(USER_JACK_OID, USER_ADMINISTRATOR_OID));

        assertCaseHistoricOutcomes(ceoDummyCase, REVOKE);
        assertCaseHistoricOutcomes(cooDummyCase, REVOKE);
        assertCaseHistoricOutcomes(cooDummyBlackCase, ACCEPT);
        assertCaseHistoricOutcomes(cooSuperuserCase, ACCEPT);
        assertCaseHistoricOutcomes(superuserDummyCase, ACCEPT);

        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);                    // 0 work items in this stage
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);                    // 0 work items in this stage
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, NO_RESPONSE, NO_RESPONSE, null);    // 2 work items in this stage
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, NO_RESPONSE, NO_RESPONSE, null);          // 1 work item in this stage
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, NO_RESPONSE, NO_RESPONSE, null);    // 2 work items in this stage

        // current stage cases: 3 --> completed: 0, decided 0
        // work items completed: 0 of 5
        assertPercentCompleteCurrent(campaign, 0, 0, 0);
        // overall:
        // 2 of 5 (40%) of cases is completed - decisions are done
        // 2 of 5 (40%) is decided (2x "REVOKE" in stage 1 + 3x "NO_RESPONSE" in stage 2)
        // decisions: 4 of 4 in stage 1, 0 of 5 in stage 2 -> 4 of 9 in total (44%)
        assertPercentCompleteCurrentIteration(campaign, 40, 40, 44);
        assertPercentCompleteAll(campaign, 40, 40, 44);
    }

    @Test
    public void test210Statistics() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(0, stat.getMarkedAsAccept());
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(3, stat.getWithoutResponse());
    }

    @Test
    public void test220StatisticsAllStages() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, false, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(0, stat.getMarkedAsAccept());
        assertEquals(2, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(3, stat.getWithoutResponse());
    }

    @Test
    public void test250RecordDecisionsSecondStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // WHEN
        when();

        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        recordDecision(campaignOid, cooDummyBlackCase, ACCEPT, "OK", USER_ADMINISTRATOR_OID, task, result);
        recordDecision(campaignOid, cooDummyBlackCase, REVOKE, "Sorry", USER_ELAINE_OID, task, result);
        recordDecision(campaignOid, cooSuperuserCase, ACCEPT, null, USER_ADMINISTRATOR_OID, task, result);
        recordDecision(campaignOid, superuserDummyCase, ACCEPT, null, USER_JACK_OID, task, result);       // decision of administrator is missing here

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 2", campaign);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);

        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        assertWorkItemsCount(ceoDummyCase, 1);
        assertWorkItemsCount(cooDummyCase, 1);
        assertWorkItemsCount(cooDummyBlackCase, 3);
        assertWorkItemsCount(cooSuperuserCase, 2);
        assertWorkItemsCount(superuserDummyCase, 2);

        assertDecision2(cooDummyBlackCase, ACCEPT, "OK", 2, 1, USER_ADMINISTRATOR_OID, REVOKE);
        assertDecision2(cooDummyBlackCase, REVOKE, "Sorry", 2, 1, USER_ELAINE_OID, REVOKE);
        assertDecision2(cooSuperuserCase, ACCEPT, null, 2, 1, USER_ADMINISTRATOR_OID, ACCEPT);
        assertDecision2(superuserDummyCase, ACCEPT, null, 2, 1, USER_JACK_OID, NO_RESPONSE);
        assertDecision2(superuserDummyCase, null, null, 2, 1, USER_ADMINISTRATOR_OID, NO_RESPONSE);

        assertCaseHistoricOutcomes(ceoDummyCase, REVOKE);
        assertCaseHistoricOutcomes(cooDummyCase, REVOKE);
        assertCaseHistoricOutcomes(cooDummyBlackCase, ACCEPT);
        assertCaseHistoricOutcomes(cooSuperuserCase, ACCEPT);
        assertCaseHistoricOutcomes(superuserDummyCase, ACCEPT);

        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, NO_RESPONSE, NO_RESPONSE, null);

        /*
Subject-Target            Stage 1                       Stage 2                             Overall
===================================================================================================
CEO-Dummy:                elaine:RV -> RV               jack,administrator (skipped)        RV
COO-Dummy:                administrator:RV -> RV        jack,administrator (skipped)        RV
COO-DummyBlack:           administrator:A -> A          administrator:A,elaine:RV -> RV     RV
COO-Superuser:            administrator:ND -> A         administrator:A                     A
Superuser-Dummy:          - -> A                        jack:A,administrator:null -> NR     NR
         */

        // Current stage:
        //  - complete: 2 of 3
        //  - decided: 2 of 3
        //  - work items: 4 of 5
        assertPercentCompleteCurrent(campaign, 67, 67, 80);
        // Overall:
        //  - complete: 4 of 5
        //  - decided: 4 of 5
        //  - work items: 4 of 4 (stage 1) + 4 of 5 (stage 2)
        assertPercentCompleteCurrentIteration(campaign, 80, 80, 89);
        assertPercentCompleteAll(campaign, 80, 80, 89);
    }

    @Test
    public void test260Statistics() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(1, stat.getMarkedAsAccept());
        assertEquals(1, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(1, stat.getWithoutResponse());
    }

    @Test
    public void test290CloseSecondStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationManager.closeCurrentStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after closing stage 2", campaign);
        assertSanityAfterStageClose(campaign, certificationDefinition, 2);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("wrong # of cases", 5, caseList.size());
        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);
        assertCurrentState(ceoDummyCase, REVOKE, 1);
        assertCurrentState(cooDummyCase, REVOKE, 1);
        assertCurrentState(cooDummyBlackCase, REVOKE, 2);
        assertCurrentState(cooSuperuserCase, ACCEPT, 2);
        assertCurrentState(superuserDummyCase, NO_RESPONSE, 2);       // decision of administrator is missing here

        assertCaseHistoricOutcomes(ceoDummyCase, REVOKE);
        assertCaseHistoricOutcomes(cooDummyCase, REVOKE);
        assertCaseHistoricOutcomes(cooDummyBlackCase, ACCEPT, REVOKE);
        assertCaseHistoricOutcomes(cooSuperuserCase, ACCEPT, ACCEPT);
        assertCaseHistoricOutcomes(superuserDummyCase, ACCEPT, NO_RESPONSE);

        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, 1);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, 1);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, REVOKE, REVOKE, 2);
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, 2);
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, NO_RESPONSE, NO_RESPONSE, 2);

        // Current stage:
        //  - complete: 2 of 3
        //  - decided: 2 of 3
        //  - work items: 4 of 5
        assertPercentCompleteCurrent(campaign, 67, 67, 80);
        // Overall:
        //  - complete: 4 of 5
        //  - decided: 4 of 5
        //  - work items: 4 of 4 (stage 1) + 4 of 5 (stage 2)
        assertPercentCompleteCurrentIteration(campaign, 80, 80, 89);
        assertPercentCompleteAll(campaign, 80, 80, 89);
    }

    @Test
    public void test300StartRemediation() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationManager.startRemediation(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation start", campaign);
        assertTrue("wrong campaign state: " + campaign.getState(), campaign.getState() == CLOSED || campaign.getState() == IN_REMEDIATION);

        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(campaign.getOid())
                .build();
        List<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, query, null, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation finished", campaign);
        assertEquals("wrong campaign state", CLOSED, campaign.getState());
        assertEquals("wrong campaign stage", 3, campaign.getStageNumber());
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertApproximateTime("end time", new Date(), campaign.getEndTimestamp());
        assertEquals("wrong # of stages", 2, campaign.getStage().size());

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("wrong # of cases", 5, caseList.size());
        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);
        assertApproximateTime("ceoDummyCase.remediedTimestamp", new Date(), ceoDummyCase.getRemediedTimestamp());
        assertApproximateTime("cooDummyCase.remediedTimestamp", new Date(), cooDummyCase.getRemediedTimestamp());
        assertApproximateTime("cooDummyBlackCase.remediedTimestamp", new Date(), cooDummyBlackCase.getRemediedTimestamp());

        roleCeo = getRole(ROLE_CEO_OID).asObjectable();
        display("roleCeo", roleCeo);
        assertEquals("wrong # of CEO's inducements", 0, roleCeo.getInducement().size());

        roleCoo = getRole(ROLE_COO_OID).asObjectable();
        display("roleCoo", roleCoo);
        assertEquals("wrong # of COO's inducements", 1, roleCoo.getInducement().size());
        assertEquals("wrong OID of remaining COO inducement", ROLE_SUPERUSER_OID, roleCoo.getInducement().get(0).getTargetRef().getOid());

        PrismObject<AccessCertificationDefinitionType> def = getObject(AccessCertificationDefinitionType.class, certificationDefinition.getOid());
        assertApproximateTime("last campaign closed", new Date(), def.asObjectable().getLastCampaignClosedTimestamp());

        // Current stage:
        //  - complete: 2 of 3
        //  - decided: 2 of 3
        //  - work items: 4 of 5
        assertPercentCompleteCurrent(campaign, 67, 67, 80);
        // Overall:
        //  - complete: 4 of 5
        //  - decided: 4 of 5
        //  - work items: 4 of 4 (stage 1) + 4 of 5 (stage 2)
        assertPercentCompleteCurrentIteration(campaign, 80, 80, 89);
        assertPercentCompleteAll(campaign, 80, 80, 89);
    }

    @Test
    public void test310Statistics() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, false, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(1, stat.getMarkedAsAccept());
        assertEquals(3, stat.getMarkedAsRevoke());
        assertEquals(3, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(1, stat.getWithoutResponse());
    }

    @Test
    public void test320CheckAfterClose() throws Exception {
        login(userAdministrator.asPrismObject());

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        waitForCampaignTasks(campaignOid, 20000, result);

        // THEN
        roleCoo = getRole(ROLE_COO_OID).asObjectable();
        display("COO", roleCoo);
        AssignmentType inducement = findInducementByTarget(ROLE_COO_OID, ROLE_SUPERUSER_OID);
        assertCertificationMetadata(inducement.getMetadata(), SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT, singleton(USER_ADMINISTRATOR_OID), singleton("administrator: I'm so procrastinative..."));
    }
}
