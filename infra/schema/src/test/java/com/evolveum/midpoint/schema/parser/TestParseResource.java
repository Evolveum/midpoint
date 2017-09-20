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
package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.TestConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.*;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.util.List;

import static com.evolveum.midpoint.schema.TestConstants.RESOURCE_FILE_BASENAME;
import static com.evolveum.midpoint.schema.TestConstants.RESOURCE_FILE_SIMPLE_BASENAME;
import static com.evolveum.midpoint.schema.util.SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
public class TestParseResource extends AbstractContainerValueParserTest<ResourceType> {

	@Override
	protected File getFile() {
		return getFile(RESOURCE_FILE_BASENAME);
	}

	@Test
	public void testParseResourceFile() throws Exception {
		displayTestTitle("testParseResourceFile");

		// GIVEN
		PrismContext prismContext = getPrismContext();

		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(getFile(RESOURCE_FILE_BASENAME));

		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());

		assertResource(resource, true, true, false);
	}

	@Test
	public void testParseResourceFileSimple() throws Exception {
		displayTestTitle("testParseResourceFileSimple");

		// GIVEN
		PrismContext prismContext = getPrismContext();

		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(getFile(RESOURCE_FILE_SIMPLE_BASENAME));

		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());

		assertResource(resource, true, true, true);
	}

	@Test
	public void testParseResourceRoundtripXml() throws Exception {
		displayTestTitle("testParseResourceRoundtripXml");
		doTestParseResourceRoundtrip(PrismContext.LANG_XML);
	}

	@Test
	public void testParseResourceRoundtripJson() throws Exception {
		displayTestTitle("testParseResourceRoundtripJson");
		doTestParseResourceRoundtrip(PrismContext.LANG_JSON);
	}

	@Test
	public void testParseResourceRoundtripYaml() throws Exception {
		displayTestTitle("testParseResourceRoundtripYaml");
		doTestParseResourceRoundtrip(PrismContext.LANG_YAML);
	}

	private void doTestParseResourceRoundtrip(String serializeInto) throws Exception {

		// GIVEN
		PrismContext prismContext = getPrismContext();

		PrismObject<ResourceType> resource = prismContext.parseObject(getFile(RESOURCE_FILE_BASENAME));

		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());

		assertResource(resource, true, false, false);

		// SERIALIZE

		String serializedResource = prismContext.serializerFor(serializeInto).serialize(resource);

		System.out.println("serialized resource:");
		System.out.println(serializedResource);

        // hack ... to make sure there's no "<clazz>" element there
        assertFalse("<clazz> element is present in the serialized form!", serializedResource.contains("<clazz>"));

        // RE-PARSE

		PrismObject<ResourceType> reparsedResource = prismContext.parseObject(serializedResource);

		System.out.println("Re-parsed resource:");
		System.out.println(reparsedResource.debugDump());

		// Cannot assert here. It will cause parsing of some of the raw values and diff will fail
		assertResource(reparsedResource, true, false, false);

		PrismProperty<SchemaDefinitionType> definitionProperty = reparsedResource.findContainer(ResourceType.F_SCHEMA).findProperty(XmlSchemaType.F_DEFINITION);
		SchemaDefinitionType definitionElement = definitionProperty.getValue().getValue();
		System.out.println("Re-parsed definition element:");
		System.out.println(DOMUtil.serializeDOMToString(definitionElement.getSchema()));

		ObjectDelta<ResourceType> objectDelta = resource.diff(reparsedResource);
		System.out.println("Delta:");
		System.out.println(objectDelta.debugDump());
		assertTrue("Delta is not empty", objectDelta.isEmpty());

		PrismAsserts.assertEquivalent("Resource re-parsed equivalence", resource, reparsedResource);

