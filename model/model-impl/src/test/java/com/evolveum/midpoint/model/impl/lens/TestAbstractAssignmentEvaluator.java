/**
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

import static com.evolveum.midpoint.prism.delta.PlusMinusZero.MINUS;
import static com.evolveum.midpoint.prism.delta.PlusMinusZero.PLUS;
import static com.evolveum.midpoint.prism.delta.PlusMinusZero.ZERO;
import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createDuration;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType.F_ADMINISTRATIVE_STATUS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.F_ACTIVATION;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ActivationUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class TestAbstractAssignmentEvaluator extends AbstractLensTest {

	@Autowired
	private RepositoryService repositoryService;

	@Autowired
	private ObjectResolver objectResolver;

	@Autowired
	private SystemObjectCache systemObjectCache;

	@Autowired
	private Clock clock;

	@Autowired
	private ActivationComputer activationComputer;

	@Autowired
	private MappingFactory mappingFactory;

	@Autowired
	private MappingEvaluator mappingEvaluator;

	@Autowired
	private Projector projector;

	public abstract File[] getRoleCorpFiles();

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}

	@Test
	public void test100Direct() throws Exception {
		final String TEST_NAME = "test100Direct";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_DIRECT_FILE);

		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
		userOdo.recompute();

		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.recompute();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, "testDirect", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment", evaluatedAssignment.debugDump());
		assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

		Construction<UserType> construction = evaluatedAssignment.getConstructionTriple().getZeroSet().iterator().next();
		display("Evaluated construction", construction);
		assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());

		assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
	}

	@Test
	public void test110DirectExpression() throws Exception {
		final String TEST_NAME = "test110DirectExpression";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_DIRECT_EXPRESSION_FILE);

		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
		userOdo.recompute();
		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);

		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.recompute();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, "testDirect", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment", evaluatedAssignment);
		assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

		Construction<UserType> construction = evaluatedAssignment.getConstructionTriple().getZeroSet().iterator().next();
		assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());

		assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
	}

	@Test
	public void test120DirectExpressionReplaceDescription() throws Exception {
		final String TEST_NAME = "test120DirectExpressionReplaceDescription";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
		AssignmentType assignmentType = unmarshallValueFromFile(ASSIGNMENT_DIRECT_EXPRESSION_FILE, AssignmentType.class);
		user.asObjectable().getAssignment().add(assignmentType.clone());

		ItemPath path = new ItemPath(
				new NameItemPathSegment(UserType.F_ASSIGNMENT),
				new IdItemPathSegment(123L),
				new NameItemPathSegment(AssignmentType.F_DESCRIPTION));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_JACK_OID,
				path, prismContext, "captain");
		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(user, userDelta, null);
		userOdo.recompute();
		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);

		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.setSubItemDeltas(userDelta.getModifications());
		assignmentIdi.recompute();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, "testDirect", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment);
		assertEquals(1,evaluatedAssignment.getConstructionTriple().size());
		PrismAsserts.assertParentConsistency(user);

		Construction<UserType> construction = evaluatedAssignment.getConstructionTriple().getZeroSet().iterator().next();
		assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());
		assertEquals(1,construction.getAttributeMappings().size());
		Mapping<PrismPropertyValue<String>, PrismPropertyDefinition<String>> attributeMapping = (Mapping<PrismPropertyValue<String>, PrismPropertyDefinition<String>>) construction.getAttributeMappings().iterator().next();
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = attributeMapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, "The best captain the world has ever seen");
	  	PrismAsserts.assertTripleMinus(outputTriple, "The best pirate the world has ever seen");

        // the same using other words

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO);
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS, "The best captain the world has ever seen");
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS, "The best pirate the world has ever seen");
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

	@Test
	public void test130DirectExpressionReplaceDescriptionFromNull() throws Exception {
		final String TEST_NAME = "test130DirectExpressionReplaceDescriptionFromNull";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
		AssignmentType assignmentType = unmarshallValueFromFile(ASSIGNMENT_DIRECT_EXPRESSION_FILE, AssignmentType.class);
		assignmentType.setDescription(null);
		user.asObjectable().getAssignment().add(assignmentType.clone());

//		// We need to make sure that the assignment has a parent
//		PrismContainerDefinition<AssignmentType> assignmentContainerDefinition = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
//		PrismContainer<AssignmentType> assignmentContainer = assignmentContainerDefinition.instantiate();
//		assignmentContainer.add(assignmentType.asPrismContainerValue().clone());

		ItemPath path = new ItemPath(
				new NameItemPathSegment(UserType.F_ASSIGNMENT),
				new IdItemPathSegment(123L),
				new NameItemPathSegment(AssignmentType.F_DESCRIPTION));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_JACK_OID,
				path, prismContext, "sailor");
		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(user, userDelta, null);
		userOdo.recompute();
		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);

		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.setSubItemDeltas(userDelta.getModifications());
		assignmentIdi.recompute();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, "testDirect", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment);
		assertEquals(1,evaluatedAssignment.getConstructionTriple().size());
		PrismAsserts.assertParentConsistency(user);

		Construction<UserType> construction = evaluatedAssignment.getConstructionTriple().getZeroSet().iterator().next();
		assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());
		assertEquals(1,construction.getAttributeMappings().size());
		PrismValueDeltaSetTripleProducer<PrismPropertyValue<String>, PrismPropertyDefinition<String>> attributeMapping = (PrismValueDeltaSetTripleProducer<PrismPropertyValue<String>, PrismPropertyDefinition<String>>) construction.getAttributeMappings().iterator().next();
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = attributeMapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, "The best sailor the world has ever seen");
	  	PrismAsserts.assertTripleMinus(outputTriple, "The best man the world has ever seen");

        // the same using other words

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO);
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS, "The best sailor the world has ever seen");
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS, "The best man the world has ever seen");
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
	}

    /*

    Explanation for roles structure (copied from role-corp-generic-metarole.xml)

        user-assignable roles:

          roles of unspecified type
          - Visitor
          - Customer

          roles of type: job
          - Contractor
          - Employee
            - Engineer (induces Employee)
            - Manager (induces Employee)

        metaroles:

          - Generic Metarole:                                   assigned to Visitor and Customer                            [ induces ri:location attribute - from user/locality ]
            - Job Metarole (induces Generic Metarole):          assigned to Contractor, Employee, Engineer, Manager         [ induces ri:title attribute - from role/name ]

     */

    @Test
    public void test140RoleVisitor() throws Exception {
        final String TEST_NAME = "test140RoleVisitor";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_VISITOR_FILE);

        ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
        userOdo.recompute();

        ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
        assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
        assignmentIdi.recompute();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, TEST_NAME, task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        display("Evaluated assignment",evaluatedAssignment.debugDump());
        assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO);
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");
        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test142RoleVisitorDisabledAssignment() throws Exception {
        final String TEST_NAME = "test142RoleVisitorDisabledAssignment";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_VISITOR_FILE);
        assignmentType.setActivation(ActivationUtil.createDisabled());

        ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
        userOdo.recompute();

        ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
        assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
        assignmentIdi.recompute();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, TEST_NAME, task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        display("Evaluated assignment",evaluatedAssignment.debugDump());
        assertEquals(1, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO);
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");
        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test150RoleEngineer() throws Exception {
        final String TEST_NAME = "test150RoleEngineer";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

        ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
        userOdo.recompute();

        ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
        assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
        assignmentIdi.recompute();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, "testRoleEngineer", task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        display("Evaluated assignment",evaluatedAssignment.debugDump());
        assertEquals(4, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Employee", "Engineer");
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 1, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    @Test
    public void test160AddRoleEngineer() throws Exception {
        final String TEST_NAME = "test160AddRoleEngineer";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

        AssignmentType assignmentForUser = assignmentType.clone();
        assignmentForUser.asPrismContainerValue().setParent(null);
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddContainer(UserType.class, USER_JACK_OID, UserType.F_ASSIGNMENT, prismContext, assignmentForUser.asPrismContainerValue());
        ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(user, userDelta, null);
        userOdo.recompute();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
        assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
        assignmentIdi.recompute();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, TEST_NAME, task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        display("Evaluated assignment",evaluatedAssignment.debugDump());
        assertEquals("Wrong number of constructions", 4, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        /*
         *  Here we observe an important thing about AssignmentEvaluator/AssignmentProcessor.
         *  The evaluator does not consider whether the assignment as such is being added or deleted or stays present.
         *  In all these cases all the constructions go into the ZERO set of constructions.
         *
         *  However, it considers changes in data that are used by assignments - either in conditions or in mappings.
         *  Changes of data used in mappings are demonstrated by testDirectExpressionReplaceDescription
         *  and testDirectExpressionReplaceDescriptionFromNull. Changes of data used in conditions are demonstrated by
         *  a couple of tests below.
         *
         *  Changes in assignment presence (add/delete) are reflected into plus/minus sets by AssignmentProcessor.
         */

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Employee", "Engineer");
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");

        assertEquals("Wrong number of admin GUI configs", 1, evaluatedAssignment.getAdminGuiConfigurations().size());
    }

    /**
     * jack has assigned role Manager.
     *
     * However, condition in job metarole for Manager is such that it needs "management"
     * to be present in user/costCenter in order to be active.
     */
    @Test
    public void test170RoleManagerChangeCostCenter() throws Exception {
        final String TEST_NAME = "test170RoleManagerChangeCostCenter";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_MANAGER_FILE);

        AssignmentType assignmentForUser = assignmentType.clone();
        assignmentForUser.asPrismContainerValue().setParent(null);
        user.asObjectable().getAssignment().add(assignmentForUser);

        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_JACK_OID,
                UserType.F_COST_CENTER, prismContext, "management");
        ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(user, userDelta, null);
        userOdo.recompute();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
        assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
        assignmentIdi.recompute();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, TEST_NAME, task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        display("Evaluated assignment",evaluatedAssignment.debugDump());
        assertEquals(4, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Employee");                   // because Employee's job metarole is active even if Manager's is not
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertConstruction(evaluatedAssignment, PLUS, "title", ZERO, "Manager");                    // because Manager's job metarole is originally not active
        assertConstruction(evaluatedAssignment, PLUS, "title", PLUS);
        assertConstruction(evaluatedAssignment, PLUS, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, MINUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");               // because Generic Metarole is active all the time
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");
    }

    /**
     * jack has assigned role Manager.
     *
     * However, condition in job metarole for Manager is such that it needs "management"
     * to be present in user/costCenter in order to be active.
     *
     * In this test we remove the value of "management" from jack.
     */
    @Test
    public void test180RoleManagerRemoveCostCenter() throws Exception {
        final String TEST_NAME = "test180RoleManagerRemoveCostCenter";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
        user.asObjectable().setCostCenter("management");
        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_MANAGER_FILE);

        AssignmentType assignmentForUser = assignmentType.clone();
        assignmentForUser.asPrismContainerValue().setParent(null);
        user.asObjectable().getAssignment().add(assignmentForUser);

        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_JACK_OID,
                UserType.F_COST_CENTER, prismContext);
        ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(user, userDelta, null);
        userOdo.recompute();
        AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
        assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
        assignmentIdi.recompute();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, TEST_NAME, task, result);
        evaluatedAssignment.evaluateConstructions(userOdo, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull(evaluatedAssignment);
        display("Evaluated assignment",evaluatedAssignment.debugDump());
        assertEquals(4, evaluatedAssignment.getConstructionTriple().size());
        PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

        assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Employee");                   // because Employee's job metarole is active even if Manager's is not
        assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
        assertConstruction(evaluatedAssignment, MINUS, "title", ZERO, "Manager");                    // because Manager's job metarole is not active any more
        assertConstruction(evaluatedAssignment, MINUS, "title", PLUS);
        assertConstruction(evaluatedAssignment, MINUS, "title", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "title");

        assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");               // because Generic Metarole is active all the time
        assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
        assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
        assertNoConstruction(evaluatedAssignment, PLUS, "location");
        assertNoConstruction(evaluatedAssignment, MINUS, "location");
    }

	/**
	 * Disable Engineer -> Employee inducement.
	 */

	@Test(enabled = false)
	public void test200DisableEngineerEmployeeInducement() throws Exception {
		final String TEST_NAME = "test200DisableEngineerEmployeeInducement";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// disable Engineer->Employee inducement
		ObjectDelta disableInducementDelta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_INDUCEMENT, 3, F_ACTIVATION, F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
				.asObjectDelta(ROLE_CORP_ENGINEER_OID);
		modelService.executeChanges(Collections.singletonList(disableInducementDelta),
				null, task, result);

		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

		AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
		userOdo.recompute();

		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.recompute();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, "testRoleEngineer", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment.debugDump());

		// TODO
//		assertEquals(2, evaluatedAssignment.getConstructions().size());
//		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
//
//		assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Engineer");
//		assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
//		assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
//		assertNoConstruction(evaluatedAssignment, PLUS, "title");
//		assertNoConstruction(evaluatedAssignment, MINUS, "title");
//
//		assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
//		assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
//		assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
//		assertNoConstruction(evaluatedAssignment, PLUS, "location");
//		assertNoConstruction(evaluatedAssignment, MINUS, "location");
//
//		assertEquals("Wrong number of admin GUI configs", 1, evaluatedAssignment.getAdminGuiConfigurations().size());
	}

	@Test(enabled = false)
	public void test299ReenableEngineerEmployeeInducement() throws Exception {
		final String TEST_NAME = "test299ReenableEngineerEmployeeInducement";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// disable Engineer->Employee inducement
		ObjectDelta enableInducementDelta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_INDUCEMENT, 3, F_ACTIVATION, F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
				.asObjectDelta(ROLE_CORP_ENGINEER_OID);
		modelService.executeChanges(Collections.singletonList(enableInducementDelta),
				null, task, result);
	}

	/**
	 * Jack is an Engineer which induces Employee. But role Employee is not valid anymore.
     */

	@Test
	public void test300DisableRoleEmployee() throws Exception {
		final String TEST_NAME = "test300DisableRoleEmployee";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// disable role Employee
		ObjectDelta disableEmployeeDelta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(ACTIVATION_ADMINISTRATIVE_STATUS_PATH).replace(ActivationStatusType.DISABLED)
				.asObjectDelta(ROLE_CORP_EMPLOYEE_OID);
		modelService.executeChanges(Collections.<ObjectDelta<? extends ObjectType>>singletonList(disableEmployeeDelta),
				null, task, result);

		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

		AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
		userOdo.recompute();

		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.recompute();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, "testRoleEngineer", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment.debugDump());
		assertEquals(2, evaluatedAssignment.getConstructionTriple().size());
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

		assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Engineer");
		assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
		assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
		assertNoConstruction(evaluatedAssignment, PLUS, "title");
		assertNoConstruction(evaluatedAssignment, MINUS, "title");

		assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
		assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
		assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
		assertNoConstruction(evaluatedAssignment, PLUS, "location");
		assertNoConstruction(evaluatedAssignment, MINUS, "location");

		assertEquals("Wrong number of admin GUI configs", 1, evaluatedAssignment.getAdminGuiConfigurations().size());
	}

	@Test
	public void test310DisableRoleEngineer() throws Exception {
		final String TEST_NAME = "test310DisableRoleEngineer";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// disable role Engineer
		ObjectDelta disableEngineerDelta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(ACTIVATION_ADMINISTRATIVE_STATUS_PATH).replace(ActivationStatusType.DISABLED)
				.asObjectDelta(ROLE_CORP_ENGINEER_OID);
		modelService.executeChanges(Collections.<ObjectDelta<? extends ObjectType>>singletonList(disableEngineerDelta),
				null, task, result);

		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

		AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_ENGINEER_FILE);

		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
		userOdo.recompute();

		ItemDeltaItem<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.recompute();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, PlusMinusZero.ZERO, false, userTypeJack, "testRoleEngineer", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment", evaluatedAssignment.debugDump());
		assertEquals(2, evaluatedAssignment.getConstructionTriple().size());
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());

		for (Construction<UserType> construction : evaluatedAssignment.getConstructionSet(ZERO)) {
			assertEquals("Wrong validity for " + construction, false, construction.isValid());
		}

		assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Engineer");
		assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
		assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
		assertNoConstruction(evaluatedAssignment, PLUS, "title");
		assertNoConstruction(evaluatedAssignment, MINUS, "title");

		assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
		assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
		assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
		assertNoConstruction(evaluatedAssignment, PLUS, "location");
		assertNoConstruction(evaluatedAssignment, MINUS, "location");

		assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
	}

	// MID-4251
	@Test
	public void test400UserFred() throws Exception {
		final String TEST_NAME = "test400UserFred";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		String pastTime = XmlTypeConverter.fromNow(createDuration("-P3D")).toString();
		String futureTime = XmlTypeConverter.fromNow(createDuration("P3D")).toString();
		UserType fred = new UserType(prismContext)
				.name("fred")
				.description(futureTime)
				.employeeType(DYNAMIC_ORG_ASSIGNMENT_EMPLOYEE_TYPE);
		addObject(fred.asPrismObject());
		PrismObject<UserType> fredAsCreated = findUserByUsername("fred");
		display("fred as created", fredAsCreated);

		ObjectDelta<UserType> descriptionDelta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_DESCRIPTION).replace(pastTime)
				.asObjectDeltaCast(fredAsCreated.getOid());

		LensContext<UserType> lensContext = createUserLensContext();
		fillContextWithUser(lensContext, fredAsCreated.getOid(), result);
		addFocusDeltaToContext(lensContext, descriptionDelta);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(lensContext, "test", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		display("Evaluated assignment", lensContext.getEvaluatedAssignmentTriple().debugDump());
//		assertEquals(2, evaluatedAssignment.getConstructionTriple().size());
//		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
//
//		for (Construction<UserType> construction : evaluatedAssignment.getConstructionSet(ZERO)) {
//			assertEquals("Wrong validity for " + construction, false, construction.isValid());
//		}
//
//		assertConstruction(evaluatedAssignment, ZERO, "title", ZERO, "Engineer");
//		assertConstruction(evaluatedAssignment, ZERO, "title", PLUS);
//		assertConstruction(evaluatedAssignment, ZERO, "title", MINUS);
//		assertNoConstruction(evaluatedAssignment, PLUS, "title");
//		assertNoConstruction(evaluatedAssignment, MINUS, "title");
//
//		assertConstruction(evaluatedAssignment, ZERO, "location", ZERO, "Caribbean");
//		assertConstruction(evaluatedAssignment, ZERO, "location", PLUS);
//		assertConstruction(evaluatedAssignment, ZERO, "location", MINUS);
//		assertNoConstruction(evaluatedAssignment, PLUS, "location");
//		assertNoConstruction(evaluatedAssignment, MINUS, "location");
//
//		assertEquals("Wrong number of admin GUI configs", 0, evaluatedAssignment.getAdminGuiConfigurations().size());
	}

	protected void assertNoConstruction(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, PlusMinusZero constructionSet, String attributeName) {
	        Collection<Construction<UserType>> constructions = evaluatedAssignment.getConstructionSet(constructionSet);
	        for (Construction construction : constructions) {
	            PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping = construction.getAttributeMapping(new QName(MidPointConstants.NS_RI, attributeName));
	            assertNull("Unexpected mapping for " + attributeName, mapping);
	        }
	    }


	protected void assertConstruction(EvaluatedAssignmentImpl<UserType> evaluatedAssignment, PlusMinusZero constructionSet, String attributeName, PlusMinusZero attributeSet, String... expectedValues) {
        Collection<Construction<UserType>> constructions = evaluatedAssignment.getConstructionSet(constructionSet);
        Set<String> realValues = new HashSet<>();
        for (Construction construction : constructions) {
            PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping = construction.getAttributeMapping(new QName(MidPointConstants.NS_RI, attributeName));
            if (mapping != null && mapping.getOutputTriple() != null) {
                Collection<? extends PrismPropertyValue<?>> valsInMapping = mapping.getOutputTriple().getSet(attributeSet);
                if (valsInMapping != null) {
                    for (PrismPropertyValue value : valsInMapping) {
                        if (value.getValue() instanceof String) {
                            realValues.add((String) value.getValue());
                        }
                    }
                }
            }
        }
        AssertJUnit.assertEquals("Wrong values", new HashSet<String>(Arrays.asList(expectedValues)), realValues);
    }

	protected AssignmentEvaluator<UserType> createAssignmentEvaluator() throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> userJack = userTypeJack.asPrismObject();
		ObjectDeltaObject<UserType> focusOdo = new ObjectDeltaObject<>(userJack, null, null);
		focusOdo.recompute();
		return createAssignmentEvaluator(focusOdo);
	}

	protected AssignmentEvaluator<UserType> createAssignmentEvaluator(ObjectDeltaObject<UserType> focusOdo) throws ObjectNotFoundException, SchemaException {
		LensContext<UserType> lensContext = createLensContext(UserType.class);
		LensFocusContext<UserType> focusContext = lensContext.getOrCreateFocusContext();
		focusContext.setObjectOld(focusOdo.getOldObject());
		focusContext.setPrimaryDelta(focusOdo.getObjectDelta());
		focusContext.setObjectCurrent(focusOdo.getOldObject());
		focusContext.setObjectNew(focusOdo.getNewObject());

		return new AssignmentEvaluator.Builder<UserType>()
				.repository(repositoryService)
				.focusOdo(focusOdo)
				.objectResolver(objectResolver)
				.systemObjectCache(systemObjectCache)
				.prismContext(prismContext)
				.activationComputer(activationComputer)
				.now(clock.currentTimeXMLGregorianCalendar())
				.mappingFactory(mappingFactory)
				.mappingEvaluator(mappingEvaluator)
				.lensContext(lensContext)
				.build();
	}

//	protected AssignmentEvaluator<UserType> createLoginModeAssignmentEvaluator(PrismObject<UserType> focus) throws ObjectNotFoundException, SchemaException {
//		return new AssignmentEvaluator.Builder<UserType>()
//				.repository(repositoryService)
//				.focusOdo(new ObjectDeltaObject<>(focus, null, focus))
//				.channel(null)
//				.objectResolver(objectResolver)
//				.systemObjectCache(systemObjectCache)
//				.prismContext(prismContext)
//				.mappingFactory(mappingFactory)
//				.mappingEvaluator(mappingEvaluator)
//				.activationComputer(activationComputer)
//				.now(clock.currentTimeXMLGregorianCalendar())
//				.loginMode(true)
//				// We do not have real lens context here. But the push methods in ModelExpressionThreadLocalHolder
//				// will need something to push on the stack. So give them context placeholder.
//				.lensContext(new LensContextPlaceholder<>(focus, prismContext))
//				.build();
//	}

}
