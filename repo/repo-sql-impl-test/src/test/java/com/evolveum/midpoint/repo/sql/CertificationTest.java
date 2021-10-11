/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.evolveum.midpoint.prism.PrismConstants.T_PARENT;
import static com.evolveum.midpoint.schema.GetOperationOptions.createDistinct;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType.F_OUTCOME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_STAGE_OUTCOME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_WORK_ITEM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType.F_OUTPUT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import static org.testng.AssertJUnit.*;

@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CertificationTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/cert");
    public static final File CAMPAIGN_1_FILE = new File(TEST_DIR, "cert-campaign-1.xml");
    public static final File CAMPAIGN_2_FILE = new File(TEST_DIR, "cert-campaign-2.xml");
    public static final long CASE_9_ID = 105L;
    public static final long NEW_CASE_ID = 200L;
    public static final long SECOND_NEW_CASE_ID = 210L;

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

        List<ItemDelta<?, ?>> modifications = new ArrayList<>();
        modifications.add(prismContext.deltaFactory().property().createModificationReplaceProperty(F_NAME, campaignDef, new PolyString("Campaign 1+", "campaign 1")));
        modifications.add(prismContext.deltaFactory().property().createModificationReplaceProperty(F_STATE, campaignDef, IN_REVIEW_STAGE));

        executeAndCheckModification(modifications, result, 1);
        checksCountsStandard(result);
    }

    @Test
    public void test210ModifyCaseProperties() throws Exception {
        OperationResult result = new OperationResult("test210ModifyCaseProperties");

        List<ItemDelta<?, ?>> modifications = new ArrayList<>();
        ItemPath case1 = ItemPath.create(F_CASE, 1L);
        modifications.add(prismContext.deltaFactory().property().createModificationReplaceProperty(case1.append(F_CURRENT_STAGE_OUTCOME), campaignDef, SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REDUCE));
        modifications.add(prismContext.deltaFactory().property().createModificationReplaceProperty(case1.append(AccessCertificationCaseType.F_STAGE_NUMBER), campaignDef, 300));

        executeAndCheckModification(modifications, result, 0);
        checksCountsStandard(result);
    }

    @Test
    public void test220ModifyWorkItemProperties() throws Exception {
        OperationResult result = new OperationResult("test220ModifyWorkItemProperties");

        List<ItemDelta<?, ?>> modifications = deltaFor(AccessCertificationCampaignType.class)
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

        List<ItemDelta<?, ?>> modifications = deltaFor(AccessCertificationCampaignType.class)
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

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE).add(caseNoId, case100)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
        checkCasesForCampaign(campaign1Oid, 9, result);
        checkCasesTotal(9, result);
        checkWorkItemsForCampaign(campaign1Oid, 11, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID, 1, result);
        checkWorkItemsTotal(11, result);
    }

    @Test
    public void test250DeleteCase() throws Exception {
        OperationResult result = createOperationResult();

        PrismObject<AccessCertificationCampaignType> campaign10Before = getFullCampaign(campaign1Oid);
        displayValue("Campaign 10 before", campaign10Before);

        AccessCertificationCaseType case9 = new AccessCertificationCaseType();
        case9.setId(CASE_9_ID);

        List<ItemDelta<?, ?>> modifications = deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE).delete(case9)
                .asItemDeltas();

        // WHEN
        executeAndCheckModification(modifications, result, 0);

        // THEN
        PrismObject<AccessCertificationCampaignType> campaign10After = getFullCampaign(campaign1Oid);
        displayValue("Campaign 10 after", campaign10After);

        checkCasesForCampaign(campaign1Oid, 8, result);
        checkCasesTotal(8, result);
        checkWorkItemsForCampaign(campaign1Oid, 9, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID, 1, result);
        checkWorkItemsTotal(9, result);
    }

    @Test
    public void test260AddWorkItem() throws Exception {
        OperationResult result = new OperationResult("test260AddWorkItem");

        AccessCertificationWorkItemType workItem = new AccessCertificationWorkItemType(prismContext)
                .beginOriginalAssigneeRef().oid("orig1").type(UserType.COMPLEX_TYPE).<AccessCertificationWorkItemType>end()
                .beginAssigneeRef().oid("rev1").type(UserType.COMPLEX_TYPE).<AccessCertificationWorkItemType>end()
                .beginAssigneeRef().oid("rev2").type(UserType.COMPLEX_TYPE).end();

        List<ItemDelta<?, ?>> modifications = deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, NEW_CASE_ID, F_WORK_ITEM).add(workItem)
                .asItemDeltas();

        // WHEN
        executeAndCheckModification(modifications, result, 0);

        // THEN
        checkCasesForCampaign(campaign1Oid, 8, result);
        checkCasesTotal(8, result);
        checkWorkItemsForCampaign(campaign1Oid, 10, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID, 2, result);
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
        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, NEW_CASE_ID, F_WORK_ITEM, workItem.getId(), AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
                .replace(closedTimestamp)
                .asItemDeltas();

        // WHEN
        executeAndCheckModification(modifications, result, 0);

        // THEN
        checkCasesForCampaign(campaign1Oid, 8, result);
        checkCasesTotal(8, result);
        checkWorkItemsForCampaign(campaign1Oid, 10, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID, 2, result);
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

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, NEW_CASE_ID, F_WORK_ITEM).delete(workItem.clone())
                .asItemDeltas();

        // WHEN
        executeAndCheckModification(modifications, result, 0);

        // THEN
        checkCasesForCampaign(campaign1Oid, 8, result);
        checkCasesTotal(8, result);
        checkWorkItemsForCampaign(campaign1Oid, 9, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, NEW_CASE_ID, 1, result);
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

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE).add(caseNoId, case110).delete(case100)
                .item(F_CASE, 3, AccessCertificationCaseType.F_STAGE_NUMBER).replace(400)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
        checkCasesForCampaign(campaign1Oid, 9, result);
        checkCasesTotal(9, result);
        checkWorkItemsForCampaign(campaign1Oid, 9, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, SECOND_NEW_CASE_ID, 1, result);
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

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, 6, F_WORK_ITEM).add(wiNoId, wi200)
                .item(F_CASE, 6, F_WORK_ITEM).delete(wi1)
                .item(F_CASE, 6, F_WORK_ITEM, 2, F_OUTPUT, F_OUTCOME).replace(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
        checkCasesForCampaign(campaign1Oid, 9, result);
        checkCasesTotal(9, result);
        checkWorkItemsForCampaign(campaign1Oid, 10, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, SECOND_NEW_CASE_ID, 1, result);
        checkWorkItemsTotal(10, result);
    }

    @Test
    public void test330ReplaceWorkItemsExistingId() throws Exception {
        OperationResult result = new OperationResult("test330ReplaceWorkItemsExistingId");

        AccessCertificationWorkItemType wi200 = new AccessCertificationWorkItemType(prismContext);
        wi200.setId(200L);             //dangerous
        wi200.setStageNumber(44);
        wi200.assigneeRef(createObjectRef("999999", ObjectTypes.USER));

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, 6, F_WORK_ITEM).replace(wi200)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
        checkCasesForCampaign(campaign1Oid, 9, result);
        checkCasesTotal(9, result);
        checkWorkItemsForCampaign(campaign1Oid, 8, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, SECOND_NEW_CASE_ID, 1, result);
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

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, 6, F_WORK_ITEM).replace(wi250, wi251)
                .asItemDeltas();

        // TODO counts
        executeAndCheckModification(modifications, result, 0);
        checkCasesForCampaign(campaign1Oid, 9, result);
        checkCasesTotal(9, result);
        checkWorkItemsForCampaign(campaign1Oid, 9, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, SECOND_NEW_CASE_ID, 1, result);
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

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(AccessCertificationCampaignType.class)
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

        ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
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
                logger.info("Expecting case of {}:{}", campaign.getOid(), aCase.getId());
                expectedCases.add(aCase);
            }
        }
    }

    private void checkCasesForCampaign(String oid, Integer expected, OperationResult result) throws SchemaException, ObjectNotFoundException {
        ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
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

        int count = repositoryService.countContainers(AccessCertificationCaseType.class, query, null, result);
        if (expected != null) {
            assertEquals("Wrong # of certification cases", expected.intValue(), count);
        }
    }

    private void checkWorkItemsForCampaign(String oid, int expected, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .exists(T_PARENT)
                .block()
                .ownerId(oid)
                .endBlock()
                .build();
        List<AccessCertificationWorkItemType> workItems = repositoryService.searchContainers(AccessCertificationWorkItemType.class, query, null, result);
        assertWorkItemsCount(expected, workItems, " for " + oid);

        int count = repositoryService.countContainers(AccessCertificationWorkItemType.class, query, null, result);
        assertEquals("Wrong # of certification work items", expected, count);
    }

    private void checkWorkItemsForCampaignAndCase(String oid, long caseId, int expected, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AccessCertificationWorkItemType.class)
                .exists(T_PARENT)
                .block()
                .ownerId(oid)
                .and().id(caseId)
                .endBlock()
                .build();
        List<AccessCertificationWorkItemType> workItems = repositoryService.searchContainers(AccessCertificationWorkItemType.class, query, null, result);
        assertWorkItemsCount(expected, workItems, " for " + oid + ":" + caseId);

        int count = repositoryService.countContainers(AccessCertificationWorkItemType.class, query, null, result);
        assertEquals("Wrong # of certification work items", expected, count);
    }

    private void checkCasesTotal(int expected, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                .build();
        List<AccessCertificationCaseType> cases = repositoryService.searchContainers(AccessCertificationCaseType.class, query, null, result);
        assertCasesFound(expected, cases, "");

        int count = repositoryService.countContainers(AccessCertificationCaseType.class, null, null, result);        // intentionally query==null
        assertEquals("Wrong # of certification cases", expected, count);
    }

    private void assertCasesFound(Integer expected, List<AccessCertificationCaseType> cases, String desc) {
        System.out.println("Cases found" + desc + ": " + cases.size());
        if (expected != null) {
            assertEquals("Wrong # of cases" + desc, expected.intValue(), cases.size());
        }
    }

    private void checkWorkItemsTotal(int expected, OperationResult result) throws SchemaException {
        List<AccessCertificationWorkItemType> workItems = repositoryService.searchContainers(AccessCertificationWorkItemType.class, null, null, result);
        assertWorkItemsCount(expected, workItems, "");
        int count = repositoryService.countContainers(AccessCertificationWorkItemType.class, null, null, result);
        assertEquals("Wrong # of certification work items", expected, count);
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
        PrismContainerValue campaignValue = caseContainer.getParent();
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

    protected void executeAndCheckModification(List<ItemDelta<?, ?>> modifications, OperationResult result, int versionDelta) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, IOException {
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
            ItemDeltaCollectionsUtil.applyTo(modifications, expectedObject);
        }

        logger.trace("Expected object = \n{}", expectedObject.debugDump());

        boolean casesExpected = !expectedObject.asObjectable().getCase().isEmpty();

        PrismObject<AccessCertificationCampaignType> campaignFull = getFullCampaign(campaignOid, result);
        PrismContainer<AccessCertificationCaseType> caseContainerFull = campaignFull.findContainer(F_CASE);
        if (caseContainerFull != null) {
            assertFalse("campaign.case is marked as incomplete", caseContainerFull.isIncomplete());
        }

        logger.trace("Actual object from repo = \n{}", campaignFull.debugDump());

        PrismAsserts.assertEquivalent("Campaign is not as expected", expectedObject, campaignFull);
        if (expectedVersion != null) {
            AssertJUnit.assertEquals("Incorrect version", (int) expectedVersion, Integer.parseInt(campaignFull.getVersion()));
        }

        PrismObject<AccessCertificationCampaignType> campaignPlain = getCampaignPlain(campaignOid, result);
        if (casesExpected) {
            PrismContainer<AccessCertificationCaseType> caseContainerPlain = campaignPlain.findContainer(F_CASE);
            assertNotNull("campaign.case is not present", caseContainerPlain);
            assertTrue("campaign.case is NOT marked as incomplete", caseContainerPlain.isIncomplete());
        }
    }

    private PrismObject<AccessCertificationCampaignType> getFullCampaign(String campaignOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .item(F_CASE).retrieve()
                .build();
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, options, result);
    }

    private PrismObject<AccessCertificationCampaignType> getCampaignPlain(String campaignOid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, null, result);
    }

    private PrismObject<AccessCertificationCampaignType> getFullCampaign(String oid) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("getFullCampaign");
        PrismObject<AccessCertificationCampaignType> object = getFullCampaign(oid, result);
        assertSuccess(result);
        return object;
    }

    private void checksCountsStandard(OperationResult result) throws SchemaException, ObjectNotFoundException {
        checkCasesForCampaign(campaign1Oid, 7, result);
        checkCasesTotal(7, result);
        checkWorkItemsForCampaign(campaign1Oid, 10, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 1, 2, result);
        checkWorkItemsForCampaignAndCase(campaign1Oid, 2, 1, result);
        checkWorkItemsTotal(10, result);
    }

}
