/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Computes statistics for midpoint focus objects, e.g. {@link UserType} and {@link RoleType}.
 */
public class FocusObjectStatisticsComputer {

    private final QName objectTypeName;

    private final StatisticsAggregator<ItemName> aggregator =
            new StatisticsAggregator<>(FocusObjectStatisticsComputer::fromPropertyRef);

    public FocusObjectStatisticsComputer(
            @NotNull QName objectTypeName,
            @NotNull PrismObjectDefinition<?> objectDefinition) {

        this.objectTypeName = objectTypeName;

        for (ItemDefinition<?> itemDef : objectDefinition.getDefinitions()) {
            if (!(itemDef instanceof PrismPropertyDefinition<?> propDef)) {
                continue;
            }

            if (!isAggregatable(propDef.getTypeName())) {
                continue;
            }

            ItemName propName = propDef.getItemName();
            aggregator.registerItem(propName, toPropertyRef(propName), false);
        }
    }

    public <O extends ObjectType> void process(@NotNull O object) {
        aggregator.incrementSize();

        PrismObject<? extends ObjectType> prismObject = object.asPrismObject();

        for (ItemName propName : aggregator.getItemOrder()) {
            PrismProperty<?> property = prismObject.findProperty(propName);
            aggregateProperty(propName, property);
        }
    }

    public void postProcessStatistics() {
        aggregator.postProcessStatistics(this::shouldRemoveAttribute);
    }

    public ShadowObjectClassStatisticsType getStatistics() {
        return aggregator.getStatistics();
    }

    public QName getObjectTypeName() {
        return objectTypeName;
    }

    private void aggregateProperty(ItemName propName, PrismProperty<?> property) {
        if (property == null || property.isEmpty()) {
            aggregator.markMissing(propName);
            return;
        }

        Collection<?> realValues = property.getRealValues();
        if (realValues.isEmpty()) {
            aggregator.markMissing(propName);
            return;
        }

        if (realValues.size() > 1) {
            return;
        }

        Object rawValue = realValues.iterator().next();
        if (rawValue == null) {
            aggregator.markMissing(propName);
            return;
        }

        aggregator.aggregateStringValue(propName, toStringValue(rawValue));
    }

    private boolean shouldRemoveAttribute(
            ShadowAttributeStatisticsType stats,
            ShadowObjectClassStatisticsType statistics) {

        return stats.getMissingValueCount() == statistics.getSize()
                || (stats.getValueCount().isEmpty()
                && stats.getValuePatternCount().isEmpty());
    }

    private boolean isAggregatable(@NotNull QName typeName) {
        String localPart = typeName.getLocalPart();

        return "string".equals(localPart)
                || "PolyStringType".equals(localPart)
                || "int".equals(localPart)
                || "integer".equals(localPart)
                || "long".equals(localPart);
    }

    private String toStringValue(Object rawValue) {
        if (rawValue instanceof PolyString polyString) {
            return polyString.getOrig();
        }

        return String.valueOf(rawValue).trim();
    }

    private static ItemPathType toPropertyRef(@NotNull ItemName propName) {
        return propName.toBean();
    }

    private static ItemName fromPropertyRef(@NotNull ItemPathType ref) {
        return ItemName.fromQName(ref.getItemPath().asSingleNameOrFail());
    }
}
