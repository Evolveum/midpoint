/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.common.valueconstruction.ValueConstruction;
import com.evolveum.midpoint.model.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.AccountConstruction;
import com.evolveum.midpoint.model.lens.AssignmentProcessor;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.model.lens.LensTestConstants.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentProcessor extends AbstractModelIntegrationTest {

    private static final PropertyPath ATTRIBUTES_PARENT_PATH = new PropertyPath(SchemaConstants.I_ATTRIBUTES);

	private static final String RESOURCE_OPENDJ_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000003";

    @Autowired(required = true)
    private AssignmentProcessor assignmentProcessor;

    public TestAssignmentProcessor() throws JAXBException {
        super();
    }

    /**
     * Test empty change. Run the outbound processor with empty user (no assignments) and no change. Check that the
     * resulting changes are also empty.
     */
    @Test
    public void test001OutboundEmpty() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        displayTestTile(this, "test001OutboundEmpty");

        // GIVEN
        OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test001OutboundEmpty");

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        context.recompute();

        // WHEN
        assignmentProcessor.processAssignmentsAccounts(context, result);

        // THEN
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertNull(context.getFocusContext().getPrimaryDelta());
        assertNull(context.getFocusContext().getSecondaryDelta());
        assertTrue(context.getProjectionContexts().isEmpty());
    }

    @Test
    public void test002ModifyUser() throws Exception {
        displayTestTile(this, "test002ModifyUser");

        // GIVEN
        OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test002ModifyUser");

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_OPENDJ_OID, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_LOCALITY, new PolyString("Tortuga"));
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsAccounts(context, result);

        // THEN
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);
        
        assertEquals(PolicyDecision.KEEP,accContext.getPolicyDecision());
        
        assignmentProcessor.processAssignmentsAccountValues(accContext, result);
        
        PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple =
        	accContext.getAccountConstructionDeltaSetTriple();
        
        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);
        PrismAsserts.assertTripleNoPlus(accountConstructionDeltaSetTriple);
        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 2);
        
        AccountConstruction zeroAccountConstruction = getZeroAccountConstruction(accountConstructionDeltaSetTriple, "Brethren account construction");
                
//        assertZeroAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"), "Pirate Brethren, Inc.");
//        assertNoPlusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"));
//        assertNoMinusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"));
        
        assertNoZeroAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        assertPlusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"), "Tortuga");
        assertMinusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"), "Caribbean");
        
//        assertZeroAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"), "Shipwreck cove");
        
    }
    
	@Test
    public void test011AddAssignmentAddAccountDirect() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, JAXBException, FileNotFoundException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        displayTestTile(this, "test011AddAssignmentAddAccountDirect");

        // GIVEN
        OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test011AddAssignmentAddAccountDirect");

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ);
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsAccounts(context, result);

        // THEN
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);
        
        assertEquals(PolicyDecision.ADD,accContext.getPolicyDecision());
        
     // TODO: Move to a different test
//        assertEquals(ChangeType.ADD, accountSecondaryDelta.getChangeType());
//        MidPointObject<AccountShadowType> newAccount = accountSecondaryDelta.getObjectToAdd();
//        assertEquals("user", newAccount.findProperty(new QName(SchemaConstants.NS_C, "accountType")).getValue());
//        assertEquals(new QName(resourceType.getNamespace(), "AccountObjectClass"),
//                newAccount.findProperty(new QName(SchemaConstants.NS_C, "objectClass")).getValue());
//        ObjectReferenceType resourceRef = (ObjectReferenceType) newAccount.findProperty(new QName(SchemaConstants.NS_C, "resourceRef")).getValue().getValue();
//        assertEquals(resourceType.getOid(), resourceRef.getOid());

