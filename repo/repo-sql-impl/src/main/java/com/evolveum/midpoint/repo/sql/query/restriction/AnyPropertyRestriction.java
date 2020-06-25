/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 */
public class AnyPropertyRestriction extends PropertyRestriction {

    private static final Trace LOGGER = TraceManager.getTrace(AnyPropertyRestriction.class);

    public AnyPropertyRestriction(InterpretationContext context, PropertyValueFilter filter, JpaEntityDefinition baseEntityDefinition,
            Restriction parent, JpaLinkDefinition jpaLinkDefinition) {
        super(context, filter, baseEntityDefinition, parent, jpaLinkDefinition);
    }

    @Override
    public Condition interpretInternal() throws QueryException {

        String propertyValuePath = getHqlDataInstance().getHqlPath();

        if (filter.getRightHandSidePath() != null) {
            return createPropertyVsPropertyCondition(propertyValuePath);
        } else {
            Object value = RAnyConverter.getAggregatedRepoObject(getValue(filter));
            Condition c = createPropertyVsConstantCondition(propertyValuePath, value, filter);
            return addIsNotNullIfNecessary(c, propertyValuePath);
        }
    }
}
