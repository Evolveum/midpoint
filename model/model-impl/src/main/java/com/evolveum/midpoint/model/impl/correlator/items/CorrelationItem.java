/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.*;

import com.evolveum.midpoint.model.api.ModelPublicConstants;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;

import org.jetbrains.annotations.Nullable;

/**
 * Instance of a correlation item: covering both source and target side.
 *
 * The source side contains the complete data (definitions + values), whereas the target side contains the definitions,
 * and _optionally_ the values. Depending on whether we are going to correlate, or displaying correlation candidates.
 *
 * TODO finish!
 */
class CorrelationItem {

    /**
     * The source item definition + content (in pre-focus or in shadow). Provides the right-hand side of correlation queries.
     */
    @NotNull private final CorrelationItemSource source;

    /**
     * The target item definition(s), indexed by the qualifier.
     *
     * Provides the left-hand side of correlation queries.
     *
     * All keys (qualifiers) must be non-null! The primary qualifier is
     * {@link ModelPublicConstants#PRIMARY_CORRELATION_ITEM_TARGET}.
     */
    @NotNull private final Map<String, CorrelationItemTarget> targetMap;

    private CorrelationItem(
            @NotNull CorrelationItemSource source,
            @NotNull Map<String, CorrelationItemTarget> targetMap) {
        this.source = source;
        this.targetMap = targetMap;
    }

    public static CorrelationItem create(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext)
            throws ConfigurationException {

        return new CorrelationItem(
                CorrelationItemSource.create(itemBean, correlatorContext, correlationContext),
                CorrelationItemTarget.createMap(itemBean, correlatorContext));
    }

    /**
     * Adds a EQ clause to the current query builder.
     */
    S_AtomicFilterExit addClauseToQueryBuilder(S_FilterEntry builder, String targetQualifier) throws SchemaException {
        Object valueToFind = MiscUtil.requireNonNull(
                source.getRealValue(),
                () -> new UnsupportedOperationException("Correlation on null item values is not yet supported"));
        CorrelationItemTarget target = Objects.requireNonNull(targetMap.get(targetQualifier));
        return builder
                .item(target.getRelativePath()).eq(valueToFind);
        // TODO matching rule
    }

    /**
     * Can we use this item for correlation?
     *
     * Temporary implementation: We can, if it's non-null. (In future we might configure the behavior in such cases.)
     */
    public boolean isApplicable() throws SchemaException {
        return source.getRealValue() != null;
    }

    @Override
    public String toString() {
        return "CorrelationItem{" +
                "source=" + source +
                ", targets=" + targetMap +
                '}';
    }

    @NotNull Set<String> getTargetQualifiers() {
        return targetMap.keySet();
    }

    boolean supportsTarget(@NotNull String targetQualifier) {
        return targetMap.containsKey(targetQualifier);
    }

    @Nullable String getDebugName() {
        return source.getDebugName();
    }
}
