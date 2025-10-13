/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
