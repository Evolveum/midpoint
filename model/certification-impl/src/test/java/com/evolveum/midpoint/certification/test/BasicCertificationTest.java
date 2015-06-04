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
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CREATED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Very simple certification test.
 * Tests just the basic functionality.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BasicCertificationTest extends AbstractCertificationTest {

    protected static final File CERT_DEF_USER_ASSIGNMENT_BASIC_FILE = new File(COMMON_DIR, "certification-of-eroot-user-assignments.xml");
    protected static final String CERT_DEF_USER_ASSIGNMENT_BASIC_OID = "33333333-0000-0000-0000-000000000001";

    protected AccessCertificationDefinitionType certificationDefinition;

    private String campaignOid;

    @Test
    public void test010CreateCampaign() throws Exception {
        final String TEST_NAME = "test010CreateCampaign";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        certificationDefinition = repoAddObjectFromFile(CERT_DEF_USER_ASSIGNMENT_BASIC_FILE,
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

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign", campaign);
        assertEquals("Unexpected certification cases", 0, campaign.getCase().size());
        assertStateAndStage(campaign, CREATED, 0);
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertNull("Unexpected start time", campaign.getStart());
        assertNull("Unexpected end time", campaign.getEnd());
    }

    protected void assertStateAndStage(AccessCertificationCampaignType campaign, AccessCertificationCampaignStateType state, int stage) {
        assertEquals("Unexpected campaign state", state, campaign.getState());
        assertEquals("Unexpected stage number", stage, campaign.getCurrentStageNumber());
    }

    @Test
    public void test012SearchAllCases() throws Exception {
        final String TEST_NAME = "test012SearchAllCases";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }


    @Test
    public void test020OpenFirstStage() throws Exception {
        final String TEST_NAME = "test020OpenFirstStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationManager.openNextStage(campaignOid, 1, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign in stage 1", campaign);

        assertStateAndStage(campaign, IN_REVIEW_STAGE, 1);
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertApproximateTime("start time", new Date(), campaign.getStart());
        assertNull("Unexpected end time", campaign.getEnd());
        assertEquals("wrong # of stages", 1, campaign.getStage().size());
        AccessCertificationStageType stage = campaign.getStage().get(0);
        assertEquals("wrong stage #", 1, stage.getNumber());
        assertApproximateTime("stage 1 start", new Date(), stage.getStart());
        assertNotNull("stage 1 end", stage.getEnd());       // too lazy to compute exact datetime
        checkAllCases(campaign.getCase());
    }

    protected void assertDefinitionAndOwner(AccessCertificationCampaignType campaign, PrismObject<? extends ObjectType> certificationDefinition) {
        assertEquals("Unexpected ownerRef", ObjectTypeUtil.createObjectRef(USER_ADMINISTRATOR_OID, ObjectTypes.USER), campaign.getOwnerRef());
        assertEquals("Unexpected definitionRef",
                ObjectTypeUtil.createObjectRef(CERT_DEF_USER_ASSIGNMENT_BASIC_OID, ObjectTypes.ACCESS_CERTIFICATION_DEFINITION),
                campaign.getDefinitionRef());
    }

    @Test
    public void test030SearchAllCases() throws Exception {
        final String TEST_NAME = "test030SearchCases";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCases(caseList);
    }

    @Test
    public void test040SearchCasesFilteredSortedPaged() throws Exception {
        final String TEST_NAME = "test040SearchCasesFilteredSortedPaged";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        Collection<SelectorOptions<GetOperationOptions>> resolveNames =
                SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        ObjectFilter filter = RefFilter.createReferenceEqual(new ItemPath(AccessCertificationCaseType.F_SUBJECT_REF),
                AccessCertificationCaseType.class, prismContext, ObjectTypeUtil.createObjectRef(userAdministrator).asReferenceValue());
        ObjectPaging paging = ObjectPaging.createPaging(2, 2, AccessCertificationCaseType.F_TARGET_REF, OrderDirection.DESCENDING);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter, paging);
        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, query, resolveNames, task, result);

        // THEN
        // Cases for administrator are (ordered by name, descending):
        //  - Superuser
        //  - ERoot
        //  - COO
        //  - CEO
        // so paging (2, 2) should return the last two
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Wrong number of certification cases", 2, caseList.size());
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        assertEquals("Wrong target OID in case #0", ROLE_COO_OID, caseList.get(0).getTargetRef().getOid());
        assertEquals("Wrong target OID in case #1", ROLE_CEO_OID, caseList.get(1).getTargetRef().getOid());
    }

    @Test
    public void test050SearchDecisions() throws Exception {
        final String TEST_NAME = "test050SearchDecisionsAdministrator";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList =
                certificationManager.searchDecisions(null, null, USER_ADMINISTRATOR_OID, false, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Wrong number of certification cases", 6, caseList.size());
        checkAllCases(caseList);
    }

    @Test
    public void test100RecordDecision() throws Exception {
        final String TEST_NAME = "test100RecordDecision";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        AccessCertificationCaseType superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationDecisionType decision = new AccessCertificationDecisionType(prismContext);
        decision.setResponse(AccessCertificationResponseType.ACCEPT);
        decision.setComment("no comment");
        decision.setStageNumber(0);     // will be replaced by current stage number
        ObjectReferenceType administratorRef = ObjectTypeUtil.createObjectRef(USER_ADMINISTRATOR_OID, ObjectTypes.USER);
        decision.setReviewerRef(administratorRef);
        long id = superuserCase.asPrismContainerValue().getId();
        certificationManager.recordDecision(campaignOid, id, decision, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        display("caseList", caseList);
        checkAllCases(caseList);

        superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);
        assertEquals("changed case ID", Long.valueOf(id), superuserCase.asPrismContainerValue().getId());
        assertEquals("wrong # of decisions", 1, superuserCase.getDecision().size());
        AccessCertificationDecisionType storedDecision = superuserCase.getDecision().get(0);
        assertEquals("wrong response", AccessCertificationResponseType.ACCEPT, storedDecision.getResponse());
        assertEquals("wrong comment", "no comment", storedDecision.getComment());
        assertEquals("wrong reviewerRef", administratorRef, storedDecision.getReviewerRef());
        assertEquals("wrong stage number", 1, storedDecision.getStageNumber());
        assertApproximateTime("timestamp", new Date(), storedDecision.getTimestamp());
        assertEquals("wrong current response", AccessCertificationResponseType.ACCEPT, superuserCase.getCurrentResponse());
        assertEquals("wrong enabled", Boolean.TRUE, superuserCase.isEnabled());
    }

    @Test
    public void test105RecordAcceptJackCeo() throws Exception {
        final String TEST_NAME = "test105RecordAcceptJackCeo";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationDecisionType decision = new AccessCertificationDecisionType(prismContext);
        decision.setResponse(AccessCertificationResponseType.ACCEPT);
        decision.setComment("ok");
        decision.setStageNumber(1);
        // reviewerRef will be taken from the current user
        long id = ceoCase.asPrismContainerValue().getId();
        certificationManager.recordDecision(campaignOid, id, decision, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        display("caseList", caseList);
        checkAllCases(caseList);

        ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
        assertEquals("wrong # of decisions", 1, ceoCase.getDecision().size());
        AccessCertificationDecisionType storedDecision = ceoCase.getDecision().get(0);
        assertEquals("wrong response", AccessCertificationResponseType.ACCEPT, storedDecision.getResponse());
        assertEquals("wrong comment", "ok", storedDecision.getComment());
        assertEquals("wrong reviewerRef", ObjectTypeUtil.createObjectRef(USER_ADMINISTRATOR_OID, ObjectTypes.USER), storedDecision.getReviewerRef());
        assertEquals("wrong stage number", 1, storedDecision.getStageNumber());
        assertApproximateTime("timestamp", new Date(), storedDecision.getTimestamp());
        assertEquals("wrong current response", AccessCertificationResponseType.ACCEPT, ceoCase.getCurrentResponse());
        assertEquals("wrong enabled", Boolean.TRUE, ceoCase.isEnabled());
    }

    @Test
    public void test110RecordRevokeJackCeo() throws Exception {
        final String TEST_NAME = "test110RecordRevokeJackCeo";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationDecisionType decision = new AccessCertificationDecisionType(prismContext);
        decision.setResponse(AccessCertificationResponseType.REVOKE);
        decision.setComment("no way");
        decision.setStageNumber(1);
        // reviewerRef will be taken from the current user
        long id = ceoCase.asPrismContainerValue().getId();
        certificationManager.recordDecision(campaignOid, id, decision, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        display("caseList", caseList);
        checkAllCases(caseList);

        ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        display("CEO case", ceoCase.asPrismContainerValue());
        assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
        assertEquals("wrong # of decisions", 1, ceoCase.getDecision().size());
        AccessCertificationDecisionType storedDecision = ceoCase.getDecision().get(0);
        assertEquals("wrong response", AccessCertificationResponseType.REVOKE, storedDecision.getResponse());
        assertEquals("wrong comment", "no way", storedDecision.getComment());
        assertEquals("wrong reviewerRef", ObjectTypeUtil.createObjectRef(USER_ADMINISTRATOR_OID, ObjectTypes.USER), storedDecision.getReviewerRef());
        assertEquals("wrong stage number", 1, storedDecision.getStageNumber());
        assertApproximateTime("timestamp", new Date(), storedDecision.getTimestamp());
        assertEquals("wrong current response", AccessCertificationResponseType.REVOKE, ceoCase.getCurrentResponse());
        assertEquals("wrong enabled", Boolean.TRUE, ceoCase.isEnabled());
    }

    protected void checkAllCases(Collection<AccessCertificationCaseType> caseList) {
        assertEquals("Wrong number of certification cases", 6, caseList.size());
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator);
        checkCase(caseList, USER_JACK_OID, ROLE_CEO_OID, userJack);
        checkCase(caseList, USER_JACK_OID, ORG_EROOT_OID, userJack);
    }

    @Test
    public void test150CloseFirstStage() throws Exception {
        final String TEST_NAME = "test150CloseFirstStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationManager.closeCurrentStage(campaignOid, 1, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign in stage 1", campaign);

        assertStateAndStage(campaign, REVIEW_STAGE_DONE, 1);
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertNull("Unexpected end time", campaign.getEnd());
        assertEquals("wrong # of stages", 1, campaign.getStage().size());
        AccessCertificationStageType stage = campaign.getStage().get(0);
        assertEquals("wrong stage #", 1, stage.getNumber());
        assertApproximateTime("stage 1 start", new Date(), stage.getStart());
        //assertApproximateTime("stage 1 end", new Date(), stage.getStart());       // TODO when implemented
        checkAllCases(campaign.getCase());
    }

    @Test
    public void test200StartRemediation() throws Exception {
        final String TEST_NAME = "test200StartRemediation";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationManager.startRemediation(campaignOid, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        AccessCertificationCampaignType campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign after remediation start", campaign);
        assertTrue("wrong campaign state: " + campaign.getState(), campaign.getState() == CLOSED || campaign.getState() == IN_REMEDIATION);

        RefFilter taskFilter = RefFilter.createReferenceEqual(new ItemPath(TaskType.F_OBJECT_REF), TaskType.class, prismContext, ObjectTypeUtil.createObjectRef(campaign).asReferenceValue());
        List<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, ObjectQuery.createObjectQuery(taskFilter), null, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid(), true);

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        assertEquals("wrong campaign state", CLOSED, campaign.getState());
        assertEquals("wrong campaign stage", 2, campaign.getCurrentStageNumber());
        assertDefinitionAndOwner(campaign, certificationDefinition);
        // TODO assertApproximateTime("end time", new Date(), campaign.getEnd());
        assertEquals("wrong # of stages", 1, campaign.getStage().size());
        //assertApproximateTime("stage 1 end", new Date(), stage.getStart());       // TODO when implemented

        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, null, null, task, result);
        AccessCertificationCaseType jackCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        assertApproximateTime("ceoDummyCase.remediedTimestamp", new Date(), jackCase.getRemediedTimestamp());

        userJack = getUser(USER_JACK_OID).asObjectable();
        display("jack", userJack);
        assertEquals("wrong # of jack's assignments", 1, userJack.getAssignment().size());
        assertEquals("wrong target OID", ORG_EROOT_OID, userJack.getAssignment().get(0).getTargetRef().getOid());
    }
}
