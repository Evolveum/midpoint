/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayHintType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/**
 * Used to access both "legacy" and "modern" association definitions. Should be quite self-explanatory.
 *
 * TODO better name
 */
public interface AssociationConfigItem extends DebugDumpable {

    @Nullable MappingConfigItem getOutboundMapping() throws ConfigurationException;

    @NotNull List<InboundMappingConfigItem> getInboundMappings() throws ConfigurationException;

    boolean isExclusiveStrong();

    boolean isDeprecated() throws ConfigurationException;

    DisplayHintType getDisplayHint();

    boolean isTolerant();

    @NotNull List<String> getTolerantValuePatterns();

    @NotNull List<String> getIntolerantValuePatterns();

    String getDisplayName();

    Integer getDisplayOrder();

    String getHelp();

    String getDocumentation();

    String getLifecycleState();

    record AttributeBinding(
            @NotNull QName subjectSide,
            @NotNull QName objectSide) implements ShortDumpable, Serializable {

        @Override
        public void shortDump(StringBuilder sb) {
            sb.append(subjectSide.getLocalPart())
                    .append(" (subject) <-> ")
                    .append(objectSide.getLocalPart())
                    .append(" (object)");
        }
    }
}
