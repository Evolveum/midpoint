/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.task.ActivityBasedTaskHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Tests itemSelectionExpression and useSubjectManager.
 */
@ContextConfiguration(locations = {"classpath:ctx-certification-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCriticalRolesCertification extends AbstractCertificationTest {

    private static final File CERT_DEF_FILE = new File(COMMON_DIR, "certification-of-critical-roles.xml");

    @Autowired protected ActivityBasedTaskHandler activityBasedTaskHandler;

    private AccessCertificationDefinitionType certificationDefinition;

    private String campaignOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        assignRole(USER_JACK_OID, ROLE_CTO_OID);
        userJack = getObjectViaRepo(UserType.class, USER_JACK_OID).asObjectable();
        activityBasedTaskHandler.setAvoidAutoAssigningArchetypes(false);
    }

    @Test
    public void test010CreateCampaign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        certificationDefinition = repoAddObjectFromFile(CERT_DEF_FILE,
                AccessCertificationDefinitionType.class, result).asObjectable();
        dummyTransport.clearMessages();

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

        displayDumpable("dummy transport", dummyTransport);
    }

    /*
Expected cases, reviewers and decisions/outcomes:

CEO = 00000000-d34d-b33f-f00d-000000000001
COO = 00000000-d34d-b33f-f00d-000000000002

Stage1: oneAcceptAccepts, default: accept, stop on: revoke          (manager)

Case                        Stage1
================================================
elaine->CEO                 none (A) -> A
guybrush->COO               cheese: A -> A (in test100)
administrator->COO          none (A) -> A
administrator->CEO          none (A) -> A
jack->CEO                   none (A) -> A
jack->CTO                   none (A) -> A
     */

    @Test
    public void test020OpenFirstStage() throws Exception {
        // GIVEN
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        // WHEN
        when();
        certificationService.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getFirstStageTasks(campaignOid, startTime, result);
        //getNextStageTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);
        assertSanityAfterCampaignStart(campaign, certificationDefinition, 6);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("unexpected # of cases", 6, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

        checkCaseAssignmentSanity(elaineCeoCase, userElaine);
        checkCaseAssignmentSanity(guybrushCooCase, userGuybrush);
        checkCaseAssignmentSanity(administratorCeoCase, userAdministrator);
        checkCaseAssignmentSanity(administratorCooCase, userAdministrator);
        checkCaseAssignmentSanity(jackCeoCase, userJack);
        checkCaseAssignmentSanity(jackCtoCase, userJack);

        assertCaseReviewers(elaineCeoCase, ACCEPT, 1, emptyList());
        assertCaseReviewers(guybrushCooCase, NO_RESPONSE, 1, singletonList(USER_CHEESE_OID));
        assertCaseReviewers(administratorCooCase, ACCEPT, 1, emptyList());
        assertCaseReviewers(administratorCeoCase, ACCEPT, 1, emptyList());
        assertCaseReviewers(jackCeoCase, ACCEPT, 1, emptyList());
        assertCaseReviewers(jackCtoCase, ACCEPT, 1, emptyList());

        assertCaseOutcome(caseList, USER_ELAINE_OID, ROLE_CEO_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CTO_OID, ACCEPT, ACCEPT, null);

        // complete: 5 of 6 (5 cases have no work items so they are complete)
        // decided: 5 of 6 (accept because of default)
        // decisions done: 0 of 1
        assertPercentCompleteCurrent(campaign, 83, 83, 0);
        assertPercentCompleteCurrentIteration(campaign, 83, 83, 0);
        assertPercentCompleteAll(campaign, 83, 83, 0);

        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test100RecordDecisions1() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // WHEN
        when();

        assertEquals("unexpected # of cases", 6, caseList.size());
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);

        recordDecision(campaignOid, guybrushCooCase, ACCEPT, null, USER_CHEESE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);

        assertEquals("unexpected # of cases", 6, caseList.size());
        guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);

        assertSingleDecision(guybrushCooCase, ACCEPT, null, 1, 1, USER_CHEESE_OID, ACCEPT, false);

        assertCaseOutcome(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID, ACCEPT, ACCEPT, null);

        // complete: 6 of 6 (5 without WIs, 1 with completed WI)
        // decided: 6 of 6 (5 without WIs, 1 with completed WI)
        // decisions done: 1 of 1
        assertPercentCompleteCurrent(campaignOid, 100, 100, 100);
        assertPercentCompleteCurrentIteration(campaignOid, 100, 100, 100);
        assertPercentCompleteAll(campaignOid, 100, 100, 100);
        assertCasesCount(campaignOid, 6);
    }

    @Test
    public void test150CloseFirstStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

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

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("unexpected # of cases", 6, caseList.size());

        assertCaseOutcome(caseList, USER_ELAINE_OID, ROLE_CEO_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CTO_OID, ACCEPT, ACCEPT, 1);

        assertPercentCompleteCurrent(campaignOid, 100, 100, 100);
        assertPercentCompleteCurrentIteration(campaignOid, 100, 100, 100);
        assertPercentCompleteAll(campaignOid, 100, 100, 100);
        assertCasesCount(campaignOid, 6);
        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test200OpenSecondStage() throws Exception {
        // GIVEN
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        // WHEN
        when();
        certificationService.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getNextStageTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 2", campaign);
        assertSanityAfterStageOpen(campaign, certificationDefinition, 2);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("Wrong number of certification cases", 6, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

        /*
Stage2: allMustAccept, default: accept, advance on: accept          (target owner)

Case                        Stage1              Stage2
=============================================================
elaine->CEO                 none (A) -> A       elaine
guybrush->COO               cheese: A -> A      admin
administrator->COO          none (A) -> A       admin
administrator->CEO          none (A) -> A       elaine
jack->CEO                   none (A) -> A       elaine
jack->CTO                   none (A) -> A       none (A) -> A
         */

        assertCaseReviewers(elaineCeoCase, NO_RESPONSE, 2, singletonList(USER_ELAINE_OID));
        assertCaseReviewers(guybrushCooCase, NO_RESPONSE, 2, singletonList(USER_ADMINISTRATOR_OID));
        assertCaseReviewers(administratorCooCase, NO_RESPONSE, 2, singletonList(USER_ADMINISTRATOR_OID));
        assertCaseReviewers(administratorCeoCase, NO_RESPONSE, 2, singletonList(USER_ELAINE_OID));
        assertCaseReviewers(jackCeoCase, NO_RESPONSE, 2, singletonList(USER_ELAINE_OID));
        assertCaseReviewers(jackCtoCase, ACCEPT, 2, emptyList());

        assertCaseOutcome(caseList, USER_ELAINE_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CTO_OID, ACCEPT, ACCEPT, null);

        // Current stage:
        //  - cases: 6, complete: 1 (no work items), decided: 1 (default outcome)
        //  - work items: 0 of 1 done
        assertPercentCompleteCurrent(campaign, 17, 17, 0);
        // All stages:
        //  - cases: 6, complete: 1 (no work items), decided: 1 (default outcome)
        //  - work items: 1/1 + 0/5 = 1/6 = 17%
        assertPercentCompleteCurrentIteration(campaign, 17, 17, 17);
        assertPercentCompleteAll(campaign, 17, 17, 17);
        assertCasesCount(campaignOid, 6);

        displayDumpable("dummy transport", dummyTransport);
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
        assertEquals(1, stat.getMarkedAsAccept());
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(5, stat.getWithoutResponse());
    }

    @Test
    public void test250RecordDecisionsSecondStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // WHEN
        when();

/*
Stage2: allMustAccept, default: accept, advance on: accept          (target owner)

Overall: allMustAccept

owners: CEO: elaine, COO: administrator, CTO: none

Case                        Stage1              Stage2
=================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A
administrator->CEO          none (A) -> A       elaine: A -> A            | A
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A

*/

        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);

        recordDecision(campaignOid, elaineCeoCase, ACCEPT, null, USER_ELAINE_OID, task, result);
        recordDecision(campaignOid, guybrushCooCase, REVOKE, "no", USER_ADMINISTRATOR_OID, task, result);
        recordDecision(campaignOid, administratorCooCase, ACCEPT, "ok", USER_ADMINISTRATOR_OID, task, result);
        recordDecision(campaignOid, administratorCeoCase, ACCEPT, null, USER_ELAINE_OID, task, result);
        // jackCeo: no response
        // jackCto: no reviewers

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 2", campaign);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);

        elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

        assertWorkItemsCount(elaineCeoCase, 1);
        assertWorkItemsCount(guybrushCooCase, 2);
        assertWorkItemsCount(administratorCooCase, 1);
        assertWorkItemsCount(administratorCeoCase, 1);
        assertWorkItemsCount(jackCeoCase, 1);
        assertWorkItemsCount(jackCtoCase, 0);

        assertSingleDecision(elaineCeoCase, ACCEPT, null, 2, 1, USER_ELAINE_OID, ACCEPT, false);
        assertSingleDecision(guybrushCooCase, REVOKE, "no", 2, 1, USER_ADMINISTRATOR_OID, REVOKE, false);
        assertSingleDecision(administratorCooCase, ACCEPT, "ok", 2, 1, USER_ADMINISTRATOR_OID, ACCEPT, false);
        assertSingleDecision(administratorCeoCase, ACCEPT, null, 2, 1, USER_ELAINE_OID, ACCEPT, false);
        assertNoDecision(jackCeoCase, 2, 1, NO_RESPONSE, false);
        assertNoDecision(jackCtoCase, 2, 1, ACCEPT, false);

        // Current stage:
        //  - cases: 6, complete: 5 (1x no work items, 4x outcome), decided: 5
        //  - work items: 4 of 5 done
        assertPercentCompleteCurrent(campaign, 83, 83, 80);
        // All stages:
        //  - cases: 6, complete: 5 (all except jack->CEO), decided: 5 (all except jack->CEO)
        //  - work items: 1/1 + 4/5 = 5/6 = 83%
        assertPercentCompleteCurrentIteration(campaign, 83, 83, 83);
        assertPercentCompleteAll(campaign, 83, 83, 83);
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
        assertEquals(4, stat.getMarkedAsAccept());
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
        dummyTransport.clearMessages();

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
        assertEquals("wrong # of cases", 6, caseList.size());

        assertCaseOutcome(caseList, USER_ELAINE_OID, ROLE_CEO_OID, ACCEPT, ACCEPT, 2);
        assertCaseOutcome(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID, REVOKE, REVOKE, 2);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, ACCEPT, ACCEPT, 2);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, ACCEPT, ACCEPT, 2);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, 2);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CTO_OID, ACCEPT, ACCEPT, 2);

        // Current stage:
        //  - cases: 6, complete: 5 (1x no work items, 4x outcome), decided: 5
        //  - work items: 4 of 5 done
        assertPercentCompleteCurrent(campaign, 83, 83, 80);
        // All stages:
        //  - cases: 6, complete: 5 (all except jack->CEO), decided: 5 (all except jack->CEO)
        //  - work items: 1/1 + 4/5 = 5/6 = 83%
        assertPercentCompleteCurrentIteration(campaign, 83, 83, 83);
        assertPercentCompleteAll(campaign, 83, 83, 83);
        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test300OpenThirdStage() throws Exception {
        // GIVEN
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        // WHEN
        when();
        certificationService.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getNextStageTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 3", campaign);
        assertSanityAfterStageOpen(campaign, certificationDefinition, 3);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("Wrong number of certification cases", 6, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

        /*
Stage3: oneDenyDenies, stop on: not decided

Overall: allMustAccept

owners: CEO: elaine, COO: administrator, CTO: none

Case                        Stage1              Stage2                           Stage3
=====================================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A    elaine,administrator
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A    elaine,administrator
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine,administrator
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine,administrator
         */

        assertCaseReviewers(elaineCeoCase, NO_RESPONSE, 3, asList(USER_ELAINE_OID, USER_ADMINISTRATOR_OID));
        assertCaseReviewers(guybrushCooCase, REVOKE, 2, singletonList(USER_ADMINISTRATOR_OID));
        assertCaseReviewers(administratorCooCase, NO_RESPONSE, 3, asList(USER_ELAINE_OID, USER_ADMINISTRATOR_OID));
        assertCaseReviewers(administratorCeoCase, NO_RESPONSE, 3, asList(USER_ELAINE_OID, USER_ADMINISTRATOR_OID));
        assertCaseReviewers(jackCeoCase, NO_RESPONSE, 2, singletonList(USER_ELAINE_OID));
        assertCaseReviewers(jackCtoCase, NO_RESPONSE, 3, asList(USER_ELAINE_OID, USER_ADMINISTRATOR_OID));

        assertCaseOutcome(caseList, USER_ELAINE_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CTO_OID, NO_RESPONSE, NO_RESPONSE, null);

        // Current stage:
        //  - cases: 4, complete: 0, decided: 0
        //  - work items: 0 of 8 done
        assertPercentCompleteCurrent(campaign, 0, 0, 0);
        // All stages:
        //  - cases: 6, complete: 1 (only guybrush->COO), decided: 1 (guybrush->COO)
        //  - work items: 1/1 + 4/5 + 0/8 = 5/14 = 36%
        assertPercentCompleteCurrentIteration(campaign, 17, 17, 36);
        assertPercentCompleteAll(campaign, 17, 17, 36);

        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test330RecordDecisionsThirdStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        dummyTransport.clearMessages();

        // WHEN
        when();

/*
Case                        Stage1              Stage2                           Stage3
==================================================================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A    elaine:null,administrator:ND -> ND  [STOP] | ND
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A    elaine:A,administrator:null -> A           | A
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine:NR,administrator:NR -> NR           | NR
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine:null,administrator:null -> NR       | NR

*/

        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase;
        AccessCertificationCaseType administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase;
        AccessCertificationCaseType jackCtoCase;

        recordDecision(campaignOid, elaineCeoCase, NOT_DECIDED, null, USER_ADMINISTRATOR_OID, task, result);
        recordDecision(campaignOid, administratorCooCase, ACCEPT, null, USER_ELAINE_OID, task, result);
        recordDecision(campaignOid, administratorCeoCase, NO_RESPONSE, null, USER_ELAINE_OID, task, result);
        recordDecision(campaignOid, administratorCeoCase, NO_RESPONSE, null, USER_ADMINISTRATOR_OID, task, result);
        // no response for jackCto

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 3", campaign);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);

        elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

        assertWorkItemsCount(elaineCeoCase, 3);
        assertWorkItemsCount(guybrushCooCase, 2);
        assertWorkItemsCount(administratorCooCase, 3);
        assertWorkItemsCount(administratorCeoCase, 3);
        assertWorkItemsCount(jackCeoCase, 1);
        assertWorkItemsCount(jackCtoCase, 2);

        assertReviewerDecision(elaineCeoCase, NOT_DECIDED, null, 3, 1, USER_ADMINISTRATOR_OID, NOT_DECIDED, false);
        assertNoDecision(guybrushCooCase, 3, 1, REVOKE, false);
        assertReviewerDecision(administratorCooCase, ACCEPT, null, 3, 1, USER_ELAINE_OID, ACCEPT, false);
        assertReviewerDecision(administratorCooCase, null, null, 3, 1, USER_ADMINISTRATOR_OID, ACCEPT, false);
        assertReviewerDecision(administratorCeoCase, null, null, 3, 1, USER_ELAINE_OID, NO_RESPONSE, false);
        assertReviewerDecision(administratorCeoCase, null, null, 3, 1, USER_ADMINISTRATOR_OID, NO_RESPONSE, false);
        assertNoDecision(jackCeoCase, 3, 1, NO_RESPONSE, false);
        assertReviewerDecision(jackCtoCase, null, null, 3, 1, USER_ELAINE_OID, NO_RESPONSE, false);
        assertReviewerDecision(jackCtoCase, null, null, 3, 1, USER_ADMINISTRATOR_OID, NO_RESPONSE, false);

        /*
Case                        Stage1              Stage2                           Stage3
==================================================================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A    elaine:null,administrator:ND -> ND  [STOP] | ND
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A    elaine:A,administrator:null -> A           | A
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine:NR,administrator:NR -> NR           | NR
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine:null,administrator:null -> NR       | NR

*/

        assertCaseOutcome(caseList, USER_ELAINE_OID, ROLE_CEO_OID, NOT_DECIDED, NOT_DECIDED, null);
        assertCaseOutcome(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CTO_OID, NO_RESPONSE, NO_RESPONSE, null);

        // Current stage:
        //  - cases: 4, complete: 0, decided: 1
        //  - work items: 2 of 8 done
        assertPercentCompleteCurrent(campaign, 0, 25, 25);
        // All stages:
        //  - cases: 6, complete: 1 (only guybrush->COO), decided: 2 (guybrush->COO, admin->COO)
        //  - work items: 1/1 + 4/5 + 2/8 = 7/14 = 50%
        assertPercentCompleteCurrentIteration(campaign, 17, 33, 50);
        assertPercentCompleteAll(campaign, 17, 33, 50);

        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test390CloseThirdStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        // WHEN
        when();
        certificationManager.closeCurrentStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after closing stage 3", campaign);
        assertSanityAfterStageClose(campaign, certificationDefinition, 3);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("wrong # of cases", 6, caseList.size());

        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

                /*
Case                        Stage1              Stage2                           Stage3
==================================================================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A    elaine:null,administrator:ND -> ND  [STOP] | ND
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A    elaine:A,administrator:null -> A           | A
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine:NR,administrator:NR -> NR           | NR
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine:null,administrator:null -> NR       | NR

*/

        assertCaseHistoricOutcomes(elaineCeoCase, ACCEPT, ACCEPT, NOT_DECIDED);
        assertCaseHistoricOutcomes(guybrushCooCase, ACCEPT, REVOKE);
        assertCaseHistoricOutcomes(administratorCooCase, ACCEPT, ACCEPT, ACCEPT);
        assertCaseHistoricOutcomes(administratorCeoCase, ACCEPT, ACCEPT, NO_RESPONSE);
        assertCaseHistoricOutcomes(jackCeoCase, ACCEPT, NO_RESPONSE);
        assertCaseHistoricOutcomes(jackCtoCase, ACCEPT, ACCEPT, NO_RESPONSE);

        // Current stage:
        //  - cases: 4, complete: 0, decided: 1
        //  - work items: 2 of 8 done
        assertPercentCompleteCurrent(campaign, 0, 25, 25);
        // All stages:
        //  - cases: 6, complete: 1 (only guybrush->COO), decided: 2 (guybrush->COO, admin->COO)
        //  - work items: 1/1 + 4/5 + 2/8 = 7/14 = 50%
        assertPercentCompleteCurrentIteration(campaign, 17, 33, 50);
        assertPercentCompleteAll(campaign, 17, 33, 50);

        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test400OpenFourthStage() throws Exception {
        // GIVEN
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        // WHEN
        when();
        certificationService.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getNextStageTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 4", campaign);
        assertSanityAfterStageOpen(campaign, certificationDefinition, 4);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("Wrong number of certification cases", 6, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

        /*
Stage4: allMustAccept

Overall: allMustAccept

owners: CEO: elaine, COO: administrator, CTO: none

Case                        Stage1              Stage2                           Stage3                                            Stage4
===============================================================================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A    elaine:null,administrator:ND -> ND  [STOP] | ND
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A    elaine:A,administrator:null -> A           | A    cheese
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine:NR,administrator:NR -> NR           | NR   cheese
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine:null,administrator:null -> NR       | NR   cheese
         */

        assertCaseReviewers(elaineCeoCase, NOT_DECIDED, 3, asList(USER_ELAINE_OID, USER_ADMINISTRATOR_OID));
        assertCaseReviewers(guybrushCooCase, REVOKE, 2, singletonList(USER_ADMINISTRATOR_OID));
        assertCaseReviewers(administratorCooCase, NO_RESPONSE, 4, singletonList(USER_CHEESE_OID));
        assertCaseReviewers(administratorCeoCase, NO_RESPONSE, 4, singletonList(USER_CHEESE_OID));
        assertCaseReviewers(jackCeoCase, NO_RESPONSE, 2, singletonList(USER_ELAINE_OID));
        assertCaseReviewers(jackCtoCase, NO_RESPONSE, 4, singletonList(USER_CHEESE_OID));

        assertCaseOutcome(caseList, USER_ELAINE_OID, ROLE_CEO_OID, NOT_DECIDED, NOT_DECIDED, null);
        assertCaseOutcome(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CTO_OID, NO_RESPONSE, NO_RESPONSE, null);

        // Current stage:
        //  - cases: 3, complete: 0, decided: 0
        //  - work items: 0 of 3 done
        assertPercentCompleteCurrent(campaign, 0, 0, 0);
        // All stages:
        //  - cases: 6, complete: 1 (only guybrush->COO), decided: 1 (guybrush->COO)
        //  - work items: 1/1 + 4/5 + 2/8 + 0/3 = 7/17 = 41%
        assertPercentCompleteCurrentIteration(campaign, 17, 17, 41);
        assertPercentCompleteAll(campaign, 17, 17, 41);
        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test430RecordDecisionsFourthStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // WHEN
        when();

/*
Stage4: allMustAccept

Overall: allMustAccept

Case                        Stage1              Stage2                           Stage3                                            Stage4
===============================================================================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A    elaine:null,administrator:ND -> ND  [STOP] | ND
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A    elaine:A,administrator:null -> A           | A    cheese:A -> A | A
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine:NR,administrator:NR -> NR           | NR   cheese:A -> A | NR
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine:null,administrator:null -> NR       | NR   cheese:NR -> NR | NR

*/

        AccessCertificationCaseType elaineCeoCase;
        AccessCertificationCaseType guybrushCooCase;
        AccessCertificationCaseType administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase;
        AccessCertificationCaseType jackCtoCase;

        recordDecision(campaignOid, administratorCooCase, ACCEPT, null, USER_CHEESE_OID, task, result);
        recordDecision(campaignOid, administratorCeoCase, ACCEPT, null, USER_CHEESE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 4", campaign);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);

        elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

        /*
Stage4: allMustAccept

Overall: allMustAccept

Case                        Stage1              Stage2                           Stage3                                            Stage4
===============================================================================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A    elaine:null,administrator:ND -> ND  [STOP] | ND
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A    elaine:A,administrator:null -> A           | A    cheese:A -> A | A
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine:NR,administrator:NR -> NR           | NR   cheese:A -> A | NR
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine:null,administrator:null -> NR       | NR   cheese:NR -> NR | NR
*/

        assertWorkItemsCount(elaineCeoCase, 3);
        assertWorkItemsCount(guybrushCooCase, 2);
        assertWorkItemsCount(administratorCooCase, 4);
        assertWorkItemsCount(administratorCeoCase, 4);
        assertWorkItemsCount(jackCeoCase, 1);
        assertWorkItemsCount(jackCtoCase, 3);

        assertNoDecision(elaineCeoCase, 4, 1, NOT_DECIDED, false);
        assertNoDecision(guybrushCooCase, 4, 1, REVOKE, false);
        assertReviewerDecision(administratorCooCase, ACCEPT, null, 4, 1, USER_CHEESE_OID, ACCEPT, false);
        assertReviewerDecision(administratorCeoCase, ACCEPT, null, 4, 1, USER_CHEESE_OID, ACCEPT, false);
        assertNoDecision(jackCeoCase, 4, 1, NO_RESPONSE, false);
        assertNoDecision(jackCtoCase, 4, 1, NO_RESPONSE, false);

        assertCaseOutcome(caseList, USER_ELAINE_OID, ROLE_CEO_OID, NOT_DECIDED, NOT_DECIDED, null);
        assertCaseOutcome(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, ACCEPT, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CTO_OID, NO_RESPONSE, NO_RESPONSE, null);

        // Current stage:
        //  - cases: 3, complete: 2, decided: 1
        //  - work items: 2 of 3 done
        assertPercentCompleteCurrent(campaign, 67, 33, 67);
        // All stages:
        //  - cases: 6, complete: 1 (only guybrush->COO), decided: 2 (guybrush->COO, admin->COO)
        //  - work items: 1/1 + 4/5 + 2/8 + 2/3 = 9/17 = 53%
        assertPercentCompleteCurrentIteration(campaign, 17, 33, 53);
        assertPercentCompleteAll(campaign, 17, 33, 53);
    }

    @Test
    public void test490CloseFourthStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        // WHEN
        when();
        certificationManager.closeCurrentStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after closing stage 4", campaign);
        assertSanityAfterStageClose(campaign, certificationDefinition, 4);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("wrong # of cases", 6, caseList.size());

        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCooCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID);
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

        /*
Stage4: allMustAccept

Overall: allMustAccept

Case                        Stage1              Stage2                           Stage3                                            Stage4
===============================================================================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A    elaine:null,administrator:ND -> ND  [STOP] | ND
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A    elaine:A,administrator:null -> A           | A    cheese:A -> A | A
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine:NR,administrator:NR -> NR           | NR   cheese:A -> A | NR
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine:null,administrator:null -> NR       | NR   cheese:NR -> NR | NR
*/

        assertCaseHistoricOutcomes(elaineCeoCase, ACCEPT, ACCEPT, NOT_DECIDED);
        assertCaseHistoricOutcomes(guybrushCooCase, ACCEPT, REVOKE);
        assertCaseHistoricOutcomes(administratorCooCase, ACCEPT, ACCEPT, ACCEPT, ACCEPT);
        assertCaseHistoricOutcomes(administratorCeoCase, ACCEPT, ACCEPT, NO_RESPONSE, ACCEPT);
        assertCaseHistoricOutcomes(jackCeoCase, ACCEPT, NO_RESPONSE);
        assertCaseHistoricOutcomes(jackCtoCase, ACCEPT, ACCEPT, NO_RESPONSE, NO_RESPONSE);

        // Current stage:
        //  - cases: 3, complete: 2, decided: 1
        //  - work items: 2 of 3 done
        assertPercentCompleteCurrent(campaign, 67, 33, 67);
        // All stages:
        //  - cases: 6, complete: 1 (only guybrush->COO), decided: 2 (guybrush->COO, admin->COO)
        //  - work items: 1/1 + 4/5 + 2/8 + 2/3 = 9/17 = 53%
        assertPercentCompleteCurrentIteration(campaign, 17, 33, 53);
        assertPercentCompleteAll(campaign, 17, 33, 53);
        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test495StartRemediation() throws Exception {
        // GIVEN
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
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

        List<PrismObject<TaskType>> tasks = getRemediationTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        campaign = getCampaignWithCases(campaignOid);
        assertEquals("wrong campaign state", CLOSED, campaign.getState());
        assertEquals("wrong campaign stage", 5, or0(campaign.getStageNumber()));
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertApproximateTime("end time", new Date(), campaign.getEndTimestamp());
        assertEquals("wrong # of stages", 4, campaign.getStage().size());

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("wrong # of cases", 6, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        assertNull("elaineCeoCase.remediedTimestamp", elaineCeoCase.getRemediedTimestamp());
        assertApproximateTime("guybrushCooCase.remediedTimestamp", new Date(), guybrushCooCase.getRemediedTimestamp());

        userElaine = getUser(USER_ELAINE_OID).asObjectable();
        display("userElaine", userElaine);
        assertEquals("wrong # of userElaine's assignments", 6, userElaine.getAssignment().size());

        userGuybrush = getUser(USER_GUYBRUSH_OID).asObjectable();
        display("userGuybrush", userGuybrush);
        assertEquals("wrong # of userGuybrush's assignments", 2, userGuybrush.getAssignment().size());
    }

    @Test
    public void test497Statistics() throws Exception {
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

//        AccessCertificationCampaignType campaignWithCases = getCampaignWithCases(campaignOid);
//        display("campaignWithCases", campaignWithCases);

        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(1, stat.getMarkedAsAccept());
        assertEquals(1, stat.getMarkedAsRevoke());
        assertEquals(1, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(1, stat.getMarkedAsNotDecide());
        assertEquals(3, stat.getWithoutResponse());
    }

    @Test
    public void test499CheckAfterClose() throws Exception {
        login(userAdministrator.asPrismObject());

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        waitForCampaignTasks(campaignOid, 20000, result);

        // THEN
        userAdministrator = getUser(USER_ADMINISTRATOR_OID).asObjectable();
        display("administrator", userAdministrator);
        assertCertificationMetadata(
                findAssignmentByTargetRequired(userAdministrator.asPrismObject(), ROLE_COO_OID),
                SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT,
                new HashSet<>(asList(USER_ADMINISTRATOR_OID, USER_ELAINE_OID, USER_CHEESE_OID)),
                singleton("administrator: ok"));
    }

    @Test
    public void test500Reiterate() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        //certificationManager.closeCampaign(campaignOid, true, task, result);
        certificationManager.reiterateCampaignTask(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getReiterationTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after reiteration", campaign);
        assertStateStageIteration(campaign, AccessCertificationCampaignStateType.CREATED, 0, 2);

/*
Case                        Stage1              Stage2                           Stage3                                            Stage4
===============================================================================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A    elaine:null,administrator:ND -> ND  [STOP] | ND
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A    elaine:A,administrator:null -> A           | A    cheese:A -> A | A
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine:NR,administrator:NR -> NR           | NR   cheese:A -> A | NR
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine:null,administrator:null -> NR       | NR   cheese:NR -> NR | NR
*/

        // current iteration = 2, stage = 0
        // there are 3 cases in this iteration/stage: admin->CEO, jack->CEO, jack->CTO and 0 work items (no review stage is opened)
        // - among these cases, 100% is complete within iteration/stage (because of no work items!), 0 is decided
        // - work items are "all" decided (because there are none)
        // TODO ... but these percentages should not be 100%! It is misleading in the reports.
        assertPercentCompleteCurrent(campaign, 100, 0, 100);
        assertPercentCompleteCurrentStage(campaign, 100, 0, 100);

        // When observing current iteration (i.e. 2), the above still holds.
        assertPercentCompleteCurrentIteration(campaign, 100, 0, 100);

        // But for the overall picture, we have:
        // - 6 cases
        //    - among them only 1 (guybrush->COO) is complete (17%)
        //    - 2 cases are decided (33%)
        // - work items are all from iteration 1: 1/1 + 4/5 + 2/8 + 2/3 = 9/17 = 53%
        assertPercentCompleteAll(campaign, 17, 33, 53);
        displayDumpable("dummy transport", dummyTransport);
    }

/*
AFTER REITERATION
-----------------
Expected cases, reviewers and decisions/outcomes:

CEO = 00000000-d34d-b33f-f00d-000000000001
COO = 00000000-d34d-b33f-f00d-000000000002

Stage1: oneAcceptAccepts, default: accept, stop on: revoke          (manager)

Case                        Stage1
================================================
administrator->CEO          none (A) -> A
jack->CEO                   none (A) -> A
jack->CTO                   none (A) -> A


Stage2: allMustAccept, default: accept, advance on: accept          (target owner)

Case                        Stage1              Stage2
=============================================================
administrator->CEO          none (A) -> A       "A" from iter 1
jack->CEO                   none (A) -> A       elaine
jack->CTO                   none (A) -> A       "A" from iter 1

From iteration 1:
Case                        Stage1              Stage2                           Stage3                                            Stage4
===============================================================================================================================================
administrator->CEO          none (A) -> A       elaine: A -> A            | A    elaine:NR,administrator:NR -> NR           | NR   cheese:A -> A | NR
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A    elaine:null,administrator:null -> NR       | NR   cheese:NR -> NR | NR

*/


    //TODO temporarily disabled, change in behavior. now also empty stage is generated and not skipped by default
    @Test(enabled = false) // MID-10294
    public void test510OpenNextStage() throws Exception {           // next stage is 2 (because the first one has no work items)
        // GIVEN
//        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        // WHEN
        when();
        certificationService.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getNextStageTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 2", campaign);
        assertSanityAfterStageOpen(campaign, certificationDefinition, 2, 2, 5); // stage 1 in iteration 2 was skipped

        List<AccessCertificationCaseType> caseList = new ArrayList<>(queryHelper.searchCases(campaignOid, null, result));
        caseList.removeIf(c -> norm(c.getIteration()) != 2);
        assertEquals("Wrong number of certification cases", 3, caseList.size());
        AccessCertificationCaseType administratorCeoCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCeoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        AccessCertificationCaseType jackCtoCase = findCase(caseList, USER_JACK_OID, ROLE_CTO_OID);

        assertCaseReviewers(administratorCeoCase, null, 0, emptyList());
        assertCaseReviewers(jackCeoCase, NO_RESPONSE, 2, singletonList(USER_ELAINE_OID));
        assertCaseReviewers(jackCtoCase, null, 0, emptyList());

        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, null, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CTO_OID, null, NO_RESPONSE, null);

        // current iteration = 2, stage = 2 (stage 1 was skipped because all cases already have outcome for stage 1)
        // there is 1 case in this iteration/stage: jack->CEO and 1 work item
        // - it is not complete within iteration/stage neither decided
        // - 0% of work items are decided
        assertPercentCompleteCurrent(campaign, 0, 0, 0);

/*
        But looking through iterations, there are 6 cases:

Case                        Stage1              Stage2 Iteration1
=================================================================================
elaine->CEO                 none (A) -> A       elaine A -> A             | A           eventually ND (iter 1)
guybrush->COO               cheese: A -> A      admin: RV -> RV   [STOP]  | RV
administrator->COO          none (A) -> A       admin: A -> A             | A           eventually A (iter 1)
administrator->CEO          none (A) -> A       elaine: A -> A            | A           eventually NR (iter 1)
jack->CEO                   none (A) -> A       elaine: null -> NR [STOP] | NR
jack->CTO                   none (A) -> A       none (A) -> A             | A           eventually NR (iter 1)

Case                                            Stage2 Iteration2
=============================================================
administrator->CEO                              "A" from iter 1
jack->CEO                                       elaine
jack->CTO                                       "A" from iter 1

        Out of them, completed (for stage 2) are: elaine->CEO, guybrush->COO, administrator->COO, administrator->CEO, jack->CTO -> so 83%
        Decided are: only two (guybrush -> COO, admin -> COO) ... because other ones are no-response because of later stages in iteration 1
        Work items: created 5+1 (but 1 is overridden), completed 4 i.e. 80%
 */
        assertPercentCompleteCurrentStage(campaign, 83, 33, 80);

        // When observing current iteration (i.e. 2), we have
        // - three cases, 1 work item
        // - completed: 2 cases (no work items!), decided: 0 cases
        // - work items decided: 0 of 1
        assertPercentCompleteCurrentIteration(campaign, 67, 0, 0);

        // But for the overall picture, we have:
        // - 6 cases
        //    - among them only 1 (guybrush->COO) is complete (17%)
        //    - 2 cases are decided (33%)
        // - work items are all from iteration 1 plus the one created now (but we do not count the original one in iteration 1
        // that is overridden by it), so: 1/1 + 4/5 + 2/8 + 2/3 + 0/1 - 0/1 = 9/17 = 53%
        assertPercentCompleteAll(campaign, 17, 33, 53);

        assertCasesCount(campaignOid, 6);
        displayDumpable("dummy transport", dummyTransport);
    }
}
