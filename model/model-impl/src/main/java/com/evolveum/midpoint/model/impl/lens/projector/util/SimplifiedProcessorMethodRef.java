/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.util;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Typical processor "component-level" method that performs a well defined part of the computation.
 * This is the simplified version, i.e. without activityDescription.
 *
 * @param <X> Fake type parameter that is necessary to make type inference in partialProcessorExecute methods happy.
 */
@Experimental
@FunctionalInterface
public interface SimplifiedProcessorMethodRef<X extends ObjectType> {

    void run(LensContext<X> lensContext, XMLGregorianCalendar now, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException;

}
