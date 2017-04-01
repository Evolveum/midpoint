/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RoleInducementCertificationTest extends AbstractCertificationTest {

    protected AccessCertificationDefinitionType certificationDefinition;

    private String campaignOid;

    @Test
    public void test010CreateCampaign() throws Exception {
        final String TEST_NAME = "test010CreateCampaign";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        certificationDefinition = repoAddObjectFromFile(ROLE_INDUCEMENT_CERT_DEF_FILE,
                AccessCertificationDefinitionType.class, result).asObjectable();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCampaignType campaign =
                certificationManager.createCampaign(certificationDefinition.getOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getCampaignWithCases(campaignOid);
        display("campaign", campaign);
        assertAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentComplete(campaign, 100, 100, 100);
    }

    @Test
    public void test012SearchAllCases() throws Exception {
        final String TEST_NAME = "test012SearchAllCases";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }

    @Test
    public void test014Statistics() throws Exception {
        final String TEST_NAME = "test014Statistics";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("statistics", stat.asPrismContainerValue());
        assertEquals(0, stat.getMarkedAsAccept());
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }

    @Test
    public void test020OpenFirstStage() throws Exception {
        final String TEST_NAME = "test020OpenFirstStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationManager.openNextStage(campaignOid, 1, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);
        assertAfterCampaignStart(campaign, certificationDefinition, 5);
        checkAllCases(campaign.getCase(), campaignOid);

        List<AccessCertificationCaseType> caseList = campaign.getCase();
        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, null);
        assertPercentComplete(campaign, 20, 100, 0);     // preliminary outcomes for all cases are "ACCEPT"
    }

    protected void checkAllCases(Collection<AccessCertificationCaseType> caseList, String campaignOid) {
        assertEquals("Wrong number of certification cases", 5, caseList.size());
        checkCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, roleCeo, campaignOid);
        checkCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, roleCoo, campaignOid);
        checkCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, roleCoo, campaignOid);
        checkCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, roleCoo, campaignOid);
        checkCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, roleSuperuser, campaignOid);
    }

    @Test
    public void test024Statistics() throws Exception {
        final String TEST_NAME = "test024Statistics";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // all cases are marked as ACCEPT, because the outcome strategy is acceptedIfNotDenied, with a default of Accept
        display("statistics", stat.asPrismContainerValue());
        assertEquals(5, stat.getMarkedAsAccept());
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }

    @Test
    public void test030SearchAllCases() throws Exception {
        final String TEST_NAME = "test030SearchCases";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCases(caseList, campaignOid);
        AccessCertificationCaseType _case = checkCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, roleSuperuser, campaignOid);
        assertEquals("Unexpected number of reviewers in superuser case", 0, CertCampaignTypeUtil.getReviewers(_case).size());
    }

    @Test
    public void test050SearchDecisionsAdministrator() throws Exception {
        final String TEST_NAME = "test050SearchDecisionsAdministrator";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationWorkItemType> workItems =
                queryHelper.searchWorkItems(null, USER_ADMINISTRATOR_OID, false, null, task, result);

        /* Expected cases - phase 1:

        COO-Dummy:                administrator
        COO-DummyBlack:           administrator
        COO-Superuser:            administrator
         */

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 3, workItems.size());
        checkWorkItem(workItems, ROLE_COO_OID, RESOURCE_DUMMY_OID, roleCoo, campaignOid);
        checkWorkItem(workItems, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, roleCoo, campaignOid);
        checkWorkItem(workItems, ROLE_COO_OID, ROLE_SUPERUSER_OID, roleCoo, campaignOid);
    }

    @Test
    public void test051SearchDecisionsElaine() throws Exception {
        final String TEST_NAME = "test051SearchDecisionsElaine";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationWorkItemType> workItems =
                queryHelper.searchWorkItems(null, USER_ELAINE_OID, false, null, task, result);

        /* Expected cases - phase 1:

        CEO-Dummy:                elaine
         */

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", workItems);
        assertEquals("Wrong number of work items", 1, workItems.size());
        checkWorkItem(workItems, ROLE_CEO_OID, RESOURCE_DUMMY_OID, roleCeo, campaignOid);
    }

    @Test
    public void test052SearchDecisionsJack() throws Exception {
        final String TEST_NAME = "test052SearchDecisionsJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationWorkItemType> workItems =
                queryHelper.searchWorkItems(null, USER_JACK_OID, false, null, task, result);

        /* Expected cases - phase 1: NONE */

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 0, workItems.size());
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
        final String TEST_NAME = "test100RecordDecisions";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);

        recordDecision(campaignOid, ceoDummyCase, REVOKE, "no way", 1, USER_ELAINE_OID, task, result);
        recordDecision(campaignOid, cooDummyCase, REVOKE, null, 0, null, task, result);          // default reviewer - administrator
        recordDecision(campaignOid, cooDummyBlackCase, ACCEPT, "OK", 1, USER_ADMINISTRATOR_OID, task, result);
        recordDecision(campaignOid, cooSuperuserCase, NOT_DECIDED, "I'm so procrastinative...", 0, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCases(caseList, campaignOid);

        ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);

        assertSingleDecision(ceoDummyCase, REVOKE, "no way", 1, USER_ELAINE_OID, REVOKE, false);
        assertSingleDecision(cooDummyCase, REVOKE, null, 1, USER_ADMINISTRATOR_OID, REVOKE, false);
        assertSingleDecision(cooDummyBlackCase, ACCEPT, "OK", 1, USER_ADMINISTRATOR_OID, ACCEPT, false);
        assertSingleDecision(cooSuperuserCase, NOT_DECIDED, "I'm so procrastinative...", 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, null);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentComplete(campaign, 100, 100, 100);
    }

    @Test
    public void test110Statistics() throws Exception {
        final String TEST_NAME = "test110Statistics";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("statistics", stat.asPrismContainerValue());
        assertEquals(3, stat.getMarkedAsAccept());
        assertEquals(2, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }


    @Test
    public void test150CloseFirstStage() throws Exception {
        final String TEST_NAME = "test150CloseFirstStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationManager.closeCurrentStage(campaignOid, 1, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);
        assertAfterStageClose(campaign, certificationDefinition, 1);
        checkAllCases(campaign.getCase(), campaignOid);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        assertSingleDecision(ceoDummyCase, REVOKE, "no way", 1, USER_ELAINE_OID, REVOKE, true);
        assertSingleDecision(cooDummyCase, REVOKE, null, 1, USER_ADMINISTRATOR_OID, REVOKE, true);
        assertSingleDecision(cooDummyBlackCase, ACCEPT, "OK", 1, USER_ADMINISTRATOR_OID, ACCEPT, true);
        assertSingleDecision(cooSuperuserCase, NOT_DECIDED, "I'm so procrastinative...", 1, USER_ADMINISTRATOR_OID, ACCEPT, true);
        assertNoDecision(superuserDummyCase, 1, ACCEPT, true);

        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, 1);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, 1);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, ACCEPT, ACCEPT, 1);

        assertPercentComplete(campaign, 100, 100, 100);
    }

    @Test
    public void test160Statistics() throws Exception {
        final String TEST_NAME = "test160Statistics";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("statistics", stat.asPrismContainerValue());
        assertEquals(3, stat.getMarkedAsAccept());
        assertEquals(2, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }

    @Test
    public void test200OpenSecondStage() throws Exception {
        final String TEST_NAME = "test200OpenSecondStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationManager.openNextStage(campaignOid, 2, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 2", campaign);
        assertAfterStageOpen(campaign, certificationDefinition, 2);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        assertEquals("Wrong number of certification cases", 5, caseList.size());
        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        assertCaseReviewers(ceoDummyCase, REVOKE, 1, Arrays.asList(USER_ELAINE_OID));
        assertCaseReviewers(cooDummyCase, REVOKE, 1, Arrays.asList(USER_ADMINISTRATOR_OID));
        assertCaseReviewers(cooDummyBlackCase, NO_RESPONSE, 2, Arrays.asList(USER_ADMINISTRATOR_OID, USER_ELAINE_OID));
        assertCaseReviewers(cooSuperuserCase, NO_RESPONSE, 2, Arrays.asList(USER_ADMINISTRATOR_OID));
        assertCaseReviewers(superuserDummyCase, NO_RESPONSE, 2, Arrays.asList(USER_JACK_OID, USER_ADMINISTRATOR_OID));

        assertCaseHistoricOutcomes(ceoDummyCase, REVOKE);
        assertCaseHistoricOutcomes(cooDummyCase, REVOKE);
        assertCaseHistoricOutcomes(cooDummyBlackCase, ACCEPT);
        assertCaseHistoricOutcomes(cooSuperuserCase, ACCEPT);
        assertCaseHistoricOutcomes(superuserDummyCase, ACCEPT);

        assertCaseOutcome(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID, NO_RESPONSE, NO_RESPONSE, null);

        // 40% of cases is answered (not advanced to this stage)
        // 40% is decided ("REVOKE" in stage 1 + "NO_RESPONSE" in stage 2)
        // 0% decisions of current stage is done
        assertPercentComplete(campaign, 40, 40, 0);
    }

    @Test
    public void test210Statistics() throws Exception {
        final String TEST_NAME = "test210Statistics";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("statistics", stat.asPrismContainerValue());
        assertEquals(0, stat.getMarkedAsAccept());
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(3, stat.getWithoutResponse());
    }

    @Test
    public void test220StatisticsAllStages() throws Exception {
        final String TEST_NAME = "test220StatisticsAllStages";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, false, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("statistics", stat.asPrismContainerValue());
        assertEquals(0, stat.getMarkedAsAccept());
        assertEquals(2, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(3, stat.getWithoutResponse());
    }

    @Test
    public void test250RecordDecisionsSecondStage() throws Exception {
        final String TEST_NAME = "test250RecordDecisionsSecondStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        recordDecision(campaignOid, cooDummyBlackCase, ACCEPT, "OK", 2, USER_ADMINISTRATOR_OID, task, result);
        recordDecision(campaignOid, cooDummyBlackCase, REVOKE, "Sorry", 2, USER_ELAINE_OID, task, result);
        recordDecision(campaignOid, cooSuperuserCase, ACCEPT, null, 2, USER_ADMINISTRATOR_OID, task, result);
        recordDecision(campaignOid, superuserDummyCase, ACCEPT, null, 2, USER_JACK_OID, task, result);       // decision of administrator is missing here

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 2", campaign);

        caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);

        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        assertWorkItems(ceoDummyCase, 1);
        assertWorkItems(cooDummyCase, 1);
        assertWorkItems(cooDummyBlackCase, 3);
        assertWorkItems(cooSuperuserCase, 2);
        assertWorkItems(superuserDummyCase, 2);

        assertDecision2(cooDummyBlackCase, ACCEPT, "OK", 2, USER_ADMINISTRATOR_OID, REVOKE);
        assertDecision2(cooDummyBlackCase, REVOKE, "Sorry", 2, USER_ELAINE_OID, REVOKE);
        assertDecision2(cooSuperuserCase, ACCEPT, null, 2, USER_ADMINISTRATOR_OID, ACCEPT);
        assertDecision2(superuserDummyCase, ACCEPT, null, 2, USER_JACK_OID, NO_RESPONSE);
        assertDecision2(superuserDummyCase, null, null, 2, USER_ADMINISTRATOR_OID, NO_RESPONSE);

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

        // 80% cases has all decisions (or is not in current stage)
        // 80% cases has an outcome
        // 80% decisions for this stage was done
        assertPercentComplete(campaign, 80, 80, 80);
    }

    @Test
    public void test260Statistics() throws Exception {
        final String TEST_NAME = "test260Statistics";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("statistics", stat.asPrismContainerValue());
        assertEquals(1, stat.getMarkedAsAccept());
        assertEquals(1, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(1, stat.getWithoutResponse());
    }

    @Test
    public void test290CloseSecondStage() throws Exception {
        final String TEST_NAME = "test290CloseSecondStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationManager.closeCurrentStage(campaignOid, 2, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after closing stage 2", campaign);
        assertAfterStageClose(campaign, certificationDefinition, 2);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
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

        // 80% cases has all decisions (or is not in current stage)
        // 80% cases has an outcome
        // 80% decisions for this stage was done
        assertPercentComplete(campaign, 80, 80, 80);
    }

    @Test
    public void test300StartRemediation() throws Exception {
        final String TEST_NAME = "test300StartRemediation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationManager.startRemediation(campaignOid, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation start", campaign);
        assertTrue("wrong campaign state: " + campaign.getState(), campaign.getState() == CLOSED || campaign.getState() == IN_REMEDIATION);

        ObjectQuery query = QueryBuilder.queryFor(TaskType.class, prismContext)
                .item(TaskType.F_OBJECT_REF).ref(campaign.getOid())
                .build();
        List<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, query, null, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid(), true);

        campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation finished", campaign);
        assertEquals("wrong campaign state", CLOSED, campaign.getState());
        assertEquals("wrong campaign stage", 3, campaign.getStageNumber());
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertApproximateTime("end time", new Date(), campaign.getEnd());
        assertEquals("wrong # of stages", 2, campaign.getStage().size());

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
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

        // 80% cases has all decisions (or is not in current stage)
        // 80% cases has an outcome
        // 80% decisions for this stage was done
        assertPercentComplete(campaign, 80, 80, 80);
    }

    @Test
    public void test310Statistics() throws Exception {
        final String TEST_NAME = "test310Statistics";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, false, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("statistics", stat.asPrismContainerValue());
        assertEquals(1, stat.getMarkedAsAccept());
        assertEquals(3, stat.getMarkedAsRevoke());
        assertEquals(3, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(1, stat.getWithoutResponse());
    }

}
