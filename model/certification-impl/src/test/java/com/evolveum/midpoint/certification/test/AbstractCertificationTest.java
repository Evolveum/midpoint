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

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.RetrieveOption.INCLUDE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
public class AbstractCertificationTest extends AbstractModelIntegrationTest {

	@Autowired
	private AccCertResponseComputationHelper computationHelper;
	
	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	protected static final File ORGS_AND_USERS_FILE = new File(COMMON_DIR, "orgs-and-users.xml");
	protected static final File USER_BOB_FILE = new File(COMMON_DIR, "user-bob.xml");
	protected static final File USER_BOB_DEPUTY_FULL_FILE = new File(COMMON_DIR, "user-bob-deputy-full.xml");
	protected static final File USER_BOB_DEPUTY_NO_ASSIGNMENTS_FILE = new File(COMMON_DIR, "user-bob-deputy-no-assignments.xml");
	protected static final File USER_BOB_DEPUTY_NO_PRIVILEGES_FILE = new File(COMMON_DIR, "user-bob-deputy-no-privileges.xml");
	protected static final File USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_FILE = new File(COMMON_DIR, "user-administrator-deputy-no-assignments.xml");
	protected static final File USER_ADMINISTRATOR_DEPUTY_NONE_FILE = new File(COMMON_DIR, "user-administrator-deputy-none.xml");

	protected static final String ORG_GOVERNOR_OFFICE_OID = "00000000-8888-6666-0000-100000000001";
	protected static final String ORG_SCUMM_BAR_OID = "00000000-8888-6666-0000-100000000006";
	protected static final String ORG_MINISTRY_OF_OFFENSE_OID = "00000000-8888-6666-0000-100000000003";
	protected static final String ORG_MINISTRY_OF_DEFENSE_OID = "00000000-8888-6666-0000-100000000002";
	protected static final String ORG_MINISTRY_OF_RUM_OID = "00000000-8888-6666-0000-100000000004";
	protected static final String ORG_SWASHBUCKLER_SECTION_OID = "00000000-8888-6666-0000-100000000005";
	protected static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";
	protected static final String ORG_SAVE_ELAINE_OID = "00000000-8888-6666-0000-200000000001";
	protected static final String ORG_EROOT_OID = "00000000-8888-6666-0000-300000000000";

	protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-11111111111e";
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	protected static final String USER_LECHUCK_OID = "c0c010c0-d34d-b33f-f00d-1c1c11cc11c2";
	protected static final String USER_CHEESE_OID = "c0c010c0-d34d-b33f-f00d-111111111130";
	protected static final String USER_CHEF_OID = "c0c010c0-d34d-b33f-f00d-111111111131";
	protected static final String USER_BARKEEPER_OID = "c0c010c0-d34d-b33f-f00d-111111111132";
	protected static final String USER_CARLA_OID = "c0c010c0-d34d-b33f-f00d-111111111133";
	protected static final String USER_BOB_OID = "c0c010c0-d34d-b33f-f00d-111111111134";
	protected static final String USER_BOB_DEPUTY_FULL_OID = "71d27191-df8c-4513-836e-ed01c68a4ab4";
	protected static final String USER_BOB_DEPUTY_NO_ASSIGNMENTS_OID = "afc1c45d-fdb8-48cf-860b-b305f96a07e3";
	protected static final String USER_BOB_DEPUTY_NO_PRIVILEGES_OID = "ad371f45-352d-4c1f-80f3-2e279af399ae";
	protected static final String USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_OID = "0b88d83f-1722-4b13-b7cc-a2d500470d7f";
	protected static final String USER_ADMINISTRATOR_DEPUTY_NONE_OID = "e38df3fc-3510-45c2-a379-2b4a1406d4b6";

	public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_NAME = "administrator";

	protected static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	protected static final String USER_JACK_USERNAME = "jack";

