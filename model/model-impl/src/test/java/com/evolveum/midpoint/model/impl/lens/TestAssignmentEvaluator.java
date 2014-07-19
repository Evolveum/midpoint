/*
 * Copyright (c) 2010-2013 Evolveum
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

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.AssignmentEvaluator;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignment;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
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
	
	public TestAssignmentEvaluator() throws JAXBException {
		super();
	}

	@Test
	public void testDirect() throws Exception {
		final String TEST_NAME = "testDirect";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentEvaluator.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
		AssignmentEvaluator assignmentEvaluator = createAssignmentEvaluator();
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
		
		AssignmentType assignmentType = unmarshallValueFromFile(ASSIGNMENT_DIRECT_FILE, AssignmentType.class);
		
		// We need to make sure that the assignment has a parent
		PrismContainerDefinition assignmentContainerDefinition = userTypeJack.asPrismObject().getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismContainer assignmentContainer = assignmentContainerDefinition.instantiate();
		assignmentContainer.add(assignmentType.asPrismContainerValue().clone());
		
		// WHEN
		EvaluatedAssignment evaluatedAssignment = assignmentEvaluator.evaluate(assignmentType, userTypeJack, "testDirect", task, result);
		
		// THEN
		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment.debugDump());
		assertEquals(1,evaluatedAssignment.getConstructions().size());
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
	}
	
	private AssignmentEvaluator createAssignmentEvaluator() throws ObjectNotFoundException, SchemaException {
		AssignmentEvaluator assignmentEvaluator = new AssignmentEvaluator();
		assignmentEvaluator.setRepository(repositoryService);
		
		PrismObject<UserType> userJack = userTypeJack.asPrismObject();
		assignmentEvaluator.setFocusOdo(new ObjectDeltaObject<UserType>(userJack, null, null));
		
		assignmentEvaluator.setObjectResolver(objectResolver);
		assignmentEvaluator.setPrismContext(prismContext);
		assignmentEvaluator.setActivationComputer(activationComputer);
		assignmentEvaluator.setNow(clock.currentTimeXMLGregorianCalendar());
		return assignmentEvaluator;
	}
	
}
