/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * Checks that certification honors the logical {@link com.evolveum.midpoint.common.Clock}
 * when it is overridden: timestamps, triggers and notification texts (issue 11008).
 */
@ContextConfiguration(locations = { "classpath:ctx-certification-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestClockUsage extends AbstractCertificationTest {

    private static final File CERT_DEF_USER_ASSIGNMENT_BASIC_FILE =
            new File(COMMON_DIR, "certification-of-eroot-user-assignments.xml");
    private static final File CERT_DEF_USER_ASSIGNMENT_CLOCK_FILE =
            new File(COMMON_DIR, "certification-of-eroot-user-assignments-clock.xml");

    private AccessCertificationDefinitionType certificationDefinition;
    private AccessCertificationDefinitionType clockAwareCertificationDefinition;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        certificationDefinition = repoAddObjectFromFile(
                CERT_DEF_USER_ASSIGNMENT_BASIC_FILE,
                AccessCertificationDefinitionType.class,
                initResult).asObjectable();
        clockAwareCertificationDefinition = repoAddObjectFromFile(
                CERT_DEF_USER_ASSIGNMENT_CLOCK_FILE,
                AccessCertificationDefinitionType.class,
                initResult).asObjectable();

        notificationManager.setDisabled(false);
    }

    @BeforeMethod
    public void resetClock() {
        clock.resetOverride();
        dummyTransport.clearMessages();
    }

    @AfterMethod
    public void cleanupClock() {
        clock.resetOverride();
    }

    @Test
    public void test100CertificationCaseOperationsUseClockForRemediedTimestamp() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(userAdministrator.asPrismObject());

        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certificationDefinition.getOid(), task, result);
        String campaignOid = campaign.getOid();

        XMLGregorianCalendar startedAfter = clock.currentTimeXMLGregorianCalendar();
        certificationService.openNextStage(campaignOid, task, result);
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getFirstStageTasks(campaignOid, startedAfter, result);
        assertEquals("Unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCaseType aCase = findCase(
                queryHelper.searchCases(campaignOid, null, result),
                USER_JACK_OID,
                ROLE_CEO_OID);

        XMLGregorianCalendar overriddenNow =
                XmlTypeConverter.createXMLGregorianCalendar("2026-04-09T10:15:30.000+02:00");
        clock.override(overriddenNow);

        operationsHelper.markCaseAsRemedied(campaignOid, aCase.getId(), task, result);

        AccessCertificationCaseType updatedCase = findCase(
                queryHelper.searchCases(campaignOid, null, result),
                USER_JACK_OID,
                ROLE_CEO_OID);

        assertEquals(
                "Remedied timestamp should follow Clock",
                0,
                XmlTypeConverter.compareMillis(overriddenNow, updatedCase.getRemediedTimestamp()));
    }

    @Test
    public void test110CampaignStageOpenUsesClockForTimestampsAndTriggers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(userAdministrator.asPrismObject());

        XMLGregorianCalendar overriddenNow =
                XmlTypeConverter.createXMLGregorianCalendar("2000-01-01T10:15:30.000+01:00");
        clock.override(overriddenNow);

        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(clockAwareCertificationDefinition.getOid(), task, result);
        String campaignOid = campaign.getOid();

        XMLGregorianCalendar startedAfter = clock.currentTimeXMLGregorianCalendar();
        certificationService.openNextStage(campaignOid, task, result);
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getFirstStageTasks(campaignOid, startedAfter, result);
        assertEquals("Unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType openedCampaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        AccessCertificationStageType stage = openedCampaign.getStage().get(0);

        assertEquals(
                "Campaign start timestamp should follow Clock",
                0,
                XmlTypeConverter.compareMillis(overriddenNow, openedCampaign.getStartTimestamp()));
        assertEquals(
                "Stage start timestamp should follow Clock",
                0,
                XmlTypeConverter.compareMillis(overriddenNow, stage.getStartTimestamp()));

        assertNotNull("Campaign triggers should be present", openedCampaign.getTrigger());
        assertEquals("Wrong number of campaign triggers", 2, openedCampaign.getTrigger().size());
        assertTrue(
                "Expected notify-before-deadline trigger to be created from logical time",
                openedCampaign.getTrigger().stream()
                        .map(TriggerType::getHandlerUri)
                        .anyMatch(uri -> uri != null && uri.contains("close-stage-approaching")));
    }

    @Test
    public void test120ReviewerNotificationUsesClockForRemainingTime() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(userAdministrator.asPrismObject());

        XMLGregorianCalendar overriddenNow =
                XmlTypeConverter.createXMLGregorianCalendar("2000-01-01T10:15:30.000+01:00");
        clock.override(overriddenNow);

        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certificationDefinition.getOid(), task, result);
        String campaignOid = campaign.getOid();

        XMLGregorianCalendar startedAfter = clock.currentTimeXMLGregorianCalendar();
        certificationService.openNextStage(campaignOid, task, result);
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getFirstStageTasks(campaignOid, startedAfter, result);
        assertEquals("Unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        List<Message> messages = dummyTransport.getMessages("dummy:simpleReviewerNotifier");
        assertTrue("Expected reviewer notifications to be sent", !messages.isEmpty());
        assertTrue(
                "Reviewer notification should compute remaining time from Clock",
                messages.stream()
                        .map(Message::getBody)
                        .filter(body -> body != null && body.contains("The stage ends in "))
                        .anyMatch(body -> body.contains("14 day")));
        assertTrue(
                "Reviewer notification should not treat the stage as already expired",
                messages.stream()
                        .map(Message::getBody)
                        .filter(body -> body != null)
                        .noneMatch(body -> body.contains("The stage should have ended")));
    }
}
