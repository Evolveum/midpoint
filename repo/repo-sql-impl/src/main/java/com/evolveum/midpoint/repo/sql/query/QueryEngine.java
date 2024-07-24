/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query;

import java.util.Collection;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * @author lazyman
 */
public class QueryEngine {

    private static final Trace LOGGER = TraceManager.getTrace(QueryEngine.class);

    private final SqlRepositoryConfiguration repoConfiguration;
    private final ExtItemDictionary extItemDictionary;
    private final PrismContext prismContext;
    private final RelationRegistry relationRegistry;

    public QueryEngine(SqlRepositoryConfiguration config, ExtItemDictionary extItemDictionary,
            PrismContext prismContext, RelationRegistry relationRegistry) {
        this.repoConfiguration = config;
        this.extItemDictionary = extItemDictionary;
        this.prismContext = prismContext;
        this.relationRegistry = relationRegistry;
    }

    public RQuery interpret(ObjectQuery query, Class<? extends Containerable> type,
            Collection<SelectorOptions<GetOperationOptions>> options,
            boolean countingObjects, EntityManager em) throws QueryException {

        query = refineAssignmentHolderQuery(type, query);

        QueryInterpreter interpreter = new QueryInterpreter(repoConfiguration, extItemDictionary);
        HibernateQuery hibernateQuery = interpreter.interpret(query, type, options, prismContext, relationRegistry, countingObjects, em);
        Query hqlQuery = hibernateQuery.getAsHqlQuery(em);

        if (LOGGER.isTraceEnabled()) {
            String str = hqlQuery.unwrap(org.hibernate.query.Query.class).getQueryString();
            LOGGER.trace("Query interpretation result:\n--- Query:\n{}\n--- with options: {}\n--- resulted in HQL:\n{}",
                    DebugUtil.debugDump(query), options, str);
        }

        return new RQueryImpl(hqlQuery, hibernateQuery);
    }

    /**
     * MID-5579
     * Both ObjectType and AssignmentHolderType are mapped to RObject.
     * So when searching for AssignmentHolderType it is not sufficient to query this table.
     * This method hacks this situation a bit by introducing explicit type filter for AssignmentHolderType.
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
