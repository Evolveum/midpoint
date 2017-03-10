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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.displayObjectTypeCollection;

/**
 * Comprehensive test of assignment evaluator and processor.
 *
 *            MMR1 -----------I------------------------------*
 *             ^                                             |
 *             |                                             I
 *             |                                             V
 *            MR1 -----------I-------------*-----> MR3      MR4
 *             ^        MR2 --I---*        |        |        |
 *             |         ^        I        I        I        I
 *             |         |        V        V        V        V
 *             R1 --I--> R2       R3       R4       R5       R6
 *             ^
 *             |
 *             |
 *            jack
 *
 * Straight line means assignment.
 * Line marked with "I" means inducement.
 *
 * Orders of these inducements are given by the levels of participants, so that each induced role belongs to jack, and each
 * induced metarole belongs to some role. So,
 * - inducement Rx->Ry is of order 1
 * - inducement MRx->MRy is of order 1
 * - inducement MRx->Ry is of order 2
 * - inducement MMRx->MRy is of order 1
 *
 * Each role has authorization, construction, focus mapping, focus policy rule and target policy rule.
 *
 * Each assignment and each role can be selectively enabled/disabled (via activation) and has its condition matched (none/old/new/old+new).
 *
 * @author mederly
 */
public class TestAssignmentProcessor2 extends AbstractLensTest {

    @Autowired
    private AssignmentProcessor assignmentProcessor;

    @Autowired
    private Clock clock;

    private RoleType role1, role2, role3, role4, role5, role6;
    private RoleType metarole1, metarole2, metarole3, metarole4;
    private RoleType metametarole1;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

		role1 = createRole(1, 1);
        role2 = createRole(1, 2);
        role3 = createRole(1, 3);
        role4 = createRole(1, 4);
        role5 = createRole(1, 5);
        role6 = createRole(1, 6);
		metarole1 = createRole(2, 1);
        metarole2 = createRole(2, 2);
        metarole3 = createRole(2, 3);
        metarole4 = createRole(2, 4);
		metametarole1 = createRole(3, 1);
		assign(role1, metarole1);
		assign(role2, metarole2);
		assign(metarole1, metametarole1);
		induce(role1, role2, 1);
		induce(metarole1, metarole3, 1);
		induce(metarole1, role4, 2);
		induce(metarole2, role3, 2);
		induce(metarole3, role5, 2);
        induce(metarole4, role6, 2);
		induce(metametarole1, metarole4, 2);

		List<ObjectType> roles = Arrays
				.asList(role1, role2, role3, role4, role5, role6, metarole1, metarole2, metarole3, metarole4, metametarole1);

//		for (ObjectType role : roles) {
//			System.out.println(prismContext.xmlSerializer().serialize(role.asPrismObject()));
//		}
		repoAddObjects(roles, initResult);
		recomputeAndRefreshObjects(roles, initTask, initResult);
		displayObjectTypeCollection("objects", roles);

	}

	private void induce(RoleType source, RoleType target, int inducementOrder) {
    	AssignmentType inducement = ObjectTypeUtil.createAssignmentTo(target.asPrismObject());
    	if (inducementOrder > 1) {
			inducement.setOrder(inducementOrder);
		}
		source.getInducement().add(inducement);
	}

	private void assign(RoleType source, RoleType target) {
    	AssignmentType assignment = ObjectTypeUtil.createAssignmentTo(target.asPrismObject());
		source.getAssignment().add(assignment);
	}

	private RoleType createRole(int level, int number) {
		String name = StringUtils.repeat('M', level-1) + "R" + number;
		String oid = getRoleOid(name);

		RoleType role = new RoleType(prismContext);
		role.setName(PolyStringType.fromOrig(name));
		role.setOid(oid);
		return role;
	}

	private String getRoleOid(String name) {
		return "99999999-0000-0000-0000-" + StringUtils.repeat('0', 12-name.length()) + name;
	}

	@Test
	public void test002ModifyUser() throws Exception {
		final String TEST_NAME = "test002ModifyUser";
		TestUtil.displayTestTile(this, TEST_NAME);

//		// GIVEN
//		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
//		OperationResult result = task.getResult();
//
//		LensContext<UserType> context = createUserAccountContext();
//		fillContextWithUser(context, USER_BARBOSSA_OID, result);
//		fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
//		addModificationToContextReplaceUserProperty(context, UserType.F_LOCALITY, new PolyString("Tortuga"));
//		context.recompute();
//
//		display("Input context", context);
//
//		assertFocusModificationSanity(context);
//
//		// WHEN
//		assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);
//
//		// THEN
//		display("Output context", context);
//		display("outbound processor result", result);
//		//		assertSuccess("Outbound processor failed (result)", result);
//
//		assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
//		assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
//		assertFalse("No account changes", context.getProjectionContexts().isEmpty());
//
//		Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
//		assertEquals(1, accountContexts.size());
//		LensProjectionContext accContext = accountContexts.iterator().next();
//		assertNull(accContext.getPrimaryDelta());
//
//		ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
//		assertNull("Account secondary delta sneaked in", accountSecondaryDelta);
//
//		assertNoDecision(accContext);
//		assertLegal(accContext);
//
//		assignmentProcessor.processAssignmentsAccountValues(accContext, result);
//
//		PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple =
//				accContext.getConstructionDeltaSetTriple();
//		display("accountConstructionDeltaSetTriple", accountConstructionDeltaSetTriple);
//
//		PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);
//		PrismAsserts.assertTripleNoPlus(accountConstructionDeltaSetTriple);
//		assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 2);
//
//		Construction zeroAccountConstruction = getZeroAccountConstruction(accountConstructionDeltaSetTriple, "Brethren account construction");
//
//		assertNoZeroAttributeValues(zeroAccountConstruction,
//				getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
//		assertPlusAttributeValues(zeroAccountConstruction,
//				getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Tortuga");
//		assertMinusAttributeValues(zeroAccountConstruction,
//				getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Caribbean");

	}

}
