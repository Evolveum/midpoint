/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp.reclassification;


import com.evolveum.midpoint.model.impl.sync.tasks.imp.ImportWorkDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;

public class ReclassificationWorkDefinition extends ImportWorkDefinition implements ResourceObjectSetSpecificationProvider {

    public ReclassificationWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
    }
}
