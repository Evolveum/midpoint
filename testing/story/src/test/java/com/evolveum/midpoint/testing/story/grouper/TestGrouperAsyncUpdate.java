/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.grouper;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.ShadowAttributesAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismPropertyAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.io.IOUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * Test for asynchronous Grouper->midPoint interface (demo/complex2s in Internet2 scenario).
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestGrouperAsyncUpdate extends AbstractStoryTest {

	private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "grouper");

	private static final File LDIF_INITIAL_OBJECTS_FILE = new File(TEST_DIR, "ldif-initial-objects.ldif");

	private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

	private static final TestResource LIB_GROUPER = new TestResource(TEST_DIR, "function-library-grouper.xml", "2eef4181-25fa-420f-909d-846a36ca90f3");

	private static final TestResource RESOURCE_LDAP = new TestResource(TEST_DIR, "resource-ldap.xml", "0a37121f-d515-4a23-9b6d-554c5ef61272");
	private static final TestResource RESOURCE_GROUPER = new TestResource(TEST_DIR, "resource-grouper.xml", "1eff65de-5bb6-483d-9edf-8cc2c2ee0233");

	private static final TestResource METAROLE_GROUPER_PROVIDED_GROUP = new TestResource(TEST_DIR, "metarole-grouper-provided-group.xml", "bcaec940-50c8-44bb-aa37-b2b5bb2d5b90");
	private static final TestResource METAROLE_LDAP_GROUP = new TestResource(TEST_DIR, "metarole-ldap-group.xml", "8da46694-bd71-4e1e-bfd7-73865ae2ea9a");

	private static final TestResource ARCHETYPE_AFFILIATION = new TestResource(TEST_DIR, "archetype-affiliation.xml", "56f53812-047d-4b69-83e8-519a73d161e1");
	private static final TestResource ORG_AFFILIATIONS = new TestResource(TEST_DIR, "org-affiliations.xml", "1d7c0e3a-4456-409c-9f50-95407b2eb785");

	private static final TestResource ROLE_LDAP_BASIC = new TestResource(TEST_DIR, "role-ldap-basic.xml", "c89f31dd-8d4f-4e0a-82cb-58ff9d8c1b2f");

	private static final TestResource TEMPLATE_USER = new TestResource(TEST_DIR, "template-user.xml", "8098b124-c20c-4965-8adf-e528abedf7a4");

	private static final TestResource USER_BANDERSON = new TestResource(TEST_DIR, "user-banderson.xml", "4f439db5-181e-4297-9f7d-b3115524dbe8");
	private static final TestResource USER_JLEWIS685 = new TestResource(TEST_DIR, "user-jlewis685.xml", "8b7bd936-b863-45d0-aabe-734fa3e22081");

	private static final TestResource TASK_GROUP_SCAVENGER = new TestResource(TEST_DIR, "task-group-scavenger.xml", "1d7bef40-953e-443e-8e9a-ec6e313668c4");

	private static final String NS_EXT = "http://grouper-demo.tier.internet2.edu";
	public static final QName EXT_GROUPER_NAME = new QName(NS_EXT, "grouperName");
	public static final QName EXT_LDAP_DN = new QName(NS_EXT, "ldapDn");

	private static final String BANDERSON_USERNAME = "banderson";
	private static final String JLEWIS685_USERNAME = "jlewis685";
	private static final String NOBODY_USERNAME = "nobody";

	private static final String ALUMNI_NAME = "ref:affiliation:alumni";
	private static final String STAFF_NAME = "ref:affiliation:staff";

	private static final File CHANGE_110 = new File(TEST_DIR, "change-110-alumni-add.json");
	private static final File CHANGE_115 = new File(TEST_DIR, "change-115-staff-add.json");
	private static final File CHANGE_200 = new File(TEST_DIR, "change-200-banderson-add-alumni.json");
	private static final File CHANGE_210 = new File(TEST_DIR, "change-210-banderson-add-staff.json");
	private static final File CHANGE_220 = new File(TEST_DIR, "change-220-jlewis685-add-alumni.json");
	private static final File CHANGE_221 = new File(TEST_DIR, "change-221-jlewis685-add-staff.json");
	private static final File CHANGE_230 = new File(TEST_DIR, "change-230-nobody-add-alumni.json");
	private static final File CHANGE_250 = new File(TEST_DIR, "change-250-banderson-delete-alumni.json");
	private static final File CHANGE_310 = new File(TEST_DIR, "change-310-staff-delete.json");

	private static final ItemName ATTR_MEMBER = new ItemName(MidPointConstants.NS_RI, "members");

	private static final String DN_BANDERSON = "uid=banderson,ou=people,dc=example,dc=com";
	private static final String DN_JLEWIS685 = "uid=jlewis685,ou=people,dc=example,dc=com";
	private static final String DN_ALUMNI = "cn=alumni,ou=Affiliations,ou=Groups,dc=example,dc=com";
	private static final String DN_STAFF = "cn=staff,ou=Affiliations,ou=Groups,dc=example,dc=com";

	private static final String GROUPER_DUMMY_RESOURCE_ID = "grouper";
	private PrismObject<ResourceType> resourceGrouper;
	private DummyResourceContoller grouperResourceCtl;
	private DummyResource grouperDummyResource;

	private PrismObject<ResourceType> resourceLdap;

	private String orgAlumniOid;
	private String orgStaffOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		// These are experimental features, so they need to be explicitly enabled. This will be eliminated later,
		// when we make them enabled by default.
		sqlRepositoryService.getConfiguration().setEnableIndexOnlyItems(true);
		sqlRepositoryService.getConfiguration().setEnableNoFetchExtensionValuesInsertion(true);
		sqlRepositoryService.getConfiguration().setEnableNoFetchExtensionValuesDeletion(true);

		openDJController.addEntriesFromLdifFile(LDIF_INITIAL_OBJECTS_FILE);

		repoAddObject(LIB_GROUPER, initResult);

		grouperResourceCtl = DummyResourceContoller.create(GROUPER_DUMMY_RESOURCE_ID);
		grouperResourceCtl.populateWithDefaultSchema();
		grouperDummyResource = grouperResourceCtl.getDummyResource();
		resourceGrouper = importAndGetObjectFromFile(ResourceType.class, RESOURCE_GROUPER.file, RESOURCE_GROUPER.oid, initTask, initResult);
		grouperResourceCtl.setResource(resourceGrouper);

		resourceLdap = importAndGetObjectFromFile(ResourceType.class, RESOURCE_LDAP.file, RESOURCE_LDAP.oid, initTask, initResult);
		openDJController.setResource(resourceLdap);

		addObject(METAROLE_GROUPER_PROVIDED_GROUP, initTask, initResult);
		addObject(METAROLE_LDAP_GROUP, initTask, initResult);

		addObject(ARCHETYPE_AFFILIATION, initTask, initResult);
		addObject(ORG_AFFILIATIONS, initTask, initResult);

		addObject(ROLE_LDAP_BASIC, initTask, initResult);
		addObject(TEMPLATE_USER, initTask, initResult);

		addObject(TASK_GROUP_SCAVENGER, initTask, initResult);

		setGlobalTracingOverride(createModelAndProvisioningLoggingTracingProfile());
	}

    @Override
    protected boolean isAutoTaskManagementEnabled() {
        return true;
    }

	@Override
	protected TracingProfileType getTestMethodTracingProfile() {
		return createModelAndProvisioningLoggingTracingProfile()
				.fileNamePattern(TEST_METHOD_TRACING_FILENAME_PATTERN);
	}

	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Override
	protected void startResources() throws Exception {
		openDJController.startCleanServerRI();
	}

	@AfterClass
	public static void stopResources() throws Exception {
		openDJController.stop();
	}

	@Test
	public void test000Sanity() throws Exception {
        Task task = getTask();

		assertSuccess(modelService.testResource(RESOURCE_LDAP.oid, task));
		assertSuccess(modelService.testResource(RESOURCE_GROUPER.oid, task));
	}

	@Test
	public void test010CreateUsers() throws Exception {
		Task task = getTask();
		OperationResult result = getResult();

		addObject(USER_BANDERSON, task, result);
		addObject(USER_JLEWIS685, task, result);

		assertSuccess(result);

		assertNotNull("no LDAP entry for banderson", openDJController.fetchEntry(DN_BANDERSON));
		assertNotNull("no LDAP entry for jlewis685", openDJController.fetchEntry(DN_JLEWIS685));
	}

	/**
	 * GROUP_ADD event for ref:affiliation:alumni.
	 */
	@Test
	public void test110AddAlumni() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_110));
		grouperDummyResource.addGroup(new DummyGroup(ALUMNI_NAME));

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result);

		orgAlumniOid = assertOrgByName("affiliation_alumni", "alumni after")
				.display()
				.assertLifecycleState("active")
				.extension()
						.property(EXT_GROUPER_NAME).singleValue().assertValue(ALUMNI_NAME).end().end()
						.property(EXT_LDAP_DN).singleValue().assertValue(DN_ALUMNI).end().end()
				.end()
				.assertAssignments(1)           // archetype, todo assert target
				.assertDisplayName("Affiliation: alumni")
				.assertIdentifier("alumni")
				.assertLinks(2)                // todo assert details
				.links()
					.projectionOnResource(RESOURCE_GROUPER.oid)
						.target()
							.assertNotDead()
						.end()
					.end()
					.projectionOnResource(RESOURCE_LDAP.oid)
						.target()
							.assertNotDead()
						.end()
					.end()
				.end()
				.getOid();
	}

	/**
	 * GROUP_ADD event for ref:affiliation:staff.
	 */
	@Test
	public void test115AddStaff() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_115));
		grouperDummyResource.addGroup(new DummyGroup(STAFF_NAME));
		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		assertSuccess(result);

		assertMembers(STAFF_NAME, task, result);

		orgStaffOid = assertOrgByName("affiliation_staff", "staff after")
				.display()
				.assertLifecycleState("active")
				.extension()
						.property(EXT_GROUPER_NAME).singleValue().assertValue(STAFF_NAME).end().end()
						.property(EXT_LDAP_DN).singleValue().assertValue(DN_STAFF).end().end()
				.end()
				.assertAssignments(1)           // archetype, todo assert target
				.assertDisplayName("Affiliation: staff")
				.assertIdentifier("staff")
				.assertLinks(2)                // todo assert details
				.links()
					.projectionOnResource(RESOURCE_GROUPER.oid)
						.target()
							.assertNotDead()
						.end()
					.end()
					.projectionOnResource(RESOURCE_LDAP.oid)
						.target()
							.assertNotDead()
						.end()
					.end()
				.end()
				.getOid();
	}

	/**
	 * Adding ref:affiliation:alumni membership for banderson.
	 */
	@Test
	public void test200AddAlumniForAnderson() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_200));
		grouperDummyResource.getGroupByName(ALUMNI_NAME).addMember(BANDERSON_USERNAME);

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME);

		assertUserAfterByUsername(BANDERSON_USERNAME)
				.triggers()
				.assertTriggers(1);
	}

	/**
	 * Anderson should obtain the assignment.
	 */
	@Test
	public void test202RecomputeAnderson() throws Exception {
		Task task = getTask();
		OperationResult result = getResult();

		// WHEN

		recomputeUser(USER_BANDERSON.oid, task, result);

		// THEN

		assertSuccess(result);

		assertUserAfterByUsername(BANDERSON_USERNAME)
				.assignments()
				.assertAssignments(2)
					.assertRole(ROLE_LDAP_BASIC.oid)
					.assertOrg(orgAlumniOid)
				.end()
				.links()
					.assertLinks(1)
					.projectionOnResource(resourceLdap.getOid());

		openDJController.assertUniqueMember(DN_ALUMNI, DN_BANDERSON);
	}

	/**
	 * Adding ref:affiliation:staff membership for banderson.
	 */
	@Test
	public void test210AddStaffForAnderson() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_210));
		grouperDummyResource.getGroupByName(STAFF_NAME).addMember(BANDERSON_USERNAME);

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME);
		assertMembers(STAFF_NAME, task, result, BANDERSON_USERNAME);

		assertUserAfterByUsername(BANDERSON_USERNAME)
				.triggers()
				.assertTriggers(1);
	}

	/**
	 * Anderson should obtain the second assignment.
	 */
	@Test
	public void test212RecomputeAnderson() throws Exception {
		Task task = getTask();
		OperationResult result = getResult();

		// WHEN

		recomputeUser(USER_BANDERSON.oid, task, result);

		// THEN

		assertSuccess(result);

		assertUserAfterByUsername(BANDERSON_USERNAME)
				.assignments()
				.assertAssignments(3)
					.assertRole(ROLE_LDAP_BASIC.oid)
					.assertOrg(orgAlumniOid)
					.assertOrg(orgStaffOid)
				.end()
				.links()
					.assertLinks(1)
					.projectionOnResource(resourceLdap.getOid());

		openDJController.assertUniqueMember(DN_ALUMNI, DN_BANDERSON);
		openDJController.assertUniqueMember(DN_STAFF, DN_BANDERSON);
	}


	/**
	 * Adding ref:affiliation:alumni membership for jlewis685.
	 */
	@Test
	public void test220AddAlumniForLewis() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_220));
		grouperDummyResource.getGroupByName(ALUMNI_NAME).addMember(JLEWIS685_USERNAME);

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME);

		assertUserAfterByUsername(JLEWIS685_USERNAME)
				.triggers()
				.assertTriggers(1);
	}

	/**
	 * Adding ref:affiliation:staff membership for jlewis685.
	 */
	@Test
	public void test221AddStaffForLewis() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_221));
		grouperDummyResource.getGroupByName(STAFF_NAME).addMember(JLEWIS685_USERNAME);

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME);
		assertMembers(STAFF_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME);

		assertUserAfterByUsername(JLEWIS685_USERNAME)
				.triggers()
				.assertTriggers(1);
	}

    /**
     * Lewis should obtain two assignments.
     */
    @Test
    public void test222RecomputeLewis() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

        // WHEN

        recomputeUser(USER_JLEWIS685.oid, task, result);

        // THEN

	    assertSuccess(result);

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .assignments()
                    .assertAssignments(3)
                    .assertRole(ROLE_LDAP_BASIC.oid)
                    .assertOrg(orgAlumniOid)
                    .assertOrg(orgStaffOid)
                .end()
                .links()
                    .assertLinks(1)
                    .projectionOnResource(resourceLdap.getOid());

        openDJController.assertUniqueMember(DN_ALUMNI, DN_JLEWIS685);
        openDJController.assertUniqueMember(DN_STAFF, DN_JLEWIS685);
    }

	/**
	 * Adding ref:affiliation:alumni membership for non-existing user (nobody).
	 */
	@Test
	public void test230AddAlumniForNobody() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_230));
		grouperDummyResource.getGroupByName(ALUMNI_NAME).addMember(NOBODY_USERNAME);

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME, NOBODY_USERNAME);
	}

	/**
	 * Deleting ref:affiliation:alumni membership for banderson.
	 */
	@Test
	public void test250DeleteAlumniForAnderson() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_250));
		grouperDummyResource.getGroupByName(ALUMNI_NAME).removeMember(BANDERSON_USERNAME);

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result, JLEWIS685_USERNAME, NOBODY_USERNAME);

		assertUserAfterByUsername(BANDERSON_USERNAME)
				.triggers()
				.assertTriggers(1);
	}

	/**
	 * Anderson should lose the first assignment.
	 */
	@Test
	public void test252RecomputeAnderson() throws Exception {
		Task task = getTask();
		OperationResult result = getResult();

		// WHEN

		recomputeUser(USER_BANDERSON.oid, task, result);

		// THEN

		assertSuccess(result);

		assertUserAfterByUsername(BANDERSON_USERNAME)
				.assignments()
					.assertAssignments(2)
					.assertRole(ROLE_LDAP_BASIC.oid)
					.assertOrg(orgStaffOid)
				.end()
				.links()
					.assertLinks(1)
					.projectionOnResource(resourceLdap.getOid());

		openDJController.assertUniqueMember(DN_STAFF, DN_BANDERSON);
	}

	/**
	 * Deleting ref:affiliation:staff group.
	 */
	@Test
	public void test310DeleteStaff() throws Exception {
		Task task = getTask();
		OperationResult result = getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_310));
		grouperDummyResource.deleteGroupByName(STAFF_NAME);

		executeChanges(deltaFor(UserType.class).item(UserType.F_TRIGGER).replace().asObjectDelta(USER_BANDERSON.oid), null, task, result);
		executeChanges(deltaFor(UserType.class).item(UserType.F_TRIGGER).replace().asObjectDelta(USER_JLEWIS685.oid), null, task, result);

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		assertSuccess(result);

		assertOrgByName("affiliation_staff", "staff after deletion")
				.display()
				.assertLifecycleState("retired")
				.extension()
					.property(EXT_GROUPER_NAME).singleValue().assertValue(STAFF_NAME).end().end()
					.property(EXT_LDAP_DN).singleValue().assertValue(DN_STAFF).end().end()
				.end()
				.assertAssignments(1)           // archetype, todo assert target
					.assertDisplayName("Affiliation: staff")
					.assertIdentifier("staff")
				.links()
					.projectionOnResource(RESOURCE_GROUPER.oid)
						.target()
							.assertDead()
						.end()
					.end()
					.projectionOnResource(RESOURCE_LDAP.oid)
						.target()
							.assertNotDead()
						.end()
					.end()
				.end();
	}

	/**
	 * Completes the deletion of staff group.
	 */
	@Test
	public void test312ScavengeGroups() throws Exception {
		Task task = getTask();
		OperationResult result = getResult();

		// GIVEN


		// WHEN

		rerunTask(TASK_GROUP_SCAVENGER.oid);

		// THEN

		assertSuccess(result);

		assertNoObject(OrgType.class, orgStaffOid, task, result);
		assertUserAfterByUsername(BANDERSON_USERNAME)
				.assignments()
					.assertAssignments(1)
					.assertRole(ROLE_LDAP_BASIC.oid)
				.end()
				.links()
					.assertLinks(1)
					.projectionOnResource(resourceLdap.getOid());

		assertUserAfterByUsername(JLEWIS685_USERNAME)
				.assignments()
					.assertAssignments(2)
					.assertRole(ROLE_LDAP_BASIC.oid)
					.assertOrg(orgAlumniOid)
				.end()
				.links()
					.assertLinks(1)
					.projectionOnResource(resourceLdap.getOid());

		openDJController.assertNoEntry(DN_STAFF);

		openDJController.assertNoUniqueMember(DN_ALUMNI, DN_BANDERSON);
		openDJController.assertUniqueMember(DN_ALUMNI, DN_JLEWIS685);
	}

	private AsyncUpdateMessageType getAmqp091Message(File file) throws IOException {
		Amqp091MessageType rv = new Amqp091MessageType();
		String json = String.join("\n", IOUtils.readLines(new FileReader(file)));
		rv.setBody(json.getBytes(StandardCharsets.UTF_8));
		return rv;
	}

	@SuppressWarnings({ "SameParameterValue", "UnusedReturnValue" })
	private PrismPropertyAsserter<Object, ShadowAttributesAsserter<Void>> assertMembers(String groupName, Task task,
			OperationResult result, String... expectedUsers)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException {

		PrismObject<ShadowType> shadowInRepo = findShadowByName(ShadowKindType.ENTITLEMENT, "group", groupName, resourceGrouper, result);
		assertNotNull("No shadow with name '"+groupName+"'", shadowInRepo);

		Collection<SelectorOptions<GetOperationOptions>> options =
				schemaHelper.getOperationOptionsBuilder()
						.noFetch()
						.retrieve()
						.build();
		PrismObject<ShadowType> shadow = provisioningService
				.getObject(ShadowType.class, shadowInRepo.getOid(), options, task, result);

		return assertShadow(shadow, "after")
				.attributes()
					.attribute(ATTR_MEMBER.getLocalPart())
						.assertRealValues(expectedUsers);
	}
}
