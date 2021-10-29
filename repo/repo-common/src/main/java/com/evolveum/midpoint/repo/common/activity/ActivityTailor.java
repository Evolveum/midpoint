/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityTailoring;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySubtaskDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityTailoringType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tailors (modifies) an activity according to specified {@link ActivityTailoring}.
 */
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
            findChildren(change.getReference())
                    .forEach(child -> child.applyChangeTailoring(change));
        }
        ActivitySubtaskDefinitionType subtasksForChildren = tailoring.getSubtasksForChildren();
        if (subtasksForChildren != null) {
            childrenList.forEach(
                    child -> child.applySubtaskTailoring(subtasksForChildren));
        }
    }

    private @NotNull Collection<Activity<?, ?>> findChildren(List<String> references) throws SchemaException {
        if (!references.isEmpty()) {
            List<Activity<?, ?>> matching = childrenList.stream()
                    .filter(ch -> references.contains(ch.getIdentifier()))
                    .collect(Collectors.toList());
            if (matching.isEmpty()) {
                throw new SchemaException("No child activity matching " + references + " in " + activity);
            } else {
                return matching;
            }
        } else {
            return childrenList;
        }
    }
}
