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

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.api.UcfException;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import java.util.Set;
import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.File;
import java.util.HashSet;
import javax.xml.namespace.QName;

import org.eclipse.core.resources.ResourceAttributes;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

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
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class OpenDjUcfTest extends AbstractIntegrationTest {
	
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String FILENAME_RESOURCE_OPENDJ_BAD = "src/test/resources/ucf/opendj-resource-bad.xml";
	private static final String FILENAME_CONNECTOR_LDAP = "src/test/resources/ucf/ldap-connector.xml";
	
	ResourceType resource;
	ResourceType badResource;
	ConnectorType connectorType;
	private ConnectorFactory manager;
	private ConnectorInstance cc;
	Schema schema;
	
	public OpenDjUcfTest() throws JAXBException {
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
	}

	@BeforeClass
    public static void startLdap() throws Exception{
        openDJController.startCleanServer();
    }
    
    @AfterClass
    public static void stopLdap() throws Exception{
        openDJController.stop();
    }

    @BeforeMethod
	public void initUcf() throws Exception {

        File file = new File(FILENAME_RESOURCE_OPENDJ);
        FileInputStream fis = new FileInputStream(file);

        // Resource
        Unmarshaller u = jaxbctx.createUnmarshaller();
        Object object = u.unmarshal(fis);		
		resource = (ResourceType) ((JAXBElement) object).getValue();
		
		// Resource: Second copy for negative test cases
		file = new File(FILENAME_RESOURCE_OPENDJ_BAD);
        fis = new FileInputStream(file);
		object = u.unmarshal(fis);
		badResource = (ResourceType) ((JAXBElement) object).getValue();

		// Connector
		file = new File(FILENAME_CONNECTOR_LDAP);
        fis = new FileInputStream(file);
		object = u.unmarshal(fis);
		connectorType = (ConnectorType) ((JAXBElement) object).getValue();

		ConnectorFactoryIcfImpl managerImpl = new ConnectorFactoryIcfImpl();
		managerImpl.initialize();
		manager = managerImpl;

		cc = manager.createConnectorInstance(connectorType,resource.getNamespace());
		AssertJUnit.assertNotNull(cc);
		cc.configure(resource.getConfiguration(), new OperationResult("initUcf"));
		// TODO: assert something
		
		OperationResult result = new OperationResult(this.getClass().getName()+".initUcf");
		schema = cc.fetchResourceSchema(result);
		
		AssertJUnit.assertNotNull(schema);

    }

    @AfterMethod
	public void shutdownUcf() throws Exception {
    }

	/**
	 * Simple call to connector test() method.
	 * 
	 * @throws Exception 
	 */
	@Test
    public void testTestConnection() throws Exception {
		displayTestTile("testTestConnection");
        //GIVEN

		OperationResult result = new OperationResult("testTestConnection");
		
        //WHEN
		
        cc.test(result);

        //THEN
        result.computeStatus("test failed");
        AssertJUnit.assertNotNull(result);
        OperationResult connectorConnectionResult = result.getSubresults().get(0);
        AssertJUnit.assertNotNull(connectorConnectionResult);
		System.out.println("Test \"connector connection\" result: "+connectorConnectionResult);
        AssertJUnit.assertTrue(connectorConnectionResult.isSuccess());
		AssertJUnit.assertTrue(result.isSuccess());
    }

	
	/**
	 * Simple call to connector test() method.
	 * 
	 * @throws Exception 
	 */
	@Test
    public void testTestConnectionNegative() throws Exception {
		displayTestTile("testTestConnectionNegative");
        //GIVEN

		OperationResult result = new OperationResult("testTestConnectionNegative");
		
		ConnectorInstance badConnector = manager.createConnectorInstance(connectorType,badResource.getNamespace());
		badConnector.configure(badResource.getConfiguration(),result);
		
        //WHEN
		
        badConnector.test(result);

        //THEN
        result.computeStatus("test failed");
        display("Test result (FAILURE EXPECTED)",result);
        AssertJUnit.assertNotNull(result);
        OperationResult connectorConnectionResult = result.getSubresults().get(1);
        AssertJUnit.assertNotNull(connectorConnectionResult);
		System.out.println("Test \"connector connection\" result: "+connectorConnectionResult+" (FAILURE EXPECTED)");
        AssertJUnit.assertTrue("Unexpected success of bad connector test",!connectorConnectionResult.isSuccess());
		AssertJUnit.assertTrue(!result.isSuccess());
    }

	/**
	 * Test fetching and translating resource schema.
	 * 
	 * @throws Exception 
	 */
	@Test
    public void testFetchResourceSchema() throws CommunicationException, SchemaProcessorException {
		displayTestTile("testFetchResourceSchema");
		// GIVEN
		
		// WHEN
		
		// The schema was fetched during test init. Now just check if it was OK.
		
		// THEN
		
		AssertJUnit.assertNotNull(schema);
		
		System.out.println(schema.dump());
		
		Document xsdSchema = Schema.serializeToXsd(schema);
		
		System.out.println("-------------------------------------------------------------------------------------");
		System.out.println(DOMUtil.printDom(xsdSchema));
		System.out.println("-------------------------------------------------------------------------------------");
		
		ResourceObjectDefinition accountDefinition = (ResourceObjectDefinition) schema.findContainerDefinitionByType(new QName(resource.getNamespace(),"AccountObjectClass"));
		AssertJUnit.assertNotNull(accountDefinition);
		
		AssertJUnit.assertFalse("No identifiers for account object class ",accountDefinition.getIdentifiers().isEmpty());
		
		PropertyDefinition uidDefinition = accountDefinition.findPropertyDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		AssertJUnit.assertNotNull(uidDefinition);
		
		for (Definition def : schema.getDefinitions()) {
			ResourceObjectDefinition rdef = (ResourceObjectDefinition)def;
			assertNotEmpty("No type name in object class",rdef.getTypeName());
			assertNotEmpty("No native object class for "+rdef.getTypeName(),rdef.getNativeObjectClass());
			
			// This is maybe not that important, but just for a sake of completeness
			assertNotEmpty("No name for "+rdef.getTypeName(),rdef.getName());
		}
		
	}

	@Test
	public void testFetchObject() throws Exception {
		displayTestTile("testFetchObject");
		// GIVEN
		
		// Account type is hardcoded now
		ResourceObjectDefinition accountDefinition = (ResourceObjectDefinition) schema.findContainerDefinitionByType(new QName(resource.getNamespace(),"AccountObjectClass"));
		// Determine identifier from the schema
		ResourceObject resourceObject = accountDefinition.instantiate();
		
		ResourceObjectAttributeDefinition road = accountDefinition.findAttributeDefinition(new QName(resource.getNamespace(), "sn"));
		ResourceObjectAttribute roa = road.instantiate();
		roa.setValue("Teell");
		resourceObject.getAttributes().add(roa);
		
		road = accountDefinition.findAttributeDefinition(new QName(resource.getNamespace(), "cn"));
		roa = road.instantiate();
		roa.setValue("Teell William");
		resourceObject.getAttributes().add(roa);
		
		road = accountDefinition.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		roa = road.instantiate();
		roa.setValue("uid=Teell,ou=People,dc=example,dc=com");
		resourceObject.getAttributes().add(roa);
		
		OperationResult addResult = new OperationResult(this.getClass().getName()+".testFetchObject");
		Set<ResourceObjectAttribute> attrs = cc.addObject(resourceObject, null, addResult);
		resourceObject = accountDefinition.instantiate();
		resourceObject.getAttributes().addAll(attrs);
		
		
		Set<ResourceObjectAttributeDefinition> identifierDefinition = accountDefinition.getIdentifiers();
		Set<ResourceObjectAttribute> identifiers = new HashSet<ResourceObjectAttribute>();
		for (Property property : resourceObject.getIdentifiers()) {
			ResourceObjectAttribute identifier = new ResourceObjectAttribute(property.getName(), property.getDefinition(), property.getValues());
			System.out.println("Fetch: Identifier "+identifier);
			identifiers.add(identifier);
		}
		// Determine object class from the schema
		QName objectClass = accountDefinition.getTypeName();
		
		OperationResult result = new OperationResult(this.getClass().getName()+".testFetchObject");
	
		// WHEN
		ResourceObject ro = cc.fetchObject(objectClass, identifiers, result);
		
		// THEN
		
		AssertJUnit.assertNotNull(ro);
		System.out.println("Fetched object "+ro);
		System.out.println("Result:");
		System.out.println(result.dump());
		
	}

	@Test
	public void testSearch() throws UcfException {
		displayTestTile("testSearch");
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem(com.evolveum.midpoint.common.result.OperationResult)
	 */
	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
