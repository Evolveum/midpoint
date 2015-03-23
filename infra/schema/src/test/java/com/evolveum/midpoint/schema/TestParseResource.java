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

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ExpressionWrapper;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.JaxbTestUtil;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.evolveum.midpoint.schema.util.SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author semancik
 *
 */
public class TestParseResource {
	
	public static final File RESOURCE_NO_XMLNS_FILE = new File(TestConstants.COMMON_DIR, "resource-opendj-no-xmlns.xml");
	public static final File RESOURCE_SIMPLE_FILE = new File(TestConstants.COMMON_DIR, "resource-opendj-simple.xml");
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testParseResourceFile() throws Exception {
		System.out.println("===[ testParseResourceFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(TestConstants.RESOURCE_FILE);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());
		
		assertResource(resource, true, true, false);
	}
	
	@Test
	public void testParseResourceFileSimple() throws Exception {
		System.out.println("===[ testParseResourceFileSimple ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_SIMPLE_FILE);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());
		
		assertResource(resource, true, true, true);
	}

	@Test
	public void testParseResourceDom() throws Exception {
		final String TEST_NAME = "testParseResourceDom";
		PrismTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		DomParser parserDom = prismContext.getParserDom();
		XNode xnode = parserDom.parse(TestConstants.RESOURCE_FILE);
		PrismObject<ResourceType> resource = prismContext.getXnodeProcessor().parseObject(xnode);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());
		
		assertResource(resource, true, true, false);
	}
	
	@Test
	public void testParseResourceDomSimple() throws Exception {
		System.out.println("===[ testParseResourceDomSimple ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		Document document = DOMUtil.parseFile(RESOURCE_SIMPLE_FILE);
		Element resourceElement = DOMUtil.getFirstChildElement(document);
		
		// WHEN
		PrismObject<ResourceType> resource = prismContext.parseObject(resourceElement);
		
		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());
		
		assertResource(resource, true, true, true);
	}

    @Deprecated
    @Test(enabled = false)
	public void testPrismParseJaxb() throws Exception {
		System.out.println("===[ testPrismParseJaxb ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();

		// WHEN
		ResourceType resourceType = jaxbProcessor.unmarshalObject(TestConstants.RESOURCE_FILE, ResourceType.class);
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, false);
	}

    @Deprecated
    @Test(enabled = false)
	public void testPrismParseJaxbSimple() throws Exception {
		System.out.println("===[ testPrismParseJaxbSimple ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();
		
		// WHEN
		ResourceType resourceType = jaxbProcessor.unmarshalObject(RESOURCE_SIMPLE_FILE, ResourceType.class);
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, true);
	}
	
	/**
	 * The definition should be set properly even if the declared type is ObjectType. The Prism should determine
	 * the actual type.
	 */
    @Deprecated
    @Test(enabled = false)
	public void testPrismParseJaxbObjectType() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbObjectType ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();
		
		// WHEN
		ObjectType resourceType = jaxbProcessor.unmarshalObject(TestConstants.RESOURCE_FILE, ObjectType.class);
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, false);
	}
	
	/**
	 * Parsing in form of JAXBELement
	 */
    @Deprecated
    @Test(enabled = false)
	public void testPrismParseJaxbElement() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElement ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();
		
		// WHEN
		JAXBElement<ResourceType> jaxbElement = jaxbProcessor.unmarshalElement(TestConstants.RESOURCE_FILE, ResourceType.class);
		ResourceType resourceType = jaxbElement.getValue();
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, false);
	}

	/**
	 * Parsing in form of JAXBELement, with declared ObjectType
	 */
    @Deprecated
    @Test(enabled = false)
	public void testPrismParseJaxbElementObjectType() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElementObjectType ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();
		
		// WHEN
		JAXBElement<ObjectType> jaxbElement = jaxbProcessor.unmarshalElement(TestConstants.RESOURCE_FILE, ObjectType.class);
		ObjectType resourceType = jaxbElement.getValue();
		
		// THEN
		// HACK: the JAXB parsing methods do not support filter yet, so avoid checking for it
		assertResource(resourceType.asPrismObject(), false, true, false);
	}

	@Test
	public void testParseResourceRoundtrip() throws Exception {
		System.out.println("===[ testParseResourceRoundtrip ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		PrismObject<ResourceType> resource = prismContext.parseObject(TestConstants.RESOURCE_FILE);
		
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());
		
		assertResource(resource, true, false, false);
		
		// SERIALIZE
		
		String serializedResource = prismContext.serializeObjectToString(resource, PrismContext.LANG_XML);
		
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

    @Test
    public void testParseResourceRoundtripNoNamespaces() throws Exception {
        System.out.println("===[ testParseResourceRoundtripNoNamespaces ]===");

        // GIVEN
        PrismContext prismContext = PrismTestUtil.getPrismContext();

        PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_NO_XMLNS_FILE);

        System.out.println("Parsed resource:");
        System.out.println(resource.debugDump());

        assertResource(resource, true, false, false);

        // SERIALIZE

        String serializedResource = prismContext.serializeObjectToString(resource, PrismContext.LANG_XML);

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
    }


    /**
	 * Serialize and parse "schema" element on its own. There may be problems e.g. with preservation
	 * of namespace definitions.
	 */
	@Test
	public void testSchemaRoundtrip() throws Exception {
		System.out.println("===[ testSchemaRoundtrip ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		PrismObject<ResourceType> resource = prismContext.parseObject(TestConstants.RESOURCE_FILE);
				
		assertResource(resource, true, false, false);
		
		PrismContainer<Containerable> schemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
		
		System.out.println("Parsed schema:");
		System.out.println(schemaContainer.debugDump());

		// SERIALIZE
		
		String serializesSchema = prismContext.serializeContainerValueToString(schemaContainer.getValue(), new QName("fakeNs", "fake"), PrismContext.LANG_XML);
		
		System.out.println("serialized schema:");
		System.out.println(serializesSchema);
		
		// RE-PARSE
		PrismContainer<Containerable> reparsedSchemaContainer = prismContext.parseContainer(serializesSchema, schemaContainer.getDefinition(), PrismContext.LANG_XML);
		
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
		ResourceSchema reparsedSchema = ResourceSchema.parse(reparsedXsdSchemaElement, "reparsed schema", prismContext);
		
	}
	
	private void assertResource(PrismObject<ResourceType> resource, boolean checkConsistence, boolean checkJaxb, boolean isSimple) 
			throws SchemaException, JAXBException {
		if (checkConsistence) {
			resource.checkConsistence();
		}
		assertResourcePrism(resource, isSimple);
		assertResourceJaxb(resource.asObjectable(), isSimple);
		
		if (checkJaxb) {
			serializeDom(resource);
			//serializeJaxb(resource);
		}
	}

	private void assertResourcePrism(PrismObject<ResourceType> resource, boolean isSimple) throws SchemaException {

        PrismContext prismContext = PrismTestUtil.getPrismContext();

		assertEquals("Wrong oid (prism)", TestConstants.RESOURCE_OID, resource.getOid());
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
            PrismAsserts.assertPathEqualsExceptForPrefixes("Wrong filter path", new ItemPath(new QName("extension"), new QName("http://x/", "extConnType")), path);
            PrismPropertyValue filterValue = (PrismPropertyValue) equalFilter.getValues().get(0);
            assertEquals("Wrong filter value", "org.identityconnectors.ldap.LdapConnector", ((String) filterValue.getValue()).trim());
        }

        PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertContainerDefinition(configurationContainer, "configuration", ConnectorConfigurationType.COMPLEX_TYPE, 1, 1);
		PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
		List<Item<?>> configItems = configContainerValue.getItems();
		assertEquals("Wrong number of config items", isSimple ? 1 : 4, configItems.size());
		
		PrismContainer<?> ldapConfigPropertiesContainer = configurationContainer.findContainer(ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No icfcldap:configurationProperties container", ldapConfigPropertiesContainer);
		PrismContainerDefinition<?> ldapConfigPropertiesContainerDef = ldapConfigPropertiesContainer.getDefinition();
		assertNotNull("No icfcldap:configurationProperties container definition", ldapConfigPropertiesContainerDef);
		assertEquals("icfcldap:configurationProperties container definition maxOccurs", 1, ldapConfigPropertiesContainerDef.getMaxOccurs());
		List<Item<?>> ldapConfigPropItems = ldapConfigPropertiesContainer.getValue().getItems();
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
                    new ItemPath(
                            new NameItemPathSegment(new QName("account"), true),
                            new NameItemPathSegment(new QName(SchemaConstantsGenerated.NS_COMMON, "attributes")),
                            new NameItemPathSegment(new QName("http://myself.me/schemas/whatever", "yyy"))
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
                        MappingSourceDeclarationType source = outbound.getSource().get(0);
                        System.out.println("source for departmentNumber = " + source);
                        assertNotNull("no source for outbound mapping for departmentNumber", source);
                        //<path xmlns:z="http://z/">$user/extension/z:dept</path>
                        ItemPath expected = new ItemPath(
                                new NameItemPathSegment(new QName("user"), true),
                                new NameItemPathSegment(new QName("extension")),
                                new NameItemPathSegment(new QName("http://z/", "dept")));
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
		DomParser domParser = PrismTestUtil.getPrismContext().getParserDom();
		XNodeProcessor xnodeProcessor = PrismTestUtil.getPrismContext().getXnodeProcessor();
		RootXNode xnode = xnodeProcessor.serializeObject(resource);
		Element domElement = domParser.serializeXRootToElement(xnode);
		assertNotNull("Null resulting DOM element after DOM serialization", domElement);
	}

	// Try to serialize it to DOM using JAXB processor. See if it does not fail.
    @Deprecated
	private void serializeJaxb(PrismObject<ResourceType> resource) throws SchemaException, JAXBException {
        JaxbTestUtil jaxbProcessor = JaxbTestUtil.getInstance();
		Document document = DOMUtil.getDocument();
		Element element = jaxbProcessor.marshalObjectToDom(resource.asObjectable(), new QName(SchemaConstants.NS_C, "resorce"), document);
		System.out.println("JAXB serialization result:\n"+DOMUtil.serializeDOMToString(element));
		assertNotNull("No resulting DOM element after JAXB serialization", element);
		assertNotNull("Empty resulting DOM element after JAXB serialization", element.getChildNodes().getLength() != 0);
	}

	
	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}
	
	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}
	
	private void assertContainerDefinition(PrismContainer container, String contName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName qName = new QName(SchemaConstantsGenerated.NS_COMMON, contName);
		PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
	}

}
