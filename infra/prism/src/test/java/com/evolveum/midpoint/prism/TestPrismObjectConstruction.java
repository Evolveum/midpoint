/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ActivationType;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestPrismObjectConstruction {

	private static final String USER_OID = "1234567890";
	private static final String ACCOUNT1_OID = "11100000111";
	private static final String ACCOUNT2_OID = "11100000222";


	@BeforeSuite
	public void setupDebug() {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
	}

    /**
     * Construct object with schema. Starts by instantiating a definition and working downwards.
     * All the items in the object should have proper definition.
     */
	@Test
	public void testConstructionWithSchema() throws Exception {
		final String TEST_NAME = "testConstructionWithSchema";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext ctx = constructInitializedPrismContext();
		PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));

		// WHEN
		PrismObject<UserType> user = userDefinition.instantiate();
		// Fill-in object values, checking presence of definition while doing so
		fillInUserDrake(user, true);

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		// Check if the values are correct, also checking definitions
		assertUserDrake(user, true, ctx);
	}

	/**
	 * Construct object without schema. Starts by creating object "out of the blue" and
	 * the working downwards.
	 */
	@Test(enabled = false)			// definition-less containers are no longer supported
	public void testDefinitionlessConstruction() throws Exception {
		final String TEST_NAME = "testDefinitionlessConstruction";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		// No context needed

		// WHEN
		PrismObject<UserType> user = new PrismObject<UserType>(USER_QNAME, UserType.class);
		// Fill-in object values, no schema checking
		fillInUserDrake(user, false);

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		// Check if the values are correct, no schema checking
		PrismContext ctx = constructInitializedPrismContext();
		assertUserDrake(user, false, ctx);
	}

	/**
	 * Construct object without schema. Starts by creating object "out of the blue" and
	 * the working downwards. Then apply the schema. Check definitions.
	 */
