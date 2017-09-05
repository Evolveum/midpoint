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
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ShadowResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * UCF test with dummy resource and several connector instances.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public class TestUcfDummyMulti extends AbstractUcfDummyTest {

	private static Trace LOGGER = TraceManager.getTrace(TestUcfDummyMulti.class);

	@Test
	public void test000PrismContextSanity() throws Exception {
		final String TEST_NAME = "test000PrismContextSanity";
		TestUtil.displayTestTitle(TEST_NAME);

		SchemaRegistry schemaRegistry = PrismTestUtil.getPrismContext().getSchemaRegistry();
		PrismSchema schemaIcfc = schemaRegistry.findSchemaByNamespace(SchemaConstants.NS_ICF_CONFIGURATION);
		assertNotNull("ICFC schema not found in the context ("+SchemaConstants.NS_ICF_CONFIGURATION+")", schemaIcfc);
		PrismContainerDefinition<ConnectorConfigurationType> configurationPropertiesDef =
			schemaIcfc.findContainerDefinitionByElementName(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("icfc:configurationProperties not found in icfc schema ("+
				SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME+")", configurationPropertiesDef);
		PrismSchema schemaIcfs = schemaRegistry.findSchemaByNamespace(SchemaConstants.NS_ICF_SCHEMA);
		assertNotNull("ICFS schema not found in the context ("+SchemaConstants.NS_ICF_SCHEMA+")", schemaIcfs);
	}

	@Test
	public void test020CreateConfiguredConnector() throws Exception {
		final String TEST_NAME = "test020CreateConfiguredConnector";
		TestUtil.displayTestTitle(TEST_NAME);

		cc = connectorFactory.createConnectorInstance(connectorType, ResourceTypeUtil.getResourceNamespace(resourceType),
				"test connector");
		assertNotNull("Failed to instantiate connector", cc);
		OperationResult result = new OperationResult(TestUcfDummyMulti.class.getName() + "." + TEST_NAME);
		PrismContainerValue<ConnectorConfigurationType> configContainer = resourceType.getConnectorConfiguration().asPrismContainerValue();
		display("Configuration container", configContainer);

		// WHEN
		cc.configure(configContainer, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);

		resourceSchema = cc.fetchResourceSchema(null, result);
		assertNotNull("No resource schema", resourceSchema);
	}

	@Test
	public void test100AddAccount() throws Exception {
		final String TEST_NAME = "test100AddAccount";
		TestUtil.displayTestTitle(this, TEST_NAME);

		OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);

		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		ShadowType shadowType = new ShadowType();
		PrismTestUtil.getPrismContext().adopt(shadowType);
		shadowType.setName(PrismTestUtil.createPolyStringType(ACCOUNT_JACK_USERNAME));
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		shadowType.setResourceRef(resourceRef);
		shadowType.setObjectClass(defaultAccountDefinition.getTypeName());
		PrismObject<ShadowType> shadow = shadowType.asPrismObject();
		ResourceAttributeContainer attributesContainer = ShadowUtil.getOrCreateAttributesContainer(shadow, defaultAccountDefinition);
		ResourceAttribute<String> icfsNameProp = attributesContainer.findOrCreateAttribute(SchemaConstants.ICFS_NAME);
		icfsNameProp.setRealValue(ACCOUNT_JACK_USERNAME);

		// WHEN
		cc.addObject(shadow, null, null, result);

		// THEN
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_JACK_USERNAME);
		assertNotNull("Account "+ACCOUNT_JACK_USERNAME+" was not created", dummyAccount);
		assertNotNull("Account "+ACCOUNT_JACK_USERNAME+" has no username", dummyAccount.getName());

	}

	@Test
	public void test110SearchNonBlocking() throws Exception {
		final String TEST_NAME = "test100SearchNonBlocking";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN

		final ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		// Determine object class from the schema

		final List<PrismObject<ShadowType>> searchResults = new ArrayList<PrismObject<ShadowType>>();

		ShadowResultHandler handler = new ShadowResultHandler() {

			@Override
			public boolean handle(PrismObject<ShadowType> shadow) {
				System.out.println("Search: found: " + shadow);
				checkUcfShadow(shadow, accountDefinition);
				searchResults.add(shadow);
				return true;
			}
		};

		OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);

		// WHEN
		cc.search(accountDefinition, new ObjectQuery(), handler, null, null, null, null, result);

		// THEN
		assertEquals("Unexpected number of search results", 1, searchResults.size());

		ConnectorOperationalStatus opStat = cc.getOperationalStatus();
		display("stats", opStat);
		assertEquals("Wrong pool active", (Integer)0, opStat.getPoolStatusNumActive());
		assertEquals("Wrong pool active", (Integer)1, opStat.getPoolStatusNumIdle());
	}

	@Test
	public void test200BlockingSearch() throws Exception {
		final String TEST_NAME = "test200BlockingSearch";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN

		final OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);

		final ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		// Determine object class from the schema

		final List<PrismObject<ShadowType>> searchResults = new ArrayList<PrismObject<ShadowType>>();

		final ShadowResultHandler handler = new ShadowResultHandler() {

			@Override
			public boolean handle(PrismObject<ShadowType> shadow) {
				checkUcfShadow(shadow, accountDefinition);
				searchResults.add(shadow);
				return true;
			}
		};

		dummyResource.setBlockOperations(true);

		// WHEN
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					cc.search(accountDefinition, new ObjectQuery(), handler, null, null, null, null, result);
				} catch (CommunicationException | GenericFrameworkException | SchemaException
						| SecurityViolationException | ObjectNotFoundException e) {
					LOGGER.error("Error in the search: {}", e.getMessage(), e);
				}
			}
		});
		t.setName("search1");
		t.start();

		// Give the new thread a chance to get blocked
		Thread.sleep(500);

		ConnectorOperationalStatus opStat = cc.getOperationalStatus();
		display("stats (blocked)", opStat);
		assertEquals("Wrong pool active", (Integer)1, opStat.getPoolStatusNumActive());
		assertEquals("Wrong pool active", (Integer)0, opStat.getPoolStatusNumIdle());

		assertEquals("Unexpected number of search results", 0, searchResults.size());

		dummyResource.unblock();

		t.join();

		dummyResource.setBlockOperations(false);

		// THEN
		assertEquals("Unexpected number of search results", 1, searchResults.size());

		opStat = cc.getOperationalStatus();
		display("stats (final)", opStat);
		assertEquals("Wrong pool active", (Integer)0, opStat.getPoolStatusNumActive());
		assertEquals("Wrong pool active", (Integer)1, opStat.getPoolStatusNumIdle());

		PrismObject<ShadowType> searchResult = searchResults.get(0);
		display("Search result", searchResult);
	}

	@Test
	public void test210TwoBlockingSearches() throws Exception {
		final String TEST_NAME = "test210TwoBlockingSearches";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN


		final ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		// Determine object class from the schema

		final OperationResult result1 = new OperationResult(this.getClass().getName() + "." + TEST_NAME);
		final List<PrismObject<ShadowType>> searchResults1 = new ArrayList<PrismObject<ShadowType>>();
		final ShadowResultHandler handler1 = new ShadowResultHandler() {
			@Override
			public boolean handle(PrismObject<ShadowType> shadow) {
				checkUcfShadow(shadow, accountDefinition);
				searchResults1.add(shadow);
				return true;
			}
		};

		final OperationResult result2 = new OperationResult(this.getClass().getName() + "." + TEST_NAME);
		final List<PrismObject<ShadowType>> searchResults2 = new ArrayList<PrismObject<ShadowType>>();
		final ShadowResultHandler handler2 = new ShadowResultHandler() {
			@Override
			public boolean handle(PrismObject<ShadowType> shadow) {
				checkUcfShadow(shadow, accountDefinition);
				searchResults2.add(shadow);
				return true;
			}
		};

		dummyResource.setBlockOperations(true);

		// WHEN
		Thread t1 = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					cc.search(accountDefinition, new ObjectQuery(), handler1, null, null, null, null, result1);
				} catch (CommunicationException | GenericFrameworkException | SchemaException
						| SecurityViolationException | ObjectNotFoundException e) {
					LOGGER.error("Error in the search: {}", e.getMessage(), e);
				}
			}
		});
		t1.setName("search1");
		t1.start();

		// Give the new thread a chance to get blocked
		Thread.sleep(500);

		ConnectorOperationalStatus opStat = cc.getOperationalStatus();
		display("stats (blocked 1)", opStat);
		assertEquals("Wrong pool active", (Integer)1, opStat.getPoolStatusNumActive());
		assertEquals("Wrong pool active", (Integer)0, opStat.getPoolStatusNumIdle());

		assertEquals("Unexpected number of search results", 0, searchResults1.size());

		Thread t2 = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					cc.search(accountDefinition, new ObjectQuery(), handler2, null, null, null, null, result2);
				} catch (CommunicationException | GenericFrameworkException | SchemaException
						| SecurityViolationException | ObjectNotFoundException e) {
					LOGGER.error("Error in the search: {}", e.getMessage(), e);
				}
			}
		});
		t2.setName("search2");
		t2.start();

		// Give the new thread a chance to get blocked
		Thread.sleep(500);

		opStat = cc.getOperationalStatus();
		display("stats (blocked 2)", opStat);
		assertEquals("Wrong pool active", (Integer)2, opStat.getPoolStatusNumActive());
		assertEquals("Wrong pool active", (Integer)0, opStat.getPoolStatusNumIdle());

		assertEquals("Unexpected number of search results", 0, searchResults1.size());

		dummyResource.unblockAll();

		t1.join();
		t2.join();

		dummyResource.setBlockOperations(false);

		// THEN
		assertEquals("Unexpected number of search results 1", 1, searchResults1.size());
		assertEquals("Unexpected number of search results 2", 1, searchResults2.size());

		opStat = cc.getOperationalStatus();
		display("stats (final)", opStat);
		assertEquals("Wrong pool active", (Integer)0, opStat.getPoolStatusNumActive());
		assertEquals("Wrong pool active", (Integer)2, opStat.getPoolStatusNumIdle());

		PrismObject<ShadowType> searchResult1 = searchResults1.get(0);
		display("Search result 1", searchResult1);

		PrismObject<ShadowType> searchResult2 = searchResults2.get(0);
		display("Search result 2", searchResult2);
	}

	private void checkUcfShadow(PrismObject<ShadowType> shadow, ObjectClassComplexTypeDefinition objectClassDefinition) {
		assertNotNull("No objectClass in shadow "+shadow, shadow.asObjectable().getObjectClass());
		assertEquals("Wrong objectClass in shadow "+shadow, objectClassDefinition.getTypeName(), shadow.asObjectable().getObjectClass());
		Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
		assertNotNull("No attributes in shadow "+shadow, attributes);
		assertFalse("Empty attributes in shadow "+shadow, attributes.isEmpty());
	}


}
