/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.test.util.MidPointAsserts.assertSerializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.delta.PlusMinusZero.*;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedAssignedResourceObjectConstructionImpl;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentProcessor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class TestAssignmentProcessor extends AbstractLensTest {

    @Autowired private AssignmentProcessor assignmentProcessor;
    @Autowired private Clock clock;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObjects(ROLE_CORP_FILES);
    }

    /**
     * Test empty change. Run the outbound processor with empty user (no assignments) and no change. Check that the
     * resulting changes are also empty.
     */
    @Test
    public void test001OutboundEmpty() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        recompute(context);

        when();
        processAssignments(task, result, context);

        then();
        display("outbound processor result", result);

        assertNull(context.getFocusContext().getPrimaryDelta());
        assertNull(context.getFocusContext().getSecondaryDelta());
        assertTrue(context.getProjectionContexts().isEmpty());

        assertSerializable(context);
    }

    /**
     * Context: user barbossa (2 assignments to dummy account) + dummy account projection context.
     *
     * Primary delta: change of user locality.
     *
     * Assignment evaluator should produce 2 constructions.
     */
    @Test
    public void test002ModifyUser() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_LOCALITY, new PolyString("Tortuga"));
        recompute(context);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        then();
        displayDumpable("Output context", context);
        display("outbound processor result", result);
        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());

        LensProjectionContext accContext = getSingleProjectionContext(context);
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

        //assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple =
                accContext.getEvaluatedAssignedConstructionDeltaSetTriple();
        displayDumpable("accountConstructionDeltaSetTriple", accountConstructionDeltaSetTriple);

        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);
        PrismAsserts.assertTripleNoPlus(accountConstructionDeltaSetTriple);
        assertThat(accountConstructionDeltaSetTriple.getZeroSet())
                .as("zero set")
                .hasSize(2);

        EvaluatedAssignedResourceObjectConstructionImpl<UserType> zeroEvaluatedAccountConstruction =
                getZeroEvaluatedAccountConstruction(accountConstructionDeltaSetTriple, "Brethren account construction");

        assertNoZeroAttributeValues(zeroEvaluatedAccountConstruction, getLocationAttributeName());
        assertPlusAttributeValues(zeroEvaluatedAccountConstruction, getLocationAttributeName(), "Tortuga");
        assertMinusAttributeValues(zeroEvaluatedAccountConstruction, getLocationAttributeName(), "Caribbean");

        assertSerializable(context);
    }

    /**
     * Context: user jack (no assignments)
     *
     * Primary delta: add new dummy account assignment
     *
     * Assignment evaluator execution should result in projection context being created.
     */
    @Test
    public void test011AddAssignmentAddAccountDirect() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);
        recompute(context);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        then();
        displayDumpable("Output context", context);
        display("outbound processor result", result);
        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());

        LensProjectionContext accContext = getSingleProjectionContext(context);
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

        assertSerializable(context);
    }

    /**
     * Context: user jack (no assignments)
     *
     * Primary delta: add new dummy account assignment with two attribute mappings
     *
     * Assignment evaluator execution should result in projection context being created. Constructions with evaluated
     * mappings should be created as well.
     */
    @Test
    public void test012AddAssignmentAddAccountDirectAssignmentWithAttrs() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR);
        recompute(context);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        then();
        displayDumpable("Output context", context);
        display("outbound processor result", result);
        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());

        LensProjectionContext accContext = getSingleProjectionContext(context);
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

