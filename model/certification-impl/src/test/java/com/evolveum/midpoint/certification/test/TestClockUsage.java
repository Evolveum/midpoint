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
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.certification.impl.AccCertCaseOperationsHelper;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAffectedObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BasicObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationStartCampaignWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAffectedObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

@ContextConfiguration(locations = { "classpath:ctx-certification-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestClockUsage extends AbstractUninitializedCertificationTest {

    private static final File CERT_DEF_USER_ASSIGNMENT_BASIC_FILE =
            new File(COMMON_DIR, "certification-of-eroot-user-assignments.xml");
    private static final File CERT_DEF_USER_ASSIGNMENT_CLOCK_FILE =
            new File(COMMON_DIR, "certification-of-eroot-user-assignments-clock.xml");

    private static final File ORGS_AND_USERS_FILE = new File(COMMON_DIR, "orgs-and-users.xml");
    private static final File USER_BOB_FILE = new File(COMMON_DIR, "user-bob.xml");
    private static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    private static final File ROLE_REVIEWER_FILE = new File(COMMON_DIR, "role-reviewer.xml");
    private static final File ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_FILE =
            new File(COMMON_DIR, "role-eroot-user-assignment-campaign-owner.xml");
    private static final File METAROLE_CXO_FILE = new File(COMMON_DIR, "metarole-cxo.xml");
    private static final File ROLE_CEO_FILE = new File(COMMON_DIR, "role-ceo.xml");
    private static final File ROLE_COO_FILE = new File(COMMON_DIR, "role-coo.xml");
    private static final File ROLE_CTO_FILE = new File(COMMON_DIR, "role-cto.xml");

    private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
    private static final String ROLE_CEO_OID = "00000000-d34d-b33f-f00d-000000000001";

    @Autowired private AccCertCaseOperationsHelper operationsHelper;
    @Autowired private NotificationManager notificationManager;

    private AccessCertificationDefinitionType certificationDefinition;
    private AccessCertificationDefinitionType clockAwareCertificationDefinition;

    @Override
    protected File getUserAdministratorFile() {
        return USER_ADMINISTRATOR_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.addCertificationTasks(this, initTask, initResult);

        repoAddObjectFromFile(METAROLE_CXO_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_CEO_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_COO_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_CTO_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_REVIEWER_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_FILE, RoleType.class, initResult);

        repoAddObjectsFromFile(ORGS_AND_USERS_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(USER_BOB_FILE, UserType.class, initResult);
        repoAddObjectFromFile(USER_JACK_FILE, UserType.class, initResult);

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

    private AccessCertificationCaseType findCase(
            List<AccessCertificationCaseType> caseList, String subjectOid, String targetOid) {
        return caseList.stream()
                .filter(aCase -> aCase.getTargetRef() != null)
                .filter(aCase -> targetOid.equals(aCase.getTargetRef().getOid()))
                .filter(aCase -> aCase.getObjectRef() != null)
                .filter(aCase -> subjectOid.equals(aCase.getObjectRef().getOid()))
                .findAny()
                .orElse(null);
    }

    private List<PrismObject<TaskType>> getFirstStageTasks(
            String campaignOid, XMLGregorianCalendar startTime, OperationResult result) throws Exception {
        if (isNativeRepository()) {
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_CERTIFICATION_START_CAMPAIGN_TASK.value())
                    .and()
                    .item(
                            ItemPath.create(
                                    TaskType.F_AFFECTED_OBJECTS,
                                    TaskAffectedObjectsType.F_ACTIVITY,
                                    ActivityAffectedObjectsType.F_OBJECTS,
                                    BasicObjectSetType.F_OBJECT_REF))
                    .ref(campaignOid)
                    .and()
                    .item(
                            ItemPath.create(
                                    TaskType.F_AFFECTED_OBJECTS,
                                    TaskAffectedObjectsType.F_ACTIVITY,
                                    ActivityAffectedObjectsType.F_OBJECTS,
                                    BasicObjectSetType.F_TYPE))
                    .eq(AccessCertificationCampaignType.COMPLEX_TYPE)
                    .and()
                    .block()
                    .item(TaskType.F_LAST_RUN_START_TIMESTAMP).isNull()
                    .or()
                    .item(TaskType.F_LAST_RUN_START_TIMESTAMP).ge(startTime)
                    .endBlock()
                    .build();
            return taskManager.searchObjects(TaskType.class, query, null, result);
        }

        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_CERTIFICATION_START_CAMPAIGN_TASK.value())
                .and()
                .block()
                .item(TaskType.F_LAST_RUN_START_TIMESTAMP).ge(startTime)
                .endBlock()
                .build();

        List<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, query, null, result);
        List<PrismObject<TaskType>> matching = new ArrayList<>();
        for (PrismObject<TaskType> task : tasks) {
            if (campaignOidMatch(task, campaignOid)) {
                matching.add(task);
            }
        }
        return matching;
    }

    private boolean campaignOidMatch(PrismObject<TaskType> task, String campaignOid) {
        var activity = task.asObjectable().getActivity();
        if (activity == null) {
            return false;
        }
        WorkDefinitionsType work = activity.getWork();
        if (work == null) {
            return false;
        }
        CertificationStartCampaignWorkDefinitionType startCampaign = work.getCertificationStartCampaign();
        if (startCampaign == null) {
            return false;
        }
        ObjectReferenceType campaign = startCampaign.getCertificationCampaignRef();
        return campaign != null && campaignOid.equals(campaign.getOid());
    }
}
