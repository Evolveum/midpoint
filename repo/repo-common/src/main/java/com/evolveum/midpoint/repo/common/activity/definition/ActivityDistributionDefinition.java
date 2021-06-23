/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

public class ActivityDistributionDefinition implements DebugDumpable, Cloneable {

    @NotNull private WorkDistributionType bean;

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

    void applyChangeTailoring(@NotNull ActivityTailoringType tailoring) {
        if (tailoring.getDistribution() != null) {
            this.bean = tailoring.getDistribution();
        } else {
            // null means we do not want it to change.
        }
    }

    @Override
    public ActivityDistributionDefinition clone() {
        try {
            return (ActivityDistributionDefinition) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new SystemException(e);
        }
    }
}
