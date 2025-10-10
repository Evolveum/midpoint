package com.evolveum.midpoint.ninja.action.stats;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemFactory;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;

public record PropertyStats(String path, float multiValuedRatio, float missingValuesRatio, @Nullable Float cardinality,
        @Nullable CategoriesDistribution distribution) implements PrismTransformableRecord {
    private static final ItemName C_DISTRIBUTION = new ItemName(SchemaConstants.NS_C, "distribution");
    private static final ItemName F_PATH = new ItemName(SchemaConstants.NS_C, "path");
    private static final ItemName F_MISSING_RATIO = new ItemName(SchemaConstants.NS_C, "missingRatio");
    private static final ItemName F_CARDINALITY = new ItemName(SchemaConstants.NS_C, "cardinality");
    private static final ItemName F_MULTI_VALUED_RATIO = new ItemName(SchemaConstants.NS_C, "multiValuedRatio");

    @Override
    public PrismContainerValue<Containerable> asPrismContainerValue(ItemFactory itemFactory) throws SchemaException {
        final PrismProperty<Float> multiValuedRatio = itemFactory.createProperty(F_MULTI_VALUED_RATIO);
        multiValuedRatio.setRealValue(this.multiValuedRatio);
        final PrismProperty<Float> missingCountProperty = itemFactory.createProperty(F_MISSING_RATIO);
        missingCountProperty.setRealValue(this.missingValuesRatio);
        final PrismProperty<Float> cardinalityProperty = itemFactory.createProperty(F_CARDINALITY);
        cardinalityProperty.setRealValue(this.cardinality);
        final PrismProperty<String> pathProperty = itemFactory.createProperty(F_PATH);
        pathProperty.setRealValue(path);

        final PrismContainerValue<Containerable> propertyStatsValue = itemFactory.createContainerValue();
        propertyStatsValue.add(pathProperty);
        propertyStatsValue.add(multiValuedRatio);
        propertyStatsValue.add(missingCountProperty);
        propertyStatsValue.add(cardinalityProperty);
        final PrismContainer<Containerable> distributionContainer = itemFactory.createContainer(C_DISTRIBUTION);
        if (this.distribution != null) {
            distributionContainer.addIgnoringEquivalents(distribution.asPrismContainerValue(itemFactory));
        }
        propertyStatsValue.add(distributionContainer);

        return propertyStatsValue;
    }

}
