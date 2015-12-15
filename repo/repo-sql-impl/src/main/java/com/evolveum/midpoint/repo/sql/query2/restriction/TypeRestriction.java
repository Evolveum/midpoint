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

import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter2;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
public class TypeRestriction extends Restriction<TypeFilter> {

    public TypeRestriction(InterpretationContext context, TypeFilter filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        InterpretationContext context = getContext();
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();

        String property = getBaseHqlEntity().getHqlPath() + "." + RObject.F_OBJECT_TYPE_CLASS;

        Set<RObjectType> values = getValues(filter.getType());

        Condition basedOnType;
        if (values.size() > 1) {
            basedOnType = hibernateQuery.createIn(property, values);
        } else {
            basedOnType = hibernateQuery.createEq(property, values.iterator().next());
        }

        if (filter.getFilter() == null) {
            return basedOnType;
        }

        QueryInterpreter2 interpreter = context.getInterpreter();
        Condition basedOnFilter = interpreter.interpretFilter(context, filter.getFilter(), this);

        return hibernateQuery.createAnd(basedOnType, basedOnFilter);
    }

    private Set<RObjectType> getValues(QName typeQName) {
        Set<RObjectType> set = new HashSet<>();

        RObjectType type = ClassMapper.getHQLTypeForQName(typeQName);
        set.add(type);

        switch (type) {
            case OBJECT:
                set.addAll(Arrays.asList(RObjectType.values()));
                break;
            case FOCUS:
                set.add(RObjectType.USER);
            case ABSTRACT_ROLE:
                set.add(RObjectType.ROLE);
                set.add(RObjectType.ORG);
                break;
            default:
        }

        return set;
    }
}
