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

import com.evolveum.midpoint.prism.query.NaryLogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter2;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.JunctionCondition;
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
        QueryInterpreter2 interpreter = context.getInterpreter();

        for (ObjectFilter subfilter : subfilters) {
            Condition condition = interpreter.interpretFilter(context, subfilter, this);
            junction.add(condition);
        }
    }
}
