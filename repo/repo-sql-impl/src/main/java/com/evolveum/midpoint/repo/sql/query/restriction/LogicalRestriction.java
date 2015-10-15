/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import org.hibernate.criterion.Criterion;

/**
 * @author lazyman
 */
public abstract class LogicalRestriction<T extends LogicalFilter> extends Restriction<T> {

    @Override
    public boolean canHandle(ObjectFilter filter) {
        if (filter instanceof LogicalFilter) {
            return true;
        }

        return false;
    }

    protected Criterion interpretChildFilter(ObjectFilter filter) throws QueryException {
        QueryContext context = getContext();
        QueryInterpreter interpreter = context.getInterpreter();
        return interpreter.interpretFilter(filter, context, this);
    }
}
