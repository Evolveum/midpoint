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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

import static com.evolveum.midpoint.schema.RetrieveOption.INCLUDE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CAMPAIGN_REF;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CertificationTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(CertificationTest.class);
    private static final File TEST_DIR = new File("src/test/resources/cert");

    private String campaignOid;

    @Test
    public void test100AddCampaignNonOverwrite() throws Exception {
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(new File(TEST_DIR, "cert-campaign-1.xml"));
        OperationResult result = new OperationResult("test100AddCampaignNonOverwrite");

        campaignOid = repositoryService.addObject(campaign, null, result);

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        // rereading, as repo strips cases from the campaign (!)
        PrismObject<AccessCertificationCampaignType> expected = prismContext.parseObject(new File(TEST_DIR, "cert-campaign-1.xml"));
        checkCampaign(campaignOid, expected, result);
    }

    private void checkCampaign(String campaignOid, PrismObject<AccessCertificationCampaignType> expectedObject, OperationResult result) throws SchemaException, ObjectNotFoundException {
        SelectorOptions<GetOperationOptions> retrieve = SelectorOptions.create(F_CASE, GetOperationOptions.createRetrieve(INCLUDE));
        PrismObject<AccessCertificationCampaignType> campaign = repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, Arrays.asList(retrieve), result);
        expectedObject.setOid(campaignOid);
        removeCampaignRef(campaign.asObjectable());
        PrismAsserts.assertEquivalent("Campaign is not as expected", expectedObject, campaign);
    }

    private void removeCampaignRef(AccessCertificationCampaignType campaign) {
        for (AccessCertificationCaseType aCase : campaign.getCase()) {
            aCase.asPrismContainerValue().removeReference(F_CAMPAIGN_REF);
        }
    }

    @Test(expectedExceptions = ObjectAlreadyExistsException.class)
    public void test105AddCampaignNonOverwriteExisting() throws Exception {
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(new File(TEST_DIR, "cert-campaign-1.xml"));
        OperationResult result = new OperationResult("test105AddCampaignNonOverwriteExisting");
        repositoryService.addObject(campaign, null, result);
    }

    @Test
    public void test108AddCampaignOverwriteExisting() throws Exception {
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(new File(TEST_DIR, "cert-campaign-1.xml"));
        OperationResult result = new OperationResult("test108AddCampaignOverwriteExisting");
        campaign.setOid(campaignOid);       // doesn't work without specifying OID
        campaignOid = repositoryService.addObject(campaign, RepoAddOptions.createOverwrite(), result);

        // rereading, as repo strips cases from the campaign (!)
        PrismObject<AccessCertificationCampaignType> expected = prismContext.parseObject(new File(TEST_DIR, "cert-campaign-1.xml"));
        checkCampaign(campaignOid, expected, result);
    }



    @Test
    public void test900DeleteCampaign() throws Exception {
        OperationResult result = new OperationResult("test900DeleteCampaign");
        repositoryService.deleteObject(AccessCertificationCampaignType.class, campaignOid, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }
}
