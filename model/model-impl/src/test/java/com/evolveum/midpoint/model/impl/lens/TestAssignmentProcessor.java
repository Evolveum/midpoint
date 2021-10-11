/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.delta.PlusMinusZero.*;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

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

    @Autowired
    private AssignmentProcessor assignmentProcessor;

    @Autowired
    private Clock clock;

    public TestAssignmentProcessor() {
        super();
    }

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        context.recompute();

        // WHEN
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
        display("outbound processor result", result);

        assertNull(context.getFocusContext().getPrimaryDelta());
        assertNull(context.getFocusContext().getSecondaryDelta());
        assertTrue(context.getProjectionContexts().isEmpty());
    }

    @Test
    public void test002ModifyUser() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_LOCALITY, new PolyString("Tortuga"));
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
        displayDumpable("Output context", context);
        display("outbound processor result", result);
//        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple =
                accContext.getConstructionDeltaSetTriple();
        displayDumpable("accountConstructionDeltaSetTriple", accountConstructionDeltaSetTriple);

        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);
        PrismAsserts.assertTripleNoPlus(accountConstructionDeltaSetTriple);
        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 2);

        Construction zeroAccountConstruction = getZeroAccountConstruction(accountConstructionDeltaSetTriple, "Brethren account construction");

        assertNoZeroAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        assertPlusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Tortuga");
        assertMinusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Caribbean");

    }

    @Test
    public void test011AddAssignmentAddAccountDirect() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
        displayDumpable("Output context", context);
        display("outbound processor result", result);
//        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);
    }

    @Test
    public void test012AddAssignmentAddAccountDirectAssignmentWithAttrs() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR);
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
        then();
        displayDumpable("Output context", context);
        display("outbound processor result", result);
//        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple =
                accContext.getConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);
        PrismAsserts.assertTripleNoZero(accountConstructionDeltaSetTriple);
        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);

        Construction plusAccountConstruction = getPlusAccountConstruction(accountConstructionDeltaSetTriple);

        assertZeroAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
                "Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        assertNoMinusAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));

        assertZeroAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Caribbean");
        assertNoPlusAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        assertNoMinusAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));

    }

    @Test
    public void test021AddAssignmentModifyAccountAssignment() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addFocusModificationToContext(context, REQ_USER_BARBOSSA_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR);
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
        displayDumpable("Output context", context);
        display("outbound processor result", result);
