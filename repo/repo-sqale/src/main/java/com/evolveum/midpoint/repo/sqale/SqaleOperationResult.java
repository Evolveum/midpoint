package com.evolveum.midpoint.repo.sqale;

import com.evolveum.midpoint.schema.result.OperationResult;

public class SqaleOperationResult implements AutoCloseable {
    private static ThreadLocal<OperationResult> CURRENT_OPERATION_RESULT = new ThreadLocal<>();

    public static OperationResult get() {
        var maybe = CURRENT_OPERATION_RESULT.get();
        if (maybe != null) {
            return maybe;
        }
        // Nullables (for operations we are not tracking)
        return new OperationResult("temporary");
    }

    public static OperationResult set(OperationResult result) {
        CURRENT_OPERATION_RESULT.set(result);
        return result;
    }

    public static void free() {
        CURRENT_OPERATION_RESULT.remove();
    }

    public static OperationResult createSubresult(String name) {
        return get().createSubresult(name);
    }

    public static SqaleOperationResult with(OperationResult result) {
        set(result);
        return new SqaleOperationResult();
    }

    @Override
    public void close() {
        free();
    }
}
