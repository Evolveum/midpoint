/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SubscriptionComplianceException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Sampler for correlation operations.
 * Uses larger sample sizes when caching is enabled to improve correlation accuracy.
 */
@Component
public class CorrelationObjectsSampler implements ObjectsSampler {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationObjectsSampler.class);

    private static final int DEFAULT_SAMPLE_SIZE = 2000;
    private static final int CACHED_SAMPLE_SIZE = 5000;

    private final ModelService modelService;

    public CorrelationObjectsSampler(ModelService modelService) {
        this.modelService = modelService;
    }

    @Override
    public List<PrismObject<ShadowType>> sample(
            ResourceType resource,
            ResourceObjectDefinition typeDefinition,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {

        boolean useNoFetch = typeDefinition.isCachingEnabled();
        int sampleSize = useNoFetch ? CACHED_SAMPLE_SIZE : DEFAULT_SAMPLE_SIZE;

        LOGGER.debug("Sampling shadows for correlation: {}/{}, sampleSize={}, cached={}",
                resource.getOid(), typeDefinition.getTypeIdentification(), sampleSize, useNoFetch);

        List<PrismObject<ShadowType>> reservoir = new ArrayList<>(sampleSize);
        AtomicInteger count = new AtomicInteger(0);
        Random random = new Random(1);

        modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(typeDefinition.getTypeIdentification())
                        .build(),
                (shadow, lResult) -> {
                    try {
                        int i = count.getAndIncrement();
                        if (i < sampleSize) {
                            reservoir.add(shadow);
                        } else {
                            int j = random.nextInt(i + 1);
                            if (j < sampleSize) {
                                reservoir.set(j, shadow);
                            }
                        }
                        return true;
                    } finally {
                        lResult.computeStatusIfUnknown();
                        lResult.setSummarizeSuccesses(true);
                        lResult.summarize();
                    }
                },
                createGetOptions(useNoFetch),
                task,
                result);

        LOGGER.debug("Sampled {} shadows for correlation", reservoir.size());
        return reservoir;
    }

    @Override
    public List<PrismObject<ShadowType>> sample(
            ResourceType resource,
            ResourceObjectDefinition typeDefinition,
            Predicate<PrismObject<ShadowType>> acceptancePredicate,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {

        boolean useNoFetch = typeDefinition.isCachingEnabled();
        int sampleSize = useNoFetch ? CACHED_SAMPLE_SIZE : DEFAULT_SAMPLE_SIZE;

        LOGGER.debug("Sampling shadows for correlation with predicate: {}/{}, sampleSize={}, cached={}",
                resource.getOid(), typeDefinition.getTypeIdentification(), sampleSize, useNoFetch);

        List<PrismObject<ShadowType>> reservoir = new ArrayList<>(sampleSize);
        AtomicInteger totalCount = new AtomicInteger(0);
        Random random = new Random(1);

        modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(typeDefinition.getTypeIdentification())
                        .build(),
                (shadow, lResult) -> {
                    try {
                        int i = totalCount.getAndIncrement();

                        // Reservoir sampling algorithm on all shadows, but only accept those passing predicate
                        if (reservoir.size() < sampleSize) {
                            if (acceptancePredicate.test(shadow)) {
                                reservoir.add(shadow);
                            }
                        } else {
                            int j = random.nextInt(i + 1);
                            if (j < sampleSize && acceptancePredicate.test(shadow)) {
                                reservoir.set(j, shadow);
                            }
                        }
                        return true;
                    } finally {
                        lResult.computeStatusIfUnknown();
                        lResult.setSummarizeSuccesses(true);
                        lResult.summarize();
                    }
                },
                createGetOptions(useNoFetch),
                task,
                result);

        LOGGER.debug("Sampled {} shadows for correlation", reservoir.size());
        return reservoir;
    }

    private Collection<SelectorOptions<GetOperationOptions>> createGetOptions(boolean useNoFetch) {
        if (!useNoFetch) {
            return null;
        }

        GetOperationOptions options = new GetOperationOptions();
        options.setNoFetch(true);
        options.setReadOnly(true);

        return SelectorOptions.createCollection(options);
    }
}
