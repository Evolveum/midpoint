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

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.common.object.ResourceTypeUtil;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import javax.xml.bind.JAXBContext;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.File;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorManagerIcfImpl;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import java.io.FileNotFoundException;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Element;

import static org.junit.Assert.*;

/**
 * Simple UCF tests. No real resource, just basic setup and sanity.
 * 
 * @author Radovan Semancik
 */
public class SimpleUcfTest {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	
	ConnectorManager manager;
	ResourceType resource;

	public SimpleUcfTest() {
	}

	@Before
	public void setUp() throws FileNotFoundException, JAXBException {
		ConnectorManagerIcfImpl managerImpl = new ConnectorManagerIcfImpl();
		managerImpl.initialize();
		manager = managerImpl;
		
        File file = new File(FILENAME_RESOURCE_OPENDJ);
        FileInputStream fis = new FileInputStream(file);

        Unmarshaller u = null;

        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
        u = jc.createUnmarshaller();

        Object object = u.unmarshal(fis);
		
		resource = (ResourceType) ((JAXBElement) object).getValue();

	}

	@After
	public void tearDown() {
	}

	/**
	 * Test listing connectors. Very simple. Just test that the list
	 * is non-empty and that there are mandatory values filled in.
	 */
	@Test
	public void testListConnectors() {
		Set<ConnectorType> listConnectors = manager.listConnectors();
		
		assertNotNull(listConnectors);
		assertFalse(listConnectors.isEmpty());
		
		for (ConnectorType connector : listConnectors) {
			assertNotNull(connector.getOid());
			assertNotNull(connector.getName());
			System.out.println("CONNECTOR OID="+connector.getOid()+", name="+connector.getName()+", version="+connector.getConnectorVersion());
		}
		
	}
	
	@Test
	public void testCreateConfiguredConnector() throws FileNotFoundException, JAXBException {
				
		ConnectorInstance cc = manager.createConnectorInstance(resource);

		assertNotNull(cc);
	}
	
	@Test
	public void testParseResourceSchema() throws SchemaProcessorException {
		Element schemaElement = ResourceTypeUtil.getResourceXsdSchema(resource);
		Schema schema = Schema.parse(schemaElement);
		assertNotNull(schema);
	}

}
