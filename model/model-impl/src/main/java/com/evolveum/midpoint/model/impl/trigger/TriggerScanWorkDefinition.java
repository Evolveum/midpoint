/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.trigger;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerScanWorkDefinitionType;

public class TriggerScanWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

    @NotNull private final ObjectSetType objects;

    TriggerScanWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
        var typedDefinition = (TriggerScanWorkDefinitionType) info.getBean();
        objects = ObjectSetUtil.emptyIfNull(typedDefinition.getObjects());
    }

    @Override
    public @NotNull ObjectSetType getObjectSetSpecification() {
        return objects;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "objects", objects, indent+1);
    }
}
