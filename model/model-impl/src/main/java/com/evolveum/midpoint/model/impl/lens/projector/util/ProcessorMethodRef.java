/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.util;

import com.evolveum.midpoint.model.impl.lens.ConflictDetectedException;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Typical processor "component-level" method that performs a well defined part of the computation.
 * This is the full version for focus-level, i.e. with activityDescription and without projection context.
 *
 * @param <X> Fake type parameter that is necessary to make type inference in partialProcessorExecute methods happy.
 */
@Experimental
@FunctionalInterface
public interface ProcessorMethodRef<X extends ObjectType> {

    void run(@NotNull LensContext<X> lensContext, @NotNull String activityDescription,
            @NotNull XMLGregorianCalendar now, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, ConflictDetectedException;

}
