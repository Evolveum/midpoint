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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.test.ucf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.object.ResourceTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * Simple UCF tests. No real resource, just basic setup and sanity.
 * 
 * @author Radovan Semancik
 */
public class SimpleUcfTest {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String FILENAME_CONNECTOR_LDAP = "src/test/resources/ucf/ldap-connector.xml";
	
	ConnectorFactory manager;
	ResourceType resource;
	ConnectorType connectorType;

	public SimpleUcfTest() {
	}

	@Before
	public void setUp() throws FileNotFoundException, JAXBException {
		ConnectorFactoryIcfImpl managerImpl = new ConnectorFactoryIcfImpl();
		managerImpl.initialize();
		manager = managerImpl;

		File file = new File(FILENAME_RESOURCE_OPENDJ);
		FileInputStream fis = new FileInputStream(file);
		Unmarshaller u = null;
		JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		u = jc.createUnmarshaller();
		Object object = u.unmarshal(fis);
		resource = (ResourceType) ((JAXBElement) object).getValue();

		file = new File(FILENAME_CONNECTOR_LDAP);
		fis = new FileInputStream(file);
		object = u.unmarshal(fis);
		connectorType = (ConnectorType) ((JAXBElement) object).getValue();

	}

	@After
	public void tearDown() {
	}

	/**
	 * Test listing connectors. Very simple. Just test that the list is
	 * non-empty and that there are mandatory values filled in.
	 */
	@Test
	public void testListConnectors() {
		Set<ConnectorType> listConnectors = manager.listConnectors(null);

		assertNotNull(listConnectors);
		assertFalse(listConnectors.isEmpty());

		for (ConnectorType connector : listConnectors) {
			assertNotNull(connector.getName());
			System.out.println("CONNECTOR OID=" + connector.getOid() + ", name=" + connector.getName()
					+ ", version=" + connector.getConnectorVersion());
			System.out.println("--");
			System.out.println(ObjectTypeUtil.dump(connector));
			System.out.println("--");
		}

	}
	
	@Test
	public void testConnectorSchema() throws ObjectNotFoundException {
		ConnectorInstance cc = manager.createConnectorInstance(connectorType,resource.getNamespace());
		assertNotNull("Failed to instantiate connector",cc);
		Schema connectorSchema = cc.generateConnectorSchema();
		assertNotNull("No connector schema",connectorSchema);
		display("Generated connector schema",connectorSchema);
		assertFalse("Empty schema returned",connectorSchema.isEmpty());
	}

	@Test
	public void testCreateConfiguredConnector() throws FileNotFoundException, JAXBException,
			com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException, CommunicationException, GenericFrameworkException, SchemaException {

		ConnectorInstance cc = manager.createConnectorInstance(connectorType,resource.getNamespace());
		assertNotNull("Failed to instantiate connector",cc);
		OperationResult result = new OperationResult(SimpleUcfTest.class.getName()+".testCreateConfiguredConnector");
		cc.configure(resource.getConfiguration(),result);
		result.computeStatus("test failed");
		assertSuccess("Connector configuration failed",result);
		// TODO: assert something
	}

}
