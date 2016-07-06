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

import com.evolveum.midpoint.prism.query.ExistsFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
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
        if (!(dataInstance.getJpaDefinition() instanceof JpaEntityDefinition)) {
            // should be checked when instantiating this restriction, so now we can throw hard exception
            throw new IllegalStateException("Internal error - resolutionState for ExistsRestriction points to non-entity node: " + dataInstance.getJpaDefinition());
        }
        setHqlDataInstance(dataInstance);

        InterpretationContext context = getContext();
        QueryInterpreter2 interpreter = context.getInterpreter();

		if (filter.getFilter() != null) {
			return interpreter.interpretFilter(context, filter.getFilter(), this);
		} else {
			throw new UnsupportedOperationException("Exists filter with 'all' subfilter is currently not supported");
		}
    }

    @Override
    public HqlEntityInstance getBaseHqlEntityForChildren() {
        return hqlDataInstance.asHqlEntityInstance();
    }
}
