package com.evolveum.midpoint.ninja.action.stats;

import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemFactory;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;

public record FocusTypeStats(String focusType, int count, Collection<PropertyStats> propertiesStats)
        implements PrismTransformableRecord {
    private static final ItemName C_PROPERTY = new ItemName(SchemaConstants.NS_C, "property");
    private static final ItemName F_TYPE = new ItemName(SchemaConstants.NS_C, "type");
    private static final ItemName F_COUNT = new ItemName(SchemaConstants.NS_C, "count");

    @Override
    public PrismContainerValue<Containerable> asPrismContainerValue(ItemFactory itemFactory) throws SchemaException {
        final PrismProperty<String> focusType = itemFactory.createProperty(F_TYPE);
        new ItemName(SchemaConstants.NS_C, this.focusType);
        focusType.setRealValue(this.focusType);

        final PrismProperty<Integer> focusInstancesCount = itemFactory.createProperty(F_COUNT);
        focusInstancesCount.setRealValue(this.count);

        final PrismContainerValue<Containerable> focusStatsValue = itemFactory.createContainerValue();
        focusStatsValue.add(focusType);
        focusStatsValue.add(focusInstancesCount);
        final PrismContainer<Containerable> properties = itemFactory.createContainer(C_PROPERTY);
        for (final PropertyStats propertyStats : this.propertiesStats) {
            properties.addIgnoringEquivalents(propertyStats.asPrismContainerValue(itemFactory));
        }
        focusStatsValue.add(properties);
        return focusStatsValue;
    }

}
