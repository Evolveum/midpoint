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
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.GreaterFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.resolution.HqlDataInstance;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter2;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.IsNotNullCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.IsNullCondition;
import com.evolveum.midpoint.repo.sql.query2.matcher.Matcher;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Abstract superclass for all value-related filters. There are two major problems solved:
 * 1) mapping from ItemPath to HQL property paths
 * 2) adding joined entities to the query, along with necessary conditions
 *
 * After the necessary entity is available, the fine work (creating one or more conditions
 * to execute the filtering) is done by subclasses of this path in the interpretInternal(..) method.
 *
 * @author lazyman
 * @author mederly
 */
public abstract class ItemValueRestriction<T extends ValueFilter> extends ItemRestriction<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemValueRestriction.class);

    public ItemValueRestriction(InterpretationContext context, T filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, filter.getFullPath(), filter.getDefinition(), baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {

    	ItemPath path = getItemPath();
        if (ItemPath.isNullOrEmpty(path)) {
            throw new QueryException("Null or empty path for ItemValueRestriction in " + filter.debugDump());
        }
        HqlDataInstance dataInstance = getItemPathResolver().resolveItemPath(path, itemDefinition, getBaseHqlEntity(), false);
        setHqlDataInstance(dataInstance);

        Condition condition = interpretInternal();
        return condition;
    }

    public abstract Condition interpretInternal() throws QueryException;

    protected Condition createPropertyVsConstantCondition(String hqlPropertyPath, Object value, ValueFilter filter) throws QueryException {
        ItemRestrictionOperation operation = findOperationForFilter(filter);

        InterpretationContext context = getContext();
        QueryInterpreter2 interpreter = context.getInterpreter();
        Matcher matcher = interpreter.findMatcher(value);
        String matchingRule = filter.getMatchingRule() != null ? filter.getMatchingRule().getLocalPart() : null;

        // TODO treat null for multivalued properties (at least throw an exception!)
        return matcher.match(context.getHibernateQuery(), operation, hqlPropertyPath, value, matchingRule);
    }

    protected ItemRestrictionOperation findOperationForFilter(ValueFilter filter) throws QueryException {
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
        return operation;
    }

    protected Object getValue(PropertyValueFilter filter) throws QueryException {
        PrismValue val = filter.getSingleValue();
        if (val == null) {
            return null;
        } else if (val instanceof PrismPropertyValue) {
            PrismPropertyValue propertyValue = (PrismPropertyValue) val;
            return propertyValue.getValue();
        } else {
            throw new QueryException("Non-property value in filter: " + filter + ": " + val.getClass());
        }
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
