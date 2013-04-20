/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query2.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query2.definition.PropertyDefinition;
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
