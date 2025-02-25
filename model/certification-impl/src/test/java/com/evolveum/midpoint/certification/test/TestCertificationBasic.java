/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.test;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_ACTIVATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.ENABLED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType.F_ADMINISTRATIVE_STATUS;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import com.evolveum.midpoint.util.exception.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Very simple certification test.
 * Tests just the basic functionality, along with security features.
 */
@ContextConfiguration(locations = { "classpath:ctx-certification-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCertificationBasic extends AbstractCertificationTest {

    private static final File CERT_DEF_USER_ASSIGNMENT_BASIC_FILE =
            new File(COMMON_DIR, "certification-of-eroot-user-assignments.xml");

    private AccessCertificationDefinitionType certificationDefinition;
    private AccessCertificationDefinitionType roleInducementCertDefinition;

    private String campaignOid;
    private String roleInducementCampaignOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        certificationDefinition = repoAddObjectFromFile(CERT_DEF_USER_ASSIGNMENT_BASIC_FILE,
                AccessCertificationDefinitionType.class, initResult).asObjectable();

        // to test MID-3838
        assignFocus(
                UserType.class, USER_JACK_OID, UserType.COMPLEX_TYPE, USER_GUYBRUSH_OID, SchemaConstants.ORG_DEPUTY,
                null, initTask, initResult);
    }

    /**
     * "Foreign" campaign - generates a few cases, just to test authorizations.
     */
    @Test
    public void test001CreateForeignCampaign() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        roleInducementCertDefinition = repoAddObjectFromFile(ROLE_INDUCEMENT_CERT_DEF_FILE,
                AccessCertificationDefinitionType.class, result).asObjectable();

        when();
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(roleInducementCertDefinition.getOid(), task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        roleInducementCampaignOid = campaign.getOid();

        campaign = getCampaignWithCases(roleInducementCampaignOid);
        display("campaign", campaign);
        assertSanityAfterCampaignCreate(campaign, roleInducementCertDefinition);
        assertCasesCount(campaign.getOid(), 0);
    }

    @Test
    public void test002OpenFirstForeignStage() throws Exception {
        given();
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        when();
        certificationService.openNextStage(roleInducementCampaignOid, task, result);

        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getFirstStageTasks(roleInducementCampaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(roleInducementCampaignOid);
        display("campaign in stage 1", campaign);
        assertSanityAfterCampaignStart(campaign, roleInducementCertDefinition, 5);
    }

    @Test
    public void test005CreateCampaignDenied() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_ELAINE_OID)); // elaine is a reviewer, not authorized to create campaigns

        expect();
        try {
            certificationService.createCampaign(certificationDefinition.getOid(), task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Expected security violation exception: " + e.getMessage());
        }
    }

    @Test
    public void test006CreateCampaignDeniedBobWrongDeputy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_DEPUTY_NO_ASSIGNMENTS_OID)); // this is a deputy with limitation blocking all assignments

        expect();
        try {
            certificationService.createCampaign(certificationDefinition.getOid(), task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Expected security violation exception: " + e.getMessage());
        }
    }

    @Test
    public void test010CreateCampaignAllowedForDeputy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_DEPUTY_FULL_OID));

        when();
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certificationDefinition.getOid(), task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign", campaign);
        assertSanityAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentCompleteAll(campaign, 100, 100, 100); // no cases
        assertPercentCompleteCurrent(campaign, 100, 100, 100); // no cases

        // delete the campaign to keep other tests working
        login(userAdministrator.asPrismObject());
        deleteObject(AccessCertificationCampaignType.class, campaignOid);
    }

    @Test
    public void test011CreateCampaignAllowed() throws Exception {
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_OID));

        when();
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certificationDefinition.getOid(), task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign", campaign);
        assertSanityAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentCompleteAll(campaign, 100, 100, 100); // no cases
        assertPercentCompleteCurrent(campaign, 100, 100, 100); // no cases
    }

    @Test
    public void test012SearchAllCasesDenied() throws Exception {
        login(getUserFromRepo(USER_ELAINE_OID));

        searchWithNoCasesExpected();
    }

    @Test
    public void test013SearchAllCasesAllowed() throws Exception {
        login(getUserFromRepo(USER_BOB_OID));

        searchWithNoCasesExpected();
    }

    private void searchWithNoCasesExpected() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, CertCampaignTypeUtil.createCasesForCampaignQuery(campaignOid),
                null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }

    @Test
    public void test020OpenFirstStageDenied() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_ELAINE_OID));

        expect();
        try {
            certificationService.openNextStage(campaignOid, task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Got expected denial exception: " + e.getMessage());
        }
    }

    // TODO test with bob's full deputy

    @Test
    public void test021OpenFirstStageAllowed() throws Exception {
        given();
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_OID));
        task.setOwner(getUserFromRepo(USER_BOB_OID));

        when();
        certificationService.openNextStage(campaignOid, task, result);

        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getFirstStageTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);

        assertSanityAfterCampaignStart(campaign, certificationDefinition, 7);
        checkAllCasesSanity(campaign.getCase());
        List<AccessCertificationCaseType> caseList = campaign.getCase();
        // no responses -> NO_RESPONSE in all cases
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);

        assertPercentCompleteAll(campaign, 0, 0, 0);
        assertPercentCompleteCurrent(campaign, 0, 0, 0);
    }

    @Test
    public void test030SearchAllCasesDenied() throws Exception {
        login(getUserFromRepo(USER_ELAINE_OID));

        searchWithNoCasesExpected();
    }

    @Test
    public void test031SearchAllCasesDeniedLimitedDeputy() throws Exception {
        login(getUserFromRepo(USER_BOB_DEPUTY_NO_ASSIGNMENTS_OID));

        searchWithNoCasesExpected();
    }

    @Test
    public void test032SearchAllCasesAllowed() throws Exception {
        login(getUserFromRepo(USER_BOB_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, null, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCasesSanity(caseList);
    }

    @Test
    public void test034SearchAllCasesAllowedDeputy() throws Exception {
        login(getUserFromRepo(USER_BOB_DEPUTY_FULL_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, null, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCasesSanity(caseList);
    }

    @Test
    public void test040SearchCasesFilteredSortedPaged() throws Exception {
        login(getUserFromRepo(USER_BOB_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        Collection<SelectorOptions<GetOperationOptions>> resolveNames =
                SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                .item(AccessCertificationCaseType.F_OBJECT_REF).ref(userAdministrator.getOid())
                .desc(AccessCertificationCaseType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME)
                .offset(2).maxSize(2)
                .build();
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, query, resolveNames, task, result);

        then();
        // Cases for administrator are (ordered by name, descending):
        //  - Superuser
        //  - ERoot
        //  - COO
        //  - CEO
        // so paging (2, 2) should return the last two
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Wrong number of certification cases", 2, caseList.size());
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        assertEquals("Wrong target OID in case #0", ROLE_COO_OID, caseList.get(0).getTargetRef().getOid());
        assertEquals("Wrong target OID in case #1", ROLE_CEO_OID, caseList.get(1).getTargetRef().getOid());
    }

    @Test
    public void test050SearchWorkItemsAdministrator() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(
                        CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid),
                        false, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkAllWorkItemsSanity(workItems);
    }

    @Test
    public void test052SearchWorkItemsByTenantRef() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .exists(T_PARENT)
                .block()
                .item(AccessCertificationCaseType.F_TENANT_REF).ref(ORG_GOVERNOR_OFFICE_OID)
                .and().ownerId(campaignOid)
                .endBlock()
                .build();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(
                        query, false, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification cases", 1, workItems.size());
        checkWorkItemSanity(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
    }

    @Test
    public void test054SearchDecisionsByOrgRef() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .exists(T_PARENT)
                .block()
                .item(AccessCertificationCaseType.F_ORG_REF).ref(ORG_SCUMM_BAR_OID)
                .and().ownerId(campaignOid)
                .endBlock()
                .build();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(query, false, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 1, workItems.size());
        checkWorkItemSanity(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
    }

    @Test
    public void test056SearchDecisionsByAdminStatus() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .exists(T_PARENT)
                .block()
                .item(F_ACTIVATION, F_ADMINISTRATIVE_STATUS).eq(ENABLED)
                .and().ownerId(campaignOid)
                .endBlock()
                .build();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(query, false, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification cases", 1, workItems.size());
        checkWorkItemSanity(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
    }

    @Test
    public void test060SearchOpenWorkItemsDeputyDenied() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_DEPUTY_NONE_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(
                        CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid),
                        false, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification cases", 0, workItems.size());
    }

    @Test
    public void test062SearchOpenWorkItemsDeputyAllowed() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(
                        CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid),
                        false, null, task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkAllWorkItemsSanity(workItems);
    }

    @Test
    public void test100RecordDecision() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        AccessCertificationCaseType superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);

        when();
        AccessCertificationWorkItemType workItem = findWorkItem(superuserCase, 1, 1, USER_ADMINISTRATOR_OID);
        long id = superuserCase.asPrismContainerValue().getId();
        certificationService.recordDecision(campaignOid, id, workItem.getId(), ACCEPT, "no comment", task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);
        assertEquals("changed case ID", Long.valueOf(id), superuserCase.asPrismContainerValue().getId());
        assertSingleDecision(
                superuserCase, ACCEPT, "no comment", 1, 1,
                USER_ADMINISTRATOR_OID, ACCEPT, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        // 1 reviewer per case (always administrator)
        assertPercentCompleteAll(campaign, Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f));
        assertPercentCompleteCurrent(campaign, Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f));
    }

    @Test
    public void test105RecordAcceptJackCeo() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);

        when();
        AccessCertificationWorkItemType workItem = findWorkItem(ceoCase, 1, 1, USER_ADMINISTRATOR_OID);
        // reviewerRef will be taken from the current user
        long id = ceoCase.asPrismContainerValue().getId();
        certificationService.recordDecision(campaignOid, id, workItem.getId(), ACCEPT, "ok", task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
        assertSingleDecision(
                ceoCase, ACCEPT, "ok", 1, 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        // 1 reviewer per case (always administrator)
        assertPercentCompleteAll(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));
        assertPercentCompleteCurrent(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));
    }

    @Test
    public void test110RecordRevokeJackCeo() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);

        when();
        // reviewerRef will be taken from the current user
        long id = ceoCase.asPrismContainerValue().getId();
        AccessCertificationWorkItemType workItem = findWorkItem(ceoCase, 1, 1, USER_ADMINISTRATOR_OID);
        certificationService.recordDecision(campaignOid, id, workItem.getId(), REVOKE, "no way", task, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        displayDumpable("CEO case", ceoCase.asPrismContainerValue());
        assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
        assertSingleDecision(
                ceoCase, REVOKE, "no way", 1, 1, USER_ADMINISTRATOR_OID, REVOKE, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        // 1 reviewer per case (always administrator)
        assertPercentCompleteAll(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));
        assertPercentCompleteCurrent(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));
    }

    @SuppressWarnings("Duplicates")
    private void checkAllCasesSanity(Collection<AccessCertificationCaseType> caseList) {
        assertEquals("Wrong number of certification cases", 7, caseList.size());
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator);
        checkCaseSanity(caseList, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkCaseSanity(caseList, USER_JACK_OID, ORG_EROOT_OID, userJack);
    }

    @SuppressWarnings("Duplicates")
    private void checkAllWorkItemsSanity(Collection<AccessCertificationWorkItemType> workItems) {
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkWorkItemSanity(workItems, USER_JACK_OID, ORG_EROOT_OID, userJack);
    }

    @Test
    public void test150CloseFirstStageDeny() throws Exception {
        login(getUserFromRepo(USER_ELAINE_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        expect();
        try {
            certificationService.closeCurrentStage(campaignOid, task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Got expected deny exception: " + e.getMessage());
        }
    }

    @Test
    public void test151CloseCampaignDeny() throws Exception {
        login(getUserFromRepo(USER_ELAINE_OID));

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        expect();
        try {
            certificationService.closeCampaign(campaignOid, task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Got expected deny exception: " + e.getMessage());
        }
    }

    @Test
    public void test152CloseFirstStageAllow() throws Exception {
        login(getUserFromRepo(USER_BOB_OID));

        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();

        given();
        Task task = getTestTask();
        task.setOwner(getUserFromRepo(USER_BOB_OID));
        OperationResult result = task.getResult();

        when();
        certificationService.closeCurrentStage(campaignOid, task, result);

        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        List<PrismObject<TaskType>> tasks = getCloseStageTask(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);
        assertSanityAfterStageClose(campaign, certificationDefinition, 1);
        List<AccessCertificationCaseType> caseList = campaign.getCase();
        checkAllCasesSanity(caseList);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, 1);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, 1);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, REVOKE, REVOKE, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, 1);

        // 1 reviewer per case (always administrator)
        assertPercentCompleteAll(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));
        assertPercentCompleteCurrent(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));
    }

    @Test
    public void test200StartRemediationDeny() throws Exception {
        login(getUserFromRepo(USER_ELAINE_OID));

        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        expect();
        try {
            certificationService.startRemediation(campaignOid, task, result);
        } catch (SecurityViolationException e) {
            System.out.println("Got expected deny exception: " + e.getMessage());
        }
    }

    @Test
    public void test205StartRemediationAllow() throws Exception {
        clock.resetOverride();
        XMLGregorianCalendar startTime = clock.currentTimeXMLGregorianCalendar();
        login(getUserFromRepo(USER_BOB_OID));

        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        when();
        certificationService.startRemediation(campaignOid, task, result);

        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation start", campaign);
        assertTrue("wrong campaign state: " + campaign.getState(), campaign.getState() == CLOSED || campaign.getState() == IN_REMEDIATION);

        List<PrismObject<TaskType>> tasks = getRemediationTasks(campaignOid, startTime, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid());

        campaign = getCampaignWithCases(campaignOid);
        assertEquals("wrong campaign state", CLOSED, campaign.getState());
        assertEquals("wrong campaign stage", 2, or0(campaign.getStageNumber()));
        assertDefinitionAndOwner(campaign, certificationDefinition, USER_BOB_OID);
        assertApproximateTime("end time", new Date(), campaign.getEndTimestamp());
        assertEquals("wrong # of stages", 1, campaign.getStage().size());
        assertApproximateTime("stage 1 end", new Date(), campaign.getStage().get(0).getEndTimestamp());

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        AccessCertificationCaseType jackCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        assertApproximateTime("ceoDummyCase.remediedTimestamp", new Date(), jackCase.getRemediedTimestamp());

        userJack = getUser(USER_JACK_OID).asObjectable();
        display("jack", userJack);
        assertEquals("wrong # of jack's assignments", 4, userJack.getAssignment().size());
        assertEquals("wrong target OID", ORG_EROOT_OID, userJack.getAssignment().get(0).getTargetRef().getOid());

        // 1 reviewer per case (always administrator)
        assertPercentCompleteAll(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));
        assertPercentCompleteCurrent(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));
    }

    @Test
    public void test210CheckAfterClose() throws Exception {
        login(userAdministrator.asPrismObject());

        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        when();
        waitForCampaignTasks(campaignOid, 20000, result);

        then();
        userAdministrator = getUser(USER_ADMINISTRATOR_OID).asObjectable();
        display("administrator", userAdministrator);
        assertCertificationMetadata(
                findAssignmentByTargetRequired(userAdministrator.asPrismObject(), ROLE_SUPERUSER_OID),
                SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT,
                singleton(USER_ADMINISTRATOR_OID),
                singleton("administrator: no comment"));
    }

    /**
     * Checks whether certification reports basically work. MID-8665.
     *
     * Note that we check the reports here and not in `report-impl` because of the current dependencies between these
     * two modules (`report-impl` and `certification-impl`). Maybe we'll move this test in the future into `report-impl`.
     */
    @Test
    public void test220CreateReports() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String year = currentYearFragment();

        var definitions = REPORT_CERTIFICATION_DEFINITIONS.export()
                .execute(result);

        assertCsv(definitions, "definitions")
                .forRecord(C_DEF_NAME, "Basic User Assignment Certification (ERoot only)", a ->
                        a.assertValue(C_DEF_OWNER, "administrator")
                                .assertValue(C_DEF_CAMPAIGNS, "1")
                                .assertValue(C_DEF_OPEN_CAMPAIGNS, "0")
                                .assertValue(C_DEF_LAST_STARTED, v -> v.contains(year))
                                .assertValue(C_DEF_LAST_CLOSED, v -> v.contains(year)))
                .forRecord(C_DEF_NAME, "Role Inducements", a ->
                        a.assertValue(C_DEF_OWNER, "")
                                .assertValue(C_DEF_CAMPAIGNS, "1")
                                .assertValue(C_DEF_OPEN_CAMPAIGNS, "1")
                                .assertValue(C_DEF_LAST_STARTED, v -> v.contains(year))
                                .assertValue(C_DEF_LAST_CLOSED, ""));

        var campaigns = REPORT_CERTIFICATION_CAMPAIGNS.export()
                .execute(result);

        assertCsv(campaigns, "campaigns")
                .assertRecords(1) // only Role Inducements is open
                .record(0)
                .assertValue(C_CMP_NAME, "Role Inducements 1")
                .assertValue(C_CMP_OWNER, "administrator")
                .assertValue(C_CMP_START, v -> v.contains(year))
                .assertValue(C_CMP_FINISH, "")
                .assertValue(C_CMP_CASES, "5")
                .assertValue(C_CMP_STATE, "In review stage")
                .assertValue(C_CMP_ACTUAL_STAGE, "1")
                .assertValue(C_CMP_STAGE_CASES, "5")
                .assertValue(C_CMP_PERCENT_COMPLETE, "100.0 %");

        var campaignsAll = REPORT_CERTIFICATION_CAMPAIGNS.export()
                .withParameter("alsoClosedCampaigns", "true")
                .execute(result);

        assertCsv(campaignsAll, "campaigns")
                .assertRecords(2);

        var cases = REPORT_CERTIFICATION_CASES.export()
                .withParameter(
                        "campaignRef",
                        ObjectTypeUtil.createObjectRef(campaignOid, ObjectTypes.ACCESS_CERTIFICATION_CAMPAIGN))
                .execute(result);

        assertCsv(cases, "cases")
                .assertRecords(7)
                .forRecords(1,
                        r -> "User: administrator".equals(r.get(C_CASES_OBJECT))
                                && "Role: Superuser".equals(r.get(C_CASES_TARGET)),
                        a -> a.assertValue(C_CASES_CAMPAIGN, "Basic User Assignment Certification (ERoot only) 2")
                                .assertValue(C_CASES_REVIEWERS, "")
                                .assertValue(C_CASES_LAST_REVIEWED_ON, s -> s.contains(year))
                                .assertValue(C_CASES_REVIEWED_BY, "administrator")
                                .assertValue(C_CASES_ITERATION, "1")
                                .assertValue(C_CASES_IN_STAGE, "1")
                                .assertValue(C_CASES_OUTCOME, "Accept")
                                .assertValue(C_CASES_COMMENTS, "no comment")
                                .assertValue(C_CASES_REMEDIED_ON, ""));

        var casesAll = REPORT_CERTIFICATION_CASES.export()
                .execute(result);

        assertCsv(casesAll, "cases")
                .assertRecords(12);

        var workItemsAll = REPORT_CERTIFICATION_WORK_ITEMS.export()
                .execute(result);

        assertCsv(workItemsAll, "work items")
                .assertRecords(11)
                .forRecords(1,
                        r -> "Role: CEO".equals(r.get(C_WI_OBJECT))
                                && "Resource: Dummy Resource".equals(r.get(C_WI_TARGET)),
                        a -> a.assertValue(C_WI_CAMPAIGN, "Role Inducements 1")
                                .assertValue(C_WI_ITERATION, "1")
                                .assertValue(C_WI_STAGE_NUMBER, "1")
                                .assertValue(C_WI_ORIGINAL_ASSIGNEE, "elaine")
                                .assertValue(C_WI_DEADLINE, "")
                                .assertValue(C_WI_CURRENT_ASSIGNEES, "elaine")
                                .assertValue(C_WI_ESCALATION, "")
                                .assertValue(C_WI_PERFORMER, "")
                                .assertValue(C_WI_OUTCOME, "")
                                .assertValue(C_WI_COMMENT, "")
                                .assertValue(C_WI_LAST_CHANGED, "")
                                .assertValue(C_WI_CLOSED, ""))
                .forRecords(1,
                        r -> "User: jack".equals(r.get(C_WI_OBJECT))
                                && "Role: CEO".equals(r.get(C_WI_TARGET)),
                        a -> a.assertValue(C_WI_CAMPAIGN, "Basic User Assignment Certification (ERoot only) 2")
                                .assertValue(C_WI_ITERATION, "1")
                                .assertValue(C_WI_STAGE_NUMBER, "1")
                                .assertValue(C_WI_ORIGINAL_ASSIGNEE, "administrator")
                                .assertValue(C_WI_DEADLINE, "")
                                .assertValue(C_WI_CURRENT_ASSIGNEES, "administrator")
                                .assertValue(C_WI_ESCALATION, "")
                                .assertValue(C_WI_PERFORMER, "administrator")
                                .assertValue(C_WI_OUTCOME, "Revoke")
                                .assertValue(C_WI_COMMENT, "no way")
                                .assertValue(C_WI_LAST_CHANGED, s -> s.contains(year))
                                .assertValue(C_WI_CLOSED, s -> s.contains(year)));
    }

    @Test
    public void test900CleanupCampaignsDeny() throws Exception {
        login(getUserFromRepo(USER_ELAINE_OID));

        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        expect();
        CleanupPolicyType policy = new CleanupPolicyType().maxRecords(0);
        certificationService.cleanupCampaigns(policy, task, result);
        display("result", result);

        SearchResultList<PrismObject<AccessCertificationCampaignType>> campaigns = getAllCampaigns(result);
        display("campaigns", campaigns);
    }

    @Test
    public void test910CleanupCampaignsAllow() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        when();
        CleanupPolicyType policy = new CleanupPolicyType().maxRecords(0);
        certificationService.cleanupCampaigns(policy, task, result);
        result.computeStatus();
        display("result", result);
        assertSuccess(result);

        SearchResultList<PrismObject<AccessCertificationCampaignType>> campaigns = getAllCampaigns(result);
        display("campaigns", campaigns);
        assertEquals("Wrong # of remaining campaigns", 1, campaigns.size());
        PrismObject<AccessCertificationCampaignType> remainingCampaign = campaigns.get(0);
        assertEquals("Wrong OID of the remaining campaign", roleInducementCampaignOid, remainingCampaign.getOid());

        certificationManager.closeCampaign(roleInducementCampaignOid, true, task, result);

        certificationService.cleanupCampaigns(policy, task, result);
        result.computeStatus();
        assertSuccess(result);

        SearchResultList<PrismObject<AccessCertificationCampaignType>> campaigns2 = getAllCampaigns(result);
        display("campaigns after 2nd cleanup", campaigns2);
        assertEquals("Wrong # of remaining campaigns", 0, campaigns2.size());
    }

    @Test
    public void test920CleanupCampaignsByAge() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        AccessCertificationCampaignType c1 = new AccessCertificationCampaignType()
                .name("c1")
                .state(CLOSED)
                .stageNumber(0)
                .iteration(1)
                .endTimestamp(XmlTypeConverter.fromNow(XmlTypeConverter.createDuration("-PT10M")));
        AccessCertificationCampaignType c2 = new AccessCertificationCampaignType()
                .name("c2")
                .state(CLOSED)
                .stageNumber(0)
                .iteration(1)
                .endTimestamp(XmlTypeConverter.fromNow(XmlTypeConverter.createDuration("-P2D")));
        AccessCertificationCampaignType c3 = new AccessCertificationCampaignType()
                .name("c3")
                .state(CLOSED)
                .stageNumber(0)
                .iteration(1)
                .endTimestamp(XmlTypeConverter.fromNow(XmlTypeConverter.createDuration("-P2M")));
        repositoryService.addObject(c1.asPrismObject(), null, result);
        repositoryService.addObject(c2.asPrismObject(), null, result);
        repositoryService.addObject(c3.asPrismObject(), null, result);
        display("campaigns", getAllCampaigns(result));

        expect();
        CleanupPolicyType policy = new CleanupPolicyType().maxAge(XmlTypeConverter.createDuration("P1D"));
        certificationService.cleanupCampaigns(policy, task, result);
        result.computeStatus();
        assertSuccess(result);
        display("cleanup result", result);

        SearchResultList<PrismObject<AccessCertificationCampaignType>> campaignsAfter = getAllCampaigns(result);
        display("campaigns after cleanup", campaignsAfter);

        assertEquals("Wrong # of remaining campaigns", 1, campaignsAfter.size());
        PrismObject<AccessCertificationCampaignType> remainingCampaign = campaignsAfter.get(0);
        assertEquals("Wrong name of the remaining campaign", "c1", remainingCampaign.getName().getOrig());
    }
}
