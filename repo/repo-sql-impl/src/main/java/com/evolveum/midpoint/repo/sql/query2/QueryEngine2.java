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

package com.evolveum.midpoint.repo.sql.query2;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.query.Query;
import org.hibernate.Session;

import java.util.Collection;

/**
 * @author lazyman
 */
public class QueryEngine2 {

    private static final Trace LOGGER = TraceManager.getTrace(QueryEngine2.class);

    private SqlRepositoryConfiguration repoConfiguration;
    private PrismContext prismContext;

    public QueryEngine2(SqlRepositoryConfiguration config, PrismContext prismContext) {
        this.repoConfiguration = config;
        this.prismContext = prismContext;
    }

    public RQuery interpret(ObjectQuery query, Class<? extends Containerable> type,
            Collection<SelectorOptions<GetOperationOptions>> options,
            boolean countingObjects, Session session) throws QueryException {

        QueryInterpreter2 interpreter = new QueryInterpreter2(repoConfiguration);
        RootHibernateQuery hibernateQuery = interpreter.interpret(query, type, options, prismContext, countingObjects, session);
        Query hqlQuery = hibernateQuery.getAsHqlQuery(session);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Query interpretation result:\n--- Query:\n{}\n--- with options: {}\n--- resulted in HQL:\n{}",
                    DebugUtil.debugDump(query), options, hqlQuery.getQueryString());

        }
        return new RQueryImpl(hqlQuery, hibernateQuery);
    }
}
