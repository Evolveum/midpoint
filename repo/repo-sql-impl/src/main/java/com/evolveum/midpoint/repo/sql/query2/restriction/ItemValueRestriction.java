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

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.GreaterFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter2;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.PropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.IsNotNullCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.IsNullCondition;
import com.evolveum.midpoint.repo.sql.query2.matcher.Matcher;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Abstract superclass for all value-related filters. There are two major problems solved:
 * 1) mapping from ItemPath to HQL property paths
 * 2) adding joined entities to the query, along with necessary conditions
 *    (there are two kinds of joins: left outer join and carthesian join)
 *
 * After the necessary entity is available, the fine work (creating one or more conditions
 * to execute the filtering) is done by subclasses of this path in the interpretInternal(..) method.
 *
 * @author lazyman
 * @author mederly
 */
public abstract class ItemValueRestriction<T extends ValueFilter> extends ItemRestriction<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemValueRestriction.class);

    public ItemValueRestriction(InterpretationContext context, T filter, EntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {

    	ItemPath path = filter.getFullPath();
        if (ItemPath.isNullOrEmpty(path)) {
            throw new QueryException("Null or empty path for ItemValueRestriction in " + filter.debugDump());
        }
        String hqlPropertyPath = getHelper().prepareJoins(path, getBaseHqlPath(), baseEntityDefinition);

        Condition condition = interpretInternal(hqlPropertyPath);
        return condition;
    }

    public abstract Condition interpretInternal(String hqlPath) throws QueryException;

    protected Condition createCondition(String propertyName, Object value, ValueFilter filter) throws QueryException {
        ItemRestrictionOperation operation;
        if (filter instanceof EqualFilter) {
            operation = ItemRestrictionOperation.EQ;
        } else if (filter instanceof GreaterFilter) {
            GreaterFilter gf = (GreaterFilter) filter;
            operation = gf.isEquals() ? ItemRestrictionOperation.GE : ItemRestrictionOperation.GT;
        } else if (filter instanceof LessFilter) {
            LessFilter lf = (LessFilter) filter;
            operation = lf.isEquals() ? ItemRestrictionOperation.LE : ItemRestrictionOperation.LT;
        } else if (filter instanceof SubstringFilter) {
            SubstringFilter substring = (SubstringFilter) filter;
            if (substring.isAnchorEnd()) {
                operation = ItemRestrictionOperation.ENDS_WITH;
            } else if (substring.isAnchorStart()) {
                operation = ItemRestrictionOperation.STARTS_WITH;
            } else {
                operation = ItemRestrictionOperation.SUBSTRING;
            }
        } else {
            throw new QueryException("Can't translate filter '" + filter + "' to operation.");
        }

        InterpretationContext context = getContext();
        QueryInterpreter2 interpreter = context.getInterpreter();
        Matcher matcher = interpreter.findMatcher(value);

        String matchingRule = null;
        if (filter.getMatchingRule() != null){
        	matchingRule = filter.getMatchingRule().getLocalPart();
        }
        
        return matcher.match(context.getHibernateQuery(), operation, propertyName, value, matchingRule);
    }

    protected Object getValue(List<? extends PrismValue> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        PrismValue val = values.get(0);
        if (val instanceof PrismPropertyValue) {
            PrismPropertyValue propertyValue = (PrismPropertyValue) val;
            return propertyValue.getValue();
        }

        return null;
    }

    protected Object getValueFromFilter(ValueFilter filter, PropertyDefinition def) throws QueryException {
        Object value;
        if (filter instanceof PropertyValueFilter) {
            value = getValue(((PropertyValueFilter) filter).getValues());
        } else {
            throw new QueryException("Unknown filter '" + filter + "', can't get value from it.");
        }

        //todo remove after some time [lazyman]
        //attempt to fix value type for polystring (if it was string in filter we create polystring from it)
        if (PolyString.class.equals(def.getJaxbType()) && (value instanceof String)) {
            LOGGER.debug("Trying to query PolyString value but filter contains String '{}'.", new Object[]{filter});
            value = new PolyString((String) value, (String) value);
        }
        //attempt to fix value type for polystring (if it was polystringtype in filter we create polystring from it)
        if (PolyString.class.equals(def.getJaxbType()) && (value instanceof PolyStringType)) {
            LOGGER.debug("Trying to query PolyString value but filter contains PolyStringType '{}'.", new Object[]{filter});
            PolyStringType type = (PolyStringType) value;
            value = new PolyString(type.getOrig(), type.getNorm());
        }

        if (String.class.equals(def.getJaxbType()) && (value instanceof QName)) {
            //eg. shadow/objectClass
            value = RUtil.qnameToString((QName) value);
        }

        if (value != null && !def.getJaxbType().isAssignableFrom(value.getClass())) {
            throw new QueryException("Value should be type of '" + def.getJaxbType() + "' but it's '"
                    + value.getClass() + "', filter '" + filter + "'.");
        }

        if (def.isEnumerated()) {
            value = getRepoEnumValue((Enum) value, def.getJpaType());
        }

        return value;
    }

    private Enum getRepoEnumValue(Enum schemaValue, Class repoType) throws QueryException {
        if (schemaValue == null) {
            return null;
        }

        if (SchemaEnum.class.isAssignableFrom(repoType)) {
            return (Enum) RUtil.getRepoEnumValue(schemaValue, repoType);
        }

        Object[] constants = repoType.getEnumConstants();
        for (Object constant : constants) {
            Enum e = (Enum) constant;
            if (e.name().equals(schemaValue.name())) {
                return e;
            }
        }

        throw new QueryException("Unknown enum value '" + schemaValue + "', which is type of '"
                + schemaValue.getClass() + "'.");
    }



    /**
     * Filter of type NOT(PROPERTY=VALUE) causes problems when there are entities with PROPERTY set to NULL.
     *
     * Such a filter has to be treated like
     *
     *      NOT (PROPERTY=VALUE & PROPERTY IS NOT NULL)
     *
     * TODO implement for restrictions other than PropertyRestriction.
     */
    protected Condition addIsNotNullIfNecessary(Condition condition, String propertyPath) {
        if (condition instanceof IsNullCondition || condition instanceof IsNotNullCondition) {
            return condition;
        }
        if (!isNegated()) {
            return condition;
        }
        RootHibernateQuery hibernateQuery = getContext().getHibernateQuery();
        AndCondition conjunction = hibernateQuery.createAnd();
        conjunction.add(condition);
        conjunction.add(hibernateQuery.createIsNotNull(propertyPath));
        return conjunction;
    }

}
