/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Defines the distribution aspects of an activity: buckets, worker tasks, worker threads, subtasks, and so on.
 */
public class ActivityDistributionDefinition implements DebugDumpable, Cloneable {

    /**
     * This bean is detached copy dedicated for this definition. It is therefore freely modifiable.
     */
    @NotNull private ActivityDistributionDefinitionType bean;

    private ActivityDistributionDefinition(@NotNull ActivityDistributionDefinitionType bean) {
        this.bean = bean;
    }

    @NotNull
    public static ActivityDistributionDefinition create(ActivityDefinitionType activityDefinitionBean) {
        ActivityDistributionDefinitionType bean =
                activityDefinitionBean != null && activityDefinitionBean.getDistribution() != null ?
                        activityDefinitionBean.getDistribution().clone() : new ActivityDistributionDefinitionType();
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

    public BucketsDefinitionType getBuckets() {
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

    public WorkersDefinitionType getWorkers() {
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

    void applySubtaskTailoring(@NotNull ActivitySubtaskDefinitionType subtaskSpecification) {
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
