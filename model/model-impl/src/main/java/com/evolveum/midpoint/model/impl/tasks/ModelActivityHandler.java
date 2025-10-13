/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
