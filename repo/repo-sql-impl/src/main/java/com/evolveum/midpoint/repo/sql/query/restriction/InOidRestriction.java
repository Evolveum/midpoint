/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * @author lazyman
 */
public class InOidRestriction extends Restriction<InOidFilter> {

    @Override
    public Criterion interpret() throws QueryException {
        String property = getContext().getAlias(null) + ".oid";

        return Restrictions.in(property, filter.getOids());
    }

    @Override
    public boolean canHandle(ObjectFilter filter) throws QueryException {
        if (filter instanceof InOidFilter) {
            return true;
        }

        return false;
    }

    @Override
    public Restriction newInstance() {
        return new InOidRestriction();
    }
}
