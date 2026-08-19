/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.mappings.ShadowsWithOwnerSampleResult;
import com.evolveum.midpoint.util.exception.*;

/**
 * Strategy interface for fetching owned shadows samples for mapping suggestion.
 */
interface ShadowsWithOwnersProvider {

    ShadowsWithOwnerSampleResult fetch(
            TypeOperationContext ctx,
            OperationContext.StateHolder state,
            OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException, SubscriptionComplianceException;
}
