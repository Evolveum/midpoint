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

import org.jetbrains.annotations.Nullable;

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
 * Sampler for correlation operations when shadow cache is disabled.
 *
 * First samples OIDs from repository, then loads full shadows from resource only for selected ones.
 */
public class CorrelationObjectsSamplerWhenShadowCacheDisabled implements ObjectsSampler<List<PrismObject<ShadowType>>> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationObjectsSamplerWhenShadowCacheDisabled.class);

    private static final int SAMPLE_SIZE = 2000;

    private final ModelService modelService;
    private final ResourceType resource;
    private final ResourceObjectDefinition typeDefinition;

    public CorrelationObjectsSamplerWhenShadowCacheDisabled(
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

        LOGGER.debug("Sampling shadows for correlation: {}/{}, sampleSize={}",
                resource.getOid(), typeDefinition.getTypeIdentification(), SAMPLE_SIZE);

        List<PrismObject<ShadowType>> reservoir = new ArrayList<>(SAMPLE_SIZE);
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

                        // Decide if this shadow should go into reservoir based on reservoir sampling algorithm
                        Integer reservoirPosition = getReservoirPosition(reservoir.size(), i, random, SAMPLE_SIZE);

                        // If shadow should go into reservoir, load it fully from resource
                        if (reservoirPosition != null) {
                            PrismObject<ShadowType> fullShadow = loadFullShadowFromResource(shadow.getOid(), task, lResult);
                            if (fullShadow == null) {
                                return true; // Skip this shadow if we couldn't load it
                            }

                            // Test predicate and add to reservoir if it passes
                            if (acceptancePredicate.test(fullShadow)) {
                                addToReservoir(reservoir, reservoirPosition, fullShadow);
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

        if (totalCount.get() == 0) {
            sampleDirectlyFromResource(query, reservoir, totalCount, acceptancePredicate, task, result);
        }

        if (reservoir.isEmpty()) {
            LOGGER.warn("No shadows were loaded from resource {}/{}",
                    resource.getOid(), typeDefinition.getTypeIdentification());
        }

        LOGGER.debug("Sampled {} shadows for correlation", reservoir.size());
        return reservoir;
    }

    private void sampleDirectlyFromResource(
            ObjectQuery query,
            List<PrismObject<ShadowType>> reservoir,
            AtomicInteger totalCount,
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
                        Integer reservoirPosition = getReservoirPosition(reservoir.size(), i, random, SAMPLE_SIZE);

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

    /**
     * Loads a full shadow from the resource by OID.
     * Used when caching is disabled to get complete shadow data for predicate testing.
     */
    @Nullable
    private PrismObject<ShadowType> loadFullShadowFromResource(
            String shadowOid,
            Task task,
            OperationResult result) {
        try {
            GetOperationOptions options = new GetOperationOptions();
            options.setReadOnly(true);
            options.setNoFetch(false);

            return modelService.getObject(
                    ShadowType.class,
                    shadowOid,
                    SelectorOptions.createCollection(options),
                    task,
                    result);
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Shadow {} not found: {}", shadowOid, e.getMessage());
            return null;
        } catch (SchemaException | ConfigurationException | SecurityViolationException |
                CommunicationException | ExpressionEvaluationException | SubscriptionComplianceException e) {
            LOGGER.warn("Failed to load shadow {}: {}", shadowOid, e.getMessage(), e);
            return null;
        }
    }

}
