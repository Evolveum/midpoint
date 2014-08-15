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

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.Construction;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.AssignmentProcessor;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 */
public class TestAssignmentProcessor extends AbstractLensTest {

    private static final ItemPath ATTRIBUTES_PARENT_PATH = new ItemPath(ShadowType.F_ATTRIBUTES);

    @Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;
    
    @Autowired(required = true)
    private Clock clock;

    public TestAssignmentProcessor() throws JAXBException {
        super();
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
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        assertPlusAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Tortuga");
        assertMinusAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Caribbean");
                
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
        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);
        
        Construction plusAccountConstruction = getPlusAccountConstruction(accountConstructionDeltaSetTriple);
                
        assertZeroAttributeValues(plusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		"Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(plusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        assertNoMinusAttributeValues(plusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        
        assertZeroAttributeValues(plusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Caribbean");
        assertNoPlusAttributeValues(plusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        assertNoMinusAttributeValues(plusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
                
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
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		"Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(zeroAccountConstruction,
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction,
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        
        assertZeroAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Caribbean");
        assertNoPlusAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        
        assertZeroAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME), "Sword");
        assertNoPlusAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));
        
        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);
        Construction plusAccountConstruction = getPlusAccountConstruction(accountConstructionDeltaSetTriple, "Monkey account construction");
        
        assertZeroAttributeValues(plusAccountConstruction,
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME), "Rum");
        assertNoPlusAttributeValues(plusAccountConstruction,
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME));
        assertNoMinusAttributeValues(plusAccountConstruction,
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME));
        
        assertZeroAttributeValues(plusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME),
        		"Dagger", "Pistol");
        assertNoPlusAttributeValues(plusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));
        assertNoMinusAttributeValues(plusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));

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
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME), "Caribbean");
        assertNoPlusAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction,
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME));
        
        assertZeroAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
        		"Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(zeroAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        assertNoMinusAttributeValues(zeroAccountConstruction,
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME));
        
        assertSetSize("minus", accountConstructionDeltaSetTriple.getMinusSet(), 1);
        Construction minusAccountConstruction = getMinusAccountConstruction(accountConstructionDeltaSetTriple);
        
        assertZeroAttributeValues(minusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME),
        		"Undead Monkey");
        assertNoPlusAttributeValues(minusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));
        assertNoMinusAttributeValues(minusAccountConstruction, 
        		dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME));

    }
	
	@Test
	 public void test032ModifyUserLegalizeAccount() throws Exception {
		final String TEST_NAME = "test032ModifyUserLegalizeAccount";
	        TestUtil.displayTestTile(this, TEST_NAME);

	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestAssignmentProcessor.class.getName() + "." + TEST_NAME);
	        OperationResult result = task.getResult();

	        repoAddObjectFromFile(USER_LARGO_FILENAME, UserType.class, result);
	        
	        LensContext<UserType> context = createUserAccountContext();
	        fillContextWithUser(context, USER_LARGO_OID, result);
	        fillContextWithAccountFromFile(context, ACCOUNT_SHADOW_ELAINE_DUMMY_FILENAME, result);
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
	
	private <T> void assertPlusAttributeValues(Construction accountConstruction, QName attrName, T... expectedValue) {
        Mapping<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute "+attrName+" in plus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple, triple.getPlusSet());
        TestUtil.assertSetEquals("Attribute "+attrName+" value in plus set", actual, expectedValue);
	}
	
	private <T> void assertZeroAttributeValues(Construction accountConstruction, QName attrName, T... expectedValue) {
        Mapping<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute "+attrName+" in zero set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple, triple.getZeroSet());
        TestUtil.assertSetEquals("Attribute "+attrName+" value in zero set", actual, expectedValue);
	}
	
	private <T> void assertMinusAttributeValues(Construction accountConstruction, QName attrName, T... expectedValue) {
        Mapping<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeMapping(attrName);
        assertNotNull("No value construction for attribute "+attrName+" in minus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple, triple.getMinusSet());
        TestUtil.assertSetEquals("Attribute "+attrName+" value in minus set", actual, expectedValue);
	}
		
	private void assertNoPlusAttributeValues(Construction accountConstruction, QName attrName) {
        Mapping<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeMapping(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoPlus(triple);
	}

	private void assertNoZeroAttributeValues(Construction accountConstruction, QName attrName) {
        Mapping<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeMapping(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoZero(triple);
	}

	private void assertNoMinusAttributeValues(Construction accountConstruction, QName attrName) {
        Mapping<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeMapping(attrName);
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
