/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Describes how the correlator (could) came to a given candidate owner, and the specific confidence value of it.
 */
public abstract class CorrelationExplanation implements Serializable, DebugDumpable {

    /** The configuration used by the correlator (at this level). */
    @NotNull private final CorrelatorConfiguration correlatorConfiguration;

    /** The resulting confidence computed by the correlator. */
    private final double confidence;

    CorrelationExplanation(@NotNull CorrelatorConfiguration correlatorConfiguration, double confidence) {
        this.correlatorConfiguration = correlatorConfiguration;
        this.confidence = confidence;
    }

    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "configuration", correlatorConfiguration.identify(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "confidence", confidence, indent + 1);
        doSpecificDebugDump(sb, indent);
        return sb.toString();
    }

    abstract void doSpecificDebugDump(StringBuilder sb, int indent);

    public double getConfidence() {
        return confidence;
    }

    /** For correlators that support candidate check but not the specific explanation. */
    public static class GenericCorrelationExplanation extends CorrelationExplanation {

        public GenericCorrelationExplanation(@NotNull CorrelatorConfiguration correlatorConfiguration, double confidence) {
            super(correlatorConfiguration, confidence);
        }

        @Override
        void doSpecificDebugDump(StringBuilder sb, int indent) {
        }
    }

    /** For correlators that do not support neither explanation nor candidate check. */
    public static class UnsupportedCorrelationExplanation extends CorrelationExplanation {

        public UnsupportedCorrelationExplanation(@NotNull CorrelatorConfiguration correlatorConfiguration) {
            super(correlatorConfiguration, 0);
        }

        @Override
        void doSpecificDebugDump(StringBuilder sb, int indent) {
        }
    }
}
