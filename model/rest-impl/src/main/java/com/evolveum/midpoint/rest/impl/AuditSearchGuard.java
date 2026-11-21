/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.rest.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConversionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Validates and normalizes audit record queries coming through REST, so that a REST client cannot overload
 * the audit database (or reach data it is not authorized to see).
 *
 * The rules are tied to the (native PostgreSQL) audit database layout: `ma_audit_event` is partitioned
 * by `timestamp` and has B-tree indexes only on `timestamp`, `eventIdentifier`, `sessionIdentifier`,
 * `requestIdentifier`, `targetOid` and the primary key `(id, timestamp)`. Any other filter is a scan.
 *
 * Rules (violations are rejected, the query is never silently "fixed" except for adding a deterministic ordering):
 *
 * . `paging/maxSize` is required and limited by {@link Limits#maxPageSize()}.
 * . `paging/offset` is limited by {@link Limits#maxOffset()}.
 * . Ordering is allowed only by `timestamp` (with `repoId` as an optional tie-breaker) or by `repoId`.
 * When no ordering is specified, `timestamp ascending, repoId ascending` is used; when only `timestamp` is specified,
 * `repoId` in the same direction is added as the tie-breaker. This makes paging deterministic.
 * . The top-level conjunction (AND) of the filter must contain an indexed "anchor" (see {@link #isIndexedAnchor(ObjectFilter)}),
 * unless {@link Limits#requireIndexedAnchor()} is switched off.
 * . A query anchored by `timestamp` only must not cover a longer time range than {@link Limits#maxTimeWindow()}:
 * from the (highest) lower bound to the (lowest) upper bound, or to the current time if there is no upper bound.
 * An anchored `timestamp` range alone does not prevent the database from walking the whole range, e.g. when the query
 * contains a selective condition on a non-indexed item. Queries anchored by an indexed identifier (see
 * {@link #isEqualityAnchor(ObjectFilter)}) are not limited, as they are resolved by a different index.
 * . Paths with special symbols - most notably dereferencing (`targetRef/@/name`) - and reference target filters are rejected,
 * as they are evaluated by joining into the main repository tables without any authorization.
 * . Filter expressions are rejected; only plain values are supported.
 * . Only filter types meaningful for audit records are accepted.
 */
public class AuditSearchGuard {

    private static final Trace LOGGER = TraceManager.getTrace(AuditSearchGuard.class);

    /** Configuration keys, relative to the `midpoint.webApplication` configuration section. */
    static final String CONFIG_MAX_PAGE_SIZE = "restAudit.maxPageSize";
    static final String CONFIG_MAX_OFFSET = "restAudit.maxOffset";
    static final String CONFIG_REQUIRE_INDEXED_ANCHOR = "restAudit.requireIndexedAnchor";
    static final String CONFIG_MAX_TIME_WINDOW = "restAudit.maxTimeWindow";

    /** Equality on these items is an "anchor": it is resolved by an index. */
    private static final Set<ItemName> INDEXED_EQUALITY_ITEMS = Set.of(
            AuditEventRecordType.F_EVENT_IDENTIFIER,
            AuditEventRecordType.F_REQUEST_IDENTIFIER,
            AuditEventRecordType.F_SESSION_IDENTIFIER,
            AuditEventRecordType.F_REPO_ID);

    /**
     * Limits enforced by the guard.
     *
     * @param maxPageSize Maximal allowed value of `paging/maxSize`.
     * @param maxOffset Maximal allowed value of `paging/offset`.
     * @param requireIndexedAnchor Whether the filter must contain an indexed anchor.
     * @param maxTimeWindow Maximal time range covered by a query anchored by `timestamp` only; null means no limit.
     */
    public record Limits(int maxPageSize, int maxOffset, boolean requireIndexedAnchor, @Nullable Duration maxTimeWindow) {

        public static final int DEFAULT_MAX_PAGE_SIZE = 1000;
        public static final int DEFAULT_MAX_OFFSET = 10_000;
        public static final Duration DEFAULT_MAX_TIME_WINDOW = Duration.ofDays(31);

        public static final Limits DEFAULT =
                new Limits(DEFAULT_MAX_PAGE_SIZE, DEFAULT_MAX_OFFSET, true, DEFAULT_MAX_TIME_WINDOW);

        /**
         * Reads the limits from the `midpoint.webApplication` configuration section.
         * The values can be set in `config.xml`, or - e.g. in containerized deployments - via system properties
         * (`-Dmidpoint.webApplication.restAudit.maxPageSize=500`) or the corresponding environment variables
         * understood by the startup script (`MP_SET_midpoint_webApplication_restAudit_maxPageSize=500`).
         */
        public static @NotNull Limits fromConfiguration(@Nullable Configuration webAppConfiguration) {
            if (webAppConfiguration == null) {
                return DEFAULT;
            }
            return new Limits(
                    positiveOrDefault(webAppConfiguration, CONFIG_MAX_PAGE_SIZE, DEFAULT_MAX_PAGE_SIZE),
                    nonNegativeOrDefault(webAppConfiguration, CONFIG_MAX_OFFSET, DEFAULT_MAX_OFFSET),
                    booleanOrDefault(webAppConfiguration, CONFIG_REQUIRE_INDEXED_ANCHOR, true),
                    parseMaxTimeWindow(webAppConfiguration.getString(CONFIG_MAX_TIME_WINDOW)));
        }

        /**
         * Parses the maximal time window: an ISO-8601 duration with days, hours, minutes or seconds
         * (e.g. `P31D`, `PT12H`). Zero (`0`, `P0D`, `PT0S`) switches the limit off.
         */
        public static @Nullable Duration parseMaxTimeWindow(@Nullable String value) {
            if (value == null || value.isBlank()) {
                return DEFAULT_MAX_TIME_WINDOW;
            }
            String trimmed = value.trim();
            if ("0".equals(trimmed)) {
                return null;
            }
            try {
                Duration duration = Duration.parse(trimmed);
                if (duration.isZero()) {
                    return null;
                } else if (duration.isNegative()) {
                    LOGGER.warn("Invalid value of {}: {}, using the default of {}",
                            CONFIG_MAX_TIME_WINDOW, value, DEFAULT_MAX_TIME_WINDOW);
                    return DEFAULT_MAX_TIME_WINDOW;
                } else {
                    return duration;
                }
            } catch (DateTimeParseException e) {
                LOGGER.warn("Invalid value of {}: {} (expected an ISO-8601 duration like P31D or PT12H), "
                        + "using the default of {}", CONFIG_MAX_TIME_WINDOW, value, DEFAULT_MAX_TIME_WINDOW);
                return DEFAULT_MAX_TIME_WINDOW;
            }
        }

        private static int positiveOrDefault(Configuration configuration, String key, int defaultValue) {
            int value = intOrDefault(configuration, key, defaultValue);
            if (value <= 0) {
                LOGGER.warn("Invalid value of {}: {}, using the default of {}", key, value, defaultValue);
                return defaultValue;
            }
            return value;
        }

        private static int nonNegativeOrDefault(Configuration configuration, String key, int defaultValue) {
            int value = intOrDefault(configuration, key, defaultValue);
            if (value < 0) {
                LOGGER.warn("Invalid value of {}: {}, using the default of {}", key, value, defaultValue);
                return defaultValue;
            }
            return value;
        }

        /** A value that cannot be converted must not prevent midPoint from starting; the default is used instead. */
        private static int intOrDefault(Configuration configuration, String key, int defaultValue) {
            try {
                return configuration.getInt(key, defaultValue);
            } catch (ConversionException e) {
                LOGGER.warn("Invalid value of {}: {}, using the default of {}",
                        key, configuration.getString(key), defaultValue);
                return defaultValue;
            }
        }

        private static boolean booleanOrDefault(Configuration configuration, String key, boolean defaultValue) {
            try {
                return configuration.getBoolean(key, defaultValue);
            } catch (ConversionException e) {
                LOGGER.warn("Invalid value of {}: {}, using the default of {}",
                        key, configuration.getString(key), defaultValue);
                return defaultValue;
            }
        }
    }

    /** The query was rejected by the guard. The message is meant for the REST client. */
    public static class RejectedException extends Exception {
        public RejectedException(String message) {
            super(message);
        }

        public RejectedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @NotNull private final Limits limits;
    @NotNull private final QueryFactory queryFactory;

    public AuditSearchGuard(@NotNull Limits limits, @NotNull QueryFactory queryFactory) {
        this.limits = limits;
        this.queryFactory = queryFactory;
    }

    /**
     * Checks the query and normalizes its paging (ordering) in place.
     *
     * @return true if the query contains an indexed anchor
     */
    public boolean checkAndNormalize(@NotNull ObjectQuery query) throws RejectedException {
        ObjectFilter filter = query.getFilter();
        checkFilter(filter);
        boolean anchored = hasIndexedAnchor(filter);
        if (!anchored && limits.requireIndexedAnchor()) {
            throw new RejectedException("The filter must contain (at the top level, possibly in a conjunction) a condition "
                    + "on an indexed item: 'timestamp >= ...' (or '>'), or equality on 'eventIdentifier', "
                    + "'requestIdentifier', 'sessionIdentifier', 'repoId', or 'targetRef' with an OID.");
        }
        checkTimeWindow(filter);
        query.setPaging(checkAndNormalizePaging(query.getPaging()));
        return anchored;
    }

    private void checkTimeWindow(@Nullable ObjectFilter filter) throws RejectedException {
        Duration maxTimeWindow = limits.maxTimeWindow();
        if (maxTimeWindow == null) {
            return;
        }
        List<ObjectFilter> conjuncts = topLevelConjuncts(filter);
        if (conjuncts.stream().anyMatch(AuditSearchGuard::isEqualityAnchor)) {
            return; // resolved by a selective index, the time range does not matter
        }
        Long from = null;
        Long to = null;
        for (ObjectFilter conjunct : conjuncts) {
            if (conjunct instanceof GreaterFilter<?> greaterFilter && isTimestampBound(greaterFilter)) {
                for (Long millis : timestampValues(greaterFilter)) {
                    from = from == null ? millis : Math.max(from, millis);
                }
            } else if (conjunct instanceof LessFilter<?> lessFilter && isTimestampBound(lessFilter)) {
                for (Long millis : timestampValues(lessFilter)) {
                    to = to == null ? millis : Math.min(to, millis);
                }
            }
        }
        if (from == null) {
            // No lower bound: the query was either rejected by the anchor rule,
            // or anchors are not required at all (a deliberately permissive configuration).
            return;
        }
        long now = System.currentTimeMillis();
        long end = to != null ? Math.min(to, now) : now; // there are no records from the future
        long window = end - from;
        if (end >= from && window < 0) {
            window = Long.MAX_VALUE; // overflow: the lower bound is extremely far in the past
        }
        if (window > maxTimeWindow.toMillis()) {
            throw new RejectedException("The 'timestamp' range of the query (from " + Instant.ofEpochMilli(from)
                    + " to " + (to != null && to < now ? Instant.ofEpochMilli(to) : "now") + ", i.e. "
                    + formatDuration(window) + ") is longer than the allowed maximum of "
                    + formatDuration(maxTimeWindow.toMillis())
                    + ". Narrow the range (e.g. add 'timestamp < ...') and query the time windows one by one, "
                    + "or add equality on an indexed identifier ('eventIdentifier', 'requestIdentifier', "
                    + "'sessionIdentifier' or 'repoId'), or 'targetRef' with an OID; ranges on these do not lift the limit.");
        }
    }

    private static boolean isTimestampBound(ComparativeFilter<?> filter) {
        return filter.getFullPath().equivalent(AuditEventRecordType.F_TIMESTAMP)
                && filter.getRightHandSidePath() == null
                && hasValues(filter);
    }

    private static List<Long> timestampValues(ComparativeFilter<?> filter) throws RejectedException {
        List<Long> millis = new ArrayList<>();
        for (PrismPropertyValue<?> value : filter.getValues()) {
            if (value.getRealValue() instanceof XMLGregorianCalendar calendar) {
                millis.add(XmlTypeConverter.toMillis(calendar));
            } else {
                throw new RejectedException("Unsupported value of 'timestamp' in the filter: " + value.getRealValue());
            }
        }
        return millis;
    }

    /** Exact (to the millisecond), so that a range just over the limit does not read as equal to it. */
    static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        List<String> parts = new ArrayList<>();
        addPart(parts, duration.toDays(), "day");
        addPart(parts, duration.toHoursPart(), "hour");
        addPart(parts, duration.toMinutesPart(), "minute");
        int seconds = duration.toSecondsPart();
        int fraction = duration.toMillisPart();
        if (fraction > 0) {
            parts.add(String.format(Locale.ROOT, "%d.%03d seconds", seconds, fraction));
        } else {
            addPart(parts, seconds, "second");
        }
        return parts.isEmpty() ? "0 seconds" : String.join(" ", parts);
    }

    private static void addPart(List<String> parts, long value, String unit) {
        if (value > 0) {
            parts.add(value + " " + unit + (value == 1 ? "" : "s"));
        }
    }

    private ObjectPaging checkAndNormalizePaging(@Nullable ObjectPaging paging) throws RejectedException {
        Integer maxSize = paging != null ? paging.getMaxSize() : null;
        if (maxSize == null) {
            throw new RejectedException("Paging with 'maxSize' is required (at most " + limits.maxPageSize() + ").");
        }
        if (maxSize < 1 || maxSize > limits.maxPageSize()) {
            throw new RejectedException(
                    "The 'maxSize' must be between 1 and " + limits.maxPageSize() + ", it is " + maxSize + ".");
        }
        Integer offset = paging.getOffset();
        if (offset != null && (offset < 0 || offset > limits.maxOffset())) {
            throw new RejectedException("The 'offset' must be between 0 and " + limits.maxOffset() + ", it is " + offset
                    + ". Use a 'timestamp' condition to page through larger result sets.");
        }
        return queryFactory.createPaging(offset, maxSize, normalizeOrdering(paging.getOrderingInstructions()));
    }

    private List<ObjectOrdering> normalizeOrdering(@Nullable List<? extends ObjectOrdering> orderings)
            throws RejectedException {
        List<ObjectOrdering> normalized = new ArrayList<>();
        if (orderings == null || orderings.isEmpty()) {
            normalized.add(queryFactory.createOrdering(AuditEventRecordType.F_TIMESTAMP, OrderDirection.ASCENDING));
            normalized.add(queryFactory.createOrdering(AuditEventRecordType.F_REPO_ID, OrderDirection.ASCENDING));
            return normalized;
        }

        ObjectOrdering first = orderings.get(0);
        OrderDirection firstDirection = directionOf(first);
        if (isOrderBy(first, AuditEventRecordType.F_TIMESTAMP)) {
            normalized.add(queryFactory.createOrdering(AuditEventRecordType.F_TIMESTAMP, firstDirection));
            if (orderings.size() == 1) {
                normalized.add(queryFactory.createOrdering(AuditEventRecordType.F_REPO_ID, firstDirection));
                return normalized;
            }
            ObjectOrdering second = orderings.get(1);
            if (orderings.size() == 2 && isOrderBy(second, AuditEventRecordType.F_REPO_ID)) {
                normalized.add(queryFactory.createOrdering(AuditEventRecordType.F_REPO_ID, directionOf(second)));
                return normalized;
            }
        } else if (isOrderBy(first, AuditEventRecordType.F_REPO_ID) && orderings.size() == 1) {
            normalized.add(queryFactory.createOrdering(AuditEventRecordType.F_REPO_ID, firstDirection));
            return normalized;
        }
        throw new RejectedException("Ordering is supported only by 'timestamp' (optionally followed by 'repoId'), "
                + "or by 'repoId'.");
    }

    private static boolean isOrderBy(ObjectOrdering ordering, ItemName itemName) {
        ItemPath orderBy = ordering.getOrderBy();
        return orderBy != null && orderBy.equivalent(itemName);
    }

    private static OrderDirection directionOf(ObjectOrdering ordering) {
        return ordering.getDirection() != null ? ordering.getDirection() : OrderDirection.ASCENDING;
    }

    /** Rejects constructs that are unsafe (authorization bypass, expressions) or not meaningful for audit records. */
    private void checkFilter(@Nullable ObjectFilter filter) throws RejectedException {
        if (filter == null || filter instanceof AllFilter || filter instanceof NoneFilter || filter instanceof UndefinedFilter) {
            return;
        }
        if (filter instanceof ExpressionAware expressionAware && expressionAware.getExpression() != null) {
            throw new RejectedException("Expressions in filters are not supported.");
        }
        if (filter instanceof AndFilter || filter instanceof OrFilter || filter instanceof NotFilter) {
            for (ObjectFilter condition : ((LogicalFilter) filter).getConditions()) {
                checkFilter(condition);
            }
        } else if (filter instanceof ExistsFilter existsFilter) {
            checkPath(existsFilter.getFullPath());
            checkFilter(existsFilter.getFilter());
        } else if (filter instanceof RefFilter refFilter) {
            checkPath(refFilter.getFullPath());
            if (refFilter.getFilter() != null) {
                throw new RejectedException("Reference target filters are not supported for audit records.");
            }
        } else if (filter instanceof ValueFilter<?, ?> valueFilter) {
            checkPath(valueFilter.getFullPath());
            if (valueFilter.getRightHandSidePath() != null) {
                throw new RejectedException("Comparing with another item (right-hand side path) is not supported.");
            }
            if (valueFilter.getFullPath().equivalent(AuditEventRecordType.F_CHANGED_ITEM)) {
                checkChangedItemValues(valueFilter);
            }
        } else if (!(filter instanceof FullTextFilter)) {
            // (Full-text filter is supported: it searches the content of audit record payloads, not the message.)
            throw new RejectedException(
                    "Filter of type " + filter.getClass().getSimpleName() + " is not supported for audit records.");
        }
    }

    /**
     * Changed item paths are stored canonicalized with namespaces (see `ChangedItemPathComputer`), while
     * a query value is canonicalized as it is. So an unqualified path (`changedItem = fullName`) can never
     * match anything; instead of silently returning nothing, such a query is rejected.
     */
    private static void checkChangedItemValues(ValueFilter<?, ?> filter) throws RejectedException {
        List<? extends PrismValue> values = filter.getValues();
        if (values == null) {
            return;
        }
        for (PrismValue value : values) {
            Object realValue = value != null ? value.getRealValue() : null;
            ItemPath path = realValue instanceof ItemPathType itemPathType ? itemPathType.getItemPath() : null;
            if (path != null && path.getSegments().stream()
                    .anyMatch(segment -> ItemPath.isName(segment) && !QNameUtil.hasNamespace(ItemPath.toName(segment)))) {
                throw new RejectedException("The 'changedItem' path '" + path + "' must be namespace-qualified, "
                        + "e.g. 'c:fullName' or 'c:credentials/c:password/c:value'; an unqualified path never matches. "
                        + "In an XML request, declare the prefix on the query element "
                        + "(xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\"); "
                        + "in JSON and YAML, the standard prefixes are predefined.");
            }
        }
    }

    private static void checkPath(@Nullable ItemPath path) throws RejectedException {
        if (path != null && path.containsSpecialSymbols()) {
            throw new RejectedException("Path '" + path + "' is not supported for audit records. "
                    + "Dereferencing (e.g. 'targetRef/@/name') and other special path segments are not allowed; "
                    + "filter by the reference OID instead.");
        }
    }

    private static boolean hasIndexedAnchor(@Nullable ObjectFilter filter) {
        return topLevelConjuncts(filter).stream().anyMatch(AuditSearchGuard::isIndexedAnchor);
    }

    private static List<ObjectFilter> topLevelConjuncts(@Nullable ObjectFilter filter) {
        List<ObjectFilter> conjuncts = new ArrayList<>();
        collectConjuncts(filter, conjuncts);
        return conjuncts;
    }

    /** Nested conjunctions (e.g. `a and (b and c)`) are flattened, as they are equivalent to a single one. */
    private static void collectConjuncts(@Nullable ObjectFilter filter, List<ObjectFilter> conjuncts) {
        if (filter instanceof AndFilter andFilter) {
            for (ObjectFilter condition : andFilter.getConditions()) {
                collectConjuncts(condition, conjuncts);
            }
        } else if (filter != null) {
            conjuncts.add(filter);
        }
    }

    /**
     * An "anchor" is a condition that can be resolved by an index of `ma_audit_event`, so that the database
     * does not need to scan all the audit partitions:
     *
     * . `timestamp >= value` or `timestamp > value`,
     * . equality on `eventIdentifier`, `requestIdentifier`, `sessionIdentifier` or `repoId`,
     * . `targetRef` with an explicit OID.
     */
    static boolean isIndexedAnchor(@Nullable ObjectFilter filter) {
        return filter instanceof GreaterFilter<?> greaterFilter && isTimestampBound(greaterFilter)
                || isEqualityAnchor(filter);
    }

    /**
     * Equality (without a matching rule) on `eventIdentifier`, `requestIdentifier`, `sessionIdentifier` or `repoId`,
     * or `targetRef` with an explicit OID. These are resolved by (selective) indexes, regardless of the `timestamp` range.
     */
    static boolean isEqualityAnchor(@Nullable ObjectFilter filter) {
        if (filter instanceof EqualFilter<?> equalFilter) {
            ItemPath path = equalFilter.getFullPath();
            return INDEXED_EQUALITY_ITEMS.stream().anyMatch(path::equivalent)
                    && equalFilter.getRightHandSidePath() == null
                    // A matching rule (e.g. stringIgnoreCase) wraps the column in a function, so the index is not used.
                    && equalFilter.getMatchingRule() == null
                    && hasValues(equalFilter);
        } else if (filter instanceof RefFilter refFilter) {
            if (!refFilter.getFullPath().equivalent(AuditEventRecordType.F_TARGET_REF) || refFilter.getFilter() != null) {
                return false;
            }
            List<PrismReferenceValue> values = refFilter.getValues();
            return values != null && !values.isEmpty()
                    && values.stream().allMatch(v -> v != null && v.getOid() != null);
        } else {
            return false;
        }
    }

    private static boolean hasValues(ValueFilter<?, ?> filter) {
        List<?> values = filter.getValues();
        return values != null && !values.isEmpty() && values.stream().allMatch(v -> v != null);
    }
}
