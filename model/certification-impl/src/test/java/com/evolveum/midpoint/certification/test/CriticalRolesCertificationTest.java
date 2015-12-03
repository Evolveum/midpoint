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
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CREATED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Tests itemSelectionExpression and useSubjectManager.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CriticalRolesCertificationTest extends AbstractCertificationTest {

    protected static final File CERT_DEF_FILE = new File(COMMON_DIR, "certification-of-critical-roles.xml");

    protected AccessCertificationDefinitionType certificationDefinition;

    private String campaignOid;

    @Test
    public void test010CreateCampaign() throws Exception {
        final String TEST_NAME = "test010CreateCampaign";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        certificationDefinition = repoAddObjectFromFile(CERT_DEF_FILE,
                AccessCertificationDefinitionType.class, result).asObjectable();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCampaignType campaign =
                certificationManager.createCampaign(certificationDefinition, null, task, result);

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

    @Test
    public void test020OpenFirstStage() throws Exception {
        final String TEST_NAME = "test020OpenFirstStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
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

        /*
        Expected cases and reviewers:

        Case                        Stage1                      Stage2
        ==================================================================
        elaine->CEO                 elaine                      elaine
        guybrush->COO               cheese                      elaine
         */

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        assertEquals("unexpected # of cases", 2, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        checkSpecificCase(elaineCeoCase, userElaine);
        checkSpecificCase(guybrushCooCase, userGuybrush);

        assertCaseReviewers(elaineCeoCase, null, true, 1, Arrays.asList(USER_ELAINE_OID));
        assertCaseReviewers(guybrushCooCase, null, true, 1, Arrays.asList(USER_CHEESE_OID));
    }

    @Test
    public void test100RecordDecisions() throws Exception {
        final String TEST_NAME = "test100RecordDecisions";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertEquals("unexpected # of cases", 2, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);

        recordDecision(campaignOid, elaineCeoCase, REVOKE, null, 1, USER_ELAINE_OID, task, result);
        recordDecision(campaignOid, guybrushCooCase, ACCEPT, null, 1, USER_CHEESE_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        display("caseList", caseList);

        assertEquals("unexpected # of cases", 2, caseList.size());
        elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);

        assertDecision(elaineCeoCase, REVOKE, null, 1, USER_ELAINE_OID, REVOKE, true);
        assertDecision(guybrushCooCase, ACCEPT, null, 1, USER_CHEESE_OID, ACCEPT, true);
    }

    @Test
    public void test150CloseFirstStage() throws Exception {
        final String TEST_NAME = "test150CloseFirstStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
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

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        assertEquals("unexpected # of cases", 2, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);

        assertDecision(elaineCeoCase, REVOKE, null, 1, USER_ELAINE_OID, REVOKE, true);
        assertDecision(guybrushCooCase, ACCEPT, null, 1, USER_CHEESE_OID, ACCEPT, true);
    }

    @Test
    public void test200OpenSecondStage() throws Exception {
        final String TEST_NAME = "test200OpenSecondStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
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

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        assertEquals("Wrong number of certification cases", 2, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);

        assertCaseReviewers(elaineCeoCase, REVOKE, false, 1, Arrays.<String>asList());
        assertCaseReviewers(guybrushCooCase, null, true, 2, Arrays.asList(USER_ELAINE_OID));
    }

    @Test
    public void test220StatisticsAllStages() throws Exception {
        final String TEST_NAME = "test220StatisticsAllStages";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
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
        assertEquals(1, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(1, stat.getWithoutResponse());
    }

    @Test
    public void test250RecordDecisionsSecondStage() throws Exception {
        final String TEST_NAME = "test250RecordDecisionsSecondStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);

        recordDecision(campaignOid, guybrushCooCase, ACCEPT, "OK", 2, USER_ELAINE_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 2", campaign);

        caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        display("caseList", caseList);

        guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);

        assertDecisions(guybrushCooCase, 2);
        assertDecision2(guybrushCooCase, ACCEPT, "OK", 2, USER_ELAINE_OID, ACCEPT, true);
    }

    @Test
    public void test260Statistics() throws Exception {
        final String TEST_NAME = "test260Statistics";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
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
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }

    @Test
    public void test290CloseSecondStage() throws Exception {
        final String TEST_NAME = "test290CloseSecondStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
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

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        assertEquals("wrong # of cases", 2, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        assertCurrentState(elaineCeoCase, REVOKE, 1, false);
        assertCurrentState(guybrushCooCase, ACCEPT, 2, true);
    }

    @Test
    public void test300StartRemediation() throws Exception {
        final String TEST_NAME = "test300StartRemediation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
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

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        assertEquals("wrong # of cases", 2, caseList.size());
        AccessCertificationCaseType elaineCeoCase = findCase(caseList, USER_ELAINE_OID, ROLE_CEO_OID);
        AccessCertificationCaseType guybrushCooCase = findCase(caseList, USER_GUYBRUSH_OID, ROLE_COO_OID);
        assertApproximateTime("elaineCeoCase.remediedTimestamp", new Date(), elaineCeoCase.getRemediedTimestamp());
        assertNull("guybrushCooCase.remediedTimestamp", guybrushCooCase.getRemediedTimestamp());

        userElaine = getUser(USER_ELAINE_OID).asObjectable();
        display("userElaine", userElaine);
        assertEquals("wrong # of userElaine's assignments", 2, userElaine.getAssignment().size());

        userGuybrush = getUser(USER_GUYBRUSH_OID).asObjectable();
        display("userGuybrush", userGuybrush);
        assertEquals("wrong # of userGuybrush's assignments", 3, userGuybrush.getAssignment().size());
    }

    @Test
    public void test310Statistics() throws Exception {
        final String TEST_NAME = "test310Statistics";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(CriticalRolesCertificationTest.class.getName() + "." + TEST_NAME);
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
        assertEquals(1, stat.getMarkedAsRevoke());
        assertEquals(1, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsDelegate());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }

}
