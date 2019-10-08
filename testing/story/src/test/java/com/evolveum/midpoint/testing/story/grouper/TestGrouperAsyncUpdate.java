/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.grouper;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.ShadowAttributesAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismPropertyAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
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

	private static final String NS_EXT = "http://grouper-demo.tier.internet2.edu";
	public static final QName EXT_GROUPER_NAME = new QName(NS_EXT, "grouperName");
	public static final QName EXT_LDAP_DN = new QName(NS_EXT, "ldapDn");

	private static final String BANDERSON_USERNAME = "banderson";
	private static final String JLEWIS685_USERNAME = "jlewis685";
	private static final String NOBODY_USERNAME = "nobody";

	private static final String ALUMNI_NAME = "ref:affiliation:alumni";

	private static final File CHANGE_110 = new File(TEST_DIR, "change-110-alumni-add.json");
	private static final File CHANGE_200 = new File(TEST_DIR, "change-200-banderson-add-alumni.json");
	private static final File CHANGE_220 = new File(TEST_DIR, "change-220-jlewis685-add-alumni.json");
	private static final File CHANGE_230 = new File(TEST_DIR, "change-230-nobody-add-alumni.json");
	private static final File CHANGE_250 = new File(TEST_DIR, "change-250-banderson-delete-alumni.json");

	private static final ItemName ATTR_MEMBER = new ItemName(MidPointConstants.NS_RI, "member");

	private static final String DN_BANDERSON = "uid=banderson,ou=people,dc=example,dc=com";
	private static final String DN_JLEWIS685 = "uid=jlewis685,ou=people,dc=example,dc=com";
	public static final String DN_ALUMNI = "cn=alumni,ou=Affiliations,ou=Groups,dc=example,dc=com";

	private PrismObject<ResourceType> resourceGrouper;
	private PrismObject<ResourceType> resourceLdap;

	private String alumniOid;

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

		resourceGrouper = importAndGetObjectFromFile(ResourceType.class, RESOURCE_GROUPER.file, RESOURCE_GROUPER.oid, initTask, initResult);
		resourceLdap = importAndGetObjectFromFile(ResourceType.class, RESOURCE_LDAP.file, RESOURCE_LDAP.oid, initTask, initResult);
		openDJController.setResource(resourceLdap);

		addObject(METAROLE_GROUPER_PROVIDED_GROUP, initTask, initResult);
		addObject(METAROLE_LDAP_GROUP, initTask, initResult);

		addObject(ARCHETYPE_AFFILIATION, initTask, initResult);
		addObject(ORG_AFFILIATIONS, initTask, initResult);

		addObject(ROLE_LDAP_BASIC, initTask, initResult);
		addObject(TEMPLATE_USER, initTask, initResult);

		setGlobalTracingOverride(createModelAndProvisioningLoggingTracingProfile());
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
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = createTestTask(TEST_NAME);

		assertSuccess(modelService.testResource(RESOURCE_LDAP.oid, task));
		assertSuccess(modelService.testResource(RESOURCE_GROUPER.oid, task));
	}

	@Test
	public void test010CreateUsers() throws Exception {
		final String TEST_NAME = "test010CreateUsers";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = createTestTask(TEST_NAME);
		OperationResult result = task.getResult();

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
		final String TEST_NAME = "test110AddAlumni";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = createTestTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_110));

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result);

		alumniOid = assertOrgByName("affiliation_alumni", "alumni after")
				.extension()
						.property(EXT_GROUPER_NAME).singleValue().assertValue("ref:affiliation:alumni").end().end()
						.property(EXT_LDAP_DN).singleValue().assertValue(DN_ALUMNI).end().end()
				.end()
				.assertAssignments(1)           // archetype, todo assert target
				.assertDisplayName("Affiliation: alumni")
				.assertIdentifier("alumni")
				.assertLinks(2)                // todo assert details
				.getOid();
	}

	/**
	 * Adding ref:affiliation:alumni membership for banderson.
	 */
	@Test
	public void test200AddAlumniForAnderson() throws Exception {
		final String TEST_NAME = "test200AddAlumniForAnderson";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = createTestTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_200));

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME);

		assertUserAfterByUsername(BANDERSON_USERNAME)
				.triggers()
				.assertTriggers(1);
	}

	/**
	 * Anderson should obtain an assignment.
	 */
	@Test
	public void test202RecomputeAnderson() throws Exception {
		final String TEST_NAME = "test202RecomputeAnderson";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = createTestTask(TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN

		recomputeUser(USER_BANDERSON.oid, task, result);

		// THEN

		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertUserAfterByUsername(BANDERSON_USERNAME)
				.assignments()
						.assertAssignments(2)
						.assertRole(ROLE_LDAP_BASIC.oid)
						.assertOrg(alumniOid)
				.end()
				.links()
					.assertLinks(1)
					.projectionOnResource(resourceLdap.getOid());

		openDJController.assertUniqueMember(DN_ALUMNI, DN_BANDERSON);
	}

	/**
	 * Adding ref:affiliation:alumni membership for jlewis685.
	 */
	@Test
	public void test220AddAlumniForLewis() throws Exception {
		final String TEST_NAME = "test220AddAlumniForLewis";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = createTestTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_220));

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME);

		assertUserAfterByUsername(JLEWIS685_USERNAME)
				.triggers()
				.assertTriggers(1);
	}

	/**
	 * Adding ref:affiliation:alumni membership for non-existing user (nobody).
	 */
	@Test
	public void test230AddAlumniForNobody() throws Exception {
		final String TEST_NAME = "test230AddAlumniForNobody";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = createTestTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_230));

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertMembers(ALUMNI_NAME, task, result, BANDERSON_USERNAME, JLEWIS685_USERNAME, NOBODY_USERNAME);
	}

	/**
	 * Deleting ref:affiliation:alumni membership for banderson.
	 */
	@Test
	public void test250DeleteAlumniForAnderson() throws Exception {
		final String TEST_NAME = "test250DeleteAlumniForAnderson";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = createTestTask(TEST_NAME);
		OperationResult result = task.getResult();

		// GIVEN

		MockAsyncUpdateSource.INSTANCE.reset();
		MockAsyncUpdateSource.INSTANCE.prepareMessage(getAmqp091Message(CHANGE_250));

		// WHEN

		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_GROUPER.oid);
		String handle = provisioningService.startListeningForAsyncUpdates(coords, task, result);
		provisioningService.stopListeningForAsyncUpdates(handle, task, result);

		// THEN

		result.computeStatus();
		TestUtil.assertSuccess(result);

		if (false) {        // Disabled because of MID-5832
			assertMembers(ALUMNI_NAME, task, result, JLEWIS685_USERNAME, NOBODY_USERNAME);

			assertUserAfterByUsername(BANDERSON_USERNAME)
					.triggers()
					.assertTriggers(1);
		}
	}

	private Task createTestTask(String TEST_NAME) {
		return createTask(TestGrouperAsyncUpdate.class.getName() + "." + TEST_NAME);
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
