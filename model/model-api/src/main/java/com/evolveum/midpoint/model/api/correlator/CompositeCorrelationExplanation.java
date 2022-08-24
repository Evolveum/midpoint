/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.util.*;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
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

    // Temporary implementation
    @Override
    public @NotNull LocalizableMessage toLocalizableMessage() {
        if (childRecords.isEmpty()) {
            return new LocalizableMessageBuilder()
                    .key("CorrelationExplanation.Composite.noChildren")
                    .args(getDisplayableName(), getConfidenceScaledTo100())
                    .build();
        }

        LocalizableMessageListBuilder componentsBuilder = new LocalizableMessageListBuilder()
                .prefix(LocalizableMessageBuilder.buildFallbackMessage("["))
                .postfix(LocalizableMessageBuilder.buildFallbackMessage("]"))
                .separator(LocalizableMessageList.SEMICOLON);

        for (ChildCorrelationExplanationRecord childRecord : childRecords) {
            if (!childRecord.ignoredBecause.isEmpty()) {
                continue;
            }
            if (childRecord.explanation.confidence == 0) {
                continue;
            }
            componentsBuilder.addMessage(
                    new LocalizableMessageBuilder()
                            .key("CorrelationExplanation.Composite.child")
                            .arg(childRecord.explanation.toLocalizableMessage())
                            .arg(String.format(Locale.US, "%.2f", childRecord.weight)) // TODO i18n
                            .arg(Math.round(childRecord.confidenceIncrement * 100))
                            .build());
        }

        LocalizableMessageListBuilder topBuilder = new LocalizableMessageListBuilder();
        topBuilder.addMessage(
                componentsBuilder.build());
        topBuilder.addMessage(
                LocalizableMessageBuilder.buildFallbackMessage(" => " + getConfidenceScaledTo100()));
        return topBuilder.build();
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
