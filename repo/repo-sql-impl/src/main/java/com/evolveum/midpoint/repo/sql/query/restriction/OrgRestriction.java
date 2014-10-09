/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.api.query.Query;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.hibernate.sql.JoinType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class OrgRestriction extends Restriction<OrgFilter> {

    private static final String QUERY_PATH = "descendants";
    private static final ItemPath QUERY_ITEM_PATH = new ItemPath(new QName(RUtil.NS_SQL_REPO, QUERY_PATH));

    private static final String CLOSURE_ALIAS = "closure";
    private static final String DEPTH = CLOSURE_ALIAS + ".depth";

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) {
        if (filter instanceof OrgFilter) {
            return true;
        }
        return false;
    }

    @Override
    public Criterion interpret(OrgFilter filter) throws QueryException {
        if (filter.isRoot()) {
//			Criteria pCriteria = getInterpreter().getCriteria(null);
            DetachedCriteria dc = DetachedCriteria.forClass(ROrgClosure.class);
            String[] strings = new String[1];
            strings[0] = "descendant.oid";
            Type[] type = new Type[1];
            type[0] = StringType.INSTANCE;
            dc.setProjection(Projections.sqlGroupProjection("descendant_oid", "descendant_oid having count(descendant_oid)=1", strings, type));
//			pCriteria.add(Subqueries.in("this.oid", dc));
            return Subqueries.propertyIn("oid", dc);
//			Query rootOrgQuery = session.createQuery("select org from ROrg as org where org.oid in (select descendant.oid from ROrgClosure group by descendant.oid having count(descendant.oid)=1)");
        }

        if (filter.getOrgRef() == null) {
            throw new QueryException("No organization reference defined in the search query.");
        }

        if (filter.getOrgRef().getOid() == null) {
            throw new QueryException("No oid specified in organization reference " + filter.getOrgRef().debugDump());
        }

        DetachedCriteria detached;
        switch (filter.getScope()) {
            case ONE_LEVEL:
                detached = DetachedCriteria.forClass(RParentOrgRef.class, "p");
                detached.setProjection(Projections.distinct(Projections.property("p.ownerOid")));
                detached.add(Restrictions.eq("p.targetOid", filter.getOrgRef().getOid()));
                break;
            case SUBTREE:
            default:
                detached = DetachedCriteria.forClass(RParentOrgRef.class, "p");
                detached.setProjection(Projections.distinct(Projections.property("p.ownerOid")));
                detached.add(Property.forName("targetOid").in(
                        DetachedCriteria.forClass(ROrgClosure.class, "cl")
                            .setProjection(Projections.property("cl.descendantOid"))
                            .add(Restrictions.eq("cl.ancestorOid", filter.getOrgRef().getOid()))));
        }
        String mainAlias = getContext().getAlias(null);
        return Subqueries.propertyIn(mainAlias + ".oid", detached);
    }

    @Override
    public OrgRestriction cloneInstance() {
        return new OrgRestriction();
    }
}
