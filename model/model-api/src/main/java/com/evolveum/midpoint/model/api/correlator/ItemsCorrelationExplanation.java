/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.LocalizableMessage;

import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

public class ItemsCorrelationExplanation extends CorrelationExplanation {

    public ItemsCorrelationExplanation(
            @NotNull CorrelatorConfiguration correlatorConfiguration,
            @NotNull Confidence confidence) {
        super(correlatorConfiguration, confidence);
    }

    @Override
    public @NotNull LocalizableMessage toLocalizableMessage() {
        StringBuilder sb = new StringBuilder();
        AbstractCorrelatorType configurationBean = correlatorConfiguration.getConfigurationBean();
        sb.append(getDisplayableName()).append(": ");
        if (configurationBean instanceof ItemsCorrelatorType itemsDef) {
            // Should be always the case
            boolean first = true;
            for (CorrelationItemType itemDef : itemsDef.getItem()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                ItemPathType pathBean = itemDef.getRef();
                if (pathBean != null) { // should always be the case
                    var path = pathBean.getItemPath();
                    sb.append(path); // TODO i18n
                    var confidence = getItemConfidence(path);
                    if (confidence != null && confidence != 1.0) {
                        sb.append(" (").append(Math.round(confidence * 100)).append("%)");
                    }
                }
            }
        }
        sb.append(": ").append(getConfidenceAsPercent());
        return LocalizableMessageBuilder.buildFallbackMessage(sb.toString());
    }

    private Double getItemConfidence(@NotNull ItemPath path) {
        if (confidence instanceof Confidence.PerItemConfidence perItemConfidence) {
            return perItemConfidence.getItemConfidence(path);
        } else {
            return null;
        }
    }

    @Override
    void doSpecificDebugDump(StringBuilder sb, int indent) {
    }
}
