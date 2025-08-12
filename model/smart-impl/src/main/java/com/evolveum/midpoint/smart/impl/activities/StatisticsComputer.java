/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeTupleStatisticsType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * Computes statistics for shadow objects.
 * Does not need to care about the timestamp and the coverage.
 */
@VisibleForTesting
public class StatisticsComputer {

    /**
     * Maximum number of value occurrences to be retained per attribute.
     * TODO: Make this configurable.
     */
    private static final int TOP_N_LIMIT = 30;

    /**
     * Cardinality limit for cross-table (pairwise attribute) statistics.
     * <p>
     * If the number of unique value pairs for a particular attribute exceeds this limit,
     * the attribute is excluded from pairwise statistics.
     * TODO: Make this configurable.
     */
    private static final int VALUE_COUNT_PAIR_HARD_LIMIT = 10;

    /**
     * Percentage-based cardinality limit for cross-table statistics.
     * If the unique value count for an attribute exceeds this percentage of the total sample size,
     * the attribute is excluded from pairwise statistics.
     * TODO: Make this configurable.
     */
    private static final double VALUE_COUNT_PAIR_PERCENTAGE_LIMIT = 0.05;

    /**
     * Set of common affixes to match in attribute values.
     */
    private static final Set<String> AFFIXES = new HashSet<>(Arrays.asList(
            "prod", "priv", "adm", "admin", "usr", "user", "ops", "svc", "int", "ext"
    ));

    /**
     * List of delimiters used to separate affixes in strings.
     */
    private static final List<String> DELIMITERS = Arrays.asList(
            ".", "-", "_"
    );

    /**
     * Mapping from affix to compiled regex {@link Pattern} to match affix at string boundaries.
     */
    private static final Map<String, Pattern> AFFIX_PATTERNS = initAffixPatterns();

    /**
     * Stores attribute values for each shadow, for use in cross-table (pairwise) statistics.
     * <p>
     * Maps attribute QName to a list of observed values (one per processed shadow).
     */
    private final Map<QName, LinkedList<List<?>>> shadowStorage = new HashMap<>();

    /**
     * JAXB statistics object being built.
     */
    private final ShadowObjectClassStatisticsType statistics = new ShadowObjectClassStatisticsType();

    /**
     * Constructs a new statistics computer for the given object class definition.
     * <p>
     * @param objectClassDef Resource object class definition.
     */
    public StatisticsComputer(ResourceObjectClassDefinition objectClassDef) {
        List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> attributeDefinitions = objectClassDef.getAttributeDefinitions();
        for (ShadowAttributeDefinition<?, ?, ?, ?> attrDef : attributeDefinitions) {
            createAttributeStatisticsIfNeeded(attrDef.getItemName());
            shadowStorage.put(attrDef.getItemName(), new LinkedList<>());
        }
    }

    /**
     * Initializes {@link #AFFIX_PATTERNS} by constructing regex patterns for each affix
     * using the defined delimiters. Patterns match affixes at the start or end of a string.
     */
    private static Map<String, Pattern> initAffixPatterns() {
        String delimiterRegex = "[" + DELIMITERS.stream().map(Pattern::quote).collect(Collectors.joining()) + "]";

        return AFFIXES.stream().collect(Collectors.toMap(
                affix -> affix,
                affix -> Pattern.compile(
                        String.format("(^%s%s)|(%s%s$)",
                                Pattern.quote(affix), delimiterRegex,
                                delimiterRegex, Pattern.quote(affix)
                        )
                )
        ));
    }

    /**
     * Processes the given shadow object, updating statistics.
     *
     * @param shadow Shadow object to process.
     */
    public void process(ShadowType shadow) {
        statistics.setSize(statistics.getSize() + 1);
        updateShadowStorage(shadow);
    }

    /**
     * Performs post-processing: retains only the top N value counts and
     * converts internal maps to JAXB-compatible structures.
     */
    public void postProcessStatistics() {
        assert shadowStorage.values().stream().map(List::size).distinct().count() <= 1;

        // Value counts
        setMissingValueCountStatistics();
        setValueCountStatistics();
        // Cross-table value counts
        computeValueCountsOfValuePairs();
        // Affixes statistics
        setAffixesStatistics();
        // Dn parsing statistics
        setOUAttributeStatistics();
    }

    /**
     * Returns the statistics object.
     *
     * @return Shadow object class statistics.
     */
    public ShadowObjectClassStatisticsType getStatistics() {
        return statistics;
    }