//		// Compare schema container
//
//		PrismContainer<?> originalSchemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
//		PrismContainer<?> reparsedSchemaContainer = reparsedResource.findContainer(ResourceType.F_SCHEMA);
	}

    /**
	 * Serialize and parse "schema" element on its own. There may be problems e.g. with preservation
	 * of namespace definitions.
	 */
	@Test
	public void testSchemaRoundtrip() throws Exception {
		displayTestTitle("testSchemaRoundtrip");

		// GIVEN
		PrismContext prismContext = getPrismContext();

		PrismObject<ResourceType> resource = prismContext.parseObject(getFile(RESOURCE_FILE_BASENAME));

		assertResource(resource, true, false, false);

		PrismContainer<Containerable> schemaContainer = resource.findContainer(ResourceType.F_SCHEMA);

		System.out.println("Parsed schema:");
		System.out.println(schemaContainer.debugDump());

		// SERIALIZE

		String serializesSchema = prismContext.serializerFor(language).serialize(schemaContainer.getValue(), new QName("fakeNs", "fake"));

		System.out.println("serialized schema:");
		System.out.println(serializesSchema);

		// RE-PARSE
		PrismContainer reparsedSchemaContainer = (PrismContainer) prismContext.parserFor(serializesSchema).language(language).definition(schemaContainer.getDefinition()).parseItem();

		System.out.println("Re-parsed schema container:");
		System.out.println(reparsedSchemaContainer.debugDump());

//		String reserializesSchema = prismContext.serializeContainerValueToString(reparsedSchemaContainer.getValue(), new QName("fakeNs", "fake"), PrismContext.LANG_XML);
//
//		System.out.println("re-serialized schema:");
//		System.out.println(reserializesSchema);

//		Document reparsedDocument = DOMUtil.parseDocument(serializesSchema);
//		Element reparsedSchemaElement = DOMUtil.getFirstChildElement(DOMUtil.getFirstChildElement(reparsedDocument));
//		Element reparsedXsdSchemaElement = DOMUtil.getChildElement(DOMUtil.getFirstChildElement(reparsedSchemaElement), DOMUtil.XSD_SCHEMA_ELEMENT);

		XmlSchemaType defType = (XmlSchemaType) reparsedSchemaContainer.getValue().asContainerable();
		Element reparsedXsdSchemaElement = defType.getDefinition().getSchema();
		ResourceSchema reparsedSchema = ResourceSchemaImpl.parse(reparsedXsdSchemaElement, "reparsed schema", prismContext);

	}

	@Test
	public void testParseResourceFileExpression() throws Exception {
		final String TEST_NAME = "testParseResourceFileExpression";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = getPrismContext();

		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(getFile(TestConstants.RESOURCE_FILE_EXPRESSION_BASENAME));

		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());

		assertResourceExpression(resource, prismContext, true);

	}

	private void assertResourceExpression(PrismObject<ResourceType> resource, PrismContext prismContext, boolean checkExpressions) throws SchemaException {
		resource.checkConsistence();

		AssertJUnit.assertEquals("Wrong oid (prism)", TestConstants.RESOURCE_OID, resource.getOid());
		PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
		assertNotNull("No resource definition", resourceDefinition);
		PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "resource"),
				ResourceType.COMPLEX_TYPE, ResourceType.class);
		assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("asObjectable resulted in null", resourceType);

		assertPropertyValue(resource, "name", PrismTestUtil.createPolyString("Resource with expressions"));

        PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertContainerDefinition(configurationContainer, "configuration", ConnectorConfigurationType.COMPLEX_TYPE, 1, 1);
		PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
		List<Item<?,?>> configItems = configContainerValue.getItems();
		assertEquals("Wrong number of config items", 1, configItems.size());

		PrismContainer<?> ldapConfigPropertiesContainer = configurationContainer.findContainer(ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No icfcldap:configurationProperties container", ldapConfigPropertiesContainer);
		PrismContainerDefinition<?> ldapConfigPropertiesContainerDef = ldapConfigPropertiesContainer.getDefinition();
		assertNotNull("No icfcldap:configurationProperties container definition", ldapConfigPropertiesContainerDef);
		assertEquals("icfcldap:configurationProperties container definition maxOccurs", 1, ldapConfigPropertiesContainerDef.getMaxOccurs());
		List<Item<?,?>> ldapConfigPropItems = ldapConfigPropertiesContainer.getValue().getItems();
		assertEquals("Wrong number of ldapConfigPropItems items", 7, ldapConfigPropItems.size());

		if (checkExpressions) {
			PrismProperty<String> hostProp = findProp(ldapConfigPropItems, "host");
			assertRaw(hostProp);
			hostProp.applyDefinition(new PrismPropertyDefinitionImpl<>(new QName("whatever","host"), DOMUtil.XSD_STRING, prismContext));
			assertNotRaw(hostProp);
			assertExpression(hostProp, "const");

			PrismProperty<String> baseContextsProp = findProp(ldapConfigPropItems, "baseContexts");
			assertRaw(baseContextsProp);
			baseContextsProp.applyDefinition(new PrismPropertyDefinitionImpl<>(new QName("whatever","baseContexts"), DOMUtil.XSD_STRING, prismContext));
			assertNotRaw(baseContextsProp);
			assertExpression(baseContextsProp, "script");
			
			PrismProperty<ProtectedStringType> credentialsProp = findProp(ldapConfigPropItems, "credentials");
			assertRaw(credentialsProp);
			credentialsProp.applyDefinition(new PrismPropertyDefinitionImpl<>(new QName("whatever","credentials"), ProtectedStringType.COMPLEX_TYPE, prismContext));
			assertNotRaw(credentialsProp);
			assertExpression(credentialsProp, "const");
		}

		PrismContainer<Containerable> schemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
		assertNull("Schema sneaked in", schemaContainer);

		PrismContainer<?> schemaHandlingContainer = resource.findContainer(ResourceType.F_SCHEMA_HANDLING);
		assertNull("SchemaHandling sneaked in", schemaHandlingContainer);

	}

	@Test
	public void testParseResourceExpressionRoundtripXml() throws Exception {
		displayTestTitle("testParseResourceExpressionRoundtripXml");
		doTestParseResourceExpressionRoundtrip(PrismContext.LANG_XML);
	}

	@Test
	public void testParseResourceExpressionRoundtripJson() throws Exception {
		displayTestTitle("testParseResourceExpressionRoundtripJson");
		doTestParseResourceExpressionRoundtrip(PrismContext.LANG_JSON);
	}

	@Test
	public void testParseResourceExpressionRoundtripYaml() throws Exception {
		displayTestTitle("testParseResourceExpressionRoundtripYaml");
		doTestParseResourceExpressionRoundtrip(PrismContext.LANG_YAML);
	}

	private void doTestParseResourceExpressionRoundtrip(String serializeInto) throws Exception {

		// GIVEN
		PrismContext prismContext = getPrismContext();

		PrismObject<ResourceType> resource = prismContext.parseObject(getFile(TestConstants.RESOURCE_FILE_EXPRESSION_BASENAME));

		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());

		assertResourceExpression(resource, prismContext, false);

		// SERIALIZE (1)

		String serializedResource = prismContext.serializerFor(serializeInto).serialize(resource);

		System.out.println("\nserialized resource (1):");
		System.out.println(serializedResource);

        // hack ... to make sure there's no "<clazz>" element there
        assertFalse("<clazz> element is present in the serialized form!", serializedResource.contains("<clazz>"));

        // RE-PARSE (1)

		PrismObject<ResourceType> reparsedResource = prismContext.parseObject(serializedResource);

		System.out.println("Re-parsed resource (1):");
		System.out.println(reparsedResource.debugDump());

		// Cannot assert here. It will cause parsing of some of the raw values and diff will fail
		assertResourceExpression(reparsedResource, prismContext, true);

		ObjectDelta<ResourceType> objectDelta = resource.diff(reparsedResource);
		System.out.println("Delta:");
		System.out.println(objectDelta.debugDump());
		assertTrue("Delta is not empty", objectDelta.isEmpty());

		PrismAsserts.assertEquivalent("Resource re-parsed equivalence", resource, reparsedResource);

		// SERIALIZE (2)
		// Do roundtrip again, this time after the expressions were checked and definitions applied.

		assertResourceExpression(resource, prismContext, true);
		System.out.println("\nResource (2):");
		System.out.println(resource.debugDump());

		serializedResource = prismContext.serializerFor(serializeInto).serialize(resource);

		System.out.println("\nserialized resource (2):");
		System.out.println(serializedResource);

        // hack ... to make sure there's no "<clazz>" element there
        assertFalse("<clazz> element is present in the serialized form!", serializedResource.contains("<clazz>"));

        // RE-PARSE (2)

		reparsedResource = prismContext.parseObject(serializedResource);

		System.out.println("Re-parsed resource (2):");
		System.out.println(reparsedResource.debugDump());

		// Cannot assert here. It will cause parsing of some of the raw values and diff will fail
		assertResourceExpression(reparsedResource, prismContext, true);

	}

	private <T> void assertRaw(PrismProperty<T> prop) {
		assertTrue("Prop "+prop+" no raw", prop.isRaw());
	}

	private <T> void assertNotRaw(PrismProperty<T> prop) {
		assertFalse("Prop "+prop+" raw (unexpected)", prop.isRaw());
	}

	private <T> PrismProperty<T> findProp(List<Item<?, ?>> items, String local) {
		for (Item<?, ?> item: items) {
			if (local.equals(item.getElementName().getLocalPart())) {
				return (PrismProperty<T>) item;
			}
		}
		fail("No item "+local);
		return null; // not reached
	}

	private <T> void assertExpression(PrismProperty<T> prop, String evaluatorName) {
		System.out.println("Prop:");
		System.out.println(prop.debugDump(1));
		PrismPropertyValue<T> pval = prop.getValue();
		ExpressionWrapper expressionWrapper = pval.getExpression();
		assertNotNull("No expression wrapper in "+prop, expressionWrapper);
		Object expressionObj = expressionWrapper.getExpression();
		assertNotNull("No expression in "+prop, expressionObj);
		System.out.println("- Expression: "+expressionObj);
		if (namespaces) {
			assertTrue("Wrong expression type ("+language+","+(namespaces?"ns":"no-ns") + ") : " +expressionObj.getClass(), expressionObj instanceof ExpressionType);
			ExpressionType expressionType = (ExpressionType)expressionObj;
			JAXBElement<?> evaluatorElement = expressionType.getExpressionEvaluator().iterator().next();
			assertEquals("Wrong expression evaluator name", evaluatorName, evaluatorElement.getName().getLocalPart());
		}
	}

	@Override
	protected void assertPrismContainerValueLocal(PrismContainerValue<ResourceType> value) throws SchemaException {
		//assertResource(object.asContainerable().asPrismObject(), true, true);
	}

	protected void assertResource(PrismObject<ResourceType> resource, boolean checkConsistence, boolean checkJaxb, boolean isSimple)
			throws SchemaException, JAXBException {
		if (checkConsistence) {
			resource.checkConsistence();
		}
		assertResourcePrism(resource, isSimple);
		assertResourceJaxb(resource.asObjectable(), isSimple);

		if (checkJaxb) {
//			serializeDom(resource);
			//serializeJaxb(resource);
		}
	}

	private void assertResourcePrism(PrismObject<ResourceType> resource, boolean isSimple) throws SchemaException {

        PrismContext prismContext = getPrismContext();

		AssertJUnit.assertEquals("Wrong oid (prism)", TestConstants.RESOURCE_OID, resource.getOid());
//		assertEquals("Wrong version", "42", resource.getVersion());
		PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
		assertNotNull("No resource definition", resourceDefinition);
		PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "resource"),
				ResourceType.COMPLEX_TYPE, ResourceType.class);
		assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("asObjectable resulted in null", resourceType);

		assertPropertyValue(resource, "name", PrismTestUtil.createPolyString("Embedded Test OpenDJ"));
		assertPropertyDefinition(resource, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

		if (!isSimple) {
			assertPropertyValue(resource, "namespace", TestConstants.RESOURCE_NAMESPACE);
			assertPropertyDefinition(resource, "namespace", DOMUtil.XSD_ANYURI, 0, 1);
		}

		PrismReference connectorRef = resource.findReference(ResourceType.F_CONNECTOR_REF);
		assertNotNull("No connectorRef", connectorRef);
    	PrismReferenceValue connectorRefVal = connectorRef.getValue();
    	assertNotNull("No connectorRef value", connectorRefVal);
    	assertEquals("Wrong type in connectorRef value", ConnectorType.COMPLEX_TYPE, connectorRefVal.getTargetType());
    	SearchFilterType filter = connectorRefVal.getFilter();
    	assertNotNull("No filter in connectorRef value", filter);
        if (!isSimple) {
            ObjectFilter objectFilter = QueryConvertor.parseFilter(filter, ConnectorType.class, prismContext);
            assertTrue("Wrong kind of filter: " + objectFilter, objectFilter instanceof EqualFilter);
            EqualFilter equalFilter = (EqualFilter) objectFilter;
            ItemPath path = equalFilter.getPath();      // should be extension/x:extConnType
            PrismAsserts.assertPathEqualsExceptForPrefixes("Wrong filter path",
					namespaces ?
						new ItemPath(new QName("extension"), new QName("http://x/", "extConnType")) :
						new ItemPath(new QName("extension"), new QName("extConnType")),
					path);
            PrismPropertyValue filterValue = (PrismPropertyValue) equalFilter.getValues().get(0);
            assertEquals("Wrong filter value", "org.identityconnectors.ldap.LdapConnector", ((RawType) filterValue.getValue()).getParsedRealValue(String.class).trim());
			//assertEquals("Wrong filter value", "org.identityconnectors.ldap.LdapConnector", ((String) filterValue.getValue()).trim());
        }
        EvaluationTimeType resolutionTime = connectorRefVal.getResolutionTime();
        if (isSimple) {
        	assertEquals("Wrong resolution time in connectorRef value", EvaluationTimeType.RUN, resolutionTime);
        } else {
        	assertEquals("Wrong resolution time in connectorRef value", EvaluationTimeType.IMPORT, resolutionTime);
        }

        PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertContainerDefinition(configurationContainer, "configuration", ConnectorConfigurationType.COMPLEX_TYPE, 1, 1);
		PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
		List<Item<?,?>> configItems = configContainerValue.getItems();
		assertEquals("Wrong number of config items", isSimple ? 1 : 4, configItems.size());

		PrismContainer<?> ldapConfigPropertiesContainer = configurationContainer.findContainer(ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No icfcldap:configurationProperties container", ldapConfigPropertiesContainer);
		PrismContainerDefinition<?> ldapConfigPropertiesContainerDef = ldapConfigPropertiesContainer.getDefinition();
		assertNotNull("No icfcldap:configurationProperties container definition", ldapConfigPropertiesContainerDef);
		assertEquals("icfcldap:configurationProperties container definition maxOccurs", 1, ldapConfigPropertiesContainerDef.getMaxOccurs());
		List<Item<?,?>> ldapConfigPropItems = ldapConfigPropertiesContainer.getValue().getItems();
		assertEquals("Wrong number of ldapConfigPropItems items", 7, ldapConfigPropItems.size());

		PrismContainer<Containerable> schemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
		if (isSimple) {
			assertNull("Schema sneaked in", schemaContainer);
		} else {
			assertNotNull("No schema container", schemaContainer);
		}

		PrismContainer<?> schemaHandlingContainer = resource.findContainer(ResourceType.F_SCHEMA_HANDLING);
		if (isSimple) {
			assertNull("SchemaHandling sneaked in", schemaHandlingContainer);
		} else {
			assertNotNull("No schemaHandling container", schemaHandlingContainer);
		}

		if (!isSimple) {
			PrismProperty<SynchronizationType> synchronizationProp = resource.findProperty(ResourceType.F_SYNCHRONIZATION);
			SynchronizationType synchronizationType = synchronizationProp.getRealValue();
			ObjectSynchronizationType objectSynchronizationType = synchronizationType.getObjectSynchronization().get(0);
			List<ConditionalSearchFilterType> correlations = objectSynchronizationType.getCorrelation();
			assertEquals("Wrong number of correlation expressions", 1, correlations.size());
			ConditionalSearchFilterType correlationFilterType = correlations.get(0);
			System.out.println("\nCorrelation filter");
			System.out.println(correlationFilterType.debugDump());

			ObjectFilter objectFilter = QueryConvertor.parseFilter(correlationFilterType.serializeToXNode(), prismContext);
			PrismAsserts.assertAssignableFrom(EqualFilter.class, objectFilter);
			EqualFilter equalsFilter = (EqualFilter)objectFilter;
			equalsFilter.getFullPath();
			assertNull("Unexpected values in correlation expression", equalsFilter.getValues());
			ExpressionWrapper expression = equalsFilter.getExpression();
			assertNotNull("No expressions in correlation expression", expression);

            ExpressionType expressionType = (ExpressionType) expression.getExpression();
            assertEquals("Wrong number of expression evaluators in correlation expression", 1, expressionType.getExpressionEvaluator().size());
            ItemPathType itemPathType = (ItemPathType) expressionType.getExpressionEvaluator().get(0).getValue();
            // $account/c:attributes/my:yyy
            PrismAsserts.assertPathEqualsExceptForPrefixes("path in correlation expression",
					namespaces ?
							new ItemPath(
									new NameItemPathSegment(new QName("account"), true),
									new NameItemPathSegment(new QName(SchemaConstantsGenerated.NS_COMMON, "attributes")),
									new NameItemPathSegment(new QName("http://myself.me/schemas/whatever", "yyy"))
							) :
							new ItemPath(
									new NameItemPathSegment(new QName("account"), true),
									new NameItemPathSegment(new QName("attributes")),
									new NameItemPathSegment(new QName("yyy"))
							), itemPathType.getItemPath());
			//PrismAsserts.assertAllParsedNodes(expression);
			// TODO
		}

	}

    private void assertResourceJaxb(ResourceType resourceType, boolean isSimple) throws SchemaException {
		assertEquals("Wrong oid (JAXB)", TestConstants.RESOURCE_OID, resourceType.getOid());
		assertEquals("Wrong name (JAXB)", PrismTestUtil.createPolyStringType("Embedded Test OpenDJ"), resourceType.getName());
		String expectedNamespace = TestConstants.RESOURCE_NAMESPACE;
		if (isSimple) {
			expectedNamespace = MidPointConstants.NS_RI;
		}
		assertEquals("Wrong namespace (JAXB)", expectedNamespace, ResourceTypeUtil.getResourceNamespace(resourceType));

		ObjectReferenceType connectorRef = resourceType.getConnectorRef();
		assertNotNull("No connectorRef (JAXB)", connectorRef);
		assertEquals("Wrong type in connectorRef (JAXB)", ConnectorType.COMPLEX_TYPE, connectorRef.getType());
		SearchFilterType filter = connectorRef.getFilter();
    	assertNotNull("No filter in connectorRef (JAXB)", filter);
    	MapXNode filterElement = filter.getFilterClauseXNode();
    	assertNotNull("No filter element in connectorRef (JAXB)", filterElement);
    	EvaluationTimeType resolutionTime = connectorRef.getResolutionTime();
    	if (isSimple) {
    		assertEquals("Wrong resolution time in connectorRef (JAXB)", EvaluationTimeType.RUN, resolutionTime);
    	} else {
    		assertEquals("Wrong resolution time in connectorRef (JAXB)", EvaluationTimeType.IMPORT, resolutionTime);
    	}

    	XmlSchemaType xmlSchemaType = resourceType.getSchema();
    	SchemaHandlingType schemaHandling = resourceType.getSchemaHandling();
    	if (isSimple) {
    		assertNull("Schema sneaked in", xmlSchemaType);
    		assertNull("SchemaHandling sneaked in", schemaHandling);
    	} else {
	    	assertNotNull("No schema element (JAXB)", xmlSchemaType);
	    	SchemaDefinitionType definition = xmlSchemaType.getDefinition();
	    	assertNotNull("No definition element in schema (JAXB)", definition);
	    	List<Element> anyElements = definition.getAny();
	    	assertNotNull("Null element list in definition element in schema (JAXB)", anyElements);
	    	assertFalse("Empty element list in definition element in schema (JAXB)", anyElements.isEmpty());

			assertNotNull("No schema handling (JAXB)", schemaHandling);
			for(ResourceObjectTypeDefinitionType accountType: schemaHandling.getObjectType()) {
				String name = accountType.getIntent();
				assertNotNull("Account type without a name", name);
				assertNotNull("Account type "+name+" does not have an objectClass", accountType.getObjectClass());
                boolean foundDescription = false;
                boolean foundDepartmentNumber = false;
                for (ResourceAttributeDefinitionType attributeDefinitionType : accountType.getAttribute()) {
                    if ("description".equals(ItemPathUtil.getOnlySegmentQName(attributeDefinitionType.getRef()).getLocalPart())) {
                        foundDescription = true;
                        MappingType outbound = attributeDefinitionType.getOutbound();
                        JAXBElement<?> valueEvaluator = outbound.getExpression().getExpressionEvaluator().get(0);
                        System.out.println("value evaluator for description = " + valueEvaluator);
                        assertNotNull("no expression evaluator for description", valueEvaluator);
                        assertEquals("wrong expression evaluator element name for description", SchemaConstantsGenerated.C_VALUE, valueEvaluator.getName());
                        assertEquals("wrong expression evaluator actual type for description", RawType.class, valueEvaluator.getValue().getClass());
                    } else if ("departmentNumber".equals(ItemPathUtil.getOnlySegmentQName(attributeDefinitionType.getRef()).getLocalPart())) {
                        foundDepartmentNumber = true;
                        MappingType outbound = attributeDefinitionType.getOutbound();
                        VariableBindingDefinitionType source = outbound.getSource().get(0);
                        System.out.println("source for departmentNumber = " + source);
                        assertNotNull("no source for outbound mapping for departmentNumber", source);
                        //<path xmlns:z="http://z/">$user/extension/z:dept</path>
                        ItemPath expected = new ItemPath(
                                new NameItemPathSegment(new QName("user"), true),
                                new NameItemPathSegment(new QName("extension")),
								namespaces ?
										new NameItemPathSegment(new QName("http://z/", "dept")) :
										new NameItemPathSegment(new QName("dept"))
						);
                        PrismAsserts.assertPathEqualsExceptForPrefixes("source for departmentNubmer", expected, source.getPath().getItemPath());
                    }
                }
                assertTrue("ri:description attribute was not found", foundDescription);
                assertTrue("ri:departmentNumber attribute was not found", foundDepartmentNumber);
			}

            // checking <class> element in fetch result
            OperationResultType fetchResult = resourceType.getFetchResult();
            assertNotNull("No fetchResult (JAXB)", fetchResult);
            JAXBElement<?> value = fetchResult.getParams().getEntry().get(0).getEntryValue();
            assertNotNull("No fetchResult param value (JAXB)", value);
            assertEquals("Wrong value class", UnknownJavaObjectType.class, value.getValue().getClass());
            UnknownJavaObjectType unknownJavaObjectType = (UnknownJavaObjectType) value.getValue();
            assertEquals("Wrong value class", "my.class", unknownJavaObjectType.getClazz());
            assertEquals("Wrong value toString value", "my.value", unknownJavaObjectType.getToString());
    	}
	}

	// Try to serialize it to DOM using just DOM processor. See if it does not fail.
	private void serializeDom(PrismObject<ResourceType> resource) throws SchemaException {
//		DomParser domProcessor = PrismTestUtil.getPrismContext().getParserDom();
//		XNodeProcessor xnodeProcessor = PrismTestUtil.getPrismContext().getXnodeProcessor();
//		RootXNode xnode = xnodeProcessor.serializeObject(resource);
//		Element domElement = domProcessor.serializeXRootToElement(xnode);
//		assertNotNull("Null resulting DOM element after DOM serialization", domElement);
	}

	@Test
	public void testParseResourceDom() throws Exception {
		if (!"xml".equals(language)) {
			return;
		}
		final String TEST_NAME = "testParseResourceDom";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = getPrismContext();

		// WHEN
		DomLexicalProcessor parserDom = ((PrismContextImpl) prismContext).getParserDom();
		RootXNode xnode = parserDom.read(new ParserFileSource(getFile(TestConstants.RESOURCE_FILE_BASENAME)), ParsingContext.createDefault());
		PrismObject<ResourceType> resource = prismContext.parserFor(xnode).parse();

		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());

		assertResource(resource, true, true, false);
	}

	@Test
	public void testParseResourceDomSimple() throws Exception {
		if (!"xml".equals(language)) {
			return;
		}
		displayTestTitle("testParseResourceDomSimple");

		// GIVEN
		PrismContext prismContext = getPrismContext();

		Document document = DOMUtil.parseFile(getFile(TestConstants.RESOURCE_FILE_SIMPLE_BASENAME));
		Element resourceElement = DOMUtil.getFirstChildElement(document);

		// WHEN
		PrismObject<ResourceType> resource = prismContext.parserFor(resourceElement).parse();

		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());

		assertResource(resource, true, true, true);
	}

}
