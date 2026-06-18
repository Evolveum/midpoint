/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowValuePatternType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractStatisticsComputer<K> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractStatisticsComputer.class);

    /** Regular expression pattern for token delimiters. */
    private static final String DELIMITERS = "[-_:*#+.()\\[\\]]+";

    /** Compiled pattern for token delimiters (used by Matcher). */
    private static final Pattern DELIMITER_PATTERN = Pattern.compile(DELIMITERS);

    /**
     * Regular expression pattern for matching URLs.
     * Matches strings that start with "http://", "https://", or "www.", followed by non-whitespace characters.
     */
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://|www\\.)\\S+$", Pattern.CASE_INSENSITIVE);

    /**
     * Regular expression pattern for matching email addresses.
     * Matches simple email addresses of the form localpart@domain.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    /**
     * Regular expression pattern for matching phone numbers.
     * Matches phone numbers with optional leading '+', containing digits, spaces, dashes, or parentheses,
     * and with a length between 7 and 20 characters.
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[\\d\\s\\-()]{7,20}$");

    /** Attribute local names that should be treated as DN attributes. */
    private static final Set<String> DN_ATTRIBUTE_LOCAL_NAMES = Set.of("dn", "distinguishedname");

    /** Per-attribute incremental aggregation state. Attribute order is kept separately in attributeOrder. */
    private final Map<K, AttributeAggregation> aggregations = new ConcurrentHashMap<>();

    /** Item processing order matching the resource object class definition. */
    private final List<K> itemOrder = new ArrayList<>();

    /** Number of processed shadows. */
    private final AtomicInteger size = new AtomicInteger();

    /** JAXB statistics object being built. */
    private final ShadowObjectClassStatisticsType statistics = new ShadowObjectClassStatisticsType();

    protected void registerItem(K key, ItemPathType ref, boolean dnAttribute) {
        AttributeAggregation previous = aggregations.putIfAbsent(key, new AttributeAggregation(dnAttribute));
        if (previous == null) {
            itemOrder.add(key);
            statistics.getAttribute().add(new ShadowAttributeStatisticsType().ref(ref));
        }
    }

    protected List<K> getItemOrder() {
        return itemOrder;
    }

    protected void incrementSize() {
        size.incrementAndGet();
    }

    protected void aggregateItem(K key, List<?> values) {
        AttributeAggregation agg = aggregations.get(key);
        if (agg != null) {
            aggregateValues(agg, values);
        }
    }

    protected void aggregateStringValue(K key, String value) {
        AttributeAggregation agg = aggregations.get(key);
        if (agg == null) {
            return;
        }

        if (value == null || value.isBlank()) {
            agg.missingCount.incrementAndGet();
            return;
        }

        agg.valueCounts.merge(value, 1, Integer::sum);
        aggregateTokenPatterns(agg, value);
    }

    protected void markMissing(K key) {
        AttributeAggregation agg = aggregations.get(key);
        if (agg != null) {
            agg.missingCount.incrementAndGet();
        }
    }

    /**
     * Aggregates values from one item into the running counts.
     */
    private void aggregateValues(AttributeAggregation agg, List<?> values) {
        if (values == null || values.isEmpty()) {
            agg.missingCount.incrementAndGet();
            return;
        }

        if (values.size() > 1) {
            return;
        }

        Object rawValue = values.get(0);
        if (rawValue == null) {
            agg.missingCount.incrementAndGet();
            return;
        }

        String value = String.valueOf(rawValue).trim();
        agg.valueCounts.merge(value, 1, Integer::sum);

        if (agg.isDnAttribute) {
            String suffix = parseDNSuffix(value);
            if (suffix != null) {
                agg.dnSuffixCounts.merge(suffix, 1, Integer::sum);
            }
        } else if (rawValue instanceof String) {
            aggregateTokenPatterns(agg, value);
        }
    }

    /**
     * Extracts first/last tokens (including their adjacent delimiter) and increments their counts.
     * Skips values that look like URLs or phone numbers. For email addresses, splits on "@" and
     * records the domain suffix (e.g. "@example.com") as a last token.
     * Only "inside" delimiters are considered: any leading or trailing delimiter sequences are
     * stripped before processing, so they do not influence the token boundaries.
     * <p>
     * For example, "-prod-server01-" yields firstToken="-prod-" and lastToken="-server01-"
     */
    private void aggregateTokenPatterns(AttributeAggregation agg, String value) {
        if (isUrl(value) || isPhoneNumber(value)) {
            return;
        }

        if (isEmail(value)) {
            int atIndex = value.indexOf('@');
            if (atIndex >= 0) {
                agg.lastTokenCounts.merge(value.substring(atIndex), 1, Integer::sum);
            }
            return;
        }

        // Strip leading and trailing delimiter sequences to find only "inside" delimiters.
        // leadingDelimLength is used as an offset to extract tokens from the original value,
        // so that tokens preserve any leading/trailing delimiters of the original string.
        String withoutLeading = value.replaceAll("^" + DELIMITERS, "");
        int leadingDelimLength = value.length() - withoutLeading.length();
        String inner = withoutLeading.replaceAll(DELIMITERS + "$", "");

        Matcher matcher = DELIMITER_PATTERN.matcher(inner);
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

        // firstToken: text + trailing delimiter, mapped back to original value (includes leading delimiters)
        if (firstDelimStart > 0) {
            agg.firstTokenCounts.merge(value.substring(0, leadingDelimLength + firstDelimEnd), 1, Integer::sum);
        }

        // lastToken: leading delimiter + text, mapped back to original value (includes trailing delimiters)
        if (lastDelimStart < inner.length() - 1) {
            agg.lastTokenCounts.merge(value.substring(leadingDelimLength + lastDelimStart), 1, Integer::sum);
        }
    }

    /**
     * Performs post-processing: converts aggregated counts into JAXB-compatible structures.
     * All values and patterns are emitted without any filtering or top-N limits.
     */
    public synchronized void postProcessStatistics() {
        statistics.setSize(size.get());

        for (var iterator = statistics.getAttribute().iterator(); iterator.hasNext(); ) {
            ShadowAttributeStatisticsType stats = iterator.next();

            K key = fromRef(stats.getRef());
            AttributeAggregation agg = aggregations.get(key);

            if (agg == null) {
                iterator.remove();
                continue;
            }

            stats.setMissingValueCount(agg.missingCount.get());
            stats.setUniqueValueCount(agg.valueCounts.size());

            emitAllValueCounts(agg.valueCounts, stats);

            if (agg.isDnAttribute) {
                emitAllPatterns(agg.dnSuffixCounts, ShadowValuePatternType.DN_SUFFIX, stats);
            } else {
                emitAllPatterns(agg.firstTokenCounts, ShadowValuePatternType.FIRST_TOKEN, stats);
                emitAllPatterns(agg.lastTokenCounts, ShadowValuePatternType.LAST_TOKEN, stats);
            }

            afterPostProcessAttribute(stats, iterator);
        }
    }

    protected void afterPostProcessAttribute(
            ShadowAttributeStatisticsType stats,
            Iterator<ShadowAttributeStatisticsType> iterator) {
    }

    public ShadowObjectClassStatisticsType getStatistics() {
        return statistics;
    }

    protected ShadowObjectClassStatisticsType getStatisticsObject() {
        return statistics;
    }

    protected abstract K fromRef(ItemPathType ref);

    /**
     * Emits all value counts into the statistics, sorted by count descending.
     */
    private void emitAllValueCounts(
            @NotNull Map<String, Integer> valueCounts,
            ShadowAttributeStatisticsType stats) {

        List<Map.Entry<String, Integer>> snapshot = new ArrayList<>(valueCounts.entrySet());

        snapshot.stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> stats.beginValueCount()
                        .value(entry.getKey())
                        .count(entry.getValue()));
    }

    /**
     * Emits all pattern counts of a given type into the statistics, sorted by count descending.
     */
    private void emitAllPatterns(
            @NotNull Map<String, Integer> counts,
            ShadowValuePatternType type,
            ShadowAttributeStatisticsType stats) {

        List<Map.Entry<String, Integer>> snapshot = new ArrayList<>(counts.entrySet());

        snapshot.stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> stats.beginValuePatternCount()
                        .value(entry.getKey())
                        .type(type)
                        .count(entry.getValue()));
    }

    /** Checks whether the given string matches the URL pattern. */
    private boolean isUrl(String value) {
        return value != null && URL_PATTERN.matcher(value).matches();
    }

    /** Checks whether the given string matches the email address pattern. */
    private boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    /** Checks whether the given string matches the phone number pattern. */
    private boolean isPhoneNumber(String value) {
        return value != null && PHONE_PATTERN.matcher(value).matches();
    }

    /**
     * Determines whether the given attribute should be treated as a DN attribute
     * based on its local name (case-insensitive).
     */
    protected static boolean isDnAttribute(@NotNull QName attrName) {
        return DN_ATTRIBUTE_LOCAL_NAMES.contains(attrName.getLocalPart().toLowerCase());
    }

    /**
     * Parses the given distinguished name (DN) string to extract the suffix starting
     * with the first "OU" or "O" (organizational unit) RDN (case-insensitive).
     */
    private @Nullable String parseDNSuffix(String dn) {
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
    protected ItemPathType toAttributeRef(QName attrName) {
        return ShadowType.F_ATTRIBUTES.append(attrName).toBean();
    }

    private static class AttributeAggregation {

        private final AtomicInteger missingCount = new AtomicInteger();

        private final ConcurrentMap<String, Integer> valueCounts = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Integer> dnSuffixCounts = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Integer> firstTokenCounts = new ConcurrentHashMap<>();
        private final ConcurrentMap<String, Integer> lastTokenCounts = new ConcurrentHashMap<>();

        private final boolean isDnAttribute;

        private AttributeAggregation(boolean isDnAttribute) {
            this.isDnAttribute = isDnAttribute;
        }
    }
}
