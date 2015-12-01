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
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaAnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.VirtualAnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.JoinSpecification;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class AnyPropertyRestriction extends ItemValueRestriction<ValueFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(AnyPropertyRestriction.class);

    private JpaAnyDefinition jpaAnyDefinition;

    public AnyPropertyRestriction(InterpretationContext context, PropertyValueFilter filter, JpaEntityDefinition baseEntityDefinition,
                                  Restriction parent, JpaAnyDefinition jpaAnyDefinition) {
        super(context, filter, baseEntityDefinition, parent);
        Validate.notNull(jpaAnyDefinition, "anyDefinition");
        this.jpaAnyDefinition = jpaAnyDefinition;
    }

    @Override
    public Condition interpretInternal() throws QueryException {

        ItemDefinition itemDefinition = filter.getDefinition();
        QName itemName = itemDefinition.getName();
        QName itemType = itemDefinition.getTypeName();

        if (itemName == null || itemType == null) {
            throw new QueryException("Couldn't get name or type for queried item '" + itemDefinition + "'");
        }

        RObjectExtensionType ownerType;
        if (jpaAnyDefinition instanceof VirtualAnyDefinition) {
            ownerType = ((VirtualAnyDefinition) jpaAnyDefinition).getOwnerType();
        } else {
            ownerType = null;       // assignment extension has no ownerType
        }
        String anyAssociationName;        // longs, strings, ...
        try {
            anyAssociationName = RAnyConverter.getAnySetType(itemDefinition);
        } catch (SchemaException e) {
            throw new QueryException(e.getMessage(), e);
        }
        String hqlPath = getHqlDataInstance().getHqlPath();
        String alias = addJoinAny(hqlPath, anyAssociationName, itemName, ownerType);

        String propertyValuePath = alias + '.' + RAnyValue.F_VALUE;

        Object testedValue = getValue(((PropertyValueFilter) filter).getValues());
        Object value = RAnyConverter.getAggregatedRepoObject(testedValue);
        Condition c = createCondition(propertyValuePath, value, filter);

        return addIsNotNullIfNecessary(c, propertyValuePath);
    }

    private String addJoinAny(String currentHqlPath, String anyAssociationName, QName itemName, RObjectExtensionType ownerType) {
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        String joinedItemJpaName = anyAssociationName;
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias = hibernateQuery.createAlias(joinedItemJpaName, false);

        AndCondition conjunction = hibernateQuery.createAnd();
        if (ownerType != null) {        // null for assignment extensions
            conjunction.add(hibernateQuery.createEq(joinedItemAlias + ".ownerType", ownerType));
        }
        conjunction.add(hibernateQuery.createEq(joinedItemAlias + "." + RAnyValue.F_NAME, RUtil.qnameToString(itemName)));

        hibernateQuery.getPrimaryEntity().addJoin(new JoinSpecification(joinedItemAlias, joinedItemFullPath, conjunction));
        return joinedItemAlias;
    }

}
