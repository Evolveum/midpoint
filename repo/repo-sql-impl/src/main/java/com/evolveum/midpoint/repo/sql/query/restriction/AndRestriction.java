/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;

/**
 * @author lazyman
 * @author mederly
 */
public class AndRestriction extends NaryLogicalRestriction<AndFilter> {

    public AndRestriction(InterpretationContext context, AndFilter filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        validateFilter();
        AndCondition conjunction = getContext().getHibernateQuery().createAnd();
        updateJunction(filter.getConditions(), conjunction);
        return conjunction;
    }

}
