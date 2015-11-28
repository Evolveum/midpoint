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

import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaPropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class PropertyRestriction extends ItemValueRestriction<ValueFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(PropertyRestriction.class);

    JpaPropertyDefinition jpaPropertyDefinition;

    public PropertyRestriction(InterpretationContext context, ValueFilter filter, JpaEntityDefinition baseEntityDefinition,
                               Restriction parent, JpaPropertyDefinition jpaPropertyDefinition) {
        super(context, filter, baseEntityDefinition, parent);
        Validate.notNull(jpaPropertyDefinition, "propertyDefinition");
        this.jpaPropertyDefinition = jpaPropertyDefinition;
    }

    @Override
    public Condition interpretInternal(String hqlPath) throws QueryException {

        if (jpaPropertyDefinition.isLob()) {
            throw new QueryException("Can't query based on clob property value '" + jpaPropertyDefinition + "'.");
        }

        String propertyFullName;
        if (jpaPropertyDefinition.isMultivalued()) {
            propertyFullName = hqlPath;
        } else {
            propertyFullName = hqlPath + "." + jpaPropertyDefinition.getJpaName();
        }
        Object value = getValueFromFilter(filter, jpaPropertyDefinition);
        Condition condition = createCondition(propertyFullName, value, filter);

        return addIsNotNullIfNecessary(condition, propertyFullName);
    }
}
