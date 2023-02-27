/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.executor;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.ChangeExecutionResult;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

class ElementChangeExecution<O extends ObjectType, E extends ObjectType> {

    @NotNull final LensContext<O> context;
    @NotNull final Task task;
    @NotNull final ModelBeans b = ModelBeans.get();

    /** The same object as is put into {@link LensElementContext#lastChangeExecutionResult} */
    @NotNull final ChangeExecutionResult<E> changeExecutionResult;

    ElementChangeExecution(@NotNull LensElementContext<E> elementContext, @NotNull Task task) {
        //noinspection unchecked
        this.context = (LensContext<O>) elementContext.getLensContext();
        this.task = task;
        this.changeExecutionResult = elementContext.setupLastChangeExecutionResult();
    }
}
