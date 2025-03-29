/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;

import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.impl.resources.ConnectorManager;
import com.evolveum.midpoint.provisioning.impl.resources.ResourceManager;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.schema.processor.BareResourceSchema;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SimpleObjectResolver;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.*;
import com.evolveum.midpoint.util.FailableProcessor;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.mock.SynchronizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class AbstractProvisioningIntegrationTest
        extends AbstractIntegrationTest {

    public static final File COMMON_DIR = ProvisioningTestUtil.COMMON_TEST_DIR_FILE;

    protected static final String CSV_CONNECTOR_TYPE = "com.evolveum.polygon.connector.csv.CsvConnector";

    @Autowired protected ProvisioningService provisioningService;
    @Autowired protected SynchronizationServiceMock syncServiceMock;

    @Autowired protected TaskManager taskManager;

    // Testing connector discovery
    @Autowired protected ConnectorManager connectorManager;

    // Used to make sure that the connector is cached
    @Autowired protected ResourceManager resourceManager;

    @Autowired protected MatchingRuleRegistry matchingRuleRegistry;

    @Autowired protected MockLiveSyncTaskHandler mockLiveSyncTaskHandler;
    @Autowired protected MockAsyncUpdateTaskHandler mockAsyncUpdateTaskHandler;

    @Autowired protected CommonBeans beans;

    // Values used to check if something is unchanged or changed properly
    private Long lastResourceVersion = null;
    private ConnectorInstance lastConfiguredConnectorInstance = null;
    private CachingMetadataType lastCachingMetadata;
    private BareResourceSchema lastBareResourceSchema = null;
    private CompleteResourceSchema lastCompleteResourceSchema;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        InternalsConfig.turnOnAllChecks();
        // We need to switch off the encryption checks. Some values cannot be encrypted as we do not have a definition.
        InternalsConfig.readEncryptionChecks = false;
        InternalsConfig.encryptionChecks = false;
        repositoryService.postInit(initResult); // initialize caches here
        provisioningService.postInit(initResult);
    }

    @SuppressWarnings("SameParameterValue")
    protected <T extends ObjectType> void assertVersion(PrismObject<T> object, String expectedVersion) {
        assertEquals("Wrong version of " + object, expectedVersion, object.asObjectable().getVersion());
    }

    protected void rememberResourceVersion(String version) {
        lastResourceVersion = parseVersion(version);
    }

    protected void assertResourceVersionIncrement(PrismObject<ResourceType> resource, int expectedIncrement) {
        assertResourceVersionIncrement(resource.getVersion(), expectedIncrement);
    }

    protected void assertResourceVersionIncrement(String currentVersion, int expectedIncrement) {
        long currentVersionLong = parseVersion(currentVersion);
        long actualIncrement = currentVersionLong - lastResourceVersion;
        assertEquals("Unexpected increment in resource version", expectedIncrement, actualIncrement);
        lastResourceVersion = currentVersionLong;
    }

    private long parseVersion(String stringVersion) {
        if (stringVersion == null) {
            AssertJUnit.fail("Version is null");
        }
        if (stringVersion.isEmpty()) {
            AssertJUnit.fail("Version is empty");
        }
        return Long.parseLong(stringVersion);
    }

    private CachingMetadataType getSchemaCachingMetadata(PrismObject<ResourceType> resource) {
        ResourceType resourceType = resource.asObjectable();
        XmlSchemaType xmlSchemaType = resourceType.getSchema();
        assertNotNull("No schema", xmlSchemaType);
        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchemaElement(resourceType);
        assertNotNull("No schema XSD element", resourceXsdSchemaElementAfter);
        return xmlSchemaType.getCachingMetadata();
    }

    protected void rememberSchemaMetadata(PrismObject<ResourceType> resource) {
        lastCachingMetadata = getSchemaCachingMetadata(resource);
    }

    protected void assertSchemaMetadataUnchanged(PrismObject<ResourceType> resource) {
        CachingMetadataType current = getSchemaCachingMetadata(resource);
        assertEquals("Schema caching metadata changed", lastCachingMetadata, current);
    }

    protected void rememberBareResourceSchema(BareResourceSchema resourceSchema) {
        lastBareResourceSchema = resourceSchema;
    }

    protected void rememberCompleteResourceSchema(CompleteResourceSchema resourceSchema) {
        lastCompleteResourceSchema = resourceSchema;
    }

    protected void assertNativeSchemaCached(BareResourceSchema currentResourceSchema) {
        // We really want == there. We want to make sure that this is actually the same instance and that it was properly cached.
        // But we do this for native schemas, as bare schema is just a wrapper that is not cached.
        assertSame("Resource schema has changed",
                lastBareResourceSchema.getNativeSchema(),
                currentResourceSchema.getNativeSchema());
    }

    protected void assertNativeSchemaCached(BareResourceSchema previousSchema, BareResourceSchema currentSchema) {
        // Check whether it is reusing the existing schema and not parsing it all over again
        // Not equals() but == ... we want to really know if exactly the same object instance is returned.
        assertSame("Broken caching", previousSchema.getNativeSchema(), currentSchema.getNativeSchema());
    }

    protected void assertCompleteSchemaCached(CompleteResourceSchema previousSchema, CompleteResourceSchema currentSchema) {
        // Check whether it is reusing the existing schema and not parsing it all over again
        // Not equals() but == ... we want to really know if exactly the same object instance is returned.
        assertSame("Broken caching", previousSchema, currentSchema);
    }

    protected void rememberRefinedResourceSchema(CompleteResourceSchema completeSchema) {
        lastCompleteResourceSchema = completeSchema;
    }

    protected void assertRefinedResourceSchemaUnchanged(CompleteResourceSchema currentCompleteSchema) {
        // We really want == (identity test) here.
        // We want to make sure that this is actually the same instance and that it was properly cached.
        assertSame("Refined resource schema has changed", lastCompleteResourceSchema, currentCompleteSchema);
    }

    protected void assertHasSchema(PrismObject<ResourceType> resource, String desc) throws SchemaException {
        ResourceType resourceType = resource.asObjectable();
        display("Resource " + desc, resourceType);

        XmlSchemaType xmlSchemaTypeAfter = resourceType.getSchema();
        assertNotNull("No schema in " + desc, xmlSchemaTypeAfter);
        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchemaElement(resourceType);
        assertNotNull("No schema XSD element in " + desc, resourceXsdSchemaElementAfter);

        CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
        assertNotNull("No caching metadata in " + desc, cachingMetadata);
        assertNotNull("No retrievalTimestamp in " + desc, cachingMetadata.getRetrievalTimestamp());
        assertNotNull("No serialNumber in " + desc, cachingMetadata.getSerialNumber());

        Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
        ResourceSchema parsedSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(xsdElement);
        assertNotNull("No schema after parsing in " + desc, parsedSchema);
    }

    protected void rememberConnectorInstance(PrismObject<ResourceType> resource) throws ConfigurationException {
        rememberConnectorInstance(
                resourceManager.getConfiguredConnectorInstanceFromCache(resource.asObjectable(), ReadCapabilityType.class));
    }

    protected void rememberConnectorInstance(ConnectorInstance currentConnectorInstance) {
        logger.debug("Remembering connector instance {}", currentConnectorInstance);
        lastConfiguredConnectorInstance = currentConnectorInstance;
    }

    protected void assertConnectorInstanceUnchanged(PrismObject<ResourceType> resource)
            throws ConfigurationException {
        if (lastConfiguredConnectorInstance == null) {
            return;
        }
        ConnectorInstance currentConfiguredConnectorInstance =
                resourceManager.getConfiguredConnectorInstanceFromCache(resource.asObjectable(), ReadCapabilityType.class);
        assertSame("Connector instance has changed", lastConfiguredConnectorInstance, currentConfiguredConnectorInstance);
    }

    protected void assertSteadyResource(PrismObject<ResourceType> resource)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        if (resource != null) {
            String version = repositoryService.getVersion(ResourceType.class, resource.getOid(), getTestOperationResult());
            assertResourceVersionIncrement(version, 0);
            assertSchemaMetadataUnchanged(resource);
            assertConnectorInstanceUnchanged(resource);
        }

        displayDumpable("Resource cache", InternalMonitor.getResourceCacheStats());
        // We do not assert hits, there may be a lot of them
        assertResourceCacheMissesIncrement(0);
    }

    protected void assertSteadyResource() throws SchemaException, ConfigurationException, ObjectNotFoundException {
        assertSteadyResource(getResource());
    }

    protected PrismObject<ResourceType> getResource() {
        return null;
    }

    protected ShadowAsserter<Void> assertShadowProvisioning(String oid) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = createSubresult("assertShadowProvisioning");
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, oid, null, getTestTask(), result);
        assertSuccess(result);
        ShadowAsserter<Void> asserter = ShadowAsserter.forShadow(shadow, "provisioning");
        asserter.display();
        return asserter;
    }

    protected ShadowAsserter<Void> assertShadowNoFetch(String oid)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = createSubresult("assertShadowNoFetch-" + oid);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, oid, options, getTestTask(), result);
        assertSuccess(result);
        ShadowAsserter<Void> asserter = ShadowAsserter.forShadow(shadow, "noFetch");
        asserter.display();
        return asserter;
    }

    protected ShadowAsserter<Void> assertShadowFuture(String oid)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = createSubresult("assertShadowFuture-" + oid);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, oid, options, getTestTask(), result);
        assertSuccess(result);
        ShadowAsserter<Void> asserter = ShadowAsserter.forShadow(shadow, "future");
        asserter.display();
        return asserter;
    }

    protected ShadowAsserter<Void> assertShadowFutureNoFetch(String oid) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        GetOperationOptions rootOptions = GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE);
        rootOptions.setNoFetch(true);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(rootOptions);
        PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, oid, options, task, result);
        assertSuccess(result);
        ShadowAsserter<Void> asserter = ShadowAsserter.forShadow(shadow, "future,noFetch");
        asserter.display();
        return asserter;
    }

    @SuppressWarnings({ "SameParameterValue", "UnusedReturnValue" })
    protected OperationResult testResourceAssertSuccess(String resourceOid, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        OperationResult testResult = provisioningService.testResource(resourceOid, task, result);
        assertSuccess(testResult);
        return testResult;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected OperationResult testResourceAssertSuccess(DummyTestResource resource, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        OperationResult testResult = provisioningService.testResource(resource.oid, task, result);
        assertSuccess(testResult);
        resource.reload(result);
        return testResult;
    }

    /** This method is limited to single-connector resources. */
    protected DummyResourceContoller initDummyResource(
            String name, File resourceFile, String resourceOid,
            FailableProcessor<DummyResourceContoller> controllerInitLambda, OperationResult result) throws Exception {
        return initDummyResourceInternal(name, resourceFile, null, resourceOid, controllerInitLambda, result);
    }

    /** This method is limited to single-connector resources. */
    protected DummyResourceContoller initDummyResource(DummyTestResource testResource, OperationResult result) throws Exception {
        DummyResourceContoller controller =
                initDummyResourceInternal(
                        testResource.name,
                        null,
                        testResource,
                        testResource.oid,
                        testResource.controllerInitLambda,
                        result);
        testResource.controller = controller;
        return controller;
    }

    /** This method is limited to single-connector resources. TODO fix the hack "file vs testResource". */
    private DummyResourceContoller initDummyResourceInternal(
            String name, File resourceFile, TestObject<ResourceType> testResource, String resourceOid,
            FailableProcessor<DummyResourceContoller> controllerInitLambda, OperationResult result) throws Exception {
        DummyResourceContoller controller = DummyResourceContoller.create(name);
        if (controllerInitLambda != null) {
            controllerInitLambda.process(controller);
        } else {
            controller.populateWithDefaultSchema();
        }
        if (resourceFile != null) {
            addResourceFromFile(resourceFile, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, result);
        } else if (testResource != null) {
            addResource(testResource, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, result);
        }
        if (resourceOid != null) {
            PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, resourceOid, null, result);
            controller.setResource(resource);
        }
        return controller;
    }

    /** This method is limited to single-connector resources. */
    @Override
    public void initAndTestDummyResource(DummyTestResource resource, Task task, OperationResult result) throws Exception {
        initDummyResource(resource, result);
        assertSuccess(
                provisioningService.testResource(resource.controller.getResource().getOid(), task, result));
        resource.reload(result); // To have schema, etc
    }

    /** This method is limited to single-connector resources. */
    @Override
    public DummyResourceContoller initDummyResource(DummyTestResource resource, Task task, OperationResult result)
            throws Exception {
        initDummyResource(resource, result);
        resource.reload(result); // To have schema, etc
        return resource.controller;
    }

    @Override
    public OperationResult testResource(@NotNull String oid, @NotNull Task task, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        return provisioningService.testResource(oid, task, result);
    }

    /**
     * Returns repository-level resolver; but - in the future - we might use the {@link ProvisioningService} itself to reload
     * the resource objects in order to process them via {@link ResourceManager}.
     */
    @Override
    public SimpleObjectResolver getResourceReloader() {
        return RepoSimpleObjectResolver.get();
    }

    protected void checkShadowConsistence(@NotNull PrismObject<ShadowType> shadow) {
        shadow.checkConsistence(true, true);
        ShadowUtil.checkConsistence(shadow, getTestNameShort());
    }

    protected ShadowAsserter<Void> assertProvisioningShadowNew(@NotNull String oid)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var shadow = provisioningService
                .getObject(ShadowType.class, oid, null, getTestTask(), getTestOperationResult())
                .asObjectable();
        return assertShadowNew(shadow);
    }

    protected void markShadow(String shadowOid, String markOid, Task task, OperationResult result) throws CommonException {
        provisioningService.modifyObject(
                ShadowType.class,
                shadowOid,
                deltaFor(ShadowType.class)
                        .item(ShadowType.F_POLICY_STATEMENT)
                        .add(markFor(markOid))
                        .asItemDeltas(),
                null, null, task, result);
    }

    /** Requires the mark to be set by {@link #markShadow(String, String, Task, OperationResult)} method. */
    protected void unmarkShadow(String shadowOid, String markOid, Task task, OperationResult result) throws CommonException {
        provisioningService.modifyObject(
                ShadowType.class,
                shadowOid,
                deltaFor(ShadowType.class)
                        .item(ShadowType.F_POLICY_STATEMENT)
                        .delete(markFor(markOid))
                        .asItemDeltas(),
                null, null, task, result);
    }

    private static PolicyStatementType markFor(String markOid) {
        return new PolicyStatementType()
                .markRef(markOid, MarkType.COMPLEX_TYPE)
                .type(PolicyStatementTypeType.APPLY);
    }

    /** Quick hack to obtain type-qualified performance information. */
    protected void initRepoPerformanceMonitor() {
        var perfMon = repositoryService.getPerformanceMonitor();
        perfMon.setConfiguration(
                new RepositoryStatisticsReportingConfigurationType()
                        .classification(RepositoryStatisticsClassificationType.PER_OPERATION_AND_OBJECT_TYPE));
        perfMon.clearGlobalPerformanceInformation();
    }

    protected PerformanceInformation getRepoPerformanceInformation() {
        return repositoryService.getPerformanceMonitor().getGlobalPerformanceInformation();
    }

    protected int getShadowGetOperationsCount() {
        return repositoryService.getPerformanceMonitor()
                .getGlobalPerformanceInformation()
                .getInvocationCount(
                        isNativeRepository() ?
                                "SqaleRepositoryService.getObject.ShadowType" :
                                "getObject.ShadowType");
    }

    protected int getShadowSearchOperationsCount() {
        return repositoryService.getPerformanceMonitor()
                .getGlobalPerformanceInformation()
                .getInvocationCount(
                        isNativeRepository() ?
                                "SqaleRepositoryService.searchObjects.ShadowType" :
                                "searchObjects.ShadowType");
    }
}
