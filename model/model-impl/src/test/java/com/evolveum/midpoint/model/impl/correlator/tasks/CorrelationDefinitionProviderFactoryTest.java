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

import org.testng.annotations.Test;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CorrelationDefinitionProviderFactoryTest {

    @Test
    void inlineCorrelatorIsDefined_providerIsCreated_providerShouldProvideInlinedCorrelator()
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        final CorrelationDefinitionType correlationDefinition = new CorrelationDefinitionType();
        final CorrelatorsDefinitionType correlatorsSpecification = new CorrelatorsDefinitionType().inlineCorrelators(
                correlationDefinition);

        final CorrelationDefinitionType resolvedDefinition = new CorrelationDefinitionProviderFactory(null)
                .providerFor(correlatorsSpecification, null).get(null);

        assertSame(resolvedDefinition, correlationDefinition);
    }

    @Test
    void inclusionOfExistingCorrelationIsEnabled_providerIsCreated_correctProviderImplementationShouldBeCreated() {
        final CorrelatorsDefinitionType correlatorsSpecification = new CorrelatorsDefinitionType()
                .includeExistingCorrelators(true);

        final CorrelationDefinitionProvider provider = new CorrelationDefinitionProviderFactory(null)
                .providerFor(correlatorsSpecification, null);

        assertTrue(provider instanceof ResourceCorrelationDefinitionProvider);
    }

    @Test
    void noSourceOfCorrelationIsDefined_providerCreationIsCalled_exceptionShouldBeThrown() {
        // In practice, at least one source needs to be specified in the specification, but here we create an empty one.
        final CorrelatorsDefinitionType correlatorsSpecification = new CorrelatorsDefinitionType();

        final CorrelationDefinitionProviderFactory providerFactory = new CorrelationDefinitionProviderFactory(null);

        assertThrows(IllegalArgumentException.class, () -> providerFactory.providerFor(correlatorsSpecification, null));
    }

    @Test
    void inlineCorrelatorIsDefinedAsWellAsInclusionFromResource_providerIsCreated_noExceptionShouldBeThrown() {
        final CorrelationDefinitionType correlationDefinition = new CorrelationDefinitionType();
        final CorrelatorsDefinitionType correlatorsSpecification = new CorrelatorsDefinitionType()
                .includeExistingCorrelators(true)
                .inlineCorrelators(correlationDefinition);

        final CorrelationDefinitionProvider provider = new CorrelationDefinitionProviderFactory(null)
                .providerFor(correlatorsSpecification, null);

        assertNotNull(provider);

    }
}