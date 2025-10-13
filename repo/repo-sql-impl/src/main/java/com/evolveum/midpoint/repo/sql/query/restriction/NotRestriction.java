/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;

/**
 * @author lazyman
 */
public class NotRestriction extends UnaryLogicalRestriction<NotFilter> {

    public NotRestriction(InterpretationContext context, NotFilter filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        validateFilter();
        Condition condition = interpretChildFilter();
        return getContext().getHibernateQuery().createNot(condition);
    }
}
