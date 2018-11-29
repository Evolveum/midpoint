/*
 * Copyright (c) 2010-2018 Evolveum
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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Tests various aspects of consistency mechanism. Unlike the complex story test,
 * those tests here are much simpler (e.g. use dummy resource instead of OpenDJ)
 * and relatively isolated.
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
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME + ".given");
        OperationResult result = task.getResult();

		cleanUpBeforeTest(task, result);

		assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		display("Jack with account", jack);
		assertEquals("Unexpected # of accounts for jack", 1, jack.asObjectable().getLinkRef().size());

		ShadowType shadowBefore = getObject(ShadowType.class, jack.asObjectable().getLinkRef().get(0).getOid()).asObjectable();
		display("Shadow", shadowBefore);

		if (shadowOperation != ShadowOperation.KEEP) {
			@SuppressWarnings("unchecked")
			ObjectDelta<UserType> removeLinkRefDelta = deltaFor(UserType.class)
					.item(UserType.F_LINK_REF).replace()
					.asObjectDelta(USER_JACK_OID);
			executeChanges(removeLinkRefDelta, ModelExecuteOptions.createRaw(), task, result);
			jack = getUser(USER_JACK_OID);
			assertEquals("Unexpected # of accounts for jack after linkRef removal", 0, jack.asObjectable().getLinkRef().size());

			if (shadowOperation == ShadowOperation.DELETE) {
				deleteObjectRaw(ShadowType.class, shadowBefore.getOid(), task, result);
				assertNoObject(ShadowType.class, shadowBefore.getOid());
			} else {
				if (shadowOperation == ShadowOperation.UNLINK_AND_TOMBSTONE) {
					@SuppressWarnings("unchecked")
					ObjectDelta<ShadowType> markAsDead = deltaFor(ShadowType.class)
							.item(ShadowType.F_DEAD).replace(Boolean.TRUE)
							.asObjectDelta(shadowBefore.getOid());
					executeChanges(markAsDead, ModelExecuteOptions.createRaw(), task, result);
				}
				assertNotNull("jack's shadow does not exist", getObject(ShadowType.class, shadowBefore.getOid()));
			}
		}

		if (resourceObjectOperation == ResourceObjectOperation.DELETE) {
			getDummyResource().deleteAccountByName("jack");
			assertNoDummyAccount(null, "jack");
		} else {
			assertDummyAccount(null, "jack");
		}

		task = createTask(TEST_NAME);
        result = task.getResult();
		
		// WHEN
		displayWhen(TEST_NAME);
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
		displayThen(TEST_NAME);
		result.computeStatus();
		display("Result", result);
		if (ASSERT_SUCCESS) {
			// Do not look too deep into the result. There may be failures deep inside.
			TestUtil.assertSuccess(result, 2);
		}

		jack = getUser(USER_JACK_OID);
		display("Jack after " + focusOperation, jack);
		assertEquals("Unexpected # of accounts for jack", 1, jack.asObjectable().getLinkRef().size());
		String shadowOidAfter = jack.asObjectable().getLinkRef().get(0).getOid();

		// check the shadow really exists
		assertNotNull("jack's shadow does not exist after " + focusOperation, getObject(ShadowType.class, shadowOidAfter));

		assertLiveShadows(1, result);
		
		// other checks
		assertDummyAccount(null, "jack");

		cleanUpAfterTest(task, result);
	}

    private List<PrismObject<ShadowType>> assertLiveShadows(int expected, OperationResult result) throws SchemaException {
    	List<PrismObject<ShadowType>> shadowsAfter = getJacksShadows(result);
		display("Shadows for 'jack' on dummy resource", shadowsAfter);
		PrismObject<ShadowType> liveShadowAfter = null;
		for (PrismObject<ShadowType> shadowAfter : shadowsAfter) {
			if (!ShadowUtil.isDead(shadowAfter.asObjectable())) {
				if (liveShadowAfter == null) {
					liveShadowAfter = shadowAfter;
				} else {
					fail("More than one live shadow "+liveShadowAfter + ", " + shadowAfter);
				}
			}
		}
		if (expected == 0 && liveShadowAfter != null) {
			fail("Unexpected live shadow: "+liveShadowAfter);
		}
		if (expected == 1 && liveShadowAfter == null) {
			fail("No live shadow");
		}
		return shadowsAfter;
    }
    
	private void cleanUpBeforeTest(Task task, OperationResult result) throws Exception {
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		display("Jack on start", jack);
		if (!jack.asObjectable().getAssignment().isEmpty()) {
			unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
			jack = getUser(USER_JACK_OID);
			display("Jack after initial unassign", jack);
		}
		if (!jack.asObjectable().getLinkRef().isEmpty()) {
			for (ObjectReferenceType ref : jack.asObjectable().getLinkRef()) {
				deleteObject(ShadowType.class, ref.getOid());
			}
			ObjectDelta<UserType> killLinkRefDelta = deltaFor(UserType.class)
					.item(UserType.F_LINK_REF).replace().asObjectDelta(USER_JACK_OID);
			executeChanges(killLinkRefDelta, ModelExecuteOptions.createRaw(), task, result);
		}
		List<PrismObject<ShadowType>> jacksShadows = getJacksShadows(result);
		for (PrismObject<ShadowType> shadow : jacksShadows) {
			deleteObject(ShadowType.class, shadow.getOid());
		}
	}

	private void cleanUpAfterTest(Task task, OperationResult result) throws Exception {
		unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, SchemaConstants.INTENT_DEFAULT, task, result);
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		display("Jack after cleanup", jack);
		assertEquals("Unexpected # of accounts for jack after cleanup", 0, jack.asObjectable().getLinkRef().size());

		List<PrismObject<ShadowType>> deadShadows = assertLiveShadows(0, result);
		for (PrismObject<ShadowType> deadShadow : deadShadows) {
			repositoryService.deleteObject(ShadowType.class, deadShadow.getOid(), result);
		}
		
		assertNoDummyAccount(null, "jack");
	}

	private List<PrismObject<ShadowType>> getJacksShadows(OperationResult result) throws SchemaException {
		ObjectQuery shadowQuery = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
				.and().item(ItemPath.create(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME),
						getAccountObjectClassDefinition().findAttributeDefinition(SchemaConstants.ICFS_NAME)).eq("jack")
				.build();
		return repositoryService.searchObjects(ShadowType.class, shadowQuery, null, result);
	}
}
