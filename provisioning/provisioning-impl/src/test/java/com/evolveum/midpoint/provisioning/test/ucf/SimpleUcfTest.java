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

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
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
public class SimpleUcfTest extends AbstractTestNGSpringContextTests {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String FILENAME_CONNECTOR_LDAP = "src/test/resources/ucf/ldap-connector.xml";

	ConnectorFactory manager;
	ResourceType resourceType;
	ConnectorType connectorType;

	@Autowired(required = true)
	ConnectorFactory connectorFactoryIcfImpl;
	
	private static Trace LOGGER = TraceManager.getTrace(SimpleUcfTest.class);

	public SimpleUcfTest() {
		System.setProperty("midpoint.home", "target/midPointHome/");
	}
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@BeforeClass
	public static void before() throws Exception {
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("START:  SimpleUcfTest  ");
		LOGGER.info("------------------------------------------------------------------------------");
		
	}

	@AfterClass
	public static void after() throws Exception {
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("STOP:  SimpleUcfTest");
		LOGGER.info("------------------------------------------------------------------------------");	
	}
	
	@BeforeMethod
	public void setUp() throws FileNotFoundException, JAXBException, SchemaException {
		manager = connectorFactoryIcfImpl;

		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(FILENAME_RESOURCE_OPENDJ));
		resourceType = resource.asObjectable();

		PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(new File (FILENAME_CONNECTOR_LDAP));
		connectorType = connector.asObjectable();

	}

	@AfterMethod
	public void tearDown() {
	}

	/**
	 * Test listing connectors. Very simple. Just test that the list is
	 * non-empty and that there are mandatory values filled in.
	 * @throws CommunicationException 
	 */
	@Test
	public void testListConnectors() throws CommunicationException {
		displayTestTile("testListConnectors");
		
		OperationResult result = new OperationResult(SimpleUcfTest.class+".testListConnectors");
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
	public void testConnectorSchema() throws ObjectNotFoundException {
		displayTestTile("testConnectorSchema");
		
		ConnectorInstance cc = manager.createConnectorInstance(connectorType, resourceType.getNamespace());
		assertNotNull("Failed to instantiate connector", cc);
		PrismSchema connectorSchema = cc.generateConnectorSchema();
		assertNotNull("No connector schema", connectorSchema);
		display("Generated connector schema", connectorSchema);
		assertFalse("Empty schema returned", connectorSchema.isEmpty());
	}

	@Test
	public void testCreateConfiguredConnector() throws FileNotFoundException, JAXBException,
			ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException {
		displayTestTile("testCreateConfiguredConnector");
		
		ConnectorInstance cc = manager.createConnectorInstance(connectorType, resourceType.getNamespace());
		assertNotNull("Failed to instantiate connector", cc);
		OperationResult result = new OperationResult(SimpleUcfTest.class.getName() + ".testCreateConfiguredConnector");
		PrismContainer configContainer = resourceType.getConfiguration().asPrismContainer();
		display("Configuration container", configContainer);
		cc.configure(configContainer, result);
		result.computeStatus("test failed");
		assertSuccess("Connector configuration failed", result);
		// TODO: assert something
	}

}
