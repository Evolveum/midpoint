/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import static com.evolveum.midpoint.repo.cache.handlers.CacheUseMode.PassReasonType.*;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;

/**
 * Mode in which we use the cache(s).
 *
 * Originally called a "pass reason" it described only the reason why the caches were to be passed - altogether.
 *
 * However, now it is more elaborate, and says e.g. that
 *
 * - we may use the cache for getting the data, but not update it if the data is not there, like when searching for objects
 * reduced by {@link RetrieveOptionType#EXCLUDE} option - {@link #cachedDataUse} is {@link CachedDataUse#ALWAYS},
 * but {@link #canUpdateObjectCache} is `false` (version and query caches can be updated);
 *
 * - or vice versa, we cannot use the cache, but after retrieving the object, we update the cache with it, like when having
 * zero staleness option - {@link #cachedDataUse} is {@link CachedDataUse#NEVER}, but {@link #canUpdateObjectCache} and others
 * are `true`;
 *
 * - or that we can use the data from the object cache only if it's complete, like when having {@link RetrieveOptionType#INCLUDE}
 * option; {@link #cachedDataUse} is {@link CachedDataUse#ONLY_IF_COMPLETE}.
 */
class CacheUseMode {

    /** The reason of reduced processing (if any). */
    @Nullable private final PassReasonType reason;

    /** More detailed information on the {@link #reason}. For the time being it is provided as plain text. */
    @Nullable private final String comment;

    /** Whether we can use the data from the cache. */
    private final CachedDataUse cachedDataUse;

    /** Whether we can use the fetched data to update the object cache. */
    private final boolean canUpdateObjectCache;

    /** Whether we can use the fetched data to update the version cache. */
    private final boolean canUpdateVersionCache;

    /**
     * Whether we can use the fetched data to update the query cache.
     *
     * It's `false` e.g. when {@link RetrieveOptionType#EXCLUDE} option is present.
     */
    private final boolean canUpdateQueryCache;

    enum PassReasonType {
        NOT_CACHEABLE_TYPE,
        MULTIPLE_OPTIONS,
        NON_ROOT_OPTIONS,
        UNSUPPORTED_OPTION,
        INCLUDE_OPTION_PRESENT,
        EXCLUDE_OPTION_PRESENT,
        UNTYPED_OPERATION,
        ZERO_STALENESS_REQUESTED
    }

    /** Whether we can use the data from the cache. */
    enum CachedDataUse {

        /** The normal case. */
        ALWAYS,

        /**
         * Only if they're complete. It's `false` e.g. for requests with the {@link RetrieveOptionType#INCLUDE} option.
         * (Temporary solution, maybe - we could perhaps look at the completeness per individual items.)
         */
        ONLY_IF_COMPLETE,

        /** E.g. for zero staleness requests. */
        NEVER
    }

    private CacheUseMode(
            @Nullable PassReasonType reason,
            @Nullable String comment,
            CachedDataUse cachedDataUse,
            boolean canUpdateObjectCache,
            boolean canUpdateVersionCache,
            boolean canUpdateQueryCache) {
        this.reason = reason;
        this.comment = comment;
        this.cachedDataUse = cachedDataUse;
        this.canUpdateObjectCache = canUpdateObjectCache;
        this.canUpdateVersionCache = canUpdateVersionCache;
        this.canUpdateQueryCache = canUpdateQueryCache;
    }

    private static CacheUseMode proceed() {
        return new CacheUseMode(
                null, null, CachedDataUse.ALWAYS,
                true, true, true);
    }

    private static CacheUseMode pass(@NotNull PassReasonType passReason) {
        return new CacheUseMode(
                passReason, null, CachedDataUse.NEVER,
                false, false, false);
    }

    @SuppressWarnings("SameParameterValue")
    private static CacheUseMode pass(@NotNull PassReasonType reason, String comment) {
        return new CacheUseMode(
                reason, comment, CachedDataUse.NEVER,
                false, false, false);
    }

    public @Nullable String getComment() {
        return comment;
    }

    boolean canNeverUseCachedData() {
        return cachedDataUse == CachedDataUse.NEVER;
    }

