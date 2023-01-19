/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import java.util.UUID;
import javax.xml.namespace.QName;

import com.querydsl.core.types.Predicate;
import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgClosure;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;

/**
 * Filter processor that resolves {@link OrgFilter}.
 */
public class OrgFilterProcessor implements FilterProcessor<OrgFilter> {

    private final SqaleQueryContext<?, ?, ?> context;

    public OrgFilterProcessor(SqaleQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(OrgFilter filter) throws QueryException {
        context.markContainsOrgFilter(); // necessary for lazy refresh of org closure

        FlexibleRelationalPathBase<?> path = context.root();
        if (!(path instanceof QObject)) {
            throw new QueryException("Org filter can only be used for objects,"
                    + " not for path " + path + " of type " + path.getClass());
        }

        QObject<?> objectPath = (QObject<?>) path;
        if (filter.isRoot()) {
            QObjectReference<MObject> ref = getNewRefAlias();
            return subQuery(ref)
                    .where(ref.ownerOid.eq(objectPath.oid))
                    .notExists();
        }

        if (filter.getOrgRef() == null) {
            throw new QueryException("No organization reference defined in the search query.");
        }

        String oidParam = filter.getOrgRef().getOid();
        if (oidParam == null) {
            throw new QueryException("No oid specified in organization reference " + filter.getOrgRef().debugDump());
        }

        QName relation = filter.getOrgRef().getRelation();
        // null means ANY (not "default") here, so we only search/normalize non-nulls
        Integer relationId = relation != null
                ? context.repositoryContext().searchCachedRelationId(relation)
                : null;

        if (filter.getScope() == OrgFilter.Scope.ONE_LEVEL) {
            QObjectReference<MObject> ref = getNewRefAlias();
            SQLQuery<?> subQuery = subQuery(ref)
                    .where(ref.ownerOid.eq(objectPath.oid)
                            .and(ref.targetOid.eq(UUID.fromString(oidParam))));
            if (relationId != null) {
                subQuery.where(ref.relationId.eq(relationId));
            }
            return subQuery.exists();
        } else if (filter.getScope() == OrgFilter.Scope.SUBTREE) {
            QObjectReference<MObject> ref = getNewRefAlias();
            QOrgClosure oc = getNewClosureAlias();
            SQLQuery<?> subQuery = subQuery(ref)
                    .join(oc).on(oc.descendantOid.eq(ref.targetOid))
                    .where(ref.ownerOid.eq(objectPath.oid)
                            .and(oc.ancestorOid.eq(UUID.fromString(oidParam))));
            if (relationId != null) {
                subQuery.where(ref.relationId.eq(relationId));
            }
            return subQuery.exists();
        } else if (filter.getScope() == OrgFilter.Scope.ANCESTORS) {
            QOrgClosure oc = getNewClosureAlias();
            return subQuery(oc)
                    .where(oc.ancestorOid.eq(objectPath.oid)
                            .and(oc.descendantOid.eq(UUID.fromString(oidParam)))
                            .and(oc.ancestorOid.ne(oc.descendantOid)))
                    .exists();
        } else {
            throw new QueryException("Unknown scope if org filter: " + filter);
        }
    }

    private SQLQuery<?> subQuery(FlexibleRelationalPathBase<?> entityPath) {
        return new SQLQuery<>().select(QuerydslUtils.EXPRESSION_ONE)
                .from(entityPath);
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
}
