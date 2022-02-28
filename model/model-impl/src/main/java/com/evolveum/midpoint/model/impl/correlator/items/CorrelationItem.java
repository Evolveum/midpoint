/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Instance of a correlation item: covering both source and target side.
 *
 * The source side contains the compete data (definitions + values), whereas the target side contains the definitions,
 * and _optionally_ the values. Depending on whether we are going to correlate, or displaying correlation candidates.
 *
 * TODO finish!
 */
public class CorrelationItem {

    @NotNull private final CorrelationItemSource source;
    @NotNull private final CorrelationItemTarget primaryTarget;
    @NotNull private final Collection<CorrelationItemTarget> secondaryTargets;

    private CorrelationItem(
            @NotNull CorrelationItemSource source,
            @NotNull CorrelationItemTarget primaryTarget,
            @NotNull Collection<CorrelationItemTarget> secondaryTargets) {
        this.source = source;
        this.primaryTarget = primaryTarget;
        this.secondaryTargets = secondaryTargets;
    }

    public static CorrelationItem create(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelationContext correlationContext)
            throws ConfigurationException {

        CorrelationItemSource source = CorrelationItemSource.create(itemBean, correlationContext);
        CorrelationItemTarget primaryTarget = CorrelationItemTarget.createPrimary(itemBean, correlationContext);

        return new CorrelationItem(
                source, primaryTarget, List.of());
    }

    S_AtomicFilterExit addToQueryBuilder(S_FilterEntry builder) {
        Object valueToFind = MiscUtil.requireNonNull(
                source.getRealValue(),
                () -> new UnsupportedOperationException("Correlation on null item values is not yet supported"));
        return builder.item(primaryTarget.getPath())
                .eq(valueToFind);
    }

    /**
     * Can we use this item for correlation?
     *
     * Temporary implementation: The value must not be null. (In future we might configure the behavior in such cases.)
     */
    public boolean isApplicable() {
        return source.getRealValue() != null;
    }

    @Override
    public String toString() {
        return "CorrelationItem{" +
                "source=" + source +
                ", primaryTarget=" + primaryTarget +
                ", secondaryTargets=" + secondaryTargets +
                '}';
    }
}
