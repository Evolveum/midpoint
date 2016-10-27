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

import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * @author lazyman
 */
public class NotRestriction extends UnaryLogicalRestriction<NotFilter> {

    @Override
    public boolean canHandle(ObjectFilter filter) {
        if (!super.canHandle(filter)) {
            return false;
        }

        return (filter instanceof NotFilter);
    }


    @Override
    public Criterion interpret() throws QueryException {
        validateFilter(filter);
        Criterion criterion = interpretChildFilter(filter.getFilter());

        return Restrictions.not(criterion);
    }

    @Override
    public NotRestriction newInstance() {
        return new NotRestriction();
    }
}
