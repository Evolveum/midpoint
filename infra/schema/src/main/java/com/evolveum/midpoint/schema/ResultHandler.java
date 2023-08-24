/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Classes implementing this interface are used to handle iterative results.
 *
 * It is only used to handle iterative search results now. It may be reused for
 * other purposes as well.
 *
 * Also see {@link ObjectHandler} which is not limited to ObjectType and handles
 * real values instead of prism values.
 *
 * TODO this should perhaps be named ObjectResultHandler
 *
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface ResultHandler<T extends ObjectType> extends ObjectHandler<PrismObject<T>> {

    /**
     * Handle a single result.
     *
     * @param object Resource object to process.
     * @return true if the operation should proceed, false if it should stop
     */
    boolean handle(PrismObject<T> object, OperationResult parentResult);
}
