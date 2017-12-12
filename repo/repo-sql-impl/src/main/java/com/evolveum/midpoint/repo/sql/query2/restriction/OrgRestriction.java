/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class OrgRestriction extends Restriction<OrgFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(OrgRestriction.class);

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

        QName relation = filter.getOrgRef().getRelation();
        String relationParamName;
        if (relation != null) {
            if (filter.getScope() == OrgFilter.Scope.ONE_LEVEL) {
                relationParamName = hibernateQuery.addParameter("relation", RUtil.qnameToString(relation));
            } else {
                LOGGER.warn("'relation' clause ({}) not supported for subtree queries; ignoring it", relation);
                relationParamName = null;
            }
        } else {
            relationParamName = null;
        }

        String oidQueryText;    // oid in ...
        switch (filter.getScope()) {
            case ONE_LEVEL:
                oidQueryText =
                        "select ref.ownerOid " +     // TODO distinct(ref.ownerOid) ? (was in original QueryInterpreter)
                              "from RObjectReference ref " +
                           "where " +
                              "ref.referenceType = " + nameOf(RReferenceOwner.OBJECT_PARENT_ORG) + " and " +
                              "ref.targetOid = :" + orgOidParamName;
                if (relationParamName != null) {
                    oidQueryText += " and ref.relation = :" + relationParamName;        // TODO normalized vs unnormalized QNames, default vs null
                }
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
                            "ref.referenceType = " + nameOf(RReferenceOwner.OBJECT_PARENT_ORG) + " and " +
                            "ref.targetOid in (" +
                                "select descendantOid from ROrgClosure where ancestorOid = :" + orgOidParamName + ")";
        }
        return hibernateQuery.createIn(getBaseHqlEntity().getHqlPath() + ".oid", oidQueryText);
    }
}
