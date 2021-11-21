/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class LegacyWorkDefinitionSource implements WorkDefinitionSource {

    @NotNull private final TaskType taskBean;

    private LegacyWorkDefinitionSource(@NotNull TaskType taskBean) {
        this.taskBean = taskBean;
    }

    @NotNull
    public static WorkDefinitionSource create(@NotNull TaskType taskBean) {
        return new LegacyWorkDefinitionSource(taskBean);
    }

    public @NotNull TaskType getTaskBean() {
        return taskBean;
    }

    public @NotNull String getTaskHandlerUri() {
        return taskBean.getHandlerUri();
    }

    public @Nullable PrismContainerValue<?> getTaskExtension() {
        return taskBean.asPrismObject().getExtensionContainerValue();
    }

    public ObjectReferenceType getObjectRef() {
        return taskBean.getObjectRef();
    }

    @Override
    public String toString() {
        return "LegacyWorkDefinitionSource{" +
                "taskHandlerUri='" + getTaskHandlerUri() + '\'' +
                ", taskExtension size=" + getExtensionSize() +
                ", objectRef=" + getObjectRef() +
                '}';
    }

    private int getExtensionSize() {
        PrismContainerValue<?> taskExtension = getTaskExtension();
        return taskExtension != null ? taskExtension.size() : 0;
    }

    public <T> T getExtensionItemRealValue(ItemName name, Class<T> expectedClass) {
        PrismContainerValue<?> taskExtension = getTaskExtension();
        return taskExtension != null ? taskExtension.getItemRealValue(name, expectedClass) : null;
    }

    // TODO move to prism-api
    public @NotNull <T> Collection<T> getExtensionItemRealValues(ItemName name, Class<T> expectedClass) {
        PrismContainerValue<?> taskExtension = getTaskExtension();
        //noinspection unchecked
        return taskExtension != null ?
                taskExtension.getAllValues(name).stream()
                        .filter(Objects::nonNull)
                        .map(val -> (T) val.getRealValue())
                        .collect(Collectors.toList()) :
                List.of();
    }
}
