package com.evolveum.midpoint.certification.test;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.binding.AbstractMutableContainerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-certification-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCertificationNotifications extends AbstractCertificationTest {
    private static final File CERT_DEF_USER_ASSIGNMENT_BASIC_FILE =
            new File(COMMON_DIR, "certification-of-eroot-user-assignments-notifications.xml");

    private AccessCertificationDefinitionType certificationDefinition;
    private String campaignOid;
    private AccessCertificationEventListenerStub eventListenerStub;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        this.certificationDefinition = repoAddObjectFromFile(CERT_DEF_USER_ASSIGNMENT_BASIC_FILE,
                AccessCertificationDefinitionType.class, initResult).asObjectable();
        this.eventListenerStub = new AccessCertificationEventListenerStub();
        this.certificationManager.registerCertificationEventListener(this.eventListenerStub);
        importTriggerTask(initResult);
    }

    @BeforeMethod
    private void resetEventListenerStub() {
        this.eventListenerStub.reset();
    }

    @Test
    public void test100StartCampaign() throws Exception {
        given("Campaign has been created.");
        final Task task = getTestTask();
        final OperationResult result = task.getResult();
        this.campaignOid = this.certificationService
                .createCampaign(this.certificationDefinition.getOid(), task, result)
                .getOid();
        this.clock.resetOverride();
        final XMLGregorianCalendar startedAfter = this.clock.currentTimeXMLGregorianCalendar();

        when("First campaign stage has started.");
        this.certificationService.openNextStage(this.campaignOid, task, result);

        then("Notifications should be sent to all assigned reviewers.");
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        final List<PrismObject<TaskType>> tasks = getFirstStageTasks(this.campaignOid, startedAfter, result);
        assertEquals("Unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        final AccessCertificationCampaignType campaign = getCampaignWithCases(this.campaignOid);
        display("Campaign in stage 1", campaign);

        assertSanityAfterCampaignStart(campaign, this.certificationDefinition, 7);
        final List<AccessCertificationCaseType> certCases = campaign.getCase();
        checkAllCasesSanity(certCases);

        final List<Long> casesWithAdminAsReviewer = getCaseIds(filterReviewerCases(certCases, USER_ADMINISTRATOR_OID));
        assertTrue("", this.eventListenerStub.reviewRequestedEventSentTo(USER_ADMINISTRATOR_OID, USER_ADMINISTRATOR_OID,
                casesWithAdminAsReviewer, this.campaignOid));
        assertTrue("", this.eventListenerStub.reviewRequestedEventSentTo(USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_OID,
                USER_ADMINISTRATOR_OID, casesWithAdminAsReviewer, this.campaignOid));

        final List<Long> casesWithBobAsReviewer = getCaseIds(filterReviewerCases(certCases, USER_BOB_OID));
        assertTrue("", this.eventListenerStub.reviewRequestedEventSentTo(USER_BOB_OID, USER_BOB_OID,
                casesWithBobAsReviewer, this.campaignOid));
        assertTrue("", this.eventListenerStub.reviewRequestedEventSentTo(USER_BOB_DEPUTY_NO_ASSIGNMENTS_OID, USER_BOB_OID,
                casesWithBobAsReviewer, this.campaignOid));
        assertTrue("", this.eventListenerStub.reviewRequestedEventSentTo(USER_BOB_DEPUTY_FULL_OID, USER_BOB_OID,
                casesWithBobAsReviewer, this.campaignOid));

        final List<Long> casesWithJackAsReviewer = getCaseIds(filterReviewerCases(certCases, USER_JACK_OID));
        assertTrue("", this.eventListenerStub.reviewRequestedEventSentTo(USER_JACK_OID, USER_JACK_OID,
                casesWithJackAsReviewer, this.campaignOid));
    }

    @Test
    public void test110Escalate() throws Exception {
        given("Two reviewers (Bob and Jack) has replied to all work items, while the last (Admin) has not.");
        final Task task = getTestTask();
        final OperationResult result = task.getResult();

        List<AccessCertificationCaseType> certCases = this.queryHelper.searchCases(this.campaignOid, null, result);
        recordNotDecidedDecision(certCases, USER_BOB_OID, task, result);
        recordNotDecidedDecision(certCases, USER_JACK_OID, task, result);

        when("Period for escalation has passed and there still are work items without replies.");
        this.clock.resetOverride();
        this.clock.overrideDuration("P2D"); // first escalation is at P1D
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, 20000, true);

        then("Notifications should be sent to the person (Admin) who has unanswered work items and to the person "
                + "(Jack) handling escalations.");
        result.computeStatus();
        TestUtil.assertSuccess(result);

        certCases = this.queryHelper.searchCases(this.campaignOid, null, result);
        final List<Long> casesWithAdminAsReviewer = getCaseIds(filterReviewerCases(certCases, USER_ADMINISTRATOR_OID));
        assertTrue("", this.eventListenerStub.reviewRequestedEventSentTo(USER_ADMINISTRATOR_OID, USER_ADMINISTRATOR_OID,
                casesWithAdminAsReviewer, this.campaignOid));
        assertTrue("", this.eventListenerStub.reviewRequestedEventSentTo(USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_OID,
                USER_ADMINISTRATOR_OID, casesWithAdminAsReviewer, this.campaignOid));

        // Jack replied to all his work items, so here he should be notified only about escalated cases (that means
        // cases with work items, where Admin is a reviewer). Thus, we use the same cases list as above for the admin.
        assertTrue("", this.eventListenerStub.reviewRequestedEventSentTo(USER_JACK_OID, USER_JACK_OID,
                casesWithAdminAsReviewer, this.campaignOid));

        final List<Long> casesWithBobAsReviewer = getCaseIds(filterReviewerCases(certCases, USER_BOB_OID));
        assertFalse("", this.eventListenerStub.reviewRequestedEventSentTo(USER_BOB_OID, USER_BOB_OID,
                casesWithBobAsReviewer, this.campaignOid));
        assertFalse("", this.eventListenerStub.reviewRequestedEventSentTo(USER_BOB_DEPUTY_NO_ASSIGNMENTS_OID,
                USER_BOB_OID, casesWithBobAsReviewer, this.campaignOid));
        assertFalse("", this.eventListenerStub.reviewRequestedEventSentTo(USER_BOB_DEPUTY_FULL_OID, USER_BOB_OID,
                casesWithBobAsReviewer, this.campaignOid));
    }

    private void checkAllCasesSanity(Collection<AccessCertificationCaseType> caseList) {
        assertEquals("Wrong number of certification cases", 7, caseList.size());
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator);
        checkCaseSanity(caseList, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkCaseSanity(caseList, USER_JACK_OID, ORG_EROOT_OID, userJack);
    }

    private static @NotNull List<Long> getCaseIds(List<AccessCertificationCaseType> certCases) {
        return certCases.stream()
                .map(AbstractMutableContainerable::getId)
                .toList();
    }

    private void recordNotDecidedDecision(Collection<AccessCertificationCaseType> certCases, String reviewerOid,
            Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException {
        final Map<Long, List<AccessCertificationWorkItemType>> workItemsToDecide =
                mapActiveStageWorkItemsWithoutOutcomeToCaseIds(certCases, reviewerOid);
        for (Map.Entry<Long, List<AccessCertificationWorkItemType>> entry : workItemsToDecide.entrySet()) {
            final long caseId = entry.getKey();
            final List<AccessCertificationWorkItemType> workItems = entry.getValue();
            for (final AccessCertificationWorkItemType workItem : workItems) {
                this.certificationService.recordDecision(this.campaignOid, caseId, workItem.getId(),
                        NOT_DECIDED, "", task, result);
            }
        }
    }

    private static Map<Long, List<AccessCertificationWorkItemType>> mapActiveStageWorkItemsWithoutOutcomeToCaseIds(
            Collection<AccessCertificationCaseType> certCases, String reviewerOid) {
        final Map<Long, List<AccessCertificationWorkItemType>> reviewerWorkItems = new HashMap<>();
        for (final AccessCertificationCaseType aCase : certCases) {
            reviewerWorkItems.put(aCase.getId(), aCase.getWorkItem().stream()
                    .filter(WorkItemTypeUtil::isWithoutOutcome)
                    .filter(WorkItemTypeUtil::isOpened) // this means it is from active stage
                    .filter(wi -> ObjectTypeUtil.containsOid(wi.getAssigneeRef(), reviewerOid))
                    .toList());
        }
        return reviewerWorkItems;
    }
}
