/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

public class ResourceObjectSetUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectSetUtil.class);

    public static void removeQuery(ResourceObjectSetType set) {
        if (set.getQuery() != null) {
            LOGGER.warn("Ignoring object query because the task does not support it: {}", set.getQuery());
            set.setQuery(null);
        }
    }

    public static void setDefaultQueryApplicationMode(ResourceObjectSetType set, ResourceObjectSetQueryApplicationModeType mode) {
        if (set.getQueryApplication() == null) {
            set.setQueryApplication(mode);
        }
    }

    /** Returns detached bean so that clients can freely modify it. */
    public static @NotNull ResourceObjectSetType fromConfiguration(ResourceObjectSetType resourceObjects) {
         return resourceObjects != null ? resourceObjects.clone() : new ResourceObjectSetType();
    }

    public static @Nullable ResourceObjectSetType fromTask(TaskType task){
        if (Objects.isNull(task) || Objects.isNull(task.getActivity()) || Objects.isNull(task.getActivity().getWork())) {
            return null;
        }
        WorkDefinitionsType work = task.getActivity().getWork();
        if (!Objects.isNull(work.getReconciliation()) && !Objects.isNull(work.getReconciliation().getResourceObjects())){
            return work.getReconciliation().getResourceObjects();
        }

        if (!Objects.isNull(work.getLiveSynchronization()) && !Objects.isNull(work.getLiveSynchronization().getResourceObjects())){
            return work.getLiveSynchronization().getResourceObjects();
        }

        if (!Objects.isNull(work.getImport()) && !Objects.isNull(work.getImport().getResourceObjects())){
            return work.getImport().getResourceObjects();
        }
        return null;
    }
}