//        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple =
                accContext.getEvaluatedAssignedConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);
        PrismAsserts.assertTripleNoZero(accountConstructionDeltaSetTriple);
        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);

        EvaluatedAssignedResourceObjectConstructionImpl<UserType> plusAccountConstruction =
                getPlusEvaluatedAccountConstruction(accountConstructionDeltaSetTriple);

        assertZeroAttributeValues(plusAccountConstruction, getShipAttributeName(), "Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(plusAccountConstruction, getShipAttributeName());
        assertNoMinusAttributeValues(plusAccountConstruction, getShipAttributeName());

        assertZeroAttributeValues(plusAccountConstruction, getLocationAttributeName(), "Caribbean");
        assertNoPlusAttributeValues(plusAccountConstruction, getLocationAttributeName());
        assertNoMinusAttributeValues(plusAccountConstruction, getLocationAttributeName());

        assertSerializable(context);
    }

    /**
     * Context: user barbossa (two assignments of dummy account) + dummy account projection
     *
     * Primary delta: add (third) dummy account assignment with some attribute mappings.
     */
    @Test
    public void test021AddAssignmentModifyAccountAssignment() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addFocusModificationToContext(context, REQ_USER_BARBOSSA_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR);
        recompute(context);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        then();
        displayDumpable("Output context", context);
        display("outbound processor result", result);
        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());

        LensProjectionContext accContext = getSingleProjectionContext(context);
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

//        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple =
                accContext.getEvaluatedAssignedConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);

        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 2);
        EvaluatedAssignedResourceObjectConstructionImpl<UserType> zeroAccountConstruction =
                getZeroEvaluatedAccountConstruction(accountConstructionDeltaSetTriple, "Brethren account construction");

        assertZeroAttributeValues(zeroAccountConstruction, getShipAttributeName(), "Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(zeroAccountConstruction, getShipAttributeName());
        assertNoMinusAttributeValues(zeroAccountConstruction, getShipAttributeName());

        assertZeroAttributeValues(zeroAccountConstruction, getLocationAttributeName(), "Caribbean");
        assertNoPlusAttributeValues(zeroAccountConstruction, getLocationAttributeName());
        assertNoMinusAttributeValues(zeroAccountConstruction, getLocationAttributeName());

        assertZeroAttributeValues(zeroAccountConstruction, getWeaponAttributeName(), "Sword");
        assertNoPlusAttributeValues(zeroAccountConstruction, getWeaponAttributeName());
        assertNoMinusAttributeValues(zeroAccountConstruction, getWeaponAttributeName());

        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);
        EvaluatedAssignedResourceObjectConstructionImpl<UserType> plusAccountConstruction =
                getPlusEvaluatedAccountConstruction(accountConstructionDeltaSetTriple, "Monkey account construction");

        assertZeroAttributeValues(plusAccountConstruction, getDrinkAttributeName(), "Rum");
        assertNoPlusAttributeValues(plusAccountConstruction, getDrinkAttributeName());
        assertNoMinusAttributeValues(plusAccountConstruction, getDrinkAttributeName());

        assertZeroAttributeValues(plusAccountConstruction, getWeaponAttributeName(), "Dagger", "Pistol");
        assertNoPlusAttributeValues(plusAccountConstruction, getWeaponAttributeName());
        assertNoMinusAttributeValues(plusAccountConstruction, getWeaponAttributeName());

        assertTrue("Old legal variable for projection context is not true", accContext.isLegalOld());
        assertTrue("Legal variable for projection context is not true", accContext.isLegal());
        assertTrue("Old assigned variable for projection context is not true", accContext.isAssignedOld());
        assertTrue("Assigned variable for projection context is not true", accContext.isAssigned());

        assertSerializable(context);
    }

    /**
     * Context: user barbossa (two assignments of dummy account) + dummy account projection
     *
     * Primary delta: delete assignment with "weapon: Undead Monkey" mapping
     */
    @Test
    public void test031DeleteAssignmentModifyAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addFocusModificationToContext(context, REQ_USER_BARBOSSA_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY_ATTR);
        recompute(context);

        displayDumpable("Input context", context);

        PrismObject<UserType> userNew = context.getFocusContext().getObjectNew();
        assertEquals("Unexpected number of assignments in userNew after recompute", 1, userNew.asObjectable().getAssignment().size());

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        then();
        displayValue("Output context", context.dump(true));
        display("result", result);
        assertSuccess(result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());

        LensProjectionContext accContext = getSingleProjectionContext(context);
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

