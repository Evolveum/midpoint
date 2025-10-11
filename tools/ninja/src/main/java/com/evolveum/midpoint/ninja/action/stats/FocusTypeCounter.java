package com.evolveum.midpoint.ninja.action.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

class FocusTypeCounter {

    private final String focusType;
    private final Predicate<ItemDefinition<?>> itemsIncludePredicate;
    private final Map<String, PropertyStatsCounter> statsCounterMap;
    private final MagnitudeCounter totalCount;

    FocusTypeCounter(String focusType, Predicate<ItemDefinition<?>> itemsIncludePredicate) {
        this.focusType = focusType;
        this.itemsIncludePredicate = itemsIncludePredicate;
        this.totalCount = new MagnitudeCounter();
        this.statsCounterMap = new HashMap<>();
    }

    void count(PrismObject<? extends ObjectType> object) {
        this.totalCount.increment();
        final List<PrismPropertyDefinition<?>> propertyDefinitions = object.getDefinition().getPropertyDefinitions();
        final Collection<PrismProperty<?>> properties = collectProperties(object.getValue(), propertyDefinitions);
        try {
            final PrismContainer<?> extension = object.getOrCreateExtension();
            properties.addAll(flatten(extension));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        properties.forEach(property -> {
            final String propertyPath = property.getPath().toString();
            final PropertyStatsCounter counter = this.statsCounterMap.computeIfAbsent(
                    propertyPath, key -> new PropertyStatsCounter(propertyPath));
            counter.count(property);
        });
    }

    FocusTypeStats calculate() {
        final Collection<PropertyStats> propertiesStats = this.statsCounterMap.values().stream()
                .map(PropertyStatsCounter::calculate)
                .toList();
        return new FocusTypeStats(this.focusType, this.totalCount.toOrderOfMagnitude(), propertiesStats);
    }

    private Collection<PrismProperty<?>> flatten(PrismContainer<?> container) {
        if (isExcluded(container.getDefinition())) {
            return Collections.emptyList();
        }

        final ComplexTypeDefinition complexTypeDefinition = container.getComplexTypeDefinition();
        final PrismContainerValue<?> containerValue = container.hasNoValues()
                ? container.createNewValue()
                : container.getValue();

        final Collection<PrismProperty<?>> currentLevelProperties = collectProperties(containerValue,
                complexTypeDefinition.getPropertyDefinitions());
        final List<PrismProperty<?>> nestedProperties = new ArrayList<>();
        for (final ItemDefinition<?> definition : complexTypeDefinition.getDefinitions()) {
            if (definition instanceof PrismContainerDefinition<?> containerDefinition) {
                // We decided to not calculate stats for multivalued containers, because it complicates calculation,
                // and we are  not even sure if we would use such stats. We also MUST NOT work with "elaborate"
                // containers, because they may contain cycles.
                if (definition.isSingleValue() && !definition.isElaborate()) {
                    try {
                        final PrismContainer<Containerable> innerContainer = containerValue.findOrCreateContainer(
                                containerDefinition.getItemName());
                        nestedProperties.addAll(flatten(innerContainer));
                    } catch (SchemaException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        currentLevelProperties.addAll(nestedProperties);
        return currentLevelProperties;
    }

    private Collection<PrismProperty<?>> collectProperties(PrismContainerValue<?> containerValue,
            Collection<PrismPropertyDefinition<?>> propertyDefinitions) {
        final List<PrismProperty<?>> properties = new ArrayList<>();
        for (PrismPropertyDefinition<?> definition : propertyDefinitions) {
            if (isExcluded(definition)) {
                continue;
            }

            try {
                properties.add(containerValue.findOrCreateProperty(definition));
            } catch (SchemaException e) {
                throw new RuntimeException(e);
            }
        }
        return properties;
    }

    private boolean isExcluded(ItemDefinition<?> definition) {
        return ! this.itemsIncludePredicate.test(definition);
    }
}
