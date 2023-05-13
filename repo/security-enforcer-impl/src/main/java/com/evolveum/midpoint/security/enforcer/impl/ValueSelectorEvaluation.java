/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/** EXPERIMENTAL, PoC CODE */
public class ValueSelectorEvaluation {

    @NotNull private final PrismValue value;
    @NotNull private final ItemValueSelectorType valueSelector;
    @NotNull private final PrismObject<? extends ObjectType> object;
    @NotNull private final AuthorizationEvaluation evaluation;

    public ValueSelectorEvaluation(
            @NotNull PrismValue value,
            @NotNull ItemValueSelectorType valueSelector,
            @NotNull PrismObject<? extends ObjectType> object,
            @NotNull AuthorizationEvaluation evaluation) {
        this.value = value;
        this.valueSelector = valueSelector;
        this.object = object;
        this.evaluation = evaluation;
    }

    public boolean matches() throws ConfigurationException, SchemaException {
        ValueSelectorType selector = valueSelector.getValue();
        if (selector == null) {
            // Empty selector means we match everything (is that OK?)
            return true;
        }
        Object realValue = value.getRealValue();

        SearchFilterType filterBean = selector.getFilter();
        if (filterBean != null) {
            if (!(value instanceof PrismContainerValue<?>)) {
                throw new UnsupportedOperationException("Filter clause can be used only on a container value");
            }
            PrismContainerDefinition<?> pcd = getPrismContainerDefinition(((PrismContainerValue<?>) value));
            ObjectFilter filter = PrismContext.get().getQueryConverter().parseFilter(filterBean, pcd);
            if (!filter.match((PrismContainerValue<?>) value, SchemaService.get().matchingRuleRegistry())) {
                return false;
            }
        }

        List<SubjectedObjectSelectorType> assigneeSelectors = selector.getAssignee();
        if (!assigneeSelectors.isEmpty()) {
            if (!(realValue instanceof AbstractWorkItemType)) {
                return false;
            }
            List<ObjectReferenceType> assigneeRefs = ((AbstractWorkItemType) realValue).getAssigneeRef();
            for (SubjectedObjectSelectorType assigneeSelector : assigneeSelectors) {
                if (!assigneeSelector.getSpecial().isEmpty()) {
                    for (ObjectReferenceType assigneeRef : assigneeRefs) {
                        var assigneeOid = assigneeRef.getOid();
                        if (assigneeOid != null && assigneeOid.equals(evaluation.op.getPrincipalOid())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }

    private PrismContainerDefinition<?> getPrismContainerDefinition(PrismContainerValue<?> value) {
        ComplexTypeDefinition ctd = value.getComplexTypeDefinition();
        if (ctd == null) {
            throw new UnsupportedOperationException(
                    "Filter clause cannot be used on a value without complex type definition: " + value);
        }
        return PrismContext.get().definitionFactory().createContainerDefinition(SchemaConstants.C_VALUE, ctd);
    }
}
