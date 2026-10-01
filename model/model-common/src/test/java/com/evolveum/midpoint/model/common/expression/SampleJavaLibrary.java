/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression;

import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.LogExpressionFunctions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;

public class SampleJavaLibrary {

    public static String hello(String foo, String bar) {
        return foo + bar;
    }

    public static String checkEnv(
            String foo,
            LogExpressionFunctions log,
            BasicExpressionFunctions basic,
            Task task,
            OperationResult result,
            ValueTransformationContext vtCtx) {
        MiscUtil.stateNonNull(log, "log is missing");
        MiscUtil.stateNonNull(basic, "basic is missing");
        MiscUtil.stateNonNull(task, "task is missing");
        MiscUtil.stateNonNull(result, "result is missing");
        MiscUtil.stateNonNull(vtCtx, "vtCtx is missing");
        return "Hello, " + foo;
    }

    public static String nonExistingVariable(String wrong) {
        return "Hello, " + wrong;
    }
}
