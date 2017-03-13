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
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentProcessor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.*;

import static com.evolveum.midpoint.prism.delta.PlusMinusZero.MINUS;
import static com.evolveum.midpoint.prism.delta.PlusMinusZero.PLUS;
import static com.evolveum.midpoint.prism.delta.PlusMinusZero.ZERO;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestAssignmentProcessor extends AbstractLensTest {

    private static final ItemPath ATTRIBUTES_PARENT_PATH = new ItemPath(ShadowType.F_ATTRIBUTES);

    @Autowired
    private AssignmentProcessor assignmentProcessor;
    
    @Autowired
    private Clock clock;

    public TestAssignmentProcessor() throws JAXBException {
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
    	final String TEST_NAME = "test001OutboundEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        context.recompute();

        // WHEN
        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

        // THEN
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertNull(context.getFocusContext().getPrimaryDelta());
        assertNull(context.getFocusContext().getSecondaryDelta());
        assertTrue(context.getProjectionContexts().isEmpty());
    }

    @Test
    public void test002ModifyUser() throws Exception {
    	final String TEST_NAME = "test002ModifyUser";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_LOCALITY, new PolyString("Tortuga"));
        context.recompute();

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

        // THEN
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
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
        display("accountConstructionDeltaSetTriple", accountConstructionDeltaSetTriple);
        
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
		final String TEST_NAME = "test011AddAssignmentAddAccountDirect";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);
        context.recompute();

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

        // THEN
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
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
    	final String TEST_NAME = "test012AddAssignmentAddAccountDirectAssignmentWithAttrs";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR);
        context.recompute();

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
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
		final String TEST_NAME = "test021AddAssignmentModifyAccountAssignment";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addFocusModificationToContext(context, REQ_USER_BARBOSSA_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR);
        context.recompute();

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

        // THEN
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
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

    }

		

	@Test
    public void test031DeleteAssignmentModifyAccount() throws Exception {
		final String TEST_NAME = "test031DeleteAssignmentModifyAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addFocusModificationToContext(context, REQ_USER_BARBOSSA_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY_ATTR);
        context.recomputeFocus();

        display("Input context", context);
        
        PrismObject<UserType> userNew = context.getFocusContext().getObjectNew();
        assertEquals("Unexpected number of assignemnts in userNew after recompute", 1, userNew.asObjectable().getAssignment().size());

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

        // THEN
        display("Output context", context.dump(true));
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
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
		final String TEST_NAME = "test032ModifyUserLegalizeAccount";
	        TestUtil.displayTestTile(this, TEST_NAME);

	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
	        OperationResult result = task.getResult();

	        repoAddObjectFromFile(USER_LARGO_FILE, result);
	        
	        LensContext<UserType> context = createUserAccountContext();
	        fillContextWithUser(context, USER_LARGO_OID, result);
	        fillContextWithAccountFromFile(context, ACCOUNT_SHADOW_ELAINE_DUMMY_FILE, result);
	        context.recompute();
	        
	        ProjectionPolicyType accountSynchronizationSettings = new ProjectionPolicyType();
	        accountSynchronizationSettings.setLegalize(Boolean.TRUE);
	        accountSynchronizationSettings.setAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.POSITIVE);
	        context.setAccountSynchronizationSettings(accountSynchronizationSettings);
	        
	        assumeResourceAssigmentPolicy(RESOURCE_DUMMY_OID, AssignmentPolicyEnforcementType.POSITIVE, true);

	        display("Input context", context);

	        assertFocusModificationSanity(context);

	        // WHEN
	        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);
	        
	        context.recompute();
	        // THEN
	        display("Output context", context);
	        display("outbound processor result", result);

	        assertNotNull("Expected assigment change in secondary user changes, but it does not exist.", context.getFocusContext().getSecondaryDelta());
	        assertEquals("Unexpected number of secundary changes. ", 1, context.getFocusContext().getSecondaryDelta().getModifications().size());
	        assertNotNull("Expected assigment delta in secondary changes, but it does not exist.", ContainerDelta.findContainerDelta(context.getFocusContext().getSecondaryDelta().getModifications(), UserType.F_ASSIGNMENT));
	        assertFalse("No account changes", context.getProjectionContexts().isEmpty());
	        
	        LensProjectionContext accContext = context.getProjectionContexts().iterator().next();
	        
	        assertNoDecision(accContext);
	        assertLegal(accContext);

	    }

    @Test
    public void test100AddAssignmentWithConditionalMetarole() throws Exception {
        final String TEST_NAME = "test100AddAssignmentWithConditionalMetarole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE_ENGINEER);
        context.recompute();

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

        // THEN
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
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
        final String TEST_NAME = "test102EnableConditionalMetarole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserAccountContext();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        AssignmentType assignmentType = getAssignmentType(ASSIGNMENT_ROLE_MANAGER_FILE);
        assignmentType.asPrismContainerValue().setParent(null);
        user.asObjectable().getAssignment().add(assignmentType);
        fillContextWithFocus(context, user);

        addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_SET_COST_CENTER);
        context.recompute();

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

        // THEN
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
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
	 *
     * Checking approval policy rules.
	 * Visitor has a generic metarole that has associated policy rule (approve-any-corp-role).
	 * Generic metarole also induces metarole-sod-notifications that has "notify-exclusion-violations" rule.
     */
    @Test
    public void test200AssignVisitor() throws Exception {
        final String TEST_NAME = "test200AssignVisitor";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserAccountContext();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        AssignmentType assignmentType = new AssignmentType(prismContext);
        assignmentType.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_CORP_VISITOR_OID, ObjectTypes.ROLE));
        fillContextWithFocus(context, user);

        addFocusDeltaToContext(context, (ObjectDelta) DeltaBuilder.deltaFor(UserType.class, prismContext)
                .item(UserType.F_ASSIGNMENT).add(assignmentType)
                .asObjectDelta(USER_JACK_OID));
        context.recompute();

        display("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

        // THEN
	    //DebugUtil.setDetailedDebugDump(true);
        display("Output context", context);
        display("outbound processor result", result);
        //assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        DeltaSetTriple<EvaluatedAssignmentImpl> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        assertEquals("Wrong # of added assignments", 1, evaluatedAssignmentTriple.getPlusSet().size());

        display("Policy rules", context.dumpPolicyRules(3));

		EvaluatedAssignmentImpl evaluatedAssignment = evaluatedAssignmentTriple.getPlusSet().iterator().next();
		assertEquals("Wrong # of focus policy rules", 0, evaluatedAssignment.getFocusPolicyRules().size());
		Collection<EvaluatedPolicyRule> targetPolicyRules = evaluatedAssignment.getTargetPolicyRules();
		assertEquals("Wrong # of target policy rules", 2, targetPolicyRules.size());
    }

	/**
	 * Checking approval policy rules. (See note above.)
	 * Engineer has a generic metarole that provides these policy rules: approve-any-corp-rule, notify-exclusion-violations.
	 * However, it induces an Employee role that has also this generic metarole. Moreover, it has "employee-excludes-contractor"
	 * rule.
	 *
	 * First occurrence of the approval rule should have a trigger. Second one should be without a trigger.
	 */
	@Test
	public void test210AssignEngineer() throws Exception {
		final String TEST_NAME = "test210AssignEngineer";
		TestUtil.displayTestTile(this, TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserAccountContext();
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		AssignmentType assignmentType = new AssignmentType(prismContext);
		assignmentType.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_CORP_ENGINEER_OID, ObjectTypes.ROLE));
		fillContextWithFocus(context, user);

		addFocusDeltaToContext(context, (ObjectDelta) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(assignmentType)
				.asObjectDelta(USER_JACK_OID));
		context.recompute();

		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		assignmentProcessor.processAssignmentsProjections(context, getNow(), task, result);

		// THEN
		//DebugUtil.setDetailedDebugDump(true);
		display("Output context", context);
		display("outbound processor result", result);
		//assertSuccess("Outbound processor failed (result)", result);

		assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
		assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
		assertFalse("No account changes", context.getProjectionContexts().isEmpty());

		DeltaSetTriple<EvaluatedAssignmentImpl> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		assertEquals("Wrong # of added assignments", 1, evaluatedAssignmentTriple.getPlusSet().size());

		display("Policy rules", context.dumpPolicyRules(3));

		EvaluatedAssignmentImpl evaluatedAssignment = evaluatedAssignmentTriple.getPlusSet().iterator().next();
		assertEquals("Wrong # of focus policy rules", 0, evaluatedAssignment.getFocusPolicyRules().size());
		assertEquals("Wrong # of this target policy rules", 2, evaluatedAssignment.getThisTargetPolicyRules().size());
		Collection<EvaluatedPolicyRule> policyRules = evaluatedAssignment.getTargetPolicyRules();
		assertEquals("Wrong # of target policy rules", 5, policyRules.size());
	}

	private <T> void assertAttributeValues(Collection<PrismPropertyValue<Construction>> accountConstructions, QName attrName, PlusMinusZero attrSet, T... expectedValue) {
        Set<T> realValues = getAttributeValues(accountConstructions, attrName, attrSet);
        assertEquals("Unexpected attributes", new HashSet(Arrays.asList(expectedValue)), realValues);
    }

    private <T> Set<T> getAttributeValues(Collection<PrismPropertyValue<Construction>> accountConstructions, QName attrName, PlusMinusZero attributeSet) {
        Set<T> retval = new HashSet<>();
        for (PrismPropertyValue<Construction> constructionPropVal : accountConstructions) {
            Mapping<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping = constructionPropVal.getValue().getAttributeMapping(attrName);
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
        assertNotNull("No value construction for attribute "+attrName+" in plus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple, triple.getPlusSet());
        TestUtil.assertSetEquals("Attribute "+attrName+" value in plus set", actual, expectedValue);
	}
	
	private <T> void assertZeroAttributeValues(Construction accountConstruction, QName attrName, T... expectedValue) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = accountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute "+attrName+" in zero set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple, triple.getZeroSet());
        TestUtil.assertSetEquals("Attribute "+attrName+" value in zero set", actual, expectedValue);
	}
	
	private <T> void assertMinusAttributeValues(Construction accountConstruction, QName attrName, T... expectedValue) {
        PrismValueDeltaSetTripleProducer<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> vc = accountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute "+attrName+" in minus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple, triple.getMinusSet());
        TestUtil.assertSetEquals("Attribute "+attrName+" value in minus set", actual, expectedValue);
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

    private Object getSingleValueFromDeltaSetTriple(PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple, 
    		Collection<? extends PrismPropertyValue<?>> set) {
    	Collection<?> values = getMultiValueFromDeltaSetTriple(triple,set);
    	assertEquals(1,values.size());
    	return values.iterator().next();
    }
    
    private <T> Collection<T> getMultiValueFromDeltaSetTriple(PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple, 
    		Collection<? extends PrismPropertyValue<?>> set) {
    	Collection<T> vals = new ArrayList<T>(set.size());
    	for (PrismPropertyValue<?> pval: set) {
    		vals.add((T)pval.getValue());
    	}
    	return vals;
    }
    
	private void assertSetSize(String setName, Collection<PrismPropertyValue<Construction>> set,
			int expectedSize) {
        assertEquals("Unexpected number of value in "+setName+" set", expectedSize, set.size());
	}

	private Construction getZeroAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple) {
		return getZeroAccountConstruction(accountConstructionDeltaSetTriple, null);
	}
	
	private Construction getZeroAccountConstruction(
		PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple,
		String description) {
		Collection<PrismPropertyValue<Construction>> set = accountConstructionDeltaSetTriple.getZeroSet();
		return getAccountConstruction(accountConstructionDeltaSetTriple, description, set, "zero");
	}
	
	private Construction getPlusAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple) {
		return getPlusAccountConstruction(accountConstructionDeltaSetTriple, null);
	}

	private Construction getPlusAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple,
			String description) {
		Collection<PrismPropertyValue<Construction>> set = accountConstructionDeltaSetTriple.getPlusSet();
		return getAccountConstruction(accountConstructionDeltaSetTriple, description, set, "plus");
	}

	private Construction getMinusAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple) {
		return getMinusAccountConstruction(accountConstructionDeltaSetTriple, null);
	}

	private Construction getMinusAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple,
			String description) {
		Collection<PrismPropertyValue<Construction>> set = accountConstructionDeltaSetTriple.getMinusSet();
		return getAccountConstruction(accountConstructionDeltaSetTriple, description, set, "minus");
	}
	
	private Construction getAccountConstruction(PrismValueDeltaSetTriple<PrismPropertyValue<Construction>> accountConstructionDeltaSetTriple,
			String description, Collection<PrismPropertyValue<Construction>> set, String setName) {
		for (PrismPropertyValue<Construction> constructionPVal: set) {
			Construction accountConstruction = constructionPVal.getValue();
			if (description == null || description.equals(accountConstruction.getDescription())) {
				assertNotNull("Null accountConstruction in "+setName+" set (description: '"+description+"')", accountConstruction);
				return accountConstruction;
			}
		}
		return null;
	}

	private void assertLegal(LensProjectionContext accContext) {
		assertEquals("Expected projection "+accContext+" not legal", Boolean.TRUE, accContext.isLegal());
	}

	private void assertIllegal(LensProjectionContext accContext) {
		assertEquals("Expected projection "+accContext+" not illegal", Boolean.FALSE, accContext.isLegal());
	}

	private void assertNoDecision(LensProjectionContext accContext) {
		assertNull("Projection "+accContext+" has decision "+accContext.getSynchronizationPolicyDecision()+" while not expecting any", accContext.getSynchronizationPolicyDecision());
	}
    
	private XMLGregorianCalendar getNow() {
		return clock.currentTimeXMLGregorianCalendar();
	}
	
}
