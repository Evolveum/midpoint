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
import java.util.List;
import java.util.Set;

public class CompositeCorrelationExplanation extends CorrelationExplanation {

    @NotNull private final List<ChildCorrelationExplanationRecord> childRecords;

    public CompositeCorrelationExplanation(
            @NotNull CorrelatorConfiguration correlatorConfiguration,
            double confidence,
            @NotNull List<ChildCorrelationExplanationRecord> childRecords) {
        super(correlatorConfiguration, confidence);
        this.childRecords = childRecords;
    }

    @Override
    void doSpecificDebugDump(StringBuilder sb, int indent) {
        sb.append('\n');
        DebugUtil.debugDumpWithLabel(sb, "child records", childRecords, indent + 1);
    }

    public static class ChildCorrelationExplanationRecord implements Serializable, DebugDumpable {

        @NotNull private final CorrelationExplanation explanation;
        private final double weight;
        private final double confidenceIncrement;
        @NotNull private final Set<String> ignoredBecause;

        public ChildCorrelationExplanationRecord(
                @NotNull CorrelationExplanation explanation,
                double weight,
                double confidenceIncrement,
                @NotNull Set<String> ignoredBecause) {
            this.explanation = explanation;
            this.weight = weight;
            this.confidenceIncrement = confidenceIncrement;
            this.ignoredBecause = ignoredBecause;
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
            DebugUtil.debugDumpWithLabelLn(sb, "explanation", explanation, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "weight", String.valueOf(weight), indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "confidenceIncrement", String.valueOf(confidenceIncrement), indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "ignoredBecause", ignoredBecause, indent + 1);
            return sb.toString();
        }
    }
}
