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

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

/**
 * @author lazyman
 */
public class OrgRestriction extends Restriction<OrgFilter> {

    private static final String QUERY_PATH = "descendants";
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

        updateCriteria();

        if (filter.getOrgRef() == null) {
            throw new QueryException("No organization reference defined in the search query.");
        }

        if (filter.getOrgRef().getOid() == null) {
            throw new QueryException("No oid specified in organization reference " + filter.getOrgRef().debugDump());
        }

        String orgRefOid = filter.getOrgRef().getOid();

        Integer maxDepth = filter.getMaxDepth();
        if (maxDepth != null && maxDepth < 0) {
            maxDepth = null;
        }

        Integer minDepth = filter.getMinDepth();
        if (minDepth != null && minDepth < 0) {
            minDepth = null;
        }

        if (minDepth == null && maxDepth == null) {
            return Restrictions.eq(CLOSURE_ALIAS + ".ancestorOid", orgRefOid);
        }

        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.eq(CLOSURE_ALIAS + ".ancestorOid", orgRefOid));
        if (ObjectUtils.equals(minDepth, maxDepth)) {
            conjunction.add(Restrictions.eq(DEPTH, minDepth));
        } else {
            if (minDepth != null) {
                conjunction.add(Restrictions.gt(DEPTH, minDepth));
            }
            if (maxDepth != null) {
                conjunction.add(Restrictions.le(DEPTH, maxDepth));
            }
        }
        return conjunction;
    }

    private void updateCriteria() {
        // get root criteria
        Criteria main = getContext().getCriteria(null);
        // create subcriteria on the ROgrClosure table to search through org struct

        ProjectionList list = Projections.projectionList();
        String alias = getContext().getAlias(null);

        QueryContext context = getContext();
        SqlRepositoryConfiguration sqlConfig = context.getInterpreter().getRepoConfiguration();

        list.add(Projections.property(alias + ".fullObject"));
        list.add(Projections.property(alias + ".stringsCount"));
        list.add(Projections.property(alias + ".longsCount"));
        list.add(Projections.property(alias + ".datesCount"));
        list.add(Projections.property(alias + ".referencesCount"));
        list.add(Projections.property(alias + ".polysCount"));

        if (sqlConfig.isUsingSQLServer()) {
            list.add(Projections.groupProperty(alias + ".fullObject"));
            list.add(Projections.groupProperty(alias + ".stringsCount"));
            list.add(Projections.groupProperty(alias + ".longsCount"));
            list.add(Projections.groupProperty(alias + ".datesCount"));
            list.add(Projections.groupProperty(alias + ".referencesCount"));
            list.add(Projections.groupProperty(alias + ".polysCount"));
        }

        main.createCriteria(QUERY_PATH, CLOSURE_ALIAS);
        main.setProjection(list);
    }

    @Override
    public OrgRestriction cloneInstance() {
        return new OrgRestriction();
    }
}
