/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers;

import static com.evolveum.midpoint.repo.common.tasks.handlers.composite.MockComponentActivityExecution.NS_EXT;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractMockActivityHandler<WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>>
        implements ActivityHandler<WD, AH> {

    private static final QName WORK_STATE_TYPE_NAME = new QName(NS_EXT, "CommonMockWorkStateType");

    @Override
    public @NotNull QName getWorkStateTypeName() {
        return WORK_STATE_TYPE_NAME;
    }
}
