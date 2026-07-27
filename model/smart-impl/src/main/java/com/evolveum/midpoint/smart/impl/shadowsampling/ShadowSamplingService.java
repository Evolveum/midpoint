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

import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Service for sampling shadow objects with different strategies based on cache availability.
 * Implements random sampling when possible, falls back to sequential sampling when necessary.
 */
@Service
public class ShadowSamplingService {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowSamplingService.class);

    private final ModelService modelService;

    public ShadowSamplingService(ModelService modelService) {
        this.modelService = modelService;
    }

    /**
     * Samples shadows for the given resource and type definition using reservoir sampling algorithm.
     * This ensures O(k) memory complexity where k is the sample size, preventing memory issues with large datasets.
     */
    public List<PrismObject<ShadowType>> sampleShadows(
            ResourceType resource,
            ResourceObjectDefinition typeDefinition,
            SamplingConfiguration config,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {

        LOGGER.debug("Sampling shadows for {}/{}, sampleSize={}", resource.getOid(), typeDefinition.getTypeIdentification(), config.getSampleSize());

        int sampleSize = config.getSampleSize();
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
                config.createGetOptions(),
                task,
                result);

        return reservoir;
    }
}
