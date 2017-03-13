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
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentProcessor;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ActivationUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayObjectTypeCollection;
import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

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
 *             R1 --I--> R2       O3       R4       R5       R6
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
 * Each role has an authorization, GUI config, constructions, focus mappings, focus policy rules and target policy rules.
 *
 * Each assignment and each role can be selectively enabled/disabled (via activation) and has its condition matched (none/old/new/old+new).
 *
 * @author mederly
 */
public class TestAssignmentProcessor2 extends AbstractLensTest {

	private static final int CONSTRUCTION_LEVELS = 5;
	private static final int FOCUS_MAPPING_LEVELS = 5;
	private static final int POLICY_RULES_LEVELS = 5;

	public static final File RESOURCE_DUMMY_EMPTY_FILE = new File(TEST_DIR, "resource-dummy-empty.xml");
	public static final String RESOURCE_DUMMY_EMPTY_OID = "10000000-0000-0000-0000-00000000EEE4";
	public static final String RESOURCE_DUMMY_EMPTY_INSTANCE_NAME = "empty";

	@Autowired private AssignmentProcessor assignmentProcessor;
    @Autowired private Clock clock;

    private RoleType role1, role2, role4, role5, role6;
    private OrgType org3;
    private RoleType metarole1, metarole2, metarole3, metarole4;
    private RoleType metametarole1;
	private List<ObjectType> roles;

	private static final String R1_OID = getRoleOid("R1");

	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
		initDummyResourcePirate(RESOURCE_DUMMY_EMPTY_INSTANCE_NAME, RESOURCE_DUMMY_EMPTY_FILE,
				RESOURCE_DUMMY_EMPTY_OID, initTask, initResult);

