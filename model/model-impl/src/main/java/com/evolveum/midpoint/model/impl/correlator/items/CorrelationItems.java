/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.schema.route.ItemRouteSegment;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Collection of correlation items (for given correlation or correlation-like operation.)
 */
class CorrelationItems {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItems.class);

    @NotNull private final List<CorrelationItem> items;

    @NotNull private final Set<String> allTargetQualifiers;

    @NotNull private final CorrelatorContext<?> correlatorContext;

    private CorrelationItems(
            @NotNull List<CorrelationItem> items,
            @NotNull CorrelatorContext<?> correlatorContext) {
        this.items = items;
        this.correlatorContext = correlatorContext;
        this.allTargetQualifiers = computeAllTargetQualifiers();
        LOGGER.trace("CorrelationItems created with target qualifiers {}:\n{}", allTargetQualifiers, items);
    }

    public static @NotNull CorrelationItems create(
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext) throws ConfigurationException {

        List<CorrelationItem> items = new ArrayList<>();
        for (ItemCorrelationType itemBean : correlatorContext.getConfigurationBean().getItem()) {
            items.add(
                    CorrelationItem.create(itemBean, correlatorContext, correlationContext));
        }
        return new CorrelationItems(items, correlatorContext);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public Collection<CorrelationItem> getItems() {
        return items;
    }

    private Set<String> computeAllTargetQualifiers() {
        return Sets.union(
                correlatorContext.getTargetPlaces().keySet(),
                items.stream()
                        .flatMap(item -> item.getTargetQualifiers().stream())
                        .collect(Collectors.toSet()));
    }

    /**
     * Creates a list of queries to be executed. There should be a single query for each target.
     *
     * Each query is either a simple conjunction, or an "exists" blocks - if the targets are contained e.g. in an assignment.
     */
    public List<ObjectQuery> createQueries(Class<? extends ObjectType> focusType)
            throws SchemaException, ConfigurationException {

        List<ObjectQuery> queries = new ArrayList<>();

        for (@NotNull String targetQualifier : allTargetQualifiers) {

            Set<String> unsupported = items.stream()
                    .filter(item -> !item.supportsTarget(targetQualifier))
                    .map(CorrelationItem::getName)
                    .collect(Collectors.toSet());
            if (!unsupported.isEmpty()) {
                LOGGER.debug("Correlation item(s) {} does not support target '{}', skipping querying for this target",
                        unsupported, targetQualifier);
                continue;
            }

            S_FilterEntry start = PrismContext.get().queryFor(focusType);
            S_FilterEntry blockStart = createBlockStart(start, targetQualifier);
            S_AtomicFilterExit primaryEnd =
                    addTargetClauses(blockStart, targetQualifier)
                            .endBlock();
            queries.add(primaryEnd.build());
        }
        return queries;
    }

    private S_FilterEntry createBlockStart(S_FilterEntry start, @NotNull String targetQualifier) {
        ItemRoute placeRoute = correlatorContext.getTargetPlaceRoute(targetQualifier);
        if (placeRoute.isEmpty()) {
            return start.block();
        } else if (placeRoute.size() == 1) {
            // Very simplified processing: assumes single segment, ignores filtering conditions!
            ItemRouteSegment segment = placeRoute.get(0);
            return start.exists(segment.getPath()).block();
        } else {
            throw new UnsupportedOperationException("Multi-segment routes are not supported yet: " + placeRoute);
        }
    }

    private @NotNull S_AtomicFilterExit addTargetClauses(S_FilterEntry nextStart, @NotNull String targetQualifier)
            throws SchemaException {
        S_AtomicFilterExit currentEnd = null;
        for (int i = 0; i < items.size(); i++) {
            CorrelationItem correlationItem = items.get(i);
            currentEnd = correlationItem.addClauseToQueryBuilder(nextStart, targetQualifier);
            if (i < items.size() - 1) {
                nextStart = currentEnd.and();
            } else {
                // We shouldn't modify the builder if we are at the end.
                // (The builder API does not mention it, but the state of the objects are modified on each operation.)
            }
        }
        return Objects.requireNonNull(currentEnd);
    }
}
