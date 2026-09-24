/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.test;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.notifications.api.events.CustomEvent;
import com.evolveum.midpoint.prism.Safe;
import com.evolveum.midpoint.schema.expression.CustomVelocityExtension;

@SuppressWarnings("unused") // used from templates
@NullMarked
public class TestingVelocityExtension implements CustomVelocityExtension {

    @Override
    public String getVariableName() {
        return "custom";
    }

    @Safe
    public @Nullable Object getObject(CustomEvent event) {
        return event.getObject();
    }

    @Safe
    public @Nullable Object getValueAt(CustomEvent event, int index) {
        //noinspection unchecked
        return Objects.requireNonNull((List<PipelineItem>) getObject(event))
                .get(index)
                .getValue()
                .getRealValue();
    }
}
