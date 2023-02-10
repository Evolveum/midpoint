/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.mapping;

import java.util.*;

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

    protected abstract <S> S resolve(S object, JdbcSession session);

    public static ReferenceNameResolver from(Collection<SelectorOptions<GetOperationOptions>> options) {
        @NotNull
        List<? extends ItemPath> paths = getPathsToResolve(options);
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
        protected <S> S resolve(S object, JdbcSession session) {
            return object;
        }
    }

    private static final class Impl extends ReferenceNameResolver {

        private final List<? extends ItemPath> paths;
        private final Map<UUID, PolyString> uuidToName = new HashMap<>();
        private final Set<UUID> oidsToResolve = new HashSet<>();

        public Impl(List<? extends ItemPath> paths) {
            super();
            this.paths = paths;
        }

        @Override
        protected <S> S resolve(S object, JdbcSession session) {
            if (!(object instanceof Containerable)) {
                return object;
            }
            PrismContainerValue<?> container = ((Containerable) object).asPrismContainerValue();
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
        }

        private void resolveNames(JdbcSession session) {
            QObject<MObject> object = new QObject<>(MObject.class, "obj");
            // TODO: Add batch processing
            if (oidsToResolve.isEmpty()) {
                return;
            }

            List<Tuple> namesResult = session.newQuery()
                    .from(object)
                    .select(object.oid, object.nameOrig, object.nameNorm)
                    .where(object.oid.in(oidsToResolve))
                    .fetch();
            for (Tuple named : namesResult) {
                UUID uuid = named.get(object.oid);
                oidsToResolve.remove(uuid);
                String orig = named.get(object.nameOrig);
                String norm = named.get(object.nameNorm);
                PolyString poly = new PolyString(orig, norm);
                uuidToName.put(uuid, poly);
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
            PolyString maybe = uuidToName.get(oid);
            if (maybe != null) {
                value.setTargetName(maybe);
            } else {
                oidsToResolve.add(oid);
            }
        }

        private void updateReference(PrismReferenceValue value) {
            if (value.getOid() == null) {
                return;
            }
            PolyString name = uuidToName.get(SqaleUtils.oidToUuid(value.getOid()));
            if (name != null) {
                value.setTargetName(name);
            }
        }
    }
}