//        PropertyContainer attributes = newAccount.findPropertyContainer(SchemaConstants.I_ATTRIBUTES);
//        assertEquals("Sparrow", attributes.findProperty(new QName(resourceType.getNamespace(), "sn")).getValue());
//        assertEquals("Jack", attributes.findProperty(new QName(resourceType.getNamespace(), "givenName")).getValue());
//        assertEquals("Jack Sparrow", attributes.findProperty(new QName(resourceType.getNamespace(), "cn")).getValue());
//        assertEquals("middle of nowhere", attributes.findProperty(new QName(resourceType.getNamespace(), "l")).getValue());
//        assertEquals("Created by IDM", attributes.findProperty(new QName(resourceType.getNamespace(), "description")).getValue());
//        assertEquals("jack", attributes.findProperty(new QName(resourceType.getNamespace(), "uid")).getValue());
//        assertEquals("uid=jack,ou=people,dc=example,dc=com", attributes.findProperty(ICFS_NAME).getValue());
    }

    @Test
    public void test012AddAssignmentAddAccountDirectAssignmentWithAttrs() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, JAXBException, FileNotFoundException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        displayTestTile(this, "test012AddAssignmentAddAccountDirectAssignmentWithAttrs");

        // GIVEN
        OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test012AddAssignmentAddAccountDirectAssignmentWithAttrs");

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR);
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsAccounts(context, result);

        // THEN
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);
        
        assertEquals(PolicyDecision.ADD,accContext.getPolicyDecision());
        
        assignmentProcessor.processAssignmentsAccountValues(accContext, result);
        
        PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple =
        	accContext.getAccountConstructionDeltaSetTriple();
        
        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);
        PrismAsserts.assertTripleNoZero(accountConstructionDeltaSetTriple);
        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);
        
        AccountConstruction plusAccountConstruction = getPlusAccountConstruction(accountConstructionDeltaSetTriple);
                
        assertZeroAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"), "Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"));
        assertNoMinusAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"));
        
        assertZeroAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"), "Caribbean");
        assertNoPlusAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        assertNoMinusAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        
        
// TODO: Move to a different test
//        assertEquals(ChangeType.ADD, accountSecondaryDelta.getChangeType());
//        MidPointObject<AccountShadowType> newAccount = accountSecondaryDelta.getObjectToAdd();
//        assertEquals("user", newAccount.findProperty(new QName(SchemaConstants.NS_C, "accountType")).getValue());
//        assertEquals(new QName(resourceType.getNamespace(), "AccountObjectClass"),
//                newAccount.findProperty(new QName(SchemaConstants.NS_C, "objectClass")).getValue());
//        ObjectReferenceType resourceRef = (ObjectReferenceType) newAccount.findProperty(new QName(SchemaConstants.NS_C, "resourceRef")).getValue().getValue();
//        assertEquals(resourceType.getOid(), resourceRef.getOid());
//
//        PropertyContainer attributes = newAccount.findPropertyContainer(SchemaConstants.I_ATTRIBUTES);
//        assertEquals("Sparrow", attributes.findProperty(new QName(resourceType.getNamespace(), "sn")).getValue());
//        assertEquals("Jack", attributes.findProperty(new QName(resourceType.getNamespace(), "givenName")).getValue());
//        assertEquals("Jack Sparrow", attributes.findProperty(new QName(resourceType.getNamespace(), "cn")).getValue());
//        assertEquals("Created by IDM", attributes.findProperty(new QName(resourceType.getNamespace(), "description")).getValue());
//        assertEquals("jack", attributes.findProperty(new QName(resourceType.getNamespace(), "uid")).getValue());
//        assertEquals("uid=jack,ou=people,dc=example,dc=com", attributes.findProperty(ICFS_NAME).getValue());
//
//        assertEquals("Pirate Brethren, Inc.", attributes.findProperty(new QName(resourceType.getNamespace(), "o")).getValue());
//
//        Set<PropertyValue<Object>> lValues = attributes.findProperty(new QName(resourceType.getNamespace(), "l")).getValues();
//        TestUtil.assertPropertyValueSetEquals(lValues, "middle of nowhere", "Caribbean");
    }

	@Test
    public void test021AddAssignmentModifyAccountAssignment() throws Exception {
        displayTestTile(this, "test021AddAssignmentModifyAccountAssignment");

        // GIVEN
        OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test021AddAssignmentModifyAccountAssignment");

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_OPENDJ_OID, result);
        addModificationToContext(context, REQ_USER_BARBOSSA_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR);
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsAccounts(context, result);

        // THEN
        display("Output context", context);
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);
        
        assertEquals(PolicyDecision.KEEP,accContext.getPolicyDecision());
        
        assignmentProcessor.processAssignmentsAccountValues(accContext, result);
        
        PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple =
        	accContext.getAccountConstructionDeltaSetTriple();
        
        PrismAsserts.assertTripleNoMinus(accountConstructionDeltaSetTriple);
        
        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 2);
        AccountConstruction zeroAccountConstruction = getZeroAccountConstruction(accountConstructionDeltaSetTriple, "Brethren account construction");
        
        assertZeroAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"), "Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"));
        assertNoMinusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"));
        
        assertZeroAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"), "Caribbean");
        assertNoPlusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        assertNoMinusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        
        assertSetSize("plus", accountConstructionDeltaSetTriple.getPlusSet(), 1);
        AccountConstruction plusAccountConstruction = getPlusAccountConstruction(accountConstructionDeltaSetTriple, "Monkey account construction");
        
        assertZeroAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"secretary"), "Jack the Monkey");
        assertNoPlusAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"secretary"));
        assertNoMinusAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"secretary"));
        
        assertZeroAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"), "Caribbean", "World's End");
        assertNoPlusAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        assertNoMinusAttributeValues(plusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));

        
        
