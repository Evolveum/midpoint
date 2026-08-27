/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Computes statistics for shadow objects of a resource object type.
 */
public class ObjectTypeStatisticsComputer {

    private final StatisticsAggregator<QName> aggregator =
            new StatisticsAggregator<>(ObjectTypeStatisticsComputer::fromAttributeRef);

    public ObjectTypeStatisticsComputer(@NotNull ResourceObjectTypeDefinition typeDefinition) {
        for (ShadowAttributeDefinition<?, ?, ?, ?> attrDef : typeDefinition.getAttributeDefinitions()) {
            ItemName attrName = attrDef.getItemName();
            aggregator.registerItem(attrName, toAttributeRef(attrName), aggregator.isDnAttribute(attrName));
        }
    }

    public void process(@NotNull ShadowType shadow) {
        aggregator.incrementSize();

        for (QName attrName : aggregator.getItemOrder()) {
            List<?> values = ShadowUtil.getAttributeValues(shadow, attrName);
            aggregator.aggregateValues(attrName, values);
        }
    }

    public void postProcessStatistics() {
        aggregator.postProcessStatistics();
    }

    public ShadowObjectClassStatisticsType getStatistics() {
        return aggregator.getStatistics();
    }

    /** Converts plain attribute name to an {@link ItemPathType} used in statistics beans. */
    private static ItemPathType toAttributeRef(@NotNull QName attrName) {
        return ShadowType.F_ATTRIBUTES.append(attrName).toBean();
    }

    private static QName fromAttributeRef(@NotNull ItemPathType ref) {
        return ref.getItemPath().rest().asSingleNameOrFail();
    }
}
