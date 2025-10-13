/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.api;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * TODO
 */
public interface CaseEngineOperation extends DebugDumpable {

    @NotNull CaseType getCurrentCase();

    @NotNull Task getTask();
    int getCurrentStageNumber();
    @NotNull MidPointPrincipal getPrincipal();

    void closeCaseInRepository(OperationResult result) throws ObjectNotFoundException;
}