	public static final File ROLE_REVIEWER_FILE = new File(COMMON_DIR, "role-reviewer.xml");
	protected static final String ROLE_REVIEWER_OID = "00000000-d34d-b33f-f00d-ffffffff0000";

	public static final File ORG_SECURITY_TEAM_FILE = new File(COMMON_DIR, "org-security-team.xml");
	protected static final String ORG_SECURITY_TEAM_OID = "e015eb10-1426-4104-86c0-eb0cf9dc423f";

	public static final File ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_FILE = new File(COMMON_DIR, "role-eroot-user-assignment-campaign-owner.xml");
	protected static final String ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_OID = "00000000-d34d-b33f-f00d-ffffffff0001";

	public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

	public static final File METAROLE_CXO_FILE = new File(COMMON_DIR, "metarole-cxo.xml");
	protected static final String METAROLE_CXO_OID = "00000000-d34d-b33f-f00d-444444444444";

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
	protected static final String RESOURCE_DUMMY_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004";
	protected static final String DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME = "sea";

	protected static final String RESOURCE_DUMMY_BLACK_FILENAME = COMMON_DIR + "/resource-dummy-black.xml";
	protected static final String RESOURCE_DUMMY_BLACK_OID = "10000000-0000-0000-0000-000000000305";
	protected static final String RESOURCE_DUMMY_BLACK_NAME = "black";
	protected static final String RESOURCE_DUMMY_BLACK_NAMESPACE = MidPointConstants.NS_RI;

