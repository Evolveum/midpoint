/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Options for {@link ModelInteractionService#submit(ActivityDefinitionType, ActivitySubmissionOptions, Task, OperationResult)}.
 */
@Experimental
public record ActivitySubmissionOptions(
        @Nullable TaskType taskTemplate,
        @NotNull String[] archetypes,
        @Nullable FocusType owner) {

    public static ActivitySubmissionOptions create() {
        return new ActivitySubmissionOptions(null, new String[0], null);
    }

    /**
     * The provided task object will be used as a "starting point" when constructing the resulting task.
     * Beware, some parts (e.g., the activity definition) will be completely replaced.
     * See the {@link ModelInteractionService#submit(ActivityDefinitionType, ActivitySubmissionOptions, Task, OperationResult)}
     * implementation for the details.
     */
    public ActivitySubmissionOptions withTaskTemplate(@Nullable TaskType task) {
        return new ActivitySubmissionOptions(task, archetypes, owner);
    }

    /**
     * If present, the new task will have specified archetype OID(s), regardless of any other options.
     */
    public ActivitySubmissionOptions withArchetypes(@NotNull String... oids) {
        Arrays.stream(oids)
                .forEach(Objects::requireNonNull);
        return new ActivitySubmissionOptions(taskTemplate, oids, owner);
    }

    public ActivitySubmissionOptions withOwner(@Nullable FocusType owner) {
        return new ActivitySubmissionOptions(taskTemplate, archetypes, owner);
    }

    /** Creates or updates {@link #taskTemplate} with specified updater. */
    public ActivitySubmissionOptions updateTaskTemplate(@NotNull Consumer<TaskType> taskTemplateUpdater) {
        TaskType newTaskTemplate = taskTemplate != null ? taskTemplate.clone() : new TaskType();
        taskTemplateUpdater.accept(newTaskTemplate);
        return withTaskTemplate(newTaskTemplate);
    }
}
