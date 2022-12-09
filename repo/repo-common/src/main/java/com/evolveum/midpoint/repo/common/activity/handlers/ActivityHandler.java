/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.handlers;

import java.util.ArrayList;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.CandidateIdentifierFormatter;
import com.evolveum.midpoint.repo.common.activity.ActivityRunSupplier;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Spring component that ensures handling activity invocations.
 *
 * It is really minimalistic: its only responsibility is to instantiate appropriate {@link AbstractActivityRun} object.
 *
 * The naming is derived from the {@link TaskHandler}, to which it is conceptually somewhat similar.
 */
@Component
@Experimental
public interface ActivityHandler<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
        extends ActivityRunSupplier<WD, AH>, CandidateIdentifierFormatter {

    default ArrayList<Activity<?,?>> createChildActivities(Activity<WD, AH> activity) {
        return new ArrayList<>();
    }

    @Override
    @NotNull
    default String formatCandidateIdentifier(int iteration) {
        return String.format("%s:%d", getIdentifierPrefix(), iteration);
    }

    default String getIdentifierPrefix() {
        return getClass().getSimpleName(); // should be overridden as this does not look nice
    }

    /**
     * Returns state definition for standalone (root) activity paired with this handler.
     * Definitions for embedded activities are provided by activities themselves, which are returned
     * by {@link #createChildActivities(Activity)} method.
     */
    default @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal();
    }

    /**
     * Returns task archetype OID connected to this activity handler. When a generic task starts, it is given an archetype
     * based on the activity handler for the task's main activity. (If not set before.)
     */
    default @Nullable String getDefaultArchetypeOid() {
        return null;
    }
}
