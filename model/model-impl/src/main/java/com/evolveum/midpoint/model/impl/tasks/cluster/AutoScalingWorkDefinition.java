/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.cluster;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAutoScalingWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.Duration;

public class AutoScalingWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

    @NotNull private final ObjectSetType tasks;
    private final Duration minReconciliationInterval;
    private final Duration maxReconciliationInterval;
    private final boolean skipInitialReconciliation;

    AutoScalingWorkDefinition(@NotNull WorkDefinitionBean source, @NotNull ConfigurationItemOrigin origin) {
        super(origin);
        var typedDefinition = (ActivityAutoScalingWorkDefinitionType) source.getBean();

        tasks = ObjectSetUtil.emptyIfNull(typedDefinition.getTasks());
        ObjectSetUtil.assumeObjectType(tasks, TaskType.COMPLEX_TYPE);

        minReconciliationInterval = typedDefinition.getMinReconciliationInterval();
        maxReconciliationInterval = typedDefinition.getMaxReconciliationInterval();
        skipInitialReconciliation = Boolean.TRUE.equals(typedDefinition.isSkipInitialReconciliation());
    }

    @Override
    public @NotNull ObjectSetType getObjectSetSpecification() {
        return tasks;
    }

    Duration getMinReconciliationInterval() {
        return minReconciliationInterval;
    }

    Duration getMaxReconciliationInterval() {
        return maxReconciliationInterval;
    }

    boolean isSkipInitialReconciliation() {
        return skipInitialReconciliation;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "tasks", tasks, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "minReconciliationInterval", String.valueOf(minReconciliationInterval), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "maxReconciliationInterval", String.valueOf(maxReconciliationInterval), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "skipInitialReconciliation", skipInitialReconciliation, indent + 1);
    }
}
