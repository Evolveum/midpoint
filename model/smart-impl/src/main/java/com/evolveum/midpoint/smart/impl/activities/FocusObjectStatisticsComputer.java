/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computes statistics for midpoint objects (e.g. {@link UserType}, {@link RoleType}) by dynamically
 * discovering their properties from the prism schema definition.
 *
 * <p>Reuses {@link ShadowObjectClassStatisticsType} / {@link ShadowAttributeStatisticsType} as the
 * output data structure.</p>
 */
public class FocusObjectStatisticsComputer {

    private static final Trace LOGGER = TraceManager.getTrace(FocusObjectStatisticsComputer.class);

    /** Regular expression pattern for token delimiters. */
    private static final String DELIMITERS = "[-_:*#+.]+";

    /** Compiled pattern for token delimiters (used by Matcher). */
    private static final Pattern DELIMITER_PATTERN = Pattern.compile(DELIMITERS);

    /**
     * Regular expression pattern for matching URLs.
     */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://|www\\.)\\S+$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Regular expression pattern for matching email addresses.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    /**
     * Regular expression pattern for matching phone numbers.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[\\d\\s\\-()]{7,20}$"
    );

    /** Per-property incremental aggregation state, insertion-ordered to match attribute[] output. */
    private final Map<ItemName, PropertyAggregation> aggregations = new LinkedHashMap<>();

    /** JAXB statistics object being built. */
    private final ShadowObjectClassStatisticsType statistics = new ShadowObjectClassStatisticsType();

    /** The object type QName for which statistics are computed. */
    private final QName objectTypeName;

    /**
     * Creates a new computer that dynamically discovers simple (string-like, single-valued) properties
     * from the given object type definition.
     *
     * @param objectTypeName QName of the object type (e.g. UserType)
     * @param objectDefinition prism object definition used to discover properties
     */
    public FocusObjectStatisticsComputer(QName objectTypeName, PrismObjectDefinition<?> objectDefinition) {
        this.objectTypeName = objectTypeName;
        statistics.setSize(0);

        for (ItemDefinition<?> itemDef : objectDefinition.getDefinitions()) {
            if (!(itemDef instanceof PrismPropertyDefinition<?> propDef)) {
                continue;
            }

            // Only include simple value types that can be meaningfully aggregated as strings
            QName typeName = propDef.getTypeName();
            if (!isAggregatable(typeName)) {
                continue;
            }

            ItemName propName = propDef.getItemName();
            if (!aggregations.containsKey(propName)) {
                aggregations.put(propName, new PropertyAggregation());
                statistics.getAttribute().add(
                        new ShadowAttributeStatisticsType().ref(toPropertyRef(propName)));
            }
        }
    }

    /**
     * Processes a single midpoint object, extracting values for all tracked properties.
     */
    public <O extends ObjectType> void process(O object) {
        statistics.setSize(statistics.getSize() + 1);
        PrismObject<? extends ObjectType> prismObject = object.asPrismObject();

        for (Map.Entry<ItemName, PropertyAggregation> entry : aggregations.entrySet()) {
            ItemName propName = entry.getKey();
            PropertyAggregation agg = entry.getValue();

            PrismProperty<?> property = prismObject.findProperty(propName);
            if (property == null || property.isEmpty()) {
                agg.missingCount++;
                continue;
            }

            Collection<?> realValues = property.getRealValues();
            if (realValues == null || realValues.isEmpty()) {
                agg.missingCount++;
                continue;
            }

            // Only aggregate single-valued properties for meaningful statistics
            if (realValues.size() > 1) {
                continue;
            }

            Object rawValue = realValues.iterator().next();
            if (rawValue == null) {
                agg.missingCount++;
                continue;
            }

            String stringValue = toStringValue(rawValue);
            if (stringValue == null || stringValue.isBlank()) {
                agg.missingCount++;
                continue;
            }

            agg.valueCounts.merge(stringValue, 1, Integer::sum);
            aggregateTokenPatterns(agg, stringValue);
        }
    }

    /**
     * Performs post-processing of collected values and converts internal structures
     * to JAXB-compatible statistics.
     */
    public void postProcessStatistics() {
        for (Iterator<ShadowAttributeStatisticsType> it = statistics.getAttribute().iterator(); it.hasNext(); ) {
            ShadowAttributeStatisticsType statsAttr = it.next();
            ItemName propKey = fromPropertyRef(statsAttr.getRef());
            PropertyAggregation agg = aggregations.get(propKey);
            if (agg == null) {
                it.remove();
                continue;
            }

            statsAttr.setMissingValueCount(agg.missingCount);
            statsAttr.setUniqueValueCount(agg.valueCounts.size());

            emitAllValueCounts(agg.valueCounts, statsAttr);
            emitAllPatterns(agg.firstTokenCounts, ShadowValuePatternType.FIRST_TOKEN, statsAttr);
            emitAllPatterns(agg.lastTokenCounts, ShadowValuePatternType.LAST_TOKEN, statsAttr);

            if (statsAttr.getMissingValueCount() == statistics.getSize()
                    || (statsAttr.getValueCount().isEmpty() && statsAttr.getValuePatternCount().isEmpty())) {
                it.remove();
            }
        }
    }

    /**
     * Returns the statistics object.
     */
    public ShadowObjectClassStatisticsType getStatistics() {
        return statistics;
    }

    /**
     * Returns the object type QName for which statistics are computed.
     */
    public QName getObjectTypeName() {
        return objectTypeName;
    }

    /**
     * Determines whether the given type can be meaningfully aggregated as a string statistic.
     */
    private boolean isAggregatable(QName typeName) {
        String localPart = typeName.getLocalPart();
        return "string".equals(localPart)
                || "PolyStringType".equals(localPart)
                || "int".equals(localPart)
                || "integer".equals(localPart)
                || "long".equals(localPart);
    }

    /**
     * Converts a raw property value to its string representation.
     */
    private String toStringValue(Object rawValue) {
        if (rawValue instanceof PolyString polyString) {
            return polyString.getOrig();
        }
        return String.valueOf(rawValue).trim();
    }

    /**
     * Extracts first/last tokens (including their adjacent delimiter) and increments their counts.
     * Skips values that look like URLs, emails, or phone numbers.
     */
    private void aggregateTokenPatterns(PropertyAggregation agg, String value) {
        if (isUrl(value) || isEmail(value) || isPhoneNumber(value)) {
            return;
        }

        Matcher matcher = DELIMITER_PATTERN.matcher(value);

        if (!matcher.find()) {
            return;
        }

        int firstDelimStart = matcher.start();
        int firstDelimEnd = matcher.end();

        // Walk to find the last delimiter occurrence
        int lastDelimStart = firstDelimStart;
        while (matcher.find()) {
            lastDelimStart = matcher.start();
        }

        // firstToken: text + trailing delimiter (skip if value starts with delimiter)
        if (firstDelimStart > 0) {
            agg.firstTokenCounts.merge(value.substring(0, firstDelimEnd), 1, Integer::sum);
        }

        // lastToken: leading delimiter + text (skip if value ends with delimiter)
        if (lastDelimStart < value.length() - 1) {
            agg.lastTokenCounts.merge(value.substring(lastDelimStart), 1, Integer::sum);
        }
    }

    /**
     * Emits ALL value counts into the statistics without any topN limit.
     */
    private void emitAllValueCounts(Map<String, Integer> valueCounts, ShadowAttributeStatisticsType stats) {
        valueCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> stats.beginValueCount()
                        .value(entry.getKey())
                        .count(entry.getValue()));
    }

    /**
     * Emits ALL pattern counts of a given type into the statistics without any topN limit.
     */
    private void emitAllPatterns(
            Map<String, Integer> counts,
            ShadowValuePatternType type,
            ShadowAttributeStatisticsType stats) {
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> stats.beginValuePatternCount()
                        .value(entry.getKey())
                        .type(type)
                        .count(entry.getValue()));
    }

    private boolean isUrl(String str) {
        return str != null && URL_PATTERN.matcher(str).matches();
    }

    private boolean isEmail(String str) {
        return str != null && EMAIL_PATTERN.matcher(str).matches();
    }

    private boolean isPhoneNumber(String str) {
        return str != null && PHONE_PATTERN.matcher(str).matches();
    }

    /** Converts property name to an {@link ItemPathType} used in statistics beans. */
    private ItemPathType toPropertyRef(ItemName propName) {
        return propName.toBean();
    }

    /** Converts {@link ItemPathType} back to a property name. */
    private ItemName fromPropertyRef(ItemPathType ref) {
        return ItemName.fromQName(ref.getItemPath().asSingleNameOrFail());
    }

    /**
     * Holds incrementally aggregated counts for a single property.
     */
    private static class PropertyAggregation {
        int missingCount;
        final Map<String, Integer> valueCounts = new HashMap<>();
        final Map<String, Integer> firstTokenCounts = new HashMap<>();
        final Map<String, Integer> lastTokenCounts = new HashMap<>();
    }
}
