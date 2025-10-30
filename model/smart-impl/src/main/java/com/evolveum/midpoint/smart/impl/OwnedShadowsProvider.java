/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licenced under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.Collection;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.mappings.OwnedShadow;
import com.evolveum.midpoint.util.exception.*;

/**
 * Strategy interface for fetching owned shadows samples for mapping suggestion.
 */
public interface OwnedShadowsProvider {

    Collection<OwnedShadow> fetch(
            TypeOperationContext ctx,
            OperationContext.StateHolder state,
            OperationResult result,
            int maxExamples)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException;
}
