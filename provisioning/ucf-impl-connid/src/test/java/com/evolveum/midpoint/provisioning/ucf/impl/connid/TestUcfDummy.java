/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import static com.evolveum.midpoint.schema.processor.ResourceSchemaFactory.*;
import static com.evolveum.midpoint.schema.processor.ShadowReferenceParticipantRole.SUBJECT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.provisioning.ucf.api.UcfFetchErrorReportingMethod.EXCEPTION;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.*;

import com.evolveum.midpoint.test.AttrName;
import com.evolveum.midpoint.test.DummyHrScenario.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.icf.dummy.connector.DummyConnector;
import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
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
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.test.DummyHrScenario;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Simple UCF tests, using dummy resource. Some tests even avoid UCF/ConnId.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public class TestUcfDummy extends AbstractUcfDummyTest {

    private static final File HR_RESOURCE_DUMMY_FILE = new File(UcfTestUtil.TEST_DIR, "hr-resource-dummy.xml");

    /** Dummy resource with the support of hierarchical (LDAP-like) object names. Used for 2xx tests. */
    private DummyResource hierarchicalResource;
    private DummyResourceContoller hierarchicalResourceCtl;

    /** Scenario with reference attributes, HR-style. Used for 3xx tests. */
    private DummyHrScenario hrScenario;

    /** Connector instance to access {@link #hrScenario} via UCF. */
    private ConnectorInstance hrConnectorInstance;

    @Test
    public void test000PrismContextSanity() {
        SchemaRegistry schemaRegistry = PrismTestUtil.getPrismContext().getSchemaRegistry();

        PrismSchema icfcSchema = schemaRegistry.findSchemaByNamespace(SchemaConstants.NS_ICF_CONFIGURATION);
        assertNotNull("ICFC schema not found in the context", icfcSchema);
        PrismContainerDefinition<ConnectorConfigurationType> configurationPropertiesDef =
                icfcSchema.findContainerDefinitionByElementName(ICF_CONFIGURATION_PROPERTIES_NAME);
        assertNotNull("icfc:configurationProperties container definition not found", configurationPropertiesDef);

        PrismSchema icfsSchema = schemaRegistry.findSchemaByNamespace(SchemaConstants.NS_ICF_SCHEMA);
        assertNotNull("ICFS schema not found in the context (" + SchemaConstants.NS_ICF_SCHEMA + ")", icfsSchema);
    }

    @Test
    public void test001ResourceSanity() {
        displayDumpable("Resource", resource);

        assertEquals("Wrong oid", "ef2bc95b-76e0-59e2-86d6-9999dddddddd", resource.getOid());
        PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
        assertNotNull("No resource definition", resourceDefinition);
        PrismAsserts.assertObjectDefinition(
                resourceDefinition, SchemaConstantsGenerated.C_RESOURCE, ResourceType.COMPLEX_TYPE, ResourceType.class);
        assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
        ResourceType resourceType = resource.asObjectable();
        assertNotNull("asObjectable resulted in null", resourceType);

        assertPropertyValue(resource, "name", PolyString.fromOrig("Dummy Resource"));
        assertPropertyDefinition(resource, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertContainerDefinition(configurationContainer, "configuration", ConnectorConfigurationType.COMPLEX_TYPE, 0, 1);
        PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
        Collection<Item<?, ?>> configItems = configContainerValue.getItems();
        assertEquals("Wrong number of config items", 2, configItems.size());

        PrismContainer<?> dummyConfigPropertiesContainer =
                configurationContainer.findContainer(ICF_CONFIGURATION_PROPERTIES_NAME);
        assertNotNull("No icfc:configurationProperties container", dummyConfigPropertiesContainer);
        Collection<Item<?, ?>> dummyConfigPropItems = dummyConfigPropertiesContainer.getValue().getItems();
        assertEquals("Wrong number of dummy ConfigPropItems items", 4, dummyConfigPropItems.size());
    }

    @Test
    public void test002ConnectorSchema() throws Exception {
        var connectorSchema = connectorFactory.generateConnectorConfigurationSchema(connectorBean);
        assertThat(connectorSchema).isNotNull();

        IntegrationTestTools.assertConnectorSchemaSanity(connectorSchema, "generated", true);
        assertEquals("Unexpected number of definitions", 3, connectorSchema.size());

        Document xsdSchemaDom = connectorSchema.serializeToXsd();
        displayValue("Serialized XSD connector schema", DOMUtil.serializeDOMToString(xsdSchemaDom));

        // Try to re-parse
        var reparsedConnectorSchema = ConnectorSchemaFactory.parse(DOMUtil.getFirstChildElement(xsdSchemaDom), "");
        IntegrationTestTools.assertConnectorSchemaSanity(reparsedConnectorSchema, "re-parsed", true);
        assertEquals("Unexpected number of definitions in re-parsed schema", 3, reparsedConnectorSchema.size());
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
            System.out.printf("CONNECTOR OID=%s, name=%s, version=%s%n",
                    connector.getOid(), connector.getName(), connector.getConnectorVersion());
            System.out.println("--");
            System.out.println(ObjectTypeUtil.dump(connector));
            System.out.println("--");
        }

        System.out.println("---------------------------------------------------------------------");

        assertEquals("Unexpected number of connectors discovered", 8, connectors.size());
    }

    @Test
    public void test020CreateConfiguredConnector() throws Exception {
        cc = connectorFactory.createConnectorInstance(connectorBean,
                "dummy",
                "description of dummy test connector instance");
        OperationResult result = createOperationResult();

        // WHEN
        configure(
                resourceBean.getConnectorConfiguration(),
                ResourceTypeUtil.getSchemaGenerationConstraints(resourceBean),
                result);

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

        cc = connectorFactory.createConnectorInstance(connectorBean,
                "dummy",
                "description of dummy test connector instance");

        configure(
                resourceBean.getConnectorConfiguration(),
                List.of(),
                result);

        // WHEN
        var nativeResourceSchema = cc.fetchResourceSchema(result);
        resourceSchema = nativeToBare(nativeResourceSchema);
        completeResourceSchema = ResourceSchemaFactory.parseCompleteSchema(resourceBean, nativeResourceSchema);

        // THEN
        displayDumpable("Generated resource schema", resourceSchema);
        assertEquals("Unexpected number of definitions", 4, resourceSchema.size());

        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(resourceSchema, resourceBean, true);

        Document xsdSchemaDom = resourceSchema.serializeNativeToXsd();
        assertNotNull("No serialized resource schema", xsdSchemaDom);
        displayValue("Serialized XSD resource schema", DOMUtil.serializeDOMToString(xsdSchemaDom));

        // Try to re-parse
        var reparsedResourceSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(xsdSchemaDom);
        displayDumpable("Re-parsed resource schema", reparsedResourceSchema);
        assertEquals("Unexpected number of definitions in re-parsed schema", 4, reparsedResourceSchema.size());

        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(reparsedResourceSchema, resourceBean, true);
    }

    /**
     * Currently this test fails, because the limited raw schema (accounts only) is not sufficient for the complete schema
     * (as there are reference attributes). To be resolved later.
     */
    @Test(enabled = false)
    public void test031ResourceSchemaAccountObjectClass() throws Exception {
        OperationResult result = createOperationResult();

        cc = connectorFactory.createConnectorInstance(connectorBean,
                "dummy",
                "description of dummy test connector instance");

        configure(
                resourceBean.getConnectorConfiguration(),
                List.of(RI_ACCOUNT_OBJECT_CLASS),
                result);

        // WHEN
        var nativeResourceSchema = cc.fetchResourceSchema(result);
        resourceSchema = ResourceSchemaFactory.nativeToBare(nativeResourceSchema);
        completeResourceSchema = ResourceSchemaFactory.parseCompleteSchema(resourceBean, nativeResourceSchema);

        // THEN
        displayDumpable("Generated resource schema", resourceSchema);

        var definitions = resourceSchema.getDefinitions();
        assertEquals("Unexpected number of definitions", 1, definitions.size());
        assertEquals("Unexpected number of object class definitions", 1, resourceSchema.getObjectClassDefinitions().size());
        display("RESOURCE SCHEMA DEFINITION" + definitions.iterator().next().getTypeName());
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
        var ctx = createExecutionContext();

        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);

        var shadow = ShadowBuilder.withDefinition(accountClassDefinition)
                .withSimpleAttribute(SchemaConstants.ICFS_NAME, ACCOUNT_JACK_USERNAME)
                .asPrismObject();

        when();
        cc.addObject(shadow, ctx, result);

        then();
        DummyAccount dummyAccount = dummyResource.getAccountByName(ACCOUNT_JACK_USERNAME);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " was not created", dummyAccount);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " has no username", dummyAccount.getName());
    }

    @Test
    public void test050Search() throws Exception {

        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);

        final List<PrismObject<ShadowType>> searchResults = new ArrayList<>();

        UcfObjectHandler handler = (ucfObject, result) -> {
            displayDumpable("Search: found", ucfObject);
            checkUcfObject(ucfObject, accountClassDefinition);
            searchResults.add(ucfObject.getPrismObject());
            return true;
        };

        var ctx = createExecutionContext();
        OperationResult result = createOperationResult();

        when();
        cc.search(
                accountClassDefinition, null, handler,
                null, null, null, null,
                ctx, result);

        then();
        assertEquals("Unexpected number of search results", 1, searchResults.size());
    }

    private void checkUcfObject(UcfResourceObject ucfResourceObject, ResourceObjectClassDefinition objectClassDefinition) {
        var object = ucfResourceObject.getPrismObject();
        ShadowType bean = object.asObjectable();
        assertNotNull("No objectClass in shadow " + object, bean.getObjectClass());
        assertEquals("Wrong objectClass in shadow " + object, objectClassDefinition.getTypeName(), bean.getObjectClass());
        Collection<ShadowSimpleAttribute<?>> attributes = ShadowUtil.getSimpleAttributes(object);
        assertNotNull("No attributes in shadow " + object, attributes);
        assertFalse("Empty attributes in shadow " + object, attributes.isEmpty());
    }

    /** MID-8145 */
    @Test
    public void test060GetByUidWithNameHint() throws Exception {
        given();
        var ctx = createExecutionContext();
        var result = createOperationResult();

        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        var uidAttr = accountClassDefinition.<String>getPrimaryIdentifierRequired().instantiateFromRealValue("jack");
        var nameAttr = accountClassDefinition.<String>getSecondaryIdentifierRequired().instantiateFromRealValue("jack");
        var identification = ResourceObjectIdentification.withPrimary(accountClassDefinition, uidAttr, List.of(nameAttr));

        when("getting account by UID with name hint");
        var resourceObject = cc.fetchObject(identification, null, ctx, result);

        then("account is retrieved OK");
        displayDumpable("resourceObject retrieved", resourceObject);
        assertThat(resourceObject).as("resourceObject").isNotNull();
        checkUcfObject(resourceObject, accountClassDefinition);
    }

    /** No operation, no sync changes. */
    @Test
    public void test100FetchEmptyChanges() throws Exception {
        OperationResult result = createOperationResult();
        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        var ctx = createExecutionContext();

        when("current token is fetched");
        UcfSyncToken lastToken = cc.fetchCurrentToken(accountClassDefinition, ctx, result);

        then();
        displayValue("Token", lastToken);
        assertNotNull("No last sync token", lastToken);

        when("changes are fetched");
        CollectingChangeListener handler = new CollectingChangeListener();
        cc.fetchChanges(accountClassDefinition, lastToken, null, null, ctx, handler, result);

        then();
        assertThat(handler.getChanges()).as("changes").isEmpty();
    }

    /** An account is manually added to the resource; then a single sync change is fetched. */
    @Test
    public void test101FetchAddChange() throws Exception {
        OperationResult result = createOperationResult();
        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        var ctx = createExecutionContext();

        UcfSyncToken lastToken = cc.fetchCurrentToken(accountClassDefinition, ctx, result);
        assertNotNull("No last sync token", lastToken);

        given("account is added to the resource");
        dummyResource.setSyncStyle(DummySyncStyle.DUMB);
        DummyAccount newAccount = new DummyAccount("blackbeard");
        newAccount.addAttributeValues("fullname", "Edward Teach");
        newAccount.setEnabled(true);
        newAccount.setPassword("shiverMEtimbers");
        dummyResource.addAccount(newAccount);

        when("changes are fetched");
        CollectingChangeListener handler = new CollectingChangeListener();
        cc.fetchChanges(accountClassDefinition, lastToken, null, null, ctx, handler, result);

        then("there is 1 change, and is sane");

        List<UcfLiveSyncChange> changes = handler.getChanges();
        AssertJUnit.assertEquals(1, changes.size());
        UcfLiveSyncChange change = changes.get(0);
        assertNotNull("null change", change);
        var resourceObject = change.getResourceObject();
        assertNotNull("null current resource object", resourceObject);
        PrismAsserts.assertParentConsistency(resourceObject.getPrismObject());
        Collection<ShadowSimpleAttribute<?>> identifiers = change.getIdentifiers();
        assertNotNull("null identifiers", identifiers);
        assertFalse("empty identifiers", identifiers.isEmpty());
    }

    /** Checks if the hierarchical support on {@link DummyResource} works OK (no UCF/ConnId contact). */
    @Test
    public void test200AddHierarchicalObjects() throws Exception {
        initializeHierarchicalResourceIfNeeded();

        given("an org exists");
        hierarchicalResourceCtl.addOrg("org200");

        when("top-level account is added");
        hierarchicalResourceCtl.addAccount("test");

        then("it is there");
        assertThat(hierarchicalResource.getAccountByName("test")).isNotNull();

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
        assertThat(hierarchicalResource.getAccountByName("test:org200")).isNotNull();
    }

    /** Again, checks the hierarchical support (deleting non-empty org); no UCF/ConnId access here. */
    @Test
    public void test210DeleteNonEmptyOrgInHierarchy() throws Exception {
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

    /** Again a hierarchical support test (rename org -> renaming its content). No UCF/ConnId. */
    @Test
    public void test220RenameOrgInHierarchy() throws Exception {
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

    /** For test2xx. */
    private void initializeHierarchicalResourceIfNeeded() throws Exception {
        if (hierarchicalResource != null) {
            return;
        }

        hierarchicalResourceCtl = DummyResourceContoller.create("hierarchical")
                .extendSchemaPirate();

        hierarchicalResource = hierarchicalResourceCtl.getDummyResource();
        hierarchicalResource.setUidMode(UidMode.UUID);
        hierarchicalResource.setHierarchicalObjectsEnabled(true);

        // No need for UCF/ConnId access
    }

    private void assertOrgExists(String name)
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        DummyOrg org = hierarchicalResource.getOrgByName(name);
        assertThat(org).as("org named '" + name + "'").isNotNull();
    }

    private void assertAccountExists(String name)
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        DummyAccount account = hierarchicalResource.getAccountByName(name);
        assertThat(account).as("account named '" + name + "'").isNotNull();
    }

    /** Checks whether the schema with reference attributes is fetched, serialized, and parsed correctly. */
    @Test
    public void test300SchemaWithReferenceAttributes() throws Exception {
        initializeHrScenarioIfNeeded();

        var completeSchema = hrScenario.getResourceSchemaRequired();
        checkHrSchema(completeSchema);

        when("native schema is serialized to XSD and reparsed");
        var schemaDocument = completeSchema.serializeNativeToXsd();
        displayValue("Native XML schema", DOMUtil.serializeDOMToString(schemaDocument));

        var reparsedNativeSchema = parseNativeSchema(schemaDocument.getDocumentElement(), "");
        displayDumpable("Reparsed native schema", reparsedNativeSchema);

        var reparsedCompleteSchema = parseCompleteSchema(hrScenario.getResourceBean(), reparsedNativeSchema);
        displayDumpable("Reparsed complete schema", reparsedCompleteSchema);

        checkHrSchema(reparsedCompleteSchema);
    }

    private void checkHrSchema(CompleteResourceSchema completeSchema) throws SchemaException {
        then("native object class definitions are OK");
        var nativeSchema = completeSchema.getNativeSchema();
        assertThat(nativeSchema.getObjectClassDefinitions()).as("object class definitions").hasSize(7);

        and("native 'contract' class definition is OK");
        var contractClassDefN = nativeSchema.findObjectClassDefinition(Contract.OBJECT_CLASS_NAME.xsd());
        assertThat(contractClassDefN).as("contract definition").isNotNull();

        and("native 'contract <-> org' and 'contract <-> person' references definitions are OK");
        // contract-org
        var orgDefN = contractClassDefN.findReferenceAttributeDefinition(Contract.LinkNames.ORG.q());
        assertThat(orgDefN).as("contract.org ref attr definition").isNotNull();
        assertThat(orgDefN.getTypeName()).as("contract.org type").isEqualTo(ContractOrgUnit.NAME.xsd());
        assertThat(orgDefN.getReferenceParticipantRole()).as("role of contract in contract-org reference").isEqualTo(SUBJECT);

        and("complete schema is OK");
        completeSchema.findDefinitionForObjectClassRequired(Contract.OBJECT_CLASS_NAME.xsd())
                .findReferenceAttributeDefinitionRequired(Contract.LinkNames.ORG.q());
    }

    /** Creates some references manually, and then queries them via UCF. */
    @Test
    public void test310QueryReferences() throws Exception {
        initializeHrScenarioIfNeeded();

        given("some objects and links are created");

        DummyObject sciences = hrScenario.orgUnit.add("sciences")
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Sciences");
        DummyObject law = hrScenario.orgUnit.add("law")
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Law");

        DummyObject john = hrScenario.person.add("john")
                .addAttributeValue(Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(Person.AttributeNames.LAST_NAME.local(), "Doe")
                .addAttributeValue(Person.AttributeNames.TITLE.local(), "Ing.");

        DummyObject johnContractSciences = hrScenario.contract.add("john-sciences");
        DummyObject johnContractLaw = hrScenario.contract.add("john-law");

        hrScenario.personContract.add(john, johnContractSciences);
        hrScenario.personContract.add(john, johnContractLaw);

        hrScenario.contractOrgUnit.add(johnContractSciences, sciences);
        hrScenario.contractOrgUnit.add(johnContractLaw, law);

        then("references on the resource are OK");

        displayDumpable("dummy resource", hrScenario.getDummyResource());

        assertThat(john.getLinkedObjects(Person.LinkNames.CONTRACT.local()))
                .as("john's contracts")
                .containsExactlyInAnyOrder(johnContractSciences, johnContractLaw);
        assertThat(johnContractSciences.getLinkedObjects(Contract.LinkNames.ORG.local()))
                .as("john first contract's org")
                .containsExactlyInAnyOrder(sciences);
        assertThat(johnContractLaw.getLinkedObjects(Contract.LinkNames.ORG.local()))
                .as("john second contract's org")
                .containsExactlyInAnyOrder(law);
        assertThat(sciences.getLinkedObjects(OrgUnit.LinkNames.CONTRACT.local()))
                .as("sciences' contracts")
                .containsExactlyInAnyOrder(johnContractSciences);
        assertThat(law.getLinkedObjects(OrgUnit.LinkNames.CONTRACT.local()))
                .as("law's contracts")
                .containsExactlyInAnyOrder(johnContractLaw);

        when("references are queried via UCF");

        OperationResult result = createOperationResult();
        var ctx = createExecutionContext(hrScenario.getResourceBean(), hrScenario.getResourceSchemaRequired());

        ResourceObjectDefinition personDefinition = hrScenario.person.getObjectClassDefinition();
        var handler = new UcfObjectHandler.Collecting();
        hrConnectorInstance.search(
                personDefinition,
                PrismContext.get().queryFor(ShadowType.class)
                        .item(
                                Person.AttributeNames.FIRST_NAME.path(),
                                personDefinition.findSimpleAttributeDefinitionRequired(Person.AttributeNames.FIRST_NAME.q()))
                        .eq("John")
                        .build(),
                handler,
                null, null, null, EXCEPTION, ctx, result);

        then("these are OK");

        List<UcfResourceObject> objects = handler.getCollectedObjects();
        assertThat(objects).as("objects found").hasSize(1);
        UcfResourceObject johnUcfObject = objects.get(0);

        displayDumpable("john's UCF object", johnUcfObject);
        assertThat(johnUcfObject.getAttributeRealValues(Person.AttributeNames.FIRST_NAME.q()))
                .as("john's first name")
                .containsExactlyInAnyOrder("John");
        assertThat(johnUcfObject.getAttributeRealValues(Person.AttributeNames.LAST_NAME.q()))
                .as("john's last name")
                .containsExactlyInAnyOrder("Doe");
        assertThat(johnUcfObject.getAttributeRealValues(Person.AttributeNames.TITLE.q()))
                .as("john's title")
                .containsExactlyInAnyOrder("Ing.");
        var refAttributes = johnUcfObject.getReferenceAttributes();
        assertThat(refAttributes).as("john's ref attributes").hasSize(1);
        var contractItem = refAttributes.iterator().next();
        assertThat(contractItem.getElementName()).as("ref attr name").isEqualTo(Person.LinkNames.CONTRACT.q());
        List<? extends ShadowReferenceAttributeValue> contracts = contractItem.getReferenceValues();
        assertThat(contracts).as("john's contracts").hasSize(2);
        for (ShadowReferenceAttributeValue contract : contracts) {
            assertThat(contract.getTargetObjectClassName())
                    .as("target class name")
                    .isEqualTo(Contract.OBJECT_CLASS_NAME.xsd());
            var contractAttrContainer = contract.getAttributesContainerRequired();
            assertThat(contractAttrContainer.getSimpleAttributes())
                    .as("contract simple attributes")
                    .hasSize(2);
            assertThat(contractAttrContainer.getReferenceAttributes())
                    .as("contract reference attributes")
                    .hasSize(1);
            var orgRefAttributes = contractAttrContainer.getReferenceAttributes().iterator().next();
            assertThat(orgRefAttributes.getElementName())
                    .as("reference attribute name")
                    .isEqualTo(Contract.LinkNames.ORG.q());
            var orgs = orgRefAttributes.getReferenceValues();
            assertThat(orgs).as("contract's orgs").hasSize(1);
            var org = orgs.iterator().next();
            var orgAttrContainer = org.getAttributesContainerRequired();
            assertThat(orgAttrContainer.getSimpleAttributes())
                    .as("org attributes in contract")
                    .hasSize(1);
            ShadowSimpleAttribute<?> orgAttribute = orgAttrContainer.getSimpleAttributes().iterator().next();
            assertThat(orgAttribute.getElementName()).as("org attribute name").isEqualTo(ICFS_NAME);
            var orgName = (String) orgAttribute.getRealValue();

            var contractName = contractAttrContainer.getNamingAttribute().getRealValue();
            if ("john-sciences".equals(contractName)) {
                assertThat(orgName).as("associated org name").isEqualTo("sciences");
            } else if ("john-law".equals(contractName)) {
                assertThat(orgName).as("associated org name").isEqualTo("law");
            } else {
                throw new AssertionError("Unknown contract: " + contractName);
            }
        }
    }

    /** Create account with references via UCF. */
    @Test
    public void test320CreateAccountWithReferences() throws Exception {
        initializeHrScenarioIfNeeded();

        var result = createOperationResult();
        var ctx = createExecutionContext(hrScenario.getResourceBean(), hrScenario.getResourceSchemaRequired());
        var resourceSchema = hrScenario.getResourceSchemaRequired();

        given("engineering org unit");
        var engineering = createHrOrgUnit("engineering", result);

        and("ann's account and contract");
        var contractClassDefinition = resourceSchema.findObjectClassDefinitionRequired(Contract.OBJECT_CLASS_NAME.xsd());
        var annContractShadow =
                ShadowBuilder.withDefinition(contractClassDefinition)
                        .withSimpleAttribute(Contract.AttributeNames.NAME.q(), "ann-engineering")
                        .withReferenceAttribute(Contract.LinkNames.ORG.q(), engineering)
                        .asAbstractShadow();

        var personClassDefinition = resourceSchema.findObjectClassDefinitionRequired(Person.OBJECT_CLASS_NAME.xsd());
        var annShadow =
                ShadowBuilder.withDefinition(personClassDefinition)
                        .withSimpleAttribute(Person.AttributeNames.NAME.q(), "ann")
                        .withSimpleAttribute(Person.AttributeNames.FIRST_NAME.q(), "Ann")
                        .withSimpleAttribute(Person.AttributeNames.LAST_NAME.q(), "Green")
                        .withReferenceAttribute(Person.LinkNames.CONTRACT.q(), annContractShadow)
                        .asPrismObject();

        when("ann is created on the resource");
        hrConnectorInstance.addObject(annShadow, ctx, result);

        then("she's there");
        displayDumpable("dummy resource", hrScenario.getDummyResource());

        var annShadowAfter = searchHrObjectByName(personClassDefinition, Person.AttributeNames.NAME, "ann", result);
        displayDumpable("ann's UCF object", annShadowAfter);

        assertThat(annShadowAfter.getAttributeRealValues(Person.AttributeNames.FIRST_NAME.q()))
                .containsExactlyInAnyOrder("Ann");
        assertThat(annShadowAfter.getAttributeRealValues(Person.AttributeNames.LAST_NAME.q()))
                .containsExactlyInAnyOrder("Green");

        var references = annShadowAfter.getReferenceAttributes();
        assertThat(references).as("ann's references").hasSize(1);
        List<? extends ShadowReferenceAttributeValue> contracts = references.iterator().next().getReferenceValues();
        assertThat(contracts).as("ann's contracts").hasSize(1);
        var contract = contracts.get(0);

        var contractAttrContainer = contract.getAttributesContainerRequired();
        assertThat(contractAttrContainer.getNamingAttribute().getRealValue()).isEqualTo("ann-engineering");
        assertThat(contractAttrContainer.getSimpleAttributes())
                .as("contract simple attributes")
                .hasSize(2);
        assertThat(contractAttrContainer.getReferenceAttributes())
                .as("contract reference attributes")
                .hasSize(1);
        var orgRefAttribute = contractAttrContainer.getReferenceAttributes().iterator().next();
        assertThat(orgRefAttribute.getElementName())
                .as("reference attribute name")
                .isEqualTo(Contract.LinkNames.ORG.q());

        var orgs = orgRefAttribute.getReferenceValues();
        assertThat(orgs).as("contract's orgs").hasSize(1);

        var org = orgs.iterator().next();
        var orgAttrContainer = org.getAttributesContainerRequired();
        assertThat(orgAttrContainer.getSimpleAttributes()).as("org attributes in contract").hasSize(1);
        var orgName = (String) orgAttrContainer.getSimpleAttributes().iterator().next().getRealValue();
        assertThat(orgName).as("associated org name").isEqualTo("engineering");
    }

    /** Add and delete references via UCF. */
    @Test
    public void test330AddDeleteReference() throws Exception {
        initializeHrScenarioIfNeeded();

        var result = createOperationResult();
        var ctx = createExecutionContext(hrScenario.getResourceBean(), hrScenario.getResourceSchemaRequired());
        var resourceSchema = hrScenario.getResourceSchemaRequired();
        var contractClassDefinition = resourceSchema.findObjectClassDefinitionRequired(Contract.OBJECT_CLASS_NAME.xsd());
        var personClassDefinition = resourceSchema.findObjectClassDefinitionRequired(Person.OBJECT_CLASS_NAME.xsd());

        given("pharmacy and bob are on the resource");
        var pharmacy = createHrOrgUnit("pharmacy", result);
        var bob = createHrPerson("bob", result);

        and("bob's new contract is prepared in memory");
        var bobContractReferenceValue =
                ShadowReferenceAttributeValue.fromShadow(
                        ShadowBuilder.withDefinition(contractClassDefinition)
                                .withSimpleAttribute(Contract.AttributeNames.NAME.q(), "bob-pharmacy")
                                .withReferenceAttribute(Contract.LinkNames.ORG.q(), pharmacy)
                                .asAbstractShadow());

        when("the contract is created on the resource");
        var referenceAddDelta =
                personClassDefinition.findReferenceAttributeDefinition(Person.LinkNames.CONTRACT.q())
                        .createEmptyDelta();
        referenceAddDelta.addValueToAdd(bobContractReferenceValue.clone());
        hrConnectorInstance.modifyObject(
                Objects.requireNonNull(bob.getPrimaryIdentification()),
                bob.getPrismObject(),
                List.of(new ReferenceModificationOperation(referenceAddDelta)),
                null, ctx, result);

        then("it's there");
        displayDumpable("dummy resource", hrScenario.getDummyResource());

        displayDumpable("bob's dummy object", hrScenario.person.getByNameRequired("bob"));

        var bobShadowAfter = searchHrObjectByName(personClassDefinition, Person.AttributeNames.NAME, "bob", result);
        displayDumpable("bob's UCF object", bobShadowAfter);

        var references = bobShadowAfter.getReferenceAttributes();
        assertThat(references).as("bob's references").hasSize(1);
        List<? extends ShadowReferenceAttributeValue> contracts = references.iterator().next().getReferenceValues();
        assertThat(contracts).as("bob's contracts").hasSize(1);
        var contract = contracts.get(0);

        var contractAttrContainer = contract.getAttributesContainerRequired();
        assertThat(contractAttrContainer.getNamingAttribute().getRealValue()).isEqualTo("bob-pharmacy");
        assertThat(contractAttrContainer.getSimpleAttributes()).as("contract simple attributes").hasSize(2);

        Collection<ShadowReferenceAttribute> contractRefAttributes = contractAttrContainer.getReferenceAttributes();
        assertThat(contractRefAttributes).as("contract ref attributes").hasSize(1);
        var orgRefAttr = contractRefAttributes.iterator().next();
        assertThat(orgRefAttr.getElementName()).as("ref attr name").isEqualTo(Contract.LinkNames.ORG.q());

        var orgs = orgRefAttr.getReferenceValues();
        assertThat(orgs).as("contract's orgs").hasSize(1);

        var org = orgs.iterator().next();
        var orgAttrContainer = org.getAttributesContainerRequired();
        assertThat(orgAttrContainer.getSimpleAttributes()).as("org attributes in contract").hasSize(1);
        var orgName = (String) orgAttrContainer.getSimpleAttributes().iterator().next().getRealValue();
        assertThat(orgName).as("associated org name").isEqualTo("pharmacy");

        when("the contract is deleted from the resource");
        var referenceDeleteDelta =
                personClassDefinition.findReferenceAttributeDefinition(Person.LinkNames.CONTRACT.q())
                        .createEmptyDelta();
        referenceDeleteDelta.addValueToDelete(bobContractReferenceValue.clone());

        hrConnectorInstance.modifyObject(
                Objects.requireNonNull(bob.getPrimaryIdentification()),
                bob.getPrismObject(),
                List.of(new ReferenceModificationOperation(referenceDeleteDelta)),
                null, ctx, result);

        then("it's no longer there there");
        displayDumpable("dummy resource", hrScenario.getDummyResource());

        displayDumpable("bob's dummy object", hrScenario.person.getByNameRequired("bob"));

        var bobShadowAfterDeletion =
                searchHrObjectByName(personClassDefinition, Person.AttributeNames.NAME, "bob", result);
        displayDumpable("bob's UCF object", bobShadowAfterDeletion);

        assertThat(bobShadowAfterDeletion.getReferenceAttributes()).as("bob's references").isEmpty();
    }

    private @NotNull UcfResourceObject createHrOrgUnit(String name, OperationResult result) throws Exception {
        var classDefinition =
                hrScenario.getResourceSchemaRequired().findObjectClassDefinitionRequired(OrgUnit.OBJECT_CLASS_NAME.xsd());
        var ctx = createExecutionContext();
        hrConnectorInstance.addObject(
                ShadowBuilder.withDefinition(classDefinition)
                        .withSimpleAttribute(OrgUnit.AttributeNames.NAME.q(), name)
                        .asPrismObject(),
                ctx, result);
        return searchHrObjectByName(classDefinition, OrgUnit.AttributeNames.NAME, name, result);
    }

    @SuppressWarnings("SameParameterValue")
    private @NotNull UcfResourceObject createHrPerson(String name, OperationResult result) throws Exception {
        var classDefinition =
                hrScenario.getResourceSchemaRequired().findObjectClassDefinitionRequired(Person.OBJECT_CLASS_NAME.xsd());
        var ctx = createExecutionContext();
        hrConnectorInstance.addObject(
                ShadowBuilder.withDefinition(classDefinition)
                        .withSimpleAttribute(Person.AttributeNames.NAME.q(), name)
                        .asPrismObject(),
                ctx, result);
        return searchHrObjectByName(classDefinition, Person.AttributeNames.NAME, name, result);
    }

    private @NotNull UcfResourceObject searchHrObjectByName(
            ResourceObjectClassDefinition objectClassDefinition, AttrName nameAttr, String nameAttrValue, OperationResult result)
            throws Exception {
        var ctx = createExecutionContext(hrScenario.getResourceBean(), hrScenario.getResourceSchemaRequired());
        var handler = new UcfObjectHandler.Collecting();
        hrConnectorInstance.search(
                objectClassDefinition,
                objectClassDefinition.queryFor()
                        .item(nameAttr.path()).eq(nameAttrValue)
                        .build(),
                handler, null, null,
                null, null, ctx, result);
        return MiscUtil.extractSingletonRequired(handler.getCollectedObjects());
    }

    /** For test3xx. */
    private void initializeHrScenarioIfNeeded() throws Exception {
        if (hrScenario != null) {
            return;
        }

        OperationResult result = createOperationResult();

        var resourceDef = PrismTestUtil.<ResourceType>parseObjectable(HR_RESOURCE_DUMMY_FILE);

        DummyResourceContoller hrResourceCtl = DummyResourceContoller.create("hr")
                .setResource(resourceDef);
        hrScenario = DummyHrScenario.on(hrResourceCtl).initialize();

        var configuration = resourceDef.getConnectorConfiguration();
        displayDumpable("Configuration", configuration);

        hrConnectorInstance = connectorFactory
                .createConnectorInstance(connectorBean, "hr", "")
                .configure(
                        new ConnectorConfiguration(
                                configuration.asPrismContainerValue(),
                                List.of()),
                        new ConnectorConfigurationOptions(),
                        result)
                .initialize(null, null, result);

        var nativeSchema = hrConnectorInstance.fetchResourceSchema(result);
        displayDumpable("HR resource schema (native)", nativeSchema);
        assertThat(nativeSchema).as("native HR resource schema").isNotNull();

        hrScenario.attachResourceSchema(
                parseCompleteSchema(resourceDef, nativeSchema));

        displayDumpable("HR resource schema", hrScenario.getResourceSchemaRequired());
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