    boolean canUseCachedDataOnlyIfComplete() {
        return cachedDataUse == CachedDataUse.ONLY_IF_COMPLETE;
    }

    boolean canUpdateAtLeastOneCache() {
        return canUpdateObjectCache || canUpdateVersionCache || canUpdateQueryCache;
    }

    boolean canUpdateObjectCache() {
        return canUpdateObjectCache;
    }

    boolean canUpdateVersionCache() {
        return canUpdateVersionCache;
    }

    boolean canUpdateQueryCache() {
        return canUpdateQueryCache;
    }

    static @NotNull CacheUseMode determineForGetVersion(Class<?> objectType) {
        if (alwaysNotCacheable(objectType)) {
            return pass(NOT_CACHEABLE_TYPE);
        } else {
            return proceed();
        }
    }

    /**
     * Main entry point. By looking at situation we determine if there's a reason to pass the cache.
     */
    static @NotNull CacheUseMode determine(Collection<SelectorOptions<GetOperationOptions>> options, Class<?> objectType) {
        if (alwaysNotCacheable(objectType)) {
            return pass(NOT_CACHEABLE_TYPE);
        }
        var isObjectType = ObjectType.class.equals(objectType);
        if (!isObjectType && (options == null || options.isEmpty())) {
            return proceed(); // a shortcut for the most usual case (no options)
        }
        var analysis = OptionAnalysis.of(options);
        if (analysis.nonRootOptionsPresent) {
            return pass(NON_ROOT_OPTIONS);
        }
        var unsupportedDesc = analysis.getUnsupportedRootOptionsDescription();
        if (unsupportedDesc != null) {
            return pass(UNSUPPORTED_OPTION, unsupportedDesc);
        }

        // What remains is: include, exclude, staleness

        PassReasonType reason = null;
        String comment = null;

        CachedDataUse cachedDataUse;
        if (analysis.hasStalenessZero()) {
            // Zero staleness means we never want to retrieve data from the cache.
            // (But we may still put retrieved data into it.)
            cachedDataUse = CachedDataUse.NEVER;
            reason = ZERO_STALENESS_REQUESTED;
        } else if (!analysis.include.isEmpty() && !SelectorOptions.isRetrievedFullyByDefault(objectType)) {
            // If there is something extra to include, and the object type really has something to be requested this way,
            // we may use the cached data only if it's provably complete. (Temporary solution: we could get here into
            // more granularity by checking only the explicitly included paths for completeness.)
            cachedDataUse = CachedDataUse.ONLY_IF_COMPLETE;
            reason = INCLUDE_OPTION_PRESENT;
        } else {
            cachedDataUse = CachedDataUse.ALWAYS;
        }

        boolean canUpdateObjectCache;
        boolean canUpdateVersionCache;
        boolean canUpdateQueryCache;
        if (isObjectType) {
            // We cannot update the object cache if reading the generic ObjectType, because the repository may use
            // object type specific mappings e.g. for ShadowType, so the retrieved data may not be complete.
            // Note that retrieval by ObjectType should not be used anyway.
            canUpdateObjectCache = false;
            canUpdateVersionCache = true;
            canUpdateQueryCache = false; // To be 100% sure, we will not update the query cache either.
            if (reason == null) {
                reason = UNTYPED_OPERATION;
            } else {
                // The (single-valued) reason will not be quite exact in this case. Hence putting it into the comment.
                comment = "untyped operation";
            }
        } else if (!analysis.exclude.isEmpty()) {
            // We cannot update the object cache if we're excluding anything. But we may still update the query & version caches.
            // We can also use the cached data.
            canUpdateObjectCache = false;
            canUpdateVersionCache = true;
            canUpdateQueryCache = true;
            if (reason == null) {
                reason = EXCLUDE_OPTION_PRESENT;
            } else {
                // The (single-valued) reason will not be quite exact in this case. Hence putting it into the comment.
                comment = "exclude option present as well";
            }
        } else {
            canUpdateObjectCache = true;
            canUpdateVersionCache = true;
            canUpdateQueryCache = true;
        }

        return new CacheUseMode(reason, comment, cachedDataUse, canUpdateObjectCache, canUpdateVersionCache, canUpdateQueryCache);
    }

