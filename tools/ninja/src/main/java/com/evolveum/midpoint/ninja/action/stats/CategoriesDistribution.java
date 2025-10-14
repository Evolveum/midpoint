package com.evolveum.midpoint.ninja.action.stats;

import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemFactory;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;

public record CategoriesDistribution(Collection<Float> distribution) implements PrismTransformableRecord {
    private static final ItemName F_VALUE = new ItemName(SchemaConstants.NS_C, "value");

    @Override
    public PrismContainerValue<Containerable> asPrismContainerValue(ItemFactory itemFactory)
            throws SchemaException {
        final PrismProperty<Float> valueProperty = itemFactory.createProperty(F_VALUE);
        for (float value : this.distribution) {
            valueProperty.addRealValueSkipUniquenessCheck(value);
        }

        final PrismContainerValue<Containerable> distributionValues = itemFactory.createContainerValue();
        distributionValues.add(valueProperty, false);

        return distributionValues;
    }

}
