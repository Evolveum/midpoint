/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.test;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.ENABLED;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.security.api.SecurityUtil;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Very simple certification test.
 * Tests just the basic functionality, along with security features.
 */
@ContextConfiguration(locations = { "classpath:ctx-certification-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestManualEscalation extends AbstractCertificationTest {

    protected static final File CERT_DEF_USER_ASSIGNMENT_BASIC_FILE =
            new File(COMMON_DIR, "certification-of-eroot-user-assignments.xml");

    protected AccessCertificationDefinitionType certificationDefinition;

    private String campaignOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        certificationDefinition = repoAddObjectFromFile(CERT_DEF_USER_ASSIGNMENT_BASIC_FILE,
                AccessCertificationDefinitionType.class, initResult).asObjectable();

        // to test MID-3838
        assignFocus(UserType.class, USER_JACK_OID, UserType.COMPLEX_TYPE, USER_GUYBRUSH_OID, SchemaConstants.ORG_DEPUTY, null, initTask, initResult);
    }

    @Test
    public void test010CreateCampaign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_OID));

        // WHEN
        when();
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certificationDefinition.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign", campaign);
        assertSanityAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentCompleteAll(campaign, 100, 100, 100);      // no cases, no problems
    }

    @Test
    public void test013SearchAllCasesAllowed() throws Exception {
        login(getUserFromRepo(USER_BOB_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, CertCampaignTypeUtil.createCasesForCampaignQuery(campaignOid),
                null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }

    @Test
    public void test021OpenFirstStageAllowed() throws Exception {
        // GIVEN
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_OID));

        // WHEN
        when();
        certificationService.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getFirstStageTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);

        assertSanityAfterCampaignStart(campaign, certificationDefinition, 7);
        checkAllCases(campaign.getCase());
        List<AccessCertificationCaseType> caseList = campaign.getCase();
        // no responses -> NO_RESPONSE in all cases
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);

        assertPercentCompleteAll(campaign, 0, 0, 0);
    }

    @Test
    public void test032SearchAllCasesAllowed() throws Exception {
        login(getUserFromRepo(USER_BOB_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, null, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCases(caseList);
    }

    @Test
    public void test050SearchWorkItemsAsAdministrator() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(
                        CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid),
                        false, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkAllWorkItems(workItems);
    }

    @Test
    public void test100RecordDecision() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        AccessCertificationCaseType superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);

        // WHEN
        when();
        AccessCertificationWorkItemType workItem = findWorkItem(superuserCase, 1, 1, USER_ADMINISTRATOR_OID);
        long id = superuserCase.asPrismContainerValue().getId();
        certificationService.recordDecision(campaignOid, id, workItem.getId(), ACCEPT, "no comment", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);
        checkAllCases(caseList);

        superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);
        assertEquals("changed case ID", Long.valueOf(id), superuserCase.asPrismContainerValue().getId());
        assertSingleDecision(superuserCase, ACCEPT, "no comment", 1, 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f));      // 1 reviewer per case (always administrator)
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return (PrismObject<UserType>) SecurityUtil.getPrincipalSilent().getFocus().asPrismObject();
    }

    @Test
    public void test110Escalate() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        EscalateWorkItemActionType action = new EscalateWorkItemActionType()
                .approverRef(USER_JACK_OID, UserType.COMPLEX_TYPE)
                .delegationMethod(WorkItemDelegationMethodType.ADD_ASSIGNEES)
                .escalationLevelName("ESC-1");
        operationsHelper.escalateCampaign(campaignOid, action, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);
        checkAllCases(caseList);

        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        display("CEO case after escalation", ceoCase);

        AccessCertificationWorkItemType workItem = findWorkItem(ceoCase, 1, 1, USER_ADMINISTRATOR_OID);
        assertObjectRefs("assignees", false, workItem.getAssigneeRef(), USER_JACK_OID, USER_ADMINISTRATOR_OID);
        assertEquals("Wrong originalAssignee OID", USER_ADMINISTRATOR_OID, workItem.getOriginalAssigneeRef().getOid());
        final WorkItemEscalationLevelType newEscalationLevel = new WorkItemEscalationLevelType().number(1).name("ESC-1");
        assertEquals("Wrong escalation info", newEscalationLevel, workItem.getEscalationLevel());
        assertEquals("Wrong # of events", 1, ceoCase.getEvent().size());
        WorkItemEscalationEventType event = (WorkItemEscalationEventType) ceoCase.getEvent().get(0);
        assertNotNull("No timestamp in event", event.getTimestamp());
        assertEquals("Wrong initiatorRef OID", USER_ADMINISTRATOR_OID, event.getInitiatorRef().getOid());
        assertEquals("Wrong workItemId", workItem.getId(), event.getWorkItemId());
        assertObjectRefs("assigneeBefore", false, event.getAssigneeBefore(), USER_ADMINISTRATOR_OID);
        assertObjectRefs("delegatedTo", false, event.getDelegatedTo(), USER_JACK_OID);
        assertEquals("Wrong delegationMethod", WorkItemDelegationMethodType.ADD_ASSIGNEES, event.getDelegationMethod());
        assertEquals("Wrong new escalation level", newEscalationLevel, event.getNewEscalationLevel());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f));
    }

    @Test
    public void test120EscalateAgain() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        EscalateWorkItemActionType action = new EscalateWorkItemActionType()
                .approverRef(USER_ELAINE_OID, UserType.COMPLEX_TYPE)
                .escalationLevelName("ESC-2");
        operationsHelper.escalateCampaign(campaignOid, action, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);
        checkAllCases(caseList);

        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        display("CEO case after escalation", ceoCase);
        AccessCertificationWorkItemType workItem = findWorkItem(ceoCase, 1, 1, USER_ELAINE_OID);
        assertNotNull("No work item found", workItem);
        assertObjectRefs("assignees", false, workItem.getAssigneeRef(), USER_ELAINE_OID);
        assertEquals("Wrong originalAssignee OID", USER_ADMINISTRATOR_OID, workItem.getOriginalAssigneeRef().getOid());
        final WorkItemEscalationLevelType oldEscalationLevel = new WorkItemEscalationLevelType().number(1).name("ESC-1");
        final WorkItemEscalationLevelType newEscalationLevel = new WorkItemEscalationLevelType().number(2).name("ESC-2");
        assertEquals("Wrong escalation info", newEscalationLevel, workItem.getEscalationLevel());
        assertEquals("Wrong # of events", 2, ceoCase.getEvent().size());
        WorkItemEscalationEventType event = (WorkItemEscalationEventType) ceoCase.getEvent().get(1);
        assertNotNull("No timestamp in event", event.getTimestamp());
        assertEquals("Wrong initiatorRef OID", USER_ADMINISTRATOR_OID, event.getInitiatorRef().getOid());
        assertEquals("Wrong workItemId", workItem.getId(), event.getWorkItemId());
        assertObjectRefs("assigneeBefore", false, event.getAssigneeBefore(), USER_ADMINISTRATOR_OID, USER_JACK_OID);
        assertObjectRefs("delegatedTo", false, event.getDelegatedTo(), USER_ELAINE_OID);
        assertEquals("Wrong delegationMethod", WorkItemDelegationMethodType.REPLACE_ASSIGNEES, event.getDelegationMethod());
        assertEquals("Wrong old escalation level", oldEscalationLevel, event.getEscalationLevel());
        assertEquals("Wrong new escalation level", newEscalationLevel, event.getNewEscalationLevel());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f));
    }

    @Test
    public void test130Delegate() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        display("CEO case", ceoCase);

        // WHEN
        when();
        AccessCertificationWorkItemType workItem = findWorkItem(ceoCase, 1, 1, USER_ELAINE_OID);
        long id = ceoCase.asPrismContainerValue().getId();
        DelegateWorkItemActionType action = new DelegateWorkItemActionType()
                .approverRef(USER_ADMINISTRATOR_OID, UserType.COMPLEX_TYPE)
                .approverRef(USER_JACK_OID, UserType.COMPLEX_TYPE);
        certificationManager.delegateWorkItems(campaignOid, Collections.singletonList(workItem), action, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);
        checkAllCases(caseList);

        ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        display("CEO case after escalation", ceoCase);
        assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
        workItem = findWorkItem(ceoCase, 1, 1, USER_ADMINISTRATOR_OID);
        assertNotNull("No work item found", workItem);
        assertObjectRefs("assignees", false, workItem.getAssigneeRef(), USER_ADMINISTRATOR_OID, USER_JACK_OID);
        assertEquals("Wrong originalAssignee OID", USER_ADMINISTRATOR_OID, workItem.getOriginalAssigneeRef().getOid());
        final WorkItemEscalationLevelType oldEscalationLevel = new WorkItemEscalationLevelType().number(2).name("ESC-2");
        assertEquals("Wrong escalation info", oldEscalationLevel, workItem.getEscalationLevel());
        assertEquals("Wrong # of events", 3, ceoCase.getEvent().size());
        WorkItemDelegationEventType event = (WorkItemDelegationEventType) ceoCase.getEvent().get(2);
        assertFalse("Event is Escalation one, although it shouldn't", event instanceof WorkItemEscalationEventType);
        assertNotNull("No timestamp in event", event.getTimestamp());
        assertEquals("Wrong initiatorRef OID", USER_ADMINISTRATOR_OID, event.getInitiatorRef().getOid());
        assertEquals("Wrong workItemId", workItem.getId(), event.getWorkItemId());
        assertObjectRefs("assigneeBefore", false, event.getAssigneeBefore(), USER_ELAINE_OID);
        assertObjectRefs("delegatedTo", false, event.getDelegatedTo(), USER_ADMINISTRATOR_OID, USER_JACK_OID);
        assertEquals("Wrong delegationMethod", WorkItemDelegationMethodType.REPLACE_ASSIGNEES, event.getDelegationMethod());
        assertEquals("Wrong old escalation level", oldEscalationLevel, event.getEscalationLevel());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f));
    }

    private void checkAllCases(Collection<AccessCertificationCaseType> caseList) {
        assertEquals("Wrong number of certification cases", 7, caseList.size());
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator);
        checkCaseSanity(caseList, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkCaseSanity(caseList, USER_JACK_OID, ORG_EROOT_OID, userJack);
    }

    private void checkAllWorkItems(Collection<AccessCertificationWorkItemType> workItems) {
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkWorkItemSanity(workItems, USER_JACK_OID, ORG_EROOT_OID, userJack);
    }
}
