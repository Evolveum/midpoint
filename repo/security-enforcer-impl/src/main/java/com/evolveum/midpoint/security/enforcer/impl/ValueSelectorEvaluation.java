/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

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

    public boolean matches() throws ConfigurationException {
        ValueSelectorType selector = valueSelector.getValue();
        configCheck(selector != null,
                "Selector is null? In: %s in %s", valueSelector, evaluation.getAuthorization());
        Object realValue = value.getRealValue();
        List<SubjectedObjectSelectorType> assigneeSelectors = selector.getAssignee();
        if (!assigneeSelectors.isEmpty()) {
            if (!(realValue instanceof CaseWorkItemType)) {
                return false;
            }
            List<ObjectReferenceType> assigneeRefs = ((CaseWorkItemType) realValue).getAssigneeRef();
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
}
