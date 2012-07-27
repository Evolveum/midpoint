/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.synchronizer;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.valueconstruction.ObjectDeltaObject;
import com.evolveum.midpoint.model.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.lens.Assignment;
import com.evolveum.midpoint.model.lens.AssignmentEvaluator;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-test.xml",
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-task.xml",
		"classpath:application-context-audit.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentEvaluator extends AbstractModelIntegrationTest {
	
	protected static final String TEST_RESOURCE_DIR_NAME = "src/test/resources/synchronizer";
	
	protected static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ = TEST_RESOURCE_DIR_NAME + "/user-jack-modify-add-assignment-account-opendj.xml";

	@Autowired(required=true)
	private RepositoryService repositoryService;
	
	@Autowired(required=true)
	private ObjectResolver objectResolver;
	
	public TestAssignmentEvaluator() throws JAXBException {
		super();
	}

	@Test
	public void testDirect() throws ObjectNotFoundException, SchemaException, FileNotFoundException, JAXBException, ExpressionEvaluationException {
		displayTestTile(this, "testDirect");
		
		// GIVEN
		OperationResult result = new OperationResult(TestAssignmentEvaluator.class.getName() + ".testDirect");
		AssignmentEvaluator assignmentEvaluator = createAssignmentEvaluator();
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
		
		AssignmentType assignmentType = unmarshallJaxbFromFile(TEST_RESOURCE_DIR_NAME + "/assignment-direct.xml", AssignmentType.class);
		
		// We need to make sure that the assignment has a parent
		PrismContainerDefinition assignmentContainerDefinition = userTypeJack.asPrismObject().getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismContainer assignmentContainer = assignmentContainerDefinition.instantiate();
		assignmentContainer.add(assignmentType.asPrismContainerValue());
		
		// WHEN
		Assignment evaluatedAssignment = assignmentEvaluator.evaluate(assignmentType, userTypeJack, "testDirect", result);
		
		// THEN
		assertNotNull(evaluatedAssignment);
		display("Evaluated assignment",evaluatedAssignment.dump());
		assertEquals(1,evaluatedAssignment.getAccountConstructions().size());
		PrismAsserts.assertParentConsistency(userTypeJack.asPrismObject());
	}
	
	private AssignmentEvaluator createAssignmentEvaluator() throws ObjectNotFoundException, SchemaException {
		AssignmentEvaluator assignmentEvaluator = new AssignmentEvaluator();
		assignmentEvaluator.setRepository(repositoryService);
		
		PrismObject<UserType> userJack = userTypeJack.asPrismObject();
		assignmentEvaluator.setUserOdo(new ObjectDeltaObject<UserType>(userJack, null, null));
		
		assignmentEvaluator.setObjectResolver(objectResolver);
		assignmentEvaluator.setPrismContext(prismContext);
		return assignmentEvaluator;
	}
	
}
