/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType.APPEND;

import com.evolveum.midpoint.model.impl.sync.tasks.ResourceSetTaskWorkDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReconciliationWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;

public class ReconciliationWorkDefinition extends ResourceSetTaskWorkDefinition implements ResourceObjectSetSpecificationProvider {

    ReconciliationWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);
        ResourceObjectSetUtil.setDefaultQueryApplicationMode(getResourceObjectSetSpecification(), APPEND);
    }
}
