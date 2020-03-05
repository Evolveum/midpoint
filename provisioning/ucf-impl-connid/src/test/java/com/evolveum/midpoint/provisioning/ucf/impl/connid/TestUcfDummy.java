/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static org.testng.AssertJUnit.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.icf.dummy.connector.DummyConnector;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ShadowResultHandler;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Simple UCF tests. No real resource, just basic setup and sanity.
 * <p>
 * This is an UCF test. It shold not need repository or other things from the midPoint spring context
 * except from the provisioning beans. But due to a general issue with spring context initialization
 * this is a lesser evil for now (MID-392)
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public class TestUcfDummy extends AbstractUcfDummyTest {

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
    public void test001ResourceSanity() {
        display("Resource", resource);

        assertEquals("Wrong oid", "ef2bc95b-76e0-59e2-86d6-9999dddddddd", resource.getOid());
//        assertEquals("Wrong version", "42", resource.getVersion());
        PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
        assertNotNull("No resource definition", resourceDefinition);
        PrismAsserts.assertObjectDefinition(resourceDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "resource"),
                ResourceType.COMPLEX_TYPE, ResourceType.class);
        assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
        ResourceType resourceType = resource.asObjectable();
        assertNotNull("asObjectable resulted in null", resourceType);

        assertPropertyValue(resource, "name", PrismTestUtil.createPolyString("Dummy Resource"));
        assertPropertyDefinition(resource, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertContainerDefinition(configurationContainer, "configuration", ConnectorConfigurationType.COMPLEX_TYPE, 1, 1);
        PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
        Collection<Item<?, ?>> configItems = configContainerValue.getItems();
        assertEquals("Wrong number of config items", 2, configItems.size());

        PrismContainer<?> dummyConfigPropertiesContainer = configurationContainer.findContainer(
                SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
        assertNotNull("No icfc:configurationProperties container", dummyConfigPropertiesContainer);
        Collection<Item<?, ?>> dummyConfigPropItems = dummyConfigPropertiesContainer.getValue().getItems();
        assertEquals("Wrong number of dummy ConfigPropItems items", 4, dummyConfigPropItems.size());
    }

    @Test
    public void test002ConnectorSchema() throws Exception {
        PrismSchema connectorSchema = connectorFactory.generateConnectorConfigurationSchema(connectorType);
        IntegrationTestTools.assertConnectorSchemaSanity(connectorSchema, "generated", true);
        assertEquals("Unexpected number of definitions", 3, connectorSchema.getDefinitions().size());

        Document xsdSchemaDom = connectorSchema.serializeToXsd();
        assertNotNull("No serialized connector schema", xsdSchemaDom);
        display("Serialized XSD connector schema", DOMUtil.serializeDOMToString(xsdSchemaDom));

        // Try to re-parse
        PrismSchema reparsedConnectorSchema = PrismSchemaImpl.parse(DOMUtil.getFirstChildElement(xsdSchemaDom), true, "schema fetched from " + cc, PrismTestUtil.getPrismContext());
        IntegrationTestTools.assertConnectorSchemaSanity(reparsedConnectorSchema, "re-parsed", true);
        // TODO: 3 definitions would be cleaner. But we can live with this
        assertEquals("Unexpected number of definitions in re-parsed schema", 6, reparsedConnectorSchema.getDefinitions().size());
    }

    /**
     * Test listing connectors. Very simple. Just test that the list is
     * non-empty and that there are mandatory values filled in.
     */
    @Test
    public void test010ListConnectors() throws Exception {
        OperationResult result = createResult();
        Set<ConnectorType> connectors = connectorFactory.listConnectors(null, result);

        System.out.println("---------------------------------------------------------------------");
        assertNotNull(connectors);
        assertFalse(connectors.isEmpty());

        for (ConnectorType connector : connectors) {
            assertNotNull(connector.getName());
            System.out.println("CONNECTOR OID=" + connector.getOid() + ", name=" + connector.getName() + ", version="
                    + connector.getConnectorVersion());
            System.out.println("--");
            System.out.println(ObjectTypeUtil.dump(connector));
            System.out.println("--");
        }

        System.out.println("---------------------------------------------------------------------");

        assertEquals("Unexpected number of connectors discovered", 8, connectors.size());
    }

    @Test
    public void test020CreateConfiguredConnector() throws Exception {
        cc = connectorFactory.createConnectorInstance(connectorType, ResourceTypeUtil.getResourceNamespace(resourceType),
                "dummy",
                "description of dummy test connector instance");
        assertNotNull("Failed to instantiate connector", cc);
        OperationResult result = createResult();
        PrismContainerValue<ConnectorConfigurationType> configContainer = resourceType.getConnectorConfiguration().asPrismContainerValue();
        display("Configuration container", configContainer);

        // WHEN
        cc.configure(configContainer, ResourceTypeUtil.getSchemaGenerationConstraints(resourceType), result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test022ConnectorStatsConfigured() throws Exception {
        // WHEN
        ConnectorOperationalStatus operationalStatus = cc.getOperationalStatus();

        // THEN
        display("Connector operational status", operationalStatus);
        assertNotNull("null operational status", operationalStatus);

        assertEquals("Wrong connectorClassName", DummyConnector.class.getName(), operationalStatus.getConnectorClassName());
        assertEquals("Wrong poolConfigMinSize", null, operationalStatus.getPoolConfigMinSize());
        assertEquals("Wrong poolConfigMaxSize", (Integer) 10, operationalStatus.getPoolConfigMaxSize());
        assertEquals("Wrong poolConfigMinIdle", (Integer) 1, operationalStatus.getPoolConfigMinIdle());
        assertEquals("Wrong poolConfigMaxIdle", (Integer) 10, operationalStatus.getPoolConfigMaxIdle());
        assertEquals("Wrong poolConfigWaitTimeout", (Long) 150000L, operationalStatus.getPoolConfigWaitTimeout());
        assertEquals("Wrong poolConfigMinEvictableIdleTime", (Long) 120000L, operationalStatus.getPoolConfigMinEvictableIdleTime());
        assertEquals("Wrong poolStatusNumIdle", (Integer) 0, operationalStatus.getPoolStatusNumIdle());
        assertEquals("Wrong poolStatusNumActive", (Integer) 0, operationalStatus.getPoolStatusNumActive());
    }

    @Test
    public void test030ResourceSchema() throws Exception {
        OperationResult result = createResult();

        cc = connectorFactory.createConnectorInstance(connectorType, ResourceTypeUtil.getResourceNamespace(resourceType),
                "dummy",
                "description of dummy test connector instance");
        assertNotNull("Failed to instantiate connector", cc);

        PrismContainerValue<ConnectorConfigurationType> configContainer = resourceType.getConnectorConfiguration().asPrismContainerValue();
        display("Configuration container", configContainer);
        //ResourceTypeUtil.getSchemaGenerationConstraints(resourceType)
        cc.configure(configContainer, null, result);

        // WHEN
        resourceSchema = cc.fetchResourceSchema(result);

        // THEN
        display("Generated resource schema", resourceSchema);
        assertEquals("Unexpected number of definitions", 4, resourceSchema.getDefinitions().size());

        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(resourceSchema, resourceType, true);

        Document xsdSchemaDom = resourceSchema.serializeToXsd();
        assertNotNull("No serialized resource schema", xsdSchemaDom);
        display("Serialized XSD resource schema", DOMUtil.serializeDOMToString(xsdSchemaDom));

        // Try to re-parse
        ResourceSchema reparsedResourceSchema = ResourceSchemaImpl.parse(DOMUtil.getFirstChildElement(xsdSchemaDom),
                "serialized schema", PrismTestUtil.getPrismContext());
        display("Re-parsed resource schema", reparsedResourceSchema);
        assertEquals("Unexpected number of definitions in re-parsed schema", 4, reparsedResourceSchema.getDefinitions().size());

        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(reparsedResourceSchema, resourceType, true);
    }

    @Test
    public void test031ResourceSchemaAccountObjectClass() throws Exception {
        OperationResult result = createResult();

        cc = connectorFactory.createConnectorInstance(connectorType, ResourceTypeUtil.getResourceNamespace(resourceType),
                "dummy",
                "description of dummy test connector instance");
        assertNotNull("Failed to instantiate connector", cc);

        PrismContainerValue<ConnectorConfigurationType> configContainer = resourceType.getConnectorConfiguration().asPrismContainerValue();
        display("Configuration container", configContainer);
        List<QName> objectClassesToGenerate = new ArrayList<>();
        QName accountObjectClass = new QName(resource.asObjectable().getNamespace(), "AccountObjectClass");
        objectClassesToGenerate.add(accountObjectClass);

        cc.configure(configContainer, objectClassesToGenerate, result);

        // WHEN
        resourceSchema = cc.fetchResourceSchema(result);

        // THEN
        display("Generated resource schema", resourceSchema);
        assertEquals("Unexpected number of definitions", 1, resourceSchema.getDefinitions().size());

        assertEquals("Unexpected number of object class definitions", 1, resourceSchema.getObjectClassDefinitions().size());

        display("RESOURCE SCHEMA DEFINITION" + resourceSchema.getDefinitions().iterator().next().getTypeName());
    }

    @Test
    public void test033ConnectorStatsInitialized() throws Exception {
        // WHEN
        ConnectorOperationalStatus operationalStatus = cc.getOperationalStatus();

        // THEN
        display("Connector operational status", operationalStatus);
        assertNotNull("null operational status", operationalStatus);

        assertEquals("Wrong connectorClassName", DummyConnector.class.getName(), operationalStatus.getConnectorClassName());
        assertEquals("Wrong poolConfigMinSize", null, operationalStatus.getPoolConfigMinSize());
        assertEquals("Wrong poolConfigMaxSize", (Integer) 10, operationalStatus.getPoolConfigMaxSize());
        assertEquals("Wrong poolConfigMinIdle", (Integer) 1, operationalStatus.getPoolConfigMinIdle());
        assertEquals("Wrong poolConfigMaxIdle", (Integer) 10, operationalStatus.getPoolConfigMaxIdle());
        assertEquals("Wrong poolConfigWaitTimeout", (Long) 150000L, operationalStatus.getPoolConfigWaitTimeout());
        assertEquals("Wrong poolConfigMinEvictableIdleTime", (Long) 120000L, operationalStatus.getPoolConfigMinEvictableIdleTime());
        assertEquals("Wrong poolStatusNumIdle", (Integer) 1, operationalStatus.getPoolStatusNumIdle());
        assertEquals("Wrong poolStatusNumActive", (Integer) 0, operationalStatus.getPoolStatusNumActive());
    }

    @Test
    public void test040AddAccount() throws Exception {
        final String TEST_NAME = "test040AddAccount";

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
        cc.addObject(shadow, null, result);

        // THEN
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_JACK_USERNAME);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " was not created", dummyAccount);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " has no username", dummyAccount.getName());

    }

    @Test
    public void test050Search() throws Exception {
        // GIVEN

        final ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
        // Determine object class from the schema

        final List<PrismObject<ShadowType>> searchResults = new ArrayList<>();

        ShadowResultHandler handler = new ShadowResultHandler() {

            @Override
            public boolean handle(PrismObject<ShadowType> shadow) {
                display("Search: found", shadow);
                checkUcfShadow(shadow, accountDefinition);
                searchResults.add(shadow);
                return true;
            }
        };

        OperationResult result = createResult();

        // WHEN
        cc.search(accountDefinition, null, handler, null, null, null, null, result);

        // THEN
        assertEquals("Unexpected number of search results", 1, searchResults.size());
    }

    private void checkUcfShadow(PrismObject<ShadowType> shadow, ObjectClassComplexTypeDefinition objectClassDefinition) {
        assertNotNull("No objectClass in shadow " + shadow, shadow.asObjectable().getObjectClass());
        assertEquals("Wrong objectClass in shadow " + shadow, objectClassDefinition.getTypeName(), shadow.asObjectable().getObjectClass());
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertNotNull("No attributes in shadow " + shadow, attributes);
        assertFalse("Empty attributes in shadow " + shadow, attributes.isEmpty());
    }

    @Test
    public void test100FetchEmptyChanges() throws Exception {
        final String TEST_NAME = "test100FetchEmptyChanges";

        OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);
        ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);

        // WHEN
        PrismProperty<Integer> lastToken = cc.fetchCurrentToken(accountDefinition, null, result);

        assertNotNull("No last sync token", lastToken);

        System.out.println("Property:");
        System.out.println(lastToken.debugDump());

        PrismPropertyDefinition<Integer> lastTokenDef = lastToken.getDefinition();
        assertNotNull("No last sync token definition", lastTokenDef);
        assertEquals("Last sync token definition has wrong type", DOMUtil.XSD_INT, lastTokenDef.getTypeName());
        assertTrue("Last sync token definition is NOT dynamic", lastTokenDef.isDynamic());

        // WHEN
        CollectingChangeHandler handler = new CollectingChangeHandler();
        cc.fetchChanges(accountDefinition, lastToken, null, null, null, handler, result);

        AssertJUnit.assertEquals(0, handler.getChanges().size());
    }

    @Test
    public void test101FetchAddChange() throws Exception {
        final String TEST_NAME = "test101FetchAddChange";

        OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);
        ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);

        PrismProperty<?> lastToken = cc.fetchCurrentToken(accountDefinition, null, result);
        assertNotNull("No last sync token", lastToken);

        // Add account to the resource
        dummyResource.setSyncStyle(DummySyncStyle.DUMB);
        DummyAccount newAccount = new DummyAccount("blackbeard");
        newAccount.addAttributeValues("fullname", "Edward Teach");
        newAccount.setEnabled(true);
        newAccount.setPassword("shiverMEtimbers");
        dummyResource.addAccount(newAccount);

        // WHEN
        CollectingChangeHandler handler = new CollectingChangeHandler();
        cc.fetchChanges(accountDefinition, lastToken, null, null, null, handler, result);
        List<Change> changes = handler.getChanges();

        AssertJUnit.assertEquals(1, changes.size());
        Change change = changes.get(0);
        assertNotNull("null change", change);
        PrismObject<ShadowType> currentResourceObject = change.getCurrentResourceObject();
        assertNotNull("null current resource object", currentResourceObject);
        PrismAsserts.assertParentConsistency(currentResourceObject);
        Collection<ResourceAttribute<?>> identifiers = change.getIdentifiers();
        assertNotNull("null identifiers", identifiers);
        assertFalse("empty identifiers", identifiers.isEmpty());

    }

    @Test
    public void test500SelfTest() {
        final String TEST_NAME = "test500SelfTest";

        // GIVEN
        OperationResult testResult = new OperationResult(TestUcfDummy.class + "." + TEST_NAME);

        // WHEN
        connectorFactoryIcfImpl.selfTest(testResult);

        // THEN
        testResult.computeStatus();
        IntegrationTestTools.display(testResult);
        TestUtil.assertSuccess(testResult);
    }

}
