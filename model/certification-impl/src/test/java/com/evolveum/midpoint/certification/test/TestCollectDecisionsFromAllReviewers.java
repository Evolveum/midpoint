/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.test;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.notifications.impl.SimpleObjectRefImpl;
import com.evolveum.midpoint.notifications.impl.events.CertReviewEventImpl;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests for the collectDecisionsFromAllReviewers query parameter.
 *
 * Verifies that QueryUtils.createQueryForOpenWorkItems and CertReviewEventImpl
 * correctly filter work items and notification cases based on the
 * collectDecisionsFromAllReviewers parameter (true and false).
 *
 * Uses a single campaign with 2 reviewers per case (administrator + jack)
 * and oneAcceptAccepts strategy. After jack accepts one case, verifies
 * the different behavior when the parameter is true or false.
 */
@ContextConfiguration(locations = { "classpath:ctx-certification-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCollectDecisionsFromAllReviewers extends AbstractCertificationTest {

    private static final File CERT_DEF_FILE =
            new File(COMMON_DIR, "certification-collect-decisions-test.xml");

    @Autowired
    private LightweightIdentifierGenerator idGenerator;

    private AccessCertificationDefinitionType certDefinition;

    private String campaignOid;
    private int caseCount;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        certDefinition = repoAddObjectFromFile(CERT_DEF_FILE,
                AccessCertificationDefinitionType.class, initResult).asObjectable();
    }

    /**
     * Verify test environment is properly initialized.
     */
    @Test
    public void test000Sanity() throws Exception {
        assertNotNull("Certification definition not loaded", certDefinition);
        display("Certification definition", certDefinition);
    }

    /**
     * Create a campaign and open first stage.
     * Verify each case has 2 work items (administrator + jack).
     */
    @Test
    public void test010CreateCampaignAndOpenStage() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certDefinition.getOid(), task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();
        campaign = getCampaignWithCases(campaignOid);
        display("Campaign", campaign);
        assertSanityAfterCampaignCreate(campaign, certDefinition);

        // Open first stage
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        task.setOwner(userAdministrator.asPrismObject());
        certificationService.openNextStage(campaignOid, task, result);

        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getFirstStageTasks(campaignOid, startTime, result);
        assertEquals("Unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        campaign = getCampaignWithCases(campaignOid);
        display("Campaign after opening stage 1", campaign);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        caseCount = caseList.size();
        assertTrue("No cases generated", caseCount > 0);
        displayValue("Number of cases", caseCount);

        for (AccessCertificationCaseType aCase : caseList) {
            List<AccessCertificationWorkItemType> workItems = aCase.getWorkItem();
            assertEquals("Wrong number of work items for case " + aCase, 2, workItems.size());

            assertNotNull("No work item for administrator in case " + aCase,
                    findWorkItem(aCase, 1, 1, USER_ADMINISTRATOR_OID));
            assertNotNull("No work item for jack in case " + aCase,
                    findWorkItem(aCase, 1, 1, USER_JACK_OID));
        }
    }

    /**
     * Jack accepts one case. Then verify behavior with collectTrue and collectFalse.
     * With collectTrue: admin's work item for the decided case is still visible.
     * With collectFalse: admin's work item for the decided case is excluded.
     */
    @Test
    public void test100RecordDecisionAndVerifyFiltering() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        AccessCertificationCaseType targetCase = caseList.get(0);
        long targetCaseId = targetCase.asPrismContainerValue().getId();

        when();
        recordDecision(campaignOid, targetCase, ACCEPT, "test accept", USER_JACK_OID, task, result);

        then();
        caseList = queryHelper.searchCases(campaignOid, null, result);
        targetCase = caseList.stream()
                .filter(c -> c.asPrismContainerValue().getId().equals(targetCaseId))
                .findFirst().orElseThrow();
        display("Target case after decision", targetCase);

        assertCaseOutcome(caseList, targetCase.getObjectRef().getOid(), targetCase.getTargetRef().getOid(),
                ACCEPT, ACCEPT, null);

        AccessCertificationWorkItemType adminWi = findWorkItem(targetCase, 1, 1, USER_ADMINISTRATOR_OID);
        assertNotNull("Admin work item not found", adminWi);
        assertNull("Admin work item should have no outcome", WorkItemTypeUtil.getOutcome(adminWi));

        MidPointPrincipal adminPrincipal = MidPointPrincipal.create(
                getUser(USER_ADMINISTRATOR_OID).asObjectable());
        ObjectQuery wiQuery = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .exists(PrismConstants.T_PARENT)
                .ownerId(campaignOid)
                .build();

        // collectTrue: decided case's work items should still be visible
        ObjectQuery queryTrue = QueryUtils.createQueryForOpenWorkItems(wiQuery.clone(), adminPrincipal, true, true);
        List<AccessCertificationWorkItemType> workItemsTrue = repositoryService.searchContainers(
                AccessCertificationWorkItemType.class, queryTrue, null, result);
        assertEquals("With collectTrue parameter, all admin's undecided work items should be visible",
                caseCount, workItemsTrue.size());

        // collectFalse: decided case should be excluded
        ObjectQuery queryFalse = QueryUtils.createQueryForOpenWorkItems(wiQuery.clone(), adminPrincipal, true, false);
        List<AccessCertificationWorkItemType> workItemsFalse = repositoryService.searchContainers(
                AccessCertificationWorkItemType.class, queryFalse, null, result);
        assertEquals("With collectFalse parameter, decided case should be excluded",
                caseCount - 1, workItemsFalse.size());

        // principal=null with collectFalse: decided case excluded for all reviewers
        ObjectQuery wiQueryNoPrincipal = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .exists(PrismConstants.T_PARENT)
                .ownerId(campaignOid)
                .build();
        ObjectQuery queryNoPrincipalFalse = QueryUtils.createQueryForOpenWorkItems(
                wiQueryNoPrincipal, null, true, false);
        int wiCountNoPrincipal = modelService.countContainers(
                AccessCertificationWorkItemType.class, queryNoPrincipalFalse, null, task, result);
        assertEquals("With principal=null and collectFalse, decided case work items should be excluded",
                (caseCount - 1) * 2, wiCountNoPrincipal);
    }

    /**
     * Verify that a deputy of administrator can see work items via QueryUtils.
     * With collectTrue: all admin's undecided work items visible.
     * With collectFalse: decided case excluded.
     */
    @Test
    public void test110QueryWithDeputy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        login(getUserFromRepo(USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_OID));
        MidPointPrincipal deputyPrincipal = securityContextManager.getPrincipal();

        when();
        ObjectQuery wiQuery = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .exists(PrismConstants.T_PARENT)
                .ownerId(campaignOid)
                .build();

        // collectTrue: deputy sees all admin's undecided work items
        ObjectQuery queryTrue = QueryUtils.createQueryForOpenWorkItems(wiQuery.clone(), deputyPrincipal, true, true);
        List<AccessCertificationWorkItemType> workItemsTrue = repositoryService.searchContainers(
                AccessCertificationWorkItemType.class, queryTrue, null, result);

        // collectFalse: deputy also has decided case excluded
        ObjectQuery queryFalse = QueryUtils.createQueryForOpenWorkItems(wiQuery.clone(), deputyPrincipal, true, false);
        List<AccessCertificationWorkItemType> workItemsFalse = repositoryService.searchContainers(
                AccessCertificationWorkItemType.class, queryFalse, null, result);

        then();
        display("Work items visible to deputy with collectTrue", workItemsTrue);
        assertEquals("Deputy with collectTrue: all admin's undecided work items",
                caseCount, workItemsTrue.size());

        display("Work items visible to deputy with collectFalse", workItemsFalse);
        assertEquals("Deputy with collectFalse: decided case should be excluded",
                caseCount - 1, workItemsFalse.size());

        login(userAdministrator.asPrismObject());
    }

    /**
     * Verify that with notDecidedOnly=false, all work items are returned
     * regardless of collectDecisionsFromAllReviewers parameter.
     * The case-level filter (hiding decided cases) requires both
     * collectDecisionsFromAllReviewers=false AND notDecidedOnly=true.
     */
    @Test
    public void test120QueryWithNotDecidedOnlyFalse() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        MidPointPrincipal adminPrincipal = MidPointPrincipal.create(
                getUser(USER_ADMINISTRATOR_OID).asObjectable());
        ObjectQuery wiQuery = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .exists(PrismConstants.T_PARENT)
                .ownerId(campaignOid)
                .build();

        when();
        ObjectQuery queryAllCollectFalse = QueryUtils.createQueryForOpenWorkItems(
                wiQuery.clone(), adminPrincipal, false, false);
        List<AccessCertificationWorkItemType> allItemsCollectFalse = repositoryService.searchContainers(
                AccessCertificationWorkItemType.class, queryAllCollectFalse, null, result);

        ObjectQuery queryAllCollectTrue = QueryUtils.createQueryForOpenWorkItems(
                wiQuery.clone(), adminPrincipal, false, true);
        List<AccessCertificationWorkItemType> allItemsCollectTrue = repositoryService.searchContainers(
                AccessCertificationWorkItemType.class, queryAllCollectTrue, null, result);

        then();
        assertEquals("notDecidedOnly=false, collectFalse: all admin work items should be visible",
                caseCount, allItemsCollectFalse.size());
        assertEquals("notDecidedOnly=false, collectTrue: all admin work items should be visible",
                caseCount, allItemsCollectTrue.size());
    }

    /**
     * Verify notification filtering via CertReviewEventImpl.
     * With collectTrue: all cases await admin response (decided case included).
     * With collectFalse: decided case excluded from awaiting list.
     */
    @Test
    public void test130NotificationFiltering() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        SimpleObjectRefImpl adminRef = new SimpleObjectRefImpl(
                getUser(USER_ADMINISTRATOR_OID).asObjectable());

        when();
        CertReviewEventImpl eventTrue = new CertReviewEventImpl(
                idGenerator, campaign.getCase(), campaign, EventOperationType.MODIFY, true);
        eventTrue.setActualReviewer(adminRef);
        Collection<AccessCertificationCaseType> awaitingTrue = eventTrue.getCasesAwaitingResponseFromActualReviewer();

        CertReviewEventImpl eventFalse = new CertReviewEventImpl(
                idGenerator, campaign.getCase(), campaign, EventOperationType.MODIFY, false);
        eventFalse.setActualReviewer(adminRef);
        Collection<AccessCertificationCaseType> awaitingFalse = eventFalse.getCasesAwaitingResponseFromActualReviewer();

        then();
        assertEquals("With collectTrue, all cases should await admin response",
                caseCount, awaitingTrue.size());
        assertEquals("With collectFalse, decided case should be excluded from notifications",
                caseCount - 1, awaitingFalse.size());
    }
}
