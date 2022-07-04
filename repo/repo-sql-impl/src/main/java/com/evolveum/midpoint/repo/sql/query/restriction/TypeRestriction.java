/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.ExistsCondition;
import com.evolveum.midpoint.repo.sql.query.resolution.HqlEntityInstance;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author lazyman
 */
public class TypeRestriction extends Restriction<TypeFilter> {

    public TypeRestriction(InterpretationContext context, TypeFilter filter, JpaEntityDefinition baseEntityDefinition, Restriction parent) {
        super(context, filter, baseEntityDefinition, parent);
    }

    @Override
    public Condition interpret() throws QueryException {
        Class<ObjectType> targetType = ObjectTypes.getObjectTypeClass(filter.getType());
        InterpretationContext subcontext = context.createSubcontext(targetType);
        HqlEntityInstance outerEntity = getBaseHqlEntity(); // parent query

        ExistsCondition existsCondition = new ExistsCondition(subcontext);
        existsCondition.addCorrelationCondition("oid", outerEntity.getHqlPath() + ".oid");
        existsCondition.interpretFilter(filter.getFilter());

        return existsCondition;
    }

    public Condition interpretOld() throws QueryException {
        InterpretationContext context = getContext();
        HibernateQuery hibernateQuery = context.getHibernateQuery();

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
