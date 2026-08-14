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
 * Sampler for correlation operations when shadow cache is enabled.
 *
 * Iterates through cached shadows and tests predicate directly on them.
 */
public class CorrelationObjectsSamplerWhenShadowCacheEnabled implements ObjectsSampler<List<PrismObject<ShadowType>>> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationObjectsSamplerWhenShadowCacheEnabled.class);

    private static final int SAMPLE_SIZE = 5000;

    private final ModelService modelService;
    private final ResourceType resource;
    private final ResourceObjectDefinition typeDefinition;

    public CorrelationObjectsSamplerWhenShadowCacheEnabled(
            ModelService modelService, ResourceType resource, ResourceObjectDefinition typeDefinition) {
        this.modelService = modelService;
        this.resource = resource;
        this.typeDefinition = typeDefinition;
    }

    @Override
    public List<PrismObject<ShadowType>> sample(
            Predicate<PrismObject<ShadowType>> acceptancePredicate,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException, SubscriptionComplianceException {

        LOGGER.debug("Sampling cached shadows for correlation (cached): {}/{}, sampleSize={}",
                resource.getOid(), typeDefinition.getTypeIdentification(), SAMPLE_SIZE);

        List<PrismObject<ShadowType>> reservoir = new ArrayList<>(SAMPLE_SIZE);
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
                        if (reservoir.size() < SAMPLE_SIZE) {
                            if (acceptancePredicate.test(shadow)) {
                                reservoir.add(shadow);
                            }
                        } else {
                            int j = random.nextInt(i + 1);
                            if (j < SAMPLE_SIZE && acceptancePredicate.test(shadow)) {
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
                GetOperationOptions.createNoFetchReadOnlyCollection(),
                task,
                result);

        LOGGER.debug("Sampled {} shadows for correlation", reservoir.size());
        return reservoir;
    }

}
