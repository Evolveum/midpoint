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
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ObjectFactory;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class TestPrismConstruction {
	
	private static final String NS_FOO = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd";
	private static final String NS_BAR = "http://www.example.com/bar";
	private static final String USER_OID = "1234567890";
	
	private static final QName USER_QNAME = new QName(NS_FOO,"user");
	
	private static final QName USER_FULLNAME_QNAME = new QName(NS_FOO,"fullName");
	
	private static final QName USER_ACTIVATION_QNAME = new QName(NS_FOO,"activation");
	private static final QName USER_ENABLED_QNAME = new QName(NS_FOO,"enabled");
	private static final PropertyPath USER_ENABLED_PATH = new PropertyPath(USER_ACTIVATION_QNAME, USER_ENABLED_QNAME);
	private static final QName ACTIVATION_TYPE_QNAME = new QName(NS_FOO,"ActivationType");
	
	private static final QName USER_ASSIGNMENT_QNAME = new QName(NS_FOO,"assignment");
	private static final QName USER_DESCRIPTION_QNAME = new QName(NS_FOO,"description");
	private static final PropertyPath USER_ASSIGNMENT_DESCRIPTION_PATH = new PropertyPath(USER_ASSIGNMENT_QNAME, USER_DESCRIPTION_QNAME);
	private static final QName ASSIGNMENT_TYPE_QNAME = new QName(NS_FOO,"AssignmentType");
	
	@BeforeSuite
	public void setupDebug() {
		DebugUtil.setDefaultNamespacePrefix("http://midpoint.evolveum.com/xml/ns");
	}

	/**
	 * Construct object with schema. Starts by instantiating a definition and working downwards.
	 * All the items in the object should have proper definition. 
	 */
	@Test
	public void testConstructionWithSchema() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testConstructionWithSchema ]===");
		
		// GIVEN
		PrismContext ctx = constructPrismContext();
		PrismObjectDefinition<UserType> userDefinition = ctx.getSchemaRegistry().getObjectSchema().findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
		
		// WHEN
		PrismObject<UserType> user = userDefinition.instantiate();
		// Fill-in object values, checking presence of definition while doing so
		fillInUserDrake(user, true);
		
		// THEN
		System.out.println("User:");
		System.out.println(user.dump());
		// Check if the values are correct, also checking definitions
		assertUserDrake(user, true);
	}
	
	/**
	 * Construct object without schema. Starts by creating object "out of the blue" and
	 * the working downwards. 
	 */
	@Test
	public void testDefinitionlessConstruction() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testDefinitionlessConstruction ]===");

		// GIVEN
		// No context needed
		
		// WHEN
		PrismObject<UserType> user = new PrismObject<UserType>(USER_QNAME);
		// Fill-in object values, no schema checking
		fillInUserDrake(user, false);
		
		// THEN
		System.out.println("User:");
		System.out.println(user.dump());
		// Check if the values are correct, no schema checking
		assertUserDrake(user, false);
	}
	
	/**
	 * Construct object without schema. Starts by creating object "out of the blue" and
	 * the working downwards. Then apply the schema. Check definitions.
	 */
	@Test
	public void testDefinitionlessConstructionAndSchemaApplication() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testDefinitionlessConstructionAndSchemaApplication ]===");

		// GIVEN
		// No context needed (yet)
		
		PrismObject<UserType> user = new PrismObject<UserType>(USER_QNAME);
		// Fill-in object values, no schema checking
		fillInUserDrake(user, false);
		// Make sure the object is OK
		assertUserDrake(user, false);
		
		PrismContext ctx = constructPrismContext();
		PrismObjectDefinition<UserType> userDefinition =
			ctx.getSchemaRegistry().getObjectSchema().findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
		
		// WHEN
		user.applyDefinition(userDefinition);
			
		// THEN
		System.out.println("User:");
		System.out.println(user.dump());
		
		// Check schema now 
		assertUserDrake(user, true);

	}

	
	private void fillInUserDrake(PrismObject<UserType> user, boolean assertDefinitions) {
		user.setOid(USER_OID);
		
		// fullName
		PrismProperty fullNameProperty = user.findOrCreateProperty(USER_FULLNAME_QNAME);
		assertEquals(USER_FULLNAME_QNAME, fullNameProperty.getName());
		if (assertDefinitions) assertDefinition(fullNameProperty, DOMUtil.XSD_STRING, 1, 1);
		fullNameProperty.setValue(new PrismPropertyValue<String>("Sir Fancis Drake"));
		PrismProperty fullNamePropertyAgain = user.findOrCreateProperty(USER_FULLNAME_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Property not the same", fullNameProperty == fullNamePropertyAgain);
		
		// activation
		PrismContainer activationContainer = user.findOrCreateContainer(USER_ACTIVATION_QNAME);
		assertEquals(USER_ACTIVATION_QNAME, activationContainer.getName());
		if (assertDefinitions) assertDefinition(activationContainer, ACTIVATION_TYPE_QNAME, 0, 1);
		PrismContainer activationContainerAgain = user.findOrCreateContainer(USER_ACTIVATION_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Property not the same", activationContainer == activationContainerAgain);
		
		// activation/enabled
		PrismProperty enabledProperty = user.findOrCreateProperty(USER_ENABLED_PATH);
		assertEquals(USER_ENABLED_QNAME, enabledProperty.getName());
		if (assertDefinitions) assertDefinition(enabledProperty, DOMUtil.XSD_BOOLEAN, 1, 1);
		enabledProperty.setValue(new PrismPropertyValue<Boolean>(true));
		PrismProperty enabledPropertyAgain = activationContainer.findOrCreateProperty(USER_ENABLED_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Property not the same", enabledProperty == enabledPropertyAgain);
		
		// assignment
		PrismContainer assignmentContainer = user.findOrCreateContainer(USER_ASSIGNMENT_QNAME);
		assertEquals(USER_ASSIGNMENT_QNAME, assignmentContainer.getName());
		if (assertDefinitions) assertDefinition(assignmentContainer, ASSIGNMENT_TYPE_QNAME, 0, -1);
		PrismContainer assignmentContainerAgain = user.findOrCreateContainer(USER_ASSIGNMENT_QNAME);
		// The "==" is there by purpose. We really want to make sure that is the same *instance*, that is was not created again
		assertTrue("Property not the same", assignmentContainer == assignmentContainerAgain);
		
		// assignment values: construct assignment value as a new container "out of the blue" and then add it.
		PrismContainer assBlueContainer = new PrismContainer(USER_ASSIGNMENT_QNAME);
		PrismProperty assBlueDescriptionProperty = assBlueContainer.findOrCreateProperty(USER_DESCRIPTION_QNAME);
		assBlueDescriptionProperty.addValue(new PrismPropertyValue<Object>("Assignment created out of the blue"));
		assignmentContainer.mergeValues(assBlueContainer);
		
		// assignment values: construct assignment value as a new container value "out of the blue" and then add it.
		PrismContainerValue assCyanContainerValue = new PrismContainerValue();
		PrismProperty assCyanDescriptionProperty = assCyanContainerValue.findOrCreateProperty(USER_DESCRIPTION_QNAME);
		assCyanDescriptionProperty.addValue(new PrismPropertyValue<Object>("Assignment created out of the cyan"));
		assignmentContainer.mergeValue(assCyanContainerValue);
		
		// assignment values: construct assignment value from existing container
		PrismContainerValue assRedContainerValue = assignmentContainer.createNewValue();
		PrismProperty assRedDescriptionProperty = assRedContainerValue.findOrCreateProperty(USER_DESCRIPTION_QNAME);
		assRedDescriptionProperty.addValue(new PrismPropertyValue<Object>("Assignment created out of the red"));
	}
	
	private void assertUserDrake(PrismObject<UserType> user, boolean assertDefinitions) {
		assertEquals("Wrong OID", USER_OID, user.getOid());
		// fullName
		PrismProperty fullNameProperty = user.findProperty(USER_FULLNAME_QNAME);
		if (assertDefinitions) assertDefinition(fullNameProperty, DOMUtil.XSD_STRING, 1, 1);
		assertEquals("Wrong fullname", "Sir Fancis Drake", fullNameProperty.getValue().getValue());
		// activation
		PrismContainer activationContainer = user.findContainer(USER_ACTIVATION_QNAME);
		assertEquals(USER_ACTIVATION_QNAME, activationContainer.getName());
		if (assertDefinitions) assertDefinition(activationContainer, ACTIVATION_TYPE_QNAME, 0, 1);
		// activation/enabled
		PrismProperty enabledProperty = user.findProperty(USER_ENABLED_PATH);
		assertEquals(USER_ENABLED_QNAME, enabledProperty.getName());
		if (assertDefinitions) assertDefinition(enabledProperty, DOMUtil.XSD_BOOLEAN, 1, 1);
		assertEquals("Wrong enabled", true, enabledProperty.getValue().getValue());
		// assignment
		PrismContainer assignmentContainer = user.findContainer(USER_ASSIGNMENT_QNAME);
		assertEquals(USER_ASSIGNMENT_QNAME, assignmentContainer.getName());
		if (assertDefinitions) assertDefinition(assignmentContainer, ASSIGNMENT_TYPE_QNAME, 0, -1);
		// assignment values
		List<PrismContainerValue> assValues = assignmentContainer.getValues();
		assertEquals("Wrong number of assignment values", 3, assValues.size());
		// assignment values: blue
		PrismContainerValue assBlueValue = assValues.get(0);
		PrismProperty assBlueDescriptionProperty = assBlueValue.findProperty(USER_DESCRIPTION_QNAME);
		if (assertDefinitions) assertDefinition(assBlueDescriptionProperty, DOMUtil.XSD_STRING, 0, 1);
		assertEquals("Wrong blue assignment description", "Assignment created out of the blue", assBlueDescriptionProperty.getValue().getValue());
		// assignment values: cyan
		PrismContainerValue assCyanValue = assValues.get(1);
		PrismProperty assCyanDescriptionProperty = assCyanValue.findProperty(USER_DESCRIPTION_QNAME);
		if (assertDefinitions) assertDefinition(assCyanDescriptionProperty, DOMUtil.XSD_STRING, 0, 1);
		assertEquals("Wrong cyan assignment description", "Assignment created out of the cyan", assCyanDescriptionProperty.getValue().getValue());
		// assignment values: red
		PrismContainerValue assRedValue = assValues.get(2);
		PrismProperty assRedDescriptionProperty = assRedValue.findProperty(USER_DESCRIPTION_QNAME);
		if (assertDefinitions) assertDefinition(assRedDescriptionProperty, DOMUtil.XSD_STRING, 0, 1);
		assertEquals("Wrong red assignment description", "Assignment created out of the red", assRedDescriptionProperty.getValue().getValue());
		
	}


	private void assertDefinition(Item item, QName type, int minOccurs, int maxOccurs) {
		ItemDefinition definition = item.getDefinition();
		assertNotNull("No definition in "+item.getName(), definition);
		assertEquals("Wrong definition type in "+item.getName(), type, definition.getTypeName());
		assertEquals("Wrong definition minOccurs in "+item.getName(), minOccurs, definition.getMinOccurs());
		assertEquals("Wrong definition maxOccurs in "+item.getName(), maxOccurs, definition.getMaxOccurs());
	}	
	
	private PrismContext constructPrismContext() throws SchemaException, SAXException, IOException {
		SchemaRegistry schemaRegistry = new SchemaRegistry();
		DynamicNamespacePrefixMapper prefixMapper = new GlobalDynamicNamespacePrefixMapper();
		// Set default namespace?
		schemaRegistry.setNamespacePrefixMapper(prefixMapper);
		schemaRegistry.registerPrismSchemaResource("xml/ns/test/foo-1.xsd", "foo", ObjectFactory.class.getPackage());
		schemaRegistry.setObjectSchemaNamespace(NS_FOO);
		schemaRegistry.initialize();
		
		PrismContext context = PrismContext.create(schemaRegistry);
		return context;
	}
}
