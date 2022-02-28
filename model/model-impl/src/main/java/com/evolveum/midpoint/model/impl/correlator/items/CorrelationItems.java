/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.schema.route.ItemRouteSegment;
import com.evolveum.midpoint.util.MiscUtil;
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

    /** TODO define */
    @NotNull private final ItemRoute primaryTargetsPlaceRoute;

    /**
     * TODO define
     * Present even if no secondary targets are configured. */
    @NotNull private final ItemRoute secondaryTargetsPlaceRoute;

    private CorrelationItems(
            @NotNull List<CorrelationItem> items,
            @NotNull ItemRoute primaryTargetsPlaceRoute,
            @NotNull ItemRoute secondaryTargetsPlaceRoute) {
        this.items = items;
        this.primaryTargetsPlaceRoute = primaryTargetsPlaceRoute;
        this.secondaryTargetsPlaceRoute = secondaryTargetsPlaceRoute;
    }

    public static @NotNull CorrelationItems create(
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext) throws ConfigurationException {

        List<CorrelationItem> items = new ArrayList<>();
        for (ItemCorrelationType itemBean : correlatorContext.getConfigurationBean().getItem()) {
            items.add(
                    CorrelationItem.create(itemBean, correlatorContext, correlationContext));
        }
        return new CorrelationItems(
                items,
                correlatorContext.getPrimaryTargetsPlaceRoute(),
                correlatorContext.getSecondaryTargetsPlaceRoute());
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

    /**
     * The query structure is "primary targets match OR secondary targets match", with the latter part optional.
     *
     * Both primary and secondary blocks may be created as simple blocks, or as "exists" blocks - if the targets
     * are contained e.g. in an assignment.
     */
    public ObjectQuery createQuery(Class<? extends ObjectType> focusType)
            throws SchemaException, ConfigurationException {

        S_FilterEntry primaryStart = PrismContext.get().queryFor(focusType);
        S_FilterEntry primaryBlockStart = createBlockStart(primaryStart, true);
        S_AtomicFilterExit primaryEnd =
                addTargetClauses(primaryBlockStart, true)
                        .endBlock();

        S_AtomicFilterExit totalEnd;
        if (areSecondaryTargetsPresent()) {
            S_FilterEntry secondaryStart = primaryEnd.or();
            S_FilterEntry secondaryBlockStart = createBlockStart(secondaryStart, false);
            totalEnd =
                    addTargetClauses(secondaryBlockStart, false)
                            .endBlock();
        } else {
            totalEnd = primaryEnd;
        }

        return totalEnd.build();
    }

    private S_FilterEntry createBlockStart(S_FilterEntry start, boolean primary) {
        ItemRoute placeRoute = primary ? primaryTargetsPlaceRoute : secondaryTargetsPlaceRoute;
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

    private @NotNull S_AtomicFilterExit addTargetClauses(S_FilterEntry nextStart, boolean primary)
            throws SchemaException {
        S_AtomicFilterExit currentEnd = null;
        for (int i = 0; i < items.size(); i++) {
            CorrelationItem correlationItem = items.get(i);
            currentEnd = correlationItem.addClauseToQueryBuilder(nextStart, primary);
            if (i < items.size() - 1) {
                nextStart = currentEnd.and();
            } else {
                // We shouldn't modify the builder if we are at the end.
                // (The builder API does not mention it, but the state of the objects are modified on each operation.)
            }
        }
        return Objects.requireNonNull(currentEnd);
    }

    private boolean areSecondaryTargetsPresent() throws ConfigurationException {
        Set<Boolean> hasSecondary = items.stream()
                .map(CorrelationItem::hasSecondaryTarget)
                .collect(Collectors.toSet());
        return MiscUtil.requireNonNull(
                MiscUtil.extractSingleton(
                        hasSecondary,
                        () -> new ConfigurationException("Either all items should have secondary targets, or none of them.")),
                () -> new AssertionError("No information?!"));
    }

}
