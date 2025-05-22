/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl;

import com.evolveum.midpoint.common.UserFriendlyPrettyPrinterOptions;

import org.jetbrains.annotations.NotNull;

public class DeltaPrinterOptions {

    /**
     * Whether to show object delta (i.e. the whole change) in the delta printer in
     * case changed {@link com.evolveum.midpoint.prism.path.ItemPath} is empty.
     */
    private boolean showObjectDelta = true;

    /**
     * Show partial changes.
     * If changed {@link com.evolveum.midpoint.prism.path.ItemPath} is set to "assignment" and {@link #showPartialDeltas} is true,
     * then also deltas with {@link com.evolveum.midpoint.prism.path.ItemPath} "assignment/*" will be printed.
     */
    private boolean showPartialDeltas = false;

    /**
     * Whether to use estimated old values in the delta printer.
     * If true, then the delta printer will use estimated old values instead of the changes (added, removed, replaced).
     */
    private boolean useEstimatedOldValues = false;

    private UserFriendlyPrettyPrinterOptions prettyPrinterOptions = new UserFriendlyPrettyPrinterOptions();

    @NotNull
    public UserFriendlyPrettyPrinterOptions prettyPrinterOptions() {
        return prettyPrinterOptions;
    }

    public DeltaPrinterOptions prettyPrinterOptions(@NotNull UserFriendlyPrettyPrinterOptions prettyPrinterOptions) {
        this.prettyPrinterOptions = prettyPrinterOptions;
        return this;
    }

    public boolean showObjectDelta() {
        return showObjectDelta;
    }

    public DeltaPrinterOptions showObjectDelta(boolean showObjectDelta) {
        this.showObjectDelta = showObjectDelta;
        return this;
    }

    public boolean showPartialDeltas() {
        return showPartialDeltas;
    }

    public DeltaPrinterOptions showPartialDeltas(boolean showPartialDeltas) {
        this.showPartialDeltas = showPartialDeltas;
        return this;
    }

    public boolean useEstimatedOldValues() {
        return useEstimatedOldValues;
    }

    public DeltaPrinterOptions useEstimatedOldValues(boolean useEstimatedOldValues) {
        this.useEstimatedOldValues = useEstimatedOldValues;
        return this;
    }
}
