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

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public class AnyPropertyRestriction extends PropertyRestriction {

    private static final Trace LOGGER = TraceManager.getTrace(AnyPropertyRestriction.class);

    public AnyPropertyRestriction(InterpretationContext context, PropertyValueFilter filter, JpaEntityDefinition baseEntityDefinition,
            Restriction parent, JpaLinkDefinition jpaLinkDefinition) {
        super(context, filter, baseEntityDefinition, parent, jpaLinkDefinition);
    }

    @Override
    public Condition interpretInternal() throws QueryException {

        String propertyValuePath = getHqlDataInstance().getHqlPath();

        if (filter.getRightHandSidePath() != null) {
            return createPropertyVsPropertyCondition(propertyValuePath);
        } else {
            Object value = RAnyConverter.getAggregatedRepoObject(getValue(filter));
            Condition c = createPropertyVsConstantCondition(propertyValuePath, value, filter);
            return addIsNotNullIfNecessary(c, propertyValuePath);
        }
    }
}
