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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CREATED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RoleInducementCertificationTest extends AbstractCertificationTest {

    protected static final File CERT_DEF_FILE = new File(COMMON_DIR, "certification-of-role-inducements.xml");

    protected AccessCertificationDefinitionType certificationDefinition;

    private String campaignOid;

    @Test
    public void test010CreateCampaign() throws Exception {
        final String TEST_NAME = "test010CreateCampaign";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(RoleInducementCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        certificationDefinition = repoAddObjectFromFile(CERT_DEF_FILE,
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
        assertEquals("Unexpected certification cases", 0, campaign.getCase().size());
        assertStateAndStage(campaign, CREATED, 0);
        assertEquals("Unexpected # of stages", 2, campaign.getStageDefinition().size());
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertNull("Unexpected start time", campaign.getStart());
        assertNull("Unexpected end time", campaign.getEnd());
    }

    protected void assertStateAndStage(AccessCertificationCampaignType campaign, AccessCertificationCampaignStateType state, int stage) {
        assertEquals("Unexpected campaign state", state, campaign.getState());
        assertEquals("Unexpected stage number", stage, campaign.getStageNumber());
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

        assertStateAndStage(campaign, IN_REVIEW_STAGE, 1);
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertApproximateTime("start time", new Date(), campaign.getStart());
        assertNull("Unexpected end time", campaign.getEnd());
        assertEquals("wrong # of defined stages", 2, campaign.getStageDefinition().size());
        assertEquals("wrong # of stages", 1, campaign.getStage().size());
        AccessCertificationStageType stage = campaign.getStage().get(0);
        assertEquals("wrong stage #", 1, stage.getNumber());
        assertApproximateTime("stage 1 start", new Date(), stage.getStart());
        assertNotNull("stage 1 end", stage.getEnd());       // too lazy to compute exact datetime
        checkAllCases(campaign.getCase(), campaignOid);
    }

    protected void assertDefinitionAndOwner(AccessCertificationCampaignType campaign, PrismObject<? extends ObjectType> certificationDefinition) {
        assertEquals("Unexpected ownerRef", ObjectTypeUtil.createObjectRef(USER_ADMINISTRATOR_OID, ObjectTypes.USER), campaign.getOwnerRef());
        assertEquals("Unexpected definitionRef",
                ObjectTypeUtil.createObjectRef(this.certificationDefinition),
                campaign.getDefinitionRef());
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

        display("statistics", stat.asPrismContainerValue());
        assertEquals(0, stat.getMarkedAsAccept());
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(5, stat.getWithoutResponse());
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
        assertEquals("Unexpected number of reviewers in superuser case", 0, _case.getReviewerRef().size());
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
        List<AccessCertificationCaseType> caseList =
                queryHelper.searchDecisions(null, USER_ADMINISTRATOR_OID, false, null, task, result);

        /* Expected cases - phase 1:

        COO-Dummy:                administrator             jack,administrator
        COO-DummyBlack:           administrator             administrator,elaine
        COO-Superuser:            administrator             administrator
         */

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Wrong number of certification cases", 3, caseList.size());
        checkCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID, roleCoo, campaignOid);
        checkCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID, roleCoo, campaignOid);
        checkCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID, roleCoo, campaignOid);
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
        List<AccessCertificationCaseType> caseList =
                queryHelper.searchDecisions(null, USER_ELAINE_OID, false, null, task, result);

        /* Expected cases - phase 1:

        CEO-Dummy:                elaine                    jack,administrator
         */

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Wrong number of certification cases", 1, caseList.size());
        checkCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID, roleCeo, campaignOid);
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
        List<AccessCertificationCaseType> caseList =
                queryHelper.searchDecisions(null, USER_JACK_OID, false, null, task, result);

        /* Expected cases - phase 1: NONE */

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Wrong number of certification cases", 0, caseList.size());
    }

    /*
    Entering decisions:

    CEO-Dummy:                elaine          REVOKE          jack,administrator
    COO-Dummy:                administrator   REVOKE          jack,administrator
    COO-DummyBlack:           administrator   ACCEPT          administrator,elaine
    COO-Superuser:            administrator   NO-DECISION     administrator
    Superuser-Dummy:          -                               jack,administrator

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

        assertDecision(ceoDummyCase, REVOKE, "no way", 1, USER_ELAINE_OID, REVOKE, true);
        assertDecision(cooDummyCase, REVOKE, null, 1, USER_ADMINISTRATOR_OID, REVOKE, true);
        assertDecision(cooDummyBlackCase, ACCEPT, "OK", 1, USER_ADMINISTRATOR_OID, ACCEPT, true);
        assertDecision(cooSuperuserCase, NOT_DECIDED, "I'm so procrastinative...", 1, USER_ADMINISTRATOR_OID, ACCEPT, true);
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
        assertEquals(2, stat.getMarkedAsAccept());
        assertEquals(2, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(1, stat.getWithoutResponse());
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

        assertStateAndStage(campaign, REVIEW_STAGE_DONE, 1);
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertNull("Unexpected end time", campaign.getEnd());
        assertEquals("wrong # of stages", 1, campaign.getStage().size());
        AccessCertificationStageType stage = campaign.getStage().get(0);
        assertEquals("wrong stage #", 1, stage.getNumber());
        assertApproximateTime("stage 1 start", new Date(), stage.getStart());
        //assertApproximateTime("stage 1 end", new Date(), stage.getStart());       // TODO when implemented
        checkAllCases(campaign.getCase(), campaignOid);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        assertDecision(ceoDummyCase, REVOKE, "no way", 1, USER_ELAINE_OID, REVOKE, true);       // note: "enabled" is computed when next stage is opened
        assertDecision(cooDummyCase, REVOKE, null, 1, USER_ADMINISTRATOR_OID, REVOKE, true);
        assertDecision(cooDummyBlackCase, ACCEPT, "OK", 1, USER_ADMINISTRATOR_OID, ACCEPT, true);
        assertDecision(cooSuperuserCase, NOT_DECIDED, "I'm so procrastinative...", 1, USER_ADMINISTRATOR_OID, ACCEPT, true);
        assertNoDecision(superuserDummyCase, ACCEPT, true);
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

        assertStateAndStage(campaign, IN_REVIEW_STAGE, 2);
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertApproximateTime("start time", new Date(), campaign.getStart());
        assertNull("Unexpected end time", campaign.getEnd());
        assertEquals("wrong # of defined stages", 2, campaign.getStageDefinition().size());
        assertEquals("wrong # of stages", 2, campaign.getStage().size());

        AccessCertificationStageType stage = CertCampaignTypeUtil.findStage(campaign, 2);

        assertEquals("wrong stage #", 2, stage.getNumber());
        assertApproximateTime("stage 2 start", new Date(), stage.getStart());
        assertNotNull("stage 2 end", stage.getEnd());       // too lazy to compute exact datetime

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        assertEquals("Wrong number of certification cases", 5, caseList.size());
        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);

        assertCaseReviewers(ceoDummyCase, REVOKE, false, 1, Arrays.<String>asList());
        assertCaseReviewers(cooDummyCase, REVOKE, false, 1, Arrays.<String>asList());
        assertCaseReviewers(cooDummyBlackCase, null, true, 2, Arrays.asList(USER_ADMINISTRATOR_OID, USER_ELAINE_OID));
        assertCaseReviewers(cooSuperuserCase, null, true, 2, Arrays.asList(USER_ADMINISTRATOR_OID));
        assertCaseReviewers(superuserDummyCase, null, true, 2, Arrays.asList(USER_JACK_OID, USER_ADMINISTRATOR_OID));
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

        assertDecisions(ceoDummyCase, 1);
        assertDecisions(cooDummyCase, 1);
        assertDecisions(cooDummyBlackCase, 3);
        assertDecisions(cooSuperuserCase, 2);
        assertDecisions(superuserDummyCase, 1);

        assertDecision2(cooDummyBlackCase, ACCEPT, "OK", 2, USER_ADMINISTRATOR_OID, REVOKE, true);
        assertDecision2(cooDummyBlackCase, REVOKE, "Sorry", 2, USER_ELAINE_OID, REVOKE, true);
        assertDecision2(cooSuperuserCase, ACCEPT, null, 2, USER_ADMINISTRATOR_OID, ACCEPT, true);
        assertDecision2(superuserDummyCase, ACCEPT, null, 2, USER_JACK_OID, NO_RESPONSE, true);       // decision of administrator is missing here
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

        assertStateAndStage(campaign, REVIEW_STAGE_DONE, 2);
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertNull("Unexpected end time", campaign.getEnd());
        assertEquals("wrong # of stages", 2, campaign.getStage().size());
        AccessCertificationStageType stage = CertCampaignTypeUtil.findStage(campaign, 2);
        assertEquals("wrong stage #", 2, stage.getNumber());
        assertApproximateTime("stage 2 start", new Date(), stage.getStart());
        //assertApproximateTime("stage 1 end", new Date(), stage.getStart());       // TODO when implemented

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        assertEquals("wrong # of cases", 5, caseList.size());
        AccessCertificationCaseType ceoDummyCase = findCase(caseList, ROLE_CEO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_OID);
        AccessCertificationCaseType cooDummyBlackCase = findCase(caseList, ROLE_COO_OID, RESOURCE_DUMMY_BLACK_OID);
        AccessCertificationCaseType cooSuperuserCase = findCase(caseList, ROLE_COO_OID, ROLE_SUPERUSER_OID);
        AccessCertificationCaseType superuserDummyCase = findCase(caseList, ROLE_SUPERUSER_OID, RESOURCE_DUMMY_OID);
        assertCurrentState(ceoDummyCase, REVOKE, 1, false);
        assertCurrentState(cooDummyCase, REVOKE, 1, false);
        assertCurrentState(cooDummyBlackCase, REVOKE, 2, true);
        assertCurrentState(cooSuperuserCase, ACCEPT, 2, true);
        assertCurrentState(superuserDummyCase, NO_RESPONSE, 2, true);       // decision of administrator is missing here
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

        RefFilter taskFilter = RefFilter.createReferenceEqual(new ItemPath(TaskType.F_OBJECT_REF), TaskType.class, prismContext, ObjectTypeUtil.createObjectRef(campaign).asReferenceValue());
        List<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, ObjectQuery.createObjectQuery(taskFilter), null, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid(), true);

        campaign = getCampaignWithCases(campaignOid);
        assertEquals("wrong campaign state", CLOSED, campaign.getState());
        assertEquals("wrong campaign stage", 3, campaign.getStageNumber());
        assertDefinitionAndOwner(campaign, certificationDefinition);
        // TODO assertApproximateTime("end time", new Date(), campaign.getEnd());
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
