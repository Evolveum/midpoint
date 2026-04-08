/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.namespace.QName;

/**
 * Computes statistics for shadow objects using incremental aggregation.
 * Does not need to care about the timestamp and the coverage.
 *
 * Aggregates counts during {@link #process(ShadowType)} rather than storing
 * all raw attribute values in memory, making it suitable for large datasets.
 */
@VisibleForTesting
public class ObjectClassStatisticsComputer {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectClassStatisticsComputer.class);

    /** Top-N value counts to retain per attribute (spec: VALUECOUNT_TOP_N = 10). */
    private static final int VALUECOUNT_TOP_N = 10;

    /** Minimum repeat count for a value to appear in Top-N output (spec: VALUECOUNT_MIN_REPEAT = 2). */
    private static final int VALUECOUNT_MIN_REPEAT = 2;

    /** Top-N DN suffix patterns to retain per attribute (spec: DN_SUFFIX_TOP_N = 20). */
    private static final int DN_SUFFIX_TOP_N = 20;

    /** Top-N first/last token patterns to retain per token type (spec: FIRST_LAST_TOP_N = 10). */
    private static final int FIRST_LAST_TOP_N = 10;

    /** Regular expression pattern for token delimiters. */
    private static final String DELIMITERS = "[-_:*#+.]+";

    /** Compiled pattern for token delimiters (used by Matcher). */
    private static final Pattern DELIMITER_PATTERN = Pattern.compile(DELIMITERS);

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

    /** Attribute local names that should be treated as DN attributes. */
    private static final Set<String> DN_ATTRIBUTE_LOCAL_NAMES = Set.of("dn", "distinguishedname");

    /** Per-attribute incremental aggregation state, insertion-ordered to match attribute[] output. */
    private final Map<QName, AttributeAggregation> aggregations = new LinkedHashMap<>();

    /** JAXB statistics object being built. */
    private final ShadowObjectClassStatisticsType statistics = new ShadowObjectClassStatisticsType();

    /**
     * Constructs a new statistics computer for the given object class definition.
     *
     * @param objectClassDef Resource object class definition.
     */
    public ObjectClassStatisticsComputer(ResourceObjectClassDefinition objectClassDef) {
        for (ShadowAttributeDefinition<?, ?, ?, ?> attrDef : objectClassDef.getAttributeDefinitions()) {
            ItemName attrName = attrDef.getItemName();
            if (!aggregations.containsKey(attrName)) {
                boolean isDn = isDnAttribute(attrName);
                aggregations.put(attrName, new AttributeAggregation(isDn));
                statistics.getAttribute().add(
                        new ShadowAttributeStatisticsType().ref(toAttributeRef(attrName)));
            }
        }
    }

    /**
     * Processes the given shadow object, incrementally updating aggregated counts.
     */
    public void process(ShadowType shadow) {
        statistics.setSize(statistics.getSize() + 1);
        for (Map.Entry<QName, AttributeAggregation> entry : aggregations.entrySet()) {
            List<?> values = ShadowUtil.getAttributeValues(shadow, entry.getKey());
            aggregateAttribute(entry.getValue(), values);
        }
    }

    /**
     * Performs post-processing: converts aggregated counts into JAXB-compatible structures,
     * applying Top-N limits.
     */
    public void postProcessStatistics() {
        for (Iterator<ShadowAttributeStatisticsType> statisticsIterator = statistics.getAttribute().iterator(); statisticsIterator.hasNext(); ) {
            ShadowAttributeStatisticsType statisticsAttribute = statisticsIterator.next();
            QName attrKey = fromAttributeRef(statisticsAttribute.getRef());
            AttributeAggregation attributeAggregation = aggregations.get(attrKey);
            if (attributeAggregation == null) {
                statisticsIterator.remove();
                continue;
            }

            statisticsAttribute.setMissingValueCount(attributeAggregation.missingCount);
            statisticsAttribute.setUniqueValueCount(attributeAggregation.valueCounts.size());

            emitTopNValueCounts(attributeAggregation.valueCounts, statisticsAttribute);

            if (attributeAggregation.isDnAttribute) {
                emitTopNPatterns(attributeAggregation.dnSuffixCounts, ShadowValuePatternType.DN_SUFFIX, DN_SUFFIX_TOP_N, statisticsAttribute);
            } else {
                emitTopNPatterns(attributeAggregation.firstTokenCounts, ShadowValuePatternType.FIRST_TOKEN, FIRST_LAST_TOP_N, statisticsAttribute);
                emitTopNPatterns(attributeAggregation.lastTokenCounts, ShadowValuePatternType.LAST_TOKEN, FIRST_LAST_TOP_N, statisticsAttribute);
            }

            if (statisticsAttribute.getMissingValueCount() == statistics.getSize()
                    || (statisticsAttribute.getValueCount().isEmpty() && statisticsAttribute.getValuePatternCount().isEmpty())) {
                statisticsIterator.remove();
            }
        }
        statistics.getAttribute();
        statistics.getAttributeTuple();
    }

    /**
     * Returns the statistics object.
     */
    public ShadowObjectClassStatisticsType getStatistics() {
        return statistics;
    }

    /**
     * Aggregates a single attribute's values from one shadow into the running counts.
     */
    private void aggregateAttribute(AttributeAggregation agg, List<?> values) {
        if (values == null || values.isEmpty()) {
            agg.missingCount++;
            return;
        }

        if (values.size() > 1) {
            return;
        }

        Object rawValue = values.get(0);
        if (rawValue == null) {
            agg.missingCount++;
            return;
        }

        String stringValue = String.valueOf(rawValue).trim();
        agg.valueCounts.merge(stringValue, 1, Integer::sum);

        if (agg.isDnAttribute) {
            aggregateDnSuffix(agg, stringValue);
        } else if (rawValue instanceof String) {
            aggregateTokenPatterns(agg, stringValue);
        }
    }

    /**
     * Extracts the DN suffix and increments its count.
     */
    private void aggregateDnSuffix(AttributeAggregation agg, String dnValue) {
        String suffix = parseDNSuffix(dnValue);
        if (suffix != null) {
            agg.dnSuffixCounts.merge(suffix, 1, Integer::sum);
        }
    }

    /**
     * Extracts first/last tokens (including their adjacent delimiter) and increments their counts.
     * Skips values that look like URLs, emails, or phone numbers.
     *
     * For example, "prod-server01" yields firstToken="prod-" and lastToken="-server01"
     */
    private void aggregateTokenPatterns(AttributeAggregation agg, String value) {
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
     * Emits Top-N value counts into the statistics, only if at least one value
     * has count >= VALUECOUNT_MIN_REPEAT
     */
    private void emitTopNValueCounts(Map<String, Integer> valueCounts, ShadowAttributeStatisticsType stats) {
        int maxCount = valueCounts.values().stream()
                .max(Integer::compare)
                .orElse(0);
        if (maxCount < VALUECOUNT_MIN_REPEAT) {
            return;
        }

        topN(valueCounts, VALUECOUNT_TOP_N)
                .forEach(entry -> stats.beginValueCount()
                        .value(entry.getKey())
                        .count(entry.getValue()));
    }

    /**
     * Emits Top-N pattern counts of a given type into the statistics.
     */
    private void emitTopNPatterns(
            Map<String, Integer> counts,
            ShadowValuePatternType type,
            int limit,
            ShadowAttributeStatisticsType stats) {
        topN(counts, limit)
                .forEach(entry -> stats.beginValuePatternCount()
                        .value(entry.getKey())
                        .type(type)
                        .count(entry.getValue()));
    }

    /**
     * Returns top entries from the given map, sorted by value descending.
     * If the map size is within the limit, all entries are returned.
     * If the map size exceeds the limit, entries with count > 1 are kept only when
     * their number is lower than the limit (concentrated clusters). Otherwise the
     * pattern is too diffuse and an empty list is returned.
     */
    private static List<Map.Entry<String, Integer>> topN(Map<String, Integer> map, int limit) {
        if (map.size() <= limit) {
            return map.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toList());
        }
        long repeatingCount = map.values().stream().filter(v -> v > 1).count();
        if (repeatingCount >= limit) {
            return List.of();
        }
        return map.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /** Checks whether the given string matches the URL pattern. */
    private boolean isUrl(String str) {
        return str != null && URL_PATTERN.matcher(str).matches();
    }

    /** Checks whether the given string matches the email address pattern. */
    private boolean isEmail(String str) {
        return str != null && EMAIL_PATTERN.matcher(str).matches();
    }

    /** Checks whether the given string matches the phone number pattern. */
    private boolean isPhoneNumber(String str) {
        return str != null && PHONE_PATTERN.matcher(str).matches();
    }

    /**
     * Determines whether the given attribute should be treated as a DN attribute
     * based on its local name (case-insensitive).
     */
    private static boolean isDnAttribute(QName attrName) {
        return DN_ATTRIBUTE_LOCAL_NAMES.contains(attrName.getLocalPart().toLowerCase());
    }

    /**
     * Parses the given distinguished name (DN) string to extract the suffix starting
     * with the first "OU" or "O" (organizational unit) RDN (case-insensitive).
     */
    private String parseDNSuffix(String dn) {
        try {
            LdapName ldapName = new LdapName(dn);
            List<Rdn> rdns = ldapName.getRdns();
            int ouIndex = -1;
            for (int i = rdns.size() - 1; i >= 0; i--) {
                String type = rdns.get(i).getType();
                if (type.equalsIgnoreCase("ou") || type.equalsIgnoreCase("o")) {
                    ouIndex = i;
                    break;
                }
            }
            if (ouIndex == -1) {
                return null;
            }
            return new LdapName(rdns.subList(0, ouIndex + 1)).toString();
        } catch (InvalidNameException e) {
            LOGGER.trace("Failed to parse DN '{}': {}", dn, e.getMessage());
            return null;
        }
    }

    /** Converts plain attribute name to an {@link ItemPathType} used in statistics beans. */
    private static ItemPathType toAttributeRef(QName attrName) {
        return ShadowType.F_ATTRIBUTES.append(attrName).toBean();
    }

    /** Converts {@link ItemPathType} (assuming it's `attributes/xyz`) to a single attribute name. */
    private static QName fromAttributeRef(ItemPathType attrRef) {
        return attrRef.getItemPath().rest().asSingleNameOrFail();
    }

    /**
     * Holds incrementally aggregated counts for a single attribute.
     * Replaces the memory-intensive raw value storage.
     */
    private static class AttributeAggregation {
        int missingCount;
        final Map<String, Integer> valueCounts = new HashMap<>();
        final Map<String, Integer> dnSuffixCounts = new HashMap<>();
        final Map<String, Integer> firstTokenCounts = new HashMap<>();
        final Map<String, Integer> lastTokenCounts = new HashMap<>();
        final boolean isDnAttribute;

        AttributeAggregation(boolean isDnAttribute) {
            this.isDnAttribute = isDnAttribute;
        }
    }
}
