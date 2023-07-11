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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropagationWorkDefinitionType;

import org.jetbrains.annotations.NotNull;

public class PropagationWorkDefinition extends AbstractWorkDefinition {

    private final String resourceOid;

    PropagationWorkDefinition(@NotNull WorkDefinitionBean source) {
        ObjectReferenceType resourceRef = ((PropagationWorkDefinitionType) source.getBean()).getResourceRef();
        resourceOid = resourceRef != null ? resourceRef.getOid() : null;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "resourceOid", resourceOid, indent+1);
    }
}
