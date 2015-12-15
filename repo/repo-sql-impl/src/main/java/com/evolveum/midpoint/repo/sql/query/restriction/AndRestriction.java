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

import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * @author lazyman
 */
public class AndRestriction extends NaryLogicalRestriction<AndFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter) {
        if (!super.canHandle(filter)) {
            return false;
        }

        return (filter instanceof AndFilter);
    }

    @Override
    public Criterion interpret() throws QueryException {
        validateFilter(filter);
        Conjunction conjunction = Restrictions.conjunction();
        updateJunction(filter.getConditions(), conjunction);

        return conjunction;
    }

    @Override
    public AndRestriction newInstance() {
        return new AndRestriction();
    }
}
