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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProjectorPersona extends AbstractLensTest {

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		setDefaultUserTemplate(USER_TEMPLATE_OID);
		addObject(ROLE_PERSONA_ADMIN_FILE);
		InternalMonitor.reset();
//		InternalMonitor.setTraceShadowFetchOperation(true);
	}

	@Test
    public void test100AssignRolePersonaAdminToJack() throws Exception {
		final String TEST_NAME = "test100AssignRolePersonaAdminToJack";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjectorPersona.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        ObjectDelta<UserType> focusDelta = createAssignmentFocusDelta(UserType.class, USER_JACK_OID,
        		ROLE_PERSONA_ADMIN_OID, RoleType.COMPLEX_TYPE, null, null, null, true);
        addFocusDeltaToContext(context, focusDelta);

        display("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        display("Output context", context);
		assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

		assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
		assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta", ActivationStatusType.ENABLED);
		assertTrue("Unexpected projection changes", context.getProjectionContexts().isEmpty());

		DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		assertNotNull("No evaluatedAssignmentTriple", evaluatedAssignmentTriple);

		assertTrue("Unexpected evaluatedAssignmentTriple zero set", evaluatedAssignmentTriple.getZeroSet().isEmpty());
		assertTrue("Unexpected evaluatedAssignmentTriple minus set", evaluatedAssignmentTriple.getMinusSet().isEmpty());
		assertNotNull("No evaluatedAssignmentTriple plus set", evaluatedAssignmentTriple.getPlusSet());
		assertEquals("Wrong size of evaluatedAssignmentTriple plus set", 1, evaluatedAssignmentTriple.getPlusSet().size());
		EvaluatedAssignmentImpl<UserType> evaluatedAssignment = (EvaluatedAssignmentImpl<UserType>) evaluatedAssignmentTriple.getPlusSet().iterator().next();
		display("evaluatedAssignment", evaluatedAssignment);
		assertNotNull("No evaluatedAssignment", evaluatedAssignment);
		DeltaSetTriple<PersonaConstruction<UserType>> personaConstructionTriple = evaluatedAssignment.getPersonaConstructionTriple();
		display("personaConstructionTriple", personaConstructionTriple);
		assertNotNull("No personaConstructionTriple", personaConstructionTriple);
		assertFalse("Empty personaConstructionTriple", personaConstructionTriple.isEmpty());
		assertTrue("Unexpected personaConstructionTriple plus set", personaConstructionTriple.getPlusSet().isEmpty());
		assertTrue("Unexpected personaConstructionTriple minus set", personaConstructionTriple.getMinusSet().isEmpty());
		assertNotNull("No personaConstructionTriple zero set", personaConstructionTriple.getZeroSet());
		assertEquals("Wrong size of personaConstructionTriple zero set", 1, personaConstructionTriple.getZeroSet().size());
		PersonaConstruction<UserType> personaConstruction = personaConstructionTriple.getZeroSet().iterator().next();
		assertNotNull("No personaConstruction", personaConstruction);
		PersonaConstructionType personaConstructionType = personaConstruction.getConstructionType();
		assertNotNull("No personaConstructionType", personaConstructionType);
		assertTrue("Wrong type: "+personaConstructionType.getTargetType(), QNameUtil.match(UserType.COMPLEX_TYPE, personaConstructionType.getTargetType()));
		PrismAsserts.assertEqualsCollectionUnordered("Wrong subtype", personaConstructionType.getTargetSubtype(), "admin");

	}



}
