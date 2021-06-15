/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;

public class LegacyWorkDefinitionSource implements WorkDefinitionSource {

    @NotNull private final String taskHandlerUri;
    private final PrismContainerValue<?> taskExtension;
    private final ObjectReferenceType objectRef;

    private LegacyWorkDefinitionSource(@NotNull String taskHandlerUri, PrismContainerValue<?> taskExtension,
            ObjectReferenceType objectRef) {
        this.taskHandlerUri = taskHandlerUri;
        this.taskExtension = taskExtension;
        this.objectRef = objectRef;
    }

    @NotNull
    public static WorkDefinitionSource create(@NotNull String handlerUri,
            PrismContainer<? extends ExtensionType> extensionContainer, ObjectReferenceType objectRef) {
        PrismContainerValue<?> pcv = extensionContainer != null ? extensionContainer.getValue() : null;
        return new LegacyWorkDefinitionSource(handlerUri, pcv, objectRef);
    }

    public @NotNull String getTaskHandlerUri() {
        return taskHandlerUri;
    }

    public PrismContainerValue<?> getTaskExtension() {
        return taskExtension;
    }

    public ObjectReferenceType getObjectRef() {
        return objectRef;
    }

    @Override
    public String toString() {
        return "LegacyWorkDefinitionSource{" +
                "taskHandlerUri='" + taskHandlerUri + '\'' +
                ", taskExtension size=" + getExtensionSize() +
                ", objectRef=" + objectRef +
                '}';
    }

    private int getExtensionSize() {
        return taskExtension != null ? taskExtension.size() : 0;
    }
}
