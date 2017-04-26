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
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
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

	protected static final File CERT_DEF_FILE = new File(TEST_DIR, "adhoc-certification.xml");
    protected static final String CERT_DEF_OID = "540940e9-4ac5-4340-ba85-fd7e8b5e6686";

    protected static final File ORG_LABORATORY_FILE = new File(TEST_DIR, "org-laboratory.xml");
    protected static final String ORG_LABORATORY_OID = "027faec7-7763-4b26-ab92-c5c0acbb1173";

    protected static final File USER_INDIGO_FILE = new File(TEST_DIR, "user-indigo.xml");
    protected static final String USER_INDIGO_OID = "11b35bd2-9b2f-4a00-94fa-7ed0079a7500";

    protected AccessCertificationDefinitionType certificationDefinition;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        certificationDefinition = repoAddObjectFromFile(CERT_DEF_FILE, AccessCertificationDefinitionType.class, initResult).asObjectable();
        repoAddObjectFromFile(ORG_LABORATORY_FILE, initResult);
        repoAddObjectFromFile(USER_INDIGO_FILE, initResult);
    }

    @Test
    public void test010HireIndigo() throws Exception {
        final String TEST_NAME = "test010HireIndigo";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        assertAfterCampaignStart(campaign, certificationDefinition, 1);		// beware, maybe not all details would match (in the future) - then adapt this test
        assertPercentComplete(campaign, 0, 0, 0);      // no cases, no problems
	}
}
