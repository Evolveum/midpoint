package com.evolveum.midpoint.test.util;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.tools.testng.MidpointTestMixin;

/**
 * Mixin interface adding methods for {@link OperationResult} creation with context in its name.
 * It is based on {@link MidpointTestMixin} and provides all its operations too.
 */
public interface OperationResultTestMixin extends MidpointTestMixin {

    /**
     * Creates new {@link OperationResult} with name equal to {@link #contextName()}.
     */
    default OperationResult createOperationResult() {
        return new OperationResult(contextName());
    }

    /**
     * Creates new {@link OperationResult} with name prefixed by {@link #contextName()}.
     */
    default OperationResult createOperationResult(String nameSuffix) {
        return new OperationResult(contextName() + "." + nameSuffix);
    }
}
