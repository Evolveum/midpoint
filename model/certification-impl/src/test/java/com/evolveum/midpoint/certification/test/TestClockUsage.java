/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.certification.impl.AccessCertificationCloseStageApproachingTriggerHandler;
import com.evolveum.midpoint.certification.impl.AccessCertificationCloseStageTriggerHandler;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
        assertThat(tasks).as("related tasks").hasSize(1);
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

        assertTimestamp(updatedCase.getRemediedTimestamp(), overriddenNow, "remedied timestamp");
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
        assertThat(tasks).as("related tasks").hasSize(1);
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType openedCampaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        AccessCertificationStageType stage = openedCampaign.getStage().get(0);

        assertTimestamp(openedCampaign.getStartTimestamp(), overriddenNow, "campaign start timestamp");
        assertTimestamp(stage.getStartTimestamp(), overriddenNow, "stage start timestamp");

        assertThat(openedCampaign.getTrigger())
                .as("campaign triggers, notify-before-deadline one is created only from logical time")
                .extracting(TriggerType::getHandlerUri)
                .containsExactlyInAnyOrder(
                        AccessCertificationCloseStageTriggerHandler.HANDLER_URI,
                        AccessCertificationCloseStageApproachingTriggerHandler.HANDLER_URI);
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
        assertThat(tasks).as("related tasks").hasSize(1);
        waitForTaskFinish(tasks.get(0).getOid());

        List<Message> messages = dummyTransport.getMessages("dummy:simpleReviewerNotifier");
        assertThat(messages)
                .as("reviewer notifications")
                .isNotEmpty()
                .extracting(Message::getBody)
                .as("reviewer notification bodies, remaining time computed from logical time")
                .anySatisfy(body -> assertThat(body).contains("The stage ends in ", "14 day"))
                .noneSatisfy(body -> assertThat(body).contains("The stage should have ended"));
    }

    private void assertTimestamp(XMLGregorianCalendar actual, XMLGregorianCalendar expected, String description) {
        assertThat(actual).as(description).isNotNull();
        assertThat(XmlTypeConverter.toMillis(actual))
                .as(description + " should follow the clock")
                .isEqualTo(XmlTypeConverter.toMillis(expected));
    }
}