		createObjects(false, initTask, initResult, null);
	}

	@Test
	public void test010AssignR1ToJack() throws Exception {
		final String TEST_NAME = "test010AssignR1ToJack";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createContextForRoleAssignment(USER_JACK_OID, R1_OID, null, null, result);

		// WHEN
		assignmentProcessor.processAssignmentsProjections(context, clock.currentTimeXMLGregorianCalendar(), task, result);

		// THEN
		display("Output context", context);
		display("Evaluated assignment triple", context.getEvaluatedAssignmentTriple());

		result.computeStatus();
		assertSuccess("Assignment processor failed (result)", result);

		Collection<EvaluatedAssignmentImpl> evaluatedAssignments = assertAssignmentTripleSetSize(context, 0, 1, 0);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = evaluatedAssignments.iterator().next();
		assertEquals("Wrong evaluatedAssignment.isValid", true, evaluatedAssignment.isValid());

		assertTargets(evaluatedAssignment, "R1 R2 O3 R4 R5 R6 MR1 MR2 MR3 MR4 MMR1", null, null, null, null, null);
		assertMembershipRef(evaluatedAssignment, "R1 R2 O3 R4 R5 R6");
		assertOrgRef(evaluatedAssignment, "O3");
		assertDelegation(evaluatedAssignment, null);

		// Constructions are named "role-level". We expect e.g. that from R1 we get a construction induced with order=1 (R1-1).
		String expectedItems = "R1-1 R2-1 O3-1 R4-1 R5-1 R6-1 MR1-2 MR2-2 MR3-2 MR4-2 MMR1-3";
		assertConstructions(evaluatedAssignment, expectedItems, null, null, null, null, null);
		assertFocusMappings(evaluatedAssignment, expectedItems);
		assertFocusPolicyRules(evaluatedAssignment, expectedItems);

		// TODO why R4-0 R5-0 R6-0 ? Sounds not good: when we are adding R1 assignment, we are not interested
		// in approval rules residing in induced roles, even if they are induced through a higher levels
		// (in the same way as we are not interested in R2-0 and R3-0)
		// MR3-1 MR4-1 seems to be OK; these are induced in a quite intuitive way (via MR1)
		String expectedThisTargetRules = "R1-0 R4-0 R5-0 R6-0 MR1-1 MR3-1 MR4-1 MMR1-2";
		String expectedTargetRules = expectedThisTargetRules + " R2-0 O3-0 MR2-1";
		assertTargetPolicyRules(evaluatedAssignment, getList(expectedTargetRules), getList(expectedThisTargetRules));
		assertAuthorizations(evaluatedAssignment, "R1 R2 O3 R4 R5 R6");
		assertGuiConfig(evaluatedAssignment, "R1 R2 O3 R4 R5 R6");
	}

	@Test
	public void test020AssignR1ToJackProjectorDisabled() throws Exception {
		final String TEST_NAME = "test020AssignR1ToJackProjectorDisabled";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createContextForRoleAssignment(USER_JACK_OID, R1_OID, null,
				a -> a.setActivation(ActivationUtil.createDisabled()), result);

		// WHEN
		projector.project(context, "", task, result);

		// THEN
		display("Output context", context);

		result.computeStatus();
		assertSuccess("Projector failed (result)", result);

		// MID-3679
		assertEquals("Wrong # of parentOrgRef entries", 0,
				context.getFocusContext().getObjectNew().asObjectable().getParentOrgRef().size());
		assertEquals("Wrong # of roleMembershipRef entries", 0,
				context.getFocusContext().getObjectNew().asObjectable().getRoleMembershipRef().size());
	}

	/**
	 * Now disable some roles. Their administrative status is simply set to DISABLED.
	 *
	 *            MMR1(D)---------I------------------------------*
	 *             ^                                             |
	 *             |                                             I
	 *             |                                             V
	 *            MR1 -----------I-------------*-----> MR3(D)   MR4
	 *             ^        MR2 --I---*        |        |        |
	 *             |         ^        I        I        I        I
	 *             |         |        V        V        V        V
	 *             R1 --I--> R2(D)    O3       R4(D)    R5       R6
	 *             ^
	 *             |
	 *             |
	 *            jack
	 */

	@Test
	public void test100DisableSomeRoles() throws Exception {
		final String TEST_NAME = "test100DisableSomeRoles";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		createObjects(true, task, result, () -> disableRoles("MMR1 R2 MR3 R4"));

		// THEN
		// TODO check e.g. membershipRef for roles
	}


	@Test
	public void test110AssignR1ToJack() throws Exception {
		final String TEST_NAME = "test010AssignR1ToJack";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createContextForRoleAssignment(USER_JACK_OID, R1_OID, null, null, result);

		// WHEN
		assignmentProcessor.processAssignmentsProjections(context, clock.currentTimeXMLGregorianCalendar(), task, result);

		// THEN
		display("Output context", context);
		display("Evaluated assignment triple", context.getEvaluatedAssignmentTriple());

		result.computeStatus();
		assertSuccess("Assignment processor failed (result)", result);

		Collection<EvaluatedAssignmentImpl> evaluatedAssignments = assertAssignmentTripleSetSize(context, 0, 1, 0);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = evaluatedAssignments.iterator().next();
		assertEquals("Wrong evaluatedAssignment.isValid", true, evaluatedAssignment.isValid());

		assertTargets(evaluatedAssignment, "R1 MR1", null, null, null, null, null);
		assertMembershipRef(evaluatedAssignment, "R1");
		assertOrgRef(evaluatedAssignment, null);
		assertDelegation(evaluatedAssignment, null);

		// Constructions are named "role-level". We expect e.g. that from R1 we get a construction induced with order=1 (R1-1).
		String expectedItems = "R1-1 MR1-2";
		assertConstructions(evaluatedAssignment, expectedItems, null, null, null, null, null);
		assertFocusMappings(evaluatedAssignment, expectedItems);
		assertFocusPolicyRules(evaluatedAssignment, expectedItems);

		String expectedThisTargetRules = "R1-0 MR1-1";
		String expectedTargetRules = expectedThisTargetRules;
		assertTargetPolicyRules(evaluatedAssignment, getList(expectedTargetRules), getList(expectedThisTargetRules));
		assertAuthorizations(evaluatedAssignment, "R1");
		assertGuiConfig(evaluatedAssignment, "R1");

	}

	/**
	 * In a similar way, let's disable some assignments. Their administrative status is simply set to DISABLED.
	 *
	 *            MMR1 -----------I------------------------------*
	 *             ^                                             |
	 *             |                                             I
	 *             |                                             V
	 *            MR1 -----------I-------------*-(D)-> MR3      MR4
	 *             ^        MR2 --I---*        |        |        |
	 *             |         ^        I        I        I        I(D)
	 *             |         |        V        V        V        V
	 *             R1-I(D)-> R2       O3       R4       R5       R6
	 *             ^
	 *             |
	 *             |
	 *            jack
	 */

	@Test
	public void test150DisableSomeAssignments() throws Exception {
		final String TEST_NAME = "test150DisableSomeAssignments";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		createObjects(true, task, result, () -> disableAssignments("MR4-R6 MR1-MR3 R1-R2"));

		// THEN
	}

	@Test
	public void test160AssignR1ToJack() throws Exception {
		final String TEST_NAME = "test160AssignR1ToJack";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createContextForRoleAssignment(USER_JACK_OID, R1_OID, null, null, result);

		// WHEN
		assignmentProcessor.processAssignmentsProjections(context, clock.currentTimeXMLGregorianCalendar(), task, result);

		// THEN
		display("Output context", context);
		display("Evaluated assignment triple", context.getEvaluatedAssignmentTriple());

		result.computeStatus();
		assertSuccess("Assignment processor failed (result)", result);

		Collection<EvaluatedAssignmentImpl> evaluatedAssignments = assertAssignmentTripleSetSize(context, 0, 1, 0);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = evaluatedAssignments.iterator().next();
		assertEquals("Wrong evaluatedAssignment.isValid", true, evaluatedAssignment.isValid());

		assertTargets(evaluatedAssignment, "R1 MR1 MMR1 MR4 R4", null, null, null, null, null);
		assertMembershipRef(evaluatedAssignment, "R1 R4");
		assertOrgRef(evaluatedAssignment, null);
		assertDelegation(evaluatedAssignment, null);

		String expectedItems = "R1-1 MR1-2 MMR1-3 MR4-2 R4-1";
		assertConstructions(evaluatedAssignment, expectedItems, null, null, null, null, null);
		assertFocusMappings(evaluatedAssignment, expectedItems);
		assertFocusPolicyRules(evaluatedAssignment, expectedItems);

		String expectedThisTargetRules = "R1-0 MR1-1 MMR1-2 MR4-1 R4-0";		// TODO why R4-0 ?
		String expectedTargetRules = expectedThisTargetRules;
		assertTargetPolicyRules(evaluatedAssignment, getList(expectedTargetRules), getList(expectedThisTargetRules));
		assertAuthorizations(evaluatedAssignment, "R1 R4");
		assertGuiConfig(evaluatedAssignment, "R1 R4");

	}

	/**
	 * Let's attach some conditions to assignments and roles. "+" condition means that it will be satisfied only in jack's new state.
	 * "-" condition will be satisfied only in jack's old state. "0" condition will be never satisfied.
	 *
	 *            MMR1------------I------------------------------*
	 *             ^                                             |
	 *            (+)                                            I
	 *             |                                             V
	 *         (+)MR1 -----------I-------------*-----> MR3(0)   MR4(-)
	 *             ^        MR2 --I---*        |        |        |
	 *            (+)        ^   (+)  I        I        I        I
	 *             |         |        V        V        V        V
	 *             R1 --I--> R2       O3       R4(D)    R5       R6
	 *             ^     (-)
	 *             |
	 *             |
	 *            jack
	 */

	@Test
	public void test200AddConditions() throws Exception {
		final String TEST_NAME = "test200AddConditions";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		createObjects(true, task, result, () -> {
			disableRoles("R4");
			addConditionToRoles("MR1+ MR30 MR4-");
			addConditionToAssignments("R1-MR1+ MR1-MMR1+ R1-R2- MR2-O3+");
		});

		// THEN
		// TODO check e.g. membershipRef for roles
	}


	@Test
	public void test210AssignR1ToJack() throws Exception {
		final String TEST_NAME = "test210AssignR1ToJack";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createContextForRoleAssignment(USER_JACK_OID, R1_OID, null, null, result);
		context.getFocusContext().swallowToPrimaryDelta(
				DeltaBuilder.deltaFor(UserType.class, prismContext)
						.item(UserType.F_NAME).replace(PolyString.fromOrig("jack1"))
						.asItemDelta());

		// WHEN
		assignmentProcessor.processAssignmentsProjections(context, clock.currentTimeXMLGregorianCalendar(), task, result);

		// THEN
		display("Output context", context);
		display("Evaluated assignment triple", context.getEvaluatedAssignmentTriple());

		result.computeStatus();
		assertSuccess("Assignment processor failed (result)", result);

		Collection<EvaluatedAssignmentImpl> evaluatedAssignments = assertAssignmentTripleSetSize(context, 0, 1, 0);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = evaluatedAssignments.iterator().next();
		assertEquals("Wrong evaluatedAssignment.isValid", true, evaluatedAssignment.isValid());

		// R4 is not in plusInvalid, because only directly assigned targets are listed among targets (see validityOverride)
		assertTargets(evaluatedAssignment, "R1", null, "MR1 MMR1", null, "R2 MR2", null);
		assertMembershipRef(evaluatedAssignment, "R1");
		assertOrgRef(evaluatedAssignment, null);
		assertDelegation(evaluatedAssignment, null);

		// R4-1 is not in plusInvalid (see above)
		assertConstructions(evaluatedAssignment, "R1-1", null, "MR1-2 MMR1-3", null, "R2-1 MR2-2", null);
		assertFocusMappings(evaluatedAssignment, "R1-1 MR1-2 MMR1-3");
		assertFocusPolicyRules(evaluatedAssignment, "R1-1 MR1-2 MMR1-3");

		assertTargetPolicyRules(evaluatedAssignment, "R1-0 MR1-1 MMR1-2", "R1-0 MR1-1 MMR1-2");
		assertAuthorizations(evaluatedAssignment, "R1");
		assertGuiConfig(evaluatedAssignment, "R1");
	}

	//region ============================================================= helper methods (preparing scenarios)

	private void createObjects(boolean deleteFirst, Task task, OperationResult result, Runnable adjustment) throws Exception {
		role1 = createRole(1, 1);
		role2 = createRole(1, 2);
		org3 = createOrg(3);
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
		induce(metarole2, org3, 2);
		induce(metarole3, role5, 2);
		induce(metarole4, role6, 2);
		induce(metametarole1, metarole4, 2);

		roles = Arrays.asList(role1, role2, org3, role4, role5, role6, metarole1, metarole2, metarole3, metarole4, metametarole1);

		if (adjustment != null) {
			adjustment.run();
		}

		// TODO implement repoAddObjects with overwrite option
		if (deleteFirst) {
			for (ObjectType role : roles) {
				repositoryService.deleteObject(role.getClass(), role.getOid(), result);
			}
		}

		repoAddObjects(roles, result);
		recomputeAndRefreshObjects(roles, task, result);
		displayObjectTypeCollection("objects", roles);
	}

	// methods for creation-time manipulation with roles and assignments

	private void disableRoles(String text) {
		for (String name : getList(text)) {
			AbstractRoleType role = findRole(name);
			if (role.getActivation() == null) {
				role.setActivation(new ActivationType(prismContext));
			}
			role.getActivation().setAdministrativeStatus(ActivationStatusType.DISABLED);
		}
	}

	private void disableAssignments(String text) {
		for (String assignmentText : getList(text)) {
			AssignmentType assignment = findAssignmentOrInducement(assignmentText);
			if (assignment.getActivation() == null) {
				assignment.setActivation(new ActivationType(prismContext));
			}
			assignment.getActivation().setAdministrativeStatus(ActivationStatusType.DISABLED);
		}
	}

	private void addConditionToRoles(String text) {
		for (String item : getList(text)) {
			String name = StringUtils.substring(item, 0, -1);
			char conditionType = item.charAt(item.length() - 1);
			AbstractRoleType role = findRole(name);
			role.setCondition(createCondition(conditionType));
		}
	}

	private void addConditionToAssignments(String text) {
		for (String item : getList(text)) {
			String assignmentText = StringUtils.substring(item, 0,-1);
			char conditionType = item.charAt(item.length() - 1);
			AssignmentType assignment = findAssignmentOrInducement(assignmentText);
			assignment.setCondition(createCondition(conditionType));
		}
	}

	private MappingType createCondition(char conditionType) {
		ScriptExpressionEvaluatorType script = new ScriptExpressionEvaluatorType();
		switch (conditionType) {
			case '+': script.setCode("basic.stringify(name) == 'jack1'"); break;
			case '-': script.setCode("basic.stringify(name) == 'jack'"); break;
			case '0': script.setCode("basic.stringify(name) == 'never there'"); break;
			default: throw new AssertionError(conditionType);
		}
		ExpressionType expression = new ExpressionType();
		expression.getExpressionEvaluator().add(new ObjectFactory().createScript(script));
		VariableBindingDefinitionType source = new VariableBindingDefinitionType();
		source.setPath(new ItemPath(UserType.F_NAME).asItemPathType());
		MappingType rv = new MappingType();
		rv.setExpression(expression);
		rv.getSource().add(source);
		return rv;
	}

	private void induce(AbstractRoleType source, AbstractRoleType target, int inducementOrder) {
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
		return prepareAbstractRole(new RoleType(prismContext), level, number, "R");
	}

	private OrgType createOrg(int number) {
		return prepareAbstractRole(new OrgType(prismContext), 1, number, "O");
	}

	private <R extends AbstractRoleType> R prepareAbstractRole(R abstractRole, int level, int number, String nameSymbol) {
		String name = StringUtils.repeat('M', level-1) + nameSymbol + number;
		String oid = getRoleOid(name);

		abstractRole.setName(PolyStringType.fromOrig(name));
		abstractRole.setOid(oid);

		// constructions
		for (int i = 0; i <= CONSTRUCTION_LEVELS; i++) {
			ConstructionType c = new ConstructionType(prismContext);
			c.setDescription(name + "-" + i);
			c.setResourceRef(ObjectTypeUtil.createObjectRef(RESOURCE_DUMMY_EMPTY_OID, ObjectTypes.RESOURCE));
			AssignmentType a = new AssignmentType(prismContext);
			a.setDescription("Assignment for " + c.getDescription());
			a.setConstruction(c);
			addAssignmentOrInducement(abstractRole, i, a);
		}

		// focus mappings
		for (int i = 0; i <= FOCUS_MAPPING_LEVELS; i++) {
			MappingType mapping = new MappingType();
			mapping.setName(name + "-" + i);
			VariableBindingDefinitionType source = new VariableBindingDefinitionType();
			source.setPath(new ItemPath(UserType.F_NAME).asItemPathType());
			mapping.getSource().add(source);
			VariableBindingDefinitionType target = new VariableBindingDefinitionType();
			target.setPath(new ItemPath(UserType.F_DESCRIPTION).asItemPathType());
			mapping.setTarget(target);
			MappingsType mappings = new MappingsType(prismContext);
			mappings.getMapping().add(mapping);
			AssignmentType a = new AssignmentType(prismContext);
			a.setFocusMappings(mappings);
			addAssignmentOrInducement(abstractRole, i, a);
		}

		// policy rules
		for (int i = 0; i <= POLICY_RULES_LEVELS; i++) {
			PolicyRuleType rule = new PolicyRuleType(prismContext);
			rule.setName(name + "-" + i);
			AssignmentType a = new AssignmentType(prismContext);
			a.setPolicyRule(rule);
			addAssignmentOrInducement(abstractRole, i, a);
		}

		// authorization
		AuthorizationType authorization = new AuthorizationType(prismContext);
		authorization.getAction().add(name);
		abstractRole.getAuthorization().add(authorization);

		// admin gui config
		AdminGuiConfigurationType guiConfig = new AdminGuiConfigurationType();
		guiConfig.setPreferredDataLanguage(name);
		abstractRole.setAdminGuiConfiguration(guiConfig);
		return abstractRole;
	}

	private <R extends AbstractRoleType> void addAssignmentOrInducement(R abstractRole, int order, AssignmentType assignment) {
		if (order == 0) {
			abstractRole.getAssignment().add(assignment);
		} else {
			assignment.setOrder(order);
			abstractRole.getInducement().add(assignment);
		}
	}

	private static String getRoleOid(String name) {
		return "99999999-0000-0000-0000-" + StringUtils.repeat('0', 12-name.length()) + name;
	}

	@NotNull
	private LensContext<UserType> createContextForRoleAssignment(String userOid, String roleOid, QName relation,
			Consumer<AssignmentType> modificationBlock, OperationResult result)
			throws SchemaException, ObjectNotFoundException, JAXBException {
		LensContext<UserType> context = createUserAccountContext();
		fillContextWithUser(context, userOid, result);
		addFocusDeltaToContext(context, createAssignmentUserDelta(USER_JACK_OID, roleOid, RoleType.COMPLEX_TYPE, relation,
				modificationBlock, true));
		context.recompute();
		display("Input context", context);
		assertFocusModificationSanity(context);
		return context;
	}

	//endregion
	//region ============================================================= helper methods (asserts)

	private void assertMembershipRef(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, String text) {
		assertPrismRefValues("membershipRef", evaluatedAssignment.getMembershipRefVals(), findRoles(text));
	}

	private void assertDelegation(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, String text) {
		assertPrismRefValues("delegationRef", evaluatedAssignment.getDelegationRefVals(), findRoles(text));
	}

	private void assertOrgRef(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, String text) {
		assertPrismRefValues("orgRef", evaluatedAssignment.getOrgRefVals(), findRoles(text));
	}

	private void assertAuthorizations(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, String text) {
		List<String> expected = getList(text);
		assertEquals("Wrong # of authorizations", expected.size(), evaluatedAssignment.getAuthorizations().size());
		assertEquals("Wrong authorizations", new HashSet<>(expected),
				evaluatedAssignment.getAuthorizations().stream().map(a -> a.getAction().get(0)).collect(Collectors.toSet()));
	}

	private void assertGuiConfig(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, String text) {
		List<String> expected = getList(text);
		assertEquals("Wrong # of gui configurations", expected.size(), evaluatedAssignment.getAdminGuiConfigurations().size());
		assertEquals("Wrong gui authorizations", new HashSet<>(expected),
				evaluatedAssignment.getAdminGuiConfigurations().stream().map(g -> g.getPreferredDataLanguage()).collect(Collectors.toSet()));
	}

	private void assertFocusMappings(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, String expectedItems) {
		assertFocusMappings(evaluatedAssignment, getList(expectedItems));
	}

	private void assertFocusMappings(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, Collection<String> expectedItems) {
		expectedItems = CollectionUtils.emptyIfNull(expectedItems);
		assertEquals("Wrong # of focus mappings", expectedItems.size(), evaluatedAssignment.getFocusMappings().size());
		assertEquals("Wrong focus mappings", new HashSet<>(expectedItems),
				evaluatedAssignment.getFocusMappings().stream().map(m -> m.getMappingType().getName()).collect(Collectors.toSet()));
		// TODO look at the content of the mappings (e.g. zero, plus, minus sets)
	}

	private void assertFocusPolicyRules(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, String expectedItems) {
		assertFocusPolicyRules(evaluatedAssignment, getList(expectedItems));
	}

	private void assertFocusPolicyRules(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, Collection<String> expectedItems) {
		expectedItems = CollectionUtils.emptyIfNull(expectedItems);
		assertEquals("Wrong # of focus policy rules", expectedItems.size(), evaluatedAssignment.getFocusPolicyRules().size());
		assertEquals("Wrong focus policy rules", new HashSet<>(expectedItems),
				evaluatedAssignment.getFocusPolicyRules().stream().map(r -> r.getName()).collect(Collectors.toSet()));
	}

	private void assertTargetPolicyRules(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, String expectedTargetItems, String expectedThisTargetItems) {
		assertTargetPolicyRules(evaluatedAssignment, getList(expectedTargetItems), getList(expectedThisTargetItems));
	}

	private void assertTargetPolicyRules(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, Collection<String> expectedTargetItems, Collection<String> expectedThisTargetItems) {
		expectedTargetItems = CollectionUtils.emptyIfNull(expectedTargetItems);
		expectedThisTargetItems = CollectionUtils.emptyIfNull(expectedThisTargetItems);
		assertEquals("Wrong # of target policy rules", expectedTargetItems.size(), evaluatedAssignment.getTargetPolicyRules().size());
		assertEquals("Wrong # of this target policy rules", expectedThisTargetItems.size(), evaluatedAssignment.getThisTargetPolicyRules().size());
		assertEquals("Wrong target policy rules", new HashSet<>(expectedTargetItems),
				evaluatedAssignment.getTargetPolicyRules().stream().map(r -> r.getName()).collect(Collectors.toSet()));
		assertEquals("Wrong this target policy rules", new HashSet<>(expectedThisTargetItems),
				evaluatedAssignment.getThisTargetPolicyRules().stream().map(r -> r.getName()).collect(Collectors.toSet()));

		// testing (strange) condition on thisTarget vs target policy rules
		outer: for (EvaluatedPolicyRule localRule : evaluatedAssignment.getThisTargetPolicyRules()) {
			for (EvaluatedPolicyRule rule : evaluatedAssignment.getTargetPolicyRules()) {
				if (rule == localRule) {
					continue outer;
				}
			}
			fail("This target rule " + localRule + " is not among target rules: " + evaluatedAssignment.getTargetPolicyRules());
		}
	}

	private void assertTargets(EvaluatedAssignmentImpl<UserType> evaluatedAssignment,
			String zeroValid, String zeroInvalid,
			String plusValid, String plusInvalid,
			String minusValid, String minusInvalid) {
		assertTargets(evaluatedAssignment, getList(zeroValid), getList(zeroInvalid),
				getList(plusValid), getList(plusInvalid), getList(minusValid), getList(minusInvalid));
	}

	private void assertTargets(EvaluatedAssignmentImpl<UserType> evaluatedAssignment,
			List<String> zeroValid, List<String> zeroInvalid,
			List<String> plusValid, List<String> plusInvalid,
			List<String> minusValid, List<String> minusInvalid) {
		assertTargets("zero", evaluatedAssignment.getRoles().getZeroSet(), zeroValid, zeroInvalid);
		assertTargets("plus", evaluatedAssignment.getRoles().getPlusSet(), plusValid, plusInvalid);
		assertTargets("minus", evaluatedAssignment.getRoles().getMinusSet(), minusValid, minusInvalid);
	}

	private void assertTargets(String type, Collection<EvaluatedAssignmentTargetImpl> targets, List<String> expectedValid,
			List<String> expectedInvalid) {
		targets = CollectionUtils.emptyIfNull(targets);
		Collection<EvaluatedAssignmentTargetImpl> realValid = targets.stream().filter(t -> t.isValid()).collect(Collectors.toList());
		Collection<EvaluatedAssignmentTargetImpl> realInvalid = targets.stream().filter(t -> !t.isValid()).collect(Collectors.toList());
		assertEquals("Wrong # of valid targets in " + type + " set", expectedValid.size(), realValid.size());
		assertEquals("Wrong # of invalid targets in " + type + " set", expectedInvalid.size(), realInvalid.size());
		assertEquals("Wrong valid targets in " + type + " set", new HashSet<>(expectedValid),
				realValid.stream().map(t -> t.getTarget().getName().getOrig()).collect(Collectors.toSet()));
		assertEquals("Wrong invalid targets in " + type + " set", new HashSet<>(expectedInvalid),
				realInvalid.stream().map(t -> t.getTarget().getName().getOrig()).collect(Collectors.toSet()));
	}

	private void assertConstructions(EvaluatedAssignmentImpl<UserType> evaluatedAssignment,
			String zeroValid, String zeroInvalid,
			String plusValid, String plusInvalid,
			String minusValid, String minusInvalid) {
		assertConstructions(evaluatedAssignment, getList(zeroValid), getList(zeroInvalid),
				getList(plusValid), getList(plusInvalid), getList(minusValid), getList(minusInvalid));
	}

	private void assertConstructions(EvaluatedAssignmentImpl<UserType> evaluatedAssignment,
			List<String> zeroValid, List<String> zeroInvalid,
			List<String> plusValid, List<String> plusInvalid,
			List<String> minusValid, List<String> minusInvalid) {
		assertConstructions("zero", evaluatedAssignment.getConstructionSet(PlusMinusZero.ZERO), zeroValid, zeroInvalid);
		assertConstructions("plus", evaluatedAssignment.getConstructionSet(PlusMinusZero.PLUS), plusValid, plusInvalid);
		assertConstructions("minus", evaluatedAssignment.getConstructionSet(PlusMinusZero.MINUS), minusValid, minusInvalid);
	}

	private void assertConstructions(String type, Collection<Construction<UserType>> constructions, List<String> valid0,
			List<String> invalid0) {
		constructions = CollectionUtils.emptyIfNull(constructions);
		Collection<String> expectedValid = CollectionUtils.emptyIfNull(valid0);
		Collection<String> expectedInvalid = CollectionUtils.emptyIfNull(invalid0);
		Collection<Construction<UserType>> realValid = constructions.stream().filter(c -> c.isValid()).collect(Collectors.toList());
		Collection<Construction<UserType>> realInvalid = constructions.stream().filter(c -> !c.isValid()).collect(Collectors.toList());
		assertEquals("Wrong # of valid constructions in " + type + " set", expectedValid.size(), realValid.size());
		assertEquals("Wrong # of invalid constructions in " + type + " set", expectedInvalid.size(), realInvalid.size());
		assertEquals("Wrong valid constructions in " + type + " set", new HashSet<>(expectedValid),
				realValid.stream().map(c -> c.getDescription()).collect(Collectors.toSet()));
		assertEquals("Wrong invalid constructions in " + type + " set", new HashSet<>(expectedInvalid),
				realInvalid.stream().map(c -> c.getDescription()).collect(Collectors.toSet()));
	}

	private Collection<EvaluatedAssignmentImpl> assertAssignmentTripleSetSize(LensContext<UserType> context, int zero, int plus, int minus) {
		assertEquals("Wrong size of assignment triple zero set", zero, CollectionUtils.size(context.getEvaluatedAssignmentTriple().getZeroSet()));
		assertEquals("Wrong size of assignment triple plus set", plus, CollectionUtils.size(context.getEvaluatedAssignmentTriple().getPlusSet()));
		assertEquals("Wrong size of assignment triple minus set", minus, CollectionUtils.size(context.getEvaluatedAssignmentTriple().getMinusSet()));
		return context.getEvaluatedAssignmentTriple().getAllValues();
	}

	//endregion
	//region ============================================================= helper methods (misc)

	private AssignmentType findAssignmentOrInducement(String assignmentText) {
		String[] split = StringUtils.split(assignmentText, "-");
		AbstractRoleType source = findRole(split[0]);
		AbstractRoleType target = findRole(split[1]);
		return findAssignmentOrInducement(source, target);
	}

	private AssignmentType findAssignmentOrInducement(AbstractRoleType source, AbstractRoleType target) {
		return Stream.concat(source.getAssignment().stream(), source.getInducement().stream())
				.filter(a -> a.getTargetRef() != null && target.getOid().equals(a.getTargetRef().getOid()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(source + " contains no assignment/inducement to " + target));
	}

	private AbstractRoleType findRole(String name) {
		return (AbstractRoleType) roles.stream().filter(r -> name.equals(r.getName().getOrig())).findFirst()
				.orElseThrow(() -> new IllegalStateException("No role " + name));
	}

	private List<AbstractRoleType> findRoles(String text) {
		return getList(text).stream().map(n -> findRole(n)).collect(Collectors.toList());
	}

	private List<String> getList(String text) {
		return text != null ? Arrays.asList(StringUtils.split(text)) : Collections.emptyList();
	}

	//endregion
}
