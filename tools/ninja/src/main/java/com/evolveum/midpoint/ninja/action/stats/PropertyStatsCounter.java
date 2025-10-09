package com.evolveum.midpoint.ninja.action.stats;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

class PropertyStatsCounter {

    private static final int FIXED_DISTRIBUTION_THRESHOLD = 10;
    private static final float PERCENTAGE_DISTRIBUTION_THRESHOLD = 0.05f;

    private final String propertyName;
    private final Map<String, Integer> valuesCount;

    PropertyStatsCounter(String propertyName) {
        this.propertyName = propertyName;
        this.valuesCount = new HashMap<>();
    }

    void count(@Nullable String value) {
        this.valuesCount.merge(value, 1, Integer::sum);
    }

    PropertyStats calculate() {
        final int totalValuesCount = this.valuesCount.values().stream().reduce(0, Integer::sum);
        final int uniqueValuesCount = this.valuesCount.size();
        final float cardinality = (float) uniqueValuesCount / totalValuesCount;
        final int missingValuesCount = this.valuesCount.getOrDefault(null, 0);

        final CategoriesDistribution categoriesDistribution =
                computeDistribution(totalValuesCount, missingValuesCount, uniqueValuesCount);

        return new PropertyStats(this.propertyName, missingValuesCount, cardinality, categoriesDistribution);
    }

    private @Nullable CategoriesDistribution computeDistribution(int totalValuesCount, int missingValuesCount,
            int uniqueValuesCount) {
        final float floatingDistributionThreshold = totalValuesCount * PERCENTAGE_DISTRIBUTION_THRESHOLD;

        final CategoriesDistribution categoriesDistribution;
        if (missingValuesCount == totalValuesCount || uniqueValuesCount > FIXED_DISTRIBUTION_THRESHOLD
                || uniqueValuesCount > floatingDistributionThreshold) {
            categoriesDistribution = null;
        } else {
            categoriesDistribution = new CategoriesDistribution(this.valuesCount.values());
        }
        return categoriesDistribution;
    }

}
