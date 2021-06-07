/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;

public abstract class AbstractMockActivityHandler<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
        implements ActivityHandler<WD, AH> {

    //    private static final QName WORK_STATE_TYPE_NAME = new QName(NS_EXT, "CommonMockWorkStateType");
    private static final QName WORK_STATE_TYPE_NAME = AbstractActivityWorkStateType.COMPLEX_TYPE;

    @Override
    public @NotNull QName getWorkStateTypeName() {
        return WORK_STATE_TYPE_NAME;
    }

    @Override
    public boolean shouldCreateWorkStateOnInitialization() {
        return true;
    }
}