//        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple =
                accContext.getEvaluatedAssignedConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoPlus(accountConstructionDeltaSetTriple);

        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 1);
        EvaluatedAssignedResourceObjectConstructionImpl<UserType> zeroAccountConstruction =
                getZeroEvaluatedAccountConstruction(accountConstructionDeltaSetTriple);

        assertZeroAttributeValues(zeroAccountConstruction, getLocationAttributeName(), "Caribbean");
        assertNoPlusAttributeValues(zeroAccountConstruction, getLocationAttributeName());
        assertNoMinusAttributeValues(zeroAccountConstruction, getLocationAttributeName());

        assertZeroAttributeValues(zeroAccountConstruction, getShipAttributeName(), "Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(zeroAccountConstruction, getShipAttributeName());
        assertNoMinusAttributeValues(zeroAccountConstruction, getShipAttributeName());

        assertSetSize("minus", accountConstructionDeltaSetTriple.getMinusSet(), 1);
        EvaluatedAssignedResourceObjectConstructionImpl<UserType> minusAccountConstruction =
                getMinusEvaluatedAccountConstruction(accountConstructionDeltaSetTriple);

        assertZeroAttributeValues(minusAccountConstruction, getWeaponAttributeName(), "Undead Monkey");
        assertNoPlusAttributeValues(minusAccountConstruction, getWeaponAttributeName());
        assertNoMinusAttributeValues(minusAccountConstruction, getWeaponAttributeName());

        assertSerializable(context);
    }

    /**
     * Context: largo (no assignments) + one dummy account projection
     *
     * Primary delta: none
     *
     * Synchronization settings: legalize, positive enforcement (in context + in dummy resource)
     *
     * An assignment should be prepared for addition for the user.
     */
    @Test
    public void test032ModifyUserLegalizeAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        repoAddObjectFromFile(USER_LARGO_FILE, result);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_LARGO_OID, result);
        fillContextWithAccountFromFile(context, ACCOUNT_SHADOW_ELAINE_DUMMY_FILE, task, result);
        recompute(context);

        ProjectionPolicyType accountSynchronizationSettings = new ProjectionPolicyType();
        accountSynchronizationSettings.setLegalize(Boolean.TRUE);
        accountSynchronizationSettings.setAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.POSITIVE);
        context.setAccountSynchronizationSettings(accountSynchronizationSettings);

        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, true);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        context.recompute();
        then();
        displayDumpable("Output context", context);
        display("outbound processor result", result);

        assertNotNull("Expected assigment change in secondary user changes, but it does not exist.", context.getFocusContext().getSecondaryDelta());
        assertEquals("Unexpected number of secondary changes.", 1, context.getFocusContext().getSecondaryDelta().getModifications().size());
        assertNotNull("Expected assigment delta in secondary changes, but it does not exist.",
                ItemDeltaCollectionsUtil.findContainerDelta(context.getFocusContext().getSecondaryDelta().getModifications(),
                        UserType.F_ASSIGNMENT));

        LensProjectionContext accContext = context.getProjectionContexts().iterator().next();

        assertNoDecision(accContext);
        assertLegal(accContext);

        assertSerializable(context);
    }

    /**
     * Context: user jack (no assignments)
     *
     * Primary delta: add assignment to role engineer (has conditional metarole)
     *
     * MID-2190
     */
    @Test
    public void test100AddAssignmentWithConditionalMetarole() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE_ENGINEER);
        recompute(context);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        then();
        displayDumpable("Output context", context);
        display("outbound processor result", result);
        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());

        LensProjectionContext accContext = getSingleProjectionContext(context);
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

