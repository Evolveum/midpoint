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
import com.evolveum.midpoint.repo.sql.query2.definition.CollectionDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.PropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class CollectionRestriction extends ItemValueRestriction<ValueFilter> {

    private PropertyDefinition collectionPropertyDefinition;

    public CollectionRestriction(InterpretationContext context, ValueFilter filter, EntityDefinition baseEntityDefinition,
                                 Restriction parent, PropertyDefinition collectionPropertyDefinition) {
        super(context, filter, baseEntityDefinition, parent);
        Validate.notNull(collectionPropertyDefinition, "collectionPropertyDefinition");
        this.collectionPropertyDefinition = collectionPropertyDefinition;
    }


    @Override
    public Condition interpretInternal(String hqlPath) throws QueryException {
        Object value = getValueFromFilter(filter, collectionPropertyDefinition);

        return createCondition(hqlPath, value, filter);

        // TODO what about not-null ?
    }

}
