/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.action;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * An action that is executed during synchronization - like add focus, link, unlink, and so on.
 *
 * @author Vilo Repan
 */
public interface SynchronizationAction {

    void handle(@NotNull OperationResult parentResult) throws CommonException;
}
