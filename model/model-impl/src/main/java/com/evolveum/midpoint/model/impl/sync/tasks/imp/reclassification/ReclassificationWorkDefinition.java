/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
