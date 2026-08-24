/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.shadowsampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;


import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
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
 * Sampler for mapping suggestions when shadow cache is enabled.
 *
 * Iterates through cached shadows and tests predicate directly on them.
 */
public class MappingObjectsSamplerWhenShadowCacheEnabled implements ObjectsSampler<MappingSampleResult> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingObjectsSamplerWhenShadowCacheEnabled.class);

    public static final int DEFAULT_LLM_SAMPLE_SIZE = 50;
    public static final int DEFAULT_VALIDATION_SAMPLE_SIZE = 1000;

    private final ModelService modelService;
    private final ResourceType resource;
    private final ResourceObjectDefinition typeDefinition;
    private final int llmSampleSize;
    private final int validationSampleSize;

    public MappingObjectsSamplerWhenShadowCacheEnabled(
            ModelService modelService, ResourceType resource, ResourceObjectDefinition typeDefinition,
            int llmSampleSize, int validationSampleSize) {
        this.modelService = modelService;
        this.resource = resource;
        this.typeDefinition = typeDefinition;
        this.llmSampleSize = llmSampleSize;
        this.validationSampleSize = validationSampleSize;
    }

    @Override
    public MappingSampleResult sample(
            Predicate<PrismObject<ShadowType>> acceptancePredicate,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {

        int totalSize = llmSampleSize + validationSampleSize;

        LOGGER.debug("Sampling cached shadows for mappings (cached): {}/{}, llmSize={}, validationSize={}",
                resource.getOid(), typeDefinition.getTypeIdentification(), llmSampleSize, validationSampleSize);

        List<PrismObject<ShadowType>> reservoir = new ArrayList<>(totalSize);
        AtomicInteger totalCount = new AtomicInteger(0);
        Random random = new Random(1);

        ObjectQuery query = Resource.of(resource)
                .queryFor(typeDefinition.getTypeIdentification())
                .build();

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
                GetOperationOptions.createNoFetchReadOnlyCollection(),
                task,
                result);

        if (totalCount.get() == 0) {
            sampleDirectlyFromResource(query, reservoir, totalCount, totalSize, acceptancePredicate, task, result);
        }

        if (reservoir.isEmpty()) {
            LOGGER.warn("No shadows were loaded from resource {}/{}",
                    resource.getOid(), typeDefinition.getTypeIdentification());
        }

        return splitReservoirIntoSamples(reservoir);
    }

    private void sampleDirectlyFromResource(
            ObjectQuery query,
            List<PrismObject<ShadowType>> reservoir,
            AtomicInteger totalCount,
            int totalSize,
            Predicate<PrismObject<ShadowType>> acceptancePredicate,
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
                GetOperationOptions.createReadOnlyCollection(),
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
