/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceType;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.util.QNameUtil;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class OrgRestriction extends Restriction<OrgFilter> {

    public OrgRestriction(InterpretationContext context, OrgFilter filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        RootHibernateQuery hibernateQuery = getContext().getHibernateQuery();
        if (filter.isRoot()) {
            // oid in (select descendantOid from ROrgClosure group by descendantOid having count(descendantOid) = 1)
            return hibernateQuery.createIn(getBaseHqlEntity().getHqlPath() + ".oid",
                    "select descendantOid from ROrgClosure group by descendantOid having count(descendantOid) = 1");
        }

        if (filter.getOrgRef() == null) {
            throw new QueryException("No organization reference defined in the search query.");
        }

        if (filter.getOrgRef().getOid() == null) {
            throw new QueryException("No oid specified in organization reference " + filter.getOrgRef().debugDump());
        }

        String orgOidParamName = hibernateQuery.addParameter("orgOid", filter.getOrgRef().getOid());
        String relationParamName = "";
        QName relation = filter.getOrgRef().getRelation();
        if (doesRelationRestrictionExist(relation)) {
            relationParamName = hibernateQuery.addParameter("relation", ReferenceRestriction.getRelationsToTest(relation, context));
        }
        String oidQueryText;    // oid in ...
        switch (filter.getScope()) {
            case ONE_LEVEL:
                oidQueryText =
                        "select ref.ownerOid " +     // TODO distinct(ref.ownerOid) ? (was in original QueryInterpreter)
                              "from RObjectReference ref " +
                           "where " +
                              "ref.referenceType = " + RReferenceType.OBJECT_PARENT_ORG.ordinal()
                                + (doesRelationRestrictionExist(relation) ?
                                    " and ref.relation in (:" + relationParamName + ")" : "")
                                + " and ref.targetOid = :" + orgOidParamName;
                break;
            case ANCESTORS:
                oidQueryText =
                        "select c.ancestorOid " +
                            "from ROrgClosure c " +
                        "where " +
                            "c.ancestorOid != :" + orgOidParamName + " and " +
                            "c.descendantOid = :" + orgOidParamName;
                break;
            case SUBTREE:
            default:
                oidQueryText =
                        "select ref.ownerOid " +
                            "from RObjectReference ref " +
                        "where " +
                            "ref.referenceType = " + RReferenceType.OBJECT_PARENT_ORG.ordinal()
                                + (doesRelationRestrictionExist(relation) ?
                                    " and ref.relation in (:" + relationParamName + ")" : "")
                                + " and ref.targetOid in (" +
                                    "select descendantOid from ROrgClosure where ancestorOid = :" + orgOidParamName + ")";
        }
        return hibernateQuery.createIn(getBaseHqlEntity().getHqlPath() + ".oid", oidQueryText);
    }

    private boolean doesRelationRestrictionExist(QName relation) {
        return relation != null && !QNameUtil.match(PrismConstants.Q_ANY, relation);
    }
}
