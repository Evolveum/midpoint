/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ActivityControlFlowDefinition implements DebugDumpable, Cloneable {

    /**
     * This bean is detached copy dedicated for this definition. It is therefore freely modifiable.
     */
    @NotNull private ActivityControlFlowSpecificationType bean;

    private ActivityControlFlowDefinition(@NotNull ActivityControlFlowSpecificationType bean) {
        this.bean = bean;
    }

    @NotNull
    public static ActivityControlFlowDefinition create(ActivityDefinitionType activityDefinitionBean) {
        ActivityControlFlowSpecificationType bean = activityDefinitionBean != null &&
                activityDefinitionBean.getControlFlow() != null ?
                activityDefinitionBean.getControlFlow().clone() : new ActivityControlFlowSpecificationType(PrismContext.get());
        return new ActivityControlFlowDefinition(bean);
    }

    @Override
    public String toString() {
        return bean.asPrismContainerValue().size() + " item(s)";
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump(indent);
    }

    public TaskErrorHandlingStrategyType getErrorHandlingStrategy() {
        return bean.getErrorHandling();
    }

    void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        if (tailoring.getControlFlow() != null) {
            bean = TailoringUtil.getTailoredBean(bean, tailoring.getControlFlow());
        } else {
            // null means we do not want it to change.
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityControlFlowDefinition clone() {
        return new ActivityControlFlowDefinition(bean.clone());
    }

    public void setSkip() {
        bean.setProcessingOption(PartialProcessingTypeType.SKIP);
    }

    public boolean isSkip() {
        return bean.getProcessingOption() == PartialProcessingTypeType.SKIP;
    }

    public @Nullable ExpressionType getItemProcessingCondition() {
        return bean.getItemProcessingCondition();
    }
}
