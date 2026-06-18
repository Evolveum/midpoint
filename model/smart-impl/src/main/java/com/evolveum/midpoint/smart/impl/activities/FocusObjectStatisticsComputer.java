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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Iterator;

/**
 * Computes statistics for midpoint focus objects, e.g. {@link UserType} and {@link RoleType}.
 */
public class FocusObjectStatisticsComputer extends AbstractStatisticsComputer<ItemName> {

    private final QName objectTypeName;

    public FocusObjectStatisticsComputer(QName objectTypeName, @NotNull PrismObjectDefinition<?> objectDefinition) {
        this.objectTypeName = objectTypeName;
        getStatisticsObject().setSize(0);

        for (ItemDefinition<?> itemDef : objectDefinition.getDefinitions()) {
            if (!(itemDef instanceof PrismPropertyDefinition<?> propDef)) {
                continue;
            }

            if (!isAggregatable(propDef.getTypeName())) {
                continue;
            }

            ItemName propName = propDef.getItemName();
            registerItem(propName, toPropertyRef(propName), false);
        }
    }

    public <O extends ObjectType> void process(@NotNull O object) {
        incrementSize();

        PrismObject<? extends ObjectType> prismObject = object.asPrismObject();

        for (ItemName propName : getItemOrder()) {
            PrismProperty<?> property = prismObject.findProperty(propName);
            aggregateProperty(propName, property);
        }
    }

    private void aggregateProperty(ItemName propName, PrismProperty<?> property) {
        if (property == null || property.isEmpty()) {
            markMissing(propName);
            return;
        }

        Collection<?> realValues = property.getRealValues();
        if (realValues.isEmpty()) {
            markMissing(propName);
            return;
        }

        if (realValues.size() > 1) {
            return;
        }

        Object rawValue = realValues.iterator().next();
        if (rawValue == null) {
            markMissing(propName);
            return;
        }

        aggregateStringValue(propName, toStringValue(rawValue));
    }

    @Override
    protected void afterPostProcessAttribute(
            @NotNull ShadowAttributeStatisticsType stats,
            Iterator<ShadowAttributeStatisticsType> iterator) {

        if (stats.getMissingValueCount() == getStatisticsObject().getSize()
                || (stats.getValueCount().isEmpty() && stats.getValuePatternCount().isEmpty())) {
            iterator.remove();
        }
    }

    public QName getObjectTypeName() {
        return objectTypeName;
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

    private ItemPathType toPropertyRef(@NotNull ItemName propName) {
        return propName.toBean();
    }

    @Override
    protected ItemName fromRef(@NotNull ItemPathType ref) {
        return ItemName.fromQName(ref.getItemPath().asSingleNameOrFail());
    }
}
