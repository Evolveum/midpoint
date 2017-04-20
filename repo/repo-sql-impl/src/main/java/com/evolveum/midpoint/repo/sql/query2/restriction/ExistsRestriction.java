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

import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDataNodeDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaPropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.resolution.HqlDataInstance;
import com.evolveum.midpoint.repo.sql.query2.resolution.HqlEntityInstance;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter2;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;

/**
 * @author mederly
 */
public class ExistsRestriction extends ItemRestriction<ExistsFilter> {

    public ExistsRestriction(InterpretationContext context, ExistsFilter filter, JpaEntityDefinition baseEntityDefinition,
                             Restriction parent) {
        super(context, filter, filter.getFullPath(), null, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        HqlDataInstance dataInstance = getItemPathResolver()
                .resolveItemPath(filter.getFullPath(), filter.getDefinition(), getBaseHqlEntity(), false);

        boolean isAll = filter.getFilter() == null || filter.getFilter() instanceof AllFilter;
		JpaDataNodeDefinition jpaDefinition = dataInstance.getJpaDefinition();
		if (!isAll) {
        	if (!(jpaDefinition instanceof JpaEntityDefinition)) {	// partially checked already (for non-null-ness)
             	throw new QueryException("ExistsRestriction with non-empty subfilter points to non-entity node: " + jpaDefinition);
        	}
        	setHqlDataInstance(dataInstance);
    	    QueryInterpreter2 interpreter = context.getInterpreter();
			return interpreter.interpretFilter(context, filter.getFilter(), this);
		} else if (jpaDefinition instanceof JpaPropertyDefinition && (((JpaPropertyDefinition) jpaDefinition).isCount())) {
			RootHibernateQuery hibernateQuery = context.getHibernateQuery();
			return hibernateQuery.createSimpleComparisonCondition(dataInstance.getHqlPath(), 0, ">");
		} else {
			// TODO support exists also for other properties (single valued or multi valued)
			throw new UnsupportedOperationException("Exists filter with 'all' subfilter is currently not supported");
		}
    }

    @Override
    public HqlEntityInstance getBaseHqlEntityForChildren() {
        return hqlDataInstance.asHqlEntityInstance();
    }
}