    /**
     * Creates attribute statistics for the given attribute name.
     *
     * @param attrName Attribute item name.
     */
    private void createAttributeStatisticsIfNeeded(ItemName attrName) {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            if (attrName.equals(fromAttributeRef(stats.getRef()))) {
                return;
            }
        }
        ShadowAttributeStatisticsType newStats = new ShadowAttributeStatisticsType().ref(toAttributeRef(attrName));
        statistics.getAttribute().add(newStats);
    }

    /** Converts plain attribute name to an {@link ItemPathType} used in statistics beans. */
    private ItemPathType toAttributeRef(QName attrName) {
        return ShadowType.F_ATTRIBUTES.append(attrName).toBean();
    }

    /** Converts {@link ItemPathType} (assuming it's `attributes/xyz`) to a single attribute name. */
    private QName fromAttributeRef(ItemPathType attrRef) {
        return attrRef.getItemPath().rest().asSingleNameOrFail();
    }

    /**
     * Updates the shadow storage with attribute values from the provided shadow object.
     * If the number of value-count pairs for a particular key exceeds the allowed limit,
     * the entry is removed from the storage.
     */
    private void updateShadowStorage(ShadowType shadow) {
        for (Map.Entry<QName, LinkedList<List<?>>> entry : shadowStorage.entrySet()) {
            entry.getValue().add(ShadowUtil.getAttributeValues(shadow, entry.getKey()));
        }
    }

    /**
     * Calculates and sets the count of missing (null or empty) values for each attribute
     * in the statistics object, updating the corresponding statistics entry.
     */
    private void setMissingValueCountStatistics() {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            QName attrKey = fromAttributeRef(stats.getRef());
            int emptyCount = (int) shadowStorage.get(attrKey).stream()
                    .filter(list -> list == null || list.isEmpty())
                    .count();

            stats.setMissingValueCount(emptyCount);
        }
    }

    /**
     * Returns a map counting the occurrences of each unique single (size == 1) value
     * for the specified attribute key within the shadow storage.
     *
     * @param attrKey the QName key of the attribute to analyze
     * @return a map where keys are string representations of values and values are their counts
     */
    private Map<String, Integer> getValueCounts(QName attrKey) {
        Map<String, Integer> result = new HashMap<>();
        for (List<?> list : shadowStorage.get(attrKey)) {
            if (list.size() == 1) {
                String value = String.valueOf(list.get(0));
                result.merge(value, 1, Integer::sum);
            }
        }
        return result;
    }

    /**
     * Returns a map of the top N most frequent values from the provided value count map,
     * only if the highest count is greater than 1. The result is ordered by descending count.
     * <p>
     * @param valueCounts a map of values and their occurrence counts
     * @return a LinkedHashMap of the top N most frequent values and their counts, or an empty map if all counts are 1
     */
    private Map<String, Integer> getTopNValueCounts(Map<String, Integer> valueCounts) {
        int maxCount = 0;
        for (int count : valueCounts.values()) {
            if (count > maxCount) maxCount = count;
        }
        if (maxCount <= 1) return new LinkedHashMap<>();

        PriorityQueue<Map.Entry<String, Integer>> queue =
                new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<String, Integer> entry : valueCounts.entrySet()) {
            if (queue.size() < TOP_N_LIMIT) {
                queue.offer(entry);
            } else if (entry.getValue() > queue.peek().getValue()) {
                queue.poll();
                queue.offer(entry);
            }
        }

        // Collect to a LinkedHashMap in descending order
        List<Map.Entry<String, Integer>> topList = new ArrayList<>(queue);
        topList.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : topList) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Calculates and sets unique value counts and top N value frequencies for each attribute
     * in the statistics object, updating the corresponding statistics entry.
     */
    private void setValueCountStatistics() {
        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            QName attrKey = fromAttributeRef(stats.getRef());
            Map<String, Integer> valueCounts = getValueCounts(attrKey);

            stats.setUniqueValueCount(valueCounts.size());

            valueCounts = getTopNValueCounts(valueCounts);

            for (Map.Entry<String, Integer> entry : valueCounts.entrySet()) {
                stats.beginValueCount()
                        .value(entry.getKey())
                        .count(entry.getValue());
            }
        }
    }

    /**
     * Returns a list of attribute QNames that are eligible for cross-table value count analysis.
     * <p>
     * An attribute is included if its unique value count does not exceed a dynamically calculated threshold,
     * which is the lesser of a percentage of the total statistics size and a hard limit.
     */
    private List<QName> getAttributesForCrossTableValueCounts() {
        List<QName> indices = new LinkedList<>();
        double percentageThreshold = statistics.getSize() * VALUE_COUNT_PAIR_PERCENTAGE_LIMIT;

        for (ShadowAttributeStatisticsType attribute : statistics.getAttribute()) {
            if (attribute.getUniqueValueCount() <= VALUE_COUNT_PAIR_HARD_LIMIT &&
                    attribute.getUniqueValueCount() <= percentageThreshold) {
                indices.add(fromAttributeRef(attribute.getRef()));
            }
        }
        return indices;
    }

    /**
     * Computes and records the occurrence counts of all value pairs across
     * pairs of lists stored in {@code shadowStorage}. For every unique pair of
     * keys in {@code shadowStorage}, this method iterates over the corresponding
     * lists in parallel, counts how often each pair of non-null values appears
     * at the same position, and records these statistics using the {@code statistics} object.
     */
    private void computeValueCountsOfValuePairs() {
        List<QName> indices = getAttributesForCrossTableValueCounts();

        for (int x = 0; x < indices.size() - 1; x++) {
            for (int y = x + 1; y < indices.size(); y++) {
                LinkedList<List<?>> list1 = shadowStorage.get(indices.get(x));
                LinkedList<List<?>> list2 = shadowStorage.get(indices.get(y));
                assert list1.size() == list2.size();

                Map<String, Map<String, Integer>> pairCounts = new HashMap<>();
                for (int i = 0; i < list1.size(); i++) {
                    List<?> s1 = list1.get(i);
                    List<?> s2 = list2.get(i);
                    if (s1 != null && s1.size() == 1
                            && s2 != null && s2.size() == 1) {
                        pairCounts.computeIfAbsent(s1.get(0).toString(), k -> new HashMap<>())
                                .merge(s2.get(0).toString(), 1, Integer::sum);
                    }
                }

                if (!pairCounts.isEmpty()) {
                    ShadowAttributeTupleStatisticsType tuple = statistics.beginAttributeTuple()
                            .ref(toAttributeRef(indices.get(x)))
                            .ref(toAttributeRef(indices.get(y)));
                    for (Map.Entry<String, Map<String, Integer>> entry1 : pairCounts.entrySet()) {
                        for (Map.Entry<String, Integer> entry2 : entry1.getValue().entrySet()) {
                            tuple
                                    .beginTupleCount()
                                    .value(entry1.getKey())
                                    .value(entry2.getKey())
                                    .count(entry2.getValue());
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes the count of attribute values containing each affix for the given attribute key.
     *
     * @param attrKey the attribute key to analyze
     * @return map from affix to the number of occurrences in the attribute's values
     */
    private Map<String, Integer> getAffixesValueCounts(QName attrKey) {
        Map<String, Integer> result = new HashMap<>();
        List<Map.Entry<String, Pattern>> patterns = new ArrayList<>(AFFIX_PATTERNS.entrySet());
        LinkedList<List<?>> elements = shadowStorage.get(attrKey);
        if (elements == null) {
            return result;
        }
        for (List<?> inner : elements) {
            if (inner.size() != 1) continue;
            String str = inner.get(0).toString();
            for (Map.Entry<String, Pattern> e : patterns) {
                if (e.getValue().matcher(str).find()) {
                    result.merge(e.getKey(), 1, Integer::sum);
                }
            }
        }
        return result;
    }

    /**
     * Calculates and sets the statistics for affix occurrences in all shadow attribute values.
     * Updates the statistics structure with the count of each affix found.
     */
    private void setAffixesStatistics() {
        for (ShadowAttributeStatisticsType attribute : statistics.getAttribute()) {
            QName attrKey = fromAttributeRef(attribute.getRef());
            Map<String, Integer> affixCounts = getAffixesValueCounts(attrKey).entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));;
            for (Map.Entry<String, Integer> entry : affixCounts.entrySet()) {
                attribute.beginValuePatternCount()
                        .value(entry.getKey())
                        .count(entry.getValue());
            }
        }
    }

    private List<String> parseDNString(String dn) {
        List<String> ous = new ArrayList<>();
        for (String part : dn.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("OU=")) {
                ous.add(trimmed.substring(3));
            }
        }
        return ous;
    }

    private Map<String, Integer> getOUValueCounts(QName attrKey) {
        Map<String, Integer> result = new HashMap<>();
        LinkedList<List<?>> elements = shadowStorage.get(attrKey);
        if (elements == null) {
            return result;
        }
        for (List<?> inner : elements) {
            if (inner.size() != 1) continue;
            List<String> ous = parseDNString(inner.get(0).toString());
            for (String ou : ous) {
                result.merge(ou, 1, Integer::sum);
            }
        }
        return result;
    }

    private void setOUAttributeStatistics() {
        for (ShadowAttributeStatisticsType attribute : statistics.getAttribute()) {
            QName attrKey = fromAttributeRef(attribute.getRef());
            if (attrKey.toString().equalsIgnoreCase("dn") ||
                    attrKey.toString().equalsIgnoreCase("distinguishedName")) {
                Map<String, Integer> ouCounts = getOUValueCounts(attrKey).entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));
                for (Map.Entry<String, Integer> entry : ouCounts.entrySet()) {
                    attribute.beginValueCount()
                            .value(entry.getKey())
                            .count(entry.getValue());
                }
                attribute.setUniqueValueCount(ouCounts.size());
                return;
            }
        }
    }
}
