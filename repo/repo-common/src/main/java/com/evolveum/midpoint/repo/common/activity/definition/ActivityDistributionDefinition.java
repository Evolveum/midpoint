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

import java.util.function.Supplier;

import static com.evolveum.midpoint.util.MiscUtil.or0;

public class ActivityDistributionDefinition implements DebugDumpable, Cloneable {

    /**
     * This bean is detached copy dedicated for this definition. It is therefore freely modifiable.
     */
    @NotNull private WorkDistributionType bean;

    private ActivityDistributionDefinition(@NotNull WorkDistributionType bean) {
        this.bean = bean;
    }

    @NotNull
    public static ActivityDistributionDefinition create(ActivityDefinitionType activityDefinitionBean,
            Supplier<Integer> workerThreadsSupplier) {
        WorkDistributionType bean = activityDefinitionBean != null && activityDefinitionBean.getDistribution() != null ?
                activityDefinitionBean.getDistribution().clone() : new WorkDistributionType(PrismContext.get());
        if (bean.getWorkerThreads() == null) {
            bean.setWorkerThreads(workerThreadsSupplier.get());
        }
        return new ActivityDistributionDefinition(bean);
    }

    @Override
    public String toString() {
        return bean.asPrismContainerValue().size() + " item(s)";
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump(indent);
    }

    public WorkBucketsManagementType getBuckets() {
        return bean.getBuckets();
    }

    public boolean hasBuckets() {
        return bean.getBuckets() != null;
    }

    public boolean isSubtask() {
        return bean.getSubtask() != null;
    }

    public boolean hasWorkers() {
        return bean.getWorkers() != null;
    }

    public WorkersManagementType getWorkers() {
        return bean.getWorkers();
    }

    public int getWorkerThreads() {
        return or0(bean.getWorkerThreads());
    }

    void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        if (tailoring.getDistribution() != null) {
            bean = TailoringUtil.getTailoredBean(bean, tailoring.getDistribution());
        } else {
            // null means we do not want it to change.
        }
    }

    void applySubtaskTailoring(@NotNull ActivitySubtaskSpecificationType subtaskSpecification) {
        if (bean.getSubtask() == null) {
            bean.setSubtask(subtaskSpecification.clone());
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public ActivityDistributionDefinition clone() {
        return new ActivityDistributionDefinition(bean.clone());
    }
}
