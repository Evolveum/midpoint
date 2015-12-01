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

import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.Container;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class InOidRestriction extends Restriction<InOidFilter> {

    public InOidRestriction(InterpretationContext context, InOidFilter filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        String hqlPath = getBaseHqlEntity().getHqlPath() + ".";
        Collection<?> idValues;

        // TODO check applicability
        if (filter.isConsiderOwner()) {
            hqlPath += "ownerOid";
            idValues = filter.getOids();
        } else if (RObject.class.isAssignableFrom(getBaseHqlEntity().getJpaDefinition().getJpaClass())) {
            hqlPath += "oid";
            idValues = filter.getOids();
        } else if (Container.class.isAssignableFrom(getBaseHqlEntity().getJpaDefinition().getJpaClass())) {
            hqlPath += "id";        // quite a hack
            idValues = toIntList(filter.getOids());
        } else {
            throw new QueryException("InOidRestriction cannot be applied to the entity: " + getBaseHqlEntity());
        }

        return getContext().getHibernateQuery().createIn(hqlPath, idValues);
    }

    private Collection<?> toIntList(Collection<String> ids) {
        List<Integer> rv = new ArrayList<>();
        for (String id : ids) {
            rv.add(Integer.parseInt(id));
        }
        return rv;
    }

}
