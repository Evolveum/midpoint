/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script.cel;

import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;

import java.lang.reflect.Method;

@FunctionalInterface
interface FunctionLibraryMethodProcessor {

    void process(Object library, Method method, String funcName, String funcId);
}
