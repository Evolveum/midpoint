/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.test;

import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author mederly
 */
@SuppressWarnings({ "UnusedReturnValue", "WeakerAccess", "SameParameterValue" })
public class AbstractCertificationTest extends AbstractUninitializedCertificationTest {

    @Autowired
    private AccCertResponseComputationHelper computationHelper;

    protected static final File ORGS_AND_USERS_FILE = new File(COMMON_DIR, "orgs-and-users.xml");
    protected static final File USER_BOB_FILE = new File(COMMON_DIR, "user-bob.xml");
    protected static final File USER_BOB_DEPUTY_FULL_FILE = new File(COMMON_DIR, "user-bob-deputy-full.xml");
    protected static final File USER_BOB_DEPUTY_NO_ASSIGNMENTS_FILE = new File(COMMON_DIR, "user-bob-deputy-no-assignments.xml");
    protected static final File USER_BOB_DEPUTY_NO_PRIVILEGES_FILE = new File(COMMON_DIR, "user-bob-deputy-no-privileges.xml");
    protected static final File USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_FILE = new File(COMMON_DIR, "user-administrator-deputy-no-assignments.xml");
    protected static final File USER_ADMINISTRATOR_DEPUTY_NONE_FILE = new File(COMMON_DIR, "user-administrator-deputy-none.xml");

    protected static final String ORG_GOVERNOR_OFFICE_OID = "00000000-8888-6666-0000-100000000001";
    protected static final String ORG_SCUMM_BAR_OID = "00000000-8888-6666-0000-100000000006";
    protected static final String ORG_EROOT_OID = "00000000-8888-6666-0000-300000000000";

    protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-11111111111e";
    protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
    protected static final String USER_CHEESE_OID = "c0c010c0-d34d-b33f-f00d-111111111130";
    protected static final String USER_BOB_OID = "c0c010c0-d34d-b33f-f00d-111111111134";
    protected static final String USER_BOB_DEPUTY_FULL_OID = "71d27191-df8c-4513-836e-ed01c68a4ab4";
    protected static final String USER_BOB_DEPUTY_NO_ASSIGNMENTS_OID = "afc1c45d-fdb8-48cf-860b-b305f96a07e3";
    protected static final String USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_OID = "0b88d83f-1722-4b13-b7cc-a2d500470d7f";
    protected static final String USER_ADMINISTRATOR_DEPUTY_NONE_OID = "e38df3fc-3510-45c2-a379-2b4a1406d4b6";

    protected static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
    protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

    public static final File ROLE_REVIEWER_FILE = new File(COMMON_DIR, "role-reviewer.xml");
    protected static final String ROLE_REVIEWER_OID = "00000000-d34d-b33f-f00d-ffffffff0000";

    public static final File ORG_SECURITY_TEAM_FILE = new File(COMMON_DIR, "org-security-team.xml");
    protected static final String ORG_SECURITY_TEAM_OID = "e015eb10-1426-4104-86c0-eb0cf9dc423f";

    public static final File ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_FILE = new File(COMMON_DIR, "role-eroot-user-assignment-campaign-owner.xml");
    protected static final String ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_OID = "00000000-d34d-b33f-f00d-ffffffff0001";

    public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");

    public static final File METAROLE_CXO_FILE = new File(COMMON_DIR, "metarole-cxo.xml");

    public static final File ROLE_CEO_FILE = new File(COMMON_DIR, "role-ceo.xml");
    protected static final String ROLE_CEO_OID = "00000000-d34d-b33f-f00d-000000000001";

    public static final File ROLE_COO_FILE = new File(COMMON_DIR, "role-coo.xml");
    protected static final String ROLE_COO_OID = "00000000-d34d-b33f-f00d-000000000002";

    public static final File ROLE_CTO_FILE = new File(COMMON_DIR, "role-cto.xml");
    protected static final String ROLE_CTO_OID = "00000000-d34d-b33f-f00d-000000000003";

    protected static final File ROLE_INDUCEMENT_CERT_DEF_FILE = new File(COMMON_DIR, "certification-of-role-inducements.xml");

    protected static final File TASK_TRIGGER_SCANNER_FILE = new File(COMMON_DIR, "task-trigger-scanner-manual.xml");
    protected static final String TASK_TRIGGER_SCANNER_OID = "00000000-0000-0000-0000-000000000007";

    protected DummyResource dummyResource;
    protected DummyResourceContoller dummyResourceCtl;
    protected ResourceType resourceDummyType;
    protected PrismObject<ResourceType> resourceDummy;

    protected DummyResource dummyResourceBlack;
    protected DummyResourceContoller dummyResourceCtlBlack;
    protected ResourceType resourceDummyBlackType;
    protected PrismObject<ResourceType> resourceDummyBlack;

