/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static org.testng.AssertJUnit.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
                ResourceTypeUtil.getResourceNamespace(resourceType),
                "dummy",
                "description of dummy test connector instance");
        assertNotNull("Failed to instantiate connector", cc);
        OperationResult result = createOperationResult();
        PrismContainerValue<ConnectorConfigurationType> configContainer =
                resourceType.getConnectorConfiguration().asPrismContainerValue();
        displayValue("Configuration container", configContainer);

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
        cc.addObject(shadow, null, result);

        // THEN
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_JACK_USERNAME);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " was not created", dummyAccount);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " has no username", dummyAccount.getName());

    }

    @Test
    public void test110SearchNonBlocking() throws Exception {
        // GIVEN

        final ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
        // Determine object class from the schema

        final List<PrismObject<ShadowType>> searchResults = new ArrayList<>();

        ShadowResultHandler handler = new ShadowResultHandler() {

            @Override
            public boolean handle(PrismObject<ShadowType> shadow) {
                System.out.println("Search: found: " + shadow);
                checkUcfShadow(shadow, accountDefinition);
                searchResults.add(shadow);
                return true;
            }
        };

        OperationResult result = createOperationResult();

        // WHEN
        cc.search(accountDefinition, null, handler, null, null, null, null, result);

        // THEN
        assertEquals("Unexpected number of search results", 1, searchResults.size());

        ConnectorOperationalStatus opStat = cc.getOperationalStatus();
        displayValue("stats", opStat);
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 1, opStat.getPoolStatusNumIdle());
    }

    @Test
    public void test200BlockingSearch() throws Exception {
        // GIVEN

        OperationResult result = createOperationResult();

        final ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
        // Determine object class from the schema

        final List<PrismObject<ShadowType>> searchResults = new ArrayList<>();

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
                    cc.search(accountDefinition, null, handler, null, null, null, null, result);
                } catch (CommunicationException | GenericFrameworkException | SchemaException
                        | SecurityViolationException | ObjectNotFoundException e) {
                    logger.error("Error in the search: {}", e.getMessage(), e);
                }
            }
        });
        t.setName("search1");
        t.start();

        // Give the new thread a chance to get blocked
        Thread.sleep(500);

        ConnectorOperationalStatus opStat = cc.getOperationalStatus();
        displayValue("stats (blocked)", opStat);
        assertEquals("Wrong pool active", (Integer) 1, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumIdle());

        assertEquals("Unexpected number of search results", 0, searchResults.size());

        dummyResource.unblock();

        t.join();

        dummyResource.setBlockOperations(false);

        // THEN
        assertEquals("Unexpected number of search results", 1, searchResults.size());

        opStat = cc.getOperationalStatus();
        displayValue("stats (final)", opStat);
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 1, opStat.getPoolStatusNumIdle());

        PrismObject<ShadowType> searchResult = searchResults.get(0);
        displayValue("Search result", searchResult);
    }

    @Test
    public void test210TwoBlockingSearches() throws Exception {
        // GIVEN

        final ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
        // Determine object class from the schema

        OperationResult result1 = createOperationResult();
        final List<PrismObject<ShadowType>> searchResults1 = new ArrayList<>();
        final ShadowResultHandler handler1 = new ShadowResultHandler() {
            @Override
            public boolean handle(PrismObject<ShadowType> shadow) {
                checkUcfShadow(shadow, accountDefinition);
                searchResults1.add(shadow);
                return true;
            }
        };

        OperationResult result2 = createOperationResult();
        final List<PrismObject<ShadowType>> searchResults2 = new ArrayList<>();
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
                    cc.search(accountDefinition, null, handler1, null, null, null, null, result1);
                } catch (CommunicationException | GenericFrameworkException | SchemaException
                        | SecurityViolationException | ObjectNotFoundException e) {
                    logger.error("Error in the search: {}", e.getMessage(), e);
                }
            }
        });
        t1.setName("search1");
        t1.start();

        // Give the new thread a chance to get blocked
        Thread.sleep(500);

        ConnectorOperationalStatus opStat = cc.getOperationalStatus();
        displayValue("stats (blocked 1)", opStat);
        assertEquals("Wrong pool active", (Integer) 1, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumIdle());

        assertEquals("Unexpected number of search results", 0, searchResults1.size());

        Thread t2 = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    cc.search(accountDefinition, null, handler2, null, null, null, null, result2);
                } catch (CommunicationException | GenericFrameworkException | SchemaException
                        | SecurityViolationException | ObjectNotFoundException e) {
                    logger.error("Error in the search: {}", e.getMessage(), e);
                }
            }
        });
        t2.setName("search2");
        t2.start();

        // Give the new thread a chance to get blocked
        Thread.sleep(500);

        opStat = cc.getOperationalStatus();
        displayValue("stats (blocked 2)", opStat);
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
        displayValue("stats (final)", opStat);
        assertEquals("Wrong pool active", (Integer) 0, opStat.getPoolStatusNumActive());
        assertEquals("Wrong pool active", (Integer) 2, opStat.getPoolStatusNumIdle());

        PrismObject<ShadowType> searchResult1 = searchResults1.get(0);
        displayValue("Search result 1", searchResult1);

        PrismObject<ShadowType> searchResult2 = searchResults2.get(0);
        displayValue("Search result 2", searchResult2);
    }

    private void checkUcfShadow(PrismObject<ShadowType> shadow, ObjectClassComplexTypeDefinition objectClassDefinition) {
        assertNotNull("No objectClass in shadow " + shadow, shadow.asObjectable().getObjectClass());
        assertEquals("Wrong objectClass in shadow " + shadow, objectClassDefinition.getTypeName(), shadow.asObjectable().getObjectClass());
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertNotNull("No attributes in shadow " + shadow, attributes);
        assertFalse("Empty attributes in shadow " + shadow, attributes.isEmpty());
    }

}
