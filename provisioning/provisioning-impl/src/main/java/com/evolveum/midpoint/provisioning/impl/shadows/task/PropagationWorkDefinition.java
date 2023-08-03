/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.task;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public class PropagationWorkDefinition extends AbstractWorkDefinition {

    @NotNull private final String resourceOid;

    PropagationWorkDefinition(@NotNull WorkDefinitionBean source, @NotNull QName activityTypeName) throws ConfigurationException {
        super(activityTypeName);
        ObjectReferenceType resourceRef = ((PropagationWorkDefinitionType) source.getBean()).getResourceRef();
        resourceOid = MiscUtil.configNonNull(
                resourceRef != null ? resourceRef.getOid() : null,
                "No resource OID specified");
    }

    public @NotNull String getResourceOid() {
        return resourceOid;
    }

    @Override
    public @Nullable TaskAffectedObjectsType getAffectedObjects() {
        return new TaskAffectedObjectsType()
                .resourceObjects(new ActivityAffectedResourceObjectsType()
                        .activityType(getActivityTypeName())
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE));
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "resourceOid", resourceOid, indent+1);
    }
}
