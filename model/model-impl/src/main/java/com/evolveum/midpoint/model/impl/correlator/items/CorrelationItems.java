/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
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

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

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
        stateCheck(!items.isEmpty(), "No correlation items in %s", correlatorContext);
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
     * Creates a list of queries to be executed - either against focus/identity data, or legacy ones.
     */
    List<ObjectQuery> createQueries(
            @NotNull Class<? extends ObjectType> focusType,
            @Nullable String archetypeOid)
            throws SchemaException {
        return isIdentityConfigurationPresent() ?
                List.of(createIdentityQuery(focusType, archetypeOid)) :
                createLegacyQueries(focusType, archetypeOid);
    }

    private boolean isIdentityConfigurationPresent() {
        return items.stream()
                .anyMatch(CorrelationItem::hasIdentityConfiguration);
    }

    private ObjectQuery createIdentityQuery(
            @NotNull Class<? extends ObjectType> focusType,
            @Nullable String archetypeOid) throws SchemaException {

        assert !items.isEmpty();

        S_FilterEntry nextStart = PrismContext.get().queryFor(focusType);
        PrismObjectDefinition<?> focusDef =
                MiscUtil.requireNonNull(
                        PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusType),
                        () -> "No definition for " + focusType);

        S_FilterExit currentEnd = null;
        for (int i = 0; i < items.size(); i++) {
            CorrelationItem correlationItem = items.get(i);
            currentEnd = correlationItem.addClauseToQueryBuilder(nextStart, focusDef);
            if (i < items.size() - 1) {
                nextStart = currentEnd.and();
            } else {
                // We shouldn't modify the builder if we are at the end.
                // (The builder API does not mention it, but the state of the objects are modified on each operation.)
            }
        }

        assert currentEnd != null;

        // Finally, we add a condition for archetype (if needed)
        S_FilterExit end =
                archetypeOid != null ?
                        addArchetypeClause(currentEnd, archetypeOid) :
                        currentEnd;

        return end.build();
    }

    /**
     * LEGACY VARIANT:
     *
     * Creates a list of queries to be executed. There should be a single query for each target.
     *
     * Each query is either a simple conjunction, or an "exists" blocks - if the targets are contained e.g. in an assignment.
     */
    private List<ObjectQuery> createLegacyQueries(
            @NotNull Class<? extends ObjectType> focusType,
            @Nullable String archetypeOid)
            throws SchemaException {

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
            // First, we create either simple .block() or .exist(segment-path).block()
            S_FilterEntry blockStart = openTheBlock(start, targetQualifier);
            // Then we add clauses corresponding to the items, and close the block
            S_FilterExit beforeArchetypeClause =
                    addItemClausesAndCloseTheBlock(blockStart, targetQualifier);
            // Finally, we add a condition for archetype (if needed)
            S_FilterExit end =
                    archetypeOid != null ?
                            addArchetypeClause(beforeArchetypeClause, archetypeOid) :
                            beforeArchetypeClause;

            queries.add(end.build());
        }
        return queries;
    }

    private S_FilterEntry openTheBlock(S_FilterEntry start, @NotNull String targetQualifier) {
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

    private S_FilterExit addArchetypeClause(S_FilterExit before, String archetypeOid) {
        return before.and().item(FocusType.F_ARCHETYPE_REF).ref(archetypeOid);
    }

    private @NotNull S_FilterExit addItemClausesAndCloseTheBlock(S_FilterEntry nextStart, @NotNull String targetQualifier)
            throws SchemaException {
        S_FilterExit currentEnd = null;
        for (int i = 0; i < items.size(); i++) {
            CorrelationItem correlationItem = items.get(i);
            currentEnd = correlationItem.addLegacyClauseToQueryBuilder(nextStart, targetQualifier);
            if (i < items.size() - 1) {
                nextStart = currentEnd.and();
            } else {
                // We shouldn't modify the builder if we are at the end.
                // (The builder API does not mention it, but the state of the objects are modified on each operation.)
            }
        }
        return Objects.requireNonNull(currentEnd).endBlock();
    }
}
