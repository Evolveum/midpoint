package com.evolveum.midpoint.ninja.action.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

class FocusTypeCounter {

    private final String focusType;
    private final Map<String, PropertyStatsCounter> statsCounterMap;
    private int totalCount;

    FocusTypeCounter(String focusType) {
        this.focusType = focusType;
        this.totalCount = 0;
        this.statsCounterMap = new HashMap<>();
    }

    void count(PrismObject<? extends ObjectType> object) {
        this.totalCount++;
        final PrismObjectValue<? extends ObjectType> objectValue = object.getValue();
        object.getDefinition().getPropertyDefinitions().stream()
                .map(propertyDef -> {
                    try {
                        return objectValue.findOrCreateProperty(propertyDef);
                    } catch (SchemaException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(property -> property.size() <= 1)
                .forEach(property -> {
                    final String propertyName = property.getElementName().getLocalPart();
                    final PropertyStatsCounter counter = this.statsCounterMap.computeIfAbsent(
                            propertyName, key -> new PropertyStatsCounter(propertyName));
                    final String value = property.getRealValue() == null ? null : property.getRealValue().toString();
                    counter.count(value);
                });
    }

    FocusTypeStats calculate() {
        final Collection<PropertyStats> propertiesStats = this.statsCounterMap.values().stream()
                .map(PropertyStatsCounter::calculate)
                .toList();
        return new FocusTypeStats(this.focusType, this.totalCount, propertiesStats);
    }

}