//        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> constructionTriple =
                accContext.getEvaluatedAssignedConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoMinus(constructionTriple);
        PrismAsserts.assertTripleNoZero(constructionTriple);

        assertSetSize("plus", constructionTriple.getPlusSet(), 4);
        assertAttributeValues(constructionTriple.getPlusSet(), getTitleAttributeName(), ZERO, "Engineer", "Employee");
        assertAttributeValues(constructionTriple.getPlusSet(), getTitleAttributeName(), PLUS);
        assertAttributeValues(constructionTriple.getPlusSet(), getTitleAttributeName(), MINUS);

        assertAttributeValues(constructionTriple.getPlusSet(), getLocationAttributeName(), ZERO, "Caribbean");
        assertAttributeValues(constructionTriple.getPlusSet(), getLocationAttributeName(), PLUS);
        assertAttributeValues(constructionTriple.getPlusSet(), getLocationAttributeName(), MINUS);

        assertSerializable(context);
    }

    /**
     * There is a conditional metarole that references 'costCenter' attribute.
     * Let us change the value of this attribute.
     */
    @Test
    public void test102EnableConditionalMetarole() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        AssignmentType assignmentType = getAssignmentBean(ASSIGNMENT_ROLE_MANAGER_FILE);
        assignmentType.asPrismContainerValue().setParent(null);
        user.asObjectable().getAssignment().add(assignmentType);
        fillContextWithFocus(context, user);

        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_SET_COST_CENTER);
        recompute(context);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        then();
        displayDumpable("Output context", context);
        display("outbound processor result", result);
        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());

        LensProjectionContext accContext = getSingleProjectionContext(context);
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

