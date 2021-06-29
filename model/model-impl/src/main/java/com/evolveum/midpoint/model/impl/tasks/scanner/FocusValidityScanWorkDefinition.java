/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.schema.util.task.work.*;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil.getTimeValidityConstraints;
import static com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil.hasNotificationActions;

public class FocusValidityScanWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

    @NotNull private final ObjectSetType objects;
    @NotNull private final ValidityScanQueryStyleType queryStyle;
    private final TimeValidityPolicyConstraintType validityConstraint;

    FocusValidityScanWorkDefinition(WorkDefinitionSource source) {
        if (source instanceof LegacyWorkDefinitionSource) {
            LegacyWorkDefinitionSource legacySource = (LegacyWorkDefinitionSource) source;
            objects = ObjectSetUtil.fromLegacySource(legacySource);
            queryStyle = ValidityScanQueryStyleType.SINGLE_QUERY;
            validityConstraint = getNotificationEnabledValidityPolicyConstraintFromTask(legacySource.getTaskBean());
        } else {
            FocusValidityScanWorkDefinitionType typedDefinition = (FocusValidityScanWorkDefinitionType)
                    ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
            objects = typedDefinition.getObjects() != null ?
                    typedDefinition.getObjects() : new ObjectSetType(PrismContext.get());
            queryStyle = typedDefinition.getQueryStyle();
            validityConstraint = typedDefinition.getValidityConstraint();
        }
    }

    @Override
    public ObjectSetType getObjectSetSpecification() {
        return objects;
    }

    public @NotNull ValidityScanQueryStyleType getQueryStyle() {
        return queryStyle;
    }

    public TimeValidityPolicyConstraintType getValidityConstraint() {
        return validityConstraint;
    }

    private TimeValidityPolicyConstraintType getNotificationEnabledValidityPolicyConstraintFromTask(TaskType taskBean) {
        PolicyRuleType policyRule = taskBean.getPolicyRule();
        List<TimeValidityPolicyConstraintType> timeValidityConstraints = getTimeValidityConstraints(policyRule);
        if (timeValidityConstraints.isEmpty() || !hasNotificationActions(policyRule)) {
            return null;
        } else {
            return timeValidityConstraints.iterator().next();
        }
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "queryStyle", queryStyle, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "validityConstraint", validityConstraint, indent+1);
    }
}
