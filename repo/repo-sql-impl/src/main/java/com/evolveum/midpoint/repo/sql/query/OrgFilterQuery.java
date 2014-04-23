/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.Query;
import org.hibernate.Session;

import java.util.Collection;

/**
 * @author lazyman
 */
public class OrgFilterQuery extends CustomQuery {

    private static final Trace LOGGER = TraceManager.getTrace(OrgFilterQuery.class);

    @Override
    public boolean match(ObjectQuery objectQuery, Class<? extends ObjectType> type,
                         Collection<SelectorOptions<GetOperationOptions>> options, boolean countingObjects) {

        if (objectQuery == null || !(objectQuery.getFilter() instanceof OrgFilter)) {
            return false;
        }

        OrgFilter filter = (OrgFilter) objectQuery.getFilter();
        if (filter.isRoot()) {
            return false;
        }

        return true;
    }

    // http://stackoverflow.com/questions/10515391/oracle-equivalent-of-postgres-distinct-on
    // select distinct col1, first_value(col2) over (partition by col1 order by col2 asc) from tmp
    @Override
    public RQuery createQuery(ObjectQuery objectQuery, Class<? extends ObjectType> type,
                              Collection<SelectorOptions<GetOperationOptions>> options, boolean countingObjects,
                              Session session) {

        OrgFilter filter = (OrgFilter) objectQuery.getFilter();

        LOGGER.trace("createOrgQuery {}, counting={}, filter={}", new Object[]{type.getSimpleName(), countingObjects, filter});

        if (countingObjects) {
            return countQuery(filter, type, session);
        }

        if (getRepoConfiguration().isUsingOracle()) {
            return createOracleQuery(filter, type, session);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("select o.fullObject,o.stringsCount,o.longsCount,o.datesCount,o.referencesCount,o.polysCount from ");
        sb.append(ClassMapper.getHQLType(type)).append(" as o left join o.descendants as d where d.ancestorOid = :aOid");
        if (filter.getMinDepth() != null || filter.getMaxDepth() != null) {
            if (ObjectUtils.equals(filter.getMinDepth(), filter.getMaxDepth())) {
                sb.append(" and d.depth = :depth");
            } else {
                if (filter.getMinDepth() != null) {
                    sb.append(" and d.depth > :minDepth");
                }
                if (filter.getMaxDepth() != null) {
                    sb.append(" and d.depth <= :maxDepth");
                }
            }
        }
        sb.append(" group by o.fullObject, o.stringsCount,o.longsCount,o.datesCount,o.referencesCount,o.polysCount, o.name.orig order by o.name.orig asc");

        Query query = session.createQuery(sb.toString());
        updateQueryParameters(query, filter, countingObjects);

        return new RQueryImpl(query);
    }

    /**
     * This query is probably much faster than the standard one,
     * but it's using IN clause, we need to check it's performance. [lazyman]
     */
    private RQuery createOracleQuery(OrgFilter filter, Class<? extends ObjectType> type, Session session) {
        StringBuilder sb = new StringBuilder();
        sb.append("select o.fullObject,o.stringsCount,o.longsCount,o.datesCount,o.referencesCount,o.polysCount from ");
        sb.append(ClassMapper.getHQLType(type)).append(" as o where o.oid in (");
        sb.append("select distinct d.descendantOid from ROrgClosure as d where d.ancestorOid = :aOid");
        if (filter.getMinDepth() != null || filter.getMaxDepth() != null) {
            if (ObjectUtils.equals(filter.getMinDepth(), filter.getMaxDepth())) {
                sb.append(" and d.depth = :depth");
            } else {
                if (filter.getMinDepth() != null) {
                    sb.append(" and d.depth > :minDepth");
                }
                if (filter.getMaxDepth() != null) {
                    sb.append(" and d.depth <= :maxDepth");
                }
            }
        }
        sb.append(')');

        Query query = session.createQuery(sb.toString());
        updateQueryParameters(query, filter, false);

        return new RQueryImpl(query);
    }

    private void updateQueryParameters(Query query, OrgFilter filter, boolean countingObjects) {
        query.setString("aOid", filter.getOrgRef().getOid());
        if (filter.getMinDepth() != null || filter.getMaxDepth() != null) {
            if (ObjectUtils.equals(filter.getMinDepth(), filter.getMaxDepth())) {
                query.setInteger("depth", filter.getMinDepth());
            } else {
                if (filter.getMinDepth() != null) {
                    query.setInteger("minDepth", filter.getMinDepth());
                }
                if (filter.getMaxDepth() != null) {
                    query.setInteger("maxDepth", filter.getMaxDepth());
                }
            }
        }

        if (!countingObjects) {
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
        }
    }

    private RQuery countQuery(OrgFilter filter, Class type, Session session) {
        StringBuilder sb = new StringBuilder();
        if (ObjectType.class.equals(type)) {
            sb.append("select count(distinct d.descendantOid) from ROrgClosure d where d.ancestorOid = :aOid");
        } else {
            sb.append("select count(distinct d.descendantOid) from ").append(ClassMapper.getHQLType(type));
            sb.append(" as o left join o.descendants as d where d.ancestorOid = :aOid");
        }

        if (filter.getMinDepth() != null || filter.getMaxDepth() != null) {
            if (ObjectUtils.equals(filter.getMinDepth(), filter.getMaxDepth())) {
                sb.append(" and d.depth = :depth");
            } else {
                if (filter.getMinDepth() != null) {
                    sb.append(" and d.depth > :minDepth");
                }
                if (filter.getMaxDepth() != null) {
                    sb.append(" and d.depth <= :maxDepth");
                }
            }
        }

        Query query = session.createQuery(sb.toString());
        updateQueryParameters(query, filter, true);

        return new RQueryImpl(query);
    }
}
