/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor that resolves {@link OrgFilter}.
 */
public class OrgFilterProcessor implements FilterProcessor<OrgFilter> {

    private final SqlQueryContext<?, ?, ?> context;

    public OrgFilterProcessor(SqlQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(OrgFilter filter) throws QueryException {
        FlexibleRelationalPathBase<?> path = context.root();
        if (!(path instanceof QObject)) {
            throw new QueryException("Org filter can only be used for objects,"
                    + " not for path " + path + " of type " + path.getColumns());
        }

        QObject<?> objectPath = (QObject<?>) path;
        if (filter.isRoot()) {
            var refMapping = QObjectReferenceMapping.getForParentOrg();
            QObjectReference<MObject> ref = refMapping.newAlias(
                    context.uniqueAliasName(refMapping.defaultAliasName()));
            return new SQLQuery<>().select(Expressions.constant(1))
                    .from(ref)
                    .where(ref.ownerOid.eq(objectPath.oid))
                    .notExists();
        }

        if (filter.getOrgRef() == null) {
            throw new QueryException("No organization reference defined in the search query.");
        }

        if (filter.getOrgRef().getOid() == null) {
            throw new QueryException("No oid specified in organization reference " + filter.getOrgRef().debugDump());
        }

        // TODO
        throw new QueryException("OrgFilter cannot be applied to the entity: " + path);
    }
}
