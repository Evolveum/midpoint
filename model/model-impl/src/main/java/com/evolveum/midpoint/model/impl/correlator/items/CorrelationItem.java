/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;

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
     * The primary target item definition. Provides the left-hand side of correlation queries.
     */
    @NotNull private final CorrelationItemTarget primaryTarget;

    /**
     * The secondary target item definition. Provides the left-hand side of additional correlation queries.
     *
     * May or may not be present. Usually present in "multi-accounts" scenarios.
     */
    @Nullable private final CorrelationItemTarget secondaryTarget;

    private CorrelationItem(
            @NotNull CorrelationItemSource source,
            @NotNull CorrelationItemTarget primaryTarget,
            @Nullable CorrelationItemTarget secondaryTarget) {
        this.source = source;
        this.primaryTarget = primaryTarget;
        this.secondaryTarget = secondaryTarget;
    }

    public static CorrelationItem create(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext)
            throws ConfigurationException {

        CorrelationItemSource source = CorrelationItemSource.create(itemBean, correlatorContext, correlationContext);
        CorrelationItemTarget primaryTarget = CorrelationItemTarget.createPrimary(itemBean, correlatorContext);
        CorrelationItemTarget secondaryTarget = CorrelationItemTarget.createSecondary(itemBean, correlatorContext);

        return new CorrelationItem(source, primaryTarget, secondaryTarget);
    }

    /**
     * Adds a EQ clause to the current query builder.
     */
    S_AtomicFilterExit addClauseToQueryBuilder(S_FilterEntry builder, boolean primary) throws SchemaException {
        Object valueToFind = MiscUtil.requireNonNull(
                source.getRealValue(),
                () -> new UnsupportedOperationException("Correlation on null item values is not yet supported"));
        CorrelationItemTarget target = primary ? primaryTarget : secondaryTarget;
        return builder.item(
                        Objects.requireNonNull(target).getRelativePath())
                .eq(valueToFind);
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

    boolean hasSecondaryTarget() {
        return secondaryTarget != null;
    }

    @Override
    public String toString() {
        return "CorrelationItem{" +
                "source=" + source +
                ", primaryTarget=" + primaryTarget +
                ", secondaryTargets=" + secondaryTarget +
                '}';
    }
}
