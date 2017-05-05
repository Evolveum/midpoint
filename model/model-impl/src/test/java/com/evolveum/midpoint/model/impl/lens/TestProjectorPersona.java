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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

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
        TestUtil.displayTestTile(this, TEST_NAME);

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
        rememberShadowFetchOperationCount();
        
        // WHEN
        projector.project(context, "test", task, result);
        
        // THEN
        display("Output context", context);
		assertShadowFetchOperationCountIncrement(0);
		
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
