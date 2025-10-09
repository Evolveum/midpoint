/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.JunctionCondition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class NaryLogicalRestriction<T extends NaryLogicalFilter> extends LogicalRestriction<T> {

    private static final Trace LOGGER = TraceManager.getTrace(NaryLogicalRestriction.class);
    private List<Restriction> restrictions;

    public NaryLogicalRestriction(InterpretationContext context, T filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    public List<Restriction> getRestrictions() {
        if (restrictions == null) {
            restrictions = new ArrayList<>();
        }
        return restrictions;
    }

    protected void validateFilter() throws QueryException {
        if (filter.getConditions() == null || filter.getConditions().isEmpty()) {
            LOGGER.trace("NaryLogicalFilter filter must have at least two conditions in it. " +
                    "Removing logical filter and processing simple condition.");
            throw new QueryException("NaryLogicalFilter filter '" + filter.debugDump()
                    + "' must have at least two conditions in it. Removing logical filter and processing simple condition.");
        }
    }

    protected void updateJunction(List<? extends ObjectFilter> subfilters, JunctionCondition junction) throws QueryException {

        InterpretationContext context = getContext();
        QueryInterpreter interpreter = context.getInterpreter();

        for (ObjectFilter subfilter : subfilters) {
            Condition condition = interpreter.interpretFilter(context, subfilter, this);
            junction.add(condition);
        }
    }
}
