package com.evolveum.midpoint.test.util;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.tools.testng.MidpointTestMixin;
import com.evolveum.midpoint.util.DebugDumpable;

/**
 * Mixin interface adding capabilities from infra test-utils like {@link OperationResult} creation
 * (with context in its name), display method for dumpable, etc.
 * It is based on {@link MidpointTestMixin} and provides all its operations too.
 */
public interface InfraTestMixin extends MidpointTestMixin {

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

    /**
     * Displays {@link DebugDumpable} value prefixed with provided title.
     */
    // TODO: after cleanup rename to displayValue or displayDumpable
    default void displayDumpable(String title, DebugDumpable dumpable) {
        displayValue(title, dumpable == null ? "null" : dumpable.debugDump(1));
    }

    // TODO add displayValue(title, Object) based on PrettyPrinter, finish PrettyPrinter ideas

    /**
     * AssertJ-based asserter for {@link OperationResult}.
     * Note: Name `assertThat` collides with static import of `Assertions.assertThat` from AssertJ.
     */
    default OperationResultAssert assertThatOperationResult(OperationResult operationResult) {
        return new OperationResultAssert(operationResult);
    }
}
