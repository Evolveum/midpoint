/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static com.evolveum.midpoint.test.util.TestUtil.getAttrQName;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertProvisioningAccountShadow;
import static com.evolveum.midpoint.test.asserter.predicates.StringAssertionPredicates.startsWith;
import static com.evolveum.midpoint.test.asserter.predicates.TimeAssertionPredicates.approximatelyCurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.connector.AbstractBaseDummyConnector;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.provisioning.impl.resources.ConnectorManager;
import com.evolveum.midpoint.schema.constants.TestResourceOpNames;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.util.exception.*;

import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource for speed and flexibility.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class AbstractBasicDummyTest extends AbstractDummyTest {

    protected CachingMetadataType capabilitiesCachingMetadataType;
    protected String willIcfUid;
    protected XMLGregorianCalendar lastPasswordModifyStart;
    protected XMLGregorianCalendar lastPasswordModifyEnd;

    protected MatchingRule<String> getUidMatchingRule() {
        return null;
    }

    /**
     * Returns true if the resource needs pre-fetch operation.
     * E.g. for avoidDuplicateValues or attributeContentRequirement.
     */
    protected boolean isPreFetchResource() {
        return false;
    }

    protected int getExpectedRefinedSchemaDefinitions() {
        return dummyResource.getNumberOfObjectclasses();
    }

    @AfterClass
    public static void assertCleanShutdown() {
        dummyResource.assertNoConnections();
    }

    @Test
    public void test000Integrity() throws Exception {
        displayValue("Dummy resource instance", dummyResource.toString());

        assertNotNull("Resource is null", resource);
        assertNotNull("ResourceType is null", resourceBean);

        OperationResult result = createOperationResult();

        ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result)
                .asObjectable();
        String connectorOid = resource.getConnectorRef().getOid();
        ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
        assertNotNull(connector);
        display("Dummy Connector", connector);

        assertSuccess(result);

        // Check connector schema
        IntegrationTestTools.assertConnectorSchemaSanity(connector, prismContext);

        IntegrationTestTools.assertNoSchema(resource);
    }

    /**
     * Check whether the connectors were discovered correctly and were added to
     * the repository.
     */
    @Test
    public void test010ListConnectors() throws Exception {
        // GIVEN
        OperationResult result = createOperationResult();

        // WHEN
        List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class,
                null, null, result);

        // THEN
        assertSuccess(result);

        assertFalse("No connector found", connectors.isEmpty());
        for (PrismObject<ConnectorType> connPrism : connectors) {
            ConnectorType conn = connPrism.asObjectable();
            display("Found connector " + conn, conn);

            displayValue("XML " + conn, PrismTestUtil.serializeToXml(conn));

            XmlSchemaType xmlSchemaType = conn.getSchema();
            assertNotNull("xmlSchemaType is null", xmlSchemaType);
            Element connectorXsdSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(conn);
            assertNotNull("No schema", connectorXsdSchemaElement);

            // Try to parse the schema
            PrismSchema schema = PrismSchemaImpl.parse(connectorXsdSchemaElement, true, "connector schema " + conn, prismContext);
            assertNotNull("Cannot parse schema", schema);
            assertFalse("Empty schema", schema.isEmpty());

            displayDumpable("Parsed connector schema " + conn, schema);

            QName configurationElementQname = new QName(conn.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
            PrismContainerDefinition configurationContainer = schema
                    .findContainerDefinitionByElementName(configurationElementQname);
            assertNotNull("No " + configurationElementQname + " element in schema of " + conn, configurationContainer);
            PrismContainerDefinition definition = schema
                    .findItemDefinitionByElementName(new QName(ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart()),
                            PrismContainerDefinition.class);
            assertNotNull("Definition of <configuration> property container not found", definition);
            assertFalse("Empty definition", definition.isEmpty());
        }
    }

    /**
     * Running discovery for a second time should return nothing - as nothing
     * new was installed in the meantime.
     */
    @Test
    public void test012ConnectorRediscovery() {
        given();
        OperationResult result = createOperationResult();

        when();
        Set<ConnectorType> discoverLocalConnectors = connectorManager.discoverLocalConnectors(result);

        then();
        assertSuccess("discoverLocalConnectors failed", result);
        assertTrue("Rediscovered something", discoverLocalConnectors.isEmpty());
    }

    /**
     * List resources with noFetch option. This is what GUI does. This operation
     * should be harmless and should not change resource state.
     */
    @Test
    public void test015ListResourcesNoFetch() throws Exception {
        // GIVEN
        Task task = createPlainTask();
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());

        // WHEN
        SearchResultList<PrismObject<ResourceType>> resources = provisioningService.searchObjects(ResourceType.class, null, options, task, result);

        // THEN
        assertSuccess(result);

        assertFalse("No resources found", resources.isEmpty());
        for (PrismObject<ResourceType> resource : resources) {
            ResourceType resourceType = resource.asObjectable();
            display("Found resource " + resourceType, resourceType);

            displayValue("XML " + resourceType, PrismTestUtil.serializeToXml(resourceType));

            XmlSchemaType xmlSchemaType = resourceType.getSchema();
            if (xmlSchemaType != null) {
                Element xsdSchemaElement = ResourceTypeUtil.getResourceXsdSchema(resourceType);
                assertNull("Found schema in " + resource, xsdSchemaElement);
            }
        }

        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
    }

    @Test
    public void test016PartialConfigurationSuccess() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        int cachedConnectorsCount = getSizeOfConnectorCache();

        dummyResource.assertNoConnections();

        // Some connector initialization and other things might happen in previous tests.
        // The monitor is static, not part of spring context, it will not be cleared

        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
        rememberResourceCacheStats();

        // Check that there is no schema before test (pre-condition)
        PrismObject<ResourceType> resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        ResourceType resource = new ResourceType()
                .name("newResource")
                .connectorRef(resourceBefore.asObjectable().getConnectorRef())
                .connectorConfiguration(resourceBefore.asObjectable().getConnectorConfiguration());

        assertNotNull("No connector ref", resource.getConnectorRef());
        assertNotNull("No connector ref OID", resource.getConnectorRef().getOid());
        ConnectorType connector = repositoryService.getObject(ConnectorType.class,
                resource.getConnectorRef().getOid(), null, result).asObjectable();
        assertNotNull(connector);
        IntegrationTestTools.assertNoSchema("Found schema before test connection. Bad test setup?", resource);

        // WHEN
        OperationResult testResult = provisioningService.testPartialConfiguration(resource.asPrismObject(), task, result);

        // THEN
        display("Test result", testResult);
        OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_INSTANTIATION);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_INITIALIZATION);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_CONNECTION);
        assertSuccess(connectorResult);
        assertSuccess(testResult);

        PrismObject<ResourceType> resourceAfter = resource.asPrismObject();
        XmlSchemaType xmlSchemaTypeAfter = resourceAfter.asObjectable().getSchema();
        assertNull("Resource contains schema after partial configuration test", xmlSchemaTypeAfter);
        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceAfter);
        assertNull("Resource contains schema after partial configuration test", resourceXsdSchemaElementAfter);
        assertNull("Resource contains capabilities after partial configuration test", resource.getCapabilities());

        assertEquals("Was created entry connector in cache", cachedConnectorsCount, getSizeOfConnectorCache());

        IntegrationTestTools.displayXml("Resource XML", resourceAfter);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        // One increment for availability status, the other for schema

        dummyResource.assertConnections(1);

        assertNull("Resource was saved to repo, during partial configuration test", findResourceByName("newResource", testResult));
    }

    private int getSizeOfConnectorCache() {
        return connectorManager.getStateInformation().stream().filter(
                state -> ConnectorManager.CONNECTOR_INSTANCE_CACHE_NAME.equals(state.getName())).findFirst().get().getSize();
    }

    @Test
    public void test017PartialConfigurationFail() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        int cachedConnectorsCount = getSizeOfConnectorCache();

        dummyResource.assertConnections(1);

        // Some connector initialization and other things might happen in previous tests.
        // The monitor is static, not part of spring context, it will not be cleared

        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
        rememberResourceCacheStats();

        // Check that there is no schema before test (pre-condition)
        PrismObject<ResourceType> resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        ResourceType resource = new ResourceType()
                .name("newResourceFail")
                .connectorRef(resourceBefore.asObjectable().getConnectorRef())
                .connectorConfiguration(resourceBefore.asObjectable().getConnectorConfiguration());

        PrismProperty<Object> instanceId = resource.asPrismObject().findProperty(
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME,
                        "instanceId"));
        @NotNull PrismContainerValue<Containerable> confPropertiesContainer = resource.asPrismObject().findContainer(
                ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME)).getValue();
        confPropertiesContainer.remove(instanceId);

        assertNotNull("No connector ref", resource.getConnectorRef());
        assertNotNull("No connector ref OID", resource.getConnectorRef().getOid());
        ConnectorType connector = repositoryService.getObject(ConnectorType.class,
                resource.getConnectorRef().getOid(), null, result).asObjectable();
        assertNotNull(connector);
        IntegrationTestTools.assertNoSchema("Found schema before test connection. Bad test setup?", resource);

        // WHEN
        OperationResult testResult = provisioningService.testPartialConfiguration(resource.asPrismObject(), task, result);

        // THEN
        display("Test result", testResult);
        OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_INSTANTIATION);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_INITIALIZATION);
        assertTestResourceFailure(connectorResult, TestResourceOpNames.CONNECTOR_CONNECTION);
        assertFailure(connectorResult);
        assertFailure(testResult);

        PrismObject<ResourceType> resourceAfter = resource.asPrismObject();
        XmlSchemaType xmlSchemaTypeAfter = resourceAfter.asObjectable().getSchema();
        assertNull("Resource contains schema after partial configuration test", xmlSchemaTypeAfter);
        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceAfter);
        assertNull("Resource contains schema after partial configuration test", resourceXsdSchemaElementAfter);
        assertNull("Resource contains capabilities after partial configuration test", resource.getCapabilities());

        assertEquals("Was created entry connector in cache", cachedConnectorsCount, getSizeOfConnectorCache());

        IntegrationTestTools.displayXml("Resource XML", resourceAfter);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        // One increment for availability status, the other for schema

        dummyResource.assertConnections(2);

        assertNull("Resource was saved to repo, during partial configuration test", findResourceByName("newResourceFail", testResult));
    }

    /**
     * This should be the very first test that works with the resource.
     * <p>
     * The original repository object does not have resource schema. The schema
     * should be generated from the resource on the first use. This is the test
     * that executes testResource and checks whether the schema was generated.
     */
    @Test
    public void test020TestResource() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyResource.assertConnections(2);

        // Some connector initialization and other things might happen in previous tests.
        // The monitor is static, not part of spring context, it will not be cleared

        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
        rememberResourceCacheStats();

        // Check that there is no schema before test (pre-condition)
        PrismObject<ResourceType> resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        ResourceType resourceTypeBefore = resourceBefore.asObjectable();
        rememberResourceVersion(resourceBefore.getVersion());
        assertNotNull("No connector ref", resourceTypeBefore.getConnectorRef());
        assertNotNull("No connector ref OID", resourceTypeBefore.getConnectorRef().getOid());
        ConnectorType connector = repositoryService.getObject(ConnectorType.class,
                resourceTypeBefore.getConnectorRef().getOid(), null, result).asObjectable();
        assertNotNull(connector);
        IntegrationTestTools.assertNoSchema("Found schema before test connection. Bad test setup?", resourceTypeBefore);

        // WHEN
        OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID, task, result);

        // THEN
        display("Test result", testResult);
        OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_INSTANTIATION);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_INITIALIZATION);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_CONNECTION);
        assertTestResourceSuccess(connectorResult, TestResourceOpNames.CONNECTOR_CAPABILITIES);
        assertSuccess(connectorResult);
        assertTestResourceSuccess(testResult, TestResourceOpNames.RESOURCE_SCHEMA);
        assertSuccess(testResult);

        assertResourceCacheMissesIncrement(1);

        PrismObject<ResourceType> resourceRepoAfter =
                repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();

        String localNodeId = taskManager.getNodeId();
        // @formatter:off
        assertResource(resourceRepoAfter, "Resource after test")
                .display()
                .operationalState()
                    .assertAny()
                        .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.UP)
                        .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                        .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, approximatelyCurrent(60000))
                        .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status set to UP"))
                    .end()
                    .operationalStateHistory()
                        .assertSize(1)
                        .value(0)
                            .assertPropertyEquals(OperationalStateType.F_LAST_AVAILABILITY_STATUS, AvailabilityStatusType.UP)
                            .assertPropertyEquals(OperationalStateType.F_NODE_ID, localNodeId)
                            .assertItemValueSatisfies(OperationalStateType.F_TIMESTAMP, approximatelyCurrent(60000))
                            .assertItemValueSatisfies(OperationalStateType.F_MESSAGE, startsWith("Status set to UP"));
        // @formatter:on

        XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
        assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
        assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);

        IntegrationTestTools.displayXml("Resource XML", resourceRepoAfter);

        CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
        assertNotNull("No caching metadata", cachingMetadata);
        assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
        assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

        Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
        ResourceSchema parsedSchema = ResourceSchemaParser.parse(xsdElement, resourceTypeBefore.toString());
        assertNotNull("No schema after parsing", parsedSchema);

        // schema will be checked in next test

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 1);
        // No longer parsing schema XSD during test connection (traditional resource completion is not invoked anymore)
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);

        // One increment for availability status, the other for schema
        assertResourceVersionIncrement(resourceRepoAfter, 2);

        dummyResource.assertConnections(3);
        assertDummyConnectorInstances(1);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);
        assertResourceCacheMissesIncrement(1); // incurred in assertDummyConnectorInstances call

        assertResourceAfterTest();
    }

    protected void assertResourceAfterTest() {
        // For use in subclasses
    }

    @Test
    public void test021DiscoverConfiguration() throws Exception {
        given();
        OperationResult result = createOperationResult();

        int cachedConnectorsCount = getSizeOfConnectorCache();

        dummyResource.assertConnections(3);

        String unlessStringBefore = dummyResource.getUselessString();

        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
        rememberResourceCacheStats();

        // Check that there is no schema before test (pre-condition)
        PrismObject<ResourceType> resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
        PrismObject<ResourceType> resource = new ResourceType()
                .name("newResource")
                .connectorRef(resourceBefore.asObjectable().getConnectorRef())
                .connectorConfiguration(resourceBefore.asObjectable().getConnectorConfiguration()).asPrismObject();

        PrismProperty<Object> supportValidity = resource.findOrCreateProperty(
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME,
                        "supportValidity"));

        supportValidity.setRealValue(Boolean.valueOf((String)getRealValue(supportValidity)) ? false : true);

        List<String> expectedSuggestions =
                List.of(getSuggestionForProperty(resource, "instanceId"),
                        getSuggestionForProperty(resource, "uselessString"));

        when();
        Collection<PrismProperty<?>> suggestions =
                provisioningService.discoverConfiguration(resource, result)
                        .getDiscoveredProperties();

        then();
        assertSuccess(result);

        assertResourceCacheMissesIncrement(0);
        assertResourceCacheHitsIncrement(0);

        assertThat(suggestions)
                .as("suggested properties")
                .hasSize(1);
        suggestions.forEach(suggestion -> {
            Collection<?> suggestionValues = suggestion.getDefinition().getSuggestedValues().stream()
                    .map(displayVal -> displayVal.getValue()).collect(Collectors.toList());
            assertTrue("Unexpected value of suggestion " + suggestion.getDefinition().getSuggestedValues() + ", expected: " + expectedSuggestions,
                    expectedSuggestions.containsAll(suggestionValues));
        });

        assertEquals("Was created entry connector in cache", cachedConnectorsCount, getSizeOfConnectorCache());

        IntegrationTestTools.displayXml("Resource XML", resource);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);

        dummyResource.assertConnections(4);
        dummyResource.setUselessString(unlessStringBefore);
    }

    private Object getRealValue(PrismProperty<Object> property) {
        if (property.getRealValue() instanceof RawType) {
            try {
                return  ((RawType) property.getRealValue()).getValue();
            } catch (SchemaException e) {
                //ignore exception
                PrimitiveXNode primitiveXNode = ((PrimitiveXNode)((MapXNode) ((RawType) property.getRealValue())
                        .getXnode()).get(new QName("clearValue")));
                if (primitiveXNode != null) {
                    return primitiveXNode.getStringValue();
                }
                return null;
            }
        } else {
            return property.getRealValue();
        }
    }

    private String getSuggestionForProperty(PrismObject<ResourceType> resource, String propertyName) {
        PrismProperty<Object> property = resource.findProperty(
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_LOCAL_NAME,
                        propertyName));
        Object value = null;
        if (property != null) {
            value = getRealValue(property);
        }
        return AbstractBaseDummyConnector.SUGGESTION_PREFIX + value;
    }

    @Test
    public void test022Configuration() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        when();
        resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        resourceBean = resource.asObjectable();

        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertResourceCacheMissesIncrement(0);
        assertResourceCacheHitsIncrement(1);

        PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertNotNull("No configuration container", configurationContainer);
        PrismContainerDefinition confContDef = configurationContainer.getDefinition();
        assertNotNull("No configuration container definition", confContDef);
        PrismContainer configurationPropertiesContainer = configurationContainer
                .findContainer(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
        assertNotNull("No configuration properties container", configurationPropertiesContainer);
        PrismContainerDefinition confPropsDef = configurationPropertiesContainer.getDefinition();
        assertNotNull("No configuration properties container definition", confPropsDef);
        Collection<PrismProperty<?>> configurationProperties = configurationPropertiesContainer.getValue().getItems();
        assertFalse("No configuration properties", configurationProperties.isEmpty());
        for (PrismProperty<?> confProp : configurationProperties) {
            PrismPropertyDefinition confPropDef = confProp.getDefinition();
            assertNotNull("No definition for configuration property " + confProp, confPropDef);
            assertFalse("Configuration property " + confProp + " is raw", confProp.isRaw());
            assertConfigurationProperty(confProp);
        }

        // The useless configuration variables should be reflected to the resource now
        assertEquals("Wrong useless string", "Shiver me timbers!", dummyResource.getUselessString());
        assertEquals("Wrong guarded useless string", "Dead men tell no tales", dummyResource.getUselessGuardedString());

        resource.checkConsistence();

        rememberSchemaMetadata(resource);

        assertSteadyResource();
        dummyResource.assertConnections(4);
        assertDummyConnectorInstances(1);
    }

    protected <T> void assertConfigurationProperty(PrismProperty<T> confProp) {
        // for use in subclasses
    }

    @Test
    public void test023ParsedSchema() throws Exception {
        expect("The returned type should have the schema pre-parsed");
        assertTrue(ResourceSchemaFactory.hasParsedSchema(resourceBean));

        // Also test if the utility method returns the same thing
        ResourceSchema returnedSchema = ResourceSchemaFactory.getRawSchema(resourceBean);

        displayDumpable("Parsed resource schema", returnedSchema);

        // Check whether it is reusing the existing schema and not parsing it
        // all over again
        // Not equals() but == ... we want to really know if exactly the same
        // object instance is returned
        assertTrue("Broken caching",
                returnedSchema == ResourceSchemaFactory.getRawSchema(resourceBean));

        assertSchemaSanity(returnedSchema, resourceBean);

        rememberResourceSchema(returnedSchema);
        assertSteadyResource();
        dummyResource.assertConnections(4);
        assertDummyConnectorInstances(1);
    }

    @Test
    public void test024RefinedSchema() throws Exception {
        // GIVEN

        // WHEN
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resourceBean);
        displayDumpable("Refined schema", refinedSchema);

        // Check whether it is reusing the existing schema and not parsing it
        // all over again
        // Not equals() but == ... we want to really know if exactly the same
        // object instance is returned
        assertTrue("Broken caching",
                refinedSchema == ResourceSchemaFactory.getCompleteSchema(resourceBean));

        ResourceObjectDefinition accountDef = refinedSchema.findDefaultDefinitionForKind(ShadowKindType.ACCOUNT);
        assertNotNull("Account definition is missing", accountDef);
        assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
        assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
        assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
        assertFalse("No nativeObjectClass in account",
                StringUtils.isEmpty(accountDef.getObjectClassDefinition().getNativeObjectClass()));

        ResourceObjectTypeDefinition accountTypeDef = accountDef.getTypeDefinition();
        assertEquals("Unexpected kind in account definition", ShadowKindType.ACCOUNT, accountTypeDef.getKind());
        assertTrue("Account definition in not default", accountTypeDef.isDefaultForKind());
        assertEquals("Wrong intent in account definition", SchemaConstants.INTENT_DEFAULT, accountTypeDef.getIntent());
        assertFalse("Account definition is deprecated", accountDef.isDeprecated());
        assertFalse("Account definition in auxiliary",
                accountDef.getObjectClassDefinition().isAuxiliary());

        ResourceAttributeDefinition<?> uidDef = accountDef.findAttributeDefinition(SchemaConstants.ICFS_UID);
        assertEquals(1, uidDef.getMaxOccurs());
        assertEquals(0, uidDef.getMinOccurs());
        assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
        assertFalse("UID has create", uidDef.canAdd());
        assertFalse("UID has update", uidDef.canModify());
        assertTrue("No UID read", uidDef.canRead());
        assertTrue("UID definition not in identifiers", accountDef.getPrimaryIdentifiers().contains(uidDef));

        ResourceAttributeDefinition<?> nameDef = accountDef.findAttributeDefinition(SchemaConstants.ICFS_NAME);
        assertEquals(1, nameDef.getMaxOccurs());
        assertEquals(1, nameDef.getMinOccurs());
        assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
        assertTrue("No NAME create", nameDef.canAdd());
        assertTrue("No NAME update", nameDef.canModify());
        assertTrue("No NAME read", nameDef.canRead());
        assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));
        // MID-3144
        assertEquals("Wrong NAME displayOrder", (Integer) 110, nameDef.getDisplayOrder());
        assertEquals("Wrong NAME displayName", "Username", nameDef.getDisplayName());

        ResourceAttributeDefinition<?> fullnameDef = accountDef.findAttributeDefinition("fullname");
        assertNotNull("No definition for fullname", fullnameDef);
        assertEquals(1, fullnameDef.getMaxOccurs());
        assertEquals(1, fullnameDef.getMinOccurs());
        assertTrue("No fullname create", fullnameDef.canAdd());
        assertTrue("No fullname update", fullnameDef.canModify());
        assertTrue("No fullname read", fullnameDef.canRead());
        // MID-3144
        if (fullnameDef.getDisplayOrder() == null || fullnameDef.getDisplayOrder() < 100 || fullnameDef.getDisplayOrder() > 400) {
            AssertJUnit.fail("Wrong fullname displayOrder: " + fullnameDef.getDisplayOrder());
        }
        assertEquals("Wrong fullname displayName", null, fullnameDef.getDisplayName());

        assertNull("The _PASSWORD_ attribute sneaked into schema",
                accountDef.findAttributeDefinition(new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

        rememberRefinedResourceSchema(refinedSchema);

        // FIXME re-enable this assert when there's a little time
//        assertEquals("Unexpected number of schema definitions",
//                getExpectedRefinedSchemaDefinitions(),
//                refinedSchema.getDefinitions().size());

        assertSteadyResource();
        dummyResource.assertConnections(4);
        assertDummyConnectorInstances(1);
    }

    /**
     * Make sure that the refined schema haven't destroyed cached resource schema.
     * Also make sure that the caching in object's user data works well.
     */
    @Test
    public void test025ParsedSchemaAgain() throws Exception {
        // GIVEN

        // THEN
        // The returned type should have the schema pre-parsed
        assertTrue(ResourceSchemaFactory.hasParsedSchema(resourceBean));

        // Also test if the utility method returns the same thing
        ResourceSchema returnedSchema = ResourceSchemaFactory.getRawSchema(resourceBean);

        displayDumpable("Parsed resource schema", returnedSchema);
        assertSchemaSanity(returnedSchema, resourceBean);

        assertResourceSchemaUnchanged(returnedSchema);
        assertSteadyResource();
    }

    @Test
    public void test028Capabilities() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        when();
        PrismObject<ResourceType> resource =
                provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        ResourceType resourceType = resource.asObjectable();

        then();
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess(result);

        // Check native capabilities
        CapabilityCollectionType nativeCapabilities = resourceType.getCapabilities().getNative();
        displayValue("Native capabilities", PrismTestUtil.serializeAnyDataWrapped(nativeCapabilities));
        display("Resource", resourceType);
        assertFalse("Empty capabilities returned", CapabilityUtil.isEmpty(nativeCapabilities));
        CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilities, CredentialsCapabilityType.class);
        assertThat(capCred).isNotNull();
        assertNativeCredentialsCapability(capCred);

        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilities, ActivationCapabilityType.class);
        if (supportsActivation()) {
            assertNotNull("native activation capability not present", capAct);
            assertNotNull("native activation status capability not present", capAct.getStatus());
        } else {
            assertNull("native activation capability sneaked in", capAct);
        }

        TestConnectionCapabilityType capTest = CapabilityUtil.getCapability(nativeCapabilities, TestConnectionCapabilityType.class);
        assertNotNull("native test capability not present", capTest);
        ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilities, ScriptCapabilityType.class);
        assertNotNull("native script capability not present", capScript);
        assertNotNull("No host in native script capability", capScript.getHost());
        assertFalse("No host in native script capability", capScript.getHost().isEmpty());
        // TODO: better look inside

        UpdateCapabilityType capUpdate = CapabilityUtil.getCapability(nativeCapabilities, UpdateCapabilityType.class);
        assertUpdateCapability(capUpdate);

        RunAsCapabilityType capRunAs = CapabilityUtil.getCapability(nativeCapabilities, RunAsCapabilityType.class);
        assertRunAsCapability(capRunAs);

        capabilitiesCachingMetadataType = resourceType.getCapabilities().getCachingMetadata();
        assertNotNull("No capabilities caching metadata", capabilitiesCachingMetadataType);
        assertNotNull("No capabilities caching metadata timestamp", capabilitiesCachingMetadataType.getRetrievalTimestamp());
        assertNotNull("No capabilities caching metadata serial number", capabilitiesCachingMetadataType.getSerialNumber());

        // Configured capabilities

        CapabilityCollectionType configuredCapabilities = resourceType.getCapabilities().getConfigured();
        if (configuredCapabilities == null) {
            assertCountConfiguredCapability(null);
        } else {
            CountObjectsCapabilityType capCount =
                    CapabilityUtil.getCapability(configuredCapabilities, CountObjectsCapabilityType.class);
            assertCountConfiguredCapability(capCount);
        }

        // Check effective capabilities
        capCred = ResourceTypeUtil.getEnabledCapability(resourceType, CredentialsCapabilityType.class);
        assertThat(capCred).isNotNull();
        assertNotNull("password capability not found", capCred.getPassword());

        if (supportsActivation()) {
            capAct = ResourceTypeUtil.getEnabledCapability(resourceType, ActivationCapabilityType.class);
            assertNotNull("activation capability not found", capAct);
        }

        dumpResourceCapabilities(resourceType);

        assertSteadyResource();
        dummyResource.assertConnections(4);
        assertDummyConnectorInstances(1);
    }

    protected void assertUpdateCapability(UpdateCapabilityType capUpdate) {
        assertNotNull("native update capability not present", capUpdate);
        assertNull("native update capability is manual", capUpdate.isManual());
        assertNotNull("delta in native update capability is null", capUpdate.isDelta());
        assertTrue("native update capability is NOT delta", capUpdate.isDelta());
        assertNotNull("addRemoveAttributeValues in native update capability is null", capUpdate.isAddRemoveAttributeValues());
        assertTrue("native update capability is NOT addRemoveAttributeValues", capUpdate.isAddRemoveAttributeValues());
    }

    protected void assertRunAsCapability(RunAsCapabilityType capRunAs) {
        assertNotNull("native runAs capability not present", capRunAs);
    }

    protected void assertCountConfiguredCapability(CountObjectsCapabilityType capCount) {
        CountObjectsSimulateType expectedCountSimulation = getCountSimulationMode();
        if (expectedCountSimulation == null) {
            assertNull("Unexpected configured count capability", capCount);
        } else {
            assertNotNull("configured count capability not present", capCount);
            CountObjectsSimulateType simulate = capCount.getSimulate();
            assertNotNull("simulate not present in configured count capability", simulate);
            assertEquals("Wrong simulate in configured count capability", getCountSimulationMode(), simulate);
        }
    }

    protected CountObjectsSimulateType getCountSimulationMode() {
        return CountObjectsSimulateType.PAGED_SEARCH_ESTIMATE;
    }

    protected void assertNativeCredentialsCapability(CredentialsCapabilityType capCred) {
        PasswordCapabilityType passwordCapabilityType = capCred.getPassword();
        assertNotNull("password native capability not present", passwordCapabilityType);
        Boolean readable = passwordCapabilityType.isReadable();
        if (readable != null) {
            assertFalse("Unexpected 'readable' in password capability", readable);
        }
    }

    /**
     * Check if the cached native capabilities were properly stored in the repo
     */
    @Test
    public void test029CapabilitiesRepo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        // WHEN
        PrismObject<ResourceType> resource =
                repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);

        // THEN
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess(result);

        // Check native capabilities
        ResourceType resourceType = resource.asObjectable();
        CapabilitiesType capabilitiesType = resourceType.getCapabilities();
        assertNotNull("No capabilities in repo, the capabilities were not cached", capabilitiesType);
        CapabilityCollectionType nativeCapabilities = capabilitiesType.getNative();
        System.out.println("Native capabilities: " + PrismTestUtil.serializeAnyDataWrapped(nativeCapabilities));
        System.out.println("resource: " + resourceType.asPrismObject().debugDump());
        assertFalse("Empty capabilities returned", CapabilityUtil.isEmpty(nativeCapabilities));
        CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilities, CredentialsCapabilityType.class);
        assertThat(capCred).isNotNull();
        assertNotNull("password native capability not present", capCred.getPassword());
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilities, ActivationCapabilityType.class);

        if (supportsActivation()) {
            assertNotNull("native activation capability not present", capAct);
            assertNotNull("native activation status capability not present", capAct.getStatus());
        } else {
            assertNull("native activation capability sneaked in", capAct);
        }

        TestConnectionCapabilityType capTest =
                CapabilityUtil.getCapability(nativeCapabilities, TestConnectionCapabilityType.class);
        assertNotNull("native test capability not present", capTest);
        ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilities, ScriptCapabilityType.class);
        assertNotNull("native script capability not present", capScript);
        assertNotNull("No host in native script capability", capScript.getHost());
        assertFalse("No host in native script capability", capScript.getHost().isEmpty());
        // TODO: better look inside

        CachingMetadataType repoCapabilitiesCachingMetadataType = capabilitiesType.getCachingMetadata();
        assertNotNull("No repo capabilities caching metadata", repoCapabilitiesCachingMetadataType);
        assertNotNull("No repo capabilities caching metadata timestamp", repoCapabilitiesCachingMetadataType.getRetrievalTimestamp());
        assertNotNull("No repo capabilities caching metadata serial number", repoCapabilitiesCachingMetadataType.getSerialNumber());
        assertEquals("Repo capabilities caching metadata timestamp does not match previously returned value",
                capabilitiesCachingMetadataType.getRetrievalTimestamp(), repoCapabilitiesCachingMetadataType.getRetrievalTimestamp());
        assertEquals("Repo capabilities caching metadata serial does not match previously returned value",
                capabilitiesCachingMetadataType.getSerialNumber(), repoCapabilitiesCachingMetadataType.getSerialNumber());

        assertSteadyResource();
        dummyResource.assertConnections(4);
        assertDummyConnectorInstances(1);
    }

    /**
     * Create steady state of the system by invoking test connection again.
     * Previous operations may have modified the resource, which may have changed
     * resource version which might have interfered with caching.
     */
    @Test
    public void test030ResourceAndConnectorCachingTestConnection() throws Exception {
        Task task = getTestTask();

        when();
        OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID, task, task.getResult());

        then();
        display("Test result", testResult);
        assertSuccess(testResult);

        // Connector is re-configured at this point. Test connection in previous test
        // have updated resource availability status, which have changed resource version
        // which have forced connector re-configuration. But this is quite harmless.
        // However, connector is not re-initialized. The same connector instance is reused.
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 1);
        // Test connection is forcing schema and capabilities fetch again. But the schema is not used.
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 1);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 1);

        rememberConnectorInstance(resource);

        assertSteadyResource();
        dummyResource.assertConnections(4);
        assertDummyConnectorInstances(1);
    }

    @Test
    public void test032ResourceAndConnectorCaching() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        ConnectorInstance configuredConnectorInstance =
                resourceManager.getConfiguredConnectorInstance(
                        resourceBean, ReadCapabilityType.class, false, result);
        assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchema(resource);
        assertNotNull("No resource schema", resourceSchema);

        // WHEN
        when();
        PrismObject<ResourceType> resourceAgain =
                provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        ResourceType resourceTypeAgain = resourceAgain.asObjectable();
        assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
        assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());

        PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        PrismContainer<Containerable> configurationContainerAgain =
                resourceAgain.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));

        // Check resource schema caching
        ResourceSchema resourceSchemaAgain = ResourceSchemaFactory.getRawSchema(resourceAgain);
        assertNotNull("No resource schema (again)", resourceSchemaAgain);
        assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);

        // Check capabilities caching

        CapabilitiesType capabilitiesType = resourceBean.getCapabilities();
        assertNotNull("No capabilities fetched from provisioning", capabilitiesType);
        CachingMetadataType capCachingMetadataType = capabilitiesType.getCachingMetadata();
        assertNotNull("No capabilities caching metadata fetched from provisioning", capCachingMetadataType);
        CachingMetadataType capCachingMetadataTypeAgain = resourceTypeAgain.getCapabilities().getCachingMetadata();
        assertEquals("Capabilities caching metadata serial number has changed", capCachingMetadataType.getSerialNumber(),
                capCachingMetadataTypeAgain.getSerialNumber());
        assertEquals("Capabilities caching metadata timestamp has changed", capCachingMetadataType.getRetrievalTimestamp(),
                capCachingMetadataTypeAgain.getRetrievalTimestamp());

        // Rough test if everything is fine
        resource.asObjectable().setFetchResult(null);
        resourceAgain.asObjectable().setFetchResult(null);
        ObjectDelta<ResourceType> dummyResourceDiff = DiffUtil.diff(resource, resourceAgain);
        displayDumpable("Dummy resource diff", dummyResourceDiff);
        assertTrue("The resource read again is not the same as the original. diff:" + dummyResourceDiff, dummyResourceDiff.isEmpty());

        // Now we stick our nose deep inside the provisioning impl. But we need
        // to make sure that the
        // configured connector is properly cached
        ConnectorInstance configuredConnectorInstanceAgain =
                resourceManager.getConfiguredConnectorInstance(
                        resourceAgain.asObjectable(), ReadCapabilityType.class, false, result);
        assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAgain);
        assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);

        // Check if the connector still works.
        OperationResult testResult = createOperationResult("test");
        configuredConnectorInstanceAgain.test(testResult);
        testResult.computeStatus();
        TestUtil.assertSuccess("Connector test failed", testResult);

        // Test connection should also refresh the connector by itself. So check if it has been refreshed
        ConnectorInstance configuredConnectorInstanceAfterTest =
                resourceManager.getConfiguredConnectorInstance(
                        resourceAgain.asObjectable(), ReadCapabilityType.class, false, result);
        assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAfterTest);
        assertTrue("Connector instance was not cached", configuredConnectorInstanceAgain == configuredConnectorInstanceAfterTest);

        assertSteadyResource();
    }

    @Test
    public void test034ResourceAndConnectorCachingForceFresh() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        ConnectorInstance configuredConnectorInstance =
                resourceManager.getConfiguredConnectorInstance(
                        resourceBean, ReadCapabilityType.class, false, result);
        assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchema(resource);
        assertNotNull("No resource schema", resourceSchema);

        // WHEN
        PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
                null, task, result);

        // THEN
        assertSuccess(result);

        ResourceType resourceTypeAgain = resourceAgain.asObjectable();
        assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
        assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());

        PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        PrismContainer<Containerable> configurationContainerAgain = resourceAgain
                .findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));

        ResourceSchema resourceSchemaAgain = ResourceSchemaFactory.getRawSchema(resourceAgain);
        assertNotNull("No resource schema (again)", resourceSchemaAgain);
        assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);

        // Now we stick our nose deep inside the provisioning impl. But we need
        // to make sure that the configured connector is properly refreshed
        // forceFresh = true
        ConnectorInstance configuredConnectorInstanceAgain =
                resourceManager.getConfiguredConnectorInstance(
                        resourceAgain.asObjectable(), ReadCapabilityType.class, true, result);
        assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAgain);
        // Connector instance should NOT be changed at this point. It should be only re-configured.
        assertTrue("Connector instance was changed", configuredConnectorInstance == configuredConnectorInstanceAgain);

        // Check if the connector still works
        OperationResult testResult = createOperationResult("test");
        configuredConnectorInstanceAgain.test(testResult);
        testResult.computeStatus();
        TestUtil.assertSuccess("Connector test failed", testResult);

        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 1);
        rememberConnectorInstance(configuredConnectorInstanceAgain);

        assertSteadyResource();
    }

    @Test
    public void test040ApplyDefinitionShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(getAccountWillFile());

        // WHEN
        provisioningService.applyDefinition(account, task, result);

        // THEN
        result.computeStatus();
        display("applyDefinition result", result);
        TestUtil.assertSuccess(result);

        account.checkConsistence(true, true);
        ShadowUtil.checkConsistence(account, getTestNameShort());
        TestUtil.assertSuccess("applyDefinition(account) result", result);

        assertSteadyResource();
    }

    @Test
    public void test041ApplyDefinitionAddShadowDelta() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(getAccountWillFile());

        ObjectDelta<ShadowType> delta = account.createAddDelta();

        // WHEN
        provisioningService.applyDefinition(delta, task, result);

        // THEN
        result.computeStatus();
        display("applyDefinition result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence(true, true, true);
        TestUtil.assertSuccess("applyDefinition(add delta) result", result);

        assertSteadyResource();
    }

    @Test
    public void test042ApplyDefinitionResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(getResourceDummyFile());
        // Transplant connector OID. The freshly-parsed resource does have only the fake one.
        resource.asObjectable().getConnectorRef().setOid(this.resourceBean.getConnectorRef().getOid());
        // Make sure this object has a different OID than the one already loaded. This avoids caching
        // and other side-effects
        resource.setOid(RESOURCE_DUMMY_NONEXISTENT_OID);

        // WHEN
        provisioningService.applyDefinition(resource, task, result);

        // THEN
        result.computeStatus();
        display("applyDefinition result", result);
        TestUtil.assertSuccess(result);

        resource.checkConsistence(true, true);
        TestUtil.assertSuccess("applyDefinition(resource) result", result);

        assertSteadyResource();
    }

    @Test
    public void test043ApplyDefinitionAddResourceDelta() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(getResourceDummyFile());
        // Transplant connector OID. The freshly-parsed resource does have only the fake one.
        resource.asObjectable().getConnectorRef().setOid(this.resourceBean.getConnectorRef().getOid());
        ObjectDelta<ResourceType> delta = resource.createAddDelta();
        // Make sure this object has a different OID than the one already loaded. This avoids caching
        // and other side-effects
        resource.setOid(RESOURCE_DUMMY_NONEXISTENT_OID);

        // WHEN
        provisioningService.applyDefinition(delta, task, result);

        // THEN
        result.computeStatus();
        display("applyDefinition result", result);
        TestUtil.assertSuccess(result);

        delta.checkConsistence(true, true, true);
        TestUtil.assertSuccess("applyDefinition(add delta) result", result);

        assertSteadyResource();
    }

    @Test
    public void test050SelfTest() {
        // GIVEN
        Task task = getTestTask();
        OperationResult testResult = task.getResult();

        // WHEN
        provisioningService.provisioningSelfTest(testResult, task);

        // THEN
        testResult.computeStatus();
        IntegrationTestTools.display(testResult);
        display("test result", testResult);
        // There may be warning about illegal key size on some platforms. As far as it is warning and not error we are OK
        // the system will fall back to a interoperable key size
        if (testResult.getStatus() != OperationResultStatus.SUCCESS && testResult.getStatus() != OperationResultStatus.WARNING) {
            AssertJUnit.fail("Self-test failed: " + testResult);
        }
    }

    // The account must exist to test this with modify delta. So we postpone the
    // test when the account actually exists

    @Test
    public void test080TestAttributesToReturn() throws Exception {
        // GIVEN
        Task task = taskManager.createTaskInstance();
        OperationResult result = task.getResult();

        ResourceShadowCoordinates coords = new ResourceShadowCoordinates(
                RESOURCE_DUMMY_OID, ShadowKindType.ENTITLEMENT, RESOURCE_DUMMY_INTENT_GROUP);
        ProvisioningContext ctx = provisioningContextFactory.createForShadowCoordinates(coords, task, result);

        // WHEN
        AttributesToReturn attributesToReturn = ctx.createAttributesToReturn();

        // THEN
        displayValue("attributesToReturn", attributesToReturn);
        assertFalse("wrong isReturnDefaultAttributes", attributesToReturn.isReturnDefaultAttributes());
        Collection<String> attrs = new ArrayList<>();
        for (ResourceAttributeDefinition<?> attributeToReturnDef : attributesToReturn.getAttributesToReturn()) {
            attrs.add(attributeToReturnDef.getItemName().getLocalPart());
        }
        // No "members" attribute here
        var expectedBasic = new ArrayList<>(Set.of("uid", "name", "description", "cc"));
        if (supportsMemberOf()) {
            expectedBasic.add("memberOf");
        }
        PrismAsserts.assertSets("Wrong attribute to return", attrs, expectedBasic.toArray(new String[0]));

        assertSteadyResource();
    }

    @Test
    public void test090ConnectorStatsAfterSomeUse() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        List<ConnectorOperationalStatus> operationalStatuses = provisioningService.getConnectorOperationalStatus(RESOURCE_DUMMY_OID, task, result);

        // THEN
        assertSuccess(result);

        display("Connector operational status", operationalStatuses);
        assertNotNull("null operational status", operationalStatuses);
        assertEquals("Unexpected size of operational status", 1, operationalStatuses.size());
        ConnectorOperationalStatus operationalStatus = operationalStatuses.get(0);

        assertEquals("Wrong connectorClassName", getDummyConnectorClass().getName(), operationalStatus.getConnectorClassName());
        assertThat(operationalStatus.getPoolConfigMinSize())
                .as("Wrong poolConfigMinSize")
                .isNull();
        assertEquals("Wrong poolConfigMaxSize", (Integer) 10, operationalStatus.getPoolConfigMaxSize());
        assertEquals("Wrong poolConfigMinIdle", (Integer) 1, operationalStatus.getPoolConfigMinIdle());
        assertEquals("Wrong poolConfigMaxIdle", (Integer) 10, operationalStatus.getPoolConfigMaxIdle());
        assertEquals("Wrong poolConfigWaitTimeout", (Long) 150000L, operationalStatus.getPoolConfigWaitTimeout());
        assertEquals("Wrong poolConfigMinEvictableIdleTime", (Long) 120000L, operationalStatus.getPoolConfigMinEvictableIdleTime());
        assertEquals("Wrong poolStatusNumIdle", (Integer) 1, operationalStatus.getPoolStatusNumIdle());
        assertEquals("Wrong poolStatusNumActive", (Integer) 0, operationalStatus.getPoolStatusNumActive());

        assertSteadyResource();
    }

    @Test
    public void test100AddAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> account = prismContext.parseObject(getAccountWillFile());
        account.checkConsistence();

        display("Adding shadow", account);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();

        assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

        account.checkConsistence();

        PrismObject<ShadowType> accountRepo = getShadowRepo(ACCOUNT_WILL_OID);
        // Added account is slightly different case. Even not-returned-by-default attributes are stored in the cache.
        checkRepoAccountShadowWill(accountRepo, start, end);

        willIcfUid = getIcfUid(accountRepo);
        displayValue("Will ICF UID", willIcfUid);
        assertNotNull("No will ICF UID", willIcfUid);

        ActivationType activationRepo = accountRepo.asObjectable().getActivation();
        if (supportsActivation()) {
            assertNotNull("No activation in " + accountRepo + " (repo)", activationRepo);
            assertEquals("Wrong activation enableTimestamp in " + accountRepo + " (repo)", ACCOUNT_WILL_ENABLE_TIMESTAMP, activationRepo.getEnableTimestamp());
        } else {
            assertNull("Activation sneaked in (repo)", activationRepo);
        }
        assertWillRepoShadowAfterCreate(accountRepo);

        syncServiceMock.assertSingleNotifySuccessOnly();

        PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        XMLGregorianCalendar tsAfterRead = clock.currentTimeXMLGregorianCalendar();

        display("Account provisioning", accountProvisioning);
        ShadowType accountTypeProvisioning = accountProvisioning.asObjectable();
        display("account from provisioning", accountTypeProvisioning);
        assertShadowName(accountProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, accountTypeProvisioning.getKind());
        assertAttribute(accountProvisioning, SchemaConstants.ICFS_NAME, transformNameFromResource(ACCOUNT_WILL_USERNAME));
        assertAttribute(accountProvisioning, getUidMatchingRule(), SchemaConstants.ICFS_UID, willIcfUid);

        ActivationType activationProvisioning = accountTypeProvisioning.getActivation();
        if (supportsActivation()) {
            assertNotNull("No activation in " + accountProvisioning + " (provisioning)", activationProvisioning);
            assertEquals("Wrong activation administrativeStatus in " + accountProvisioning + " (provisioning)",
                    ActivationStatusType.ENABLED, activationProvisioning.getAdministrativeStatus());
            TestUtil.assertEqualsTimestamp("Wrong activation enableTimestamp in " + accountProvisioning + " (provisioning)",
                    ACCOUNT_WILL_ENABLE_TIMESTAMP, activationProvisioning.getEnableTimestamp());
        } else {
            assertNull("Activation sneaked in (provisioning)", activationProvisioning);
        }

        assertNull("The _PASSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
                accountTypeProvisioning, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

        // Check if the account was created in the dummy resource

        DummyAccount dummyAccount = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        assertNotNull("No dummy account", dummyAccount);
        assertEquals("Username is wrong", transformNameFromResource(ACCOUNT_WILL_USERNAME), dummyAccount.getName());
        assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
        assertTrue("The account is not enabled", dummyAccount.isEnabled());
        assertEquals("Wrong password", ACCOUNT_WILL_PASSWORD, dummyAccount.getPassword());

        // Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
        PrismObject<ShadowType> shadowFromRepo = getShadowRepo(addedObjectOid);
        assertNotNull("Shadow was not created in the repository", shadowFromRepo);
        displayValue("Repository shadow", shadowFromRepo.debugDump());

        checkRepoAccountShadow(shadowFromRepo);

        checkRepoAccountShadowWill(shadowFromRepo, end, tsAfterRead);

        // MID-3860
        assertShadowPasswordMetadata(shadowFromRepo, true, start, end, null, null);
        assertRepoShadowCredentials(shadowFromRepo, ACCOUNT_WILL_PASSWORD);
        lastPasswordModifyStart = start;
        lastPasswordModifyEnd = end;

        checkUniqueness(accountProvisioning);
        assertSteadyResource();
    }

    protected void assertWillRepoShadowAfterCreate(PrismObject<ShadowType> repoShadow) {
        // for the subclasses
    }

    protected void checkRepoAccountShadowWillBasic(PrismObject<ShadowType> accountRepo,
            XMLGregorianCalendar start, XMLGregorianCalendar end, Integer expectedNumberOfAttributes) throws CommonException {
        display("Will account repo", accountRepo);
        ShadowType accountTypeRepo = accountRepo.asObjectable();
        assertShadowName(accountRepo, ACCOUNT_WILL_USERNAME);

        if (isIcfNameUidSame()) {
            assertPrimaryIdentifierValue(accountRepo, getWillRepoIcfName());
        } else {
            assertPrimaryIdentifierValue(accountRepo, getIcfUid(accountRepo));
        }

        assertEquals("Wrong kind (repo)", ShadowKindType.ACCOUNT, accountTypeRepo.getKind());
        assertAttribute(accountRepo, SchemaConstants.ICFS_NAME, getWillRepoIcfName());
        if (isIcfNameUidSame() && !isProposedShadow(accountRepo)) {
            assertAttribute(accountRepo, SchemaConstants.ICFS_UID, getWillRepoIcfName());
        }

        assertNumberOfAttributes(accountRepo, expectedNumberOfAttributes);

        assertRepoCachingMetadata(accountRepo, start, end);
    }

    protected void assertPrimaryIdentifierValue(PrismObject<ShadowType> shadow, String expected) {
        if (shadow.asObjectable().getLifecycleState() == null || shadow.asObjectable().getLifecycleState().equals(SchemaConstants.LIFECYCLE_ACTIVE)) {
            assertEquals("Wrong primaryIdentifierValue in " + shadow, expected, shadow.asObjectable().getPrimaryIdentifierValue());
        }
    }

    private boolean isProposedShadow(PrismObject<ShadowType> shadow) throws CommonException {
        provisioningService.determineShadowState(shadow, getTestTask(), getTestOperationResult());
        return shadow.asObjectable().getShadowLifecycleState() == ShadowLifecycleStateType.PROPOSED;
    }

    protected void checkRepoAccountShadowWill(
            PrismObject<ShadowType> accountRepo, XMLGregorianCalendar start, XMLGregorianCalendar end) throws CommonException {
        checkRepoAccountShadowWillBasic(accountRepo, start, end, 2);
        assertRepoShadowCacheActivation(accountRepo, null);
    }

    // test101 in the subclasses

    @Test
    public void test102GetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        PrismObject<ShadowType> shadow =
                provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        // THEN
        result.computeStatus();
        display("getObject result", result);
        TestUtil.assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        display("Retrieved account shadow", shadow);

        assertNotNull("No dummy account", shadow);

        checkAccountWill(shadow, result, startTs, endTs);
        PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_WILL_OID);
        checkRepoAccountShadowWill(shadowRepo, startTs, endTs);

        checkUniqueness(shadow);

        assertCachingMetadata(shadow, false, startTs, endTs);

        // MID-3860
        assertShadowPasswordMetadata(shadow, true, lastPasswordModifyStart, lastPasswordModifyEnd, null, null);

        assertSteadyResource();
    }

    @Test
    public void test103GetAccountWillNoFetch() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        GetOperationOptions rootOptions = new GetOperationOptions();
        rootOptions.setNoFetch(true);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOptions);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, options, task, result);

        // THEN
        display("getObject result", result);
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 0);

        display("Retrieved account shadow", shadow);

        assertNotNull("No dummy account", shadow);

        checkAccountShadow(shadow, result, false);
        // This is noFetch. Therefore the read should NOT update the caching timestamp
        checkRepoAccountShadowWill(shadow, null, startTs);

        checkUniqueness(shadow);

        assertSteadyResource();
    }

    @Test
    public void test105ApplyDefinitionModifyDelta() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectModificationType accountDeltaBean = PrismTestUtil.parseAtomicValue(MODIFY_WILL_FULLNAME_FILE,
                ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<ShadowType> accountDelta = DeltaConvertor.createObjectDelta(accountDeltaBean,
                ShadowType.class, prismContext);

        // WHEN
        provisioningService.applyDefinition(accountDelta, task, result);

        // THEN
        assertSuccess(result);

        accountDelta.checkConsistence(true, true, true);

        assertSteadyResource();
    }

    /**
     * Make a native modification to an account and read it again. Make sure that
     * fresh data are returned - even though caching may be in effect.
     * MID-3481
     */
    @Test
    public void test106GetModifiedAccountWill() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = createOperationResult();
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        DummyAccount accountWill = getDummyAccountAssert(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);
        accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
        accountWill.replaceAttributeValue(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
        accountWill.setEnabled(false);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        when();
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        then();
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.SHADOW_FETCH_OPERATION_COUNT, 1);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        display("Retrieved account shadow", shadow);

        assertNotNull("No dummy account", shadow);

        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertEquals("Unexpected number of attributes", 7, attributes.size());

        PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_WILL_OID);
        checkRepoAccountShadowWillBasic(shadowRepo, startTs, endTs, null);

        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Pirate");
        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Black Pearl");
        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "sword", "love");
        assertRepoShadowCachedAttributeValue(shadowRepo, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
        assertRepoShadowCacheActivation(shadowRepo, ActivationStatusType.DISABLED);
        assertRepoShadowCredentials(shadowRepo, ACCOUNT_WILL_PASSWORD);

        checkUniqueness(shadow);

        assertCachingMetadata(shadow, false, startTs, endTs);

        assertSteadyResource();
    }

    @Test
    public void test999Shutdown() {
        when();
        provisioningService.shutdown();

        then();
        dummyResource.assertNoConnections();
    }

    protected void checkAccountWill(PrismObject<ShadowType> shadow, OperationResult result,
            XMLGregorianCalendar startTs, XMLGregorianCalendar endTs)
            throws SchemaException, EncryptionException, ConfigurationException {
        checkAccountShadow(shadow, result, true);
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(shadow);
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Flying Dutchman");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Sword", "LOVE");
        assertAttribute(shadow, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME, 42);
        assertEquals("Unexpected number of attributes", 6, attributes.size());
    }

    /**
     * We do not know what the timestamp should be
     */
    protected void assertRepoCachingMetadata(PrismObject<ShadowType> shadowRepo, XMLGregorianCalendar start, XMLGregorianCalendar end) {
        assertNull("Unexpected caching metadata in " + shadowRepo, shadowRepo.asObjectable().getCachingMetadata());
    }

    protected void assertCachingMetadata(PrismObject<ShadowType> shadow, boolean expectedCached, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
        assertNull("Unexpected caching metadata in " + shadow, shadow.asObjectable().getCachingMetadata());
    }

    protected void checkAccountShadow(
            PrismObject<ShadowType> shadowType, OperationResult parentResult, boolean fullShadow)
            throws SchemaException, ConfigurationException {
        ObjectChecker<ShadowType> checker = createShadowChecker(fullShadow);
        ShadowUtil.checkConsistence(shadowType, parentResult.getOperation());
        IntegrationTestTools.checkAccountShadow(shadowType.asObjectable(), resourceBean,
                repositoryService, checker, getUidMatchingRule(), prismContext, parentResult);
    }

    protected ObjectChecker<ShadowType> createShadowChecker(final boolean fullShadow) {
        return (shadow) -> {
            String icfName = ShadowUtil.getSingleStringAttributeValue(shadow,
                    SchemaConstants.ICFS_NAME);
            assertNotNull("No ICF NAME", icfName);
            assertEquals("Wrong shadow name (" + shadow.getName() + ")", StringUtils.lowerCase(icfName), StringUtils.lowerCase(shadow.getName().getOrig()));
            assertNotNull("No kind in " + shadow, shadow.getKind());
            assertNotNull("No shadow lifecycle state in " + shadow, shadow.getShadowLifecycleState());

            if (shadow.getKind() == ShadowKindType.ACCOUNT) {
                if (fullShadow) {
                    assertNotNull(
                            "Missing fullname attribute",
                            ShadowUtil.getSingleStringAttributeValue(shadow,
                                    new QName(MidPointConstants.NS_RI, "fullname")));
                    if (supportsActivation()) {
                        assertNotNull("no activation", shadow.getActivation());
                        assertNotNull("no activation status", shadow.getActivation().getAdministrativeStatus());
                        assertEquals("not enabled", ActivationStatusType.ENABLED, shadow.getActivation().getAdministrativeStatus());
                    }
                }

                assertProvisioningAccountShadow(shadow.asPrismObject(), resourceBean, ResourceAttributeDefinition.class);
            }
        };
    }

    protected void assertRepoShadowCachedAttributeValue(PrismObject<ShadowType> shadowRepo, String attrName, Object... attrValues) {
        PrismAsserts.assertNoItem(shadowRepo, ItemPath.create(ShadowType.F_ATTRIBUTES, getAttrQName(attrName)));
    }

    protected void assertRepoShadowCacheActivation(PrismObject<ShadowType> shadowRepo, ActivationStatusType expectedAdministrativeStatus) {
        ActivationType activationType = shadowRepo.asObjectable().getActivation();
        if (activationType == null) {
            return;
        }
        ActivationStatusType administrativeStatus = activationType.getAdministrativeStatus();
        assertNull("Unexpected activation administrativeStatus in repo shadow " + shadowRepo + ": " + administrativeStatus, administrativeStatus);
    }

    protected void assertRepoShadowCredentials(PrismObject<ShadowType> shadowRepo, String expectedPassword) throws SchemaException, EncryptionException {
        CredentialsType credentials = shadowRepo.asObjectable().getCredentials();
        if (expectedPassword == null && credentials == null) {
            return;
        }
        assertNotNull("Missing credentials in repo shadow " + shadowRepo, credentials);
        PasswordType passwordType = credentials.getPassword();
        if (expectedPassword == null && passwordType == null) {
            return;
        }
        assertNotNull("Missing password credential in repo shadow " + shadowRepo, passwordType);
        // TODO: assert password meta-data
        assertRepoShadowPasswordValue(shadowRepo, passwordType, expectedPassword);
    }

    protected void assertRepoShadowPasswordValue(PrismObject<ShadowType> shadowRepo, PasswordType passwordType,
            String expectedPassword) throws SchemaException, EncryptionException {
        ProtectedStringType passwordValue = passwordType.getValue();
        assertNull("Unexpected password value in repo shadow " + shadowRepo, passwordValue);
    }

    protected ResourceAttributeDefinition<?> getAccountAttrDef(String name) throws SchemaException {
        return requireNonNull(
                getAccountObjectClassDefinition().findAttributeDefinition(name));
    }

    protected ResourceAttributeDefinition<?> getAccountAttrDef(QName name) throws SchemaException {
        return requireNonNull(
                getAccountObjectClassDefinition().findAttributeDefinition(name));
    }

    @NotNull
    private ResourceObjectClassDefinition getAccountObjectClassDefinition() throws SchemaException {
        ResourceSchema resourceSchema =
                requireNonNull(
                        ResourceSchemaFactory.getRawSchema(resource));
        return requireNonNull(
                resourceSchema.findObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS));
    }
}
