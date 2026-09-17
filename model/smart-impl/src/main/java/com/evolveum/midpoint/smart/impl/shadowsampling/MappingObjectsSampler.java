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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
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
 * Sampler for mapping suggestions.
 *
 * Iterates through shadows using the provided fetch options (noFetch for cached shadows,
 * full fetch when shadow caching is disabled) and tests the predicate directly on them.
 */
public class MappingObjectsSampler implements ObjectsSampler<MappingSampleResult> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingObjectsSampler.class);

    public static final int DEFAULT_LLM_SAMPLE_SIZE_CACHED = 50;
    public static final int DEFAULT_VALIDATION_SAMPLE_SIZE_CACHED = 1000;
    public static final int DEFAULT_LLM_SAMPLE_SIZE_UNCACHED = 20;
    public static final int DEFAULT_VALIDATION_SAMPLE_SIZE_UNCACHED = 200;

    private final ModelService modelService;
    private final ResourceType resource;
    private final ResourceObjectDefinition typeDefinition;
    private final int llmSampleSize;
    private final int validationSampleSize;
    private final Collection<SelectorOptions<GetOperationOptions>> fetchOptions;

    public MappingObjectsSampler(
            ModelService modelService, ResourceType resource, ResourceObjectDefinition typeDefinition,
            int llmSampleSize, int validationSampleSize,
            Collection<SelectorOptions<GetOperationOptions>> fetchOptions) {
        this.modelService = modelService;
        this.resource = resource;
        this.typeDefinition = typeDefinition;
        this.llmSampleSize = llmSampleSize;
        this.validationSampleSize = validationSampleSize;
        this.fetchOptions = fetchOptions;
    }

    @Override
    public MappingSampleResult sample(
            Predicate<PrismObject<ShadowType>> acceptancePredicate,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {

        int totalSize = llmSampleSize + validationSampleSize;

        LOGGER.debug("Sampling shadows for mappings: {}/{}, llmSize={}, validationSize={}",
                resource.getOid(), typeDefinition.getTypeIdentification(), llmSampleSize, validationSampleSize);

        List<PrismObject<ShadowType>> reservoir = new ArrayList<>(totalSize);
        AtomicInteger totalCount = new AtomicInteger(0);

        ObjectQuery query = Resource.of(resource)
                .queryFor(typeDefinition.getTypeIdentification())
                .build();

        collectIntoReservoir(query, reservoir, totalCount, totalSize, acceptancePredicate, fetchOptions, task, result);

        if (totalCount.get() == 0 && GetOperationOptions.isNoFetch(fetchOptions)) {
            // No shadows in repository - sample directly from the resource with full fetch.
            // Skipped when the main search already fetched full shadows (shadow cache disabled).
            collectIntoReservoir(query, reservoir, totalCount, totalSize, acceptancePredicate,
                    GetOperationOptions.readOnly(), task, result);
        }

        if (reservoir.isEmpty()) {
            LOGGER.warn("No shadows were put into reservoir from resource {}/{}",
                    resource.getOid(), typeDefinition.getTypeIdentification());
        }

        LOGGER.debug("Sampled {} shadows for mappings", reservoir.size());
        return splitReservoirIntoSamples(reservoir);
    }

    private void collectIntoReservoir(
            ObjectQuery query,
            List<PrismObject<ShadowType>> reservoir,
            AtomicInteger totalCount,
            int totalSize,
            Predicate<PrismObject<ShadowType>> acceptancePredicate,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Task task,
            OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, ObjectNotFoundException, SubscriptionComplianceException {

        Random random = new Random(1);

        modelService.searchObjectsIterative(
                ShadowType.class,
                query,
                (shadow, lResult) -> {
                    try {
                        int i = totalCount.getAndIncrement();
                        Integer reservoirPosition = getReservoirPosition(reservoir.size(), i, random, totalSize);

                        if (reservoirPosition != null && acceptancePredicate.test(shadow)) {
                            addToReservoir(reservoir, reservoirPosition, shadow);
                        }
                        return true;
                    } finally {
                        lResult.computeStatusIfUnknown();
                        lResult.setSummarizeSuccesses(true);
                        lResult.summarize();
                    }
                },
                options,
                task,
                result);
    }

    private Integer getReservoirPosition(int currentSize, int index, Random random, int sampleSize) {
        if (currentSize < sampleSize) {
            return currentSize;
        }
        int j = random.nextInt(index + 1);
        return j < sampleSize ? j : null;
    }

    private void addToReservoir(List<PrismObject<ShadowType>> reservoir, int position, PrismObject<ShadowType> item) {
        if (position < reservoir.size()) {
            reservoir.set(position, item);
        } else {
            reservoir.add(item);
        }
    }

    private MappingSampleResult splitReservoirIntoSamples(List<PrismObject<ShadowType>> reservoir) {
        int actualLlmSize = Math.min(llmSampleSize, reservoir.size());
        int actualValidationSize = Math.min(validationSampleSize, reservoir.size());

        List<PrismObject<ShadowType>> llmSamples = reservoir.subList(0, actualLlmSize);
        List<PrismObject<ShadowType>> validationSamples = reservoir.subList(
                Math.max(0, reservoir.size() - actualValidationSize),
                reservoir.size());

        LOGGER.debug("Sampled {} shadows for mappings: {} for LLM, {} for validation",
                reservoir.size(), actualLlmSize, actualValidationSize);

        return new MappingSampleResult(llmSamples, validationSamples);
    }
}
