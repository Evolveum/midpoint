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
package com.evolveum.midpoint.wf.impl.policy.lifecycle;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.policy.ExpectedTask;
import com.evolveum.midpoint.wf.impl.policy.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 * Testing approvals of role lifecycle: create/modify/delete role.
 *
 * Subclasses provide specializations regarding ways how rules and/or approvers are attached to roles.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractTestLifecycle extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(AbstractTestLifecycle.class);

	protected static final File TEST_LIFECYCLE_RESOURCE_DIR = new File("src/test/resources/policy/lifecycle");

	protected static final File USER_PIRATE_OWNER_FILE = new File(TEST_LIFECYCLE_RESOURCE_DIR, "user-pirate-owner.xml");
	protected static final File USER_JUDGE_OWNER_FILE = new File(TEST_LIFECYCLE_RESOURCE_DIR, "user-judge-owner.xml");

	protected String userPirateOwnerOid;
	protected String userJudgeOwnerOid;

	String rolePirateOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		userPirateOwnerOid = addAndRecomputeUser(USER_PIRATE_OWNER_FILE, initTask, initResult);
		userJudgeOwnerOid = addAndRecomputeUser(USER_JUDGE_OWNER_FILE, initTask, initResult);
	}

	protected boolean approveObjectAdd() {
		return false;
	}

	@Test
	public void test010CreateRolePirate() throws Exception {
		final String TEST_NAME = "test010CreateRolePirate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		RoleType pirate = new RoleType(prismContext);
		pirate.setName(PolyStringType.fromOrig("pirate"));

		if (approveObjectAdd()) {
			createObject(TEST_NAME, pirate, false, true, userLead1Oid);
			rolePirateOid = searchObjectByName(RoleType.class, "pirate").getOid();
		} else {
			repoAddObject(pirate.asPrismObject(), result);
			rolePirateOid = pirate.getOid();
		}

		PrismReferenceValue pirateOwner = new PrismReferenceValue(rolePirateOid, RoleType.COMPLEX_TYPE);
		pirateOwner.setRelation(SchemaConstants.ORG_OWNER);
		executeChanges((ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(ObjectTypeUtil.createAssignmentTo(pirateOwner, prismContext))
				.asObjectDelta(userPirateOwnerOid),
				null, task, result);
		display("Pirate role", getRole(rolePirateOid));
		display("Pirate owner", getUser(userPirateOwnerOid));
	}

	@Test
	public void test100ModifyRolePirateDescription() throws Exception {
		final String TEST_NAME = "test100ModifyRolePirateDescription";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		ObjectDelta<RoleType> descriptionDelta = (ObjectDelta<RoleType>) DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_DESCRIPTION).replace("Bloody pirate")
				.asObjectDelta(rolePirateOid);
		ObjectDelta<RoleType> delta0 = ObjectDelta.createModifyDelta(rolePirateOid, Collections.emptyList(), RoleType.class, prismContext);
		//noinspection UnnecessaryLocalVariable
		ObjectDelta<RoleType> delta1 = descriptionDelta;
		ExpectedTask expectedTask = new ExpectedTask(null, "Modifying role pirate");
		ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userPirateOwnerOid, null, expectedTask);
		modifyObject(TEST_NAME, descriptionDelta, delta0, delta1, false, true, userPirateOwnerOid,
				Collections.singletonList(expectedTask), Collections.singletonList(expectedWorkItem),
				() -> {},
				() -> assertNull("Description is modified", getRoleSimple(rolePirateOid).getDescription()),
				() -> assertEquals("Description was NOT modified", "Bloody pirate", getRoleSimple(rolePirateOid).getDescription()));
	}

	@Test
	public void test200DeleteRolePirate() throws Exception {
		final String TEST_NAME = "test200DeleteRolePirate";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		ExpectedTask expectedTask = new ExpectedTask(null, "Deleting role pirate");
		ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userPirateOwnerOid, null, expectedTask);
		deleteObject(TEST_NAME, RoleType.class, rolePirateOid, false, true, userPirateOwnerOid,
				Collections.singletonList(expectedTask), Collections.singletonList(expectedWorkItem));
	}

	@Test
	public void zzzMarkAsNotInitialized() {
		display("Setting class as not initialized");
		unsetSystemInitialized();
	}

}
