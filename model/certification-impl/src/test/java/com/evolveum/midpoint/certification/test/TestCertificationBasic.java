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

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_ACTIVATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.ENABLED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType.F_ADMINISTRATIVE_STATUS;
import static org.testng.AssertJUnit.*;

/**
 * Very simple certification test.
 * Tests just the basic functionality, along with security features.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-certification-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCertificationBasic extends AbstractCertificationTest {

    protected static final File CERT_DEF_USER_ASSIGNMENT_BASIC_FILE = new File(COMMON_DIR, "certification-of-eroot-user-assignments.xml");
    protected static final String CERT_DEF_USER_ASSIGNMENT_BASIC_OID = "33333333-0000-0000-0000-000000000001";

    protected AccessCertificationDefinitionType certificationDefinition;
    protected AccessCertificationDefinitionType roleInducementCertDefinition;

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
        final String TEST_NAME = "test001CreateForeignCampaign";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestRoleInducementCertification.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        roleInducementCertDefinition = repoAddObjectFromFile(ROLE_INDUCEMENT_CERT_DEF_FILE,
                AccessCertificationDefinitionType.class, result).asObjectable();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(roleInducementCertDefinition.getOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        roleInducementCampaignOid = campaign.getOid();

        campaign = getCampaignWithCases(roleInducementCampaignOid);
        display("campaign", campaign);
        assertAfterCampaignCreate(campaign, roleInducementCertDefinition);
        assertCases(campaign.getOid(), 0);
    }

    @Test
    public void test002OpenFirstForeignStage() throws Exception {
        final String TEST_NAME = "test002OpenFirstForeignStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestRoleInducementCertification.class.getName() + "." + TEST_NAME);
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationService.openNextStage(roleInducementCampaignOid, 1, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(roleInducementCampaignOid);
        display("campaign in stage 1", campaign);
        assertAfterCampaignStart(campaign, roleInducementCertDefinition, 5);
    }

    @Test
    public void test005CreateCampaignDenied() throws Exception {
        final String TEST_NAME = "test005CreateCampaignDenied";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_ELAINE_OID));            // elaine is a reviewer, not authorized to create campaigns

        // WHEN/THEN
        TestUtil.displayWhen(TEST_NAME);
        try {
            certificationService.createCampaign(certificationDefinition.getOid(), task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Expected security violation exception: " + e.getMessage());
        }
    }

    @Test
    public void test006CreateCampaignDeniedBobWrongDeputy() throws Exception {
        final String TEST_NAME = "test006CreateCampaignDeniedBobWrongDeputy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_DEPUTY_NO_ASSIGNMENTS_OID));            // this is a deputy with limitation blocking all assignments

        // WHEN/THEN
        TestUtil.displayWhen(TEST_NAME);
        try {
            certificationService.createCampaign(certificationDefinition.getOid(), task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Expected security violation exception: " + e.getMessage());
        }
    }

    @Test
    public void test010CreateCampaignAllowedForDeputy() throws Exception {
        final String TEST_NAME = "test010CreateCampaignAllowedForDeputy";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_DEPUTY_FULL_OID));

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certificationDefinition.getOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign", campaign);
        assertAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentComplete(campaign, 100, 100, 100);      // no cases, no problems

        // delete the campaign to keep other tests working
        login(userAdministrator.asPrismObject());
        deleteObject(AccessCertificationCampaignType.class, campaignOid);
    }

    @Test
    public void test011CreateCampaignAllowed() throws Exception {
        final String TEST_NAME = "test010CreateCampaignAllowed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_OID));

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certificationDefinition.getOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign", campaign);
        assertAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentComplete(campaign, 100, 100, 100);      // no cases, no problems
    }

    @Test
    public void test012SearchAllCasesDenied() throws Exception {
        final String TEST_NAME = "test012SearchAllCasesDenied";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ELAINE_OID));

        searchWithNoCasesExpected(TEST_NAME);
    }

    @Test
    public void test013SearchAllCasesAllowed() throws Exception {
        final String TEST_NAME = "test013SearchAllCasesAllowed";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_BOB_OID));

        searchWithNoCasesExpected(TEST_NAME);
    }

    protected void searchWithNoCasesExpected(String TEST_NAME) throws Exception {
        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, CertCampaignTypeUtil.createCasesForCampaignQuery(campaignOid, prismContext),
                null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }


    @Test
    public void test020OpenFirstStageDenied() throws Exception {
        final String TEST_NAME = "test020OpenFirstStageDenied";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_ELAINE_OID));

        // WHEN+THEN
        TestUtil.displayWhen(TEST_NAME);
        try {
            certificationService.openNextStage(campaignOid, 1, task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Got expected denial exception: " + e.getMessage());
        }
    }

    // TODO test with bob's full deputy

    @Test
    public void test021OpenFirstStageAllowed() throws Exception {
        final String TEST_NAME = "test021OpenFirstStageAllowed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_OID));
        task.setOwner(getUserFromRepo(USER_BOB_OID));

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationService.openNextStage(campaignOid, 1, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);

        assertAfterCampaignStart(campaign, certificationDefinition, 7);
        checkAllCases(campaign.getCase(), campaignOid);
        List<AccessCertificationCaseType> caseList = campaign.getCase();
        // no responses -> NO_RESPONSE in all cases
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);

        assertPercentComplete(campaign, 0, 0, 0);
    }

    @Test
    public void test030SearchAllCasesDenied() throws Exception {
        final String TEST_NAME = "test030SearchCasesDenied";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ELAINE_OID));

        searchWithNoCasesExpected(TEST_NAME);
    }

    @Test
    public void test031SearchAllCasesDeniedLimitedDeputy() throws Exception {
        final String TEST_NAME = "test031SearchAllCasesDeniedLimitedDeputy";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_BOB_DEPUTY_NO_ASSIGNMENTS_OID));

        searchWithNoCasesExpected(TEST_NAME);
    }

    @Test
    public void test032SearchAllCasesAllowed() throws Exception {
        final String TEST_NAME = "test032SearchAllCasesAllowed";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_BOB_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, null, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCases(caseList, campaignOid);
    }

    @Test(enabled = false)
    public void test034SearchAllCasesAllowedDeputy() throws Exception {
        final String TEST_NAME = "test034SearchAllCasesAllowedDeputy";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_BOB_DEPUTY_FULL_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, null, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCases(caseList, campaignOid);
    }

    @Test
    public void test040SearchCasesFilteredSortedPaged() throws Exception {
        final String TEST_NAME = "test040SearchCasesFilteredSortedPaged";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_BOB_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        Collection<SelectorOptions<GetOperationOptions>> resolveNames =
                SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
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
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Wrong number of certification cases", 2, caseList.size());
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator, campaignOid);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator, campaignOid);
        assertEquals("Wrong target OID in case #0", ROLE_COO_OID, caseList.get(0).getTargetRef().getOid());
        assertEquals("Wrong target OID in case #1", ROLE_CEO_OID, caseList.get(1).getTargetRef().getOid());
    }

    @Test
    public void test050SearchWorkItemsAdministrator() throws Exception {
        final String TEST_NAME = "test050SearchWorkItemsAdministrator";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(
                        CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
                        false, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkAllWorkItems(workItems, campaignOid);
    }

    @Test
    public void test052SearchWorkItemsByTenantRef() throws Exception {
        final String TEST_NAME = "test052SearchWorkItemsByTenantRef";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
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
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification cases", 1, workItems.size());
        checkWorkItem(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, campaignOid, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
    }

    @Test
    public void test054SearchDecisionsByOrgRef() throws Exception {
        final String TEST_NAME = "test054SearchDecisionsByOrgRef";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
				.exists(T_PARENT)
				.block()
                	.item(AccessCertificationCaseType.F_ORG_REF).ref(ORG_SCUMM_BAR_OID)
                	.and().ownerId(campaignOid)
				.endBlock()
                .build();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(query, false, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 1, workItems.size());
        checkWorkItem(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, campaignOid, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
    }

    @Test
    public void test056SearchDecisionsByAdminStatus() throws Exception {
        final String TEST_NAME = "test056SearchDecisionsByAdminStatus";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
                .exists(T_PARENT)
                .block()
                    .item(F_ACTIVATION, F_ADMINISTRATIVE_STATUS).eq(ENABLED)
                    .and().ownerId(campaignOid)
                .endBlock()
                .build();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(query, false, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification cases", 1, workItems.size());
        checkWorkItem(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, campaignOid, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
    }

    @Test
    public void test060SearchOpenWorkItemsDeputyDenied() throws Exception {
        final String TEST_NAME = "test060SearchOpenWorkItemsDeputyDenied";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_DEPUTY_NONE_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
                        false, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification cases", 0, workItems.size());
    }

    @Test
    public void test062SearchOpenWorkItemsDeputyAllowed() throws Exception {
        final String TEST_NAME = "test062SearchOpenWorkItemsDeputyAllowed";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
                        false, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification cases", 7, workItems.size());
        checkAllWorkItems(workItems, campaignOid);
    }

    @Test
    public void test100RecordDecision() throws Exception {
        final String TEST_NAME = "test100RecordDecision";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(superuserCase, 1, USER_ADMINISTRATOR_OID);
        long id = superuserCase.asPrismContainerValue().getId();
        certificationService.recordDecision(campaignOid, id, workItem.getId(), ACCEPT, "no comment", task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCases(caseList, campaignOid);

        superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);
        assertEquals("changed case ID", Long.valueOf(id), superuserCase.asPrismContainerValue().getId());
        assertSingleDecision(superuserCase, ACCEPT, "no comment", 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentComplete(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));      // 1 reviewer per case (always administrator)
    }

    @Test
    public void test105RecordAcceptJackCeo() throws Exception {
        final String TEST_NAME = "test105RecordAcceptJackCeo";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, USER_ADMINISTRATOR_OID);
        // reviewerRef will be taken from the current user
        long id = ceoCase.asPrismContainerValue().getId();
        certificationService.recordDecision(campaignOid, id, workItem.getId(), ACCEPT, "ok", task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCases(caseList, campaignOid);

        ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
        assertSingleDecision(ceoCase, ACCEPT, "ok", 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentComplete(campaign, Math.round(200.0f/7.0f), Math.round(200.0f/7.0f), Math.round(200.0f/7.0f));      // 1 reviewer per case (always administrator)
    }

    @Test
    public void test110RecordRevokeJackCeo() throws Exception {
        final String TEST_NAME = "test110RecordRevokeJackCeo";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        // reviewerRef will be taken from the current user
        long id = ceoCase.asPrismContainerValue().getId();
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, USER_ADMINISTRATOR_OID);
        certificationService.recordDecision(campaignOid, id, workItem.getId(), REVOKE, "no way", task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCases(caseList, campaignOid);

        ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        display("CEO case", ceoCase.asPrismContainerValue());
        assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
        assertSingleDecision(ceoCase, REVOKE, "no way", 1, USER_ADMINISTRATOR_OID, REVOKE, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentComplete(campaign, Math.round(200.0f/7.0f), Math.round(200.0f/7.0f), Math.round(200.0f/7.0f));      // 1 reviewer per case (always administrator)
    }

    protected void checkAllCases(Collection<AccessCertificationCaseType> caseList, String campaignOid) {
        assertEquals("Wrong number of certification cases", 7, caseList.size());
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator, campaignOid);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator, campaignOid);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator, campaignOid);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator, campaignOid);
        checkCase(caseList, USER_JACK_OID, ROLE_CEO_OID, userJack, campaignOid, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkCase(caseList, USER_JACK_OID, ORG_EROOT_OID, userJack, campaignOid);
    }

    protected void checkAllWorkItems(Collection<AccessCertificationWorkItemType> workItems, String campaignOid) {
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkWorkItem(workItems, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator, campaignOid);
        checkWorkItem(workItems, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator, campaignOid);
        checkWorkItem(workItems, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator, campaignOid);
        checkWorkItem(workItems, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator, campaignOid);
        checkWorkItem(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, campaignOid, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkWorkItem(workItems, USER_JACK_OID, ORG_EROOT_OID, userJack, campaignOid);
    }

    @Test
    public void test150CloseFirstStageDeny() throws Exception {
        final String TEST_NAME = "test150CloseFirstStageDeny";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ELAINE_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN+THEN
        TestUtil.displayWhen(TEST_NAME);
        try {
            certificationService.closeCurrentStage(campaignOid, 1, task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Got expected deny exception: " + e.getMessage());
        }
    }

    @Test
    public void test151CloseCampaignDeny() throws Exception {
        final String TEST_NAME = "test151CloseCampaignDeny";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ELAINE_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN+THEN
        TestUtil.displayWhen(TEST_NAME);
        try {
            certificationService.closeCampaign(campaignOid, task, result);
            fail("Unexpected success");
        } catch (SecurityViolationException e) {
            System.out.println("Got expected deny exception: " + e.getMessage());
        }
    }

    @Test
    public void test152CloseFirstStageAllow() throws Exception {
        final String TEST_NAME = "test152CloseFirstStageAllow";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_BOB_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        task.setOwner(getUserFromRepo(USER_BOB_OID));
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationService.closeCurrentStage(campaignOid, 1, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);
        assertAfterStageClose(campaign, certificationDefinition, 1);
        List<AccessCertificationCaseType> caseList = campaign.getCase();
        checkAllCases(caseList, campaignOid);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, 1);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, 1);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, REVOKE, REVOKE, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, 1);

        assertPercentComplete(campaign, Math.round(200.0f/7.0f), Math.round(200.0f/7.0f), Math.round(200.0f/7.0f));      // 1 reviewer per case (always administrator)
    }

    @Test
    public void test200StartRemediationDeny() throws Exception {
        final String TEST_NAME = "test200StartRemediationDeny";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ELAINE_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN+THEN
        TestUtil.displayWhen(TEST_NAME);
        try {
            certificationService.startRemediation(campaignOid, task, result);
        } catch (SecurityViolationException e) {
            System.out.println("Got expected deny exception: " + e.getMessage());
        }
    }

    @Test
    public void test205StartRemediationAllow() throws Exception {
        final String TEST_NAME = "test205StartRemediationAllow";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_BOB_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationService.startRemediation(campaignOid, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation start", campaign);
        assertTrue("wrong campaign state: " + campaign.getState(), campaign.getState() == CLOSED || campaign.getState() == IN_REMEDIATION);

        ObjectQuery query = QueryBuilder.queryFor(TaskType.class, prismContext)
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
        assertEquals("wrong # of jack's assignments", 3, userJack.getAssignment().size());
        assertEquals("wrong target OID", ORG_EROOT_OID, userJack.getAssignment().get(0).getTargetRef().getOid());

        assertPercentComplete(campaign, Math.round(200.0f/7.0f), Math.round(200.0f/7.0f), Math.round(200.0f/7.0f));      // 1 reviewer per case (always administrator)
    }


}
