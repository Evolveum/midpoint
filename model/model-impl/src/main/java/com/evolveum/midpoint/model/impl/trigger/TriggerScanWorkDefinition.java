/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.trigger;

import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerScanWorkDefinitionType;

public class TriggerScanWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

    @NotNull private final ObjectSetType objects;

    TriggerScanWorkDefinition(@NotNull WorkDefinitionBean source) {
        var typedDefinition = (TriggerScanWorkDefinitionType) source.getBean();
        objects = ObjectSetUtil.fromConfiguration(typedDefinition.getObjects());
    }

    @Override
    public ObjectSetType getObjectSetSpecification() {
        return objects;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "objects", objects, indent+1);
    }
}
