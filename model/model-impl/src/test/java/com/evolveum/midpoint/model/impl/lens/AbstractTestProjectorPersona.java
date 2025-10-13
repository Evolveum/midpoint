/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.*;

import java.io.File;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.construction.PersonaConstruction;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractTestProjectorPersona extends AbstractLensTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        setDefaultUserTemplate(USER_TEMPLATE_OID);
        addObject(getPersonaRoleFile());
        InternalMonitor.reset();
    }

    protected abstract File getPersonaRoleFile();

    protected abstract String getPersonaRoleOid();

    @Test
    public void test100AssignRolePersonaAdminToJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        ObjectDelta<UserType> focusDelta = createAssignmentAssignmentHolderDelta(UserType.class, USER_JACK_OID,
                getPersonaRoleOid(), RoleType.COMPLEX_TYPE, null, null, null, true);
        addFocusDeltaToContext(context, focusDelta);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        projector.project(context, "test", task, result);

        // THEN
        displayDumpable("Output context", context);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertSideEffectiveDeltasOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta", ActivationStatusType.ENABLED);
        assertTrue("Unexpected projection changes", context.getProjectionContexts().isEmpty());

        DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        assertNotNull("No evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        assertTrue("Unexpected evaluatedAssignmentTriple zero set", evaluatedAssignmentTriple.getZeroSet().isEmpty());
        assertTrue("Unexpected evaluatedAssignmentTriple minus set", evaluatedAssignmentTriple.getMinusSet().isEmpty());
        assertNotNull("No evaluatedAssignmentTriple plus set", evaluatedAssignmentTriple.getPlusSet());
        assertEquals("Wrong size of evaluatedAssignmentTriple plus set", 1, evaluatedAssignmentTriple.getPlusSet().size());
        EvaluatedAssignmentImpl<UserType> evaluatedAssignment = (EvaluatedAssignmentImpl<UserType>) evaluatedAssignmentTriple.getPlusSet().iterator().next();
        displayDumpable("evaluatedAssignment", evaluatedAssignment);
        assertNotNull("No evaluatedAssignment", evaluatedAssignment);
        DeltaSetTriple<PersonaConstruction<UserType>> personaConstructionTriple = evaluatedAssignment.getPersonaConstructionTriple();
        displayDumpable("personaConstructionTriple", personaConstructionTriple);
        assertNotNull("No personaConstructionTriple", personaConstructionTriple);
        assertFalse("Empty personaConstructionTriple", personaConstructionTriple.isEmpty());
        assertTrue("Unexpected personaConstructionTriple plus set", personaConstructionTriple.getPlusSet().isEmpty());
        assertTrue("Unexpected personaConstructionTriple minus set", personaConstructionTriple.getMinusSet().isEmpty());
        assertNotNull("No personaConstructionTriple zero set", personaConstructionTriple.getZeroSet());
        assertEquals("Wrong size of personaConstructionTriple zero set", 1, personaConstructionTriple.getZeroSet().size());
        PersonaConstruction<UserType> personaConstruction = personaConstructionTriple.getZeroSet().iterator().next();
        assertNotNull("No personaConstruction", personaConstruction);
        PersonaConstructionType personaConstructionType = personaConstruction.getConstructionBean();
        assertNotNull("No personaConstructionType", personaConstructionType);
        assertTrue("Wrong type: " + personaConstructionType.getTargetType(), QNameUtil.match(UserType.COMPLEX_TYPE, personaConstructionType.getTargetType()));
        assertPersonaSubtypeOrArchetype(personaConstructionType);
    }

    protected abstract void assertPersonaSubtypeOrArchetype(PersonaConstructionType personaConstructionType);
}
