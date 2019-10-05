/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.async;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
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
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Test for asynchronous Grouper->midPoint interface (demo/complex2s in Internet2 scenario).
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestGrouperAsyncUpdate extends AbstractStoryTest {

	private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "grouper-async");

	private static final TestResource LIB_GROUPER = new TestResource(TEST_DIR, "function-library-grouper.xml", "2eef4181-25fa-420f-909d-846a36ca90f3");
	private static final TestResource RESOURCE_GROUPER = new TestResource(TEST_DIR, "resource-grouper.xml", "1eff65de-5bb6-483d-9edf-8cc2c2ee0233");
	private static final TestResource USER_BANDERSON = new TestResource(TEST_DIR, "user-banderson.xml", "4f439db5-181e-4297-9f7d-b3115524dbe8");
	private static final TestResource USER_JLEWIS685 = new TestResource(TEST_DIR, "user-jlewis685.xml", "8b7bd936-b863-45d0-aabe-734fa3e22081");

	private static final String BANDERSON_USERNAME = "banderson";
	private static final String JLEWIS685_USERNAME = "jlewis685";
	private static final String NOBODY_USERNAME = "nobody";

	private static final String ALUMNI_NAME = "ref:alumni";

	private static final File CHANGE_110 = new File(TEST_DIR, "change-110-alumni-add.json");
	private static final File CHANGE_200 = new File(TEST_DIR, "change-200-banderson-add-alumni.json");
	private static final File CHANGE_220 = new File(TEST_DIR, "change-220-jlewis685-add-alumni.json");
	private static final File CHANGE_230 = new File(TEST_DIR, "change-230-nobody-add-alumni.json");
	private static final File CHANGE_250 = new File(TEST_DIR, "change-250-banderson-delete-alumni.json");

	private static final ItemName ATTR_MEMBER = new ItemName(MidPointConstants.NS_RI, "member");

	private PrismObject<ResourceType> grouperResource;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		repoAddObject(LIB_GROUPER, initResult);
		importAndGetObjectFromFile(ResourceType.class, RESOURCE_GROUPER.file, RESOURCE_GROUPER.oid, initTask, initResult);
		repoAddObject(USER_BANDERSON, initResult);
		repoAddObject(USER_JLEWIS685, initResult);

		grouperResource = provisioningService.getObject(ResourceType.class, RESOURCE_GROUPER.oid, null, initTask, initResult);

		setGlobalTracingOverride(createModelAndProvisioningLoggingTracingProfile());
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(this, TEST_NAME);
		Task task = createTestTask(TEST_NAME);

		OperationResult testResultGrouper = modelService.testResource(RESOURCE_GROUPER.oid, task);
		TestUtil.assertSuccess(testResultGrouper);
	}

	/**
	 * GROUP_ADD event for ref:alumni.
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
	}

	/**
	 * Adding ref:alumni membership for banderson.
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
				.display();
	}


	/**
	 * Adding ref:alumni membership for jlewis685.
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
				.display();
	}

	/**
	 * Adding ref:alumni membership for non-existing user (nobody).
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
	 * Deleting ref:alumni membership for banderson.
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

		assertMembers(ALUMNI_NAME, task, result, JLEWIS685_USERNAME, NOBODY_USERNAME);

		assertUserAfterByUsername(BANDERSON_USERNAME)
				.display();
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
		return assertShadowByNameViaModel(ShadowKindType.ENTITLEMENT, "group", groupName, grouperResource, "after", task, result)
				.attributes()
					.attribute(ATTR_MEMBER.getLocalPart())
						.assertRealValues(expectedUsers);
	}

}
