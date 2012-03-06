/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner] Portions Copyrighted 2011
 * Peter Prochazka
 */
package com.evolveum.midpoint.provisioning.test.ucf;

import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SchemaConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * Simple UCF tests. No real resource, just basic setup and sanity.
 * 
 * @author Radovan Semancik
 * 
 * This is an UCF test. It shold not need repository or other things from the midPoint spring context
 * except from the provisioning beans. But due to a general issue with spring context initialization
 * this is a lesser evil for now (MID-392)
 */
@ContextConfiguration(locations = { 
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-configuration-test-no-repo.xml" })
public class TestUcfDummy extends AbstractTestNGSpringContextTests {

	private static final String FILENAME_RESOURCE_DUMMY = "src/test/resources/object/resource-dummy.xml";
	private static final String FILENAME_CONNECTOR_DUMMY = "src/test/resources/ucf/connector-dummy.xml";

	ConnectorFactory manager;
	PrismObject<ResourceType> resource;
	ResourceType resourceType;
	ConnectorType connectorType;

	@Autowired(required = true)
	ConnectorFactory connectorFactoryIcfImpl;
	
	private static Trace LOGGER = TraceManager.getTrace(TestUcfDummy.class);
	
	@BeforeClass
	public void setup() throws SchemaException, SAXException, IOException {
		displayTestTile("setup");
		System.setProperty("midpoint.home", "target/midPointHome/");

		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
		
		manager = connectorFactoryIcfImpl;

		resource = PrismTestUtil.parseObject(new File(FILENAME_RESOURCE_DUMMY));
		resourceType = resource.asObjectable();

		PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(new File (FILENAME_CONNECTOR_DUMMY));
		connectorType = connector.asObjectable();
	}
	
	@Test
	public void test000PrismContextSanity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000PrismContextSanity");
		
		SchemaRegistry schemaRegistry = PrismTestUtil.getPrismContext().getSchemaRegistry();
		PrismSchema schemaIcfc = schemaRegistry.findSchemaByNamespace(ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION);
		assertNotNull("ICFC schema not found in the context ("+ConnectorFactoryIcfImpl.NS_ICF_CONFIGURATION+")", schemaIcfc);
		PrismContainerDefinition configurationPropertiesDef = 
			schemaIcfc.findContainerDefinitionByElementName(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("icfc:configurationProperties not found in icfc schema ("+
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME+")", configurationPropertiesDef);
		PrismSchema schemaIcfs = schemaRegistry.findSchemaByNamespace(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA);
		assertNotNull("ICFS schema not found in the context ("+ConnectorFactoryIcfImpl.NS_ICF_SCHEMA+")", schemaIcfs);
	
		//"http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/configuration-1.xsd";
	}
	
	@Test
	public void test001ResourceSanity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test001ResourceSanity");
		
		display("Resource", resource);
		
		assertEquals("Wrong oid", "ef2bc95b-76e0-59e2-86d6-9999dddddddd", resource.getOid());
//		assertEquals("Wrong version", "42", resource.getVersion());
		PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
		assertNotNull("No resource definition", resourceDefinition);
		PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstants.NS_COMMON, "resource"), 
				ResourceType.COMPLEX_TYPE, ResourceType.class);
		assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("asObjectable resulted in null", resourceType);

		assertPropertyValue(resource, "name", "Dummy Resource");
		assertPropertyDefinition(resource, "name", DOMUtil.XSD_STRING, 0, 1);		
				
		PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONFIGURATION);
		assertContainerDefinition(configurationContainer, "configuration", ResourceConfigurationType.COMPLEX_TYPE, 1, 1);
		PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
		List<Item<?>> configItems = configContainerValue.getItems();
		assertEquals("Wrong number of config items", 1, configItems.size());
		
		PrismContainer<?> dummyConfigPropertiesContainer = configurationContainer.findContainer(
				ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("No icfc:configurationProperties container", dummyConfigPropertiesContainer);
		List<Item<?>> dummyConfigPropItems = dummyConfigPropertiesContainer.getValue().getItems();
		assertEquals("Wrong number of dummy ConfigPropItems items", 1, dummyConfigPropItems.size());
	}

	@Test
	public void test002ConnectorSchema() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test002ConnectorSchema");
		
		ConnectorInstance cc = manager.createConnectorInstance(connectorType, resourceType.getNamespace());
		assertNotNull("Failed to instantiate connector", cc);
		PrismSchema connectorSchema = cc.generateConnectorSchema();
		ProvisioningTestUtil.assertConnectorSchemaSanity(connectorSchema, "generated");
		assertEquals("Unexpected number of definitions", 3, connectorSchema.getDefinitions().size());

		
		Document xsdSchemaDom = connectorSchema.serializeToXsd();
		assertNotNull("No serialized connector schema", xsdSchemaDom);
		display("Serialized XSD connector schema", DOMUtil.serializeDOMToString(xsdSchemaDom));
		
		// Try to re-parse
		PrismSchema reparsedConnectorSchema = PrismSchema.parse(DOMUtil.getFirstChildElement(xsdSchemaDom), PrismTestUtil.getPrismContext());
		ProvisioningTestUtil.assertConnectorSchemaSanity(reparsedConnectorSchema, "re-parsed");
		assertEquals("Unexpected number of definitions in re-parsed schema", 3, reparsedConnectorSchema.getDefinitions().size());		
	}
	
	/**
	 * Test listing connectors. Very simple. Just test that the list is
	 * non-empty and that there are mandatory values filled in.
	 * @throws CommunicationException 
	 */
	@Test
	public void test010ListConnectors() throws CommunicationException {
		displayTestTile("test004ListConnectors");
		
		OperationResult result = new OperationResult(TestUcfDummy.class+".testListConnectors");
		Set<ConnectorType> listConnectors = manager.listConnectors(null, result);

		System.out.println("---------------------------------------------------------------------");
		assertNotNull(listConnectors);
		assertFalse(listConnectors.isEmpty());

		for (ConnectorType connector : listConnectors) {
			assertNotNull(connector.getName());
			System.out.println("CONNECTOR OID=" + connector.getOid() + ", name=" + connector.getName() + ", version="
					+ connector.getConnectorVersion());
			System.out.println("--");
			System.out.println(ObjectTypeUtil.dump(connector));
			System.out.println("--");
		}

		System.out.println("---------------------------------------------------------------------");

	}
	
	@Test
	public void test020CreateConfiguredConnector() throws FileNotFoundException, JAXBException,
			ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException, ConfigurationException {
		displayTestTile("test004CreateConfiguredConnector");
		
		ConnectorInstance cc = manager.createConnectorInstance(connectorType, resourceType.getNamespace());
		assertNotNull("Failed to instantiate connector", cc);
		OperationResult result = new OperationResult(TestUcfDummy.class.getName() + ".testCreateConfiguredConnector");
		PrismContainer configContainer = resourceType.getConfiguration().asPrismContainer();
		display("Configuration container", configContainer);
		
		// WHEN
		cc.configure(configContainer, result);
		
		// THEN
		result.computeStatus("test failed");
		assertSuccess("Connector configuration failed", result);
		// TODO: assert something
	}
	
	@Test
	public void test030ResourceSchema() throws ObjectNotFoundException, SchemaException, CommunicationException, GenericFrameworkException, ConfigurationException {
		displayTestTile("test030ResourceSchema");
		
		OperationResult result = new OperationResult(TestUcfDummy.class+".test030ResourceSchema");
		
		ConnectorInstance cc = manager.createConnectorInstance(connectorType, resourceType.getNamespace());
		assertNotNull("Failed to instantiate connector", cc);
		
		PrismContainer configContainer = resourceType.getConfiguration().asPrismContainer();
		display("Configuration container", configContainer);
		cc.configure(configContainer, result);
		
		// WHEN
		ResourceSchema resourceSchema = cc.getResourceSchema(result);
		
		// THEN
		display("Generated resource schema", resourceSchema);
		assertEquals("Unexpected number of definitions", 1, resourceSchema.getDefinitions().size());

		
		Document xsdSchemaDom = resourceSchema.serializeToXsd();
		assertNotNull("No serialized resource schema", xsdSchemaDom);
		display("Serialized XSD resource schema", DOMUtil.serializeDOMToString(xsdSchemaDom));
		
		// Try to re-parse
		PrismSchema reparsedResourceSchema = ResourceSchema.parse(DOMUtil.getFirstChildElement(xsdSchemaDom), PrismTestUtil.getPrismContext());
		assertEquals("Unexpected number of definitions in re-parsed schema", 1, reparsedResourceSchema.getDefinitions().size());		
	}


	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstants.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}
	
	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstants.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}
	
	private void assertContainerDefinition(PrismContainer container, String contName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName qName = new QName(SchemaConstants.NS_COMMON, contName);
		PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
	}
}
