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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BasicCertificationTest extends AbstractCertificationTest {

    private String campaignOid;

    @Test
    public void test010CreateCampaign() throws Exception {
        final String TEST_NAME = "test010CreateCampaign";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCampaignType campaign =
                certificationManager.createCampaign(userRoleBasicCertificationDefinition, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign", campaign);
        assertEquals("Unexpected certification cases", 0, campaign.getCase().size());
    }

    @Test
    public void test020StartFirstStage() throws Exception {
        final String TEST_NAME = "test020StartFirstStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(BasicCertificationTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCampaignType campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        certificationManager.startStage(campaign, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        campaign = getObject(AccessCertificationCampaignType.class, campaign.getOid()).asObjectable();
        display("campaign in stage 1", campaign);

        checkAllCases(campaign.getCase());
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
        ObjectPaging paging = ObjectPaging.createPaging(1, 2, AccessCertificationCaseType.F_TARGET_REF, OrderDirection.DESCENDING);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter, paging);
        List<AccessCertificationCaseType> caseList = certificationManager.searchCases(campaignOid, query, resolveNames, task, result);

        // THEN
        // Cases for administrator are (ordered by name, descending):
        //  - Superuser
        //  - COO
        //  - CEO
        // so paging (1, 2) should return the last two
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

    protected void checkAllCases(Collection<AccessCertificationCaseType> caseList) {
        assertEquals("Wrong number of certification cases", 4, caseList.size());
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        checkCase(caseList, USER_JACK_OID, ROLE_CEO_OID, userJack);
    }

    private AccessCertificationCaseType checkCase(Collection<AccessCertificationCaseType> caseList, String subjectOid, String targetOid, FocusType focus) {
        AccessCertificationCaseType ccase = findCase(caseList, subjectOid, targetOid);
        assertNotNull("Certification case for " + subjectOid + ":" + targetOid + " was not found", ccase);
        return checkSpecificCase(ccase, focus);
    }

    private AccessCertificationCaseType checkSpecificCase(AccessCertificationCaseType ccase, FocusType focus) {
        assertEquals("Wrong class for case", AccessCertificationAssignmentCaseType.class, ccase.getClass());
        AccessCertificationAssignmentCaseType acase = (AccessCertificationAssignmentCaseType) ccase;
        long id = acase.getAssignment().getId();
        for (AssignmentType assignment : focus.getAssignment()) {
            if (id == assignment.getId()) {
                assertEquals("Wrong assignment in certification case", assignment, acase.getAssignment());
                return ccase;
            }
        }
        fail("Assignment with ID " + id + " not found among assignments of " + focus);
        return null;        // won't come here
    }

    private AccessCertificationCaseType findCase(Collection<AccessCertificationCaseType> caseList, String subjectOid, String targetOid) {
        for (AccessCertificationCaseType acase : caseList) {
            if (acase.getTargetRef() != null && acase.getTargetRef().getOid().equals(targetOid) &&
                    acase.getSubjectRef() != null && acase.getSubjectRef().getOid().equals(subjectOid)) {
                return acase;
            }
        }
        return null;
    }
}