    /**
     * Main reason of cache pass:
     *
     * Tasks are usually rapidly changing.
     *
     * Cases are perhaps not changing that rapidly but these are objects that are used for communication of various parties;
     * so - to avoid having stale data - we skip caching them altogether.
     */
    private static boolean alwaysNotCacheable(Class<?> type) {
        return type.equals(TaskType.class)
                || type.equals(CaseType.class);
    }

    CacheUseTraceType toCacheUseForPass() {
        return new CacheUseTraceType()
                .category(CacheUseCategoryTraceType.PASS)
                .comment(reason + (comment != null ? ": " + comment : ""));
    }

    /**
     * @param rootOptions The root options (if any) - without include/exclude option.
     * @param nonRootOptionsPresent Non-root options - other than ones that contain only include or exclude paths - are present.
     * @param include Paths to include (including empty one if present).
     * @param exclude Paths to exclude (including empty one if present).
     */
    private record OptionAnalysis(
            @Nullable GetOperationOptions rootOptions,
            boolean nonRootOptionsPresent,
            @NotNull PathSet include,
            @NotNull PathSet exclude) {

        static OptionAnalysis of(Collection<SelectorOptions<GetOperationOptions>> selectorOptionsCollection) {
            GetOperationOptions rootOptions = null;
            var nonRootOptionsPresent = false;
            var include = new PathSet();
            var exclude = new PathSet();

            for (var selectorOptions : emptyIfNull(selectorOptionsCollection)) {
                var options = selectorOptions.getOptions();
                if (options == null) {
                    continue;
                }

                var retrieve = options.getRetrieve();
                if (retrieve == RetrieveOption.INCLUDE) {
                    include.add(ItemPath.EMPTY_PATH);
                } else if (retrieve == RetrieveOption.EXCLUDE) {
                    exclude.add(ItemPath.EMPTY_PATH);
                }

                if (selectorOptions.isRoot()) {
                    if (rootOptions != null) {
                        throw new IllegalStateException("Multiple root options: " + rootOptions + " and " + options);
                    }
                    if (retrieve != null) {
                        rootOptions = options.clone();
                        rootOptions.setRetrieve(null);
                    } else {
                        rootOptions = options;
                    }
                } else {
                    var clone = options.clone();
                    clone.setRetrieve(null);
                    if (!clone.isEmpty()) {
                        nonRootOptionsPresent = true;
                    }
                }
            }
            return new OptionAnalysis(rootOptions, nonRootOptionsPresent, include, exclude);
        }

        /** Returns `null` if there are no unsupported options. */
        @Nullable String getUnsupportedRootOptionsDescription() {
            if (rootOptions == null) {
                return null;
            }
            var clone = rootOptions.clone();

            // Options considered harmful:
            //  - resolve
            //  - resolveNames
            //  - raw (because of strange definition handling)
            //  - tolerateRawData (this is questionable, though)
            //  - relationalValueSearchQuery
            //  - distinct
            //  - attachDiagData
            //  - definitionProcessing
            //  - iterationMethod

            // Eliminate harmless options (retrieve is already eliminated)
            clone.doNotDiscovery(null);
            clone.setForceRefresh(null);
            clone.setForceRetry(null);
            clone.setAllowNotFound(null);
            clone.setExecutionPhase(null);
            clone.setReadOnly(null);
            clone.setNoFetch(null);
            clone.setPointInTimeType(null); // This is not used by repository anyway.
            // We know the staleness is not zero, so caching is (in principle) allowed.
            // More detailed treatment of staleness is not yet available.
            clone.setStaleness(null);
            clone.setErrorHandling(null);
            if (clone.isEmpty()) {
                return null;
            } else {
                return clone.toString();
            }
        }

        boolean hasStalenessZero() {
            Long staleness = rootOptions != null ? rootOptions.getStaleness() : null;
            return staleness != null && staleness == 0;
        }
    }
}
