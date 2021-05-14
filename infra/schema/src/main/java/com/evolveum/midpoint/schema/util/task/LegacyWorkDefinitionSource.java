/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

public class LegacyWorkDefinitionSource implements WorkDefinitionSource {

    @NotNull private final String taskHandlerUri;
    private final PrismContainerValue<?> taskExtension;

    private LegacyWorkDefinitionSource(@NotNull String taskHandlerUri, PrismContainerValue<?> taskExtension) {
        this.taskHandlerUri = taskHandlerUri;
        this.taskExtension = taskExtension;
    }

    @NotNull
    public static WorkDefinitionSource create(@NotNull String handlerUri,
            PrismContainer<? extends ExtensionType> extensionContainer) {
        PrismContainerValue<?> pcv = extensionContainer != null ? extensionContainer.getValue() : null;
        return new LegacyWorkDefinitionSource(handlerUri, pcv);
    }

    public @NotNull String getTaskHandlerUri() {
        return taskHandlerUri;
    }

    public PrismContainerValue<?> getTaskExtension() {
        return taskExtension;
    }

    @Override
    public String toString() {
        return "LegacyWorkDefinitionSource{" +
                "taskHandlerUri='" + taskHandlerUri + '\'' +
                ", taskExtension size=" + getExtensionSize() +
                '}';
    }

    private int getExtensionSize() {
        return taskExtension != null ? taskExtension.size() : 0;
    }
}
