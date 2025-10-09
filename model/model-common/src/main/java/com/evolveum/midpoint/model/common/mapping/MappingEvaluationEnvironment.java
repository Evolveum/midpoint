/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.mapping;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * To avoid passing contextDescription + now + task to many places let us aggregate this information
 * in this class.
 *
 * TODO
 */
@SuppressWarnings("ClassCanBeRecord")
@Experimental
public class MappingEvaluationEnvironment {

    @NotNull public final String contextDescription;
    @NotNull public final XMLGregorianCalendar now;
    @NotNull public final Task task;

    public MappingEvaluationEnvironment(
            @NotNull String contextDescription,
            @NotNull XMLGregorianCalendar now,
            @NotNull Task task) {
        this.contextDescription = contextDescription;
        this.now = now;
        this.task = task;
    }

    public MappingEvaluationEnvironment createChild(String contextDescriptionPrefix) {
        return new MappingEvaluationEnvironment(contextDescriptionPrefix + contextDescription, now, task);
    }
}
