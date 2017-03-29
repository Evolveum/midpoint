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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.prism.delta.PropertyDelta.createModificationReplaceProperty;
import static com.evolveum.midpoint.schema.RetrieveOption.INCLUDE;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CAMPAIGN_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_STAGE_OUTCOME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_STAGE_NUMBER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_DECISION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_COMMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType.F_STAGE_NUMBER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CertificationTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(CertificationTest.class);
    private static final File TEST_DIR = new File("src/test/resources/cert");
    public static final File CAMPAIGN_1_FILE = new File(TEST_DIR, "cert-campaign-1.xml");
    public static final File CAMPAIGN_2_FILE = new File(TEST_DIR, "cert-campaign-2.xml");

    private String campaign1Oid;
    private String campaign2Oid;
    private PrismObjectDefinition<AccessCertificationCampaignType> campaignDef;

	protected RepoModifyOptions getModifyOptions() {
		return null;
	}

	@Test
    public void test100AddCampaignNonOverwrite() throws Exception {
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(CAMPAIGN_1_FILE);
        campaignDef = campaign.getDefinition();

        OperationResult result = new OperationResult("test100AddCampaignNonOverwrite");

        campaign1Oid = repositoryService.addObject(campaign, null, result);

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        checkCampaign(campaign1Oid, result, (PrismObject) prismContext.parseObject(CAMPAIGN_1_FILE), null, null);
    }

    @Test(expectedExceptions = ObjectAlreadyExistsException.class)
    public void test105AddCampaignNonOverwriteExisting() throws Exception {
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(CAMPAIGN_1_FILE);
        OperationResult result = new OperationResult("test105AddCampaignNonOverwriteExisting");
        repositoryService.addObject(campaign, null, result);
    }

    @Test
    public void test108AddCampaignOverwriteExisting() throws Exception {
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(CAMPAIGN_1_FILE);
        OperationResult result = new OperationResult("test108AddCampaignOverwriteExisting");
        campaign.setOid(campaign1Oid);       // doesn't work without specifying OID
        campaign1Oid = repositoryService.addObject(campaign, RepoAddOptions.createOverwrite(), result);

        checkCampaign(campaign1Oid, result, (PrismObject) prismContext.parseObject(CAMPAIGN_1_FILE), null, null);
    }

    @Test
    public void test200ModifyCampaignProperties() throws Exception {
        OperationResult result = new OperationResult("test200ModifyCampaignProperties");

        List<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add(createModificationReplaceProperty(F_NAME, campaignDef, new PolyString("Campaign 1+", "campaign 1")));
        modifications.add(createModificationReplaceProperty(F_STATE, campaignDef, IN_REVIEW_STAGE));

        executeAndCheckModification(modifications, result, 1);
    }

    @Test
    public void test210ModifyCaseProperties() throws Exception {
        OperationResult result = new OperationResult("test210ModifyCaseProperties");

        List<ItemDelta<?,?>> modifications = new ArrayList<>();
        ItemPath case1 = new ItemPath(F_CASE).subPath(new IdItemPathSegment(1L));
        modifications.add(createModificationReplaceProperty(case1.subPath(F_CURRENT_STAGE_OUTCOME), campaignDef, DELEGATE));
        modifications.add(createModificationReplaceProperty(case1.subPath(F_CURRENT_STAGE_NUMBER), campaignDef, 300));

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test220ModifyDecisionProperties() throws Exception {
        OperationResult result = new OperationResult("test220ModifyDecisionProperties");

        List<ItemDelta<?,?>> modifications = new ArrayList<>();
        ItemPath d1 = new ItemPath(F_CASE).subPath(1L).subPath(F_DECISION).subPath(1L);
        modifications.add(createModificationReplaceProperty(d1.subPath(F_RESPONSE), campaignDef, DELEGATE));
        modifications.add(createModificationReplaceProperty(d1.subPath(F_COMMENT), campaignDef, "hi"));

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test230ModifyAllLevels() throws Exception {
        OperationResult result = new OperationResult("test230ModifyAllLevels");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_NAME).replace(new PolyString("Campaign 2", "campaign 2"))
                .item(F_STATE).replace(IN_REMEDIATION)
                .item(F_CASE, 2, F_CURRENT_STAGE_OUTCOME).replace(NO_RESPONSE)
                .item(F_CASE, 2, F_CURRENT_STAGE_NUMBER).replace(400)
                .item(F_CASE, 1, F_DECISION, 1, F_RESPONSE).replace(NOT_DECIDED)
                .item(F_CASE, 1, F_DECISION, 1, F_COMMENT).replace("low")
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 1);
    }

    @Test
    public void test240AddCases() throws Exception {
        OperationResult result = new OperationResult("test240AddDeleteCases");

        AccessCertificationCaseType caseNoId = new AccessCertificationCaseType(prismContext);
        caseNoId.setObjectRef(createObjectRef("123", ObjectTypes.USER));
        caseNoId.setTargetRef(createObjectRef("456", ObjectTypes.ROLE));
        caseNoId.setCurrentStageNumber(1);

        // explicit ID is dangerous (possibility of conflict!)
        AccessCertificationCaseType case100 = new AccessCertificationCaseType(prismContext);
        case100.setId(100L);
        case100.setObjectRef(createObjectRef("100123", ObjectTypes.USER));
        case100.setTargetRef(createObjectRef("100456", ObjectTypes.ROLE));
        //case100.getCurrentReviewerRef().add(createObjectRef("100789", ObjectTypes.USER));
        case100.setCurrentStageNumber(1);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).add(caseNoId, case100)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test242DeleteCase() throws Exception {
        OperationResult result = new OperationResult("test242DeleteCase");

        AccessCertificationCaseType case7 = new AccessCertificationCaseType();
        case7.setId(7L);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).delete(case7)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    // FIXME!!!
    @Test
    public void test244ModifyCase() throws Exception {
        OperationResult result = new OperationResult("test244ModifyCase");

        AccessCertificationCaseType case7 = new AccessCertificationCaseType();
        case7.setId(7L);

        PrismReferenceValue reviewerToDelete = createObjectRef("100789", ObjectTypes.USER).asReferenceValue();

//        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
//                .item(F_CASE, 100, F_CURRENT_REVIEWER_REF).delete(reviewerToDelete)
//                .asItemDeltas();
//
//        executeAndCheckModification(modifications, result, 0);
    }


    @Test
    public void test248AddDeleteModifyCase() throws Exception {
        OperationResult result = new OperationResult("test248AddDeleteModifyCase");

        AccessCertificationCaseType caseNoId = new AccessCertificationCaseType(prismContext);
        caseNoId.setObjectRef(createObjectRef("x123", ObjectTypes.USER));
        caseNoId.setTargetRef(createObjectRef("x456", ObjectTypes.ROLE));
        caseNoId.setCurrentStageNumber(1);

        // explicit ID is dangerous
        AccessCertificationCaseType case110 = new AccessCertificationCaseType(prismContext);
        case110.setId(110L);
        case110.setObjectRef(createObjectRef("x100123", ObjectTypes.USER));
        case110.setTargetRef(createObjectRef("x100456", ObjectTypes.ROLE));
        // FIXME!!!!
//        case110.getCurrentReviewerRef().add(createObjectRef("x100789", ObjectTypes.USER));
        case110.setCurrentStageNumber(1);

        AccessCertificationCaseType case100 = new AccessCertificationCaseType();
        case100.setId(100L);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).add(caseNoId, case110).delete(case100)
                .item(F_CASE, 3, F_CURRENT_STAGE_NUMBER).replace(400)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test250AddDeleteModifyResponse() throws Exception {
        OperationResult result = new OperationResult("test250AddDeleteModifyResponse");

        AccessCertificationDecisionType decNoId = new AccessCertificationDecisionType(prismContext);
        decNoId.setReviewerRef(createObjectRef("888", ObjectTypes.USER));
        decNoId.setStageNumber(1);

        AccessCertificationDecisionType dec200 = new AccessCertificationDecisionType(prismContext);
        dec200.setId(200L);         // this is dangerous
        dec200.setStageNumber(1);
        dec200.setReviewerRef(createObjectRef("200888", ObjectTypes.USER));

        AccessCertificationDecisionType dec1 = new AccessCertificationDecisionType();
        dec1.setId(1L);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, 6, F_DECISION).add(decNoId, dec200)
                .item(F_CASE, 6, F_DECISION).delete(dec1)
                .item(F_CASE, 6, F_DECISION, 2, F_RESPONSE).replace(ACCEPT)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test260ReplaceDecisionsExistingId() throws Exception {
        OperationResult result = new OperationResult("test260ReplaceDecisions");

        AccessCertificationDecisionType dec200 = new AccessCertificationDecisionType(prismContext);
        dec200.setId(200L);             //dangerous
        dec200.setStageNumber(44);
        dec200.setReviewerRef(createObjectRef("999999", ObjectTypes.USER));

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, 6, F_DECISION).replace(dec200)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test265ReplaceDecisionsNewId() throws Exception {
        OperationResult result = new OperationResult("test265ReplaceDecisions");

        AccessCertificationDecisionType dec250 = new AccessCertificationDecisionType(prismContext);
        dec250.setId(250L);         //dangerous
        dec250.setStageNumber(440);
        dec250.setReviewerRef(createObjectRef("250-999999", ObjectTypes.USER));

        AccessCertificationDecisionType dec251 = new AccessCertificationDecisionType(prismContext);
        dec251.setId(251L);
        dec251.setStageNumber(1);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, 6, F_DECISION).replace(dec250, dec251)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test270ReplaceCase() throws Exception {
        OperationResult result = new OperationResult("test270ReplaceCase");

        // explicit ID is dangerous
        AccessCertificationDecisionType dec777 = new AccessCertificationDecisionType(prismContext);
        dec777.setId(777L);
        dec777.setStageNumber(888);
        dec777.setReviewerRef(createObjectRef("999", ObjectTypes.USER));

        AccessCertificationDecisionType decNoId = new AccessCertificationDecisionType(prismContext);
        decNoId.setStageNumber(889);
        decNoId.setReviewerRef(createObjectRef("9999", ObjectTypes.USER));

        AccessCertificationCaseType caseNoId = new AccessCertificationCaseType(prismContext);
        caseNoId.setObjectRef(createObjectRef("aaa", ObjectTypes.USER));
        caseNoId.setTargetRef(createObjectRef("bbb", ObjectTypes.ROLE));
        // FIXME!!!!
//        caseNoId.getCurrentReviewerRef().add(createObjectRef("ccc", ObjectTypes.USER));
        caseNoId.setCurrentStageNumber(1);
        caseNoId.getDecision().add(dec777);
        caseNoId.getDecision().add(decNoId);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).replace(caseNoId)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test300PrepareForQueryCases() throws Exception {
        OperationResult result = new OperationResult("test300QueryCases");

        // overwrite the campaign
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(CAMPAIGN_1_FILE);
        campaign.setOid(campaign1Oid);       // doesn't work without specifying OID
        campaign1Oid = repositoryService.addObject(campaign, RepoAddOptions.createOverwrite(), result);

        checkCampaign(campaign1Oid, result, (PrismObject) prismContext.parseObject(CAMPAIGN_1_FILE), null, null);

        PrismObject<AccessCertificationCampaignType> campaign2 = prismContext.parseObject(CAMPAIGN_2_FILE);
        campaign2Oid = repositoryService.addObject(campaign2, null, result);

        checkCampaign(campaign2Oid, result, (PrismObject) prismContext.parseObject(CAMPAIGN_2_FILE), null, null);
    }

    @Test
    public void test310CasesForCampaign() throws Exception {
        OperationResult result = new OperationResult("test310CasesForCampaign");

        checkCasesForCampaign(campaign1Oid, result);
        checkCasesForCampaign(campaign2Oid, result);
    }

    @Test
    public void test320AllCases() throws Exception {
        OperationResult result = new OperationResult("test320AllCases");

        List<AccessCertificationCaseType> cases = repositoryService.searchContainers(AccessCertificationCaseType.class, null, null, result);

        AccessCertificationCampaignType campaign1 = getFullCampaign(campaign1Oid, result).asObjectable();
        AccessCertificationCampaignType campaign2 = getFullCampaign(campaign2Oid, result).asObjectable();
        List<AccessCertificationCaseType> expectedCases = new ArrayList<>();
        expectedCases.addAll(campaign1.getCase());
        expectedCases.addAll(campaign2.getCase());
        PrismAsserts.assertEqualsCollectionUnordered("list of cases is different", cases, expectedCases.toArray(new AccessCertificationCaseType[0]));

        for (AccessCertificationCaseType aCase : cases) {
            ObjectReferenceType campaignRef = aCase.getCampaignRef();
            String campaignOid = campaignRef.getOid();
            AccessCertificationCampaignType owner = null;
            if (campaignOid.equals(campaign1Oid)) {
                owner = campaign1;
            } else if (campaignOid.equals(campaign2Oid)) {
                owner = campaign2;
            } else {
                fail("Unknown campaign OID: " + campaignOid + " in case: " + aCase);
            }

            PrismObject<AccessCertificationCampaignType> campaign = getOwningCampaignChecked(aCase);
            assertEquals("Wrong owning campaign OID", owner.getOid(), campaign.getOid());
            assertEquals("Wrong owning campaign name", owner.getName(), campaign.asObjectable().getName());
        }
    }

    @Test
    public void test330CurrentUnansweredCases() throws Exception {
        OperationResult result = new OperationResult("test330CurrentUnansweredCases");

        // we have to find definition ourselves, as ../state cannot be currently resolved by query builder
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                .item(F_CURRENT_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                .and().item(T_PARENT, F_STATE).eq(IN_REVIEW_STAGE)
                .and().exists(F_DECISION).block()
                    .item(F_STAGE_NUMBER).eq().item(T_PARENT, F_CURRENT_STAGE_NUMBER)
                    .and().block()
                        .item(F_RESPONSE).eq(NO_RESPONSE)
                        .or().item(F_RESPONSE).isNull()
                    .endBlock()
                .endBlock()
                .build();

        List<AccessCertificationCaseType> cases = repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);

        AccessCertificationCampaignType campaign1 = getFullCampaign(campaign1Oid, result).asObjectable();
        AccessCertificationCampaignType campaign2 = getFullCampaign(campaign2Oid, result).asObjectable();
        List<AccessCertificationCaseType> expectedCases = new ArrayList<>();
        addUnansweredActiveCases(expectedCases, campaign1.getCase(), campaign1);
        addUnansweredActiveCases(expectedCases, campaign2.getCase(), campaign2);
        PrismAsserts.assertEqualsCollectionUnordered("list of cases is different", cases, expectedCases.toArray(new AccessCertificationCaseType[0]));
    }

    private void addUnansweredActiveCases(List<AccessCertificationCaseType> expectedCases, List<AccessCertificationCaseType> caseList, AccessCertificationCampaignType campaign) {
        for (AccessCertificationCaseType aCase : caseList) {
            if (aCase.getCurrentStageNumber() != campaign.getStageNumber()) {
                continue;
            }
            if (campaign.getState() != IN_REVIEW_STAGE) {
                continue;
            }
            boolean emptyDecisionFound = false;
            for (AccessCertificationDecisionType decision : aCase.getDecision()) {
                if (decision.getStageNumber() != aCase.getCurrentStageNumber()) {
                    continue;
                }
                if (decision.getResponse() == null || decision.getResponse() == NO_RESPONSE) {
                    emptyDecisionFound = true;
                    break;
                }
            }
            if (emptyDecisionFound) {
                LOGGER.info("Expecting case of {}:{}", campaign.getOid(), aCase.getId());
                expectedCases.add(aCase);
            }
        }
    }

    private void checkCasesForCampaign(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                .ownerId(oid)
                .build();
        List<AccessCertificationCaseType> cases = repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);
        for (AccessCertificationCaseType aCase : cases) {
            AssertJUnit.assertEquals("wrong campaign ref", oid, aCase.getCampaignRef().getOid());
            PrismObject<AccessCertificationCampaignType> campaign = getOwningCampaignChecked(aCase);
            AssertJUnit.assertEquals("wrong parent OID", oid, campaign.getOid());
        }
        AccessCertificationCampaignType campaign = getFullCampaign(oid, result).asObjectable();
        PrismAsserts.assertEqualsCollectionUnordered("list of cases is different", cases, campaign.getCase().toArray(new AccessCertificationCaseType[0]));
    }

    private PrismObject<AccessCertificationCampaignType> getOwningCampaignChecked(AccessCertificationCaseType aCase) {
        PrismContainer caseContainer = (PrismContainer) aCase.asPrismContainerValue().getParent();
        AssertJUnit.assertNotNull("campaign is not fetched (case parent is null)", caseContainer);
        PrismContainerValue campaignValue = (PrismContainerValue) caseContainer.getParent();
        AssertJUnit.assertNotNull("campaign is not fetched (case container parent is null)", caseContainer);
        PrismObject<AccessCertificationCampaignType> campaign = (PrismObject) campaignValue.getParent();
        AssertJUnit.assertNotNull("campaign is not fetched (campaign PCV parent is null)", campaign);
        return campaign;
    }

    @Test
    public void test900DeleteCampaign() throws Exception {
        OperationResult result = new OperationResult("test900DeleteCampaign");
        repositoryService.deleteObject(AccessCertificationCampaignType.class, campaign1Oid, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    protected void executeAndCheckModification(List<ItemDelta<?,?>> modifications, OperationResult result, int versionDelta) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, IOException {
		RepoModifyOptions modifyOptions = getModifyOptions();
		if (RepoModifyOptions.isExecuteIfNoChanges(modifyOptions) && versionDelta == 0) {
			versionDelta = 1;
		}

		PrismObject<AccessCertificationCampaignType> before = getFullCampaign(campaign1Oid, result);
        int expectedVersion = Integer.parseInt(before.getVersion()) + versionDelta;
        List<ItemDelta> savedModifications = (List) CloneUtil.cloneCollectionMembers(modifications);

		repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign1Oid, modifications, modifyOptions, result);

        checkCampaign(campaign1Oid, result, before, savedModifications, expectedVersion);
    }

    private void checkCampaign(String campaignOid, OperationResult result, PrismObject<AccessCertificationCampaignType> expectedObject, List<ItemDelta> modifications, Integer expectedVersion) throws SchemaException, ObjectNotFoundException, IOException {
        expectedObject.setOid(campaignOid);
        if (modifications != null) {
            ItemDelta.applyTo(modifications, expectedObject);
        }

        LOGGER.trace("Expected object = \n{}", expectedObject.debugDump());

        PrismObject<AccessCertificationCampaignType> campaign = getFullCampaign(campaignOid, result);

        LOGGER.trace("Actual object from repo = \n{}", campaign.debugDump());

        removeCampaignRef(expectedObject.asObjectable());
        removeCampaignRef(campaign.asObjectable());
        PrismAsserts.assertEquivalent("Campaign is not as expected", expectedObject, campaign);
        if (expectedVersion != null) {
            AssertJUnit.assertEquals("Incorrect version", (int) expectedVersion, Integer.parseInt(campaign.getVersion()));
        }
    }

    private PrismObject<AccessCertificationCampaignType> getFullCampaign(String campaignOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        SelectorOptions<GetOperationOptions> retrieve = SelectorOptions.create(F_CASE, GetOperationOptions.createRetrieve(INCLUDE));
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, Arrays.asList(retrieve), result);
    }

    private void removeCampaignRef(AccessCertificationCampaignType campaign) {
        for (AccessCertificationCaseType aCase : campaign.getCase()) {
            aCase.asPrismContainerValue().removeReference(F_CAMPAIGN_REF);
        }
    }

}
