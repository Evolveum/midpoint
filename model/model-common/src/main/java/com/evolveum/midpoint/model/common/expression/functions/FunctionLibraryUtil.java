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

/**
 * @author semancik
 *
 */
public class FunctionLibraryUtil {

    public static FunctionLibrary createBasicFunctionLibrary(PrismContext prismContext, Protector protector, Clock clock) {
        FunctionLibrary lib = new FunctionLibrary();
        lib.setVariableName(MidPointConstants.FUNCTION_LIBRARY_BASIC_VARIABLE_NAME);
        lib.setNamespace(MidPointConstants.NS_FUNC_BASIC);
        BasicExpressionFunctions func = new BasicExpressionFunctions(prismContext, protector, clock);
        lib.setGenericFunctions(func);
        return lib;
    }

    public static FunctionLibrary createLogFunctionLibrary(PrismContext prismContext) {
        FunctionLibrary lib = new FunctionLibrary();
        lib.setVariableName(MidPointConstants.FUNCTION_LIBRARY_LOG_VARIABLE_NAME);
        lib.setNamespace(MidPointConstants.NS_FUNC_LOG);
        LogExpressionFunctions func = new LogExpressionFunctions(prismContext);
        lib.setGenericFunctions(func);
        return lib;
    }

}
