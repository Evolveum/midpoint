/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import org.jetbrains.annotations.NotNull;

public class ItemsCorrelationExplanation extends CorrelationExplanation {

    public ItemsCorrelationExplanation(
            @NotNull CorrelatorConfiguration correlatorConfiguration,
            double confidence) {
        super(correlatorConfiguration, confidence);
    }

    @Override
    void doSpecificDebugDump(StringBuilder sb, int indent) {
    }
}
