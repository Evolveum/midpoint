/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Testing ad hoc certification (when changing parent orgs).
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-certification-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestAdHocCertification extends AbstractCertificationTest {

	protected static final File TEST_DIR = new File("src/test/resources/adhoc");

	protected static final File ASSIGNMENT_CERT_DEF_FILE = new File(TEST_DIR, "adhoc-certification-assignment.xml");
    protected static final String ASSIGNMENT_CERT_DEF_OID = "540940e9-4ac5-4340-ba85-fd7e8b5e6686";

	protected static final File MODIFICATION_CERT_DEF_FILE = new File(TEST_DIR, "adhoc-certification-modification.xml");
    protected static final String MODIFICATION_CERT_DEF_OID = "83a16584-bb2a-448c-aee1-82fc6d577bcb";

    protected static final File ORG_LABORATORY_FILE = new File(TEST_DIR, "org-laboratory.xml");
    protected static final String ORG_LABORATORY_OID = "027faec7-7763-4b26-ab92-c5c0acbb1173";

    protected static final File USER_INDIGO_FILE = new File(TEST_DIR, "user-indigo.xml");
    protected static final String USER_INDIGO_OID = "11b35bd2-9b2f-4a00-94fa-7ed0079a7500";

    protected AccessCertificationDefinitionType assignmentCertificationDefinition;
    protected AccessCertificationDefinitionType modificationCertificationDefinition;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        assignmentCertificationDefinition = repoAddObjectFromFile(ASSIGNMENT_CERT_DEF_FILE, AccessCertificationDefinitionType.class, initResult).asObjectable();
        modificationCertificationDefinition = repoAddObjectFromFile(MODIFICATION_CERT_DEF_FILE, AccessCertificationDefinitionType.class, initResult).asObjectable();
        repoAddObjectFromFile(ORG_LABORATORY_FILE, initResult);
        repoAddObjectFromFile(USER_INDIGO_FILE, initResult);
    }

    @Test
    public void test010HireIndigo() throws Exception {
        final String TEST_NAME = "test010HireIndigo";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAdHocCertification.class.getName() + "." + TEST_NAME);
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignOrg(USER_INDIGO_OID, ORG_LABORATORY_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		SearchResultList<PrismObject<AccessCertificationCampaignType>> campaigns = repositoryService
				.searchObjects(AccessCertificationCampaignType.class, null, null, result);
		assertEquals("Wrong # of campaigns", 1, campaigns.size());
		AccessCertificationCampaignType campaign = campaigns.get(0).asObjectable();

		campaign = getCampaignWithCases(campaign.getOid());
        display("campaign", campaign);
        assertAfterCampaignStart(campaign, assignmentCertificationDefinition, 1);		// beware, maybe not all details would match (in the future) - then adapt this test
        assertPercentComplete(campaign, 0, 0, 0);      // no cases, no problems
		assertCases(campaign.getOid(), 1);
	}

    @Test
    public void test020ModifyIndigo() throws Exception {
        final String TEST_NAME = "test020ModifyIndigo";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAdHocCertification.class.getName() + "." + TEST_NAME);
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        @SuppressWarnings({ "unchecked", "raw" })
		ObjectDelta<UserType> delta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_DESCRIPTION).replace("new description")
				.item(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
				.asObjectDelta(USER_INDIGO_OID);
        executeChanges(delta, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		SearchResultList<PrismObject<AccessCertificationCampaignType>> campaigns = repositoryService
				.searchObjects(AccessCertificationCampaignType.class, null, null, result);
		assertEquals("Wrong # of campaigns", 2, campaigns.size());
		AccessCertificationCampaignType campaign = campaigns.stream()
				.filter(c -> MODIFICATION_CERT_DEF_OID.equals(c.asObjectable().getDefinitionRef().getOid()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No modification-triggered campaign")).asObjectable();

		campaign = getCampaignWithCases(campaign.getOid());
        display("campaign", campaign);
        assertAfterCampaignStart(campaign, modificationCertificationDefinition, 1);		// beware, maybe not all details would match (in the future) - then adapt this test
        assertPercentComplete(campaign, 0, 0, 0);      // no cases, no problems
	}
}
