/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything necessary to carry out pre-clockwork inbounds evaluation.
 *
 * @param <F> the focus type
 */
public interface PreInboundsContext<F extends FocusType> extends DebugDumpable {

    @NotNull ShadowType getShadowedResourceObject();

    @NotNull F getPreFocus();

    default @NotNull PrismObject<F> getPreFocusAsPrismObject() {
        //noinspection unchecked
        return (PrismObject<F>) getPreFocus().asPrismObject();
    }

    @Nullable ObjectDelta<ShadowType> getResourceObjectDelta();

    @Nullable SystemConfigurationType getSystemConfiguration();

    @NotNull Task getTask();

    @NotNull ResourceType getResource();

    @NotNull ResourceObjectDefinition getObjectTypeDefinition() throws SchemaException, ConfigurationException;

    String getChannel();
}
