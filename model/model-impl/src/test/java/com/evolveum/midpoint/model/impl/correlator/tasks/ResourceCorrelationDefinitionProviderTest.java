package com.evolveum.midpoint.model.impl.correlator.tasks;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractEmptyInternalModelTest;
import com.evolveum.midpoint.model.impl.correlator.tasks.CorrelationDefinitionProvider.ResourceWithObjectTypeId;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = "classpath:ctx-model-test-main.xml")
public class ResourceCorrelationDefinitionProviderTest extends AbstractEmptyInternalModelTest {
    private static final File RESOURCES_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR,
            "correlator/correlation/merge");

    private static final DummyTestResource CORRELATION_IN_SCHEMA_HANDLING = new DummyTestResource(RESOURCES_DIR,
            "resource-dummy-correlation-in-schema-handling.xml", "91b45a33-e180-4988-9b27-14c28b545bdd",
            "resource-with-correlation-in-schemaHandling");
    private static final DummyTestResource CORRELATION_IN_ATTRIBUTE = new DummyTestResource(RESOURCES_DIR,
            "resource-dummy-correlation-in-attribute.xml", "91b45a33-e180-4988-9b27-14c28b545bde",
            "resource-with-correlation-in-attribute");
    private static final DummyTestResource CORRELATION_IN_SYNCHRONIZATION = new DummyTestResource(RESOURCES_DIR,
            "resource-dummy-correlation-in-synchronization.xml", "91b45a33-e180-4988-9b27-14c28b545bdf",
            "resource-with-correlation-in-synchronization");

    @Test
    public void correlationIsDefinedInSchemaHandling_readDefinitionFromResource_correlationDefinitionShouldBeReturned()
            throws Exception {
        given("Repository contains resource with defined correlation.");
        initDummyResource(CORRELATION_IN_SCHEMA_HANDLING, getTestTask(), getTestOperationResult());
        final ResourceType resource = CORRELATION_IN_SCHEMA_HANDLING.getObjectable();
        final ResourceObjectTypeDefinitionType objectType = extractFirstObjectType(resource);
        final ResourceWithObjectTypeId resourceWithObjectTypeId = extractResourceAndObjectTypeIds(resource, objectType);

        when("Resolver resolves correlation definition from given resource");
        final ResourceCorrelationDefinitionProvider correlationProvider = new ResourceCorrelationDefinitionProvider(
                this.repositoryService, resourceWithObjectTypeId);
        final CorrelationDefinitionType resolvedCorrelationDefinition = correlationProvider.get(
                getTestOperationResult());

        then("Resolved definition should match the definition in resource.");
        assertEquals(resolvedCorrelationDefinition, objectType.getCorrelation());
    }

    @Test
    public void correlationIsDefinedOnAttributeLevel_readDefinitionFromResource_correlationDefinitionShouldBeReturned()
            throws Exception {
        given("Repository contains resource with defined correlation on attribute level.");
        initDummyResource(CORRELATION_IN_ATTRIBUTE, getTestTask(), getTestOperationResult());
        final ResourceType resource = CORRELATION_IN_ATTRIBUTE.getObjectable();
        final ResourceObjectTypeDefinitionType objectType = extractFirstObjectType(resource);
        final ResourceWithObjectTypeId resourceWithObjectTypeId = extractResourceAndObjectTypeIds(resource, objectType);

        when("Resolver resolves correlation definition from given resource");
        final ResourceCorrelationDefinitionProvider correlationProvider = new ResourceCorrelationDefinitionProvider(
                this.repositoryService, resourceWithObjectTypeId);
        final CorrelationDefinitionType resolvedCorrelationDefinition = correlationProvider.get(
                getTestOperationResult());

        then("Resolved definition should match the definition in resource.");
        final CorrelationDefinitionType mergedCorrelations = expectedCorrelationDefinition(resource, objectType);
        assertEquals(resolvedCorrelationDefinition, mergedCorrelations);
    }

    @Test
    public void correlationIsDefinedInSynchronization_readDefinitionFromResource_correlationDefinitionShouldBeReturned()
            throws Exception {
        given("Repository contains resource with defined legacy correlation in synchronization.");
        initDummyResource(CORRELATION_IN_SYNCHRONIZATION, getTestTask(), getTestOperationResult());
        final ResourceType resource = CORRELATION_IN_SYNCHRONIZATION.getObjectable();
        final ResourceObjectTypeDefinitionType objectType = extractFirstObjectType(resource);
        final ResourceWithObjectTypeId resourceWithObjectTypeId = extractResourceAndObjectTypeIds(resource, objectType);

        when("Resolver resolves correlation definition from given resource");
        final ResourceCorrelationDefinitionProvider correlationProvider = new ResourceCorrelationDefinitionProvider(
                this.repositoryService, resourceWithObjectTypeId);
        final CorrelationDefinitionType resolvedCorrelationDefinition = correlationProvider.get(
                getTestOperationResult());

        then("Resolved definition should match the definition in resource.");
        final CorrelationDefinitionType mergedCorrelations = expectedCorrelationDefinition(resource, objectType);
        assertEquals(resolvedCorrelationDefinition, mergedCorrelations);
    }

    private static CorrelationDefinitionType expectedCorrelationDefinition(ResourceType resource,
            ResourceObjectTypeDefinitionType objectType) throws ConfigurationException, SchemaException {
        final ResourceObjectTypeDefinition objectTypeDefinition = Resource.of(resource)
                .getCompleteSchemaRequired()
                .getObjectTypeDefinitionRequired(
                        ResourceObjectTypeIdentification.of(objectType.getKind(), objectType.getIntent()));

        final ObjectSynchronizationType objectSynchronization = getObjectSynchronization(resource);
        return CorrelatorsDefinitionUtil.mergeCorrelationDefinition(objectTypeDefinition, objectSynchronization,
                resource);
    }

    @Nullable
    private static ObjectSynchronizationType getObjectSynchronization(ResourceType resource) {
        return Optional.ofNullable(resource.getSynchronization())
                .map(SynchronizationType::getObjectSynchronization)
                .map(synchronizations -> synchronizations.get(0))
                .orElse(null);
    }

    private static ResourceObjectTypeDefinitionType extractFirstObjectType(ResourceType resource) {
        return resource.getSchemaHandling().getObjectType().get(0);
    }

    private static @NotNull ResourceWithObjectTypeId extractResourceAndObjectTypeIds(ResourceType resource,
            ResourceObjectTypeDefinitionType objectType) {
        return new ResourceWithObjectTypeId(resource.getOid(), objectType.getKind(), objectType.getIntent());
    }

}
