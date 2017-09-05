/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Tests various aspects of consistency mechanism. Unlike tests in consistency-mechanism module,
 * tests here are much simpler (e.g. use dummy resource instead of OpenDJ) and relatively isolated.
 *
 * TODO - move to testing/consistency-mechanism?
 *
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConsistencySimple extends AbstractInitializedModelIntegrationTest {

	private static final boolean ASSERT_SUCCESS = true;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		login(USER_ADMINISTRATOR_USERNAME);
	}

	private enum FocusOperation { RECONCILE, RECOMPUTE }
	private enum ShadowOperation { KEEP, DELETE, UNLINK, UNLINK_AND_TOMBSTONE }
	private enum ResourceObjectOperation { KEEP, DELETE }

	private ObjectClassComplexTypeDefinition getAccountObjectClassDefinition() throws SchemaException {
		ResourceSchema schema = RefinedResourceSchemaImpl.getResourceSchema(getDummyResourceObject(), prismContext);
		return schema.findObjectClassDefinition(dummyResourceCtl.getAccountObjectClassQName());
	}

	@Test
	public void test100Reconcile_Keep_Keep() throws Exception {
		executeTest("test100Reconcile_Keep_Keep", FocusOperation.RECONCILE, ShadowOperation.KEEP, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test110Recompute_Keep_Keep() throws Exception {
		executeTest("test110Recompute_Keep_Keep", FocusOperation.RECOMPUTE, ShadowOperation.KEEP, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test120Reconcile_Unlink_Keep() throws Exception {
		executeTest("test120Reconcile_Unlink_Keep", FocusOperation.RECONCILE, ShadowOperation.UNLINK, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test130Recompute_Unlink_Keep() throws Exception {
		executeTest("test130Recompute_Unlink_Keep", FocusOperation.RECOMPUTE, ShadowOperation.UNLINK, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test140Reconcile_UnlinkTombstone_Keep() throws Exception {
		executeTest("test140Reconcile_UnlinkTombstone_Keep", FocusOperation.RECONCILE, ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test150Recompute_UnlinkTombstone_Keep() throws Exception {
		executeTest("test150Recompute_UnlinkTombstone_Keep", FocusOperation.RECOMPUTE, ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test160Reconcile_Delete_Keep() throws Exception {
		executeTest("test160Reconcile_Delete_Keep", FocusOperation.RECONCILE, ShadowOperation.DELETE, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test170Recompute_Delete_Keep() throws Exception {
		executeTest("test170Recompute_Delete_Keep", FocusOperation.RECOMPUTE, ShadowOperation.DELETE, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test200Reconcile_Keep_Delete() throws Exception {
		executeTest("test200Reconcile_Keep_Delete", FocusOperation.RECONCILE, ShadowOperation.KEEP, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test210Recompute_Keep_Delete() throws Exception {
		executeTest("test210Recompute_Keep_Delete", FocusOperation.RECOMPUTE, ShadowOperation.KEEP, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test220Reconcile_Unlink_Delete() throws Exception {
		executeTest("test220Reconcile_Unlink_Delete", FocusOperation.RECONCILE, ShadowOperation.UNLINK, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test230Recompute_Unlink_Delete() throws Exception {
		executeTest("test230Recompute_Unlink_Delete", FocusOperation.RECOMPUTE, ShadowOperation.UNLINK, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test240Reconcile_UnlinkTombstone_Delete() throws Exception {
		executeTest("test240Reconcile_UnlinkTombstone_Delete", FocusOperation.RECONCILE, ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test250Recompute_UnlinkTombstone_Delete() throws Exception {
		executeTest("test250Recompute_UnlinkTombstone_Delete", FocusOperation.RECOMPUTE, ShadowOperation.UNLINK_AND_TOMBSTONE, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test260Reconcile_Delete_Delete() throws Exception {
		executeTest("test260Reconcile_Delete_Delete", FocusOperation.RECONCILE, ShadowOperation.DELETE, ResourceObjectOperation.KEEP);
	}

	@Test
	public void test270Recompute_Delete_Delete() throws Exception {
		executeTest("test270Recompute_Delete_Delete", FocusOperation.RECOMPUTE, ShadowOperation.DELETE, ResourceObjectOperation.KEEP);
	}

    private void executeTest(final String TEST_NAME, FocusOperation focusOperation, ShadowOperation shadowOperation,
			ResourceObjectOperation resourceObjectOperation) throws Exception {
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestConsistencySimple.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		cleanUpBeforeTest(task, result);

		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		display("Jack with account", jack);
		assertEquals("Unexpected # of accounts for jack", 1, jack.asObjectable().getLinkRef().size());

		ShadowType shadow = getObject(ShadowType.class, jack.asObjectable().getLinkRef().get(0).getOid()).asObjectable();
		display("Shadow", shadow);

		if (shadowOperation != ShadowOperation.KEEP) {
			@SuppressWarnings("unchecked")
			ObjectDelta<UserType> removeLinkRefDelta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
					.item(UserType.F_LINK_REF).replace()
					.asObjectDelta(USER_JACK_OID);
			executeChanges(removeLinkRefDelta, ModelExecuteOptions.createRaw(), task, result);
			jack = getUser(USER_JACK_OID);
			assertEquals("Unexpected # of accounts for jack after linkRef removal", 0, jack.asObjectable().getLinkRef().size());

			if (shadowOperation == ShadowOperation.DELETE) {
				deleteObjectRaw(ShadowType.class, shadow.getOid(), task, result);
				assertNoObject(ShadowType.class, shadow.getOid());
			} else {
				if (shadowOperation == ShadowOperation.UNLINK_AND_TOMBSTONE) {
					@SuppressWarnings("unchecked")
					ObjectDelta<ShadowType> markAsDead = (ObjectDelta<ShadowType>) DeltaBuilder.deltaFor(ShadowType.class, prismContext)
							.item(ShadowType.F_DEAD).replace(Boolean.TRUE)
							.asObjectDelta(shadow.getOid());
					executeChanges(markAsDead, ModelExecuteOptions.createRaw(), task, result);
				}
				assertNotNull("jack's shadow does not exist", getObject(ShadowType.class, shadow.getOid()));
			}
		}

		if (resourceObjectOperation == ResourceObjectOperation.DELETE) {
			getDummyResource().deleteAccountByName("jack");
			assertNoDummyAccount(null, "jack");
		} else {
			assertDummyAccount(null, "jack");
		}

		// WHEN
		switch (focusOperation) {
			case RECOMPUTE:
				recomputeUser(USER_JACK_OID, task, result);
				break;
			case RECONCILE:
				ObjectDelta<UserType> emptyDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_JACK_OID, prismContext);
				modelService.executeChanges(MiscSchemaUtil.createCollection(emptyDelta), ModelExecuteOptions.createReconcile(), task, result);
				break;
			default:
				throw new IllegalArgumentException("focusOperation: " + focusOperation);
		}

		// THEN
		result.computeStatus();
		if (ASSERT_SUCCESS) {
			TestUtil.assertSuccess(result);
		}

		jack = getUser(USER_JACK_OID);
		display("Jack after " + focusOperation, jack);
		assertEquals("Unexpected # of accounts for jack", 1, jack.asObjectable().getLinkRef().size());
		String shadowOidAfter = jack.asObjectable().getLinkRef().get(0).getOid();

		// check the shadow really exists
		assertNotNull("jack's shadow does not exist after " + focusOperation, getObject(ShadowType.class, shadowOidAfter));

		// check number of jack's shadows
		List<PrismObject<ShadowType>> shadows = getJacksShadows(result);
		display("Shadows for 'jack' on dummy resource", shadows);
		assertEquals("Unexpected # of dummy shadows for 'jack'", 1, shadows.size());        // TODO some of them may be marked as dead (solve if this occurs)

		// other checks
		assertDummyAccount(null, "jack");

		cleanUpAfterTest(task, result);
	}

	private void cleanUpBeforeTest(Task task, OperationResult result) throws Exception {
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		display("Jack on start", jack);
		if (!jack.asObjectable().getAssignment().isEmpty()) {
			unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
			jack = getUser(USER_JACK_OID);
			display("Jack after initial unassign", jack);
		}
		if (!jack.asObjectable().getLinkRef().isEmpty()) {
			for (ObjectReferenceType ref : jack.asObjectable().getLinkRef()) {
				deleteObject(ShadowType.class, ref.getOid());
			}
			ObjectDelta<UserType> killLinkRefDelta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
					.item(UserType.F_LINK_REF).replace().asObjectDelta(USER_JACK_OID);
			executeChanges(killLinkRefDelta, ModelExecuteOptions.createRaw(), task, result);
		}
		List<PrismObject<ShadowType>> jacksShadows = getJacksShadows(result);
		for (PrismObject<ShadowType> shadow : jacksShadows) {
			deleteObject(ShadowType.class, shadow.getOid());
		}
	}

	private void cleanUpAfterTest(Task task, OperationResult result) throws Exception {
		unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		display("Jack after cleanup", jack);
		assertEquals("Unexpected # of accounts for jack after cleanup", 0, jack.asObjectable().getLinkRef().size());

		List<PrismObject<ShadowType>> shadowsAfterCleanup = getJacksShadows(result);
		display("Shadows for 'jack' on dummy resource after cleanup", shadowsAfterCleanup);
		assertEquals("Unexpected # of dummy shadows for 'jack' after cleanup", 0, shadowsAfterCleanup.size());		// TODO some of them may be marked as dead (solve if this occurs)
		assertNoDummyAccount(null, "jack");
	}

	private List<PrismObject<ShadowType>> getJacksShadows(OperationResult result) throws SchemaException {
		ObjectQuery shadowQuery = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
				.and().item(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME),
						getAccountObjectClassDefinition().findAttributeDefinition(SchemaConstants.ICFS_NAME)).eq("jack")
				.build();
		return repositoryService.searchObjects(ShadowType.class, shadowQuery, null, result);
	}
}
