/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import java.util.Collection;
import java.util.function.Predicate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SubscriptionComplianceException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Interface for sampling shadow objects from a resource.
 * Different implementations provide different sampling strategies based on configuration.
 *
 * @param <R> The result type returned by this sampler
 */
public interface ObjectsSampler<R> {

    /**
     * Get options for shadow iteration with noFetch and readOnly flags.
     * Used to efficiently iterate through shadows in repository without fetching data from resource.
     */
    Collection<SelectorOptions<GetOperationOptions>> NO_FETCH_READ_ONLY_OPTIONS = createNoFetchReadOnlyOptions();

    /**
     * Creates get options for shadow iteration with noFetch and readOnly flags.
     */
    static Collection<SelectorOptions<GetOperationOptions>> createNoFetchReadOnlyOptions() {
        GetOperationOptions options = new GetOperationOptions();
        options.setNoFetch(true);
        options.setReadOnly(true);
        return SelectorOptions.createCollection(options);
    }

    /**
     * Samples shadow objects for the given resource and type definition.
     * This is a default implementation that calls the predicate version with a predicate that always returns true.
     */
    default R sample(
            ResourceType resource,
            ResourceObjectDefinition typeDefinition,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {
        return sample(resource, typeDefinition, shadow -> true, task, result);
    }

    /**
     * Samples shadow objects for the given resource and type definition, filtering them using the provided
     * acceptance predicate.
     */
    R sample(
            ResourceType resource,
            ResourceObjectDefinition typeDefinition,
            Predicate<PrismObject<ShadowType>> acceptancePredicate,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException;
}
