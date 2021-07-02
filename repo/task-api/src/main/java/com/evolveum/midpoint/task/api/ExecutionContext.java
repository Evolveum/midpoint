/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

import org.jetbrains.annotations.NotNull;

/**
 * General "processing" in context of which the task (currently) carries out its work.
 */
public interface ExecutionContext {

    @NotNull ExecutionModeType getExecutionMode();
}
