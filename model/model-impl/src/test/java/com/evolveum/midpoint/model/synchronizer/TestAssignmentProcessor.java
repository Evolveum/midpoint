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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.omg.PortableInterceptor.USER_EXCEPTION;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.MidPointObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyPath;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml",
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-task.xml" })
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class TestAssignmentProcessor extends AbstractModelIntegrationTest {
	
	protected static final String TEST_RESOURCE_DIR_NAME = "src/test/resources/synchronizer";
	
	protected static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ = TEST_RESOURCE_DIR_NAME +
					"/user-jack-modify-add-assignment-account-opendj.xml";
	protected static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR = TEST_RESOURCE_DIR_NAME +
					"/user-jack-modify-add-assignment-account-opendj-attr.xml";
	private static final String REQ_USER_BARBOSSA_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR = TEST_RESOURCE_DIR_NAME +
					"/user-barbossa-modify-add-assignment-account-opendj-attr.xml";
	private static final String REQ_USER_BARBOSSA_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR = TEST_RESOURCE_DIR_NAME +
					"/user-barbossa-modify-delete-assignment-account-opendj-attr.xml";
	
	private static final PropertyPath ATTRIBUTES_PARENT_PATH = new PropertyPath(SchemaConstants.I_ATTRIBUTES);  

	@Autowired(required=true)
	private AssignmentProcessor assignmentProcessor;
	
	public TestAssignmentProcessor() throws JAXBException {
		super();
	}

	/**
	 * Test empty change. Run the outbound processor with empty user (no assignments) and no change. Check that the
	 * resulting changes are also empty.
	 */
	@Test
	public void test001OutboundEmpty() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		displayTestTile(this, "test001OutboundEmpty");
		
		// GIVEN
		OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test001OutboundEmpty");
		
		SyncContext context = new SyncContext();
		fillInUser(context, USER_JACK_OID, result);
		
		// WHEN
		assignmentProcessor.processAssignments(context, result);
		
		// THEN
		display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);
		
		assertNull(context.getUserPrimaryDelta());
		assertNull(context.getUserSecondaryDelta());
		assertTrue(context.getAccountContexts().isEmpty());
	}
	
	@Test
	public void test011AddAssignmentAddAccountDirect() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, JAXBException {
		displayTestTile(this, "test011AddAssignmentAddAccountDirect");
		
		// GIVEN
		OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test011AddAssignmentAddAccountDirect");
		
		SyncContext context = new SyncContext();
		fillInUser(context, USER_JACK_OID, result);
		addModification(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ);
		
		display("Input context",context);
		
		assertUserModificationSanity(context);
		
		// WHEN
		assignmentProcessor.processAssignments(context, result);
		
		// THEN
		display("Output context",context);
		display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);
		
		assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.MODIFY);
		assertNull("Unexpected user changes", context.getUserSecondaryDelta());
		assertFalse("No account changes", context.getAccountContexts().isEmpty());
		
		Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
		assertEquals(1,accountContexts.size());
		AccountSyncContext accContext = accountContexts.iterator().next();
		assertNull(accContext.getAccountPrimaryDelta());
		
		ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
		assertEquals(ChangeType.ADD,accountSecondaryDelta.getChangeType());
		MidPointObject<AccountShadowType> newAccount = accountSecondaryDelta.getObjectToAdd();
		assertEquals("user", newAccount.findProperty(new QName(SchemaConstants.NS_C,"accountType")).getValue());
		assertEquals(new QName(resourceType.getNamespace(),"AccountObjectClass"),
				newAccount.findProperty(new QName(SchemaConstants.NS_C,"objectClass")).getValue());
		ObjectReferenceType resourceRef = (ObjectReferenceType) newAccount.findProperty(new QName(SchemaConstants.NS_C,"resourceRef")).getValue();
		assertEquals(resourceType.getOid(), resourceRef.getOid());
		
		PropertyContainer attributes = newAccount.findPropertyContainer(SchemaConstants.I_ATTRIBUTES);
		assertEquals("Sparrow", attributes.findProperty(new QName(resourceType.getNamespace(),"sn")).getValue());
		assertEquals("Jack", attributes.findProperty(new QName(resourceType.getNamespace(),"givenName")).getValue());
		assertEquals("Jack Sparrow", attributes.findProperty(new QName(resourceType.getNamespace(),"cn")).getValue());
		assertEquals("middle of nowhere", attributes.findProperty(new QName(resourceType.getNamespace(),"l")).getValue());
		assertEquals("Created by IDM", attributes.findProperty(new QName(resourceType.getNamespace(),"description")).getValue());
		assertEquals("jack", attributes.findProperty(new QName(resourceType.getNamespace(),"uid")).getValue());
		assertEquals("uid=jack,ou=people,dc=example,dc=com", attributes.findProperty(ICFS_NAME).getValue());
	}

	@Test
	public void test012AddAssignmentAddAccountDirectWithAttrs() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, JAXBException {
		displayTestTile(this, "test012AddAssignmentAddAccountDirectWithAttrs");
		
		// GIVEN
		OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test012AddAssignmentAddAccountDirectWithAttrs");
		
		SyncContext context = new SyncContext();
		fillInUser(context, USER_JACK_OID, result);
		addModification(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR);
		
		display("Input context",context);
		
		assertUserModificationSanity(context);
		
		// WHEN
		assignmentProcessor.processAssignments(context, result);
		
		// THEN
		display("Output context",context);
		display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);
		
		assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.MODIFY);
		assertNull("Unexpected user changes", context.getUserSecondaryDelta());
		assertFalse("No account changes", context.getAccountContexts().isEmpty());
		
		Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
		assertEquals(1,accountContexts.size());
		AccountSyncContext accContext = accountContexts.iterator().next();
		assertNull(accContext.getAccountPrimaryDelta());
		
		ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
		assertEquals(ChangeType.ADD,accountSecondaryDelta.getChangeType());
		MidPointObject<AccountShadowType> newAccount = accountSecondaryDelta.getObjectToAdd();
		assertEquals("user", newAccount.findProperty(new QName(SchemaConstants.NS_C,"accountType")).getValue());
		assertEquals(new QName(resourceType.getNamespace(),"AccountObjectClass"),
				newAccount.findProperty(new QName(SchemaConstants.NS_C,"objectClass")).getValue());
		ObjectReferenceType resourceRef = (ObjectReferenceType) newAccount.findProperty(new QName(SchemaConstants.NS_C,"resourceRef")).getValue();
		assertEquals(resourceType.getOid(), resourceRef.getOid());
		
		PropertyContainer attributes = newAccount.findPropertyContainer(SchemaConstants.I_ATTRIBUTES);
		assertEquals("Sparrow", attributes.findProperty(new QName(resourceType.getNamespace(),"sn")).getValue());
		assertEquals("Jack", attributes.findProperty(new QName(resourceType.getNamespace(),"givenName")).getValue());
		assertEquals("Jack Sparrow", attributes.findProperty(new QName(resourceType.getNamespace(),"cn")).getValue());
		assertEquals("Created by IDM", attributes.findProperty(new QName(resourceType.getNamespace(),"description")).getValue());
		assertEquals("jack", attributes.findProperty(new QName(resourceType.getNamespace(),"uid")).getValue());
		assertEquals("uid=jack,ou=people,dc=example,dc=com", attributes.findProperty(ICFS_NAME).getValue());
		
		assertEquals("Pirate Brethren, Inc.", attributes.findProperty(new QName(resourceType.getNamespace(),"o")).getValue());
		
		Set<Object> lValues = attributes.findProperty(new QName(resourceType.getNamespace(),"l")).getValues();
		TestUtil.assertSetEquals(lValues, "middle of nowhere", "Caribbean");
	}

	@Test
	public void test021AddAssignmentModifyAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, JAXBException {
		displayTestTile(this, "test021AddAssignmentModifyAccount");
		
		// GIVEN
		OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test021AddAssignmentModifyAccount");
		
		SyncContext context = new SyncContext();
		fillInUser(context, USER_BARBOSSA_OID, result);
		addModification(context, REQ_USER_BARBOSSA_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR);
		
		display("Input context",context);
		
		assertUserModificationSanity(context);
		
		// WHEN
		assignmentProcessor.processAssignments(context, result);
		
		// THEN
		display("Output context",context);
		display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);
		
		assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.MODIFY);
		assertNull("Unexpected user changes", context.getUserSecondaryDelta());
		assertFalse("No account changes", context.getAccountContexts().isEmpty());
		
		Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
		assertEquals(1,accountContexts.size());
		AccountSyncContext accContext = accountContexts.iterator().next();
		assertNull(accContext.getAccountPrimaryDelta());
		
		ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
		assertEquals(ChangeType.MODIFY,accountSecondaryDelta.getChangeType());
		assertNull(accountSecondaryDelta.getObjectToAdd());
		
		assertEquals(2,accountSecondaryDelta.getModifications().size());
		
		PropertyDelta propertyDeltaSecretary = accountSecondaryDelta.getPropertyDelta(ATTRIBUTES_PARENT_PATH, new QName(resourceType.getNamespace(), "secretary"));
		assertNotNull(propertyDeltaSecretary);
		TestUtil.assertSetEquals(propertyDeltaSecretary.getValuesToAdd(),"Jack the Monkey");
		assertNull(propertyDeltaSecretary.getValuesToDelete());
		PropertyDelta propertyDeltaL = accountSecondaryDelta.getPropertyDelta(ATTRIBUTES_PARENT_PATH, new QName(resourceType.getNamespace(), "l"));
		assertNotNull(propertyDeltaL);
		TestUtil.assertSetEquals(propertyDeltaL.getValuesToAdd(),"World's End");
		assertNull(propertyDeltaL.getValuesToDelete());
		
	}


	@Test
	public void test031DeleteAssignmentModifyAccount() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, JAXBException {
		displayTestTile(this, "test031DeleteAssignmentModifyAccount");
		
		// GIVEN
		OperationResult result = new OperationResult(TestAssignmentProcessor.class.getName() + ".test031DeleteAssignmentModifyAccount");
		
		SyncContext context = new SyncContext();
		fillInUser(context, USER_BARBOSSA_OID, result);
		addModification(context, REQ_USER_BARBOSSA_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR);
		
		display("Input context",context);
		
		assertUserModificationSanity(context);
		
		// WHEN
		assignmentProcessor.processAssignments(context, result);
		
		// THEN
		display("Output context",context);
		display("outbound processor result", result);
//		assertSuccess("Outbound processor failed (result)", result);
		
		assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.MODIFY);
		assertNull("Unexpected user changes", context.getUserSecondaryDelta());
		assertFalse("No account changes", context.getAccountContexts().isEmpty());
		
		Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
		assertEquals(1,accountContexts.size());
		AccountSyncContext accContext = accountContexts.iterator().next();
		assertNull(accContext.getAccountPrimaryDelta());
		
		ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
		assertEquals(ChangeType.MODIFY,accountSecondaryDelta.getChangeType());
		assertNull(accountSecondaryDelta.getObjectToAdd());
		
		assertEquals(1,accountSecondaryDelta.getModifications().size());
		
		PropertyDelta propertyDeltaL = accountSecondaryDelta.getPropertyDelta(ATTRIBUTES_PARENT_PATH, new QName(resourceType.getNamespace(), "l"));
		assertNotNull(propertyDeltaL);
		TestUtil.assertSetEquals(propertyDeltaL.getValuesToDelete(),"Shipwreck cove");
		assertNull(propertyDeltaL.getValuesToAdd());
		
	}

	
	
	private ObjectDelta<UserType> addModification(SyncContext context, String filename) throws JAXBException, SchemaException {
		Schema commonSchema = schemaRegistry.getCommonSchema();
		JAXBElement<ObjectModificationType> modElement = (JAXBElement<ObjectModificationType>) JAXBUtil.unmarshal(new File(filename));
		ObjectDelta<UserType> userDelta = ObjectDelta.createDelta(UserType.class, modElement.getValue(), commonSchema);
		context.addPrimaryUserDelta(userDelta);
		return userDelta;
	}
	
	private void assertUserModificationSanity(SyncContext context) throws JAXBException {
		MidPointObject<UserType> userOld = context.getUserOld();
		ObjectDelta<UserType> userPrimaryDelta = context.getUserPrimaryDelta();
		assertEquals(userOld.getOid(),userPrimaryDelta.getOid());
		assertEquals(ChangeType.MODIFY,userPrimaryDelta.getChangeType());
		assertNull(userPrimaryDelta.getObjectToAdd());
		for (PropertyDelta propMod: userPrimaryDelta.getModifications()) {
			if (propMod.getValuesToDelete() != null) {
				Property property = userOld.findProperty(propMod.getParentPath(), propMod.getName());
				assertNotNull("Deleted property "+propMod.getParentPath()+"/"+propMod.getName()+" not found in user", property);
				for (Object valueToDelete: propMod.getValuesToDelete()) {
					if (!property.getValues().contains(valueToDelete)) {
						display("Deleted value "+valueToDelete+" is not in user property "+propMod.getParentPath()+"/"+propMod.getName());
						display("Deleted value",valueToDelete);
						display("Deleted value (XML)",JAXBUtil.marshalWrap(valueToDelete));
						display("HASHCODE: "+valueToDelete.hashCode());
						for (Object value: property.getValues()) {
							display("Existing value",value);
							display("Existing value (XML)", JAXBUtil.marshalWrap(value));
							display("EQUALS: "+valueToDelete.equals(value));
							display("HASHCODE: "+value.hashCode());
						}
						AssertJUnit.fail("Deleted value "+valueToDelete+" is not in user property "+propMod.getParentPath()+"/"+propMod.getName());
					}
				}
			}
			
		}
	}

	private void fillInUser(SyncContext context, String userOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
		
		UserType userType = repositoryService.getObject(UserType.class, userOid, null, result);
		
		Schema commonSchema = schemaRegistry.getCommonSchema();
		ObjectDefinition<UserType> userDefinition = commonSchema.findObjectDefinitionByType(SchemaConstants.I_USER_TYPE);
		MidPointObject<UserType> user = userDefinition.parseObjectType(userType);
		context.setUserOld(user);
		context.setUserTypeOld(userType);

	}
	
}
