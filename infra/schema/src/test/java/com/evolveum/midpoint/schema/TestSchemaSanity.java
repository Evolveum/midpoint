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
package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.schema.util.SchemaTestConstants.EXTENSION_LOCATIONS_ELEMENT;
import static com.evolveum.midpoint.schema.util.SchemaTestConstants.EXTENSION_LOCATIONS_TYPE;
import static com.evolveum.midpoint.schema.util.SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES;
import static com.evolveum.midpoint.schema.util.SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES_TYPE;
import static com.evolveum.midpoint.schema.util.SchemaTestConstants.NS_ICFC;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.SchemaTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * @author semancik
 *
 */
public class TestSchemaSanity {

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testPrefixMappings() {
		System.out.println("===[ testPrefixMappings ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		assertNotNull("No prism context", prismContext);
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
		assertNotNull("No schema registry in context", schemaRegistry);
		DynamicNamespacePrefixMapper prefixMapper = schemaRegistry.getNamespacePrefixMapper();
		assertNotNull("No prefix mapper in context", prefixMapper);

		System.out.println("Prefix mapper:");
		System.out.println(prefixMapper.debugDump());
		// WHEN, THEN
		assertMapping(prefixMapper, PrismConstants.NS_ANNOTATION, PrismConstants.PREFIX_NS_ANNOTATION);
		assertMapping(prefixMapper, SchemaConstantsGenerated.NS_COMMON, "");
		assertMapping(prefixMapper, MidPointConstants.NS_RA, MidPointConstants.PREFIX_NS_RA);
		assertMapping(prefixMapper, SchemaTestConstants.NS_ICFC, "icfc");
		assertMapping(prefixMapper, SchemaTestConstants.NS_ICFS, "icfs");

		QName cBarQName = new QName(SchemaConstantsGenerated.NS_COMMON, "bar");
		QName cBarQNameWithPrefix = prefixMapper.setQNamePrefix(cBarQName);
		assertQName("common namespace implicit", SchemaConstantsGenerated.NS_COMMON, "bar", "", cBarQNameWithPrefix);
		QName cBarQNameWithPrefixExplixit = prefixMapper.setQNamePrefixExplicit(cBarQName);
		assertQName("common namespace implicit", SchemaConstantsGenerated.NS_COMMON, "bar", "c", cBarQNameWithPrefixExplixit);

	}

	private void assertQName(String message, String expectedNamespace, String expectedLocalName, String expectedPrefix,
			QName actual) {
		assertEquals("Wrong qname namespace in "+message, expectedNamespace, actual.getNamespaceURI());
		assertEquals("Wrong qname local part in "+message, expectedLocalName, actual.getLocalPart());
		assertEquals("Wrong qname prefix in "+message, expectedPrefix, actual.getPrefix());

	}

	private void assertMapping(DynamicNamespacePrefixMapper prefixMapper, String namespace, String prefix) {
		assertEquals("Wrong prefix mapping for namespace "+namespace, prefix, prefixMapper.getPrefix(namespace));
	}

	@Test
	public void testUserDefinition() {
		System.out.println("===[ testUserDefinition ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();

		// WHEN
		PrismObjectDefinition<UserType> userDefinition = schemaRegistry.findObjectDefinitionByElementName(new QName(SchemaConstantsGenerated.NS_COMMON,"user"));

		// THEN
		assertNotNull("No user definition", userDefinition);
		System.out.println("User definition:");
		System.out.println(userDefinition.debugDump());

		PrismObjectDefinition<UserType> userDefinitionByClass = schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
		assertTrue("Different user def", userDefinition == userDefinitionByClass);

		SchemaTestUtil.assertUserDefinition(userDefinition);
	}

	@Test
	public void testAccountDefinition() {
		System.out.println("===[ testAccountDefinition ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();

		// WHEN
		PrismObjectDefinition<ShadowType> accountDefinition = schemaRegistry.findObjectDefinitionByElementName(
				SchemaConstants.C_SHADOW);
		assertNotNull("No account definition", accountDefinition);
		System.out.println("Account definition:");
		System.out.println(accountDefinition.debugDump());

		PrismObjectDefinition<ShadowType> accountDefinitionByClass =
			schemaRegistry.findObjectDefinitionByCompileTimeClass(ShadowType.class);
		assertTrue("Different account def", accountDefinition == accountDefinitionByClass);

		assertEquals("Wrong compile-time class in account definition", ShadowType.class, accountDefinition.getCompileTimeClass());
		PrismAsserts.assertPropertyDefinition(accountDefinition, ShadowType.F_NAME, PolyStringType.COMPLEX_TYPE, 0, 1);
		PrismAsserts.assertPropertyDefinition(accountDefinition, ShadowType.F_DESCRIPTION, DOMUtil.XSD_STRING, 0, 1);
		assertFalse("Account definition is marked as runtime", accountDefinition.isRuntimeSchema());

		PrismContainerDefinition attributesContainer = accountDefinition.findContainerDefinition(ShadowType.F_ATTRIBUTES);
		PrismAsserts.assertDefinition(attributesContainer, ShadowType.F_ATTRIBUTES, ShadowAttributesType.COMPLEX_TYPE, 0, 1);
		assertTrue("Attributes is NOT runtime", attributesContainer.isRuntimeSchema());
		assertEquals("Wrong attributes compile-time class", ShadowAttributesType.class, attributesContainer.getCompileTimeClass());
	}


	@Test
	public void testResourceDefinition() {
		System.out.println("===[ testResourceDefinition ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();

		// WHEN
		PrismObjectDefinition<ResourceType> resourceDefinition = schemaRegistry.findObjectDefinitionByElementName(
				new QName(SchemaConstantsGenerated.NS_COMMON, "resource"));
		assertNotNull("No resource definition", resourceDefinition);
		System.out.println("Resource definition:");
		System.out.println(resourceDefinition.debugDump());

		PrismObjectDefinition<ResourceType> resourceDefinitionByClass =
			schemaRegistry.findObjectDefinitionByCompileTimeClass(ResourceType.class);
		assertTrue("Different user def", resourceDefinition == resourceDefinitionByClass);

		assertEquals("Wrong compile-time class in resource definition", ResourceType.class, resourceDefinition.getCompileTimeClass());
		PrismAsserts.assertPropertyDefinition(resourceDefinition, ResourceType.F_NAME, PolyStringType.COMPLEX_TYPE, 0, 1);
		PrismAsserts.assertPropertyDefinition(resourceDefinition, ResourceType.F_DESCRIPTION, DOMUtil.XSD_STRING, 0, 1);
		assertFalse("Resource definition is marked as runtime", resourceDefinition.isRuntimeSchema());

		PrismContainerDefinition<ConnectorConfigurationType> connectorConfContainerDef = resourceDefinition.findContainerDefinition(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismAsserts.assertDefinition(connectorConfContainerDef, ResourceType.F_CONNECTOR_CONFIGURATION, ConnectorConfigurationType.COMPLEX_TYPE, 1, 1);
		assertTrue("<connectorConfiguration> is NOT dynamic", connectorConfContainerDef.isDynamic());
//		assertFalse("<connectorConfiguration> is runtime", connectorConfContainerDef.isRuntimeSchema());
		assertEquals("Wrong compile-time class for <connectorConfiguration> in resource definition", ConnectorConfigurationType.class, connectorConfContainerDef.getCompileTimeClass());

		PrismContainerDefinition<XmlSchemaType> schemaContainerDef = resourceDefinition.findContainerDefinition(ResourceType.F_SCHEMA);
		PrismAsserts.assertDefinition(schemaContainerDef, ResourceType.F_SCHEMA, XmlSchemaType.COMPLEX_TYPE, 0, 1);
		assertFalse("Schema is runtime", schemaContainerDef.isRuntimeSchema());
		assertEquals("Wrong compile-time class for <schema> in resource definition", XmlSchemaType.class, schemaContainerDef.getCompileTimeClass());
		assertEquals("Unexpected number of definitions in <schema>", 3, schemaContainerDef.getDefinitions().size());
		PrismAsserts.assertPropertyDefinition(schemaContainerDef, XmlSchemaType.F_CACHING_METADATA,
				CachingMetadataType.COMPLEX_TYPE, 0, 1);
		PrismAsserts.assertPropertyDefinition(schemaContainerDef, XmlSchemaType.F_DEFINITION, SchemaDefinitionType.COMPLEX_TYPE, 0, 1);
		PrismPropertyDefinition definitionPropertyDef = schemaContainerDef.findPropertyDefinition(XmlSchemaType.F_DEFINITION);
		assertNotNull("Null <definition> definition", definitionPropertyDef);
//		assertFalse("schema/definition is NOT runtime", definitionPropertyDef.isRuntimeSchema());
	}

	@Test
	public void testResourceConfigurationDefinition() {
		System.out.println("===[ testResourceConfigurationDefinition ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();

		// WHEN
		PrismContainerDefinition<?> configurationPropertiesDefinition = schemaRegistry.findContainerDefinitionByElementName(
				SchemaConstantsGenerated.ICF_C_CONFIGURATION_PROPERTIES);
		assertNotNull("No configurationProperties definition", configurationPropertiesDefinition);
		System.out.println("configurationProperties definition:");
		System.out.println(configurationPropertiesDefinition.debugDump());

		assertTrue("configurationProperties definition is NOT marked as runtime", configurationPropertiesDefinition.isRuntimeSchema());
//		assertNull("Unexpected compile-time class in configurationProperties definition", configurationPropertiesDefinition.getCompileTimeClass());

//		assertTrue("configurationProperties definition is NOT marked as wildcard", configurationPropertiesDefinition.isWildcard());

		// TODO
	}

	@Test
	public void testRoleDefinition() {
		System.out.println("===[ testRoleDefinition ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();

		// WHEN
		PrismObjectDefinition<RoleType> roleDefinition = schemaRegistry.findObjectDefinitionByElementName(new QName(SchemaConstantsGenerated.NS_COMMON,"role"));

		// THEN
		assertNotNull("No role definition", roleDefinition);
		System.out.println("Role definition:");
		System.out.println(roleDefinition.debugDump());

		PrismObjectDefinition<RoleType> roleDefinitionByClass = schemaRegistry.findObjectDefinitionByCompileTimeClass(RoleType.class);
		assertTrue("Different role def", roleDefinition == roleDefinitionByClass);

		assertEquals("Wrong compile-time class in role definition", RoleType.class, roleDefinition.getCompileTimeClass());
		PrismAsserts.assertPropertyDefinition(roleDefinition, ObjectType.F_NAME, PolyStringType.COMPLEX_TYPE, 0, 1);
		PrismAsserts.assertItemDefinitionDisplayName(roleDefinition, ObjectType.F_NAME, "ObjectType.name");
		PrismAsserts.assertItemDefinitionDisplayOrder(roleDefinition, ObjectType.F_NAME, 0);
		PrismAsserts.assertPropertyDefinition(roleDefinition, ObjectType.F_DESCRIPTION, DOMUtil.XSD_STRING, 0, 1);
		PrismAsserts.assertItemDefinitionDisplayName(roleDefinition, ObjectType.F_DESCRIPTION, "ObjectType.description");
		PrismAsserts.assertItemDefinitionDisplayOrder(roleDefinition, ObjectType.F_DESCRIPTION, 10);
		PrismAsserts.assertPropertyDefinition(roleDefinition, RoleType.F_REQUESTABLE, DOMUtil.XSD_BOOLEAN, 0, 1);
	}

	@Test
	public void testFocusDefinition() {
		System.out.println("===[ testFocusDefinition ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();

		// WHEN
		ComplexTypeDefinition focusDefinition = schemaRegistry.findComplexTypeDefinition(FocusType.COMPLEX_TYPE);

		// THEN
		assertNotNull("No focus definition", focusDefinition);
		System.out.println("Focus definition:");
		System.out.println(focusDefinition.debugDump());

		SchemaTestUtil.assertFocusDefinition(focusDefinition, "focus");
	}

	@Test
	public void testIcfSchema() {
		System.out.println("===[ testIcfSchema ]===");

		// WHEN
		// The context should have parsed common schema and also other midPoint schemas
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		assertNotNull("No prism context", prismContext);
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
		assertNotNull("No schema registry in context", schemaRegistry);

		System.out.println("Schema registry:");
		System.out.println(schemaRegistry.debugDump());

		PrismSchema icfSchema = schemaRegistry.findSchemaByNamespace(NS_ICFC);
		assertNotNull("No ICF schema", icfSchema);
		System.out.println("ICF schema:");
		System.out.println(icfSchema.debugDump());


		PrismContainerDefinition configurationPropertiesContainerDef = icfSchema.findContainerDefinitionByElementName(ICFC_CONFIGURATION_PROPERTIES);
		PrismAsserts.assertDefinition(configurationPropertiesContainerDef, ICFC_CONFIGURATION_PROPERTIES, ICFC_CONFIGURATION_PROPERTIES_TYPE, 0, 1);
		assertTrue("configurationPropertiesContainer definition is NOT marked as runtime", configurationPropertiesContainerDef.isRuntimeSchema());
		assertTrue("configurationPropertiesContainer definition is NOT marked as dynamic", configurationPropertiesContainerDef.isDynamic());

	}

	/**
	 * Extension schema should be loaded from src/test/resources/schema during test initialization.
	 */
	@Test
	public void testExtensionSchema() {
		System.out.println("===[ testExtensionSchema ]===");

		// WHEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		assertNotNull("No prism context", prismContext);
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
		assertNotNull("No schema registry in context", schemaRegistry);

		PrismSchema extensionSchema = schemaRegistry.findSchemaByNamespace(SchemaTestConstants.NS_EXTENSION);
		assertNotNull("No extension schema", extensionSchema);
		System.out.println("Extension schema:");
		System.out.println(extensionSchema.debugDump());


		PrismPropertyDefinition locationsProperty = extensionSchema.findPropertyDefinitionByElementName(EXTENSION_LOCATIONS_ELEMENT);
		PrismAsserts.assertDefinition(locationsProperty, EXTENSION_LOCATIONS_ELEMENT, EXTENSION_LOCATIONS_TYPE, 0, -1);
	}

	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

}
