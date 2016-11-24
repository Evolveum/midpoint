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
package com.evolveum.midpoint.wf.impl.policy;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.WorkflowResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractTestLifecycle extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(AbstractTestLifecycle.class);

	final String PIRATE_OID = "00000001-d34d-b33f-f00d-PIRATE000001";

	@Test(enabled = false)
    public void test010CreateRolePirate() throws Exception {
		final String TEST_NAME = "test010CreateRolePirate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		RoleType pirate = new RoleType(prismContext);
		pirate.setOid(PIRATE_OID);
		pirate.setName(PolyStringType.fromOrig("pirate"));
		createObject(TEST_NAME, pirate, false, true, USER_PIRATE_OWNER_OID);
	}

	@Test
	public void test100ModifyRolePirateDescription() throws Exception {
		final String TEST_NAME = "test100ModifyRolePirateDescription";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		ObjectDelta descriptionDelta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_DESCRIPTION).replace("Bloody pirate")
				.asObjectDelta(PIRATE_OID);
		modifyObject(TEST_NAME, descriptionDelta, false, true, USER_PIRATE_OWNER_OID);
	}

	public void createObject(final String TEST_NAME, ObjectType object, boolean immediate, boolean approve,
	                         String assigneeOid) throws Exception {
		ObjectDelta<RoleType> addObjectDelta = ObjectDelta.createAddDelta(object.asPrismObject());

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<RoleType> lensContext = createLensContext((Class) object.getClass());
				addFocusDeltaToContext(lensContext, addObjectDelta);
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems, OperationResult result) throws Exception {
				if (immediate) {
					assertFalse("There is model context in the root task (it should not be there)",
							wfTaskUtil.hasModelContext(rootTask));
				} else {
					ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
					ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
					assertTrue("Non-empty primary focus delta: " + realDelta0.debugDump(), realDelta0.isEmpty());
					assertNoObject(object);
					ExpectedTask expectedTask = new ExpectedTask(null, "Approve creating " + object.getName().getOrig());
					ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(assigneeOid, null, expectedTask);
					assertWfContextAfterClockworkRun(rootTask, subtasks, workItems, result,
							null,
							Collections.singletonList(expectedTask),
							Collections.singletonList(expectedWorkItem));
				}
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				assertNoObject(object);
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				if (approve) {
					assertObject(object);
				} else {
					assertNoObject(object);
				}
			}

			@Override
			protected boolean executeImmediately() {
				return immediate;
			}

			@Override
			protected Boolean decideOnApproval(String executionId) throws Exception {
				return approve;
			}
		}, 1);
	}

	private void assertNoObject(ObjectType object) throws SchemaException, com.evolveum.midpoint.util.exception.ObjectNotFoundException, com.evolveum.midpoint.util.exception.SecurityViolationException, com.evolveum.midpoint.util.exception.CommunicationException, com.evolveum.midpoint.util.exception.ConfigurationException {
		assertNull("Object was created but it shouldn't be",
				searchObjectByName(object.getClass(), object.getName().getOrig()));
	}

	private <T extends ObjectType> void assertObject(T object) throws SchemaException, com.evolveum.midpoint.util.exception.ObjectNotFoundException, com.evolveum.midpoint.util.exception.SecurityViolationException, com.evolveum.midpoint.util.exception.CommunicationException, com.evolveum.midpoint.util.exception.ConfigurationException {
		PrismObject<T> objectFromRepo = searchObjectByName((Class<T>) object.getClass(), object.getName().getOrig());
		assertNotNull("Object " + object + " was not created", object);
		objectFromRepo.removeItem(new ItemPath(ObjectType.F_METADATA), Item.class);
		assertEquals("Object is different from the one that was expected", object, objectFromRepo);
	}

	public <T extends ObjectType> void modifyObject(final String TEST_NAME, ObjectDelta<T> objectDelta,
	                                                ObjectDelta<T> expectedDelta0, ObjectDelta<T> expectedDelta1,
	                                                boolean immediate, boolean approve,
	                         String assigneeOid) throws Exception {

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				Class<T> clazz = objectDelta.getObjectTypeClass();
				//PrismObject<T> object = getObject(clazz, objectDelta.getOid());
				LensContext<T> lensContext = createLensContext(clazz);
				addFocusDeltaToContext(lensContext, objectDelta);
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems, OperationResult result) throws Exception {
				if (immediate) {
					assertFalse("There is model context in the root task (it should not be there)",
							wfTaskUtil.hasModelContext(rootTask));
				} else {
					ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
					ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
					assertTrue("Non-empty primary focus delta: " + realDelta0.debugDump(), realDelta0.isEmpty());
					assertNoObject(object);
					ExpectedTask expectedTask = new ExpectedTask(null, "Approve creating " + object.getName().getOrig());
					ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(assigneeOid, null, expectedTask);
					assertWfContextAfterClockworkRun(rootTask, subtasks, workItems, result,
							null,
							Collections.singletonList(expectedTask),
							Collections.singletonList(expectedWorkItem));
				}
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				assertNoObject(object);
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				if (approve) {
					assertObject(object);
				} else {
					assertNoObject(object);
				}
			}

			@Override
			protected boolean executeImmediately() {
				return immediate;
			}

			@Override
			protected Boolean decideOnApproval(String executionId) throws Exception {
				return approve;
			}
		}, 1);
	}

	@Test
	public void zzzMarkAsNotInitialized() {
		display("Setting class as not initialized");
		unsetSystemInitialized();
	}

}
