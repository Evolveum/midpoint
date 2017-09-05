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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRaceConditions extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/contract");

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
	}

	@Override
	protected ConflictResolutionActionType getDefaultConflictResolutionAction() {
		return ConflictResolutionActionType.RECOMPUTE;
	}

	@Test
    public void test100AssignRoles() throws Exception {
		final String TEST_NAME="test100AssignRoles";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        @SuppressWarnings({"unchecked", "raw"})
		ObjectDelta<UserType> objectDelta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(
						ObjectTypeUtil.createAssignmentTo(ROLE_PIRATE_OID, ObjectTypes.ROLE, prismContext),
						ObjectTypeUtil.createAssignmentTo(ROLE_SAILOR_OID, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		executeChangesAssertSuccess(objectDelta, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);

		String accountJackOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountJackOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
    }

	/**
	 * Remove both roles at once, in different threads.
	 */
	@Test
    public void test110UnassignRoles() throws Exception {
		final String TEST_NAME = "test110UnassignRoles";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestRaceConditions.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		List<AssignmentType> assignments = userJack.asObjectable().getAssignment();
		assertEquals("Wrong # of assignments", 2, assignments.size());

		// WHEN
		Thread t1 = new Thread(() -> deleteAssignment(userJack, 0, task, result));
		Thread t2 = new Thread(() -> deleteAssignment(userJack, 1, task, result));
		t1.start();
		t2.start();
		t1.join(30000L);
		t2.join(30000L);

		// THEN
		PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
		display("User after change execution", userJackAfter);
		assertEquals("Unexpected # of projections of jack", 0, userJackAfter.asObjectable().getLinkRef().size());
    }

    private void deleteAssignment(PrismObject<UserType> user, int index, Task task, OperationResult result) {
		try {
			login(userAdministrator);
			@SuppressWarnings({ "unchecked", "raw" })
			ObjectDelta<UserType> objectDelta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
					.item(FocusType.F_ASSIGNMENT).delete(user.asObjectable().getAssignment().get(index).clone())
					.asObjectDelta(USER_JACK_OID);
			modelService.executeChanges(Collections.singletonList(objectDelta), null, task,
					Collections.singletonList(new DelayingProgressListener(0, 1000)), result);
		} catch (Throwable t) {
			throw new SystemException(t);
		}
    }

}
