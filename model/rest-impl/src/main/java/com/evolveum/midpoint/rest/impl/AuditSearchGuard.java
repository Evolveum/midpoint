/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.rest.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

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
     */
    public record Limits(int maxPageSize, int maxOffset, boolean requireIndexedAnchor) {

        public static final int DEFAULT_MAX_PAGE_SIZE = 1000;
        public static final int DEFAULT_MAX_OFFSET = 10_000;

        public static final Limits DEFAULT = new Limits(DEFAULT_MAX_PAGE_SIZE, DEFAULT_MAX_OFFSET, true);

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
                    webAppConfiguration.getBoolean(CONFIG_REQUIRE_INDEXED_ANCHOR, true));
        }

        private static int positiveOrDefault(Configuration configuration, String key, int defaultValue) {
            int value = configuration.getInt(key, defaultValue);
            if (value <= 0) {
                LOGGER.warn("Invalid value of {}: {}, using the default of {}", key, value, defaultValue);
                return defaultValue;
            }
            return value;
        }

        private static int nonNegativeOrDefault(Configuration configuration, String key, int defaultValue) {
            int value = configuration.getInt(key, defaultValue);
            if (value < 0) {
                LOGGER.warn("Invalid value of {}: {}, using the default of {}", key, value, defaultValue);
                return defaultValue;
            }
            return value;
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

    public @NotNull Limits getLimits() {
        return limits;
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
        query.setPaging(checkAndNormalizePaging(query.getPaging()));
        return anchored;
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
        } else if (!(filter instanceof FullTextFilter)) {
            throw new RejectedException(
                    "Filter of type " + filter.getClass().getSimpleName() + " is not supported for audit records.");
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
        if (filter instanceof AndFilter andFilter) {
            return andFilter.getConditions().stream().anyMatch(AuditSearchGuard::isIndexedAnchor);
        } else {
            return isIndexedAnchor(filter);
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
        if (filter instanceof GreaterFilter<?> greaterFilter) {
            return greaterFilter.getFullPath().equivalent(AuditEventRecordType.F_TIMESTAMP)
                    && greaterFilter.getRightHandSidePath() == null
                    && hasValues(greaterFilter);
        } else if (filter instanceof EqualFilter<?> equalFilter) {
            ItemPath path = equalFilter.getFullPath();
            return INDEXED_EQUALITY_ITEMS.stream().anyMatch(path::equivalent)
                    && equalFilter.getRightHandSidePath() == null
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
