/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketsManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDistributionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkersManagementType;

import org.jetbrains.annotations.NotNull;

public class ActivityDistributionDefinition implements DebugDumpable {

    @NotNull private final WorkDistributionType bean;

    private ActivityDistributionDefinition(ActivityDefinitionType activityDefinitionBean) {
        this.bean = activityDefinitionBean != null && activityDefinitionBean.getDistribution() != null ?
                activityDefinitionBean.getDistribution() : new WorkDistributionType(PrismContext.get());
    }

    @NotNull
    public static ActivityDistributionDefinition create(ActivityDefinitionType activityDefinitionBean) {
        return new ActivityDistributionDefinition(activityDefinitionBean);
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

    public boolean isSubtask() {
        return bean.getSubtask() != null;
    }

    public boolean hasWorkers() {
        return bean.getWorkers() != null;
    }

    public WorkersManagementType getWorkers() {
        return bean.getWorkers();
    }
}
