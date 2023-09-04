/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.provisioning.ucf.api.UcfExecutionContext;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfObjectHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
import com.google.common.io.Files;

/**
 * UCF test with dummy resource and several connector instances.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public class TestUcfDummyMulti extends AbstractUcfDummyTest {

    private static final File CONNECTOR_FAKE = new File("src/test/resources/dummy-connector-fake-4.2.jar");
    private static final File MIDPOINT_HOME = new File("target/midpoint-home");
    private static final File MIDPOINT_HOME_ICF = new File(MIDPOINT_HOME, "connid-connectors");

    @Test
    public void test000PrismContextSanity() {
        SchemaRegistry schemaRegistry = PrismTestUtil.getPrismContext().getSchemaRegistry();
        PrismSchema schemaIcfc = schemaRegistry.findSchemaByNamespace(SchemaConstants.NS_ICF_CONFIGURATION);
        assertNotNull("ICFC schema not found in the context (" + SchemaConstants.NS_ICF_CONFIGURATION + ")", schemaIcfc);
        PrismContainerDefinition<ConnectorConfigurationType> configurationPropertiesDef =
                schemaIcfc.findContainerDefinitionByElementName(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
        assertNotNull("icfc:configurationProperties not found in icfc schema (" +
                SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME + ")", configurationPropertiesDef);
        PrismSchema schemaIcfs = schemaRegistry.findSchemaByNamespace(SchemaConstants.NS_ICF_SCHEMA);
        assertNotNull("ICFS schema not found in the context (" + SchemaConstants.NS_ICF_SCHEMA + ")", schemaIcfs);
    }

    @Test
    public void test020CreateConfiguredConnector() throws Exception {
        cc = connectorFactory.createConnectorInstance(connectorType,
                "dummy",
                "description of dummy test connector instance");
        assertNotNull("Failed to instantiate connector", cc);
        OperationResult result = createOperationResult();
        PrismContainerValue<ConnectorConfigurationType> configContainer =
                resourceType.getConnectorConfiguration().asPrismContainerValue();
        displayDumpable("Configuration container", configContainer);

        // WHEN
        cc.configure(configContainer, ResourceTypeUtil.getSchemaGenerationConstraints(resourceType), result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        resourceSchema = cc.fetchResourceSchema(result);
        assertNotNull("No resource schema", resourceSchema);
    }

    @Test
    public void test100AddAccount() throws Exception {
        OperationResult result = createOperationResult();

        ResourceObjectClassDefinition defaultAccountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
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
        cc.addObject(shadow, null, result);

        // THEN
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_JACK_USERNAME);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " was not created", dummyAccount);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " has no username", dummyAccount.getName());

    }

    @Test
    public void test110SearchNonBlocking() throws Exception {
        // GIVEN

        UcfExecutionContext ctx = createExecutionContext();

        final ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        // Determine object class from the schema

        final List<PrismObject<ShadowType>> searchResults = new ArrayList<>();

        UcfObjectHandler handler = (ucfObject, result) -> {
            displayDumpable("Search: found", ucfObject);
            checkUcfShadow(ucfObject.getResourceObject(), accountDefinition);
            searchResults.add(ucfObject.getResourceObject());
            return true;
        };

        OperationResult result = createOperationResult();

        // WHEN
        cc.search(accountDefinition, null, handler, null, null,
                null, null, ctx, result);

        // THEN
        assertEquals("Unexpected number of search results", 1, searchResults.size());

        ConnectorOperationalStatus opStat = cc.getOperationalStatus();
        displayDumpable("stats", opStat);
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 1, opStat.getPoolStatusNumIdle());
    }

    @Test
    public void test200BlockingSearch() throws Exception {
        // GIVEN
        UcfExecutionContext ctx = createExecutionContext();
        OperationResult result = createOperationResult();

        final ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        // Determine object class from the schema

        final List<PrismObject<ShadowType>> searchResults = new ArrayList<>();

        final UcfObjectHandler handler = (ucfObject, lResult) -> {
            checkUcfShadow(ucfObject.getResourceObject(), accountDefinition);
            searchResults.add(ucfObject.getResourceObject());
            return true;
        };

        dummyResource.setBlockOperations(true);

        // WHEN
        Thread t = new Thread(() -> {
            try {
                cc.search(accountDefinition, null, handler, null, null,
                        null, null, ctx, result);
            } catch (CommunicationException | GenericFrameworkException | SchemaException
                    | SecurityViolationException | ObjectNotFoundException e) {
                logger.error("Error in the search: {}", e.getMessage(), e);
            }
        });
        t.setName("search1");
        t.start();

        // Give the new thread a chance to get blocked
        Thread.sleep(500);

        ConnectorOperationalStatus opStat = cc.getOperationalStatus();
        displayDumpable("stats (blocked)", opStat);
        assertEquals("Wrong pool active", (Integer) 1, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumIdle());

        assertEquals("Unexpected number of search results", 0, searchResults.size());

        dummyResource.unblock();

        t.join();

        dummyResource.setBlockOperations(false);

        // THEN
        assertEquals("Unexpected number of search results", 1, searchResults.size());

        opStat = cc.getOperationalStatus();
        displayDumpable("stats (final)", opStat);
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 1, opStat.getPoolStatusNumIdle());

        PrismObject<ShadowType> searchResult = searchResults.get(0);
        displayDumpable("Search result", searchResult);
    }

    @Test
    public void test210TwoBlockingSearches() throws Exception {
        // GIVEN
        UcfExecutionContext ctx = createExecutionContext();

        final ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        // Determine object class from the schema

        OperationResult result1 = createOperationResult();
        final List<PrismObject<ShadowType>> searchResults1 = new ArrayList<>();
        final UcfObjectHandler handler1 = (ucfObject, result) -> {
            checkUcfShadow(ucfObject.getResourceObject(), accountDefinition);
            searchResults1.add(ucfObject.getResourceObject());
            return true;
        };

        OperationResult result2 = createOperationResult();
        final List<PrismObject<ShadowType>> searchResults2 = new ArrayList<>();
        final UcfObjectHandler handler2 = (ucfObject, result) -> {
            checkUcfShadow(ucfObject.getResourceObject(), accountDefinition);
            searchResults2.add(ucfObject.getResourceObject());
            return true;
        };

        dummyResource.setBlockOperations(true);

        // WHEN
        Thread t1 = new Thread(() -> {
            try {
                cc.search(accountDefinition, null, handler1, null, null, null, null, ctx, result1);
            } catch (CommunicationException | GenericFrameworkException | SchemaException
                    | SecurityViolationException | ObjectNotFoundException e) {
                logger.error("Error in the search: {}", e.getMessage(), e);
            }
        });
        t1.setName("search1");
        t1.start();

        // Give the new thread a chance to get blocked
        Thread.sleep(500);

        ConnectorOperationalStatus opStat = cc.getOperationalStatus();
        displayDumpable("stats (blocked 1)", opStat);
        assertEquals("Wrong pool active", (Integer) 1, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumIdle());

        assertEquals("Unexpected number of search results", 0, searchResults1.size());

        Thread t2 = new Thread(() -> {
            try {
                cc.search(accountDefinition, null, handler2, null, null,
                        null, null, ctx, result2);
            } catch (CommunicationException | GenericFrameworkException | SchemaException
                    | SecurityViolationException | ObjectNotFoundException e) {
                logger.error("Error in the search: {}", e.getMessage(), e);
            }
        });
        t2.setName("search2");
        t2.start();

        // Give the new thread a chance to get blocked
        Thread.sleep(500);

        opStat = cc.getOperationalStatus();
        displayDumpable("stats (blocked 2)", opStat);
        assertEquals("Wrong pool active", (Integer) 2, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumIdle());

        assertEquals("Unexpected number of search results", 0, searchResults1.size());

        dummyResource.unblockAll();

        t1.join();
        t2.join();

        dummyResource.setBlockOperations(false);

        // THEN
        assertEquals("Unexpected number of search results 1", 1, searchResults1.size());
        assertEquals("Unexpected number of search results 2", 1, searchResults2.size());

        opStat = cc.getOperationalStatus();
        displayDumpable("stats (final)", opStat);
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 2, opStat.getPoolStatusNumIdle());

        PrismObject<ShadowType> searchResult1 = searchResults1.get(0);
        displayDumpable("Search result 1", searchResult1);

        PrismObject<ShadowType> searchResult2 = searchResults2.get(0);
        displayDumpable("Search result 2", searchResult2);
    }

    private void checkUcfShadow(PrismObject<ShadowType> shadow, ResourceObjectClassDefinition objectClassDefinition) {
        assertNotNull("No objectClass in shadow " + shadow, shadow.asObjectable().getObjectClass());
        assertEquals("Wrong objectClass in shadow " + shadow, objectClassDefinition.getTypeName(), shadow.asObjectable().getObjectClass());
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertNotNull("No attributes in shadow " + shadow, attributes);
        assertFalse("Empty attributes in shadow " + shadow, attributes.isEmpty());
    }

    @Test
    public void test600loadFakeConnector() throws CommunicationException, IOException, InterruptedException {
        OperationResult result = new OperationResult("test");
        Set<ConnectorType> connectorsBefore = this.connectorFactoryIcfImpl.listConnectors(null, result.createSubresult("before"));
        assertNotNull(connectorsBefore);

        CountDownLatch detected = new CountDownLatch(1);
        connectorFactoryIcfImpl.registerDiscoveryListener(host -> {
            // Connector was detected
            detected.countDown();
        });

        File targetFile = new File(MIDPOINT_HOME_ICF, CONNECTOR_FAKE.getName());
        try {
            // Then move to
            Files.copy(CONNECTOR_FAKE, targetFile);
            detected.await(2, TimeUnit.MINUTES);
            Set<ConnectorType> connectorsAfter = this.connectorFactoryIcfImpl.listConnectors(null, result.createSubresult("after"));

            assertTrue(connectorsAfter.size() > connectorsBefore.size());

        } finally {
            targetFile.delete();
        }
    }

}
