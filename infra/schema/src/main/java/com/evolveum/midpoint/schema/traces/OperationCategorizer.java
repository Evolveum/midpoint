/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingOutputType;

import org.jetbrains.annotations.NotNull;

/**
 * Categorizes operations in tracing output.
 */
@Experimental
class OperationCategorizer {

    @NotNull private final TracingOutputType tracingOutput;

    OperationCategorizer(@NotNull TracingOutputType tracingOutput) {
        this.tracingOutput = tracingOutput;
    }

    void categorize() {
        if (tracingOutput.getResult() != null) {
            categorize(tracingOutput.getResult());
        }
    }

    private void categorize(OperationResultType result) {
        if (result.getOperationKind() == null) {
            result.setOperationKind(determineOperationKind(result));
        }
        result.getPartialResults().forEach(this::categorize);
    }

    private OperationKindType determineOperationKind(OperationResultType result) {
        OpType type = OpType.determine(result);
        return type != null ? type.getKind() : null;
    }
}
