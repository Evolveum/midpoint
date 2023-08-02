/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.functions;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.constants.MidPointConstants;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * TODO should we move this to test code?
 *
 * @author semancik
 */
public class FunctionLibraryUtil {

    /** Note: In production, the this function library is created by Spring as `basicFunctionLibrary` bean. */
    @VisibleForTesting
    public static FunctionLibraryBinding createBasicFunctionLibraryBinding(
            PrismContext prismContext, Protector protector, Clock clock) {
        return new FunctionLibraryBinding(
                MidPointConstants.FUNCTION_LIBRARY_BASIC_VARIABLE_NAME,
                new BasicExpressionFunctions(prismContext, protector, clock));
    }

    /** Note: In production, the this function library is created by Spring as `logFunctionLibrary` bean. */
    @VisibleForTesting
    public static FunctionLibraryBinding createLogFunctionLibraryBinding(PrismContext prismContext) {
        return new FunctionLibraryBinding(
                MidPointConstants.FUNCTION_LIBRARY_LOG_VARIABLE_NAME,
                new LogExpressionFunctions(prismContext));
    }
}