//        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple =
                accContext.getConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);

        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 2);
        Construction zeroAccountConstruction = getZeroAccountConstruction(accountConstructionDeltaSetTriple,
                "Brethren account construction");

        assertZeroAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
                "Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));

        assertZeroAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Caribbean");
        assertNoPlusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));

        assertZeroAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME), "Sword");
        assertNoPlusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));

        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);
        Construction plusAccountConstruction = getPlusAccountConstruction(accountConstructionDeltaSetTriple, "Monkey account construction");

        assertZeroAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME), "Rum");
        assertNoPlusAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME));
        assertNoMinusAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME));

        assertZeroAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME),
                "Dagger", "Pistol");
        assertNoPlusAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));
        assertNoMinusAttributeValues(plusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));

        assertTrue("Old legal variable for projection context is not true", accContext.isLegalOld());
        assertTrue("Legal variable for projection context is not true", accContext.isLegal());
        assertTrue("Old assigned variable for projection context is not true", accContext.isAssignedOld());
        assertTrue("Assigned variable for projection context is not true", accContext.isAssigned());
    }

    @Test
    public void test031DeleteAssignmentModifyAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, task, result);
        addFocusModificationToContext(context, REQ_USER_BARBOSSA_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY_ATTR);
        context.recomputeFocus();

        displayDumpable("Input context", context);

        PrismObject<UserType> userNew = context.getFocusContext().getObjectNew();
        assertEquals("Unexpected number of assignments in userNew after recompute", 1, userNew.asObjectable().getAssignment().size());

        assertFocusModificationSanity(context);

        // WHEN
        when();
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
        then();
        displayValue("Output context", context.dump(true));
        display("result", result);
        assertSuccess(result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple =
                accContext.getConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoPlus(accountConstructionDeltaSetTriple);

        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 1);
        Construction zeroAccountConstruction = getZeroAccountConstruction(accountConstructionDeltaSetTriple);

        assertZeroAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Caribbean");
        assertNoPlusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));

        assertZeroAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
                "Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));

        assertSetSize("minus", accountConstructionDeltaSetTriple.getMinusSet(), 1);
        Construction minusAccountConstruction = getMinusAccountConstruction(accountConstructionDeltaSetTriple);

        assertZeroAttributeValues(minusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME),
                "Undead Monkey");
        assertNoPlusAttributeValues(minusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));
        assertNoMinusAttributeValues(minusAccountConstruction,
                getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));

    }

    @Test
    public void test032ModifyUserLegalizeAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        repoAddObjectFromFile(USER_LARGO_FILE, result);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_LARGO_OID, result);
        fillContextWithAccountFromFile(context, ACCOUNT_SHADOW_ELAINE_DUMMY_FILE, task, result);
        context.recompute();

        ProjectionPolicyType accountSynchronizationSettings = new ProjectionPolicyType();
        accountSynchronizationSettings.setLegalize(Boolean.TRUE);
        accountSynchronizationSettings.setAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.POSITIVE);
        context.setAccountSynchronizationSettings(accountSynchronizationSettings);

        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, true);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        context.recompute();
        // THEN
        displayDumpable("Output context", context);
        display("outbound processor result", result);

        assertNotNull("Expected assigment change in secondary user changes, but it does not exist.", context.getFocusContext().getSecondaryDelta());
        assertEquals("Unexpected number of secundary changes. ", 1, context.getFocusContext().getSecondaryDelta().getModifications().size());
        assertNotNull("Expected assigment delta in secondary changes, but it does not exist.",
                ItemDeltaCollectionsUtil.findContainerDelta(context.getFocusContext().getSecondaryDelta().getModifications(),
                        UserType.F_ASSIGNMENT));
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        LensProjectionContext accContext = context.getProjectionContexts().iterator().next();

        assertNoDecision(accContext);
        assertLegal(accContext);

    }

    @Test
    public void test100AddAssignmentWithConditionalMetarole() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE_ENGINEER);
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
        displayDumpable("Output context", context);
        display("outbound processor result", result);
//        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple =
                accContext.getConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);
        PrismAsserts.assertTripleNoZero(accountConstructionDeltaSetTriple);

        final QName TITLE_QNAME = getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        final QName LOCATION_QNAME = getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME);

        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 4);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), TITLE_QNAME, ZERO, "Engineer", "Employee");
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), TITLE_QNAME, PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), TITLE_QNAME, MINUS);

        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), LOCATION_QNAME, ZERO, "Caribbean");
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), LOCATION_QNAME, PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), LOCATION_QNAME, MINUS);
    }

    /**
     * There is a conditional metarole that references 'costCenter' attribute.
     * Let us change the value of this attribute.
     */
    @Test
    public void test102EnableConditionalMetarole() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_MANAGER_FILE);
        assignmentType.asPrismContainerValue().setParent(null);
        user.asObjectable().getAssignment().add(assignmentType);
        fillContextWithFocus(context, user);

        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_SET_COST_CENTER);
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
        displayDumpable("Output context", context);
        display("outbound processor result", result);
