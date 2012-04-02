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
package com.evolveum.midpoint.provisioning.test.impl;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import org.testng.AssertJUnit;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.ldap.OpenDJController;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;

/** 
 * @author Radovan Semancik
 * @author Katka Valalikova
 */

@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-task.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-test.xml" })
@DirtiesContext
public class TestConnectorDiscovery extends AbstractIntegrationTest {

	private static final String LDAP_CONNECTOR_TYPE = "org.identityconnectors.ldap.LdapConnector";

	@Autowired
	private ProvisioningService provisioningService;

	private static Trace LOGGER = TraceManager.getTrace(TestConnectorDiscovery.class);

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
	}
		
	
	/**
	 * Check whether the connectors were discovered correctly and were added to the repository.
	 * @throws SchemaProcessorException 
	 * 
	 */
	@Test
	public void test001Connectors() throws SchemaException {
		displayTestTile("test001Connectors");
		
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".test001Connectors");
		
		ResultList<PrismObject<ConnectorType>> connectors = repositoryService.listObjects(ConnectorType.class, null, result);
		
		assertFalse("No connector found",connectors.isEmpty());
		display("Found "+connectors.size()+" discovered connector");
		
		for (PrismObject<ConnectorType> connector : connectors) {
			ConnectorType conn = connector.asObjectable();
			display("Found connector " +conn, conn);
//			if (conn.getConnectorType().equals("org.identityconnectors.ldap.LdapConnector")) {
//				// This connector is loaded manually, it has no schema
//				continue;
//			}
			ProvisioningTestUtil.assertConnectorSchemaSanity(conn, prismContext);
		}
		
		assertEquals("Unexpected number of connectors found", 4, connectors.size());
	}
		
	@Test
	public void testListConnectors(){
		displayTestTile("testListConnectors");
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".listConnectorsTest");
		
		ResultList<PrismObject<ConnectorType>> connectors = provisioningService.listObjects(ConnectorType.class, new PagingType(), result);
		assertNotNull(connectors);
		
		for (PrismObject<ConnectorType> connector : connectors){
			ConnectorType conn = connector.asObjectable();
			System.out.println(conn.toString());
			System.out.println("connector name: "+ conn.getName());
			System.out.println("connector type: "+ conn.getConnectorType());
			System.out.println("-----\n");
		}
		
		assertEquals("Unexpected number of connectors found", 4, connectors.size());
	}
	
	@Test
	public void testSearchConnectorSimple() throws SchemaException{
		displayTestTile("testSearchConnectorSimple");
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".testSearchConnector");
		
		PrismObject<ConnectorType> ldapConnector = findConnectorByType(LDAP_CONNECTOR_TYPE, result);
		assertEquals("Type does not match", LDAP_CONNECTOR_TYPE, ldapConnector.asObjectable().getConnectorType());
	}
	
	
	@Test
	public void testSearchConnectorAnd() throws SchemaException{
		displayTestTile("testSearchConnectorAnd");
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".testSearchConnector");
		
		Document doc = DOMUtil.getDocument();
		Element filter = QueryUtil
				.createAndFilter(
						doc,
						QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_CONNECTOR_FRAMEWORK,
								ConnectorFactoryIcfImpl.ICF_FRAMEWORK_URI),
						QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE,
								LDAP_CONNECTOR_TYPE));
	
		QueryType query = new QueryType();
		query.setFilter(filter);
		System.out.println("Query:\n"+DOMUtil.serializeDOMToString(query.getFilter())+"\n--");

		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, null,
				result);
		
		assertEquals("Unexpected number of results", 1, connectors.size());
		PrismObject<ConnectorType> ldapConnector = connectors.get(0);
		assertEquals("Type does not match", LDAP_CONNECTOR_TYPE, ldapConnector.asObjectable().getConnectorType());
		assertEquals("Framework does not match", ConnectorFactoryIcfImpl.ICF_FRAMEWORK_URI, ldapConnector.asObjectable().getFramework());
	}
}
