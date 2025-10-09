/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.OrCondition;

/**
 * @author lazyman
 */
public class OrRestriction extends NaryLogicalRestriction<OrFilter> {

    public OrRestriction(InterpretationContext context, OrFilter filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        validateFilter();
        OrCondition disjunction = getContext().getHibernateQuery().createOr();
        updateJunction(filter.getConditions(), disjunction);
        return disjunction;
    }
}
