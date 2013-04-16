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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryContext;
import com.evolveum.midpoint.repo.sql.query2.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query2.definition.CollectionDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.PropertyDefinition;
import org.hibernate.criterion.Criterion;

/**
 * @author lazyman
 */
public class CollectionRestriction extends ItemRestriction<ValueFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) throws QueryException {
        if (!super.canHandle(filter, context)) {
            return false;
        }

        ValueFilter valFilter = (ValueFilter) filter;
        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        ItemPath fullPath = createFullPath(valFilter);

        CollectionDefinition def = registry.findDefinition(context.getType(), fullPath, CollectionDefinition.class);
        if (def == null) {
            return false;
        }

        return def.getDefinition() instanceof PropertyDefinition;
    }

    @Override
    public Criterion interpretInternal(ValueFilter filter) throws QueryException {
        ItemPath fullPath = createFullPath(filter);
        QueryContext context = getContext();
        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        CollectionDefinition def = registry.findDefinition(context.getType(), fullPath, CollectionDefinition.class);

        String alias = context.getAlias(fullPath);
        Object value = getValueFromFilter(filter, (PropertyDefinition) def.getDefinition());

        //custom propertyPath handling for PolyString (it's embedded entity, not a primitive)
        if (value instanceof PolyString) {
            return createCriterion(alias, value, filter);
        }

        return createCriterion(alias + ".elements", value, filter);
    }

    @Override
    public Restriction cloneInstance() {
        return new CollectionRestriction();
    }
}