//        assertSuccess("Outbound processor failed (result)", result);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertNoDecision(accContext);
        assertLegal(accContext);

        assignmentProcessor.processAssignmentsAccountValues(accContext, result);

        PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple =
                accContext.getConstructionDeltaSetTriple();

        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);

        final QName TITLE_QNAME = getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        final QName LOCATION_QNAME = getDummyResourceController().getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME);

        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 3);
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), TITLE_QNAME, ZERO, "Employee");
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), TITLE_QNAME, PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), TITLE_QNAME, MINUS);

        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), LOCATION_QNAME, ZERO, "Caribbean");
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), LOCATION_QNAME, PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getZeroSet(), LOCATION_QNAME, MINUS);

        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), TITLE_QNAME, ZERO, "Manager");
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), TITLE_QNAME, PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), TITLE_QNAME, MINUS);

        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), LOCATION_QNAME, ZERO);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), LOCATION_QNAME, PLUS);
        assertAttributeValues(accountConstructionDeltaSetTriple.getPlusSet(), LOCATION_QNAME, MINUS);
    }

    /**
     * NOTE - these two tests are legacy. They should be placed in TestPolicyRules. Please do not add
     * any similar tests here; use TestPolicyRules instead. It contains better 'assume' methods for policies.
     * TODO move these ones as well
     * ===============================================================================================
     * <p>
     * Checking approval policy rules.
     * Visitor has a generic metarole that has associated policy rule (approve-any-corp-role).
     * Generic metarole also induces metarole-sod-notifications that has "notify-exclusion-violations" rule.
     */
    @Test
    public void test200AssignVisitor() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        AssignmentType assignmentType = new AssignmentType(prismContext);
        assignmentType.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_CORP_VISITOR_OID, ObjectTypes.ROLE));
        fillContextWithFocus(context, user);

        addFocusDeltaToContext(context, (ObjectDelta) prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(assignmentType)
                .asObjectDelta(USER_JACK_OID));
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
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

        EvaluatedAssignmentImpl evaluatedAssignment = evaluatedAssignmentTriple.getPlusSet().iterator().next();
        assertEquals("Wrong # of focus policy rules", 0, evaluatedAssignment.getFocusPolicyRules().size());
        Collection<EvaluatedPolicyRule> targetPolicyRules = evaluatedAssignment.getAllTargetsPolicyRules();
        assertEquals("Wrong # of target policy rules", 2, targetPolicyRules.size());
    }

    /**
     * Checking approval policy rules. (See note above.)
     * Engineer has a generic metarole that provides these policy rules: approve-any-corp-rule, notify-exclusion-violations.
     * However, it induces an Employee role that has also this generic metarole. Moreover, it has "employee-excludes-contractor"
     * rule.
     * <p>
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

        addFocusDeltaToContext(context, (ObjectDelta) prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(assignmentType)
                .asObjectDelta(USER_JACK_OID));
        context.recompute();

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignments(context, getNow(), task, result);

        // THEN
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

        EvaluatedAssignmentImpl evaluatedAssignment = evaluatedAssignmentTriple.getPlusSet().iterator().next();
        assertEquals("Wrong # of focus policy rules", 0, evaluatedAssignment.getFocusPolicyRules().size());
        assertEquals("Wrong # of this target policy rules", 2, evaluatedAssignment.getThisTargetPolicyRules().size());
        Collection<EvaluatedPolicyRule> policyRules = evaluatedAssignment.getAllTargetsPolicyRules();
        assertEquals("Wrong # of target policy rules", 5, policyRules.size());
    }

    private <T> void assertAttributeValues(Collection<PrismPropertyValue<Construction>> accountConstructions, QName attrName, PlusMinusZero attrSet, T... expectedValue) {
        Set<T> realValues = getAttributeValues(accountConstructions, attrName, attrSet);
        assertEquals("Unexpected attributes", new HashSet<>(Arrays.asList(expectedValue)), realValues);
    }

    private <T> Set<T> getAttributeValues(Collection<PrismPropertyValue<Construction>> accountConstructions, QName attrName, PlusMinusZero attributeSet) {
        Set<T> retval = new HashSet<>();
        for (PrismPropertyValue<Construction> constructionPropVal : accountConstructions) {
            MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping = constructionPropVal.getValue().getAttributeMapping(attrName);
            if (mapping != null && mapping.getOutputTriple() != null) {
                Collection<PrismPropertyValue<T>> values = (Collection) mapping.getOutputTriple().getSet(attributeSet);
                if (values != null) {
                    for (PrismPropertyValue<T> value : values) {
                        retval.add(value.getValue());
                    }
                }
            }
        }
        return retval;
    }

    private <T> void assertPlusAttributeValues(Construction accountConstruction, QName attrName, T... expectedValue) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = accountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute " + attrName + " in plus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple.getPlusSet());
        TestUtil.assertSetEquals("Attribute " + attrName + " value in plus set", actual, expectedValue);
    }

    private <T> void assertZeroAttributeValues(Construction accountConstruction, QName attrName, T... expectedValue) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = accountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute " + attrName + " in zero set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple.getZeroSet());
        TestUtil.assertSetEquals("Attribute " + attrName + " value in zero set", actual, expectedValue);
    }

    private <T> void assertMinusAttributeValues(Construction accountConstruction, QName attrName, T... expectedValue) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = accountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute " + attrName + " in minus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple.getMinusSet());
        TestUtil.assertSetEquals("Attribute " + attrName + " value in minus set", actual, expectedValue);
    }

    private void assertNoPlusAttributeValues(Construction accountConstruction, QName attrName) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = accountConstruction.getAttributeMapping(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoPlus(triple);
    }

    private void assertNoZeroAttributeValues(Construction accountConstruction, QName attrName) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = accountConstruction.getAttributeMapping(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoZero(triple);
    }

    private void assertNoMinusAttributeValues(Construction accountConstruction, QName attrName) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = accountConstruction.getAttributeMapping(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoMinus(triple);
    }

    private <T> Collection<T> getMultiValueFromDeltaSetTriple(
            Collection<? extends PrismPropertyValue<?>> set) {
        Collection<T> vals = new ArrayList<>(set.size());
        for (PrismPropertyValue<?> pval : set) {
            vals.add((T) pval.getValue());
        }
        return vals;
    }

    private void assertSetSize(
            String setName, Collection<PrismPropertyValue<Construction>> set, int expectedSize) {
        assertEquals("Unexpected number of value in " + setName + " set", expectedSize, set.size());
    }

    private Construction getZeroAccountConstruction(
            PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple) {
        return getZeroAccountConstruction(accountConstructionDeltaSetTriple, null);
    }

    private Construction getZeroAccountConstruction(
            PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple,
            String description) {
        Collection<PrismPropertyValue<Construction>> set = accountConstructionDeltaSetTriple.getZeroSet();
        return getAccountConstruction(description, set, "zero");
    }

    private Construction getPlusAccountConstruction(
            PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple) {
        return getPlusAccountConstruction(accountConstructionDeltaSetTriple, null);
    }

    private Construction getPlusAccountConstruction(
            PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple,
            String description) {
        Collection<PrismPropertyValue<Construction>> set = accountConstructionDeltaSetTriple.getPlusSet();
        return getAccountConstruction(description, set, "plus");
    }

    private Construction getMinusAccountConstruction(
            PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple) {
        return getMinusAccountConstruction(accountConstructionDeltaSetTriple, null);
    }

    private Construction getMinusAccountConstruction(
            PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple,
            String description) {
        Collection<PrismPropertyValue<Construction>> set = accountConstructionDeltaSetTriple.getMinusSet();
        return getAccountConstruction(description, set, "minus");
    }

    private Construction getAccountConstruction(
            String description, Collection<PrismPropertyValue<Construction>> set, String setName) {
        for (PrismPropertyValue<Construction> constructionPVal : set) {
            Construction accountConstruction = constructionPVal.getValue();
            if (description == null || description.equals(accountConstruction.getDescription())) {
                assertNotNull("Null accountConstruction in " + setName + " set (description: '" + description + "')", accountConstruction);
                return accountConstruction;
            }
        }
        return null;
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
}
