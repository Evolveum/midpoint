/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.mappings.OwnedShadow;
import com.evolveum.midpoint.util.exception.*;

/**
 * Strategy interface for fetching owned shadows samples for mapping suggestion.
 */
public interface OwnedShadowsProvider {

    List<OwnedShadow> fetch(
            TypeOperationContext ctx,
            OperationContext.StateHolder state,
            OperationResult result,
            int maxExamples)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException;
}