//	@Test
	public void testDefinitionlessConstructionAndSchemaApplication() throws Exception {
		final String TEST_NAME = "testDefinitionlessConstructionAndSchemaApplication";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		// No context needed (yet)
		PrismObject<UserType> user = new PrismObject<UserType>(USER_QNAME, UserType.class);
		// Fill-in object values, no schema checking
		fillInUserDrake(user, false);
		// Make sure the object is OK
		PrismContext ctx = constructInitializedPrismContext();
		assertUserDrake(user, false, ctx);


		PrismObjectDefinition<UserType> userDefinition =
				getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));

		// WHEN
		user.applyDefinition(userDefinition);

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());

		// Check schema now
		assertUserDrake(user, true, ctx);

	}

	@Test
	public void testClone() throws Exception {
		final String TEST_NAME = "testClone";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext ctx = constructInitializedPrismContext();
		PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
		PrismObject<UserType> user = userDefinition.instantiate();
		fillInUserDrake(user, true);
		// precondition
		assertUserDrake(user, true, ctx);

		// WHEN
		PrismObject<UserType> clone = user.clone();

		// THEN
		System.out.println("Cloned user:");
		System.out.println(clone.debugDump());
		// Check if the values are correct, also checking definitions
		assertUserDrake(clone, true, ctx);
	}

	@Test
	public void testCloneEquals() throws Exception {
		final String TEST_NAME = "testCloneEquals";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext ctx = constructInitializedPrismContext();
		PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
		PrismObject<UserType> user = userDefinition.instantiate();
		fillInUserDrake(user, true);
		PrismObject<UserType> clone = user.clone();

		// WHEN, THEN
		assertTrue("Clone not equal", clone.equals(user));
		assertTrue("Clone not equivalent", clone.equivalent(user));
	}


	private void fillInUserDrake(PrismObject<UserType> user, boolean assertDefinitions) throws SchemaException {
		user.setOid(USER_OID);

		// fullName
		PrismProperty<String> fullNameProperty = user.findOrCreateProperty(USER_FULLNAME_QNAME);
		assertEquals(USER_FULLNAME_QNAME, fullNameProperty.getElementName());
		PrismAsserts.assertParentConsistency(user);
		if (assertDefinitions) PrismAsserts.assertDefinition(fullNameProperty, DOMUtil.XSD_STRING, 1, 1);
		fullNameProperty.setValue(new PrismPropertyValue<String>("Sir Fancis Drake"));
		PrismProperty<String> fullNamePropertyAgain = user.findOrCreateProperty(USER_FULLNAME_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Property not the same", fullNameProperty == fullNamePropertyAgain);

		// activation
		PrismContainer<ActivationType> activationContainer = user.findOrCreateContainer(USER_ACTIVATION_QNAME);
		assertEquals(USER_ACTIVATION_QNAME, activationContainer.getElementName());
		PrismAsserts.assertParentConsistency(user);
		if (assertDefinitions) PrismAsserts.assertDefinition(activationContainer, ACTIVATION_TYPE_QNAME, 0, 1);
		PrismContainer<ActivationType> activationContainerAgain = user.findOrCreateContainer(USER_ACTIVATION_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Property not the same", activationContainer == activationContainerAgain);

		// activation/enabled
		PrismProperty<Boolean> enabledProperty = user.findOrCreateProperty(USER_ENABLED_PATH);
		assertEquals(USER_ENABLED_QNAME, enabledProperty.getElementName());
		PrismAsserts.assertParentConsistency(user);
		if (assertDefinitions) PrismAsserts.assertDefinition(enabledProperty, DOMUtil.XSD_BOOLEAN, 0, 1);
		enabledProperty.setValue(new PrismPropertyValue<Boolean>(true));
		PrismProperty<Boolean> enabledPropertyAgain = activationContainer.findOrCreateProperty(USER_ENABLED_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Property not the same", enabledProperty == enabledPropertyAgain);

		// assignment
		// Try to create this one from the value. It should work the same, but let's test a different code path
		PrismContainer<AssignmentType> assignmentContainer = user.getValue().findOrCreateContainer(USER_ASSIGNMENT_QNAME);
		assertEquals(USER_ASSIGNMENT_QNAME, assignmentContainer.getElementName());
		PrismAsserts.assertParentConsistency(user);
		if (assertDefinitions) PrismAsserts.assertDefinition(assignmentContainer, ASSIGNMENT_TYPE_QNAME, 0, -1);
		PrismContainer<AssignmentType> assignmentContainerAgain = user.findOrCreateContainer(USER_ASSIGNMENT_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Property not the same", assignmentContainer == assignmentContainerAgain);
		assertEquals("Wrong number of assignment values (empty)", 0, assignmentContainer.getValues().size());

		// assignment values: construct assignment value as a new container "out of the blue" and then add it.
		PrismContainer<AssignmentType> assBlueContainer = new PrismContainer<AssignmentType>(USER_ASSIGNMENT_QNAME);
		PrismProperty<String> assBlueDescriptionProperty = assBlueContainer.findOrCreateProperty(USER_DESCRIPTION_QNAME);
		assBlueDescriptionProperty.addValue(new PrismPropertyValue<String>("Assignment created out of the blue"));
		PrismAsserts.assertParentConsistency(user);
		assignmentContainer.mergeValues(assBlueContainer);
		assertEquals("Wrong number of assignment values (after blue)", 1, assignmentContainer.getValues().size());
		PrismAsserts.assertParentConsistency(user);

		// assignment values: construct assignment value as a new container value "out of the blue" and then add it.
		PrismContainerValue<AssignmentType> assCyanContainerValue = new PrismContainerValue<AssignmentType>();
		PrismProperty<String> assCyanDescriptionProperty = assCyanContainerValue.findOrCreateProperty(USER_DESCRIPTION_QNAME);
		assCyanDescriptionProperty.addValue(new PrismPropertyValue<String>("Assignment created out of the cyan"));
		assignmentContainer.mergeValue(assCyanContainerValue);
		assertEquals("Wrong number of assignment values (after cyan)", 2, assignmentContainer.getValues().size());
		PrismAsserts.assertParentConsistency(user);

		// assignment values: construct assignment value from existing container
		PrismContainerValue<AssignmentType> assRedContainerValue = assignmentContainer.createNewValue();
		PrismProperty<String> assRedDescriptionProperty = assRedContainerValue.findOrCreateProperty(USER_DESCRIPTION_QNAME);
		assRedDescriptionProperty.addValue(new PrismPropertyValue<String>("Assignment created out of the red"));
		assertEquals("Wrong number of assignment values (after red)", 3, assignmentContainer.getValues().size());
		PrismAsserts.assertParentConsistency(user);

		// accountRef
		PrismReference accountRef = user.findOrCreateReference(USER_ACCOUNTREF_QNAME);
		assertEquals(USER_ACCOUNTREF_QNAME, accountRef.getElementName());
		if (assertDefinitions) PrismAsserts.assertDefinition(accountRef, OBJECT_REFERENCE_TYPE_QNAME, 0, -1);
		accountRef.add(new PrismReferenceValue(ACCOUNT1_OID));
		accountRef.add(new PrismReferenceValue(ACCOUNT2_OID));
		PrismReference accountRefAgain = user.findOrCreateReference(USER_ACCOUNTREF_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Property not the same", accountRef == accountRefAgain);
		assertEquals("accountRef size", 2, accountRef.getValues().size());
		PrismAsserts.assertParentConsistency(user);

		// extension
		PrismContainer<?> extensionContainer = user.findOrCreateContainer(USER_EXTENSION_QNAME);
		assertEquals(USER_EXTENSION_QNAME, extensionContainer.getElementName());
		PrismAsserts.assertParentConsistency(user);
		if (assertDefinitions) PrismAsserts.assertDefinition(extensionContainer, DOMUtil.XSD_ANY, 0, 1);
		PrismContainer<AssignmentType> extensionContainerAgain = user.findOrCreateContainer(USER_EXTENSION_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Extension not the same", extensionContainer == extensionContainerAgain);
		assertEquals("Wrong number of extension values (empty)", 0, extensionContainer.getValues().size());

		// extension / stringType
		PrismProperty<String> stringTypeProperty = extensionContainer.findOrCreateProperty(EXTENSION_STRING_TYPE_ELEMENT);
		assertEquals(EXTENSION_STRING_TYPE_ELEMENT, stringTypeProperty.getElementName());
		PrismAsserts.assertParentConsistency(user);
		if (assertDefinitions) PrismAsserts.assertDefinition(stringTypeProperty, DOMUtil.XSD_STRING, 0, -1);

		// TODO

	}

	private void assertUserDrake(PrismObject<UserType> user, boolean assertDefinitions, PrismContext prismContext) throws SchemaException, SAXException, IOException {
		assertEquals("Wrong OID", USER_OID, user.getOid());
		assertEquals("Wrong compileTimeClass", UserType.class, user.getCompileTimeClass());

		user.checkConsistence();
		assertUserDrakeContent(user, assertDefinitions);
		if (assertDefinitions) {
			serializeAndValidate(user, prismContext);
		}
	}

	private void assertUserDrakeContent(PrismObject<UserType> user, boolean assertDefinitions) {
		// fullName
		PrismProperty fullNameProperty = user.findProperty(USER_FULLNAME_QNAME);
		if (assertDefinitions) PrismAsserts.assertDefinition(fullNameProperty, DOMUtil.XSD_STRING, 1, 1);
		assertEquals("Wrong fullname", "Sir Fancis Drake", fullNameProperty.getValue().getValue());
		// activation
		PrismContainer activationContainer = user.findContainer(USER_ACTIVATION_QNAME);
		assertEquals(USER_ACTIVATION_QNAME, activationContainer.getElementName());
		if (assertDefinitions) PrismAsserts.assertDefinition(activationContainer, ACTIVATION_TYPE_QNAME, 0, 1);
		// activation/enabled
		PrismProperty enabledProperty = user.findProperty(USER_ENABLED_PATH);
		assertEquals(USER_ENABLED_QNAME, enabledProperty.getElementName());
		if (assertDefinitions) PrismAsserts.assertDefinition(enabledProperty, DOMUtil.XSD_BOOLEAN, 0, 1);
		assertEquals("Wrong enabled", true, enabledProperty.getValue().getValue());
		// assignment
		PrismContainer assignmentContainer = user.findContainer(USER_ASSIGNMENT_QNAME);
		assertEquals(USER_ASSIGNMENT_QNAME, assignmentContainer.getElementName());
		if (assertDefinitions) PrismAsserts.assertDefinition(assignmentContainer, ASSIGNMENT_TYPE_QNAME, 0, -1);
		// assignment values
		List<PrismContainerValue> assValues = assignmentContainer.getValues();
		assertEquals("Wrong number of assignment values", 3, assValues.size());
		// assignment values: blue
		PrismContainerValue assBlueValue = assValues.get(0);
		PrismProperty assBlueDescriptionProperty = assBlueValue.findProperty(USER_DESCRIPTION_QNAME);
		if (assertDefinitions) PrismAsserts.assertDefinition(assBlueDescriptionProperty, DOMUtil.XSD_STRING, 0, 1);
		assertEquals("Wrong blue assignment description", "Assignment created out of the blue", assBlueDescriptionProperty.getValue().getValue());
		// assignment values: cyan
		PrismContainerValue assCyanValue = assValues.get(1);
		PrismProperty assCyanDescriptionProperty = assCyanValue.findProperty(USER_DESCRIPTION_QNAME);
		if (assertDefinitions) PrismAsserts.assertDefinition(assCyanDescriptionProperty, DOMUtil.XSD_STRING, 0, 1);
		assertEquals("Wrong cyan assignment description", "Assignment created out of the cyan", assCyanDescriptionProperty.getValue().getValue());
		// assignment values: red
		PrismContainerValue assRedValue = assValues.get(2);
		PrismProperty assRedDescriptionProperty = assRedValue.findProperty(USER_DESCRIPTION_QNAME);
		if (assertDefinitions) PrismAsserts.assertDefinition(assRedDescriptionProperty, DOMUtil.XSD_STRING, 0, 1);
		assertEquals("Wrong red assignment description", "Assignment created out of the red", assRedDescriptionProperty.getValue().getValue());
		// accountRef
		PrismReference accountRef = user.findReference(USER_ACCOUNTREF_QNAME);
		if (assertDefinitions) PrismAsserts.assertDefinition(accountRef, OBJECT_REFERENCE_TYPE_QNAME, 0, -1);
		PrismAsserts.assertReferenceValue(accountRef, ACCOUNT1_OID);
		PrismAsserts.assertReferenceValue(accountRef, ACCOUNT2_OID);
		assertEquals("accountRef size", 2, accountRef.getValues().size());
		PrismAsserts.assertParentConsistency(user);
	}

	private void serializeAndValidate(PrismObject<UserType> user, PrismContext prismContext) throws SchemaException, SAXException, IOException {
		String xmlString = prismContext.serializeObjectToString(user, PrismContext.LANG_XML);
		System.out.println("Serialized XML");
		System.out.println(xmlString);
		Document xmlDocument = DOMUtil.parseDocument(xmlString);
		Schema javaxSchema = prismContext.getSchemaRegistry().getJavaxSchema();
		Validator validator = javaxSchema.newValidator();
		validator.setResourceResolver(prismContext.getEntityResolver());
		validator.validate(new DOMSource(xmlDocument));
	}

}
