package com.evolveum.midpoint.ninja.action.stats;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismProperty;

class PropertyStatsCounter {

    private static final int FIXED_DISTRIBUTION_THRESHOLD = 20;
    private static final int MAP_SIZE_LIMIT = 1000;

    private final String propertyPath;
    private final Map<String, Integer> valuesCount;
    private int multiValuedPropertiesCount;
    private int propertiesCount;
    private int missingPropertiesCount;

    PropertyStatsCounter(String propertyPath) {
        this.propertyPath = propertyPath;
        this.valuesCount = new HashMap<>();
        this.multiValuedPropertiesCount = 0;
        this.propertiesCount = 0;
        this.missingPropertiesCount = 0;
    }

    void count(PrismProperty<?> property) {
        this.propertiesCount++;
        if (property.size() > 1) {
            // I am not sure, if it is possible to have also null values in multivalue property. But if it happens, we
            // don't want to consider it as a missing property, unless all its values are null. If all values are null
            // then we want to count the missing property just once.
            final List<?> nonNullValues = property.getRealValues().stream()
                    .filter(Objects::isNull)
                    .toList();
            if (nonNullValues.isEmpty()) {
                // Count missing property just once
                countSingleValue(null);
            } else {
                // Count only values which are not null
                nonNullValues.forEach(this::countSingleValue);
            }
            this.multiValuedPropertiesCount++;
        } else {
            final Object realValue = property.getRealValue();
            countSingleValue(realValue);
        }
    }

    PropertyStats calculate() {
        final int categoriesCount = this.valuesCount.size();
        final float missingPropertiesRatio = (float) this.missingPropertiesCount / this.propertiesCount;
        final int totalValuesCount = this.valuesCount.values().stream().reduce(0, Integer::sum);

        CategoriesDistribution categoriesDistribution = null;
        Float cardinality = null;
        if (this.valuesCount.size() <= MAP_SIZE_LIMIT) {
            // If we have distribution, we don't want to display cardinality, because it could be used to calculate total
            // amount of object (what we don't want because of potential sensitivity of the data).
            if (isDistributionMeaningful(missingPropertiesRatio, categoriesCount)) {
                categoriesDistribution = calculateCategoriesDistribution(totalValuesCount);
            } else {
                cardinality = calculateCardinality(totalValuesCount, categoriesCount);
            }
        }

        final float multiValuedPropertiesRatio = (float) this.multiValuedPropertiesCount / this.propertiesCount;
        return new PropertyStats(this.propertyPath, multiValuedPropertiesRatio, missingPropertiesRatio, cardinality,
                categoriesDistribution);
    }

    private void countSingleValue(@Nullable Object realValue) {
        final String value;
        if (realValue == null) {
            this.missingPropertiesCount++;
            return;
        }

        if (realValue instanceof byte[] byteArray) {
            // Some properties could be in binary form.
            value = String.valueOf(Arrays.hashCode(byteArray));
        } else {
            value = realValue.toString();
        }
        if (this.valuesCount.size() <= MAP_SIZE_LIMIT) {
            this.valuesCount.merge(value, 1, Integer::sum);
        }
    }

    private static boolean isDistributionMeaningful(float missingPropertiesRatio, int uniqueValuesCount) {
        return missingPropertiesRatio < 1 && uniqueValuesCount < FIXED_DISTRIBUTION_THRESHOLD;
    }

    private CategoriesDistribution calculateCategoriesDistribution(int totalValuesCount) {
        final Collection<Float> distribution = this.valuesCount.values().stream()
                .map(count -> (float) count/ totalValuesCount)
                .toList();
        return new CategoriesDistribution(distribution);
    }

    private static float calculateCardinality(int totalValuesCount, int categoriesCount) {
        if (totalValuesCount == 0) {
            return 0f;
        } else {
            return (float) categoriesCount / totalValuesCount;
        }
    }

}
