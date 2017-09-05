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
package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.List;

import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;

/**
 * @author Radovan Semancik
 * @author Katka Valalikova
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestConnectorDiscovery extends AbstractIntegrationTest {

	@Autowired
	private ProvisioningService provisioningService;

	private static Trace LOGGER = TraceManager.getTrace(TestConnectorDiscovery.class);

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
	}


	/**
	 * Check whether the connectors were discovered correctly and were added to the repository.
	 * @throws SchemaException
	 *
	 */
	@Test
	public void test001Connectors() throws Exception {
		final String TEST_NAME = "test001Connectors";
		TestUtil.displayTestTitle(TEST_NAME);

		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName() + "." + TEST_NAME);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, null, null, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertFalse("No connector found",connectors.isEmpty());
		display("Found "+connectors.size()+" discovered connector");

		result.computeStatus();
		TestUtil.assertSuccess(result);

		for (PrismObject<ConnectorType> connector : connectors) {
			ConnectorType conn = connector.asObjectable();
			display("Found connector " +conn, conn);
			IntegrationTestTools.assertConnectorSchemaSanity(conn, prismContext);
		}

		assertEquals("Unexpected number of connectors found", 7, connectors.size());
	}

	@Test
	public void testListConnectors() throws Exception{
		TestUtil.displayTestTitle("testListConnectors");
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".listConnectorsTest");

		List<PrismObject<ConnectorType>> connectors = provisioningService.searchObjects(ConnectorType.class, null, null, null, result);
		assertNotNull(connectors);

		for (PrismObject<ConnectorType> connector : connectors){
			ConnectorType conn = connector.asObjectable();
			System.out.println(conn.toString());
			System.out.println("connector name: "+ conn.getName());
			System.out.println("connector type: "+ conn.getConnectorType());
			System.out.println("-----\n");
		}

		assertEquals("Unexpected number of connectors found", 7, connectors.size());
	}

	@Test
	public void testSearchConnectorSimple() throws SchemaException{
		final String TEST_NAME = "testSearchConnectorSimple";
		TestUtil.displayTestTitle(TEST_NAME);
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ "." + TEST_NAME);

		PrismObject<ConnectorType> ldapConnector = findConnectorByType(IntegrationTestTools.LDAP_CONNECTOR_TYPE, result);
		assertEquals("Type does not match", IntegrationTestTools.LDAP_CONNECTOR_TYPE, ldapConnector.asObjectable().getConnectorType());
	}


	@Test
	public void testSearchConnectorAnd() throws SchemaException{
		TestUtil.displayTestTitle("testSearchConnectorAnd");
		OperationResult result = new OperationResult(TestConnectorDiscovery.class.getName()
				+ ".testSearchConnector");

		ObjectQuery query = QueryBuilder.queryFor(ConnectorType.class, prismContext)
				.item(SchemaConstants.C_CONNECTOR_FRAMEWORK).eq(SchemaConstants.ICF_FRAMEWORK_URI)
				.and().item(SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE).eq(IntegrationTestTools.LDAP_CONNECTOR_TYPE)
				.build();

		System.out.println("Query:\n"+query.debugDump());

		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);

		assertEquals("Unexpected number of results", 1, connectors.size());
		PrismObject<ConnectorType> ldapConnector = connectors.get(0);
		assertEquals("Type does not match", IntegrationTestTools.LDAP_CONNECTOR_TYPE, ldapConnector.asObjectable().getConnectorType());
		assertEquals("Framework does not match", SchemaConstants.ICF_FRAMEWORK_URI, ldapConnector.asObjectable().getFramework());
	}
}
