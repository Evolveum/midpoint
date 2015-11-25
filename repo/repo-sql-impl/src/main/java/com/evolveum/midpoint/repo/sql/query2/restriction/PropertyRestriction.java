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
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.PropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class PropertyRestriction extends ItemValueRestriction<ValueFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(PropertyRestriction.class);

    PropertyDefinition propertyDefinition;

    public PropertyRestriction(InterpretationContext context, ValueFilter filter, EntityDefinition baseEntityDefinition,
                               Restriction parent, PropertyDefinition propertyDefinition) {
        super(context, filter, baseEntityDefinition, parent);
        Validate.notNull(propertyDefinition, "propertyDefinition");
        this.propertyDefinition = propertyDefinition;
    }

    @Override
    public Condition interpretInternal(String hqlPath) throws QueryException {

        if (propertyDefinition.isLob()) {
            throw new QueryException("Can't query based on clob property value '" + propertyDefinition + "'.");
        }

        String propertyFullName;
        if (propertyDefinition.isCollection()) {
            propertyFullName = hqlPath;
        } else {
            propertyFullName = hqlPath + "." + propertyDefinition.getJpaName();
        }
        Object value = getValueFromFilter(filter, propertyDefinition);
        Condition condition = createCondition(propertyFullName, value, filter);

        return addIsNotNullIfNecessary(condition, propertyFullName);
    }
}
