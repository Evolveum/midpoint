/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import java.util.Collection;

/**
 * Customization of an activity in a task template; e.g. for
 * {@link ModelInteractionService#submitTaskFromTemplate(String, ActivityCustomization, Task, OperationResult)}.
 */
public abstract class ActivityCustomization {

    @SuppressWarnings("WeakerAccess")
    public static @NotNull ActivityCustomization forQuery(QueryType query) {
        return new ObjectQuery(query);
    }

    public static @NotNull ActivityCustomization forOids(@NotNull Collection<String> oids) {
        var objectQuery = PrismContext.get().queryFor(ObjectType.class)
                .id(oids.toArray(new String[0]))
                .build();
        try {
            return forQuery(
                    PrismContext.get().getQueryConverter().createQueryType(
                            objectQuery));
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when creating in-oid query");
        }
    }

    /** Applies the customization. May directly modify the task template. */
    public abstract @NotNull ActivityDefinitionType applyTo(@NotNull TaskType taskTemplate);

    /**
     * Replacing the set of objects in the root activity.
     *
     * Supported for iterative scripting, iterative change execution, recomputation, object integrity check, reindexing,
     * trigger scan, focus validity scan, and deletion.
     *
     * See {@link WorkDefinitionUtil#replaceObjectSetQuery(WorkDefinitionsType, QueryType)}.
     */
    public static class ObjectQuery extends ActivityCustomization {
        @Nullable
        private final QueryType query;

        /** Query must be "detached" i.e. parent-less. */
        ObjectQuery(@Nullable QueryType query) {
            this.query = query;
        }

        @Override
        public @NotNull ActivityDefinitionType applyTo(@NotNull TaskType taskTemplate) {
            ActivityDefinitionType activityDef = taskTemplate.getActivity();
            if (activityDef == null) {
                throw new IllegalArgumentException("Only activity-based tasks can be customized in this way");
            }
            WorkDefinitionsType work = activityDef.getWork();
            if (work == null) {
                throw new IllegalArgumentException("Only single-work activity tasks can be customized in this way");
            }
            WorkDefinitionUtil.replaceObjectSetQuery(work, query);
            return activityDef;
        }
    }

    /** No change. */
    public static class None extends ActivityCustomization {
        @Override
        public @NotNull ActivityDefinitionType applyTo(@NotNull TaskType taskTemplate) {
            return MiscUtil.argNonNull(
                            taskTemplate.getActivity(),
                            "No activity definition in %s", taskTemplate);
        }
    }
}
