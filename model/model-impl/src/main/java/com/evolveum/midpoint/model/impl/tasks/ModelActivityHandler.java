/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;

import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;

import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class ModelActivityHandler<WD extends WorkDefinition, AH extends ModelActivityHandler<WD, AH>>
        implements ActivityHandler<WD, AH> {

    @Autowired protected ActivityHandlerRegistry handlerRegistry;
    @Autowired protected ModelBeans beans;
    @Autowired protected CommonTaskBeans commonTaskBeans;

    public @NotNull ModelBeans getModelBeans() {
        return beans;
    }
}
