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

package com.evolveum.midpoint.repo.sql.query.custom;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query.RQueryImpl;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.hibernate.Query;
import org.hibernate.Session;

import java.util.Collection;

/**
 * @author lazyman
 *
 * CURRENTLY UNUSED.
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

    @Override
    public RQuery createQuery(ObjectQuery objectQuery, Class<? extends ObjectType> type,
                              Collection<SelectorOptions<GetOperationOptions>> options, boolean countingObjects,
                              Session session) {

        OrgFilter filter = (OrgFilter) objectQuery.getFilter();

        LOGGER.trace("createOrgQuery {}, counting={}, filter={}", new Object[]{type.getSimpleName(), countingObjects, filter});

        if (countingObjects) {
            return countQuery(filter, type, session);
        }

        StringBuilder sb = new StringBuilder();
        if (OrgFilter.Scope.ONE_LEVEL.equals(filter.getScope())) {
            sb.append("select o.fullObject,o.stringsCount,o.longsCount,o.datesCount,o.referencesCount,o.polysCount from ");
            sb.append(ClassMapper.getHQLType(type)).append(" as o where o.oid in (select distinct p.ownerOid from RParentOrgRef p where p.targetOid=:oid)");
        } else {
            sb.append("select o.fullObject,o.stringsCount,o.longsCount,o.datesCount,o.referencesCount,o.polysCount from ");
            sb.append(ClassMapper.getHQLType(type)).append(" as o where o.oid in (");
            //todo change to sb.append("select d.descendantOid from ROrgClosure as d where d.ancestorOid = :oid and d.descendantOid != :oid)");
            sb.append("select distinct d.descendantOid from ROrgClosure as d where d.ancestorOid = :oid and d.descendantOid != :oid)");
        }
        Query query = session.createQuery(sb.toString());
        query.setString("oid", filter.getOrgRef().getOid());
        query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);

        return new RQueryImpl(query);
    }

    private RQuery countQuery(OrgFilter filter, Class type, Session session) {
        StringBuilder sb = new StringBuilder();
        if (OrgFilter.Scope.ONE_LEVEL.equals(filter.getScope())) {
            sb.append("select count(distinct o.oid) from ");
            sb.append(ClassMapper.getHQLType(type)).append(" o left join o.parentOrgRef p where p.targetOid=:oid");
        } else {
            if (ObjectType.class.equals(type)) {
                //todo change to select count(*) from ROrgClosure d where d.ancestorOid = :oid
                sb.append("select count(distinct d.descendantOid) from ROrgClosure d where d.ancestorOid = :oid and d.descendantOid != :oid");
            } else {
                //todo change to sb.append("select count(d.descendantOid) from ").append(ClassMapper.getHQLType(type));
                sb.append("select count(distinct d.descendantOid) from ").append(ClassMapper.getHQLType(type));
                sb.append(" as o left join o.descendants as d where d.ancestorOid = :oid and d.descendantOid != :oid");
            }
        }
        Query query = session.createQuery(sb.toString());
        query.setString("oid", filter.getOrgRef().getOid());

        return new RQueryImpl(query);
    }
}
