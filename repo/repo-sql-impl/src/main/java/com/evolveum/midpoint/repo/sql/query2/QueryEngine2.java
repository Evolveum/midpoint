/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import org.hibernate.query.Query;
import org.hibernate.Session;

import java.util.Collection;

/**
 * @author lazyman
 */
public class QueryEngine2 {

    private static final Trace LOGGER = TraceManager.getTrace(QueryEngine2.class);

    private SqlRepositoryConfiguration repoConfiguration;
    private ExtItemDictionary extItemDictionary;
    private PrismContext prismContext;
    private final RelationRegistry relationRegistry;

    public QueryEngine2(SqlRepositoryConfiguration config, ExtItemDictionary extItemDictionary, PrismContext prismContext,
            RelationRegistry relationRegistry) {
        this.repoConfiguration = config;
        this.extItemDictionary = extItemDictionary;
        this.prismContext = prismContext;
        this.relationRegistry = relationRegistry;
    }

    public RQuery interpret(ObjectQuery query, Class<? extends Containerable> type,
            Collection<SelectorOptions<GetOperationOptions>> options,
            boolean countingObjects, Session session) throws QueryException {

        query = refineAssignmentHolderQuery(type, query);

        QueryInterpreter2 interpreter = new QueryInterpreter2(repoConfiguration, extItemDictionary);
        RootHibernateQuery hibernateQuery = interpreter.interpret(query, type, options, prismContext, relationRegistry, countingObjects, session);
        Query hqlQuery = hibernateQuery.getAsHqlQuery(session);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Query interpretation result:\n--- Query:\n{}\n--- with options: {}\n--- resulted in HQL:\n{}",
                    DebugUtil.debugDump(query), options, hqlQuery.getQueryString());

        }
        return new RQueryImpl(hqlQuery, hibernateQuery);
    }

    /** MID-5579
     * Both ObjectType and AssignmentHolderType are mapped to RObject. So when searching for AssignmentHolderType it is not sufficient to
     * query this table. This method hacks this situation a bit by introducing explicit type filter for AssignmentHolderType.
     */
    private ObjectQuery refineAssignmentHolderQuery(Class<? extends Containerable> type, ObjectQuery query) {
        if (!type.equals(AssignmentHolderType.class)) {
            return query;
        }
        if (query == null) {
            query = prismContext.queryFactory().createQuery();
        }
        query.setFilter(prismContext.queryFactory().createType(AssignmentHolderType.COMPLEX_TYPE, query.getFilter()));
        return query;
    }
}
