/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.filtering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.xml.namespace.QName;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgClosure;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

/**
 * Partially collapses ORs produced by orgRelation authorization.
 *
 * The generic OR processor would turn every subtree {@link OrgFilter} into its own correlated
 * EXISTS. This optimizer groups compatible subtree org branches by target relation and replaces
 * multi-org groups with one semijoin-style EXISTS using {@code ancestor_oid in (...)}. Branches
 * outside the optimized groups are preserved and processed through the normal sqale path.
 *
 * Example: an OR like
 * {@code isChildOf(orgA) OR id(orgA) OR isChildOf(orgB) OR id(orgB)}
 * is rewritten to one subtree check using {@code ancestor_oid IN (orgA, orgB)}
 * plus one object OID check using {@code oid IN (orgA, orgB)}. Non-compatible
 * branches remain in the OR and are processed normally.
 */
public class OrgFilterOrOptimizer implements FilterProcessor<OrFilter> {

    private final SqaleQueryContext<?, ?, ?> context;

    public OrgFilterOrOptimizer(SqaleQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public @Nullable Predicate process(OrFilter filter) throws RepositoryException {
        FlexibleRelationalPathBase<?> path = context.root();
        if (!(path instanceof QObject<?> objectPath)) {
            return null;
        }

        Optimization optimization = Optimization.from(filter);
        if (!optimization.hasUsefulOptimization()) {
            return null;
        }

        optimization.splitInOidFiltersByOptimizedGroups();

        List<Predicate> predicates = new ArrayList<>();
        for (OrgGroup group : optimization.optimizedGroups()) {
            predicates.add(processOptimized(group, objectPath));
        }
        for (Set<String> leftoverOids : optimization.leftoverOidSets) {
            predicates.add(objectPath.oid.in(toUuids(leftoverOids)));
        }
        for (ObjectFilter leftover : optimization.leftoverFilters) {
            Predicate predicate = context.process(leftover);
            if (predicate != null) {
                predicates.add(predicate);
            }
        }

        return or(predicates);
    }

    private Predicate processOptimized(OrgGroup group, QObject<?> objectPath) throws QueryException {
        context.markContainsOrgFilter();

        QObjectReference<MObject> ref = getNewRefAlias();
        QOrgClosure oc = getNewClosureAlias();
        SQLQuery<?> subQuery = new SQLQuery<>()
                .select(QuerydslUtils.EXPRESSION_ONE)
                .from(ref)
                .join(oc).on(oc.descendantOid.eq(ref.targetOid))
                .where(ref.ownerOid.eq(objectPath.oid)
                        .and(oc.ancestorOid.in(toUuids(group.orgOids))));

        if (group.relation != null) {
            subQuery.where(ref.relationId.eq(
                    context.repositoryContext().searchCachedRelationId(group.relation)));
        }

        Predicate predicate = subQuery.exists();
        if (!group.selfOids.isEmpty()) {
            predicate = ExpressionUtils.or(
                    predicate,
                    objectPath.oid.in(toUuids(group.selfOids)));
        }
        return predicate;
    }

    private Predicate or(List<Predicate> predicates) {
        Predicate result = null;
        for (Predicate predicate : predicates) {
            result = result != null
                    ? ExpressionUtils.or(result, predicate)
                    : predicate;
        }
        return result;
    }

    private UUID[] toUuids(Collection<String> oids) throws QueryException {
        List<UUID> uuids = new ArrayList<>(oids.size());
        for (String oid : oids) {
            try {
                uuids.add(UUID.fromString(oid));
            } catch (IllegalArgumentException e) {
                throw new QueryException("OID '" + oid + "' is not a valid UUID.", e);
            }
        }
        return uuids.toArray(UUID[]::new);
    }

    private QObjectReference<MObject> getNewRefAlias() {
        QObjectReferenceMapping<?, QObject<MObject>, MObject> refMapping =
                QObjectReferenceMapping.getForParentOrg();
        return refMapping.newAlias(
                context.uniqueAliasName(refMapping.defaultAliasName()));
    }

    private QOrgClosure getNewClosureAlias() {
        return new QOrgClosure(
                context.uniqueAliasName(QOrgClosure.DEFAULT_ALIAS_NAME));
    }

    /**
     * Parsed representation of an OR filter split into parts that can be optimized
     * and parts that must keep the standard processing path.
     *
     * Compatible subtree org filters are grouped by target parent-org relation.
     * Simple object OID filters are split later: OIDs matching optimized org groups
     * become self branches of those groups, while unrelated OIDs stay as normal
     * leftover OID predicates.
     */
    private static class Optimization {
        // null key means ANY target parentOrgRef relation
        private final Map<QName, OrgGroup> orgGroups = new LinkedHashMap<>();
        private final List<InOidFilter> simpleInOidFilters = new ArrayList<>();
        private final List<ObjectFilter> leftoverFilters = new ArrayList<>();
        private final List<Set<String>> leftoverOidSets = new ArrayList<>();

        private static Optimization from(OrFilter filter) {
            Optimization optimization = new Optimization();
            List<ObjectFilter> flattened = new ArrayList<>();
            flattenOr(filter, flattened);

            for (ObjectFilter condition : flattened) {
                if (condition instanceof OrgFilter orgFilter && isOptimizable(orgFilter)) {
                    optimization.groupFor(orgFilter.getOrgRef().getRelation())
                            .add(orgFilter);
                } else if (condition instanceof InOidFilter inOidFilter && isSimpleRootInOid(inOidFilter)) {
                    optimization.simpleInOidFilters.add(inOidFilter);
                } else {
                    optimization.leftoverFilters.add(condition);
                }
            }

            optimization.moveSingletonGroupsToLeftovers();
            return optimization;
        }

        private boolean hasUsefulOptimization() {
            return orgGroups.values().stream()
                    .anyMatch(OrgGroup::isOptimized);
        }

        private List<OrgGroup> optimizedGroups() {
            return orgGroups.values().stream()
                    .filter(OrgGroup::isOptimized)
                    .toList();
        }

        private void splitInOidFiltersByOptimizedGroups() {
            for (InOidFilter filter : simpleInOidFilters) {
                Set<String> leftoverOids = new LinkedHashSet<>();
                for (String oid : filter.getOids()) {
                    OrgGroup group = findOptimizedGroupContaining(oid);
                    if (group != null) {
                        group.selfOids.add(oid);
                    } else {
                        leftoverOids.add(oid);
                    }
                }
                if (!leftoverOids.isEmpty()) {
                    leftoverOidSets.add(leftoverOids);
                }
            }
        }

        private @Nullable OrgGroup findOptimizedGroupContaining(String oid) {
            for (OrgGroup group : orgGroups.values()) {
                if (group.isOptimized() && group.orgOids.contains(oid)) {
                    return group;
                }
            }
            return null;
        }

        private OrgGroup groupFor(QName relation) {
            return orgGroups.computeIfAbsent(relation, OrgGroup::new);
        }

        private void moveSingletonGroupsToLeftovers() {
            for (OrgGroup group : orgGroups.values()) {
                if (!group.isOptimized()) {
                    leftoverFilters.addAll(group.orgFilters);
                }
            }
        }

        private static void flattenOr(ObjectFilter filter, List<ObjectFilter> flattened) {
            if (filter instanceof OrFilter orFilter) {
                for (ObjectFilter condition : orFilter.getConditions()) {
                    flattenOr(condition, flattened);
                }
            } else {
                flattened.add(filter);
            }
        }

        private static boolean isOptimizable(OrgFilter filter) {
            return !filter.isRoot()
                    && filter.getScope() == OrgFilter.Scope.SUBTREE
                    && filter.getOrgRef() != null
                    && filter.getOrgRef().getOid() != null;
        }

        private static boolean isSimpleRootInOid(InOidFilter filter) {
            return !filter.isConsiderOwner()
                    && filter.getExpression() == null
                    && filter.getOids() != null
                    && !filter.getOids().isEmpty();
        }
    }

    /**
     * Group of subtree org filters that can share one optimized EXISTS predicate.
     *
     * All org filters in the group use the same target parent-org relation.
     * The optimized predicate checks whether one of the object's parent-org refs,
     * optionally constrained by relation, points to a descendant of any
     * {@link #orgOids}. Optional {@link #selfOids} represent direct object OID
     * branches such as those produced by includeReferenceOrg-style filters.
     */
    private static class OrgGroup {
        private final QName relation;
        private final Set<String> orgOids = new LinkedHashSet<>();
        private final Set<String> selfOids = new LinkedHashSet<>();
        private final List<OrgFilter> orgFilters = new ArrayList<>();

        private OrgGroup(QName relation) {
            this.relation = relation;
        }

        private void add(OrgFilter filter) {
            orgFilters.add(filter);
            orgOids.add(filter.getOrgRef().getOid());
        }

        private boolean isOptimized() {
            return orgOids.size() > 1;
        }
    }
}
