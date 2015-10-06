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
import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.certification.impl.AccCertGeneralHelper;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * @author mederly
 *
 */
public class AbstractCertificationTest extends AbstractModelIntegrationTest {
	
	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	protected static final File ORGS_AND_USERS_FILE = new File(COMMON_DIR, "orgs-and-users.xml");

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

	public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_NAME = "administrator";

	protected static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	protected static final String USER_JACK_USERNAME = "jack";
	
	public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

	public static final File METAROLE_CXO_FILE = new File(COMMON_DIR, "metarole-cxo.xml");
	protected static final String METAROLE_CXO_OID = "00000000-d34d-b33f-f00d-444444444444";

	public static final File ROLE_CEO_FILE = new File(COMMON_DIR, "role-ceo.xml");
	protected static final String ROLE_CEO_OID = "00000000-d34d-b33f-f00d-000000000001";

	public static final File ROLE_COO_FILE = new File(COMMON_DIR, "role-coo.xml");
	protected static final String ROLE_COO_OID = "00000000-d34d-b33f-f00d-000000000002";

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

    @Autowired
    protected CertificationManager certificationManager;

	@Autowired
	protected AccCertGeneralHelper helper;

	protected RoleType roleCeo;
	protected RoleType roleCoo;
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
			repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}

		repoAddObjectsFromFile(ORGS_AND_USERS_FILE, RoleType.class, initResult);

		// roles
		repoAddObjectFromFile(METAROLE_CXO_FILE, RoleType.class, initResult);
		roleSuperuser = repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RoleType.class, initResult).asObjectable();
		roleCeo = repoAddObjectFromFile(ROLE_CEO_FILE, RoleType.class, initResult).asObjectable();
		roleCoo = repoAddObjectFromFile(ROLE_COO_FILE, RoleType.class, initResult).asObjectable();

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

	}

	protected AccessCertificationCaseType checkCase(Collection<AccessCertificationCaseType> caseList, String subjectOid, String targetOid, FocusType focus) {
		AccessCertificationCaseType ccase = findCase(caseList, subjectOid, targetOid);
		assertNotNull("Certification case for " + subjectOid + ":" + targetOid + " was not found", ccase);
		assertNotNull("reviewRequestedTimestamp", ccase.getReviewRequestedTimestamp());
		assertNotNull("deadline", ccase.getReviewDeadline());
		assertNull("remediedTimestamp", ccase.getRemediedTimestamp());
		return checkSpecificCase(ccase, focus);
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

	protected void assertApproximateTime(String itemName, Date expected, XMLGregorianCalendar actual) {
        assertNotNull("missing " + itemName, actual);
        Date actualAsDate = XmlTypeConverter.toDate(actual);
        assertTrue(itemName + " out of range; expected " + expected + ", found " + actualAsDate,
				Math.abs(actualAsDate.getTime() - expected.getTime()) < 600000);     // 10 minutes
    }

	protected void assertStateAndStage(AccessCertificationCampaignType campaign, AccessCertificationCampaignStateType state, int stage) {
		assertEquals("Unexpected campaign state", state, campaign.getState());
		assertEquals("Unexpected stage number", stage, campaign.getCurrentStageNumber());
	}

	protected void assertDefinitionAndOwner(AccessCertificationCampaignType campaign, AccessCertificationDefinitionType certificationDefinition) {
		assertRefEquals("Unexpected ownerRef", ObjectTypeUtil.createObjectRef(USER_ADMINISTRATOR_OID, ObjectTypes.USER), campaign.getOwnerRef());
		assertRefEquals("Unexpected definitionRef",
				ObjectTypeUtil.createObjectRef(certificationDefinition),
				campaign.getDefinitionRef());
	}

	protected void assertCaseReviewers(AccessCertificationCaseType _case, AccessCertificationResponseType currentResponse,
									 boolean enabled, int currentResponseStage, List<String> reviewerOidList) {
		assertEquals("wrong current response", currentResponse, _case.getCurrentResponse());
		assertEquals("wrong enabled", (Boolean) enabled, _case.isEnabled());
		assertEquals("wrong current response stage number", (Integer) currentResponseStage, _case.getCurrentResponseStage());
		Set<String> realReviewerOids = new HashSet<>();
		for (ObjectReferenceType ref : _case.getReviewerRef()) {
			realReviewerOids.add(ref.getOid());
		}
		assertEquals("wrong reviewer oids", new HashSet<>(reviewerOidList), realReviewerOids);
	}

	protected void recordDecision(String campaignOid, AccessCertificationCaseType _case, AccessCertificationResponseType response, String comment,
								  int stageNumber, String reviewerOid, Task task, OperationResult result)
			throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, SecurityViolationException, ConfigurationException {
		AccessCertificationDecisionType decision = new AccessCertificationDecisionType(prismContext);
		decision.setResponse(response);
		decision.setComment(comment);
		decision.setStageNumber(stageNumber);
		if (reviewerOid != null) {
			ObjectReferenceType reviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER);
			decision.setReviewerRef(reviewerRef);
		}
		long id = _case.asPrismContainerValue().getId();
		certificationManager.recordDecision(campaignOid, id, decision, task, result);
	}

	protected void assertDecision(AccessCertificationCaseType _case, AccessCertificationResponseType response, String comment, int stageNumber, String reviewerOid, AccessCertificationResponseType aggregatedResponse, boolean enabled) {
		assertEquals("wrong # of decisions", 1, _case.getDecision().size());
		AccessCertificationDecisionType storedDecision = _case.getDecision().get(0);
		assertEquals("wrong response", response, storedDecision.getResponse());
		assertEquals("wrong comment", comment, storedDecision.getComment());
		assertRefEquals("wrong reviewerRef", ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER), storedDecision.getReviewerRef());
		assertEquals("wrong stage number", stageNumber, storedDecision.getStageNumber());
		assertApproximateTime("timestamp", new Date(), storedDecision.getTimestamp());
		assertEquals("wrong current response", aggregatedResponse, _case.getCurrentResponse());
		assertEquals("wrong enabled", (Boolean) enabled, _case.isEnabled());
	}

	protected void assertNoDecision(AccessCertificationCaseType _case, AccessCertificationResponseType aggregatedResponse, boolean enabled) {
		assertEquals("wrong # of decisions", 0, _case.getDecision().size());
		assertEquals("wrong current response", aggregatedResponse, _case.getCurrentResponse());
		assertEquals("wrong enabled", (Boolean) enabled, _case.isEnabled());
	}

	protected void assertCurrentState(AccessCertificationCaseType _case, AccessCertificationResponseType aggregatedResponse, int currentResponseStage, boolean enabled) {
		assertEquals("wrong current response", aggregatedResponse, _case.getCurrentResponse());
		assertEquals("wrong current response stage number", (Integer) currentResponseStage, _case.getCurrentResponseStage());
		assertEquals("wrong enabled", (Boolean) enabled, _case.isEnabled());
	}

	protected void assertDecisions(AccessCertificationCaseType _case, int count) {
		assertEquals("Wrong # of decisions", count, _case.getDecision().size());
	}

	protected void assertDecision2(AccessCertificationCaseType _case, AccessCertificationResponseType response, String comment,
								 int stageNumber, String reviewerOid, AccessCertificationResponseType aggregatedResponse, boolean enabled) {
		AccessCertificationDecisionType decision = CertCampaignTypeUtil.findDecision(_case, stageNumber, reviewerOid);
		assertNotNull("decision does not exist", decision);
		assertEquals("wrong response", response, decision.getResponse());
		assertEquals("wrong comment", comment, decision.getComment());
		assertApproximateTime("timestamp", new Date(), decision.getTimestamp());
		assertEquals("wrong current response", aggregatedResponse, _case.getCurrentResponse());
		assertEquals("wrong enabled", (Boolean) enabled, _case.isEnabled());
	}

}
