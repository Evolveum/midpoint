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

import com.evolveum.midpoint.prism.query.UnaryLogicalFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter2;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public abstract class UnaryLogicalRestriction<T extends UnaryLogicalFilter> extends LogicalRestriction<T> {

    private static final Trace LOGGER = TraceManager.getTrace(UnaryLogicalRestriction.class);

    public UnaryLogicalRestriction(InterpretationContext context, T filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    protected Condition interpretChildFilter() throws QueryException {
        InterpretationContext context = getContext();
        QueryInterpreter2 interpreter = context.getInterpreter();
        return interpreter.interpretFilter(context, filter.getFilter(), this);
    }

    protected void validateFilter() throws QueryException {
        if (filter.getFilter() == null) {
            LOGGER.trace("UnaryLogicalFilter filter must have child filter defined in it.");
            throw new QueryException("UnaryLogicalFilter '" + filter.debugDump()
                    + "' must have child filter defined in it.");
        }
    }
}
