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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.prism.delta.PropertyDelta.createModificationReplaceProperty;
import static com.evolveum.midpoint.schema.GetOperationOptions.createDistinct;
import static com.evolveum.midpoint.schema.RetrieveOption.INCLUDE;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType.F_OUTCOME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

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
    public static final long NEW_CASE_ID = 100L;
	public static final long SECOND_NEW_CASE_ID = 110L;

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

        checkCampaign(campaign1Oid, result, prismContext.parseObject(CAMPAIGN_1_FILE), null, null);
		checksCountsStandard(result);
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

        checkCampaign(campaign1Oid, result, prismContext.parseObject(CAMPAIGN_1_FILE), null, null);
		checksCountsStandard(result);
    }

    @Test
    public void test200ModifyCampaignProperties() throws Exception {
        OperationResult result = new OperationResult("test200ModifyCampaignProperties");

        List<ItemDelta<?,?>> modifications = new ArrayList<>();
        modifications.add(createModificationReplaceProperty(F_NAME, campaignDef, new PolyString("Campaign 1+", "campaign 1")));
        modifications.add(createModificationReplaceProperty(F_STATE, campaignDef, IN_REVIEW_STAGE));

        executeAndCheckModification(modifications, result, 1);
		checksCountsStandard(result);
	}

    @Test
    public void test210ModifyCaseProperties() throws Exception {
        OperationResult result = new OperationResult("test210ModifyCaseProperties");

        List<ItemDelta<?,?>> modifications = new ArrayList<>();
        ItemPath case1 = new ItemPath(F_CASE).subPath(new IdItemPathSegment(1L));
        modifications.add(createModificationReplaceProperty(case1.subPath(F_CURRENT_STAGE_OUTCOME), campaignDef, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REDUCE));
        modifications.add(createModificationReplaceProperty(case1.subPath(AccessCertificationCaseType.F_STAGE_NUMBER), campaignDef, 300));

        executeAndCheckModification(modifications, result, 0);
		checksCountsStandard(result);
    }

    @Test
    public void test220ModifyWorkItemProperties() throws Exception {
        OperationResult result = new OperationResult("test220ModifyWorkItemProperties");

		List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(F_CASE, 1L, F_WORK_ITEM, 1L, F_OUTPUT).replace(
						new AbstractWorkItemOutputType()
								.outcome(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED)
								.comment("hi"))
				.asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
		checksCountsStandard(result);
    }

    @Test
    public void test230ModifyAllLevels() throws Exception {
        OperationResult result = new OperationResult("test230ModifyAllLevels");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_NAME).replace(new PolyString("Campaign 2", "campaign 2"))
                .item(F_STATE).replace(IN_REMEDIATION)
                .item(F_CASE, 2, F_CURRENT_STAGE_OUTCOME).replace(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE)
                .item(F_CASE, 2, AccessCertificationCaseType.F_STAGE_NUMBER).replace(400)
                .item(F_CASE, 1, F_WORK_ITEM, 1, F_OUTPUT).replace(
                		new AbstractWorkItemOutputType()
								.outcome(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NOT_DECIDED)
								.comment("low"))
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 1);
		checksCountsStandard(result);
    }

    @Test
    public void test240AddCases() throws Exception {
        OperationResult result = new OperationResult("test240AddDeleteCases");

        AccessCertificationCaseType caseNoId = new AccessCertificationCaseType(prismContext);
        caseNoId.setObjectRef(createObjectRef("123", ObjectTypes.USER));
        caseNoId.setTargetRef(createObjectRef("456", ObjectTypes.ROLE));
        caseNoId.setStageNumber(1);

        // explicit ID is dangerous (possibility of conflict!)
        AccessCertificationCaseType case100 = new AccessCertificationCaseType(prismContext);
        case100.setId(NEW_CASE_ID);
        case100.setObjectRef(createObjectRef("100123", ObjectTypes.USER));
        case100.setTargetRef(createObjectRef("100456", ObjectTypes.ROLE));
        case100.beginWorkItem()
				.assigneeRef(createObjectRef("ref1", ObjectTypes.USER))
				.end();
        case100.setStageNumber(1);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).add(caseNoId, case100)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
		checkCasesForCampaign(campaign1Oid, 9, result);
		checkCasesTotal(9, result);
		checkWorkItemsForCampaign(campaign1Oid, 11, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID,1, result);
		checkWorkItemsTotal(11, result);
    }

    @Test
    public void test250DeleteCase() throws Exception {
        OperationResult result = new OperationResult("test250DeleteCase");

        AccessCertificationCaseType case7 = new AccessCertificationCaseType();
        case7.setId(7L);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).delete(case7)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
		checkCasesForCampaign(campaign1Oid, 8, result);
		checkCasesTotal(8, result);
		checkWorkItemsForCampaign(campaign1Oid, 9, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID,1, result);
		checkWorkItemsTotal(9, result);
    }

    @Test
    public void test260AddWorkItem() throws Exception {
        OperationResult result = new OperationResult("test260AddWorkItem");

        AccessCertificationWorkItemType workItem = new AccessCertificationWorkItemType(prismContext)
                .beginOriginalAssigneeRef().oid("orig1").type(UserType.COMPLEX_TYPE).<AccessCertificationWorkItemType>end()
                .beginAssigneeRef().oid("rev1").type(UserType.COMPLEX_TYPE).<AccessCertificationWorkItemType>end()
                .beginAssigneeRef().oid("rev2").type(UserType.COMPLEX_TYPE).end();

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, NEW_CASE_ID, F_WORK_ITEM).add(workItem)
                .asItemDeltas();

        // WHEN
        executeAndCheckModification(modifications, result, 0);

        // THEN
		checkCasesForCampaign(campaign1Oid, 8, result);
		checkCasesTotal(8, result);
		checkWorkItemsForCampaign(campaign1Oid, 10, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID,2, result);
		checkWorkItemsTotal(10, result);
    }

    @Test
    public void test270ModifyWorkItem() throws Exception {
        OperationResult result = new OperationResult("test270ModifyWorkItem");

		PrismObject<AccessCertificationCampaignType> campaign = getFullCampaign(campaign1Oid, result);
		AccessCertificationCaseType case100 = campaign.asObjectable().getCase().stream()
				.filter(c -> c.getId() == NEW_CASE_ID).findFirst().orElseThrow(() -> new AssertionError("No case 100"));
		assertEquals("Wrong # of work items in case 100", 2, case100.getWorkItem().size());
		AccessCertificationWorkItemType workItem = case100.getWorkItem().stream().filter(wi -> wi.getOriginalAssigneeRef() != null).findFirst().orElse(null);
		assertNotNull("No new work item", workItem);

		XMLGregorianCalendar closedTimestamp = XmlTypeConverter.createXMLGregorianCalendar(new Date());
		List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, NEW_CASE_ID, F_WORK_ITEM, workItem.getId(), AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
						.replace(closedTimestamp)
                .asItemDeltas();

        // WHEN
        executeAndCheckModification(modifications, result, 0);

        // THEN
		checkCasesForCampaign(campaign1Oid, 8, result);
		checkCasesTotal(8, result);
		checkWorkItemsForCampaign(campaign1Oid, 10, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID,2, result);
		checkWorkItemsTotal(10, result);
    }

    @Test
    public void test280DeleteWorkItem() throws Exception {
        OperationResult result = new OperationResult("test280DeleteWorkItem");

		PrismObject<AccessCertificationCampaignType> campaign = getFullCampaign(campaign1Oid, result);
		AccessCertificationCaseType case100 = campaign.asObjectable().getCase().stream()
				.filter(c -> c.getId() == NEW_CASE_ID).findFirst().orElseThrow(() -> new AssertionError("No case 100"));
		assertEquals("Wrong # of work items in case 100", 2, case100.getWorkItem().size());
		AccessCertificationWorkItemType workItem = case100.getWorkItem().stream().filter(wi -> wi.getOriginalAssigneeRef() != null).findFirst().orElse(null);
		assertNotNull("No new work item", workItem);

		List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, NEW_CASE_ID, F_WORK_ITEM).delete(workItem.clone())
                .asItemDeltas();

        // WHEN
        executeAndCheckModification(modifications, result, 0);

        // THEN
		checkCasesForCampaign(campaign1Oid, 8, result);
		checkCasesTotal(8, result);
		checkWorkItemsForCampaign(campaign1Oid, 9, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID,1, result);
		checkWorkItemsTotal(9, result);
    }

    @Test
    public void test300AddDeleteModifyCase() throws Exception {
        OperationResult result = new OperationResult("test300AddDeleteModifyCase");

        AccessCertificationCaseType caseNoId = new AccessCertificationCaseType(prismContext);
        caseNoId.setObjectRef(createObjectRef("x123", ObjectTypes.USER));
        caseNoId.setTargetRef(createObjectRef("x456", ObjectTypes.ROLE));
        caseNoId.setStageNumber(1);

        // explicit ID is dangerous
        AccessCertificationCaseType case110 = new AccessCertificationCaseType(prismContext)
				.id(SECOND_NEW_CASE_ID)
				.objectRef(createObjectRef("x100123", ObjectTypes.USER))
				.targetRef(createObjectRef("x100456", ObjectTypes.ROLE))
				.stageNumber(1)
				.beginWorkItem()
					.assigneeRef(createObjectRef("x100789", ObjectTypes.USER))
				.end();

        AccessCertificationCaseType case100 = new AccessCertificationCaseType();
        case100.setId(NEW_CASE_ID);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).add(caseNoId, case110).delete(case100)
                .item(F_CASE, 3, AccessCertificationCaseType.F_STAGE_NUMBER).replace(400)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
		checkCasesForCampaign(campaign1Oid, 9, result);
		checkCasesTotal(9, result);
		checkWorkItemsForCampaign(campaign1Oid, 9, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, SECOND_NEW_CASE_ID,1, result);
		checkWorkItemsTotal(9, result);

	}

    @Test
    public void test320AddDeleteModifyResponse() throws Exception {
        OperationResult result = new OperationResult("test320AddDeleteModifyResponse");

        AccessCertificationWorkItemType wiNoId = new AccessCertificationWorkItemType(prismContext);
        wiNoId.assigneeRef(createObjectRef("888", ObjectTypes.USER));
        wiNoId.setStageNumber(1);

		AccessCertificationWorkItemType wi200 = new AccessCertificationWorkItemType(prismContext);
        wi200.setId(200L);         // this is dangerous
        wi200.setStageNumber(1);
        wi200.assigneeRef(createObjectRef("200888", ObjectTypes.USER));

		AccessCertificationWorkItemType wi1 = new AccessCertificationWorkItemType();
        wi1.setId(1L);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, 6, F_WORK_ITEM).add(wiNoId, wi200)
                .item(F_CASE, 6, F_WORK_ITEM).delete(wi1)
                .item(F_CASE, 6, F_WORK_ITEM, 2, F_OUTPUT, F_OUTCOME).replace(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
		checkCasesForCampaign(campaign1Oid, 9, result);
		checkCasesTotal(9, result);
		checkWorkItemsForCampaign(campaign1Oid, 10, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, SECOND_NEW_CASE_ID,1, result);
		checkWorkItemsTotal(10, result);
    }

    @Test
    public void test330ReplaceWorkItemsExistingId() throws Exception {
        OperationResult result = new OperationResult("test330ReplaceWorkItemsExistingId");

        AccessCertificationWorkItemType wi200 = new AccessCertificationWorkItemType(prismContext);
        wi200.setId(200L);             //dangerous
        wi200.setStageNumber(44);
        wi200.assigneeRef(createObjectRef("999999", ObjectTypes.USER));

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, 6, F_WORK_ITEM).replace(wi200)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
		checkCasesForCampaign(campaign1Oid, 9, result);
		checkCasesTotal(9, result);
		checkWorkItemsForCampaign(campaign1Oid, 8, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, SECOND_NEW_CASE_ID,1, result);
		checkWorkItemsTotal(8, result);
    }

    @Test
    public void test340ReplaceWorkItemsNewId() throws Exception {
        OperationResult result = new OperationResult("test340ReplaceWorkItemsNewId");

        AccessCertificationWorkItemType wi250 = new AccessCertificationWorkItemType(prismContext);
        wi250.setId(250L);         //dangerous
        wi250.setStageNumber(440);
        wi250.assigneeRef(createObjectRef("250-999999", ObjectTypes.USER));

		AccessCertificationWorkItemType wi251 = new AccessCertificationWorkItemType(prismContext);
        wi251.setId(251L);
        wi251.setStageNumber(1);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE, 6, F_WORK_ITEM).replace(wi250, wi251)
                .asItemDeltas();

        // TODO counts
        executeAndCheckModification(modifications, result, 0);
		checkCasesForCampaign(campaign1Oid, 9, result);
		checkCasesTotal(9, result);
		checkWorkItemsForCampaign(campaign1Oid, 9, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, SECOND_NEW_CASE_ID,1, result);
		checkWorkItemsTotal(9, result);
    }

    @Test
    public void test350ReplaceCase() throws Exception {
        OperationResult result = new OperationResult("test350ReplaceCase");

        // explicit ID is dangerous
		AccessCertificationWorkItemType wi777 = new AccessCertificationWorkItemType(prismContext);
        wi777.setId(777L);
        wi777.setStageNumber(888);
        wi777.assigneeRef(createObjectRef("999", ObjectTypes.USER));

		AccessCertificationWorkItemType wiNoId = new AccessCertificationWorkItemType(prismContext);
        wiNoId.setStageNumber(889);
        wiNoId.assigneeRef(createObjectRef("9999", ObjectTypes.USER));

        AccessCertificationCaseType caseNoId = new AccessCertificationCaseType(prismContext)
        		.objectRef(createObjectRef("aaa", ObjectTypes.USER))
        		.targetRef(createObjectRef("bbb", ObjectTypes.ROLE))
				.beginWorkItem()
						.assigneeRef(createObjectRef("ccc", ObjectTypes.USER))
				.<AccessCertificationCaseType>end()
				.workItem(wi777)
				.workItem(wiNoId)
				.stageNumber(1);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(F_CASE).replace(caseNoId)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
		checkCasesForCampaign(campaign1Oid, 1, result);
		checkCasesTotal(1, result);
		checkWorkItemsForCampaign(campaign1Oid, 3, result);
		checkWorkItemsTotal(3, result);
    }

    @Test
    public void test700PrepareForQueryCases() throws Exception {
        OperationResult result = new OperationResult("test700PrepareForQueryCases");

        // overwrite the campaign
        PrismObject<AccessCertificationCampaignType> campaign = prismContext.parseObject(CAMPAIGN_1_FILE);
        campaign.setOid(campaign1Oid);       // doesn't work without specifying OID
        campaign1Oid = repositoryService.addObject(campaign, RepoAddOptions.createOverwrite(), result);

        checkCampaign(campaign1Oid, result, prismContext.parseObject(CAMPAIGN_1_FILE), null, null);

        PrismObject<AccessCertificationCampaignType> campaign2 = prismContext.parseObject(CAMPAIGN_2_FILE);
        campaign2Oid = repositoryService.addObject(campaign2, null, result);

        checkCampaign(campaign2Oid, result, prismContext.parseObject(CAMPAIGN_2_FILE), null, null);
    }

    @Test
    public void test710CasesForCampaign() throws Exception {
        OperationResult result = new OperationResult("test710CasesForCampaign");

        checkCasesForCampaign(campaign1Oid, null, result);
        checkCasesForCampaign(campaign2Oid, null, result);
    }

    @Test
    public void test720AllCases() throws Exception {
        OperationResult result = new OperationResult("test720AllCases");

        List<AccessCertificationCaseType> cases = repositoryService.searchContainers(AccessCertificationCaseType.class, null, null, result);

        AccessCertificationCampaignType campaign1 = getFullCampaign(campaign1Oid, result).asObjectable();
        AccessCertificationCampaignType campaign2 = getFullCampaign(campaign2Oid, result).asObjectable();
        List<AccessCertificationCaseType> expectedCases = new ArrayList<>();
        expectedCases.addAll(campaign1.getCase());
        expectedCases.addAll(campaign2.getCase());
        PrismAsserts.assertEqualsCollectionUnordered("list of cases is different", cases, expectedCases.toArray(new AccessCertificationCaseType[0]));
    }

    @Test
    public void test730CurrentUnansweredCases() throws Exception {
        OperationResult result = new OperationResult("test730CurrentUnansweredCases");

        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                .item(AccessCertificationCaseType.F_STAGE_NUMBER).eq().item(T_PARENT, AccessCertificationCampaignType.F_STAGE_NUMBER)
                .and().item(T_PARENT, F_STATE).eq(IN_REVIEW_STAGE)
                .and().exists(F_WORK_ITEM).block()
					.item(F_CLOSE_TIMESTAMP).isNull()
                    .and().block()
                        .item(F_OUTPUT, F_OUTCOME).isNull()
                    .endBlock()
                .endBlock()
                .build();

        List<AccessCertificationCaseType> cases = repositoryService.searchContainers(AccessCertificationCaseType.class, query, createCollection(createDistinct()), result);

        AccessCertificationCampaignType campaign1 = getFullCampaign(campaign1Oid, result).asObjectable();
        AccessCertificationCampaignType campaign2 = getFullCampaign(campaign2Oid, result).asObjectable();
        List<AccessCertificationCaseType> expectedCases = new ArrayList<>();
        addUnansweredActiveCases(expectedCases, campaign1.getCase(), campaign1);
        addUnansweredActiveCases(expectedCases, campaign2.getCase(), campaign2);
        PrismAsserts.assertEqualsCollectionUnordered("list of cases is different", cases, expectedCases.toArray(new AccessCertificationCaseType[0]));
    }

    private void addUnansweredActiveCases(List<AccessCertificationCaseType> expectedCases, List<AccessCertificationCaseType> caseList, AccessCertificationCampaignType campaign) {
        for (AccessCertificationCaseType aCase : caseList) {
            if (aCase.getStageNumber() != campaign.getStageNumber()) {
                continue;
            }
            if (campaign.getState() != IN_REVIEW_STAGE) {
                continue;
            }
            boolean emptyDecisionFound = false;
            for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
                if (WorkItemTypeUtil.getOutcome(workItem) == null) {
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

    private void checkCasesForCampaign(String oid, Integer expected, OperationResult result) throws SchemaException, ObjectNotFoundException {
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                .ownerId(oid)
                .build();
        List<AccessCertificationCaseType> cases = repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);
		assertCasesFound(expected, cases, " for " + oid);
		for (AccessCertificationCaseType aCase : cases) {
            PrismObject<AccessCertificationCampaignType> campaign = getOwningCampaignChecked(aCase);
            AssertJUnit.assertEquals("wrong parent OID", oid, campaign.getOid());
        }
        AccessCertificationCampaignType campaign = getFullCampaign(oid, result).asObjectable();
        PrismAsserts.assertEqualsCollectionUnordered("list of cases is different", cases, campaign.getCase().toArray(new AccessCertificationCaseType[0]));
    }

    private void checkWorkItemsForCampaign(String oid, Integer expected, OperationResult result) throws SchemaException, ObjectNotFoundException {
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
				.exists(T_PARENT)
				.block()
					.ownerId(oid)
				.endBlock()
                .build();
        List<AccessCertificationWorkItemType> workItems = repositoryService.searchContainers(AccessCertificationWorkItemType.class, query, null, result);
		assertWorkItemsCount(expected, workItems, " for " + oid);
    }

    private void checkWorkItemsForCampaignAndCase(String oid, long caseId, Integer expected, OperationResult result) throws SchemaException, ObjectNotFoundException {
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationWorkItemType.class, prismContext)
				.exists(T_PARENT)
				.block()
					.ownerId(oid)
					.and().id(caseId)
				.endBlock()
                .build();
        List<AccessCertificationWorkItemType> workItems = repositoryService.searchContainers(AccessCertificationWorkItemType.class, query, null, result);
		assertWorkItemsCount(expected, workItems, " for " + oid + ":" + caseId);
    }

    private void checkCasesTotal(Integer expected, OperationResult result) throws SchemaException, ObjectNotFoundException {
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                .build();
        List<AccessCertificationCaseType> cases = repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);
		assertCasesFound(expected, cases, "");
    }

	private void assertCasesFound(Integer expected, List<AccessCertificationCaseType> cases, String desc) {
		System.out.println("Cases found" + desc + ": " + cases.size());
		if (expected != null) {
			assertEquals("Wrong # of cases" + desc, expected.intValue(), cases.size());
		}
	}

	private void checkWorkItemsTotal(Integer expected, OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<AccessCertificationWorkItemType> workItems = repositoryService.searchContainers(AccessCertificationWorkItemType.class, null, null, result);
		assertWorkItemsCount(expected, workItems, "");
    }

	private void assertWorkItemsCount(Integer expected, List<AccessCertificationWorkItemType> workItems, String desc) {
		System.out.println("Work items found" + desc + ": " + workItems.size());
		if (expected != null) {
			assertEquals("Wrong # of work items" + desc, expected.intValue(), workItems.size());
		}
	}

	private PrismObject<AccessCertificationCampaignType> getOwningCampaignChecked(AccessCertificationCaseType aCase) {
        PrismContainer caseContainer = (PrismContainer) aCase.asPrismContainerValue().getParent();
        assertNotNull("campaign is not fetched (case parent is null)", caseContainer);
        PrismContainerValue campaignValue = (PrismContainerValue) caseContainer.getParent();
        assertNotNull("campaign is not fetched (case container parent is null)", caseContainer);
        PrismObject<AccessCertificationCampaignType> campaign = (PrismObject) campaignValue.getParent();
        assertNotNull("campaign is not fetched (campaign PCV parent is null)", campaign);
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

        PrismAsserts.assertEquivalent("Campaign is not as expected", expectedObject, campaign);
        if (expectedVersion != null) {
            AssertJUnit.assertEquals("Incorrect version", (int) expectedVersion, Integer.parseInt(campaign.getVersion()));
        }
    }

    private PrismObject<AccessCertificationCampaignType> getFullCampaign(String campaignOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        SelectorOptions<GetOperationOptions> retrieve = SelectorOptions.create(F_CASE, GetOperationOptions.createRetrieve(INCLUDE));
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, Collections.singletonList(retrieve), result);
    }

	private void checksCountsStandard(OperationResult result) throws SchemaException, ObjectNotFoundException {
		checkCasesForCampaign(campaign1Oid, 7, result);
		checkCasesTotal(7, result);
		checkWorkItemsForCampaign(campaign1Oid, 10, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 1,2, result);
		checkWorkItemsForCampaignAndCase(campaign1Oid, 2,1, result);
		checkWorkItemsTotal(10, result);
	}

}
