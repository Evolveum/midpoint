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

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorManagerIcfImpl;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.api.UcfException;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.File;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import java.util.HashSet;
import javax.xml.namespace.QName;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;

/**
 * Test UCF implementation with OpenDJ and ICF LDAP connector.
 * 
 * This test is using embedded OpenDJ as a resource and ICF LDAP connector.
 * The test is executed by direct calls to the UCF interface.
 * 
 * @author Radovan Semancik
 * @author Katka Valalikova
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml" })
public class OpenDjUcfTest extends OpenDJUnitTestAdapter {
	
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String FILENAME_RESOURCE_OPENDJ_BAD = "src/test/resources/ucf/opendj-resource-bad.xml";
	
    protected static OpenDJUtil djUtil = new OpenDJUtil();
	private JAXBContext jaxbctx;
	ResourceType resource;
	ResourceType badResource;
	private ConnectorManager manager;
	private ConnectorInstance cc;
	Schema schema;
	
	public OpenDjUcfTest() throws JAXBException {
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@BeforeClass
    public static void startLdap() throws Exception{
        startACleanDJ();
    }
    
    @AfterClass
    public static void stopLdap() throws Exception{
        stopDJ();
    }

    @Before
    public void initUcf() throws Exception {

        File file = new File(FILENAME_RESOURCE_OPENDJ);
        FileInputStream fis = new FileInputStream(file);

        Unmarshaller u = jaxbctx.createUnmarshaller();
        Object object = u.unmarshal(fis);		
		resource = (ResourceType) ((JAXBElement) object).getValue();
		
		// Second copy for negative test cases
		file = new File(FILENAME_RESOURCE_OPENDJ_BAD);
        fis = new FileInputStream(file);
		object = u.unmarshal(fis);
		badResource = (ResourceType) ((JAXBElement) object).getValue();
		
		ConnectorManagerIcfImpl managerImpl = new ConnectorManagerIcfImpl();
		managerImpl.initialize();
		manager = managerImpl;

		cc = manager.createConnectorInstance(resource);

		assertNotNull(cc);
		
		OperationResult result = new OperationResult(this.getClass().getName()+".initUcf");
		schema = cc.fetchResourceSchema(result);
		
		assertNotNull(schema);

    }

    @After
    public void shutdownUcf() throws Exception {
    }

	/**
	 * Simple call to connector test() method.
	 * 
	 * @throws Exception 
	 */
	@Test
    public void testTestConnection() throws Exception {
		System.out.println("*** Positive test connection");
        //GIVEN

		OperationResult result = new OperationResult("testTestConnection");
		
        //WHEN
		
        cc.test(result);

        //THEN
        assertNotNull(result);
        OperationResult connectorConnectionResult = result.getSubresults().get(0);
        assertNotNull(connectorConnectionResult);
		System.out.println("Test \"connector connection\" result: "+connectorConnectionResult);
        assertTrue(connectorConnectionResult.isSuccess());
		assertTrue(result.isSuccess());
    }

	
	/**
	 * Simple call to connector test() method.
	 * 
	 * @throws Exception 
	 */
	@Test
    public void testTestConnectionNegative() throws Exception {
		System.out.println("*** Negative test connection");
        //GIVEN

		ConnectorInstance badConnector = manager.createConnectorInstance(badResource);
		
		OperationResult result = new OperationResult("testTestConnectionNegative");
		
        //WHEN
		
        badConnector.test(result);

        //THEN
        assertNotNull(result);
        OperationResult connectorConnectionResult = result.getSubresults().get(0);
        assertNotNull(connectorConnectionResult);
		System.out.println("Test \"connector connection\" result: "+connectorConnectionResult+" (FAILURE EXPECTED)");
		System.out.println(result.debugDump());
        assertTrue(!connectorConnectionResult.isSuccess());
		assertTrue(!result.isSuccess());
    }

	/**
	 * Test fetching and translating resource schema.
	 * 
	 * @throws Exception 
	 */
	@Test
    public void testFetchResourceSchema() throws CommunicationException, SchemaProcessorException {
		System.out.println("*** Fetch resource schema");
		// GIVEN
		
		// WHEN
		
		// The schema was fetched during test init. Now just check if it was OK.
		
		// THEN
		
		assertNotNull(schema);
		
		System.out.println(schema.debugDump());
		
		Document xsdSchema = Schema.parseSchema(schema);
		
		System.out.println("-------------------------------------------------------------------------------------");
		System.out.println(DOMUtil.printDom(xsdSchema));
		System.out.println("-------------------------------------------------------------------------------------");
		
		PropertyContainerDefinition accountDefinition = schema.findContainerDefinitionByType(new QName(resource.getNamespace(),"AccountObjectClass"));
		assertNotNull(accountDefinition);
		
		PropertyDefinition uidDefinition = accountDefinition.findPropertyDefinition(SchemaConstants.ICFS_UID);
		assertNotNull(uidDefinition);
		
	}

	@Test
	public void testFetchObject() throws UcfException {
		System.out.println("*** Fetch resource object");
		// GIVEN
		
		// Account type is hardcoded now
		ResourceObjectDefinition accountDefinition = (ResourceObjectDefinition) schema.findContainerDefinitionByType(new QName(resource.getNamespace(),"AccountObjectClass"));
		// Determine identifier from the schema
		Set<ResourceObjectAttributeDefinition> identifierDefinition = accountDefinition.getIdentifiers();
		Set<ResourceObjectAttribute> identifiers = new HashSet<ResourceObjectAttribute>();
		for (ResourceObjectAttributeDefinition definition : identifierDefinition) {
			ResourceObjectAttribute identifier = definition.instantiate();
			identifier.setValue("dd2b96ec-43f5-3953-ae21-807cedac45ab");
			System.out.println("Fetch: Identifier "+identifier);
			identifiers.add(identifier);
		}
		// Determine object class from the schema
		QName objectClass = accountDefinition.getTypeName();
		
		OperationResult result = new OperationResult(this.getClass().getName()+".testFetchObject");
		
		// WHEN
		ResourceObject ro = cc.fetchObject(objectClass,identifiers,result);
		
		// THEN
		
		assertNotNull(ro);
		System.out.println("Fetched object "+ro);
		System.out.println("Result:");
		System.out.println(result.debugDump());
		
	}

	@Test
	public void testSearch() throws UcfException {
		System.out.println("*** Search");
		// GIVEN
		
		// Account type is hardcoded now
		ResourceObjectDefinition accountDefinition = (ResourceObjectDefinition) schema.findContainerDefinitionByType(new QName(resource.getNamespace(),"AccountObjectClass"));
		// Determine object class from the schema
		QName objectClass = accountDefinition.getTypeName();
		
		ResultHandler handler = new ResultHandler() {

			@Override
			public boolean handle(ResourceObject object) {
				System.out.println("Search: found: "+object);
				return true;
			}
		};
		
		OperationResult result = new OperationResult(this.getClass().getName()+".testSearch");
		
		// WHEN
		cc.search(objectClass,accountDefinition,handler,result);
		
		// THEN
		
		
	}

}