//        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple =
                accContext.getEvaluatedAssignedConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);

        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 3);
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), getTitleAttributeName(), ZERO, "Employee");
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), getTitleAttributeName(), PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), getTitleAttributeName(), MINUS);

        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), getLocationAttributeName(), ZERO, "Caribbean");
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), getLocationAttributeName(), PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), getLocationAttributeName(), MINUS);

        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), getTitleAttributeName(), ZERO, "Manager");
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), getTitleAttributeName(), PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), getTitleAttributeName(), MINUS);

        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), getLocationAttributeName(), ZERO);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), getLocationAttributeName(), PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), getLocationAttributeName(), MINUS);

        assertSerializable(context);
    }

    /**
     * NOTE: These two tests are legacy. They should be placed in TestPolicyRules. Please do not add
     * any similar tests here; use {@link TestPolicyRules} instead. It contains better 'assume' methods for policies.
     *
     * TODO move these ones as well
     *
     * ===============================================================================================
     *
     * Checking approval policy rules.
     * Visitor has a generic metarole that has associated policy rule (approve-any-corp-role).
     * Generic metarole also induces metarole-sod-notifications that has "notify-exclusion-violations" rule.
     */
    @Test
    public void test200AssignVisitor() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        AssignmentType assignment = new AssignmentType();
        assignment.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_CORP_VISITOR_OID, ObjectTypes.ROLE));
        fillContextWithFocus(context, user);

        addFocusDeltaToContext(context,
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(assignment)
                        .asObjectDelta(USER_JACK_OID));
        recompute(context);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        then();
        //DebugUtil.setDetailedDebugDump(true);
        displayDumpable("Output context", context);
        display("outbound processor result", result);
        //assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        assertEquals("Wrong # of added assignments", 1, evaluatedAssignmentTriple.getPlusSet().size());

        displayValue("Policy rules", context.dumpAssignmentPolicyRules(3));

        EvaluatedAssignmentImpl<?> evaluatedAssignment = evaluatedAssignmentTriple.getPlusSet().iterator().next();
        assertEquals("Wrong # of focus policy rules", 0, evaluatedAssignment.getObjectPolicyRules().size());
        Collection<? extends EvaluatedPolicyRule> targetPolicyRules = evaluatedAssignment.getAllTargetsPolicyRules();
        assertEquals("Wrong # of target policy rules", 2, targetPolicyRules.size());

        assertSerializable(context);
    }

    /**
     * Checking approval policy rules. (See note above.)
     *
     * Engineer has a generic metarole that provides these policy rules: approve-any-corp-rule, notify-exclusion-violations.
     * However, it induces an Employee role that has also this generic metarole. Moreover, it has "employee-excludes-contractor"
     * rule.
     *
     * First occurrence of the approval rule should have a trigger. Second one should be without a trigger.
     */
    @Test
    public void test210AssignEngineer() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        AssignmentType assignmentType = new AssignmentType(prismContext);
        assignmentType.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_CORP_ENGINEER_OID, ObjectTypes.ROLE));
        fillContextWithFocus(context, user);

        addFocusDeltaToContext(context,
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(assignmentType)
                        .asObjectDelta(USER_JACK_OID));
        recompute(context);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        processAssignments(task, result, context);

        then();
        displayDumpable("Output context", context);
        display("outbound processor result", result);
        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        assertEquals("Wrong # of added assignments", 1, evaluatedAssignmentTriple.getPlusSet().size());

        displayValue("Policy rules", context.dumpAssignmentPolicyRules(3));

        EvaluatedAssignmentImpl<?> evaluatedAssignment = evaluatedAssignmentTriple.getPlusSet().iterator().next();
        assertEquals("Wrong # of focus policy rules", 0, evaluatedAssignment.getObjectPolicyRules().size());
        assertEquals("Wrong # of this target policy rules", 2, evaluatedAssignment.getThisTargetPolicyRules().size());
        Collection<? extends EvaluatedPolicyRule> policyRules = evaluatedAssignment.getAllTargetsPolicyRules();
        assertEquals("Wrong # of target policy rules", 5, policyRules.size());

        assertSerializable(context);
    }

    @SafeVarargs
    private <T> void assertAttributeValues(Collection<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> constructions,
            QName attrName, PlusMinusZero attrSet, T... expectedValue) {
        Set<T> realValues = getAttributeValues(constructions, attrName, attrSet);
        assertEquals("Unexpected attributes", new HashSet<>(Arrays.asList(expectedValue)), realValues);
    }

    private <T> Set<T> getAttributeValues(Collection<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> constructions,
            QName attrName, PlusMinusZero attributeSet) {
        Set<T> retval = new HashSet<>();
        for (EvaluatedAssignedResourceObjectConstructionImpl<UserType> evaluatedConstruction : constructions) {
            MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping =
                    evaluatedConstruction.getAttributeMapping(attrName);
            if (mapping != null && mapping.getOutputTriple() != null) {
                //noinspection unchecked
                Collection<PrismPropertyValue<T>> values =
                        (Collection<PrismPropertyValue<T>>) mapping.getOutputTriple().getSet(attributeSet);
                if (values != null) {
                    for (PrismPropertyValue<T> value : values) {
                        retval.add(value.getValue());
                    }
                }
            }
        }
        return retval;
    }

    @SafeVarargs
    private <T> void assertPlusAttributeValues(EvaluatedAssignedResourceObjectConstructionImpl<UserType> evaluatedAccountConstruction, QName attrName, T... expectedValue) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = evaluatedAccountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute " + attrName + " in plus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple.getPlusSet());
        TestUtil.assertSetEquals("Attribute " + attrName + " value in plus set", actual, expectedValue);
    }

    @SafeVarargs
    private <T> void assertZeroAttributeValues(EvaluatedAssignedResourceObjectConstructionImpl<UserType> evaluatedAccountConstruction, QName attrName, T... expectedValue) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = evaluatedAccountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute " + attrName + " in zero set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple.getZeroSet());
        TestUtil.assertSetEquals("Attribute " + attrName + " value in zero set", actual, expectedValue);
    }

    @SafeVarargs
    private <T> void assertMinusAttributeValues(EvaluatedAssignedResourceObjectConstructionImpl<UserType> evaluatedAccountConstruction, QName attrName, T... expectedValue) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = evaluatedAccountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute " + attrName + " in minus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple.getMinusSet());
        TestUtil.assertSetEquals("Attribute " + attrName + " value in minus set", actual, expectedValue);
    }

    private void assertNoPlusAttributeValues(EvaluatedAssignedResourceObjectConstructionImpl<UserType> evaluatedAccountConstruction, QName attrName) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = evaluatedAccountConstruction.getAttributeMapping(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoPlus(triple);
    }

    private void assertNoZeroAttributeValues(EvaluatedAssignedResourceObjectConstructionImpl<UserType> evaluatedAccountConstruction, QName attrName) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = evaluatedAccountConstruction.getAttributeMapping(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoZero(triple);
    }

    private void assertNoMinusAttributeValues(EvaluatedAssignedResourceObjectConstructionImpl<UserType> evaluatedAccountConstruction, QName attrName) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = evaluatedAccountConstruction.getAttributeMapping(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoMinus(triple);
    }

    private <T> Collection<T> getMultiValueFromDeltaSetTriple(
            Collection<? extends PrismPropertyValue<?>> set) {
        Collection<T> vals = new ArrayList<>(set.size());
        for (PrismPropertyValue<?> pval : set) {
            //noinspection unchecked
            vals.add((T) pval.getValue());
        }
        return vals;
    }

    private void assertSetSize(
            String setName, Collection<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> set, int expectedSize) {
        assertEquals("Unexpected number of value in " + setName + " construction set", expectedSize, set.size());
    }

    private EvaluatedAssignedResourceObjectConstructionImpl<UserType> getZeroEvaluatedAccountConstruction(
            DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple) {
        return getZeroEvaluatedAccountConstruction(accountConstructionDeltaSetTriple, null);
    }

    private EvaluatedAssignedResourceObjectConstructionImpl<UserType> getZeroEvaluatedAccountConstruction(
            DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple,
            String description) {
        return getEvaluatedAccountConstruction(
                description,
                accountConstructionDeltaSetTriple.getZeroSet(),
                "zero");
    }

    private EvaluatedAssignedResourceObjectConstructionImpl<UserType> getPlusEvaluatedAccountConstruction(
            DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple) {
        return getPlusEvaluatedAccountConstruction(accountConstructionDeltaSetTriple, null);
    }

    private EvaluatedAssignedResourceObjectConstructionImpl<UserType> getPlusEvaluatedAccountConstruction(
            DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple,
            String description) {
        @NotNull Collection<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> set = accountConstructionDeltaSetTriple.getPlusSet();
        return getEvaluatedAccountConstruction(description, set, "plus");
    }

    private EvaluatedAssignedResourceObjectConstructionImpl<UserType> getMinusEvaluatedAccountConstruction(
            DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple) {
        return getMinusEvaluatedAccountConstruction(accountConstructionDeltaSetTriple, null);
    }

    @SuppressWarnings("SameParameterValue")
    private EvaluatedAssignedResourceObjectConstructionImpl<UserType> getMinusEvaluatedAccountConstruction(
            DeltaSetTriple<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> accountConstructionDeltaSetTriple,
            String description) {
        @NotNull Collection<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> set = accountConstructionDeltaSetTriple.getMinusSet();
        return getEvaluatedAccountConstruction(description, set, "minus");
    }

    private EvaluatedAssignedResourceObjectConstructionImpl<UserType> getEvaluatedAccountConstruction(
            String description, Collection<EvaluatedAssignedResourceObjectConstructionImpl<UserType>> set, String setName) {
        for (EvaluatedAssignedResourceObjectConstructionImpl<UserType> evaluatedConstruction : set) {
            if (description == null || description.equals(getDescription(evaluatedConstruction.getConstruction()))) {
                assertNotNull("Null accountConstruction in " + setName + " set (description: '" + description + "')", evaluatedConstruction);
                return evaluatedConstruction;
            }
        }
        return null;
    }

    private LensProjectionContext getSingleProjectionContext(LensContext<UserType> context) {
        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        return accountContexts.iterator().next();
    }

    private void assertLegal(LensProjectionContext accContext) {
        assertEquals("Expected projection " + accContext + " not legal", Boolean.TRUE, accContext.isLegal());
    }

    private void assertNoDecision(LensProjectionContext accContext) {
        assertNull(
                "Projection " + accContext + " has decision "
                        + accContext.getSynchronizationPolicyDecision() + " while not expecting any",
                accContext.getSynchronizationPolicyDecision());
    }

    private XMLGregorianCalendar getNow() {
        return clock.currentTimeXMLGregorianCalendar();
    }

    private void processAssignments(Task task, OperationResult result, LensContext<UserType> context)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        assignmentProcessor.processAssignments(context, getNow(), task, result);
    }

    private ItemName getDrinkAttributeName() {
        return getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
    }

    private ItemName getWeaponAttributeName() {
        return getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME);
    }

    private ItemName getShipAttributeName() {
        return getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
    }

    private ItemName getLocationAttributeName() {
        return getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME);
    }

    private ItemName getTitleAttributeName() {
        return getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
    }
}
