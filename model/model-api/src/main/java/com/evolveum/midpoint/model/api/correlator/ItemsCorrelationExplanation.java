/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.util.LocalizableMessage;

import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;

import org.jetbrains.annotations.NotNull;

public class ItemsCorrelationExplanation extends CorrelationExplanation {

    public ItemsCorrelationExplanation(
            @NotNull CorrelatorConfiguration correlatorConfiguration,
            double confidence) {
        super(correlatorConfiguration, confidence);
    }

    @Override
    public @NotNull LocalizableMessage toLocalizableMessage() {
        StringBuilder sb = new StringBuilder();
        AbstractCorrelatorType configurationBean = correlatorConfiguration.getConfigurationBean();
        if (configurationBean instanceof ItemsCorrelatorType) {
            // Should be always the case
            ItemsCorrelatorType itemsDef = (ItemsCorrelatorType) configurationBean;
            boolean first = true;
            for (CorrelationItemType itemDef : itemsDef.getItem()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(itemDef.getRef()); // TODO i18n
            }
        }
        sb.append(" (");
        sb.append(getDisplayableName());
        sb.append("): ").append(getConfidenceScaledTo100());
        return LocalizableMessageBuilder.buildFallbackMessage(sb.toString());
    }

    @Override
    void doSpecificDebugDump(StringBuilder sb, int indent) {
    }
}
