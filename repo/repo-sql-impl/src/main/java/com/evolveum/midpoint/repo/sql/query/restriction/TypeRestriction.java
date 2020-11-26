/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;

import javax.xml.namespace.QName;
import java.util.Collection;

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

        Collection<RObjectType> values = getValues(filter.getType());

        Condition basedOnType;
        if (values.size() > 1) {
            basedOnType = hibernateQuery.createIn(property, values);
        } else {
            basedOnType = hibernateQuery.createEq(property, values.iterator().next());
        }

        if (filter.getFilter() == null) {
            return basedOnType;
        }

        QueryInterpreter interpreter = context.getInterpreter();
        Condition basedOnFilter = interpreter.interpretFilter(context, filter.getFilter(), this);

        return hibernateQuery.createAnd(basedOnType, basedOnFilter);
    }

    private Collection<RObjectType> getValues(QName typeQName) {
        return ClassMapper.getDescendantsForQName(typeQName);
    }
}
