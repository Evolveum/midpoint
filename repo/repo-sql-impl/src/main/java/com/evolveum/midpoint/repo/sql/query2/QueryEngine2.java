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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;

import java.util.Collection;

/**
 * @author lazyman
 */
public class QueryEngine2 {

    private SqlRepositoryConfiguration repoConfiguration;
    private PrismContext prismContext;

    public QueryEngine2(SqlRepositoryConfiguration config, PrismContext prismContext) {
        this.repoConfiguration = config;
        this.prismContext = prismContext;
    }

    public RQuery interpret(ObjectQuery query, Class<? extends ObjectType> type,
                            Collection<SelectorOptions<GetOperationOptions>> options,
                            boolean countingObjects, Session session) throws QueryException {

        QueryInterpreter2 interpreter = new QueryInterpreter2(repoConfiguration);
        Criteria criteria = interpreter.interpret(query, type, options, prismContext, countingObjects, session);
        if (countingObjects) {
            criteria.setProjection(Projections.rowCount());
        } else {
            criteria.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
        }

        return new RQueryCriteriaImpl(criteria);
    }
}
