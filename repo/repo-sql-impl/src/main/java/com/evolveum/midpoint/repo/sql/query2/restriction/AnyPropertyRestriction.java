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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.definition.AnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AnyPropertyRestriction extends ItemRestriction<ValueFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(AnyPropertyRestriction.class);

    public AnyPropertyRestriction(EntityDefinition rootEntityDefinition, String startPropertyPath, EntityDefinition startEntityDefinition) {
        super(rootEntityDefinition, startPropertyPath, startEntityDefinition);
    }

    @Override
    public Condition interpretInternal(String hqlPath) throws QueryException {

        ItemDefinition itemDefinition = filter.getDefinition();
        QName itemName = itemDefinition.getName();
        QName itemType = itemDefinition.getTypeName();

        if (itemName == null || itemType == null) {
            throw new QueryException("Couldn't get name or type for queried item '" + itemDefinition + "'");
        }

        ItemPath fullPath = getFullPath(filter.getFullPath());      // TODO does not work for cross-entities!
        RObjectExtensionType ownerType = fullPath.first().equivalent(new NameItemPathSegment(ObjectType.F_EXTENSION)) ?
                RObjectExtensionType.EXTENSION : RObjectExtensionType.ATTRIBUTES;
        String anyAssociationName = null;        // longs, strings, ...
        try {
            anyAssociationName = RAnyConverter.getAnySetType(itemDefinition);
        } catch (SchemaException e) {
            throw new QueryException(e.getMessage(), e);
        }
        String alias = addJoinAny(hqlPath, anyAssociationName, itemName, ownerType);

        String propertyValuePath = alias + '.' + RAnyValue.F_VALUE;

        Object testedValue = getValue(((PropertyValueFilter) filter).getValues());
        Object value = RAnyConverter.getAggregatedRepoObject(testedValue);
        Condition c = createCondition(propertyValuePath, value, filter);

        return addIsNotNullIfNecessary(c, propertyValuePath);
    }
}
