/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class AbstractInitializedModelIntegrationTest extends AbstractConfiguredModelIntegrationTest {
	
	private static final Object NUM_FUNCTIONAL_ORGS = 6;
	private static final Object NUM_PROJECT_ORGS = 3;
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractInitializedModelIntegrationTest.class);
	
	protected UserType userTypeJack;
	protected UserType userTypeBarbossa;
	protected UserType userTypeGuybrush;
	protected UserType userTypeElaine;
	
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;
	
	protected static DummyResource dummyResource;
	protected static DummyResourceContoller dummyResourceCtl;
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;
	
	protected static DummyResource dummyResourceRed;
	protected static DummyResourceContoller dummyResourceCtlRed;
	protected ResourceType resourceDummyRedType;
	protected PrismObject<ResourceType> resourceDummyRed;
	
	protected static DummyResource dummyResourceBlue;
	protected static DummyResourceContoller dummyResourceCtlBlue;
	protected ResourceType resourceDummyBlueType;
	protected PrismObject<ResourceType> resourceDummyBlue;
	
	protected static DummyResource dummyResourceWhite;
	protected static DummyResourceContoller dummyResourceCtlWhite;
	protected ResourceType resourceDummyWhiteType;
	protected PrismObject<ResourceType> resourceDummyWhite;

    protected static DummyResource dummyResourceYellow;
    protected static DummyResourceContoller dummyResourceCtlYellow;
    protected ResourceType resourceDummyYellowType;
    protected PrismObject<ResourceType> resourceDummyYellow;

    protected static DummyResource dummyResourceGreen;
	protected static DummyResourceContoller dummyResourceCtlGreen;
	protected ResourceType resourceDummyGreenType;
	protected PrismObject<ResourceType> resourceDummyGreen;
	
	protected ResourceType resourceDummySchemalessType;
	protected PrismObject<ResourceType> resourceDummySchemaless;
	
	public AbstractInitializedModelIntegrationTest() {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		
		// Resources
		resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILENAME, RESOURCE_OPENDJ_OID, initTask, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		openDJController.setResource(resourceOpenDj);
		if (!openDJController.isRunning()) {
			initResult.muteLastSubresultError();
		}
				
		dummyResourceCtl = DummyResourceContoller.create(null);
		dummyResourceCtl.extendDummySchema();
		dummyResource = dummyResourceCtl.getDummyResource();
		dummyResourceCtl.addAttrDef(dummyResource.getAccountObjectClass(),
				DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, String.class, false, false);
		resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILENAME, RESOURCE_DUMMY_OID, initTask, initResult);
		resourceDummyType = resourceDummy.asObjectable();
		dummyResourceCtl.setResource(resourceDummy);
		
		dummyResourceCtlRed = DummyResourceContoller.create(RESOURCE_DUMMY_RED_NAME, resourceDummyRed);
		dummyResourceCtlRed.extendDummySchema();
		dummyResourceRed = dummyResourceCtlRed.getDummyResource();
		resourceDummyRed = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_RED_FILENAME, RESOURCE_DUMMY_RED_OID, initTask, initResult); 
		resourceDummyRedType = resourceDummyRed.asObjectable();
		dummyResourceCtlRed.setResource(resourceDummyRed);
		
		dummyResourceCtlBlue = DummyResourceContoller.create(RESOURCE_DUMMY_BLUE_NAME, resourceDummyBlue);
		dummyResourceCtlBlue.extendDummySchema();
		dummyResourceBlue = dummyResourceCtlBlue.getDummyResource();
		resourceDummyBlue = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_BLUE_FILENAME, RESOURCE_DUMMY_BLUE_OID, initTask, initResult); 
		resourceDummyBlueType = resourceDummyBlue.asObjectable();
		dummyResourceCtlBlue.setResource(resourceDummyBlue);		
		
		dummyResourceCtlWhite = DummyResourceContoller.create(RESOURCE_DUMMY_WHITE_NAME, resourceDummyWhite);
		dummyResourceCtlWhite.extendDummySchema();
		dummyResourceWhite = dummyResourceCtlWhite.getDummyResource();
		resourceDummyWhite = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_WHITE_FILENAME, RESOURCE_DUMMY_WHITE_OID, initTask, initResult);
		resourceDummyWhiteType = resourceDummyWhite.asObjectable();
		dummyResourceCtlWhite.setResource(resourceDummyWhite);

        dummyResourceCtlYellow = DummyResourceContoller.create(RESOURCE_DUMMY_YELLOW_NAME, resourceDummyYellow);
        dummyResourceCtlYellow.extendDummySchema();
        dummyResourceYellow = dummyResourceCtlYellow.getDummyResource();
        resourceDummyYellow = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_YELLOW_FILENAME, RESOURCE_DUMMY_YELLOW_OID, initTask, initResult);
        resourceDummyYellowType = resourceDummyYellow.asObjectable();
        dummyResourceCtlYellow.setResource(resourceDummyYellow);

        dummyResourceCtlGreen = DummyResourceContoller.create(RESOURCE_DUMMY_GREEN_NAME, resourceDummyGreen);
		dummyResourceCtlGreen.extendDummySchema();
		dummyResourceGreen = dummyResourceCtlGreen.getDummyResource();
		resourceDummyGreen = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_GREEN_FILENAME, RESOURCE_DUMMY_GREEN_OID, initTask, initResult);
		resourceDummyGreenType = resourceDummyGreen.asObjectable();
		dummyResourceCtlGreen.setResource(resourceDummyGreen);
		
		resourceDummySchemaless = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_SCHEMALESS_FILENAME, RESOURCE_DUMMY_SCHEMALESS_OID, initTask, initResult); 
		resourceDummySchemalessType = resourceDummySchemaless.asObjectable();

		
		postInitDummyResouce();
		
		dummyResourceCtl.addAccount(ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", "Monkey Island");
		dummyResourceCtl.addAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Melee Island");
		dummyResourceCtl.addAccount(ACCOUNT_DAVIEJONES_DUMMY_USERNAME, "Davie Jones", "Davie Jones' Locker");
		dummyResourceCtl.addAccount(ACCOUNT_CALYPSO_DUMMY_USERNAME, "Tia Dalma", "Pantano River");
		
		dummyResourceCtl.addAccount(ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", "Melee Island");
		dummyResourceCtlRed.addAccount(ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", "Melee Island");
		dummyResourceCtlBlue.addAccount(ACCOUNT_ELAINE_DUMMY_USERNAME, "Elaine Marley", "Melee Island");
		
		// User Templates
		repoAddObjectFromFile(USER_TEMPLATE_FILENAME, ObjectTemplateType.class, initResult);
		repoAddObjectFromFile(USER_TEMPLATE_COMPLEX_FILENAME, ObjectTemplateType.class, initResult);
		repoAddObjectFromFile(USER_TEMPLATE_COMPLEX_INCLUDE_FILENAME, ObjectTemplateType.class, initResult);

		// Accounts
		repoAddObjectFromFile(ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME, ShadowType.class, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_FILENAME, ShadowType.class, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_FILENAME, ShadowType.class, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_FILENAME, ShadowType.class, initResult);
		
		// Users
		userTypeJack = repoAddObjectFromFile(USER_JACK_FILENAME, UserType.class, initResult).asObjectable();
		userTypeBarbossa = repoAddObjectFromFile(USER_BARBOSSA_FILENAME, UserType.class, initResult).asObjectable();
		userTypeGuybrush = repoAddObjectFromFile(USER_GUYBRUSH_FILENAME, UserType.class, initResult).asObjectable();
		userTypeElaine = repoAddObjectFromFile(USER_ELAINE_FILENAME, UserType.class, initResult).asObjectable();
		
		// Roles
		repoAddObjectFromFile(ROLE_PIRATE_FILENAME, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_NICE_PIRATE_FILENAME, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_CAPTAIN_FILENAME, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_JUDGE_FILENAME, RoleType.class, initResult);
		
		// Orgstruct
		repoAddObjectsFromFile(ORG_MONKEY_ISLAND_FILENAME, OrgType.class, initResult);

	}
	
	protected void postInitDummyResouce() {
		// Do nothing be default. Concrete tests may override this.
	}

	
		
	protected void assertUserJack(PrismObject<UserType> user) {
		assertUserJack(user, "Jack Sparrow", "Jack", "Sparrow");
	}
	
	protected void assertUserJack(PrismObject<UserType> user, String fullName) {
		assertUserJack(user, fullName, "Jack", "Sparrow");
	}
	
	protected void assertUserJack(PrismObject<UserType> user, String fullName, String givenName, String familyName) {
		assertUserJack(user, fullName, givenName, familyName, "Caribbean");
	}
	
	protected void assertUserJack(PrismObject<UserType> user, String name, String fullName, String givenName, String familyName, String locality) {
		assertUser(user, USER_JACK_OID, name, fullName, givenName, familyName, locality);
		UserType userType = user.asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong jack honorificPrefix", "Cpt.", userType.getHonorificPrefix());
		PrismAsserts.assertEqualsPolyString("Wrong jack honorificSuffix", "PhD.", userType.getHonorificSuffix());
		assertEquals("Wrong jack emailAddress", "jack.sparrow@evolveum.com", userType.getEmailAddress());
		assertEquals("Wrong jack telephoneNumber", "555-1234", userType.getTelephoneNumber());
		assertEquals("Wrong jack employeeNumber", "emp1234", userType.getEmployeeNumber());
		assertEquals("Wrong jack employeeType", "CAPTAIN", userType.getEmployeeType().get(0));
		if (locality == null) {
			assertNull("Locality sneaked to user jack", userType.getLocality());
		} else {
			PrismAsserts.assertEqualsPolyString("Wrong jack locality", locality, userType.getLocality());
		}
	}
	protected void assertUserJack(PrismObject<UserType> user, String fullName, String givenName, String familyName, String locality) {
		assertUserJack(user, "jack", fullName, givenName, familyName, locality);
	}
	
	protected void assertDummyShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username) {
		assertShadowRepo(accountShadow, oid, username, resourceDummyType);
	}
	
	protected void assertDummyShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username, String fullname) {
		assertShadowModel(accountShadow, oid, username, resourceDummyType);
		IntegrationTestTools.assertAttribute(accountShadow, dummyResourceCtl.getAttributeFullnameQName(), fullname);
	}
		
	protected void setDefaultUserTemplate(String userTemplateOid)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		PrismObjectDefinition<SystemConfigurationType> objectDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(SystemConfigurationType.class);

		PrismReferenceValue userTemplateRefVal = new PrismReferenceValue(userTemplateOid);
		
		Collection<? extends ItemDelta> modifications = ReferenceDelta.createModificationReplaceCollection(
						SystemConfigurationType.F_DEFAULT_USER_TEMPLATE_REF,
						objectDefinition, userTemplateRefVal);

		OperationResult result = new OperationResult("Aplying default user template");

		repositoryService.modifyObject(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, result);
		display("Aplying default user template result", result);
		result.computeStatus();
		assertSuccess("Aplying default user template failed (result)", result);
	}

	protected void assertMonkeyIslandOrgSanity() throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractInitializedModelIntegrationTest.class.getName() + ".assertMonkeyIslandOrgSanity");
        OperationResult result = task.getResult();
        
        PrismObject<OrgType> orgGovernorOffice = modelService.getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID, null, task, result);
        result.computeStatus();
        assertSuccess(result);
        OrgType orgGovernorOfficeType = orgGovernorOffice.asObjectable();
        assertEquals("Wrong governor office name", PrismTestUtil.createPolyStringType("F0001"), orgGovernorOfficeType.getName());
        
        List<PrismObject<OrgType>> governorSubOrgs = searchOrg(ORG_GOVERNOR_OFFICE_OID, null, 1, task, result);
        if (verbose) display("governor suborgs", governorSubOrgs);
        assertEquals("Unexpected number of governor suborgs", 3, governorSubOrgs.size());
        
        List<PrismObject<OrgType>> functionalOrgs = searchOrg(ORG_GOVERNOR_OFFICE_OID, null, null, task, result);
        if (verbose) display("functional orgs (null)", functionalOrgs);
        assertEquals("Unexpected number of functional orgs (null)", NUM_FUNCTIONAL_ORGS, functionalOrgs.size());
        
        functionalOrgs = searchOrg(ORG_GOVERNOR_OFFICE_OID, null, -1, task, result);
        if (verbose) display("functional orgs (-1)", functionalOrgs);
        assertEquals("Unexpected number of functional orgs (-1)", NUM_FUNCTIONAL_ORGS, functionalOrgs.size());
        
        List<PrismObject<OrgType>> prootSubOrgs = searchOrg(ORG_PROJECT_ROOT_OID, null, 1, task, result);
        if (verbose) display("project root suborgs", prootSubOrgs);
        assertEquals("Unexpected number of governor suborgs", 2, prootSubOrgs.size());
        
        List<PrismObject<OrgType>> projectOrgs = searchOrg(ORG_PROJECT_ROOT_OID, null, null, task, result);
        if (verbose) display("project orgs (null)", projectOrgs);
        assertEquals("Unexpected number of functional orgs (null)", NUM_PROJECT_ORGS, projectOrgs.size());
        
        projectOrgs = searchOrg(ORG_PROJECT_ROOT_OID, null, -1, task, result);
        if (verbose) display("project orgs (-1)", projectOrgs);
        assertEquals("Unexpected number of functional orgs (-1)", NUM_PROJECT_ORGS, projectOrgs.size());
        
        PrismObject<OrgType> orgScummBar = modelService.getObject(OrgType.class, ORG_SCUMM_BAR_OID, null, task, result);
        List<AssignmentType> scummBarInducements = orgScummBar.asObjectable().getInducement();
        assertEquals("Unexpected number of scumm bar inducements: "+scummBarInducements,  1, scummBarInducements.size());
	}
     	
}
