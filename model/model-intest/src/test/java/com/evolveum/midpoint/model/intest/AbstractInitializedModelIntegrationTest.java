/*
 * Copyright (c) 2010-2016 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.intest.util.CheckingProgressListener;
import com.evolveum.midpoint.model.intest.util.ProfilingLensDebugListener;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author semancik
 *
 */
public class AbstractInitializedModelIntegrationTest extends AbstractConfiguredModelIntegrationTest {
	
	private static final int NUM_FUNCTIONAL_ORGS = 6;
	private static final int NUM_PROJECT_ORGS = 3;
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractInitializedModelIntegrationTest.class);
	
	@Autowired(required = true)
	protected MappingFactory mappingFactory;
	
	@Autowired(required = true)
	protected Clockwork clockwork;
	
	protected ProfilingLensDebugListener lensDebugListener;
	protected CheckingProgressListener checkingProgressListener;
	
	protected UserType userTypeJack;
	protected UserType userTypeBarbossa;
	protected UserType userTypeGuybrush;
	protected UserType userTypeElaine;
	
	protected DummyResourceContoller dummyResourceCtl;	
	protected DummyResourceContoller dummyResourceCtlRed;
	
	protected DummyResource dummyResourceBlue;
	protected DummyResourceContoller dummyResourceCtlBlue;
	protected ResourceType resourceDummyBlueType;
	protected PrismObject<ResourceType> resourceDummyBlue;

	protected DummyResource dummyResourceCyan;
	protected DummyResourceContoller dummyResourceCtlCyan;
	protected ResourceType resourceDummyCyanType;
	protected PrismObject<ResourceType> resourceDummyCyan;

	protected DummyResource dummyResourceWhite;
	protected DummyResourceContoller dummyResourceCtlWhite;
	protected ResourceType resourceDummyWhiteType;
	protected PrismObject<ResourceType> resourceDummyWhite;

    protected DummyResource dummyResourceGreen;
	protected DummyResourceContoller dummyResourceCtlGreen;
	protected ResourceType resourceDummyGreenType;
	protected PrismObject<ResourceType> resourceDummyGreen;
	
	protected static DummyResource dummyResourceEmerald;
	protected static DummyResourceContoller dummyResourceCtlEmerald;
	protected ResourceType resourceDummyEmeraldType;
	protected PrismObject<ResourceType> resourceDummyEmerald;
	
	protected DummyResource dummyResourceOrange;
	protected DummyResourceContoller dummyResourceCtlOrange;
	protected ResourceType resourceDummyOrangeType;
	protected PrismObject<ResourceType> resourceDummyOrange;

	protected DummyResource dummyResourceUpcase;
	protected DummyResourceContoller dummyResourceCtlUpcase;
	protected ResourceType resourceDummyUpcaseType;
	protected PrismObject<ResourceType> resourceDummyUpcase;

	protected ResourceType resourceDummySchemalessType;
	protected PrismObject<ResourceType> resourceDummySchemaless;
	
	public AbstractInitializedModelIntegrationTest() {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		
		mappingFactory.setProfiling(true);
		lensDebugListener = new ProfilingLensDebugListener();
		clockwork.setDebugListener(lensDebugListener);
		checkingProgressListener = new CheckingProgressListener();
		
		// Resources
				
		dummyResourceCtl = initDummyResource(null, getResourceDummyFile(), RESOURCE_DUMMY_OID, 
				controller -> {
					controller.extendSchemaPirate();
					controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
							DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME, String.class, false, false);
				},
				initTask, initResult);
		
		dummyResourceCtlRed = initDummyResourcePirate(RESOURCE_DUMMY_RED_NAME, 
				RESOURCE_DUMMY_RED_FILE, RESOURCE_DUMMY_RED_OID, initTask, initResult);
		
		initDummyResourcePirate(RESOURCE_DUMMY_YELLOW_NAME, 
				RESOURCE_DUMMY_YELLOW_FILE, RESOURCE_DUMMY_YELLOW_OID, initTask, initResult);
		
		initDummyResourcePirate(RESOURCE_DUMMY_BLACK_NAME, 
				RESOURCE_DUMMY_BLACK_FILE, RESOURCE_DUMMY_BLACK_OID, initTask, initResult);
		
		dummyResourceCtlBlue = DummyResourceContoller.create(RESOURCE_DUMMY_BLUE_NAME, resourceDummyBlue);
		dummyResourceCtlBlue.extendSchemaPirate();
		dummyResourceBlue = dummyResourceCtlBlue.getDummyResource();
		resourceDummyBlue = importAndGetObjectFromFile(ResourceType.class, getResourceDummyBlueFile(), RESOURCE_DUMMY_BLUE_OID, initTask, initResult); 
		resourceDummyBlueType = resourceDummyBlue.asObjectable();
		dummyResourceCtlBlue.setResource(resourceDummyBlue);
		
		dummyResourceCtlCyan = DummyResourceContoller.create(RESOURCE_DUMMY_CYAN_NAME, resourceDummyBlue);
		dummyResourceCtlCyan.extendSchemaPirate();
		dummyResourceCyan = dummyResourceCtlCyan.getDummyResource();
		resourceDummyCyan = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_CYAN_FILE, RESOURCE_DUMMY_CYAN_OID, initTask, initResult); 
		resourceDummyCyanType = resourceDummyCyan.asObjectable();
		dummyResourceCtlCyan.setResource(resourceDummyCyan);		
		
		dummyResourceCtlWhite = DummyResourceContoller.create(RESOURCE_DUMMY_WHITE_NAME, resourceDummyWhite);
		dummyResourceCtlWhite.extendSchemaPirate();
		dummyResourceWhite = dummyResourceCtlWhite.getDummyResource();
		resourceDummyWhite = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_WHITE_FILENAME, RESOURCE_DUMMY_WHITE_OID, initTask, initResult);
		resourceDummyWhiteType = resourceDummyWhite.asObjectable();
		dummyResourceCtlWhite.setResource(resourceDummyWhite);

        dummyResourceCtlGreen = DummyResourceContoller.create(RESOURCE_DUMMY_GREEN_NAME, resourceDummyGreen);
		dummyResourceCtlGreen.extendSchemaPirate();
		dummyResourceGreen = dummyResourceCtlGreen.getDummyResource();
		resourceDummyGreen = importAndGetObjectFromFile(ResourceType.class, getResourceDummyGreenFile(), RESOURCE_DUMMY_GREEN_OID, initTask, initResult);
		resourceDummyGreenType = resourceDummyGreen.asObjectable();
		dummyResourceCtlGreen.setResource(resourceDummyGreen);
		
		dummyResourceCtlEmerald = DummyResourceContoller.create(RESOURCE_DUMMY_EMERALD_NAME, resourceDummyEmerald);
		dummyResourceCtlEmerald.extendSchemaPirate();
		dummyResourceCtlEmerald.extendSchemaPosix();
		dummyResourceEmerald = dummyResourceCtlEmerald.getDummyResource();
		resourceDummyEmerald = importAndGetObjectFromFile(ResourceType.class, getResourceDummyEmeraldFile(), RESOURCE_DUMMY_EMERALD_OID, initTask, initResult); 
		resourceDummyEmeraldType = resourceDummyEmerald.asObjectable();
		dummyResourceCtlEmerald.setResource(resourceDummyEmerald);

		dummyResourceCtlOrange = DummyResourceContoller.create(RESOURCE_DUMMY_ORANGE_NAME, resourceDummyOrange);
		dummyResourceCtlOrange.extendSchemaPirate();
		dummyResourceOrange = dummyResourceCtlOrange.getDummyResource();
		resourceDummyOrange = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_ORANGE_FILENAME, RESOURCE_DUMMY_ORANGE_OID, initTask, initResult);
		resourceDummyOrangeType = resourceDummyOrange.asObjectable();
		dummyResourceCtlOrange.setResource(resourceDummyOrange);

		dummyResourceCtlUpcase = DummyResourceContoller.create(RESOURCE_DUMMY_UPCASE_NAME, resourceDummyUpcase);
		dummyResourceCtlUpcase.extendSchemaPirate();
		dummyResourceUpcase = dummyResourceCtlUpcase.getDummyResource();
		resourceDummyUpcase = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_UPCASE_FILE, RESOURCE_DUMMY_UPCASE_OID, initTask, initResult);
		resourceDummyUpcaseType = resourceDummyUpcase.asObjectable();
		dummyResourceCtlUpcase.setResource(resourceDummyUpcase);
		dummyResourceCtlUpcase.addGroup(GROUP_JOKER_DUMMY_UPCASE_NAME);

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
		
		repoAddObjectFromFile(LOOKUP_LANGUAGES_FILE, initResult);
		
		repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
		
		// User Templates
		repoAddObjectFromFile(USER_TEMPLATE_FILENAME, initResult);
		repoAddObjectFromFile(USER_TEMPLATE_COMPLEX_FILE, initResult);
		repoAddObjectFromFile(USER_TEMPLATE_INBOUNDS_FILENAME, initResult);
		repoAddObjectFromFile(USER_TEMPLATE_COMPLEX_INCLUDE_FILENAME, initResult);
        repoAddObjectFromFile(USER_TEMPLATE_ORG_ASSIGNMENT_FILENAME, initResult);

		// Shadows
		repoAddObjectFromFile(ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILE, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_FILE, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_FILE, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_FILE, initResult);
		repoAddObjectFromFile(GROUP_SHADOW_JOKER_DUMMY_UPCASE_FILE, initResult);
		
		// Users
		userTypeJack = repoAddObjectFromFile(USER_JACK_FILE, UserType.class, true, initResult).asObjectable();
		userTypeBarbossa = repoAddObjectFromFile(USER_BARBOSSA_FILE, UserType.class, initResult).asObjectable();
		userTypeGuybrush = repoAddObjectFromFile(USER_GUYBRUSH_FILE, UserType.class, initResult).asObjectable();
		userTypeElaine = repoAddObjectFromFile(USER_ELAINE_FILE, UserType.class, initResult).asObjectable();
		
		// Roles
		repoAddObjectFromFile(ROLE_PIRATE_FILE, initResult);
		repoAddObjectFromFile(ROLE_PIRATE_GREEN_FILE, initResult);
		repoAddObjectFromFile(ROLE_CARIBBEAN_PIRATE_FILE, initResult);
		repoAddObjectFromFile(ROLE_BUCCANEER_GREEN_FILE, initResult);
		repoAddObjectFromFile(ROLE_NICE_PIRATE_FILENAME, initResult);
		repoAddObjectFromFile(ROLE_CAPTAIN_FILENAME, initResult);
		repoAddObjectFromFile(ROLE_JUDGE_FILE, initResult);
		repoAddObjectFromFile(ROLE_JUDGE_DEPRECATED_FILE, initResult);
		repoAddObjectFromFile(ROLE_THIEF_FILE, initResult);
		repoAddObjectFromFile(ROLE_EMPTY_FILE, initResult);
		repoAddObjectFromFile(ROLE_SAILOR_FILE, initResult);
		repoAddObjectFromFile(ROLE_RED_SAILOR_FILE, initResult);
		repoAddObjectFromFile(ROLE_CYAN_SAILOR_FILE, initResult);
		
		// Orgstruct
		if (doAddOrgstruct()) {
			repoAddObjectsFromFile(ORG_MONKEY_ISLAND_FILE, OrgType.class, initResult);
		}
		
		// Services
		repoAddObjectFromFile(SERVICE_SHIP_SEA_MONKEY_FILE, initResult);

	}
	
	protected boolean doAddOrgstruct() {
		return true;
	}

	protected File getResourceDummyFile() {
		return RESOURCE_DUMMY_FILE;
	}

	protected File getResourceDummyBlueFile() {
		return RESOURCE_DUMMY_BLUE_FILE;
	}

	protected File getResourceDummyGreenFile() {
		return RESOURCE_DUMMY_GREEN_FILE;
	}
	
	protected File getResourceDummyEmeraldFile() {
		return RESOURCE_DUMMY_EMERALD_FILE;
	}

	protected void postInitDummyResouce() {
		// Do nothing be default. Concrete tests may override this.
	}

	protected void assertUserJack(PrismObject<UserType> user) {
		assertUserJack(user, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
	}
	
	protected void assertUserJack(PrismObject<UserType> user, String fullName) {
		assertUserJack(user, fullName, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
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
		assertUserJack(user, USER_JACK_USERNAME, fullName, givenName, familyName, locality);
	}
	
	protected void assertDummyAccountShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username) throws SchemaException {
		assertAccountShadowRepo(accountShadow, oid, username, dummyResourceCtl.getResource().asObjectable());
	}

    protected void assertDummyGroupShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username) throws SchemaException {
        assertShadowRepo(accountShadow, oid, username, dummyResourceCtl.getResourceType(), dummyResourceCtl.getGroupObjectClass());
    }
	
	protected void assertDummyAccountShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username) throws SchemaException {
		assertShadowModel(accountShadow, oid, username, dummyResourceCtl.getResourceType(), dummyResourceCtl.getAccountObjectClass());
	}

    protected void assertDummyGroupShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username) throws SchemaException {
        assertShadowModel(accountShadow, oid, username, dummyResourceCtl.getResourceType(), dummyResourceCtl.getGroupObjectClass());
    }
	
	protected void assertDummyAccountShadowModel(PrismObject<ShadowType> accountShadow, String oid, String username, String fullname) throws SchemaException {
		assertDummyAccountShadowModel(accountShadow, oid, username);
		IntegrationTestTools.assertAttribute(accountShadow, dummyResourceCtl.getAttributeFullnameQName(), fullname);
	}
		
	protected void setDefaultUserTemplate(String userTemplateOid)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, userTemplateOid);
	}

	protected void assertMonkeyIslandOrgSanity() throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		assertMonkeyIslandOrgSanity(0);
	}
	
	protected void assertMonkeyIslandOrgSanity(int expectedFictional) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(AbstractInitializedModelIntegrationTest.class.getName() + ".assertMonkeyIslandOrgSanity");
        OperationResult result = task.getResult();
        
        PrismObject<OrgType> orgGovernorOffice = modelService.getObject(OrgType.class, ORG_GOVERNOR_OFFICE_OID, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        OrgType orgGovernorOfficeType = orgGovernorOffice.asObjectable();
        assertEquals("Wrong governor office name", PrismTestUtil.createPolyStringType("F0001"), orgGovernorOfficeType.getName());
        
        List<PrismObject<OrgType>> governorSubOrgs = searchOrg(ORG_GOVERNOR_OFFICE_OID, OrgFilter.Scope.ONE_LEVEL, task, result);
        if (verbose) display("governor suborgs", governorSubOrgs);
        assertEquals("Unexpected number of governor suborgs", 3, governorSubOrgs.size());
        
        List<PrismObject<OrgType>> functionalOrgs = searchOrg(ORG_GOVERNOR_OFFICE_OID, OrgFilter.Scope.SUBTREE, task, result);
        if (verbose) display("functional orgs (null)", functionalOrgs);
        assertEquals("Unexpected number of functional orgs (null)", NUM_FUNCTIONAL_ORGS - 1 + expectedFictional, functionalOrgs.size());

        List<PrismObject<OrgType>> prootSubOrgs = searchOrg(ORG_PROJECT_ROOT_OID, OrgFilter.Scope.ONE_LEVEL, task, result);
        if (verbose) display("project root suborgs", prootSubOrgs);
        assertEquals("Unexpected number of governor suborgs", 2, prootSubOrgs.size());
        
        List<PrismObject<OrgType>> projectOrgs = searchOrg(ORG_PROJECT_ROOT_OID, OrgFilter.Scope.SUBTREE, task, result);
        if (verbose) display("project orgs (null)", projectOrgs);
        assertEquals("Unexpected number of functional orgs (null)", NUM_PROJECT_ORGS - 1, projectOrgs.size());

        PrismObject<OrgType> orgScummBar = modelService.getObject(OrgType.class, ORG_SCUMM_BAR_OID, null, task, result);
        List<AssignmentType> scummBarInducements = orgScummBar.asObjectable().getInducement();
        assertEquals("Unexpected number of scumm bar inducements: "+scummBarInducements,  1, scummBarInducements.size());
        
        ResultHandler<OrgType> handler = getOrgSanityCheckHandler();
        if (handler != null) {
        	modelService.searchObjectsIterative(OrgType.class, null, handler, null, task, result);
        }
	}
	
	protected ResultHandler<OrgType> getOrgSanityCheckHandler() {
		return null;
	}

	protected void assertShadowOperationalData(PrismObject<ShadowType> shadow, SynchronizationSituationType expectedSituation, Long timeBeforeSync) {
		ShadowType shadowType = shadow.asObjectable();
		SynchronizationSituationType actualSituation = shadowType.getSynchronizationSituation();
		assertEquals("Wrong situation in shadow "+shadow, expectedSituation, actualSituation);
		XMLGregorianCalendar actualTimestampCal = shadowType.getSynchronizationTimestamp();
		assert actualTimestampCal != null : "No synchronization timestamp in shadow "+shadow;
		if (timeBeforeSync != null) {
			long actualTimestamp = XmlTypeConverter.toMillis(actualTimestampCal);
			assert actualTimestamp >= timeBeforeSync : "Synchronization timestamp was not updated in shadow " + shadow;
		}
		// TODO: assert sync description
	}
	
	protected Collection<ProgressListener> getCheckingProgressListenerCollection() {
		return Collections.singleton((ProgressListener)checkingProgressListener);
	}
}
