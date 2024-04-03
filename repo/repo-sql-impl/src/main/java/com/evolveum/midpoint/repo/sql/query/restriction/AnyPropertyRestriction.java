/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.ROExtPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JpaAnyPropertyDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaPropertyDefinition;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sql.query.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query.hqm.condition.Condition;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * @author lazyman
 */
public class AnyPropertyRestriction extends PropertyRestriction {

    public AnyPropertyRestriction(
            InterpretationContext context,
            PropertyValueFilter<?> filter,
            JpaEntityDefinition baseEntityDefinition,
            Restriction<?> parent,
            JpaLinkDefinition<JpaPropertyDefinition> jpaLinkDefinition) {
        super(context, filter, baseEntityDefinition, parent, jpaLinkDefinition);
    }

    @Override
    public Condition interpretInternal() throws QueryException {

        JpaPropertyDefinition targetDefinition = linkDefinition.getTargetDefinition();

        // "Regular" definitions are treated in PropertyRestriction
        assert targetDefinition instanceof JpaAnyPropertyDefinition;

        String propertyPath = getHqlDataInstance().getHqlPath();
        String propertyValuePath;
        if (PolyString.class.equals(targetDefinition.getJaxbClass())
                && QNameUtil.match(PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME, filter.getMatchingRule())) {
            // Ugly hack, just to support this particular matching rule. (E.g., the "strict" rule is not available here.)
            propertyValuePath = propertyPath + "." + ROExtPolyString.F_NORM;
        } else {
            propertyValuePath = propertyPath + "." + RAnyValue.F_VALUE;
        }

        if (filter.getRightHandSidePath() != null) {
            return createPropertyVsPropertyCondition(propertyValuePath);
        } else {
            Object value = RAnyConverter.getAggregatedRepoObject(getValue(filter));
            Condition c = createPropertyVsConstantCondition(propertyPath, true, value, filter);
            return addIsNotNullIfNecessary(c, propertyValuePath);
        }
    }
}
