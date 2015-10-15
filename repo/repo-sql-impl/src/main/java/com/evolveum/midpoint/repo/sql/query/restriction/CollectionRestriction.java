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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.definition.CollectionDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.PropertyDefinition;
import org.hibernate.criterion.Criterion;

/**
 * @author lazyman
 */
public class CollectionRestriction extends ItemRestriction<ValueFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter) throws QueryException {
        if (!super.canHandle(filter)) {
            return false;
        }

        ValueFilter valFilter = (ValueFilter) filter;
        ItemPath fullPath = valFilter.getFullPath();

        CollectionDefinition def = findProperDefinition(fullPath, CollectionDefinition.class);
        if (def == null) {
            return false;
        }

        return def.getDefinition() instanceof PropertyDefinition;
    }

    @Override
    public Criterion interpretInternal(ValueFilter filter) throws QueryException {
        ItemPath fullPath = filter.getFullPath();
        QueryContext context = getContext();
        CollectionDefinition def = findProperDefinition(fullPath, CollectionDefinition.class);

        String alias = context.getAlias(fullPath);
        Object value = getValueFromFilter(filter, (PropertyDefinition) def.getDefinition());

        // TODO what about not-null ?

        //custom propertyPath handling for PolyString (it's embedded entity, not a primitive)
        if (value instanceof PolyString) {
            return createCriterion(alias, value, filter);
        }

        return createCriterion(alias + ".elements", value, filter);
    }

    @Override
    public Restriction newInstance() {
        return new CollectionRestriction();
    }
}
