/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.DeleteObjectResult;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *  Describes a change that was detected on a repository object:
 *  - type
 *  - OID
 *  - additional information related object ADD/MODIFY/DELETE operation
 */
public abstract class ChangeDescription {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeDescription.class);

    protected Class<? extends ObjectType> type;             // changed object type
    protected String oid;                                   // changed object oid

    ChangeDescription(Class<? extends ObjectType> type, String oid) {
        this.type = type;
        this.oid = oid;
    }

    /**
     * Describes an OBJECT ADD operation.
     */
    static class Add extends ChangeDescription {
        private AddObjectResult<?> addInfo;

        Add(Class<? extends ObjectType> type, String oid, AddObjectResult<?> addInfo) {
            super(type, oid);
            this.addInfo = addInfo;
        }

        @Override
        public boolean mayMatchAfterChange(@NotNull ObjectFilter filter, SearchResultList list, MatchingRuleRegistry matchingRuleRegistry)
                throws SchemaException {
            return filter.match(addInfo.getObject().getValue(), matchingRuleRegistry);
        }

        @Override
        public String toString() { return "Add{" + addInfo + '}';
        }
    }

    /**
     * Describes an OBJECT MODIFY operation.
     */
    static class Modify extends ChangeDescription {
        private ModifyObjectResult<?> modifyInfo;

        Modify(Class<? extends ObjectType> type, String oid, ModifyObjectResult<?> modifyInfo) {
            super(type, oid);
            this.modifyInfo = modifyInfo;
        }

        @Override
        public boolean mayMatchAfterChange(@NotNull ObjectFilter filter, SearchResultList list, MatchingRuleRegistry matchingRuleRegistry)
                throws SchemaException {
            //noinspection SimplifiableIfStatement
            if (modifyInfo.getObjectAfter() == null) {
                // This can occur only for lookup tables and certification cases. These are handled by evicting all related queries anyway.
                return true;
            } else {
                return filter.match(modifyInfo.getObjectAfter().getValue(), matchingRuleRegistry);
            }
        }

        @Override
        public String toString() {
            return "Modify{" +
                    "type=" + type +
                    ",oid=" + oid +
                    ",modifyInfo=" + modifyInfo +
                    '}';
        }
    }

    /**
     * Describes an OBJECT DELETE operation.
     */
    static class Delete extends ChangeDescription {
        Delete(Class<? extends ObjectType> type, String oid) {
            super(type, oid);
        }

        @Override
        public boolean mayMatchAfterChange(@NotNull ObjectFilter filter, SearchResultList list, MatchingRuleRegistry matchingRuleRegistry) {
            // There's no object after deletion. :)
            return false;
        }

        @Override
        public String toString() {
            return "Delete{" +
                    "type=" + type +
                    ", oid='" + oid + '\'' +
                    '}';
        }
    }

    static final class Any extends ChangeDescription {
        private final boolean safeInvalidation;

        private Any(Class<? extends ObjectType> type, String oid, boolean safeInvalidation) {
            super(type, oid);
            this.safeInvalidation = safeInvalidation;
        }

        @Override
        public boolean mayMatchAfterChange(@NotNull ObjectFilter filter, SearchResultList list, MatchingRuleRegistry matchingRuleRegistry) {
            // We know nothing about the object state after change, so we must say "yes" here.
            return safeInvalidation;
        }

        @Override
        public String toString() {
            return "Any{" +
                    "type=" + type +
                    ", oid='" + oid + '\'' +
                    ", safeInvalidation='" + safeInvalidation + '\'' +
                    '}';
        }
    }


    @NotNull
    public static ChangeDescription getFrom(Class<? extends ObjectType> type, String oid, CacheInvalidationContext context, boolean safeInvalidation) {
        Object additionalInfo;
        if (context != null) {
            additionalInfo = context.getDetails() instanceof RepositoryCache.RepositoryCacheInvalidationDetails ?
                    ((RepositoryCache.RepositoryCacheInvalidationDetails) context.getDetails()).getObject() : null;
        } else {
            additionalInfo = null;
        }
        return getFrom(type, oid, additionalInfo, safeInvalidation);

    }
    public static ChangeDescription getFrom(Class<? extends ObjectType> type, String oid, Object additionalInfo,
            boolean safeInvalidation) {

        // Lookup tables and cases are tricky to work with (their changes are not reflected in repo-emitted
        // prism objects) -- so it's safer to evict their queries completely.
        //
        // Note that the same is true for
        // - task.result -> but tasks are not cached at all
        // - focus.jpegPhoto -> but we don't use JPEG photo to query
        // TODO something other?

        boolean isTricky = LookupTableType.class.equals(type) || AccessCertificationCampaignType.class.equals(type);
        if (isTricky || additionalInfo == null) {
            return new Any(type, oid, safeInvalidation);
        } else if (additionalInfo instanceof AddObjectResult<?>) {
            return new Add(type, oid, (AddObjectResult<?>) additionalInfo);
        } else if (additionalInfo instanceof ModifyObjectResult<?>) {
            return new Modify(type, oid, ((ModifyObjectResult<?>) additionalInfo));
        } else if (additionalInfo instanceof DeleteObjectResult) {
            return new Delete(type, oid);
        } else {
            throw new IllegalArgumentException("Invalid additionalInfo: " + additionalInfo);
        }
    }

    private boolean queryTypeMatches(QueryKey queryKey) {
        return queryKey.getType().isAssignableFrom(type);
    }

    /**
     * Returns true if the given change may affect the result of a given query.
     * Better be conservative and say "true" even if we are not sure.
     */
    boolean mayAffect(QueryKey queryKey, SearchResultList list, MatchingRuleRegistry matchingRuleRegistry) {
        if (!queryTypeMatches(queryKey)) {
            return false;
        }
        ObjectFilter filter = getFilter(queryKey);
        if (filter == null) {
            // We are interested in all objects; so probably in this one as well.
            return true;
        }
        if (listContainsOid(list, oid)) {
            // The original query result contains the object being changed or deleted.
            // (In very strange cases, also the object being added -- although this should never happen.)
            return true;
        }
        try {
            return mayMatchAfterChange(filter, list, matchingRuleRegistry);
        } catch (UnsupportedOperationException e) {
            LOGGER.debug("Couldn't match object being changed to cached query because the filter is not (yet) supported -- "
                    + "continuing as if there might be an overlap:\n"
                    + "change description = {}\nfilter = {}", this, filter, e);
            return true;
        } catch (Throwable t) {
            LOGGER.warn("Couldn't match object being changed to cached query -- continuing as if there might be an overlap:\n"
                            + "change description = {}\nfilter = {}", this, filter, t);
            return true;
        }
    }

    public abstract boolean mayMatchAfterChange(@NotNull ObjectFilter filter, SearchResultList list, MatchingRuleRegistry matchingRuleRegistry)
            throws SchemaException;

    @Nullable
    private ObjectFilter getFilter(QueryKey queryKey) {
        ObjectQuery query = queryKey.getQuery();
        return query != null ? query.getFilter() : null;
    }

    private static boolean listContainsOid(SearchResultList list, String oid) {
        for (Object o : list) {
            if (o instanceof PrismObject<?>) {
                if (oid.equals(((PrismObject) o).getOid())) {
                    return true;
                }
            }
        }
        return false;
    }

}
