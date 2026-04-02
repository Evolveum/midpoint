/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.api.correlation.CorrelationDefinitionProvider;
import com.evolveum.midpoint.model.impl.correlation.ResourceCorrelationDefinitionProvider;
import com.evolveum.midpoint.model.impl.correlator.tasks.CorrelationDefinitionProviderFactory.ResourceWithObjectTypeId;
import com.evolveum.midpoint.model.impl.util.mock.RepositoryServiceMock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CorrelationDefinitionProviderFactoryTest {

    private static final ResourceWithObjectTypeId
            DUMMY_RESOURCE_OBJECT_ID = new ResourceWithObjectTypeId("111", ShadowKindType.ACCOUNT, "default");
    private final RepositoryService repositoryService;
    private final PrismContext prism;

    public CorrelationDefinitionProviderFactoryTest() throws SchemaException, IOException, SAXException {
        prism = new MidPointPrismContextFactory().createInitializedPrismContext();
        repositoryService = mockRepositoryService();
    }

    @Test
    void inlineCorrelatorIsDefined_providerIsCreated_providerShouldProvideInlinedCorrelator()
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        final CorrelationDefinitionType correlationDefinition = new CorrelationDefinitionType();
        final SimulatedCorrelatorsType correlatorsSpecification = new SimulatedCorrelatorsType().inlineCorrelators(
                correlationDefinition);

        final CorrelationDefinitionType resolvedDefinition = new CorrelationDefinitionProviderFactory(null)
                .providerFor(correlatorsSpecification, DUMMY_RESOURCE_OBJECT_ID, null).get();

        assertSame(resolvedDefinition, correlationDefinition);
    }

    @Test
    void inclusionOfExistingCorrelationIsEnabled_providerIsCreated_correctProviderImplementationShouldBeCreated()
            throws SchemaException, ObjectNotFoundException {
        final SimulatedCorrelatorsType correlatorsSpecification = new SimulatedCorrelatorsType()
                .includeExistingCorrelators(true);

        final CorrelationDefinitionProvider provider = new CorrelationDefinitionProviderFactory(this.repositoryService)
                .providerFor(correlatorsSpecification, DUMMY_RESOURCE_OBJECT_ID, null);

        assertTrue(provider instanceof ResourceCorrelationDefinitionProvider);
    }

    @Test
    void noSourceOfCorrelationIsDefined_providerCreationIsCalled_exceptionShouldBeThrown() {
        // In practice, at least one source needs to be specified in the specification, but here we create an empty one.
        final SimulatedCorrelatorsType correlatorsSpecification = new SimulatedCorrelatorsType();

        final CorrelationDefinitionProviderFactory providerFactory = new CorrelationDefinitionProviderFactory(
                this.repositoryService);

        assertThrows(IllegalArgumentException.class, () -> providerFactory.providerFor(correlatorsSpecification,
                DUMMY_RESOURCE_OBJECT_ID, null));
    }

    @Test
    void inlineCorrelatorIsDefinedAsWellAsInclusionFromResource_providerIsCreated_noExceptionShouldBeThrown()
            throws SchemaException, ObjectNotFoundException {
        final CorrelationDefinitionType correlationDefinition = new CorrelationDefinitionType();
        final SimulatedCorrelatorsType correlatorsSpecification = new SimulatedCorrelatorsType()
                .includeExistingCorrelators(true)
                .inlineCorrelators(correlationDefinition);

        final CorrelationDefinitionProvider provider = new CorrelationDefinitionProviderFactory(this.repositoryService)
                .providerFor(correlatorsSpecification, DUMMY_RESOURCE_OBJECT_ID, null);

        assertNotNull(provider);

    }

    private RepositoryService mockRepositoryService() {
        return new RepositoryServiceMock() {
            @SuppressWarnings("unchecked")
            @Override
            public @NotNull <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid,
                    Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
                try {
                    return (PrismObject<O>) prism.createObject(ResourceType.class);
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}