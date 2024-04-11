/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType.REPLACE;

import com.evolveum.midpoint.model.impl.sync.tasks.ResourceSetTaskWorkDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;

public class ImportWorkDefinition extends ResourceSetTaskWorkDefinition implements ResourceObjectSetSpecificationProvider {

    protected ImportWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
        ResourceObjectSetUtil.setDefaultQueryApplicationMode(getResourceObjectSetSpecification(), REPLACE);
    }
}
