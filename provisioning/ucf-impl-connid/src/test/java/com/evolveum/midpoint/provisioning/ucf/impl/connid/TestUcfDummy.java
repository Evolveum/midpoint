/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
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
import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Simple UCF tests. No real resource, just basic setup and sanity.
 * <p>
 * This is an UCF test. It should not need repository or other things from the midPoint spring context
 * except from the provisioning beans. But due to a general issue with spring context initialization
 * this is a lesser evil for now (MID-392)
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public class TestUcfDummy extends AbstractUcfDummyTest {

    private static final File HIERARCHICAL_RESOURCE_DUMMY_FILE =
            new File(UcfTestUtil.TEST_DIR, "hierarchical-resource-dummy.xml");

    private PrismObject<ResourceType> hierarchicalResourceObject;
    private ResourceType hierarchicalResourceBean;
    private DummyResource hierarchicalResource;
    private DummyResourceContoller hierarchicalResourceCtl;
    private ConnectorInstance hierarchicalConnectorInstance;

    /** For test2xx. */
    private void initializeHierarchicalResourceIfNeeded() throws Exception {
        if (hierarchicalResource != null) {
            return;
        }

        OperationResult result = createOperationResult();

        hierarchicalResourceObject = PrismTestUtil.parseObject(HIERARCHICAL_RESOURCE_DUMMY_FILE);
        hierarchicalResourceBean = hierarchicalResourceObject.asObjectable();

        hierarchicalResourceCtl = DummyResourceContoller.create("hierarchical");
        hierarchicalResourceCtl.setResource(hierarchicalResourceObject);
        hierarchicalResourceCtl.extendSchemaPirate();
        hierarchicalResource = hierarchicalResourceCtl.getDummyResource();

        hierarchicalConnectorInstance =
                connectorFactory.createConnectorInstance(connectorType, "hierarchical", "");
        assertNotNull("Failed to instantiate connector", hierarchicalConnectorInstance);

        //noinspection unchecked
        PrismContainerValue<ConnectorConfigurationType> configContainer =
                hierarchicalResourceBean.getConnectorConfiguration().asPrismContainerValue();
        displayDumpable("Configuration container", configContainer);
        hierarchicalConnectorInstance.configure(configContainer, ConnectorConfigurationOptions.DEFAULT, result);
        hierarchicalConnectorInstance.initialize(null, null, false, result);
    }

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
        displayDumpable("Resource", resource);

        assertEquals("Wrong oid", "ef2bc95b-76e0-59e2-86d6-9999dddddddd", resource.getOid());
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
        assertContainerDefinition(configurationContainer, "configuration", ConnectorConfigurationType.COMPLEX_TYPE, 0, 1);
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
        displayValue("Serialized XSD connector schema", DOMUtil.serializeDOMToString(xsdSchemaDom));

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
        OperationResult result = createOperationResult();
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
        cc = connectorFactory.createConnectorInstance(connectorType,
                "dummy",
                "description of dummy test connector instance");
        assertNotNull("Failed to instantiate connector", cc);
        OperationResult result = createOperationResult();
        //noinspection unchecked
        PrismContainerValue<ConnectorConfigurationType> configContainer =
                resourceType.getConnectorConfiguration().asPrismContainerValue();
        displayDumpable("Configuration container", configContainer);

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
        displayDumpable("Connector operational status", operationalStatus);
        assertNotNull("null operational status", operationalStatus);

        assertEquals("Wrong connectorClassName", DummyConnector.class.getName(), operationalStatus.getConnectorClassName());
        assertNull("Wrong poolConfigMinSize", operationalStatus.getPoolConfigMinSize());
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
        OperationResult result = createOperationResult();

        cc = connectorFactory.createConnectorInstance(connectorType,
                "dummy",
                "description of dummy test connector instance");
        assertNotNull("Failed to instantiate connector", cc);

        //noinspection unchecked
        PrismContainerValue<ConnectorConfigurationType> configContainer =
                resourceType.getConnectorConfiguration().asPrismContainerValue();
        displayDumpable("Configuration container", configContainer);
        cc.configure(configContainer, ConnectorConfigurationOptions.DEFAULT, result);

        // WHEN
        resourceSchema = cc.fetchResourceSchema(result);

        // THEN
        displayDumpable("Generated resource schema", resourceSchema);
        assertEquals("Unexpected number of definitions", 4, resourceSchema.getDefinitions().size());

        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(resourceSchema, resourceType, true);

        Document xsdSchemaDom = resourceSchema.serializeToXsd();
        assertNotNull("No serialized resource schema", xsdSchemaDom);
        displayValue("Serialized XSD resource schema", DOMUtil.serializeDOMToString(xsdSchemaDom));

        // Try to re-parse
        ResourceSchema reparsedResourceSchema = ResourceSchemaParser.parse(DOMUtil.getFirstChildElement(xsdSchemaDom), "serialized schema");
        displayDumpable("Re-parsed resource schema", reparsedResourceSchema);
        assertEquals("Unexpected number of definitions in re-parsed schema", 4, reparsedResourceSchema.getDefinitions().size());

        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(reparsedResourceSchema, resourceType, true);
    }

    @Test
    public void test031ResourceSchemaAccountObjectClass() throws Exception {
        OperationResult result = createOperationResult();

        cc = connectorFactory.createConnectorInstance(connectorType,
                "dummy",
                "description of dummy test connector instance");
        assertNotNull("Failed to instantiate connector", cc);

        //noinspection unchecked
        PrismContainerValue<ConnectorConfigurationType> configContainer =
                resourceType.getConnectorConfiguration().asPrismContainerValue();
        displayDumpable("Configuration container", configContainer);
        List<QName> objectClassesToGenerate = new ArrayList<>();
        objectClassesToGenerate.add(RI_ACCOUNT_OBJECT_CLASS);

        cc.configure(configContainer, objectClassesToGenerate, result);

        // WHEN
        resourceSchema = cc.fetchResourceSchema(result);

        // THEN
        displayDumpable("Generated resource schema", resourceSchema);
        assertEquals("Unexpected number of definitions", 1, resourceSchema.getDefinitions().size());

        assertEquals("Unexpected number of object class definitions", 1, resourceSchema.getObjectClassDefinitions().size());

        display("RESOURCE SCHEMA DEFINITION" + resourceSchema.getDefinitions().iterator().next().getTypeName());
    }

    @Test
    public void test033ConnectorStatsInitialized() throws Exception {
        // WHEN
        ConnectorOperationalStatus operationalStatus = cc.getOperationalStatus();

        // THEN
        displayDumpable("Connector operational status", operationalStatus);
        assertNotNull("null operational status", operationalStatus);

        assertEquals("Wrong connectorClassName", DummyConnector.class.getName(), operationalStatus.getConnectorClassName());
        assertNull("Wrong poolConfigMinSize", operationalStatus.getPoolConfigMinSize());
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
        OperationResult result = createOperationResult();

        ResourceObjectClassDefinition defaultAccountDefinition =
                resourceSchema.findObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS);
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
        cc.search(accountDefinition, null, handler, null, null, null, null, ctx, result);

        // THEN
        assertEquals("Unexpected number of search results", 1, searchResults.size());
    }

    private void checkUcfShadow(PrismObject<ShadowType> shadow, ResourceObjectClassDefinition objectClassDefinition) {
        assertNotNull("No objectClass in shadow " + shadow, shadow.asObjectable().getObjectClass());
        assertEquals("Wrong objectClass in shadow " + shadow, objectClassDefinition.getTypeName(), shadow.asObjectable().getObjectClass());
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertNotNull("No attributes in shadow " + shadow, attributes);
        assertFalse("Empty attributes in shadow " + shadow, attributes.isEmpty());
    }

    /** MID-8145 */
    @Test
    public void test060GetByUidWithNameHint() throws Exception {
        given();
        UcfExecutionContext ctx = createExecutionContext();
        OperationResult result = createOperationResult();

        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        //noinspection unchecked
        ResourceAttribute<String> uid =
                (ResourceAttribute<String>) accountDefinition.getPrimaryIdentifiers().iterator().next().instantiate();
        uid.setRealValue("jack");
        //noinspection unchecked
        ResourceAttribute<String> name =
                (ResourceAttribute<String>) accountDefinition.getSecondaryIdentifiers().iterator().next().instantiate();
        name.setRealValue("jack");
        ResourceObjectIdentification identification =
                new ResourceObjectIdentification(accountDefinition, List.of(uid), List.of(name));

        when("getting account by UID");
        PrismObject<ShadowType> shadow = cc.fetchObject(identification, null, ctx, result);

        then("account is retrieved OK");
        displayDumpable("shadow retrieved", shadow);
        assertThat(shadow).as("shadow").isNotNull();
        checkUcfShadow(shadow, accountDefinition);
    }

    @Test
    public void test100FetchEmptyChanges() throws Exception {
        OperationResult result = createOperationResult();
        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);

        // WHEN
        UcfSyncToken lastToken = cc.fetchCurrentToken(accountDefinition, null, result);

        assertNotNull("No last sync token", lastToken);

        System.out.println("Token:");
        System.out.println(lastToken);

        // WHEN
        CollectingChangeListener handler = new CollectingChangeListener();
        cc.fetchChanges(accountDefinition, lastToken, null, null, null, handler, result);

        AssertJUnit.assertEquals(0, handler.getChanges().size());
    }

    @Test
    public void test101FetchAddChange() throws Exception {
        OperationResult result = createOperationResult();
        ResourceObjectClassDefinition accountDefinition =
                resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);

        UcfSyncToken lastToken = cc.fetchCurrentToken(accountDefinition, null, result);
        assertNotNull("No last sync token", lastToken);

        // Add account to the resource
        dummyResource.setSyncStyle(DummySyncStyle.DUMB);
        DummyAccount newAccount = new DummyAccount("blackbeard");
        newAccount.addAttributeValues("fullname", "Edward Teach");
        newAccount.setEnabled(true);
        newAccount.setPassword("shiverMEtimbers");
        dummyResource.addAccount(newAccount);

        // WHEN
        CollectingChangeListener handler = new CollectingChangeListener();
        cc.fetchChanges(accountDefinition, lastToken, null, null, null, handler, result);
        List<UcfLiveSyncChange> changes = handler.getChanges();

        AssertJUnit.assertEquals(1, changes.size());
        UcfLiveSyncChange change = changes.get(0);
        assertNotNull("null change", change);
        PrismObject<ShadowType> resourceObject = change.getResourceObject();
        assertNotNull("null current resource object", resourceObject);
        PrismAsserts.assertParentConsistency(resourceObject);
        Collection<ResourceAttribute<?>> identifiers = change.getIdentifiers();
        assertNotNull("null identifiers", identifiers);
        assertFalse("empty identifiers", identifiers.isEmpty());
    }

    @Test
    public void test200AddObjects() throws Exception {
        initializeHierarchicalResourceIfNeeded();

        given("an org exists");
        hierarchicalResourceCtl.addOrg("org200");

        when("top-level account is added");
        hierarchicalResourceCtl.addAccount("test");

        then("it is there");
        assertThat(hierarchicalResource.getAccountByUsername("test")).isNotNull();

        when("account in non-existent org is added");
        try {
            hierarchicalResourceCtl.addAccount("test:org200a");
            fail("unexpected success");
        } catch (ObjectDoesNotExistException e) {
            then("exception is thrown");
            assertExpectedException(e)
                    .hasMessageContaining(
                            "Cannot add object with name 'test:org200a' because its parent org 'org200a' does not exist");
        }

        when("account in existing org is added");
        hierarchicalResourceCtl.addAccount("test:org200");

        then("it is there");
        assertThat(hierarchicalResource.getAccountByUsername("test:org200")).isNotNull();
    }

    @Test
    public void test210DeleteNonEmptyOrg() throws Exception {
        initializeHierarchicalResourceIfNeeded();

        given("an org with account exists");
        hierarchicalResourceCtl.addOrg("org210");
        hierarchicalResourceCtl.addAccount("test:org210");

        when("org is being deleted");
        try {
            hierarchicalResource.deleteOrgByName("org210");
            fail("unexpected success");
        } catch (SchemaViolationException e) {
            assertExpectedException(e);
        }
    }

    @Test
    public void test220RenameOrg() throws Exception {
        initializeHierarchicalResourceIfNeeded();

        given("two nested orgs with an account exist");
        DummyOrg root = hierarchicalResourceCtl.addOrg("root220");
        DummyOrg org = hierarchicalResourceCtl.addOrg("org220:root220");
        hierarchicalResourceCtl.addAccount("test:org220:root220");

        displayDumpable("root", root);
        displayDumpable("org", org);

        when("root is renamed");
        hierarchicalResource.renameOrg(root.getId(), "root220", "root220a");

        then("orgs and account have new names");
        assertOrgExists("root220a");
        assertOrgExists("org220:root220a");
        assertAccountExists("test:org220:root220a");

        when("org is renamed");
        hierarchicalResource.renameOrg(org.getId(), "org220:root220a", "org220a:root220a");

        then("orgs and account have new names");
        assertOrgExists("root220a");
        assertOrgExists("org220a:root220a");
        assertAccountExists("test:org220a:root220a");
    }

    private void assertOrgExists(String name)
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        DummyOrg org = hierarchicalResource.getOrgByName(name);
        assertThat(org).as("org named '" + name + "'").isNotNull();
    }

    private void assertAccountExists(String name)
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        DummyAccount account = hierarchicalResource.getAccountByUsername(name);
        assertThat(account).as("account named '" + name + "'").isNotNull();
    }

    @Test
    public void test500SelfTest() {
        // GIVEN
        OperationResult testResult = createOperationResult();

        // WHEN
        connectorFactoryIcfImpl.selfTest(testResult);

        // THEN
        testResult.computeStatus();
        IntegrationTestTools.display(testResult);
        TestUtil.assertSuccess(testResult);
    }
}
