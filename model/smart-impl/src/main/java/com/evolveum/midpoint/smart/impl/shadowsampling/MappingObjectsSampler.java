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
 * Sampler for mapping suggestion operations.
 * Splits samples into two groups: smaller set for LLM analysis and larger set for validation.
 * Uses larger sample sizes when caching is enabled.
 */
@Component
public class MappingObjectsSampler implements ObjectsSampler<MappingSampleResult> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingObjectsSampler.class);

    private static final int DEFAULT_LLM_SAMPLE_SIZE = 20;
    private static final int DEFAULT_VALIDATION_SAMPLE_SIZE = 200;
    private static final int CACHED_LLM_SAMPLE_SIZE = 50;
    private static final int CACHED_VALIDATION_SAMPLE_SIZE = 1000;

    private final ModelService modelService;

    public MappingObjectsSampler(ModelService modelService) {
        this.modelService = modelService;
    }

    @Override
    public MappingSampleResult sample(
            ResourceType resource,
            ResourceObjectDefinition typeDefinition,
            Predicate<PrismObject<ShadowType>> acceptancePredicate,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {

        boolean useNoFetch = typeDefinition.isCachingEnabled();
        int llmSize = useNoFetch ? CACHED_LLM_SAMPLE_SIZE : DEFAULT_LLM_SAMPLE_SIZE;
        int validationSize = useNoFetch ? CACHED_VALIDATION_SAMPLE_SIZE : DEFAULT_VALIDATION_SAMPLE_SIZE;
        int totalSize = llmSize + validationSize;

        LOGGER.debug("Sampling shadows for mappings with predicate: {}/{}, llmSize={}, validationSize={}, cached={}",
                resource.getOid(), typeDefinition.getTypeIdentification(), llmSize, validationSize, useNoFetch);

        List<PrismObject<ShadowType>> reservoir = new ArrayList<>(totalSize);
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
                        if (reservoir.size() < totalSize) {
                            if (acceptancePredicate.test(shadow)) {
                                reservoir.add(shadow);
                            }
                        } else {
                            int j = random.nextInt(i + 1);
                            if (j < totalSize && acceptancePredicate.test(shadow)) {
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

        int actualLlmSize = Math.min(llmSize, reservoir.size());
        int actualValidationSize = Math.min(validationSize, reservoir.size());

        // Split the reservoir into two lists
        List<PrismObject<ShadowType>> llmSamples = reservoir.subList(0, actualLlmSize);
        List<PrismObject<ShadowType>> validationSamples = reservoir.subList(
                Math.max(0, reservoir.size() - actualValidationSize),
                reservoir.size());

        LOGGER.debug("Sampled {} shadows for mappings: {} for LLM, {} for validation", reservoir.size(), actualLlmSize, actualValidationSize);

        return new MappingSampleResult(llmSamples, validationSamples);
    }

    /**
     * Returns the expected total sample size (llm + validation) based on whether caching is enabled
     * for the given resource object definition.
     */
    public int getExpectedSampleSize(ResourceObjectDefinition typeDefinition) {
        boolean useNoFetch = typeDefinition.isCachingEnabled();
        int llmSize = useNoFetch ? CACHED_LLM_SAMPLE_SIZE : DEFAULT_LLM_SAMPLE_SIZE;
        int validationSize = useNoFetch ? CACHED_VALIDATION_SAMPLE_SIZE : DEFAULT_VALIDATION_SAMPLE_SIZE;
        return llmSize + validationSize;
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
