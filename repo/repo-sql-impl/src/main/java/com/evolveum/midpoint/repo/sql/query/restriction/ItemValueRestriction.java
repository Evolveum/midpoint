/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.IsNotNullCondition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.IsNullCondition;
import com.evolveum.midpoint.repo.sql.query.matcher.Matcher;
import com.evolveum.midpoint.repo.sql.query.resolution.HqlDataInstance;
import com.evolveum.midpoint.repo.sqlbase.QueryException;

/**
 * Abstract superclass for all value-related filters. There are two major problems solved:
 * 1) mapping from ItemPath to HQL property paths
 * 2) adding joined entities to the query, along with necessary conditions
 * <p>
 * After the necessary entity is available, the fine work (creating one or more conditions
 * to execute the filtering) is done by subclasses of this path in the interpretInternal(..) method.
 *
 * @author lazyman
 */
public abstract class ItemValueRestriction<T extends ValueFilter> extends ItemRestriction<T> {

    ItemValueRestriction(InterpretationContext context, T filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, filter.getFullPath(), filter.getDefinition(), baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        ItemPath path = getItemPath();
        if (ItemPath.isEmpty(path)) {
            throw new QueryException("Null or empty path for ItemValueRestriction in " + filter.debugDump());
        }
        HqlDataInstance<?> dataInstance = getItemPathResolver().resolveItemPath(path, itemDefinition, getBaseHqlEntity(), false);
        setHqlDataInstance(dataInstance);

        return interpretInternal();
    }

    public abstract Condition interpretInternal() throws QueryException;

    <V> Condition createPropertyVsConstantCondition(
            String hqlPropertyPath, boolean extension, V value, ValueFilter<?, ?> filter) throws QueryException {
        ItemRestrictionOperation operation = findOperationForFilter(filter);

        InterpretationContext context = getContext();
        QueryInterpreter interpreter = context.getInterpreter();
        Matcher<V> matcher = interpreter.findMatcher(value);
        String matchingRule = filter.getMatchingRule() != null ? filter.getMatchingRule().getLocalPart() : null;

        // TODO treat null for multivalued properties (at least throw an exception!)
        return matcher.match(
                context.getHibernateQuery(), operation, hqlPropertyPath, extension, value, matchingRule);
    }

    ItemRestrictionOperation findOperationForFilter(ValueFilter filter) throws QueryException {
        ItemRestrictionOperation operation;
        if (filter instanceof EqualFilter) {
            operation = ItemRestrictionOperation.EQ;
        } else if (filter instanceof GreaterFilter gf) {
            operation = gf.isEquals() ? ItemRestrictionOperation.GE : ItemRestrictionOperation.GT;
        } else if (filter instanceof LessFilter lf) {
            operation = lf.isEquals() ? ItemRestrictionOperation.LE : ItemRestrictionOperation.LT;
        } else if (filter instanceof SubstringFilter substring) {
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
        } else if (val instanceof PrismPropertyValue propertyValue) {
            return propertyValue.getValue();
        } else {
            throw new QueryException("Non-property value in filter: " + filter + ": " + val.getClass());
        }
    }

    /**
     * Filter of type NOT(PROPERTY=VALUE) causes problems when there are entities with PROPERTY set to NULL.
     * <p>
     * Such a filter has to be treated like
     * <p>
     * NOT (PROPERTY=VALUE & PROPERTY IS NOT NULL)
     * <p>
     * TODO implement for restrictions other than PropertyRestriction.
     */
    Condition addIsNotNullIfNecessary(Condition condition, String propertyPath) {
        if (condition instanceof IsNullCondition || condition instanceof IsNotNullCondition) {
            return condition;
        }
        if (!isNegated()) {
            return condition;
        }
        HibernateQuery hibernateQuery = getContext().getHibernateQuery();
        AndCondition conjunction = hibernateQuery.createAnd();
        conjunction.add(condition);
        conjunction.add(hibernateQuery.createIsNotNull(propertyPath));
        return conjunction;
    }
}
