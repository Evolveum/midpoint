/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.test;

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
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Very simple certification test.
 * Tests just the basic functionality, along with security features.
 *
 * @author mederly
 */
@ContextConfiguration(locations = { "classpath:ctx-certification-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCertificationBasic extends AbstractCertificationTest {

    private static final File CERT_DEF_USER_ASSIGNMENT_BASIC_FILE = new File(COMMON_DIR, "certification-of-eroot-user-assignments.xml");
    //protected static final String CERT_DEF_USER_ASSIGNMENT_BASIC_OID = "33333333-0000-0000-0000-000000000001";

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
        assignFocus(UserType.class, USER_JACK_OID, UserType.COMPLEX_TYPE, USER_GUYBRUSH_OID, SchemaConstants.ORG_DEPUTY, null, initTask, initResult);
    }

    /*
     *  "Foreign" campaign - generates a few cases, just to test authorizations.
     */
    @Test
    public void test001CreateForeignCampaign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        roleInducementCertDefinition = repoAddObjectFromFile(ROLE_INDUCEMENT_CERT_DEF_FILE,
                AccessCertificationDefinitionType.class, result).asObjectable();

        // WHEN
        when();
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(roleInducementCertDefinition.getOid(), task, result);

        // THEN
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
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationService.openNextStage(roleInducementCampaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(roleInducementCampaignOid);
        display("campaign in stage 1", campaign);
        assertSanityAfterCampaignStart(campaign, roleInducementCertDefinition, 5);
    }

    @Test
    public void test005CreateCampaignDenied() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_ELAINE_OID)); // elaine is a reviewer, not authorized to create campaigns

        // WHEN/THEN
        when();
        try {
            certificationService.createCampaign(certificationDefinition.getOid(), task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Expected security violation exception: " + e.getMessage());
        }
    }

    @Test
    public void test006CreateCampaignDeniedBobWrongDeputy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_DEPUTY_NO_ASSIGNMENTS_OID));            // this is a deputy with limitation blocking all assignments

        // WHEN/THEN
        when();
        try {
            certificationService.createCampaign(certificationDefinition.getOid(), task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Expected security violation exception: " + e.getMessage());
        }
    }

    @Test
    public void test010CreateCampaignAllowedForDeputy() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_DEPUTY_FULL_OID));

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
        assertPercentCompleteCurrent(campaign, 100, 100, 100);      // no cases, no problems

        // delete the campaign to keep other tests working
        login(userAdministrator.asPrismObject());
        deleteObject(AccessCertificationCampaignType.class, campaignOid);
    }

    @Test
    public void test011CreateCampaignAllowed() throws Exception {
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
        assertPercentCompleteCurrent(campaign, 100, 100, 100);      // no cases, no problems
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, CertCampaignTypeUtil.createCasesForCampaignQuery(campaignOid, prismContext),
                null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }

    @Test
    public void test020OpenFirstStageDenied() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_ELAINE_OID));

        // WHEN+THEN
        when();
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_OID));
        task.setOwner(getUserFromRepo(USER_BOB_OID));

        // WHEN
        when();
        certificationService.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

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
        checkAllCasesSanity(caseList);
    }

    @Test(enabled = false)
    public void test034SearchAllCasesAllowedDeputy() throws Exception {
        login(getUserFromRepo(USER_BOB_DEPUTY_FULL_OID));

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
        checkAllCasesSanity(caseList);
    }

    @Test
    public void test040SearchCasesFilteredSortedPaged() throws Exception {
        login(getUserFromRepo(USER_BOB_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
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

        // THEN
        // Cases for administrator are (ordered by name, descending):
        //  - Superuser
        //  - ERoot
        //  - COO
        //  - CEO
        // so paging (2, 2) should return the last two
        then();
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

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(
                        CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
                        false, null, task, result);

        // THEN
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

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
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

        // THEN
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

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
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

        // THEN
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

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
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

        // THEN
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

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
                        false, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification cases", 0, workItems.size());
    }

    @Test
    public void test062SearchOpenWorkItemsDeputyAllowed() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
                        false, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification cases", 7, workItems.size());
        checkAllWorkItemsSanity(workItems);
    }

    @Test
    public void test100RecordDecision() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);

        // WHEN
        when();
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(superuserCase, 1, 1, USER_ADMINISTRATOR_OID);
        long id = superuserCase.asPrismContainerValue().getId();
        certificationService.recordDecision(campaignOid, id, workItem.getId(), ACCEPT, "no comment", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);
        assertEquals("changed case ID", Long.valueOf(id), superuserCase.asPrismContainerValue().getId());
        assertSingleDecision(superuserCase, ACCEPT, "no comment", 1, 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f));      // 1 reviewer per case (always administrator)
        assertPercentCompleteCurrent(campaign, Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f), Math.round(100.0f / 7.0f));      // 1 reviewer per case (always administrator)
    }

    @Test
    public void test105RecordAcceptJackCeo() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);

        // WHEN
        when();
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, 1, USER_ADMINISTRATOR_OID);
        // reviewerRef will be taken from the current user
        long id = ceoCase.asPrismContainerValue().getId();
        certificationService.recordDecision(campaignOid, id, workItem.getId(), ACCEPT, "ok", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
        assertSingleDecision(ceoCase, ACCEPT, "ok", 1, 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));      // 1 reviewer per case (always administrator)
        assertPercentCompleteCurrent(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));      // 1 reviewer per case (always administrator)
    }

    @Test
    public void test110RecordRevokeJackCeo() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);

        // WHEN
        when();
        // reviewerRef will be taken from the current user
        long id = ceoCase.asPrismContainerValue().getId();
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, 1, USER_ADMINISTRATOR_OID);
        certificationService.recordDecision(campaignOid, id, workItem.getId(), REVOKE, "no way", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        displayDumpable("CEO case", ceoCase.asPrismContainerValue());
        assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
        assertSingleDecision(ceoCase, REVOKE, "no way", 1, 1, USER_ADMINISTRATOR_OID, REVOKE, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));      // 1 reviewer per case (always administrator)
        assertPercentCompleteCurrent(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));      // 1 reviewer per case (always administrator)
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

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN+THEN
        when();
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

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN+THEN
        when();
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

        // GIVEN
        Task task = getTestTask();
        task.setOwner(getUserFromRepo(USER_BOB_OID));
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationService.closeCurrentStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

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

        assertPercentCompleteAll(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));      // 1 reviewer per case (always administrator)
        assertPercentCompleteCurrent(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));      // 1 reviewer per case (always administrator)
    }

    @Test
    public void test200StartRemediationDeny() throws Exception {
        login(getUserFromRepo(USER_ELAINE_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN+THEN
        when();
        try {
            certificationService.startRemediation(campaignOid, task, result);
        } catch (SecurityViolationException e) {
            System.out.println("Got expected deny exception: " + e.getMessage());
        }
    }

    @Test
    public void test205StartRemediationAllow() throws Exception {
        login(getUserFromRepo(USER_BOB_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationService.startRemediation(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation start", campaign);
        assertTrue("wrong campaign state: " + campaign.getState(), campaign.getState() == CLOSED || campaign.getState() == IN_REMEDIATION);

        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(campaign.getOid())
                .build();
        List<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, query, null, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid(), true);

        campaign = getCampaignWithCases(campaignOid);
        assertEquals("wrong campaign state", CLOSED, campaign.getState());
        assertEquals("wrong campaign stage", 2, campaign.getStageNumber());
        assertDefinitionAndOwner(campaign, certificationDefinition, USER_BOB_OID);
        assertApproximateTime("end time", new Date(), campaign.getEndTimestamp());
        assertEquals("wrong # of stages", 1, campaign.getStage().size());
        assertApproximateTime("stage 1 end", new Date(), campaign.getStage().get(0).getEndTimestamp());

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType jackCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        assertApproximateTime("ceoDummyCase.remediedTimestamp", new Date(), jackCase.getRemediedTimestamp());

        userJack = getUser(USER_JACK_OID).asObjectable();
        display("jack", userJack);
        assertEquals("wrong # of jack's assignments", 4, userJack.getAssignment().size());
        assertEquals("wrong target OID", ORG_EROOT_OID, userJack.getAssignment().get(0).getTargetRef().getOid());

        assertPercentCompleteAll(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));      // 1 reviewer per case (always administrator)
        assertPercentCompleteCurrent(campaign, Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f), Math.round(200.0f / 7.0f));      // 1 reviewer per case (always administrator)
    }

    @Test
    public void test210CheckAfterClose() throws Exception {
        login(userAdministrator.asPrismObject());

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        waitForCampaignTasks(campaignOid, 20000, result);

        // THEN
        userAdministrator = getUser(USER_ADMINISTRATOR_OID).asObjectable();
        display("administrator", userAdministrator);
        AssignmentType assignment = findAssignmentByTargetRequired(userAdministrator.asPrismObject(), ROLE_SUPERUSER_OID);
        assertCertificationMetadata(assignment.getMetadata(), SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT, singleton(USER_ADMINISTRATOR_OID), singleton("administrator: no comment"));
    }

    @Test
    public void test900CleanupCampaignsDeny() throws Exception {
        login(getUserFromRepo(USER_ELAINE_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN+THEN
        when();
        CleanupPolicyType policy = new CleanupPolicyType().maxRecords(0);
        certificationService.cleanupCampaigns(policy, task, result);
        display("result", result);

        SearchResultList<PrismObject<AccessCertificationCampaignType>> campaigns = getAllCampaigns(result);
        display("campaigns", campaigns);
    }

    @Test
    public void test910CleanupCampaignsAllow() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN+THEN
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

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        AccessCertificationCampaignType c1 = new AccessCertificationCampaignType(prismContext)
                .name("c1")
                .state(CLOSED)
                .stageNumber(0)
                .iteration(1)
                .endTimestamp(XmlTypeConverter.fromNow(XmlTypeConverter.createDuration("-PT10M")));
        AccessCertificationCampaignType c2 = new AccessCertificationCampaignType(prismContext)
                .name("c2")
                .state(CLOSED)
                .stageNumber(0)
                .iteration(1)
                .endTimestamp(XmlTypeConverter.fromNow(XmlTypeConverter.createDuration("-P2D")));
        AccessCertificationCampaignType c3 = new AccessCertificationCampaignType(prismContext)
                .name("c3")
                .state(CLOSED)
                .stageNumber(0)
                .iteration(1)
                .endTimestamp(XmlTypeConverter.fromNow(XmlTypeConverter.createDuration("-P2M")));
        repositoryService.addObject(c1.asPrismObject(), null, result);
        repositoryService.addObject(c2.asPrismObject(), null, result);
        repositoryService.addObject(c3.asPrismObject(), null, result);
        display("campaigns", getAllCampaigns(result));

        // WHEN+THEN
        when();
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