//        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
//        assertNull(accountSecondaryDelta.getObjectToAdd());
//
//        assertEquals(2, accountSecondaryDelta.getModifications().size());
//
//        PropertyDelta propertyDeltaSecretary = accountSecondaryDelta.getPropertyDelta(ATTRIBUTES_PARENT_PATH, new QName(resourceType.getNamespace(), "secretary"));
//        assertNotNull(propertyDeltaSecretary);
//        TestUtil.assertPropertyValueSetEquals(propertyDeltaSecretary.getValuesToAdd(), "Jack the Monkey");
//        assertNull(propertyDeltaSecretary.getValuesToDelete());
//        PropertyDelta propertyDeltaL = accountSecondaryDelta.getPropertyDelta(ATTRIBUTES_PARENT_PATH, new QName(resourceType.getNamespace(), "l"));
//        assertNotNull(propertyDeltaL);
//        TestUtil.assertPropertyValueSetEquals(propertyDeltaL.getValuesToAdd(), "World's End");
//        assertNull(propertyDeltaL.getValuesToDelete());

    }

		

	@Test
    public void test031DeleteAssignmentModifyAccount() throws Exception {
        displayTestTile(this, "test031DeleteAssignmentModifyAccount");

        // GIVEN
        OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test031DeleteAssignmentModifyAccount");

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_OPENDJ_OID, result);
        addModificationToContext(context, REQ_USER_BARBOSSA_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR);
        context.recomputeFocus();

        display("Input context", context);
        
        PrismObject<UserType> userNew = context.getFocusContext().getObjectNew();
        assertEquals("Unexpected number of assignemnts in userNew after recompute", 1, userNew.asObjectable().getAssignment().size());

        assertUserModificationSanity(context);

        // WHEN
        assignmentProcessor.processAssignmentsAccounts(context, result);

        // THEN
        display("Output context", context.dump(true));
        display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);

        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertNull("Account secondary delta sneaked in", accountSecondaryDelta);

        assertEquals(PolicyDecision.KEEP,accContext.getPolicyDecision());
        
        assignmentProcessor.processAssignmentsAccountValues(accContext, result);
        
        
        PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple =
        	accContext.getAccountConstructionDeltaSetTriple();
        
        PrismAsserts.assertTripleNoPlus(accountConstructionDeltaSetTriple);
        
        assertSetSize("zero", accountConstructionDeltaSetTriple.getZeroSet(), 1);
        AccountConstruction zeroAccountConstruction = getZeroAccountConstruction(accountConstructionDeltaSetTriple);
        
        assertZeroAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"), "Caribbean");
        assertNoPlusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        assertNoMinusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        
        assertZeroAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"), "Pirate Brethren, Inc.");
        assertNoPlusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"));
        assertNoMinusAttributeValues(zeroAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"o"));
        
        assertSetSize("minus", accountConstructionDeltaSetTriple.getMinusSet(), 1);
        AccountConstruction minusAccountConstruction = getMinusAccountConstruction(accountConstructionDeltaSetTriple);
        
        assertZeroAttributeValues(minusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"), "Shipwreck cove");
        assertNoPlusAttributeValues(minusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        assertNoMinusAttributeValues(minusAccountConstruction, new QName(RESOURCE_OPENDJ_NS,"l"));
        
//        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
//        assertNull(accountSecondaryDelta.getObjectToAdd());
//
//        assertEquals(1, accountSecondaryDelta.getModifications().size());
//
//        PropertyDelta propertyDeltaL = accountSecondaryDelta.getPropertyDelta(ATTRIBUTES_PARENT_PATH, new QName(resourceType.getNamespace(), "l"));
//        assertNotNull(propertyDeltaL);
//        TestUtil.assertPropertyValueSetEquals(propertyDeltaL.getValuesToDelete(), "Shipwreck cove");
//        assertNull(propertyDeltaL.getValuesToAdd());

    }
	
	private <T> void assertPlusAttributeValues(AccountConstruction accountConstruction, QName attrName, T... expectedValue) {
        ValueConstruction<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeConstruction(attrName);
        assertNotNull("No value construction for attribute "+attrName+" in plus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple, triple.getPlusSet());
        TestUtil.assertSetEquals("Attribute "+attrName+" value in plus set", actual, expectedValue);
	}
	
	private <T> void assertZeroAttributeValues(AccountConstruction accountConstruction, QName attrName, T... expectedValue) {
        ValueConstruction<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeConstruction(attrName);
        assertNotNull("No value construction for attribute "+attrName+" in zero set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple, triple.getZeroSet());
        TestUtil.assertSetEquals("Attribute "+attrName+" value in zero set", actual, expectedValue);
	}
	
	private <T> void assertMinusAttributeValues(AccountConstruction accountConstruction, QName attrName, T... expectedValue) {
        ValueConstruction<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeConstruction(attrName);
        assertNotNull("No value construction for attribute "+attrName+" in minus set", vc);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        Collection<T> actual = getMultiValueFromDeltaSetTriple(triple, triple.getMinusSet());
        TestUtil.assertSetEquals("Attribute "+attrName+" value in minus set", actual, expectedValue);
	}
		
	private void assertNoPlusAttributeValues(AccountConstruction accountConstruction, QName attrName) {
        ValueConstruction<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeConstruction(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoPlus(triple);
	}

	private void assertNoZeroAttributeValues(AccountConstruction accountConstruction, QName attrName) {
        ValueConstruction<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeConstruction(attrName);
        PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> triple = vc.getOutputTriple();
        PrismAsserts.assertTripleNoZero(triple);
	}

	private void assertNoMinusAttributeValues(AccountConstruction accountConstruction, QName attrName) {
        ValueConstruction<? extends PrismPropertyValue<?>> vc = accountConstruction.getAttributeConstruction(attrName);
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
    
	private void assertSetSize(String setName, Collection<PrismPropertyValue<AccountConstruction>> set,
			int expectedSize) {
        assertEquals("Unexpected number of value in "+setName+" set", expectedSize, set.size());
	}

	private AccountConstruction getZeroAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple) {
		return getZeroAccountConstruction(accountConstructionDeltaSetTriple, null);
	}
	
	private AccountConstruction getZeroAccountConstruction(
		PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple,
		String description) {
		Collection<PrismPropertyValue<AccountConstruction>> set = accountConstructionDeltaSetTriple.getZeroSet();
		return getAccountConstruction(accountConstructionDeltaSetTriple, description, set, "zero");
	}
	
	private AccountConstruction getPlusAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple) {
		return getPlusAccountConstruction(accountConstructionDeltaSetTriple, null);
	}

	private AccountConstruction getPlusAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple,
			String description) {
		Collection<PrismPropertyValue<AccountConstruction>> set = accountConstructionDeltaSetTriple.getPlusSet();
		return getAccountConstruction(accountConstructionDeltaSetTriple, description, set, "plus");
	}

	private AccountConstruction getMinusAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple) {
		return getMinusAccountConstruction(accountConstructionDeltaSetTriple, null);
	}

	private AccountConstruction getMinusAccountConstruction(
			PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple,
			String description) {
		Collection<PrismPropertyValue<AccountConstruction>> set = accountConstructionDeltaSetTriple.getMinusSet();
		return getAccountConstruction(accountConstructionDeltaSetTriple, description, set, "minus");
	}
	
	private AccountConstruction getAccountConstruction(PrismValueDeltaSetTriple<PrismPropertyValue<AccountConstruction>> accountConstructionDeltaSetTriple,
			String description, Collection<PrismPropertyValue<AccountConstruction>> set, String setName) {
		for (PrismPropertyValue<AccountConstruction> constructionPVal: set) {
			AccountConstruction accountConstruction = constructionPVal.getValue();
			if (description == null || description.equals(accountConstruction.getDescription())) {
				assertNotNull("Null accountConstruction in "+setName+" set (description: '"+description+"')", accountConstruction);
				return accountConstruction;
			}
		}
		return null;
	}

    
}
