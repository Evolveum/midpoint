/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Utils for managing work definition for specific activities.
 *
 * FIXME reconcile with {@link ActivityDefinitionBuilder}
 */
public class SpecificWorkDefinitionUtil {

    public static ActivityDefinitionType createExplicitChangeExecutionDef(
            @NotNull Collection<ObjectDelta<? extends ObjectType>> deltas,
            @Nullable ModelExecuteOptionsType options) throws SchemaException {

        ExplicitChangeExecutionWorkDefinitionType workDef = new ExplicitChangeExecutionWorkDefinitionType();

        for (ObjectDelta<?> delta : deltas) {
            workDef.getDelta().add(
                    DeltaConvertor.toObjectDeltaType(delta));
        }
        workDef.setExecutionOptions(options);

        // @formatter:off
        return new ActivityDefinitionType()
                .beginWork()
                    .explicitChangeExecution(workDef)
                .end();
        // @formatter:on
    }
}