    protected static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR, "resource-dummy.xml");
    protected static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
    protected static final String DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME = "sea";

    protected static final String RESOURCE_DUMMY_BLACK_FILENAME = COMMON_DIR + "/resource-dummy-black.xml";
    protected static final String RESOURCE_DUMMY_BLACK_OID = "10000000-0000-0000-0000-000000000305";
    protected static final String RESOURCE_DUMMY_BLACK_NAME = "black";

    @Autowired protected CertificationManagerImpl certificationManager;
    @Autowired protected AccessCertificationService certificationService;
    @Autowired protected AccCertCaseOperationsHelper operationsHelper;
    @Autowired protected AccCertUpdateHelper updateHelper;
    @Autowired protected AccCertQueryHelper queryHelper;

    protected RoleType roleCeo;
    protected RoleType roleCoo;
    protected RoleType roleCto;

    protected UserType userJack;
    protected UserType userElaine;
    protected UserType userGuybrush;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");
        super.initSystem(initTask, initResult);

        // roles
        repoAddObjectFromFile(METAROLE_CXO_FILE, RoleType.class, initResult);
        roleCeo = repoAddObjectFromFile(ROLE_CEO_FILE, RoleType.class, initResult).asObjectable();
        roleCoo = repoAddObjectFromFile(ROLE_COO_FILE, RoleType.class, initResult).asObjectable();
        roleCto = repoAddObjectFromFile(ROLE_CTO_FILE, RoleType.class, initResult).asObjectable();
        repoAddObjectFromFile(ROLE_REVIEWER_FILE, RoleType.class, initResult).asObjectable();
        repoAddObjectFromFile(ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_FILE, RoleType.class, initResult).asObjectable();

        repoAddObjectFromFile(ORG_SECURITY_TEAM_FILE, OrgType.class, initResult).asObjectable();

        repoAddObjectsFromFile(ORGS_AND_USERS_FILE, RoleType.class, initResult);
        addAndRecompute(USER_BOB_FILE, initTask, initResult);
        addAndRecompute(USER_BOB_DEPUTY_FULL_FILE, initTask, initResult);
        addAndRecompute(USER_BOB_DEPUTY_NO_ASSIGNMENTS_FILE, initTask, initResult);
        addAndRecompute(USER_BOB_DEPUTY_NO_PRIVILEGES_FILE, initTask, initResult);
        addAndRecompute(USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_FILE, initTask, initResult);
        addAndRecompute(USER_ADMINISTRATOR_DEPUTY_NONE_FILE, initTask, initResult);

        // Users
        userJack = repoAddObjectFromFile(USER_JACK_FILE, UserType.class, initResult).asObjectable();
        userElaine = getUser(USER_ELAINE_OID).asObjectable();
        userGuybrush = getUser(USER_GUYBRUSH_OID).asObjectable();

        // Resources

        dummyResourceCtl = DummyResourceContoller.create(null);
        dummyResourceCtl.extendSchemaPirate();
        dummyResource = dummyResourceCtl.getDummyResource();
        dummyResourceCtl.addAttrDef(dummyResource.getAccountObjectClass(),
                DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, String.class, false, false);
        resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
        resourceDummyType = resourceDummy.asObjectable();
        dummyResourceCtl.setResource(resourceDummy);

        dummyResourceCtlBlack = DummyResourceContoller.create(RESOURCE_DUMMY_BLACK_NAME, resourceDummyBlack);
        dummyResourceCtlBlack.extendSchemaPirate();
        dummyResourceBlack = dummyResourceCtlBlack.getDummyResource();
        resourceDummyBlack = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_BLACK_FILENAME, RESOURCE_DUMMY_BLACK_OID, initTask, initResult);
        resourceDummyBlackType = resourceDummyBlack.asObjectable();
        dummyResourceCtlBlack.setResource(resourceDummyBlack);

        // Recompute relevant objects
        recomputeUser(USER_JACK_OID, initTask, initResult);
        recomputeUser(USER_ELAINE_OID, initTask, initResult);
        recomputeUser(USER_GUYBRUSH_OID, initTask, initResult);
        recomputeUser(USER_ADMINISTRATOR_OID, initTask, initResult);
        recomputeFocus(RoleType.class, ROLE_CEO_OID, initTask, initResult);
        recomputeFocus(RoleType.class, ROLE_COO_OID, initTask, initResult);
        recomputeFocus(RoleType.class, ROLE_CTO_OID, initTask, initResult);
        recomputeFocus(RoleType.class, ROLE_REVIEWER_OID, initTask, initResult);
        recomputeFocus(RoleType.class, ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_OID, initTask, initResult);
        recomputeFocus(OrgType.class, ORG_SECURITY_TEAM_OID, initTask, initResult);
    }

    @NotNull
    @Override
    protected File getUserAdministratorFile() {
        return USER_ADMINISTRATOR_FILE;
    }

    protected AccessCertificationCaseType checkCaseSanity(Collection<AccessCertificationCaseType> caseList, String subjectOid,
            String targetOid, FocusType focus) {
        AccessCertificationCaseType aCase = findCase(caseList, subjectOid, targetOid);
        assertNotNull("Certification case for " + subjectOid + ":" + targetOid + " was not found", aCase);
        assertNotNull("reviewRequestedTimestamp", aCase.getCurrentStageCreateTimestamp());
        assertNotNull("deadline", aCase.getCurrentStageDeadline());
        assertNull("remediedTimestamp", aCase.getRemediedTimestamp());
        return checkCaseAssignmentSanity(aCase, focus);
    }

    protected AccessCertificationCaseType checkWorkItemSanity(Collection<AccessCertificationWorkItemType> workItems,
            String subjectOid, String targetOid, FocusType focus) {
        return checkWorkItemSanity(workItems, subjectOid, targetOid, focus, 1);
    }

    protected AccessCertificationCaseType checkWorkItemSanity(Collection<AccessCertificationWorkItemType> workItems,
            String subjectOid, String targetOid, FocusType focus, int iteration) {
        AccessCertificationWorkItemType workItem = findWorkItem(workItems, subjectOid, targetOid, iteration);
        assertNotNull("Certification work item for " + subjectOid + ":" + targetOid + " was not found (iteration "
                + iteration + ")", workItem);
        AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCase(workItem);
        assertNotNull("No case for " + workItem, aCase);
        assertNotNull("reviewRequestedTimestamp", aCase.getCurrentStageCreateTimestamp());
        assertNotNull("deadline", aCase.getCurrentStageDeadline());
        assertNull("remediedTimestamp", aCase.getRemediedTimestamp());
        return checkCaseAssignmentSanity(aCase, focus);
    }

    protected AccessCertificationCaseType checkCaseSanity(Collection<AccessCertificationCaseType> caseList, String objectOid,
            String targetOid, FocusType focus, String tenantOid, String orgOid,
            ActivationStatusType administrativeStatus) {
        AccessCertificationCaseType aCase = checkCaseSanity(caseList, objectOid, targetOid, focus);
        String realTenantOid = aCase.getTenantRef() != null ? aCase.getTenantRef().getOid() : null;
        String realOrgOid = aCase.getOrgRef() != null ? aCase.getOrgRef().getOid() : null;
        ActivationStatusType realStatus = aCase.getActivation() != null ? aCase.getActivation().getAdministrativeStatus() : null;
        assertEquals("incorrect tenant org", tenantOid, realTenantOid);
        assertEquals("incorrect org org", orgOid, realOrgOid);
        assertEquals("incorrect admin status", administrativeStatus, realStatus);
        return aCase;
    }

    protected AccessCertificationCaseType checkWorkItemSanity(Collection<AccessCertificationWorkItemType> workItems,
            String objectOid, String targetOid, FocusType focus, String tenantOid, String orgOid, ActivationStatusType administrativeStatus) {
        AccessCertificationCaseType aCase = checkWorkItemSanity(workItems, objectOid, targetOid, focus);
        String realTenantOid = aCase.getTenantRef() != null ? aCase.getTenantRef().getOid() : null;
        String realOrgOid = aCase.getOrgRef() != null ? aCase.getOrgRef().getOid() : null;
        ActivationStatusType realStatus = aCase.getActivation() != null ? aCase.getActivation().getAdministrativeStatus() : null;
        assertEquals("incorrect tenant org", tenantOid, realTenantOid);
        assertEquals("incorrect org org", orgOid, realOrgOid);
        assertEquals("incorrect admin status", administrativeStatus, realStatus);
        return aCase;
    }

    protected AccessCertificationCaseType checkCaseAssignmentSanity(AccessCertificationCaseType aCase, FocusType focus) {
        assertEquals("Wrong class for case", AccessCertificationAssignmentCaseType.class, aCase.getClass());
        AccessCertificationAssignmentCaseType assignmentCase = (AccessCertificationAssignmentCaseType) aCase;
        long id = assignmentCase.getAssignment().getId();
        List<AssignmentType> assignmentList;
        if (Boolean.TRUE.equals(assignmentCase.isIsInducement())) {
            assignmentList = ((AbstractRoleType) focus).getInducement();
        } else {
            assignmentList = focus.getAssignment();
        }
        for (AssignmentType assignment : assignmentList) {
            if (id == assignment.getId()) {
                assertEquals("Wrong assignment in certification case", assignment, assignmentCase.getAssignment());
                return aCase;
            }
        }
        throw new AssertionError("Assignment with ID " + id + " not found among assignments of " + focus);
    }

    protected AccessCertificationCaseType findCase(Collection<AccessCertificationCaseType> caseList, String subjectOid, String targetOid) {
        for (AccessCertificationCaseType aCase : caseList) {
            if (aCase.getTargetRef() != null && aCase.getTargetRef().getOid().equals(targetOid) &&
                    aCase.getObjectRef() != null && aCase.getObjectRef().getOid().equals(subjectOid)) {
                return aCase;
            }
        }
        return null;
    }

    protected AccessCertificationWorkItemType findWorkItem(Collection<AccessCertificationWorkItemType> workItems,
            String subjectOid, String targetOid, int iteration) {
        for (AccessCertificationWorkItemType workItem : workItems) {
            AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCase(workItem);
            if (aCase != null && aCase.getTargetRef() != null && aCase.getTargetRef().getOid().equals(targetOid) &&
                    aCase.getObjectRef() != null && aCase.getObjectRef().getOid().equals(subjectOid) &&
                    norm(workItem.getIteration()) == iteration) {
                return workItem;
            }
        }
        return null;
    }

    protected void assertApproximateTime(String itemName, Date expected, XMLGregorianCalendar actual) {
        assertNotNull("missing " + itemName, actual);
        Date actualAsDate = XmlTypeConverter.toDate(actual);
        assertTrue(itemName + " out of range; expected " + expected + ", found " + actualAsDate,
                Math.abs(actualAsDate.getTime() - expected.getTime()) < 600000);     // 10 minutes
    }

    // assumes iteration 1
    protected void assertSanityAfterCampaignCreate(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition) {
        assertEquals("Unexpected certification cases", 0, campaign.getCase().size());
        assertStateAndStage(campaign, CREATED, 0);
        assertEquals("Unexpected # of stages", definition.getStageDefinition().size(), campaign.getStageDefinition().size());
        assertDefinitionAndOwner(campaign, definition);
        assertNull("Unexpected start time", campaign.getStartTimestamp());
        assertNull("Unexpected end time", campaign.getEndTimestamp());
    }

    protected void assertSanityAfterCampaignStart(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition, int cases)
            throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        assertSanityAfterCampaignStart(campaign, definition, cases, 1, 1, new Date());
    }

    protected void assertSanityAfterCampaignStart(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition,
            int cases, int iteration, int expectedStages, Date expectedStartTime)
            throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        assertStateStageIteration(campaign, IN_REVIEW_STAGE, 1, iteration);
        assertDefinitionAndOwner(campaign, definition);
        assertApproximateTime("start time", expectedStartTime, campaign.getStartTimestamp());
        assertNull("Unexpected end time", campaign.getEndTimestamp());
        assertEquals("wrong # of defined stages", definition.getStageDefinition().size(), campaign.getStageDefinition().size());
        assertEquals("wrong # of stages", expectedStages, campaign.getStage().size());
        AccessCertificationStageType stage = campaign.getStage().stream().filter(s -> norm(s.getIteration()) == iteration && s.getNumber() == 1).findFirst().orElse(null);
        assertNotNull("No stage #1 for current iteration", stage);
        assertEquals("wrong stage #", 1, stage.getNumber());
        assertApproximateTime("stage 1 start", expectedStartTime, stage.getStartTimestamp());
        assertNotNull("stage 1 deadline", stage.getDeadline());       // too lazy to compute exact datetime
        assertNull("unexpected stage 1 end", stage.getEndTimestamp());
        assertEquals("Wrong number of certification cases", cases, campaign.getCase().size());

        PrismObject<AccessCertificationDefinitionType> def = getObjectViaRepo(AccessCertificationDefinitionType.class, definition.getOid());
        if (iteration == 1) {
            assertApproximateTime("last campaign started", expectedStartTime, def.asObjectable().getLastCampaignStartedTimestamp());
            assertNull("unexpected last campaign closed", def.asObjectable().getLastCampaignClosedTimestamp());
        } else {
            assertNotNull("last campaign closed", def.asObjectable().getLastCampaignClosedTimestamp());
        }
        assertCasesCount(campaign.getOid(), cases);
    }

    protected void assertSanityAfterStageOpen(AccessCertificationCampaignType campaign,
            AccessCertificationDefinitionType definition, int stageNumber) {
        assertSanityAfterStageOpen(campaign, definition, stageNumber, 1, stageNumber);
    }

    protected void assertSanityAfterStageOpen(AccessCertificationCampaignType campaign,
            AccessCertificationDefinitionType definition, int stageNumber, int iteration, int expectedStages) {
        assertStateStageIteration(campaign, IN_REVIEW_STAGE, stageNumber, iteration);
        assertDefinitionAndOwner(campaign, definition);
        assertApproximateTime("start time", new Date(), campaign.getStartTimestamp());
        assertNull("Unexpected end time", campaign.getEndTimestamp());
        assertEquals("wrong # of defined stages", definition.getStageDefinition().size(), campaign.getStageDefinition().size());
        assertEquals("wrong # of stages", expectedStages, campaign.getStage().size());
        AccessCertificationStageType stage = CertCampaignTypeUtil.findStage(campaign, stageNumber);
        assertEquals("wrong stage #", stageNumber, stage.getNumber());
        assertApproximateTime("stage start", new Date(), stage.getStartTimestamp());
        assertNotNull("stage deadline", stage.getDeadline());       // too lazy to compute exact datetime
        assertNull("unexpected stage end", stage.getEndTimestamp());
    }

    protected void assertStateStageIteration(AccessCertificationCampaignType campaign, AccessCertificationCampaignStateType state, int stage, int iteration) {
        assertStateAndStage(campaign, state, stage);
        assertIteration(campaign, iteration);
    }

    protected void assertStateAndStage(AccessCertificationCampaignType campaign, AccessCertificationCampaignStateType state, int stage) {
        assertEquals("Unexpected campaign state", state, campaign.getState());
        assertEquals("Unexpected stage number", stage, campaign.getStageNumber());
    }

    protected void assertIteration(AccessCertificationCampaignType campaign, int iteration) {
        assertEquals("Unexpected campaign iteration", iteration, norm(campaign.getIteration()));
    }

    protected void assertDefinitionAndOwner(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType certificationDefinition) {
        assertDefinitionAndOwner(campaign, certificationDefinition, getSecurityContextUserOid());
    }

    protected void assertDefinitionAndOwner(AccessCertificationCampaignType campaign,
            AccessCertificationDefinitionType certificationDefinition, String expectedOwnerOid) {
        assertRefEquals("Unexpected ownerRef", ObjectTypeUtil.createObjectRef(expectedOwnerOid, ObjectTypes.USER), campaign.getOwnerRef());
        assertRefEquals("Unexpected definitionRef",
                ObjectTypeUtil.createObjectRef(certificationDefinition, prismContext),
                campaign.getDefinitionRef());
    }

    protected void assertCaseReviewers(AccessCertificationCaseType _case, AccessCertificationResponseType currentStageOutcome,
            int currentStage, List<String> reviewerOidList) {
        assertEquals("wrong current stage outcome for " + _case, OutcomeUtils.toUri(currentStageOutcome), _case.getCurrentStageOutcome());
        assertEquals("wrong current stage number for " + _case, currentStage, _case.getStageNumber());
        Set<String> realReviewerOids = CertCampaignTypeUtil.getCurrentReviewers(_case).stream().map(ref -> ref.getOid()).collect(Collectors.toSet());
        assertEquals("wrong reviewer oids for " + _case, new HashSet<>(reviewerOidList), realReviewerOids);
    }

    protected void recordDecision(String campaignOid, AccessCertificationCaseType aCase, AccessCertificationResponseType response,
            String comment, String reviewerOid, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        Authentication originalAuthentication = null;
        String realReviewerOid;
        if (reviewerOid != null) {
            originalAuthentication = SecurityContextHolder.getContext().getAuthentication();
            login(getUser(reviewerOid));
            realReviewerOid = reviewerOid;
        } else {
            realReviewerOid = securityContextManager.getPrincipal().getOid();
        }
        List<AccessCertificationWorkItemType> workItems = aCase.getWorkItem().stream()
                .filter(wi -> ObjectTypeUtil.containsOid(wi.getAssigneeRef(), realReviewerOid))
                .filter(wi -> wi.getStageNumber() == aCase.getStageNumber())
                .filter(wi -> norm(wi.getIteration()) == norm(aCase.getIteration()))
                .collect(Collectors.toList());
        assertEquals("Wrong # of current work items for " + realReviewerOid + " in " + aCase, 1, workItems.size());
        long id = aCase.asPrismContainerValue().getId();
        certificationManager.recordDecision(campaignOid, id, workItems.get(0).getId(), response, comment, task, result);
        if (reviewerOid != null) {
            SecurityContextHolder.getContext().setAuthentication(originalAuthentication);
        }
    }

    // TODO remove redundant check on outcomes (see assertCaseOutcome)
    protected void assertSingleDecision(AccessCertificationCaseType aCase, AccessCertificationResponseType response,
            String comment, int stageNumber, int iteration, String reviewerOid, AccessCertificationResponseType currentStageOutcome,
            boolean checkHistory) {
        List<AccessCertificationWorkItemType> currentWorkItems = getCurrentWorkItems(aCase, stageNumber, iteration, false);
        assertEquals("wrong # of decisions for stage " + stageNumber + " for case #" + aCase.getId(), 1, currentWorkItems.size());
        AccessCertificationWorkItemType workItem = currentWorkItems.get(0);
        assertEquals("wrong response", response, OutcomeUtils.fromUri(WorkItemTypeUtil.getOutcome(workItem)));
        assertEquals("wrong comment", comment, WorkItemTypeUtil.getComment(workItem));
        assertEquals("Wrong # of reviewers", 1, workItem.getAssigneeRef().size());
        assertRefEquals("wrong reviewerRef", ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER), workItem.getAssigneeRef().get(0));
        assertEquals("wrong stage number", (Integer) stageNumber, workItem.getStageNumber());
        if (response != null) {
            assertApproximateTime("timestamp", new Date(), workItem.getOutputChangeTimestamp());
        }
        assertEquals("wrong current stage outcome", OutcomeUtils.toUri(currentStageOutcome), aCase.getCurrentStageOutcome());
        if (checkHistory) {
            assertHistoricOutcome(aCase, stageNumber, iteration, currentStageOutcome);
        }
    }

    protected void assertReviewerDecision(AccessCertificationCaseType aCase, AccessCertificationResponseType response,
            String comment, int stageNumber, int iteration, String reviewerOid, AccessCertificationResponseType currentStageOutcome,
            boolean checkHistory) {
        AccessCertificationWorkItemType workItem = getWorkItemsForReviewer(aCase, stageNumber, iteration, reviewerOid);
        assertNotNull("No work item for reviewer " + reviewerOid + " in stage " + stageNumber, workItem);
        assertEquals("wrong response", response, OutcomeUtils.fromUri(WorkItemTypeUtil.getOutcome(workItem)));
        assertEquals("wrong comment", comment, WorkItemTypeUtil.getComment(workItem));
        if (response != null) {
            assertApproximateTime("timestamp", new Date(), workItem.getOutputChangeTimestamp());
        }
        assertEquals("wrong current stage outcome", OutcomeUtils.toUri(currentStageOutcome), aCase.getCurrentStageOutcome());
        if (checkHistory) {
            assertHistoricOutcome(aCase, stageNumber, iteration, currentStageOutcome);
        }
    }

    protected void assertHistoricOutcome(AccessCertificationCaseType aCase, int stageNumber, int iteration,
            AccessCertificationResponseType outcome) {
        boolean found = false;
        for (CaseEventType event : aCase.getEvent()) {
            if (!(event instanceof StageCompletionEventType)) {
                continue;
            }
            StageCompletionEventType completionEvent = (StageCompletionEventType) event;
            if (completionEvent.getStageNumber() == stageNumber && norm(completionEvent.getIteration()) == iteration) {   // TODO sure about iteration check?
                assertEquals("Wrong outcome stored for stage #" + stageNumber + " in " + aCase, OutcomeUtils.toUri(outcome), completionEvent.getOutcome());
                if (found) {
                    fail("Duplicate outcome stored for stage #" + stageNumber + " in " + aCase);
                }
                found = true;
            }
        }
        assertTrue("No outcome stored for stage #" + stageNumber + " in " + aCase, found);
    }

    protected void assertCaseHistoricOutcomes(AccessCertificationCaseType aCase, AccessCertificationResponseType... outcomes) {
        assertCaseHistoricOutcomes(aCase, 1, outcomes);
    }

    protected void assertCaseHistoricOutcomes(AccessCertificationCaseType aCase, int iteration, AccessCertificationResponseType... outcomes) {
        for (int stage = 0; stage < outcomes.length; stage++) {
            assertHistoricOutcome(aCase, stage + 1, iteration, outcomes[stage]);
        }
        assertEquals("wrong # of stored stage outcomes", outcomes.length, CertCampaignTypeUtil.getCompletedStageEvents(aCase, iteration).size());
    }

    // we return also closed ones (TODO: what is meant by 'current' work items?)
    public List<AccessCertificationWorkItemType> getCurrentWorkItems(AccessCertificationCaseType _case, int stageNumber,
            int iteration, boolean decidedOnly) {
        List<AccessCertificationWorkItemType> rv = new ArrayList<>();
        for (AccessCertificationWorkItemType workItem : _case.getWorkItem()) {
            if (decidedOnly && WorkItemTypeUtil.getOutcome(workItem) == null) {
                continue;
            }
            if (workItem.getStageNumber() == stageNumber && norm(workItem.getIteration()) == iteration) {
                rv.add(workItem.clone());
            }
        }
        return rv;
    }

    public AccessCertificationWorkItemType getWorkItemsForReviewer(AccessCertificationCaseType _case, int stageNumber,
            int iteration, String reviewerOid) {
        for (AccessCertificationWorkItemType workItem : _case.getWorkItem()) {
            if (workItem.getStageNumber() == stageNumber && norm(workItem.getIteration()) == iteration &&
                    ObjectTypeUtil.containsOid(workItem.getAssigneeRef(), reviewerOid)) {
                return workItem;
            }
        }
        return null;
    }

    protected void assertNoDecision(AccessCertificationCaseType _case, int stageNumber, int iteration,
            AccessCertificationResponseType aggregatedResponse, boolean checkHistory) {
        List<AccessCertificationWorkItemType> currentWorkItems = getCurrentWorkItems(_case, stageNumber, iteration, true);
        assertEquals("wrong # of decisions", 0, currentWorkItems.size());
        assertEquals("wrong current response", OutcomeUtils.toUri(aggregatedResponse), _case.getCurrentStageOutcome());
        if (checkHistory) {
            assertHistoricOutcome(_case, stageNumber, iteration, aggregatedResponse);
        }
    }

    protected void assertCurrentState(AccessCertificationCaseType _case, AccessCertificationResponseType aggregatedResponse, int currentResponseStage) {
        assertEquals("wrong current response", OutcomeUtils.toUri(aggregatedResponse), _case.getCurrentStageOutcome());
        assertEquals("wrong current response stage number", currentResponseStage, _case.getStageNumber());
    }

    protected void assertWorkItemsCount(AccessCertificationCaseType _case, int count) {
        assertEquals("Wrong # of work items", count, _case.getWorkItem().size());
    }

    protected void assertDecision2(AccessCertificationCaseType _case, AccessCertificationResponseType response, String comment,
            int stageNumber, int iteration, String reviewerOid, AccessCertificationResponseType aggregatedResponse) {
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(_case, stageNumber, iteration, reviewerOid);
        assertNotNull("decision does not exist", workItem);
        assertEquals("wrong response", response, OutcomeUtils.fromUri(WorkItemTypeUtil.getOutcome(workItem)));
        assertEquals("wrong comment", comment, WorkItemTypeUtil.getComment(workItem));
        if (response != null) {
            assertApproximateTime("timestamp", new Date(), workItem.getOutputChangeTimestamp());
        }
        assertEquals("wrong current response", OutcomeUtils.toUri(aggregatedResponse), _case.getCurrentStageOutcome());
    }

    protected AccessCertificationCampaignType getCampaignWithCases(String campaignOid) throws ConfigurationException,
            ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = taskManager.createTaskInstance(AbstractCertificationTest.class.getName() + ".getObject");
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> options =
                schemaHelper.getOperationOptionsBuilder().item(F_CASE).retrieve().build();
        AccessCertificationCampaignType campaign = modelService.getObject(AccessCertificationCampaignType.class, campaignOid, options, task, result).asObjectable();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        return campaign;
    }

    private int countCampaignCases(String campaignOid) throws SchemaException, SecurityViolationException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        Task task = taskManager.createTaskInstance(AbstractCertificationTest.class.getName() + ".countCampaignCases");
        OperationResult result = task.getResult();
        ObjectQuery query = prismContext.queryFor(AccessCertificationCaseType.class)
                .ownerId(campaignOid)
                .build();
        int rv = modelService.countContainers(AccessCertificationCaseType.class, query, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        return rv;
    }

    protected void assertSanityAfterStageClose(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition, int stageNumber) {
        assertSanityAfterStageClose(campaign, definition, stageNumber, 1, stageNumber);
    }

    protected void assertSanityAfterStageClose(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition,
            int stageNumber, int iteration, int expectedStages) {
        assertStateStageIteration(campaign, REVIEW_STAGE_DONE, stageNumber, iteration);
        assertDefinitionAndOwner(campaign, definition);
        assertNull("Unexpected end time", campaign.getEndTimestamp());
        assertEquals("wrong # of stages", expectedStages, campaign.getStage().size());
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        assertNotNull(stage);
        assertEquals("wrong stage #", stageNumber, stage.getNumber());
        assertEquals("wrong stage iteration #", iteration, norm(stage.getIteration()));
        assertApproximateTime("stage start", new Date(), stage.getStartTimestamp());
        assertApproximateTime("stage end", new Date(), stage.getStartTimestamp());

        for (AccessCertificationCaseType aCase : campaign.getCase()) {
            if (aCase.getStageNumber() != stageNumber || norm(aCase.getIteration()) != iteration) {
                continue;
            }
            checkCaseOutcomesSanity(aCase, campaign, stageNumber);
        }
    }

    private void checkCaseOutcomesSanity(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, int stageNumber) {
        assertEquals("Wrong # of completed stage outcomes", stageNumber, CertCampaignTypeUtil.getCompletedStageEvents(aCase, norm(campaign.getIteration())).size());
        AccessCertificationResponseType expectedOverall = computationHelper.computeOverallOutcome(aCase, campaign);
        assertEquals("Inconsistent overall outcome", OutcomeUtils.toUri(expectedOverall), aCase.getOutcome());
    }

    // completedStage - if not null, checks the stage outcome in the history list
    protected void assertCaseOutcome(List<AccessCertificationCaseType> caseList, String subjectOid, String targetOid,
            AccessCertificationResponseType expectedStageOutcome, AccessCertificationResponseType expectedOverallOutcome, Integer completedStage) {
        AccessCertificationCaseType aCase = findCase(caseList, subjectOid, targetOid);
        assertEquals("Wrong stage outcome in " + aCase, OutcomeUtils.toUri(expectedStageOutcome), aCase.getCurrentStageOutcome());
        assertEquals("Wrong overall outcome in " + aCase, OutcomeUtils.toUri(expectedOverallOutcome), aCase.getOutcome());

        if (completedStage != null) {
            assertHistoricOutcome(aCase, completedStage, norm(aCase.getIteration()), expectedStageOutcome);
        }
    }

    protected void assertPercentCompleteAll(String campaignOid, int expCasesComplete, int expCasesDecided, int expDecisionsDone)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, expCasesComplete, expCasesDecided, expDecisionsDone);
    }

    protected void assertPercentCompleteCurrent(String campaignOid, int expCasesComplete, int expCasesDecided, int expDecisionsDone)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteCurrent(campaign, expCasesComplete, expCasesDecided, expDecisionsDone);
    }

    protected void assertPercentCompleteCurrentIteration(String campaignOid, int expCasesComplete, int expCasesDecided, int expDecisionsDone)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteCurrentIteration(campaign, expCasesComplete, expCasesDecided, expDecisionsDone);
    }

    protected void assertCasesCount(String campaignOid, int expectedCases)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        int cases = countCampaignCases(campaignOid);
        assertEquals("Wrong # of cases for campaign " + campaignOid, expectedCases, cases);
    }

    protected void assertPercentCompleteAll(AccessCertificationCampaignType campaign, int expCasesComplete, int expCasesDecided, int expDecisionsDone) {
        int casesCompletePercentage = Math.round(CertCampaignTypeUtil.getCasesCompletedPercentageAllStagesAllIterations(campaign));
        System.out.println("Cases completed (all stages, all iterations) = " + casesCompletePercentage + " %");
        assertEquals("Wrong case complete percentage (all stages, all iterations)", expCasesComplete, casesCompletePercentage);

        int casesDecidedPercentage = Math.round(CertCampaignTypeUtil.getCasesDecidedPercentageAllStagesAllIterations(campaign));
        System.out.println("Cases decided (all stages, all iterations) = " + casesDecidedPercentage + " %");
        assertEquals("Wrong case decided percentage (all stages, all iterations)", expCasesDecided, casesDecidedPercentage);

        int decisionsDonePercentage = Math.round(CertCampaignTypeUtil.getWorkItemsCompletedPercentageAllStagesAllIterations(campaign));
        System.out.println("Decisions completed (all stages, all iterations) = " + decisionsDonePercentage + " %");
        assertEquals("Wrong decisions complete percentage (all stages, all iterations)", expDecisionsDone, decisionsDonePercentage);
    }

    protected void assertPercentCompleteCurrent(AccessCertificationCampaignType campaign, int expCasesComplete, int expCasesDecided, int expDecisionsDone) {
        int casesCompletePercentage = Math.round(CertCampaignTypeUtil.getCasesCompletedPercentageCurrStageCurrIteration(campaign));
        System.out.println("Cases completed (current stage, current iteration) = " + casesCompletePercentage + " %");
        assertEquals("Wrong case complete percentage (current stage, current iteration)", expCasesComplete, casesCompletePercentage);

        int casesDecidedPercentage = Math.round(CertCampaignTypeUtil.getCasesDecidedPercentageCurrStageCurrIteration(campaign));
        System.out.println("Cases decided (current stage, current iteration) = " + casesDecidedPercentage + " %");
        assertEquals("Wrong case decided percentage (current stage, current iteration)", expCasesDecided, casesDecidedPercentage);

        int decisionsDonePercentage = Math.round(CertCampaignTypeUtil.getWorkItemsCompletedPercentageCurrStageCurrIteration(campaign));
        System.out.println("Decisions completed (current stage, current iteration) = " + decisionsDonePercentage + " %");
        assertEquals("Wrong decisions complete percentage (current stage, current iteration)", expDecisionsDone, decisionsDonePercentage);
    }

    protected void assertPercentCompleteCurrentIteration(AccessCertificationCampaignType campaign, int expCasesComplete, int expCasesDecided, int expDecisionsDone) {
        int casesCompletePercentage = Math.round(CertCampaignTypeUtil.getCasesCompletedPercentageAllStagesCurrIteration(campaign));
        System.out.println("Cases completed (all stages, current iteration) = " + casesCompletePercentage + " %");
        assertEquals("Wrong case complete percentage (all stages, current iteration)", expCasesComplete, casesCompletePercentage);

        int casesDecidedPercentage = Math.round(CertCampaignTypeUtil.getCasesDecidedPercentageAllStagesCurrIteration(campaign));
        System.out.println("Cases decided (all stages, current iteration) = " + casesDecidedPercentage + " %");
        assertEquals("Wrong case decided percentage (all stages, current iteration)", expCasesDecided, casesDecidedPercentage);

        int decisionsDonePercentage = Math.round(CertCampaignTypeUtil.getWorkItemsCompletedPercentageAllStagesCurrIteration(campaign));
        System.out.println("Decisions completed (all stages, current iteration) = " + decisionsDonePercentage + " %");
        assertEquals("Wrong decisions complete percentage (all stages, current iteration)", expDecisionsDone, decisionsDonePercentage);
    }

    protected void assertPercentCompleteCurrentStage(AccessCertificationCampaignType campaign, int expCasesComplete, int expCasesDecided, int expDecisionsDone) {
        int casesCompletePercentage = Math.round(CertCampaignTypeUtil.getCasesCompletedPercentageCurrStageAllIterations(campaign));
        System.out.println("Cases completed (current stage, all iterations) = " + casesCompletePercentage + " %");
        assertEquals("Wrong case complete percentage (current stage, all iterations)", expCasesComplete, casesCompletePercentage);

        int casesDecidedPercentage = Math.round(CertCampaignTypeUtil.getCasesDecidedPercentageCurrStageAllIterations(campaign));
        System.out.println("Cases decided (current stage, all iterations) = " + casesDecidedPercentage + " %");
        assertEquals("Wrong case decided percentage (current stage, all iterations)", expCasesDecided, casesDecidedPercentage);

        int decisionsDonePercentage = Math.round(CertCampaignTypeUtil.getWorkItemsCompletedPercentageCurrStageAllIterations(campaign));
        System.out.println("Decisions completed (current stage, all iterations) = " + decisionsDonePercentage + " %");
        assertEquals("Wrong decisions complete percentage (current stage, all iterations)", expDecisionsDone, decisionsDonePercentage);
    }

    @SuppressWarnings("unused")
    public void reimportTriggerTask(OperationResult result) throws FileNotFoundException {
        taskManager.suspendAndDeleteTasks(singletonList(TASK_TRIGGER_SCANNER_OID), 60000L, true, result);
        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE, result);
    }

    public void importTriggerTask(OperationResult result) throws FileNotFoundException {
        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE, result);
    }

    protected void waitForCampaignTasks(String campaignOid, int timeout, OperationResult result) throws CommonException {
        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(campaignOid)
                .build();
        SearchResultList<PrismObject<TaskType>> campaignTasks = repositoryService.searchObjects(TaskType.class, query, null, result);
        for (PrismObject<TaskType> campaignTask : campaignTasks) {
            if (campaignTask.asObjectable().getExecutionStatus() != TaskExecutionStatusType.CLOSED &&
                    campaignTask.asObjectable().getExecutionStatus() != TaskExecutionStatusType.SUSPENDED) {
                waitForTaskFinish(campaignTask.getOid(), false, timeout);
            }
        }
    }

    protected void assertCertificationMetadata(MetadataType metadata, String expectedOutcome, Set<String> expectedCertifiers,
            Set<String> expectedComments) {
        assertNotNull("No metadata", metadata);
        assertEquals("Wrong outcome", expectedOutcome, metadata.getCertificationOutcome());
        PrismAsserts.assertReferenceOids("Wrong certifiers", expectedCertifiers, metadata.getCertifierRef());
        assertEquals("Wrong certifier comments", expectedComments, new HashSet<>(metadata.getCertifierComment()));
    }

    @NotNull
    protected SearchResultList<PrismObject<AccessCertificationCampaignType>> getAllCampaigns(OperationResult result) throws SchemaException {
        return repositoryService.searchObjects(AccessCertificationCampaignType.class, null, null, result);
    }
}
