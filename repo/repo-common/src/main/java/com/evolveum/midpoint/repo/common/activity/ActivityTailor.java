/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityTailoring;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTailoringType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

class ActivityTailor {

    @NotNull private final Activity<?, ?> activity;
    @NotNull private final ActivityTailoring tailoring;
    @NotNull private final ArrayList<Activity<?, ?>> childrenList;

    ActivityTailor(@NotNull Activity<?, ?> activity, @NotNull ArrayList<Activity<?, ?>> childrenList) {
        this.activity = activity;
        this.tailoring = activity.getWorkDefinition().getActivityTailoring();
        this.childrenList = childrenList;
    }

    public void execute() throws SchemaException {
        for (ActivityTailoringType change : tailoring.getChanges()) {
            Activity<?, ?> child = findChild(change.getReference());
            child.applyChangeTailoring(change);
        }
    }

    private @NotNull Activity<?, ?> findChild(String reference) throws SchemaException {
        Objects.requireNonNull(reference, "An existing child identifier is required when tailoring");
        return MiscUtil.extractSingletonRequired(
                childrenList.stream()
                        .filter(ch -> reference.equals(ch.getIdentifier()))
                        .collect(Collectors.toList()),
                () -> new SchemaException("More than one child activity with identifier '" + reference + "' in " + activity),
                () -> new SchemaException("No child activity with identifier '" + reference + "' in " + activity));
    }
}
