/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.*;

import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.role.MAbstractRole;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QAbstractRole;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.QShadow;
import com.evolveum.midpoint.repo.sqlbase.SqlBaseOperationTracker;

import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

public abstract class ReferenceNameResolver {

    /**
     * Holds resolved name and display name for an object.
     * Display name is: fullName for Users, displayName for AbstractRoles, null otherwise.
     */
    public record ResolvedNames(String name, String displayName) {}

    public abstract <S> S resolve(S object, JdbcSession session);

    /**
     * Batch resolve names and display names for a set of OIDs.
     * Results are cached and can be retrieved via {@link #getResolvedNames(UUID)}.
     *
     * @param session JDBC session
     * @param oids OIDs to resolve
     * @param includeDisplayNames if true, also resolve display names (requires LEFT JOIN)
     */
    public abstract void batchResolve(JdbcSession session, Set<UUID> oids, boolean includeDisplayNames);

    /**
     * Get cached resolved names for an OID, or null if not resolved.
     */
    public abstract ResolvedNames getResolvedNames(UUID oid);

    public static ReferenceNameResolver from(Collection<SelectorOptions<GetOperationOptions>> options) {
        @NotNull
        List<? extends ItemPath> paths = getPathsToResolve(options);
        if (paths.isEmpty()) {
            return new Noop();
        }
        return new Impl(paths);
    }

    public static ReferenceNameResolver from(List<ItemPath> paths) {
        if (paths.isEmpty()) {
            return new Noop();
        }
        return new Impl(paths);
    }

    @NotNull
    private static List<? extends ItemPath> getPathsToResolve(Collection<SelectorOptions<GetOperationOptions>> options) {
        final UniformItemPath emptyPath = PrismContext.get().emptyPath();
        List<UniformItemPath> rv = new ArrayList<>();
        if (options == null || options.isEmpty()) {
            return rv;
        }
        for (SelectorOptions<GetOperationOptions> option : options) {
            if (GetOperationOptions.isResolveNames(option.getOptions())) {
                rv.add(option.getItemPath(emptyPath));
            }
        }
        return rv;
    }

    private static final class Noop extends ReferenceNameResolver {

        @Override
        public  <S> S resolve(S object, JdbcSession session) {
            return object;
        }

        @Override
        public void batchResolve(JdbcSession session, Set<UUID> oids, boolean includeDisplayNames) {
            // No-op
        }

        @Override
        public ResolvedNames getResolvedNames(UUID oid) {
            return null;
        }
    }

    private static final class Impl extends ReferenceNameResolver {

        private final List<? extends ItemPath> paths;
        private final Set<UUID> oidsToResolve = new HashSet<>();
        private final Set<UUID> shadowOidsToResolve = new HashSet<>();

        // Unified cache for name and displayName
        private final Map<UUID, ResolvedNames> resolvedNamesCache = new HashMap<>();


        public Impl(List<? extends ItemPath> paths) {
            super();
            this.paths = paths;
        }

        @Override
        public  <S> S resolve(S object, JdbcSession session) {
            PrismContainerValue<?> container = null;
            if (object instanceof Containerable cont) {
                container = cont.asPrismContainerValue();
            } else if (object instanceof PrismContainerValue<?> pcv) {
                container = pcv;
            }
            if (container == null) {
                return object;
            }

            var result = SqlBaseOperationTracker.resolveNames();
            try {
                Visitor initialWalk = visitable -> {
                    if (visitable instanceof PrismReferenceValue) {
                        initialVisit((PrismReferenceValue) visitable);
                    }
                };
                container.accept(initialWalk);
                resolveNames(session);
                Visitor updater = visitable -> {
                    if (visitable instanceof PrismReferenceValue) {
                        updateReference((PrismReferenceValue) visitable);
                    }
                };
                container.accept(updater);
                return object;
            } finally {
                result.close();
            }
        }

        private void resolveNames(JdbcSession session) {
            var object = new QObject<>(MObject.class, "obj");
            var shadow = new QShadow("s");
            resolveNamesSimple(session, object, oidsToResolve);
            resolveNamesSimple(session, shadow, shadowOidsToResolve);
        }

