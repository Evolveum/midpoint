/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import java.util.*;
import java.util.regex.Pattern;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
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
     * The maximum percentage (as a fraction) of affix patterns allowed
     * when calculating affix statistics for attribute values.
     * * TODO: Make this configurable.
     */
    private static final double AFFIX_PERCENTAGE_LIMIT = 0.05;

    /**
     * Set of common prefixes to match in attribute values.
     */
    private static final Set<String> PREFIXES = new HashSet<>(Arrays.asList(
            "prod", "priv", "adm", "usr", "user", "ops", "svc", "int", "ext"
    ));

    /**
     * Set of common suffixes to match in attribute values.
     */
    private static final Set<String> SUFFIXES = new HashSet<>(Arrays.asList(
            "prod", "priv", "adm", "admin", "administrator", "usr", "user", "ops", "svc", "int", "ext"
    ));

    /**
     * Regular expression pattern for matching URLs.
     * Matches strings that start with "http://", "https://", or "www.", followed by non-whitespace characters.
     */
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://|www\\.)\\S+$",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Regular expression pattern for matching email addresses.
     * Matches simple email addresses of the form localpart@domain.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    /**
     * Regular expression pattern for matching phone numbers.
     * Matches phone numbers with optional leading '+', containing digits, spaces, dashes, or parentheses,
     * and with a length between 7 and 20 characters.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[\\d\\s\\-()]{7,20}$"
    );

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

        for (ShadowAttributeStatisticsType stats : statistics.getAttribute()) {
            QName attrKey = fromAttributeRef(stats.getRef());

            setMissingValueCountStatistics(attrKey, stats);
            setValueCountStatistics(attrKey, stats);
            if (attrKey.toString().equalsIgnoreCase("dn") || attrKey.toString().equalsIgnoreCase("distinguishedName")) {
                setDnStatistics(attrKey, stats);
            } else {
                setAffixStatistics(attrKey, stats);
            }
        }

        computeValueCountsOfValuePairs();
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
    private void setMissingValueCountStatistics(QName attrKey, ShadowAttributeStatisticsType stats) {
        int emptyCount = (int) shadowStorage.get(attrKey).stream()
                .filter(list -> list == null || list.isEmpty())
                .count();
        stats.setMissingValueCount(emptyCount);
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
        if (valueCounts.values().stream().max(Integer::compare).orElse(0) <= 1) {
            return new LinkedHashMap<>();
        }

        return valueCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_N_LIMIT)
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }

    /**
     * Calculates and sets unique value counts and top N value frequencies for each attribute
     * in the statistics object, updating the corresponding statistics entry.
     */
    private void setValueCountStatistics(QName attrKey, ShadowAttributeStatisticsType stats) {
        Map<String, Integer> valueCounts = getValueCounts(attrKey);
        stats.setUniqueValueCount(valueCounts.size());
        valueCounts = getTopNValueCounts(valueCounts);
        for (Map.Entry<String, Integer> entry : valueCounts.entrySet()) {
            stats.beginValueCount()
                    .value(entry.getKey())
                    .count(entry.getValue());
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
                    if (s1 != null && s1.size() == 1 && s2 != null && s2.size() == 1) {
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
     * Checks whether the given string matches the URL pattern.
     */
    private boolean isUrl(String str) {
        return str != null && URL_PATTERN.matcher(str).matches();
    }

    /**
     * Checks whether the given string matches the email address pattern.
     */
    private boolean isEmail(String str) {
        return str != null && EMAIL_PATTERN.matcher(str).matches();
    }

    /**
     * Checks whether the given string matches the phone number pattern.
     */
    private boolean isPhoneNumber(String str) {
        return str != null && PHONE_PATTERN.matcher(str).matches();
    }

    /**
     * Counts the occurrences of predefined affixes that either start or end the provided value.
     * Updates the {@code affixCounts} map with the counts for each affix found.
     */
    private void incrementVocabularyAffixCounts(
            String value,
            Map<ShadowValuePatternType, Map<String, Integer>> affixCounts
    ) {
        for (String affix : PREFIXES) {
            if (value.startsWith(affix)) {
                affixCounts
                        .computeIfAbsent(ShadowValuePatternType.PREFIX, k -> new HashMap<>())
                        .merge(affix, 1, Integer::sum);
            }
        }
        for (String affix : SUFFIXES) {
            if (value.endsWith(affix)) {
                affixCounts
                        .computeIfAbsent(ShadowValuePatternType.SUFFIX, k -> new HashMap<>())
                        .merge(affix, 1, Integer::sum);
            }
        }
    }

    /**
     * Counts the first and last alphanumeric tokens in the provided value, if they are not empty and not in {@code AFFIXES}.
     * Updates the {@code affixCounts} map with the counts for each token found.
     */
    private void incrementFirstAndLastAffixCounts(
            String value,
            Map<ShadowValuePatternType, Map<String, Integer>> affixCounts
    ) {
        String[] tokens = value.split("[^a-zA-Z0-9]+");
        if (tokens.length < 2) {
            return;
        }
        String firstToken = tokens[0];
        String lastToken = tokens[tokens.length - 1];
        if (!firstToken.isEmpty() && !PREFIXES.contains(firstToken)) {
            affixCounts
                    .computeIfAbsent(ShadowValuePatternType.FIRST_TOKEN, k -> new HashMap<>())
                    .merge(firstToken, 1, Integer::sum);
        }
        if (!lastToken.isEmpty() && !SUFFIXES.contains(lastToken)) {
            affixCounts
                    .computeIfAbsent(ShadowValuePatternType.LAST_TOKEN, k -> new HashMap<>())
                    .merge(lastToken, 1, Integer::sum);
        }
    }

    /**
     * Computes the counts of affix values for the given attribute key by analyzing string values
     * stored in {@code shadowStorage}. Ignores values that are detected as URLs, emails, or phone numbers.
     * Counts both vocabulary affixes and the first/last tokens of each value.
     */
    private void getAffixValueCounts(
            QName attrKey,
            Map<ShadowValuePatternType, Map<String, Integer>> vocabularyCounts,
            Map<ShadowValuePatternType, Map<String, Integer>> splitTokenCounts
    ) {
        List<List<?>> elements = shadowStorage.get(attrKey);
        if (elements == null) {
            return;
        }
        for (List<?> group : elements) {
            if (group == null || group.size() != 1) {
                continue;
            }
            Object item = group.get(0);
            if (!(item instanceof String)) {
                return;
            }
            String value = ((String) item).trim();
            if (isUrl(value) || isEmail(value) || isPhoneNumber(value)) {
                continue;
            }
            incrementVocabularyAffixCounts(value, vocabularyCounts);
            incrementFirstAndLastAffixCounts(value, splitTokenCounts);
        }
    }

    /**
     * Calculates and sets the statistics for affix occurrences in all shadow attribute values.
     * Updates the statistics structure with the count of each affix found.
     */
    private void setAffixStatistics(QName attrKey, ShadowAttributeStatisticsType stats) {
        Map<ShadowValuePatternType, Map<String, Integer>> vocabularyCounts = new HashMap<>();
        Map<ShadowValuePatternType, Map<String, Integer>> splitTokenCounts = new HashMap<>();
        getAffixValueCounts(attrKey, vocabularyCounts, splitTokenCounts);

        for (Map.Entry<ShadowValuePatternType, Map<String, Integer>> typeEntry : vocabularyCounts.entrySet()) {
            ShadowValuePatternType type = typeEntry.getKey();
            Map<String, Integer> valueCounts = typeEntry.getValue();
            for (Map.Entry<String, Integer> entry : valueCounts.entrySet()) {
                stats.beginValuePatternCount()
                        .value(entry.getKey())
                        .type(type)
                        .count(entry.getValue());
            }
        }

        for (Map.Entry<ShadowValuePatternType, Map<String, Integer>> typeEntry : splitTokenCounts.entrySet()) {
            ShadowValuePatternType type = typeEntry.getKey();
            Map<String, Integer> valueCounts = typeEntry.getValue();
            if (valueCounts.size() > AFFIX_PERCENTAGE_LIMIT * statistics.getSize()) {
                continue;
            }
            for (Map.Entry<String, Integer> entry : valueCounts.entrySet()) {
                stats.beginValuePatternCount()
                        .value(entry.getKey())
                        .type(type)
                        .count(entry.getValue());
            }
        }
    }

    /**
     * Parses the given distinguished name (DN) string to extract the suffix starting with the first "OU" (organizational unit) RDN.
     * <p>
     * For example, for the DN {@code "CN=John Doe,OU=Users,DC=example,DC=com"}, this method will return
     * {@code "OU=Users,DC=example,DC=com"}.
     */
    private String parseDNSuffix(String dn) {
        try {
            LdapName ldapName = new LdapName(dn);
            List<Rdn> rdns = ldapName.getRdns();
            int ouIndex = -1;
            for (int i = rdns.size() - 1; i >= 0; i--) {
                Rdn rdn = rdns.get(i);
                if (rdn.getType().equalsIgnoreCase("OU")) {
                    ouIndex = i;
                    break;
                }
            }
            if (ouIndex == -1) {
                return null;
            }
            List<Rdn> suffixRdns = rdns.subList(0, ouIndex+1);
            LdapName suffixName = new LdapName(suffixRdns);
            return suffixName.toString();
        } catch (InvalidNameException e) {
            return null;
        }
    }

    /**
     * Counts the occurrences of organizational unit (OU) suffixes for the elements associated with the specified attribute key.
     * <p>
     * The method uses {@link #parseDNSuffix(String)} to extract the OU suffix from each DN,
     * then counts the occurrences of each unique OU suffix.
     */
    private Map<String, Integer> getDNSuffixValueCounts(QName attrKey) {
        Map<String, Integer> result = new HashMap<>();
        LinkedList<List<?>> elements = shadowStorage.get(attrKey);
        if (elements == null) {
            return result;
        }
        for (List<?> inner : elements) {
            if (inner.size() != 1) continue;
            String dnSuffix = parseDNSuffix(inner.get(0).toString());
            if (dnSuffix != null) {
                result.merge(dnSuffix, 1, Integer::sum);
            }
        }
        return result;
    }

    /**
     * Calculates and sets statistics for organizational unit (OU) values found in DN attributes.
     * For each DN or distinguishedName attribute, it counts occurrences of each OU value, sorts them by frequency,
     * and updates the statistics object with the counts and the number of unique OUs.
     */
    private void setDnStatistics(QName attrKey, ShadowAttributeStatisticsType stats) {
        Map<String, Integer> ouCounts = getDNSuffixValueCounts(attrKey);
        for (Map.Entry<String, Integer> entry : ouCounts.entrySet()) {
            stats.beginValuePatternCount()
                    .value(entry.getKey())
                    .type(ShadowValuePatternType.DN_SUFFIX)
                    .count(entry.getValue());
        }
    }
}
