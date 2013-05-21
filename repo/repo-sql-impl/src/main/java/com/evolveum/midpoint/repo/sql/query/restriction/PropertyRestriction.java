/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query.definition.PropertyDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.hibernate.criterion.Criterion;

/**
 * @author lazyman
 */
public class PropertyRestriction extends ItemRestriction<ValueFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(PropertyRestriction.class);

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) throws QueryException {
        if (!super.canHandle(filter, context)) {
            return false;
        }

        ValueFilter valFilter = (ValueFilter) filter;

        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        ItemPath fullPath = createFullPath(valFilter);

        PropertyDefinition def = registry.findDefinition(context.getType(), fullPath, PropertyDefinition.class);

        return def != null;
    }

    @Override
    public Criterion interpretInternal(ValueFilter filter)
            throws QueryException {
        QueryContext context = getContext();

        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        ItemPath fullPath = createFullPath(filter);
        PropertyDefinition def = registry.findDefinition(context.getType(), fullPath, PropertyDefinition.class);
        if (def.isLob()) {
            throw new QueryException("Can't query based on clob property value '" + def + "'.");
        }

        String propertyName = def.getJpaName();
        Object value = getValueFromFilter(filter, def);

        return createCriterion(propertyName, value, filter);
    }

    @Override
    public PropertyRestriction cloneInstance() {
        return new PropertyRestriction();
    }
}