        /**
         * Simple name resolution (name only, no displayName).
         * Used by resolve() for references not pre-loaded by batchResolve().
         */
        private void resolveNamesSimple(JdbcSession session, QObject<?> object, Set<UUID> oidsToResolve) {
            if (oidsToResolve.isEmpty()) {
                return;
            }
            List<Tuple> namesResult = session.newQuery()
                    .from(object)
                    .select(object.oid, object.nameOrig)
                    .where(object.oid.in(oidsToResolve))
                    .fetch();
            for (Tuple named : namesResult) {
                UUID uuid = named.get(object.oid);
                oidsToResolve.remove(uuid);
                String name = named.get(object.nameOrig);
                // Only set name, displayName is null (not requested)
                resolvedNamesCache.put(uuid, new ResolvedNames(name, null));
            }
        }

        private void initialVisit(PrismReferenceValue value) {
            if (!ItemPathCollectionsUtil.containsSubpathOrEquivalent(paths, value.getPath())) {
                return;
            }
            if (value.getTargetName() != null) {
                return;
            }
            if (value.getObject() != null) {
                value.setTargetName(value.getObject().getName());
                return;
            }
            if (value.getOid() == null) {
                return;
            }
            UUID oid = SqaleUtils.oidToUuid(value.getOid());
            ResolvedNames resolved = resolvedNamesCache.get(oid);
            if (resolved != null) {
                value.setTargetName(PolyString.fromOrig(resolved.name()));
            } else if (QNameUtil.match(ShadowType.COMPLEX_TYPE, value.getTargetType())) {
                shadowOidsToResolve.add(oid);
            } else {
                oidsToResolve.add(oid);
            }
        }

        private void updateReference(PrismReferenceValue value) {
            if (value.getOid() == null) {
                return;
            }
            ResolvedNames resolved = resolvedNamesCache.get(SqaleUtils.oidToUuid(value.getOid()));
            if (resolved != null) {
                value.setTargetName(PolyString.fromOrig(resolved.name()));
            }
        }

        @Override
        public void batchResolve(JdbcSession session, Set<UUID> oids, boolean includeDisplayNames) {
            if (oids == null || oids.isEmpty()) {
                return;
            }

            // Filter out OIDs that are already cached
            Set<UUID> oidsToLoad = new HashSet<>();
            for (UUID oid : oids) {
                if (!resolvedNamesCache.containsKey(oid)) {
                    oidsToLoad.add(oid);
                }
            }

            if (oidsToLoad.isEmpty()) {
                return;
            }

            if (includeDisplayNames) {
                // Query m_object with LEFT JOIN to m_user and m_abstract_role for display names
                QObject<MObject> qObj = new QObject<>(MObject.class, "obj");
                QUser qUser = new QUser("usr");
                QAbstractRole<MAbstractRole> qRole = new QAbstractRole<>(MAbstractRole.class, "ar");

                List<Tuple> rows = session.newQuery()
                        .select(qObj.oid, qObj.nameOrig, qUser.fullNameOrig, qRole.displayNameOrig)
                        .from(qObj)
                        .leftJoin(qUser).on(qObj.oid.eq(qUser.oid))
                        .leftJoin(qRole).on(qObj.oid.eq(qRole.oid))
                        .where(qObj.oid.in(oidsToLoad))
                        .fetch();

                for (Tuple row : rows) {
                    UUID oid = row.get(qObj.oid);
                    String fullName = row.get(qUser.fullNameOrig);
                    String roleDisplayName = row.get(qRole.displayNameOrig);
                    String name = row.get(qObj.nameOrig);

                    // fullName for Users, displayName for AbstractRoles, null otherwise
                    // GUI components decide whether to fallback to name
                    String displayName = fullName != null ? fullName : roleDisplayName;

                    resolvedNamesCache.put(oid, new ResolvedNames(name, displayName));
                }
            } else {
                // Simple name-only query
                QObject<MObject> qObj = new QObject<>(MObject.class, "obj");

                List<Tuple> rows = session.newQuery()
                        .select(qObj.oid, qObj.nameOrig)
                        .from(qObj)
                        .where(qObj.oid.in(oidsToLoad))
                        .fetch();

                for (Tuple row : rows) {
                    UUID oid = row.get(qObj.oid);
                    String name = row.get(qObj.nameOrig);
                    resolvedNamesCache.put(oid, new ResolvedNames(name, null));
                }
            }
        }

        @Override
        public ResolvedNames getResolvedNames(UUID oid) {
            return resolvedNamesCache.get(oid);
        }
    }
}
