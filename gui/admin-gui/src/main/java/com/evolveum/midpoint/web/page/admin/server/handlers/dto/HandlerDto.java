/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author mederly
 */
public class HandlerDto implements Serializable {
    public static final String F_OBJECT_REF_NAME = "objectRefName";
    public static final String F_OBJECT_REF = "objectRef";

    protected TaskDto taskDto;

    public HandlerDto(TaskDto taskDto) {
        this.taskDto = taskDto;
    }

    public TaskDto getTaskDto() {
        return taskDto;
    }

    public String getObjectRefName() {
        return taskDto.getObjectRefName();
    }

    public ObjectReferenceType getObjectRef() {
        return taskDto.getObjectRef();
    }

    public HandlerDtoEditableState getEditableState() {
        return null;
    }

    @NotNull
    public Collection<? extends ItemDelta<?, ?>> getDeltasToExecute(HandlerDtoEditableState origState, HandlerDtoEditableState currState, PrismContext prismContext)
            throws SchemaException {
        return new ArrayList<>();
    }
}
