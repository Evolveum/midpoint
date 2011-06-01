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

import com.evolveum.midpoint.common.DOMUtil;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorManagerIcfImpl;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
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
import com.evolveum.midpoint.test.repository.BaseXDatabaseFactory;
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
import static org.junit.Assert.*;
import org.w3c.dom.Document;

/**
 * Test UCF implementation with OpenDJ and ICF LDAP connector.
 * 
 * This test is using embedded OpenDJ as a resource and ICF LDAP connector.
 * The test is executed by direct calls to the UCF interface.
 * 
 * @author Radovan Semancik
 */
public class OpenDjUcfTest extends OpenDJUnitTestAdapter {
	
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	
    protected static OpenDJUtil djUtil = new OpenDJUtil();
	private JAXBContext jaxbctx;
	ResourceType resource;
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

		ConnectorManagerIcfImpl managerImpl = new ConnectorManagerIcfImpl();
		managerImpl.initialize();
		manager = managerImpl;

		cc = manager.createConnectorInstance(resource);

		assertNotNull(cc);
		
		schema = cc.fetchResourceSchema();
		
		assertNotNull(schema);

    }

    @After
    public void shutdownUcf() throws Exception {
        BaseXDatabaseFactory.XMLServerStop();
    }

	/**
	 * Simple call to connector test() method.
	 * 
	 * @throws Exception 
	 */
	@Test
    public void testTestConnection() throws Exception {
        //GIVEN

        //WHEN
		
        ResourceTestResultType result = cc.test();

        //THEN
        assertNotNull(result);
        TestResultType connectorConnectionResult = result.getConnectorConnection();
        assertNotNull(connectorConnectionResult);
		System.out.println("Test \"connector connection\" result: "+DebugUtil.prettyPrint(connectorConnectionResult));
        assertTrue(connectorConnectionResult.isSuccess());

    }

	/**
	 * Test fetching and translating resource schema.
	 * 
	 * @throws Exception 
	 */
	@Test
    public void testFetchResourceSchema() throws CommunicationException, SchemaProcessorException {
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
	public void testFetchObject() throws CommunicationException {
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
		
		// WHEN
		ResourceObject ro = cc.fetchObject(objectClass,identifiers);
		
		// THEN
		
		assertNotNull(ro);
		System.out.println("Fetched object "+ro);
		
	}

	@Test
	public void testSearch() throws CommunicationException {
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
		
		// WHEN
		cc.search(objectClass,handler);
		
		// THEN
		
		
	}

}