	protected static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);

    @Autowired protected CertificationManagerImpl certificationManager;
	@Autowired protected AccessCertificationService certificationService;
	@Autowired protected AccCertUpdateHelper updateHelper;
	@Autowired protected AccCertQueryHelper queryHelper;

	protected RoleType roleCeo;
	protected RoleType roleCoo;
	protected RoleType roleCto;
	protected RoleType roleSuperuser;

	protected UserType userAdministrator;
	protected UserType userJack;
	protected UserType userElaine;
	protected UserType userGuybrush;

    @Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);

		modelService.postInit(initResult);
		
		// System Configuration
		try {
			repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}

		repoAddObjectsFromFile(ORGS_AND_USERS_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(USER_BOB_FILE, UserType.class, initResult);
		repoAddObjectFromFile(USER_BOB_DEPUTY_FULL_FILE, UserType.class, initResult);
		repoAddObjectFromFile(USER_BOB_DEPUTY_NO_ASSIGNMENTS_FILE, UserType.class, initResult);
		repoAddObjectFromFile(USER_BOB_DEPUTY_NO_PRIVILEGES_FILE, UserType.class, initResult);
		repoAddObjectFromFile(USER_ADMINISTRATOR_DEPUTY_NO_ASSIGNMENTS_FILE, UserType.class, initResult);
		repoAddObjectFromFile(USER_ADMINISTRATOR_DEPUTY_NONE_FILE, UserType.class, initResult);

		// roles
		repoAddObjectFromFile(METAROLE_CXO_FILE, RoleType.class, initResult);
		roleSuperuser = repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RoleType.class, initResult).asObjectable();
		roleCeo = repoAddObjectFromFile(ROLE_CEO_FILE, RoleType.class, initResult).asObjectable();
		roleCoo = repoAddObjectFromFile(ROLE_COO_FILE, RoleType.class, initResult).asObjectable();
		roleCto = repoAddObjectFromFile(ROLE_CTO_FILE, RoleType.class, initResult).asObjectable();
		repoAddObjectFromFile(ROLE_REVIEWER_FILE, RoleType.class, initResult).asObjectable();
		repoAddObjectFromFile(ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_FILE, RoleType.class, initResult).asObjectable();

		repoAddObjectFromFile(ORG_SECURITY_TEAM_FILE, OrgType.class, initResult).asObjectable();

		// Administrator
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, initResult).asObjectable();
		login(userAdministrator.asPrismObject());
		
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
		recomputeFocus(RoleType.class, ROLE_CEO_OID, initTask, initResult);
		recomputeFocus(RoleType.class, ROLE_COO_OID, initTask, initResult);
		recomputeFocus(RoleType.class, ROLE_CTO_OID, initTask, initResult);
		recomputeFocus(RoleType.class, ROLE_REVIEWER_OID, initTask, initResult);
		recomputeFocus(RoleType.class, ROLE_EROOT_USER_ASSIGNMENT_CAMPAIGN_OWNER_OID, initTask, initResult);
		recomputeFocus(OrgType.class, ORG_SECURITY_TEAM_OID, initTask, initResult);
	}

	protected AccessCertificationCaseType checkCase(Collection<AccessCertificationCaseType> caseList, String subjectOid, String targetOid, FocusType focus, String campaignOid) {
		AccessCertificationCaseType ccase = findCase(caseList, subjectOid, targetOid);
		assertNotNull("Certification case for " + subjectOid + ":" + targetOid + " was not found", ccase);
		assertNotNull("reviewRequestedTimestamp", ccase.getCurrentStageCreateTimestamp());
		assertNotNull("deadline", ccase.getCurrentStageDeadline());
		assertNull("remediedTimestamp", ccase.getRemediedTimestamp());
		return checkSpecificCase(ccase, focus);
	}

	protected AccessCertificationCaseType checkWorkItem(Collection<AccessCertificationWorkItemType> workItems, String subjectOid, String targetOid, FocusType focus, String campaignOid) {
		AccessCertificationWorkItemType workItem = findWorkItem(workItems, subjectOid, targetOid);
		assertNotNull("Certification work item for " + subjectOid + ":" + targetOid + " was not found", workItem);
		AccessCertificationCaseType ccase = CertCampaignTypeUtil.getCase(workItem);
		assertNotNull("No case for " + workItem, ccase);
		assertNotNull("reviewRequestedTimestamp", ccase.getCurrentStageCreateTimestamp());
		assertNotNull("deadline", ccase.getCurrentStageDeadline());
		assertNull("remediedTimestamp", ccase.getRemediedTimestamp());
		return checkSpecificCase(ccase, focus);
	}

	protected AccessCertificationCaseType checkCase(Collection<AccessCertificationCaseType> caseList, String objectOid,
													String targetOid, FocusType focus, String campaignOid,
													String tenantOid, String orgOid, ActivationStatusType administrativeStatus) {
		AccessCertificationCaseType aCase = checkCase(caseList, objectOid, targetOid, focus, campaignOid);
		String realTenantOid = aCase.getTenantRef() != null ? aCase.getTenantRef().getOid() : null;
		String realOrgOid = aCase.getOrgRef() != null ? aCase.getOrgRef().getOid() : null;
		ActivationStatusType realStatus = aCase.getActivation() != null ? aCase.getActivation().getAdministrativeStatus() : null;
		assertEquals("incorrect tenant org", tenantOid, realTenantOid);
		assertEquals("incorrect org org", orgOid, realOrgOid);
		assertEquals("incorrect admin status", administrativeStatus, realStatus);
		return aCase;
	}

	protected AccessCertificationCaseType checkWorkItem(Collection<AccessCertificationWorkItemType> workItems, String objectOid,
			String targetOid, FocusType focus, String campaignOid,
			String tenantOid, String orgOid, ActivationStatusType administrativeStatus) {
		AccessCertificationCaseType aCase = checkWorkItem(workItems, objectOid, targetOid, focus, campaignOid);
		String realTenantOid = aCase.getTenantRef() != null ? aCase.getTenantRef().getOid() : null;
		String realOrgOid = aCase.getOrgRef() != null ? aCase.getOrgRef().getOid() : null;
		ActivationStatusType realStatus = aCase.getActivation() != null ? aCase.getActivation().getAdministrativeStatus() : null;
		assertEquals("incorrect tenant org", tenantOid, realTenantOid);
		assertEquals("incorrect org org", orgOid, realOrgOid);
		assertEquals("incorrect admin status", administrativeStatus, realStatus);
		return aCase;
	}

	protected AccessCertificationCaseType checkSpecificCase(AccessCertificationCaseType ccase, FocusType focus) {
		assertEquals("Wrong class for case", AccessCertificationAssignmentCaseType.class, ccase.getClass());
		AccessCertificationAssignmentCaseType acase = (AccessCertificationAssignmentCaseType) ccase;
		long id = acase.getAssignment().getId();
		List<AssignmentType> assignmentList;
		if (Boolean.TRUE.equals(acase.isIsInducement())) {
			assignmentList = ((AbstractRoleType) focus).getInducement();
		} else {
			assignmentList = focus.getAssignment();
		}
		for (AssignmentType assignment : assignmentList) {
			if (id == assignment.getId()) {
				assertEquals("Wrong assignment in certification case", assignment, acase.getAssignment());
				return ccase;
			}
		}
		fail("Assignment with ID " + id + " not found among assignments of " + focus);
		return null;        // won't come here
	}

	protected AccessCertificationCaseType findCase(Collection<AccessCertificationCaseType> caseList, String subjectOid, String targetOid) {
		for (AccessCertificationCaseType acase : caseList) {
			if (acase.getTargetRef() != null && acase.getTargetRef().getOid().equals(targetOid) &&
					acase.getObjectRef() != null && acase.getObjectRef().getOid().equals(subjectOid)) {
				return acase;
			}
		}
		return null;
	}

	protected AccessCertificationWorkItemType findWorkItem(Collection<AccessCertificationWorkItemType> workItems, String subjectOid, String targetOid) {
		for (AccessCertificationWorkItemType workItem : workItems) {
			AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCase(workItem);
			if (aCase != null && aCase.getTargetRef() != null && aCase.getTargetRef().getOid().equals(targetOid) &&
					aCase.getObjectRef() != null && aCase.getObjectRef().getOid().equals(subjectOid)) {
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

	protected void assertAfterCampaignCreate(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition) {
		assertEquals("Unexpected certification cases", 0, campaign.getCase().size());
		assertStateAndStage(campaign, CREATED, 0);
		assertEquals("Unexpected # of stages", definition.getStageDefinition().size(), campaign.getStageDefinition().size());
		assertDefinitionAndOwner(campaign, definition);
		assertNull("Unexpected start time", campaign.getStartTimestamp());
		assertNull("Unexpected end time", campaign.getEndTimestamp());
	}
	protected void assertAfterCampaignStart(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition, int cases) throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException {
        assertStateAndStage(campaign, IN_REVIEW_STAGE, 1);
        assertDefinitionAndOwner(campaign, definition);
        assertApproximateTime("start time", new Date(), campaign.getStartTimestamp());
        assertNull("Unexpected end time", campaign.getEndTimestamp());
        assertEquals("wrong # of defined stages", definition.getStageDefinition().size(), campaign.getStageDefinition().size());
        assertEquals("wrong # of stages", 1, campaign.getStage().size());
        AccessCertificationStageType stage = campaign.getStage().get(0);
        assertEquals("wrong stage #", 1, stage.getNumber());
        assertApproximateTime("stage 1 start", new Date(), stage.getStartTimestamp());
        assertNotNull("stage 1 deadline", stage.getDeadline());       // too lazy to compute exact datetime
		assertNull("unexpected stage 1 end", stage.getEndTimestamp());
        assertEquals("Wrong number of certification cases", cases, campaign.getCase().size());

		PrismObject<AccessCertificationDefinitionType> def = getObjectViaRepo(AccessCertificationDefinitionType.class, definition.getOid());
		assertApproximateTime("last campaign started", new Date(), def.asObjectable().getLastCampaignStartedTimestamp());
		assertNull("unexpected last campaign closed", def.asObjectable().getLastCampaignClosedTimestamp());
    }

	protected void assertAfterStageOpen(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition, int stageNumber) throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException {
		assertStateAndStage(campaign, IN_REVIEW_STAGE, stageNumber);
		assertDefinitionAndOwner(campaign, definition);
		assertApproximateTime("start time", new Date(), campaign.getStartTimestamp());
		assertNull("Unexpected end time", campaign.getEndTimestamp());
		assertEquals("wrong # of defined stages", definition.getStageDefinition().size(), campaign.getStageDefinition().size());
		assertEquals("wrong # of stages", stageNumber, campaign.getStage().size());
		AccessCertificationStageType stage = CertCampaignTypeUtil.findStage(campaign, stageNumber);
		assertEquals("wrong stage #", stageNumber, stage.getNumber());
		assertApproximateTime("stage start", new Date(), stage.getStartTimestamp());
		assertNotNull("stage deadline", stage.getDeadline());       // too lazy to compute exact datetime
		assertNull("unexpected stage end", stage.getEndTimestamp());
	}


	protected void assertStateAndStage(AccessCertificationCampaignType campaign, AccessCertificationCampaignStateType state, int stage) {
		assertEquals("Unexpected campaign state", state, campaign.getState());
		assertEquals("Unexpected stage number", stage, campaign.getStageNumber());
	}

	protected void assertDefinitionAndOwner(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType certificationDefinition) {
		assertDefinitionAndOwner(campaign, certificationDefinition, getSecurityContextUserOid());
	}

	protected void assertDefinitionAndOwner(AccessCertificationCampaignType campaign,
			AccessCertificationDefinitionType certificationDefinition, String expectedOwnerOid) {
		assertRefEquals("Unexpected ownerRef", ObjectTypeUtil.createObjectRef(expectedOwnerOid, ObjectTypes.USER), campaign.getOwnerRef());
		assertRefEquals("Unexpected definitionRef",
				ObjectTypeUtil.createObjectRef(certificationDefinition),
				campaign.getDefinitionRef());
	}

	protected void assertCaseReviewers(AccessCertificationCaseType _case, AccessCertificationResponseType currentStageOutcome,
			int currentStage, List<String> reviewerOidList) {
		assertEquals("wrong current stage outcome for "+_case, OutcomeUtils.toUri(currentStageOutcome), _case.getCurrentStageOutcome());
		assertEquals("wrong current stage number for "+_case, currentStage, _case.getStageNumber());
		Set<String> realReviewerOids = CertCampaignTypeUtil.getCurrentReviewers(_case).stream().map(ref -> ref.getOid()).collect(Collectors.toSet());
		assertEquals("wrong reviewer oids for "+_case, new HashSet<>(reviewerOidList), realReviewerOids);
	}

	protected void recordDecision(String campaignOid, AccessCertificationCaseType _case, AccessCertificationResponseType response,
			String comment, String reviewerOid, Task task, OperationResult result)
			throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, SecurityViolationException, ConfigurationException {
		Authentication originalAuthentication = null;
		String realReviewerOid;
		if (reviewerOid != null) {
			originalAuthentication = SecurityContextHolder.getContext().getAuthentication();
			login(getUser(reviewerOid));
			realReviewerOid = reviewerOid;
		} else {
			realReviewerOid = securityEnforcer.getPrincipal().getOid();
		}
		List<AccessCertificationWorkItemType> workItems = _case.getWorkItem().stream()
				.filter(wi -> ObjectTypeUtil.containsOid(wi.getAssigneeRef(), realReviewerOid))
				.filter(wi -> wi.getStageNumber() == _case.getStageNumber())
				.collect(Collectors.toList());
		assertEquals("Wrong # of current work items for " + realReviewerOid + " in " + _case, 1, workItems.size());
		long id = _case.asPrismContainerValue().getId();
		certificationManager.recordDecision(campaignOid, id, workItems.get(0).getId(), response, comment, task, result);
		if (reviewerOid != null) {
			SecurityContextHolder.getContext().setAuthentication(originalAuthentication);
		}
	}

	// TODO remove redundant check on outcomes (see assertCaseOutcome)
	protected void assertSingleDecision(AccessCertificationCaseType _case, AccessCertificationResponseType response, String comment,
			int stageNumber, String reviewerOid, AccessCertificationResponseType currentStageOutcome, boolean checkHistory) {
		List<AccessCertificationWorkItemType> currentWorkItems = getCurrentWorkItems(_case, stageNumber, false);
		assertEquals("wrong # of decisions for stage " + stageNumber + " for case #" + _case.getId(), 1, currentWorkItems.size());
		AccessCertificationWorkItemType workItem = currentWorkItems.get(0);
		assertEquals("wrong response", response, OutcomeUtils.fromUri(WorkItemTypeUtil.getOutcome(workItem)));
		assertEquals("wrong comment", comment, WorkItemTypeUtil.getComment(workItem));
		assertEquals("Wrong # of reviewers", 1, workItem.getAssigneeRef().size());
		assertRefEquals("wrong reviewerRef", ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER), workItem.getAssigneeRef().get(0));
		assertEquals("wrong stage number", (Integer) stageNumber, workItem.getStageNumber());
		if (response != null) {
			assertApproximateTime("timestamp", new Date(), workItem.getOutputChangeTimestamp());
		}
		assertEquals("wrong current stage outcome", OutcomeUtils.toUri(currentStageOutcome), _case.getCurrentStageOutcome());
		if (checkHistory) {
			assertHistoricOutcome(_case, stageNumber, currentStageOutcome);
		}
	}

	protected void assertReviewerDecision(AccessCertificationCaseType _case, AccessCertificationResponseType response, String comment,
										int stageNumber, String reviewerOid, AccessCertificationResponseType currentStageOutcome, boolean checkHistory) {
		AccessCertificationWorkItemType workItem = getWorkItemsForReviewer(_case, stageNumber, reviewerOid);
		assertNotNull("No work item for reviewer " + reviewerOid + " in stage " + stageNumber, workItem);
		assertEquals("wrong response", response, OutcomeUtils.fromUri(WorkItemTypeUtil.getOutcome(workItem)));
		assertEquals("wrong comment", comment, WorkItemTypeUtil.getComment(workItem));
		if (response != null) {
			assertApproximateTime("timestamp", new Date(), workItem.getOutputChangeTimestamp());
		}
		assertEquals("wrong current stage outcome", OutcomeUtils.toUri(currentStageOutcome), _case.getCurrentStageOutcome());
		if (checkHistory) {
			assertHistoricOutcome(_case, stageNumber, currentStageOutcome);
		}
	}

	protected void assertHistoricOutcome(AccessCertificationCaseType aCase, int stageNumber, AccessCertificationResponseType outcome) {
		boolean found = false;
		for (CaseEventType event : aCase.getEvent()) {
			if (!(event instanceof StageCompletionEventType)) {
				continue;
			}
			StageCompletionEventType completionEvent = (StageCompletionEventType) event;
			if (completionEvent.getStageNumber() == stageNumber) {
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
		for (int stage = 0; stage < outcomes.length; stage++) {
			assertHistoricOutcome(aCase, stage+1, outcomes[stage]);
		}
		assertEquals("wrong # of stored stage outcomes", outcomes.length, CertCampaignTypeUtil.getCompletedStageEvents(aCase).size());
	}


	// we return also closed ones (TODO: what is meant by 'current' work items?)
	public List<AccessCertificationWorkItemType> getCurrentWorkItems(AccessCertificationCaseType _case, int stageNumber, boolean decidedOnly) {
		List<AccessCertificationWorkItemType> rv = new ArrayList<>();
		for (AccessCertificationWorkItemType workItem : _case.getWorkItem()) {
			if (decidedOnly && WorkItemTypeUtil.getOutcome(workItem) == null) {
				continue;
			}
			if (workItem.getStageNumber() == stageNumber) {
				rv.add(workItem.clone());
			}
		}
		return rv;
	}

	public AccessCertificationWorkItemType getWorkItemsForReviewer(AccessCertificationCaseType _case, int stageNumber, String reviewerOid) {
		for (AccessCertificationWorkItemType workItem : _case.getWorkItem()) {
			if (workItem.getStageNumber() == stageNumber && ObjectTypeUtil.containsOid(workItem.getAssigneeRef(), reviewerOid)) {
				return workItem;
			}
		}
		return null;
	}

	protected void assertNoDecision(AccessCertificationCaseType _case, int stage, AccessCertificationResponseType aggregatedResponse, boolean checkHistory) {
		List<AccessCertificationWorkItemType> currentWorkItems = getCurrentWorkItems(_case, stage, true);
		assertEquals("wrong # of decisions", 0, currentWorkItems.size());
		assertEquals("wrong current response", OutcomeUtils.toUri(aggregatedResponse), _case.getCurrentStageOutcome());
		if (checkHistory) {
			assertHistoricOutcome(_case, stage, aggregatedResponse);
		}
	}

	protected void assertCurrentState(AccessCertificationCaseType _case, AccessCertificationResponseType aggregatedResponse, int currentResponseStage) {
		assertEquals("wrong current response", OutcomeUtils.toUri(aggregatedResponse), _case.getCurrentStageOutcome());
		assertEquals("wrong current response stage number", currentResponseStage, _case.getStageNumber());
	}

	protected void assertWorkItems(AccessCertificationCaseType _case, int count) {
		assertEquals("Wrong # of work items", count, _case.getWorkItem().size());
	}

	protected void assertDecision2(AccessCertificationCaseType _case, AccessCertificationResponseType response, String comment,
								   int stageNumber, String reviewerOid, AccessCertificationResponseType aggregatedResponse) {
		AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(_case, stageNumber, reviewerOid);
		assertNotNull("decision does not exist", workItem);
		assertEquals("wrong response", response, OutcomeUtils.fromUri(WorkItemTypeUtil.getOutcome(workItem)));
		assertEquals("wrong comment", comment, WorkItemTypeUtil.getComment(workItem));
		if (response != null) {
			assertApproximateTime("timestamp", new Date(), workItem.getOutputChangeTimestamp());
		}
		assertEquals("wrong current response", OutcomeUtils.toUri(aggregatedResponse), _case.getCurrentStageOutcome());
	}

	protected AccessCertificationCampaignType getCampaignWithCases(String campaignOid) throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractModelIntegrationTest.class.getName() + ".getObject");
		OperationResult result = task.getResult();
		Collection<SelectorOptions<GetOperationOptions>> options =
				Arrays.asList(SelectorOptions.create(F_CASE, GetOperationOptions.createRetrieve(INCLUDE)));
		AccessCertificationCampaignType campaign = modelService.getObject(AccessCertificationCampaignType.class, campaignOid, options, task, result).asObjectable();
		result.computeStatus();
		TestUtil.assertSuccess(result);
		return campaign;
	}


	protected void assertAfterStageClose(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition, int stageNumber) {
        assertStateAndStage(campaign, REVIEW_STAGE_DONE, stageNumber);
        assertDefinitionAndOwner(campaign, definition);
        assertNull("Unexpected end time", campaign.getEndTimestamp());
        assertEquals("wrong # of stages", stageNumber, campaign.getStage().size());
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        assertEquals("wrong stage #", stageNumber, stage.getNumber());
        assertApproximateTime("stage 1 start", new Date(), stage.getStartTimestamp());
        assertApproximateTime("stage 1 end", new Date(), stage.getStartTimestamp());

		for (AccessCertificationCaseType aCase : campaign.getCase()) {
			if (aCase.getStageNumber() != stageNumber) {
				continue;
			}
			checkCaseOutcomes(aCase, campaign, stageNumber);
		}
    }

	private void checkCaseOutcomes(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, int stageNumber) {
		List<AccessCertificationResponseType> stageOutcomes = new ArrayList<>(stageNumber);
		for (int i = 1; i <= stageNumber; i++) {
			stageOutcomes.add(checkCaseStageOutcome(aCase, stageNumber));
		}
		assertEquals("Wrong # of completed stage outcomes", stageNumber, CertCampaignTypeUtil.getCompletedStageEvents(aCase).size());
		AccessCertificationResponseType expectedOverall = computationHelper.computeOverallOutcome(aCase, campaign);
		assertEquals("Inconsistent overall outcome", OutcomeUtils.toUri(expectedOverall), aCase.getOutcome());
	}

	private AccessCertificationResponseType checkCaseStageOutcome(AccessCertificationCaseType aCase, int stageNumber) {
		return OutcomeUtils.fromUri(CertCampaignTypeUtil.getStageOutcome(aCase, stageNumber));
	}

	// completedStage - if null, checks the stage outcome in the history list
	protected void assertCaseOutcome(List<AccessCertificationCaseType> caseList, String subjectOid, String targetOid,
									 AccessCertificationResponseType stageOutcome, AccessCertificationResponseType overallOutcome, Integer completedStage) {
        AccessCertificationCaseType ccase = findCase(caseList, subjectOid, targetOid);
        assertEquals("Wrong stage outcome in " + ccase, OutcomeUtils.toUri(stageOutcome), ccase.getCurrentStageOutcome());
        assertEquals("Wrong overall outcome in " + ccase, OutcomeUtils.toUri(overallOutcome), ccase.getOutcome());

		if (completedStage != null) {
			assertHistoricOutcome(ccase, completedStage, stageOutcome);
		}
    }

	protected void assertPercentComplete(String campaignOid, int expCasesComplete, int expCasesDecided, int expDecisionsDone) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
		assertPercentComplete(campaign, expCasesComplete, expCasesDecided, expDecisionsDone);
	}

	protected void assertPercentComplete(AccessCertificationCampaignType campaign, int expCasesComplete, int expCasesDecided, int expDecisionsDone) {
		int casesCompletePercentage = Math.round(CertCampaignTypeUtil.getCasesCompletedPercentage(campaign));
		System.out.println("Cases completed = " + casesCompletePercentage + " %");
		assertEquals("Wrong case complete percentage", expCasesComplete, casesCompletePercentage);

		int casesDecidedPercentage = Math.round(CertCampaignTypeUtil.getCasesDecidedPercentage(campaign));
		System.out.println("Cases decided = " + casesDecidedPercentage + " %");
		assertEquals("Wrong case complete percentage", expCasesDecided, casesDecidedPercentage);

		int decisionsDonePercentage = Math.round(CertCampaignTypeUtil.getDecisionsDonePercentage(campaign));
		System.out.println("Decisions completed = " + decisionsDonePercentage + " %");
		assertEquals("Wrong decisions complete percentage", expDecisionsDone, decisionsDonePercentage);
    }

	public void reimportTriggerTask(OperationResult result) throws FileNotFoundException {
		taskManager.suspendAndDeleteTasks(Collections.singletonList(TASK_TRIGGER_SCANNER_OID), 60000L, true, result);
		importObjectFromFile(TASK_TRIGGER_SCANNER_FILE, result);
	}

	public void importTriggerTask(OperationResult result) throws FileNotFoundException {
		importObjectFromFile(TASK_TRIGGER_SCANNER_FILE, result);
	}
}
