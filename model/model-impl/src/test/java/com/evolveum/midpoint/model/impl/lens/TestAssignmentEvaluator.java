/*
 * Copyright (c) 2010-2014 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.util.Collection;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignment;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentEvaluator extends AbstractLensTest {

	@Autowired(required=true)
	private RepositoryService repositoryService;
	
	@Autowired(required=true)
	private ObjectResolver objectResolver;
	
	@Autowired(required=true)
	private Clock clock;
	
	@Autowired(required=true)
	private ActivationComputer activationComputer;

	@Autowired(required=true)
	private MappingFactory mappingFactory;
	
	@Test
	public void testDirect() throws Exception {
		final String TEST_NAME = "testDirect";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator();
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
		
		AssignmentType assignmentType = unmarshallValueFromFile(ASSIGNMENT_DIRECT_FILE, AssignmentType.class);
		
		// We need to make sure that the assignment has a parent
		PrismContainerDefinition<AssignmentType> assignmentContainerDefinition = userTypeJack.asPrismObject().getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> assignmentContainer = assignmentContainerDefinition.instantiate();
		assignmentContainer.add(assignmentType.asPrismContainerValue().clone());
		
		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
		userOdo.recompute();
		
		ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.recompute();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignment<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, false, userTypeJack, "testDirect", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment.debugDump());
		assertEquals(1,evaluatedAssignment.getConstructions().size());
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
		
		Construction<UserType> construction = evaluatedAssignment.getConstructions().getZeroSet().iterator().next();
		display("Evaluated construction", construction);
		assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());
	}
	
	@Test
	public void testDirectExpression() throws Exception {
		final String TEST_NAME = "testDirectExpression";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
		
		AssignmentType assignmentType = unmarshallValueFromFile(ASSIGNMENT_DIRECT_EXPRESSION_FILE, AssignmentType.class);
		
		// We need to make sure that the assignment has a parent
		PrismContainerDefinition<AssignmentType> assignmentContainerDefinition = userTypeJack.asPrismObject().getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> assignmentContainer = assignmentContainerDefinition.instantiate();
		assignmentContainer.add(assignmentType.asPrismContainerValue().clone());
		
		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(userTypeJack.asPrismObject(), null, null);
		userOdo.recompute();
		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);
		
		ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.recompute();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignment<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, false, userTypeJack, "testDirect", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment);
		assertEquals(1,evaluatedAssignment.getConstructions().size());
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
		
		Construction<UserType> construction = evaluatedAssignment.getConstructions().getZeroSet().iterator().next();
		assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());
	}
	
	@Test
	public void testDirectExpressionReplaceDescription() throws Exception {
		final String TEST_NAME = "testDirectExpressionReplaceDescription";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
		AssignmentType assignmentType = unmarshallValueFromFile(ASSIGNMENT_DIRECT_EXPRESSION_FILE, AssignmentType.class);
		user.asObjectable().getAssignment().add(assignmentType.clone());
		
		// We need to make sure that the assignment has a parent
		PrismContainerDefinition<AssignmentType> assignmentContainerDefinition = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> assignmentContainer = assignmentContainerDefinition.instantiate();
		assignmentContainer.add(assignmentType.asPrismContainerValue().clone());
		
		ItemPath path = new ItemPath(
				new NameItemPathSegment(UserType.F_ASSIGNMENT),
				new IdItemPathSegment(123L),
				new NameItemPathSegment(AssignmentType.F_DESCRIPTION));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_JACK_OID, 
				path, prismContext, "captain");
		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(user, userDelta, null);
		userOdo.recompute();
		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);
		
		ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.setSubItemDeltas((Collection)userDelta.getModifications());
		assignmentIdi.recompute();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignment<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, false, userTypeJack, "testDirect", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment);
		assertEquals(1,evaluatedAssignment.getConstructions().size());
		PrismAsserts.assertParentConsistency(user);
		
		Construction<UserType> construction = evaluatedAssignment.getConstructions().getZeroSet().iterator().next();
		assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());
		assertEquals(1,construction.getAttributeMappings().size());
		Mapping<PrismPropertyValue<String>> attributeMapping = (Mapping<PrismPropertyValue<String>>) construction.getAttributeMappings().iterator().next();
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = attributeMapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, "The best captain the world has ever seen");
	  	PrismAsserts.assertTripleMinus(outputTriple, "The best pirate the world has ever seen");
		
	}
	
	@Test
	public void testDirectExpressionReplaceDescriptionFromNull() throws Exception {
		final String TEST_NAME = "testDirectExpressionReplaceDescriptionFromNull";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<UserType> user = userTypeJack.asPrismObject().clone();
		AssignmentType assignmentType = unmarshallValueFromFile(ASSIGNMENT_DIRECT_EXPRESSION_FILE, AssignmentType.class);
		assignmentType.setDescription(null);
		user.asObjectable().getAssignment().add(assignmentType.clone());
		
		// We need to make sure that the assignment has a parent
		PrismContainerDefinition<AssignmentType> assignmentContainerDefinition = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismContainer<AssignmentType> assignmentContainer = assignmentContainerDefinition.instantiate();
		assignmentContainer.add(assignmentType.asPrismContainerValue().clone());
		
		ItemPath path = new ItemPath(
				new NameItemPathSegment(UserType.F_ASSIGNMENT),
				new IdItemPathSegment(123L),
				new NameItemPathSegment(AssignmentType.F_DESCRIPTION));
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_JACK_OID, 
				path, prismContext, "sailor");
		ObjectDeltaObject<UserType> userOdo = new ObjectDeltaObject<>(user, userDelta, null);
		userOdo.recompute();
		AssignmentEvaluator<UserType> assignmentEvaluator = createAssignmentEvaluator(userOdo);
		
		ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi = new ItemDeltaItem<>();
		assignmentIdi.setItemOld(LensUtil.createAssignmentSingleValueContainerClone(assignmentType));
		assignmentIdi.setSubItemDeltas((Collection)userDelta.getModifications());
		assignmentIdi.recompute();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EvaluatedAssignment<UserType> evaluatedAssignment = assignmentEvaluator.evaluate(assignmentIdi, false, userTypeJack, "testDirect", task, result);
		evaluatedAssignment.evaluateConstructions(userOdo, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment);
		assertEquals(1,evaluatedAssignment.getConstructions().size());
		PrismAsserts.assertParentConsistency(user);
		
		Construction<UserType> construction = evaluatedAssignment.getConstructions().getZeroSet().iterator().next();
		assertNotNull("No object class definition in construction", construction.getRefinedObjectClassDefinition());
		assertEquals(1,construction.getAttributeMappings().size());
		Mapping<PrismPropertyValue<String>> attributeMapping = (Mapping<PrismPropertyValue<String>>) construction.getAttributeMappings().iterator().next();
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = attributeMapping.getOutputTriple();
		PrismAsserts.assertTripleNoZero(outputTriple);
	  	PrismAsserts.assertTriplePlus(outputTriple, "The best sailor the world has ever seen");
	  	PrismAsserts.assertTripleMinus(outputTriple, "The best man the world has ever seen");
		
	}
	
	private AssignmentEvaluator<UserType> createAssignmentEvaluator() throws ObjectNotFoundException, SchemaException {
		PrismObject<UserType> userJack = userTypeJack.asPrismObject();
		return createAssignmentEvaluator(new ObjectDeltaObject<UserType>(userJack, null, null));
	}
	
	private AssignmentEvaluator<UserType> createAssignmentEvaluator(ObjectDeltaObject<UserType> focusOdo) throws ObjectNotFoundException, SchemaException {
		AssignmentEvaluator<UserType> assignmentEvaluator = new AssignmentEvaluator<>();
		assignmentEvaluator.setRepository(repositoryService);
		assignmentEvaluator.setFocusOdo(focusOdo);
		assignmentEvaluator.setObjectResolver(objectResolver);
		assignmentEvaluator.setPrismContext(prismContext);
		assignmentEvaluator.setActivationComputer(activationComputer);
		assignmentEvaluator.setNow(clock.currentTimeXMLGregorianCalendar());
		assignmentEvaluator.setMappingFactory(mappingFactory);
		// Fake
		assignmentEvaluator.setLensContext(new LensContext<>(UserType.class, prismContext, provisioningService));
		return assignmentEvaluator;
	}
}
