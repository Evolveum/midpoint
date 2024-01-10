package com.evolveum.midpoint.repo.sqlbase;

import com.evolveum.midpoint.schema.result.OperationResult;

public class SqlBaseOperationTracker implements AutoCloseable {
    private static final ThreadLocal<OperationResult> CURRENT_OPERATION_RESULT = new ThreadLocal<>();

    private static final String DOT_NAME = SqlBaseOperationTracker.class.getName() + ".";

    private static final String FETCH_ONE_PRIMARY = DOT_NAME + "primary.fetch.one";

    private static final String FETCH_MULTIPLE_PRIMARY = DOT_NAME + "primary.fetch.multiple";

    private static final String FETCH_CHILDREN = DOT_NAME + "children.fetch.";
    private static final String PARSE_CHILDREN = DOT_NAME + "children.parse.";

    private static final String PARSE_PRIMARY = DOT_NAME + "primary.parse";

    private static final String RESOLVE_NAMES = DOT_NAME + "primary.resolveNames";

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

    public static OperationResult fetchMultiplePrimaries() {
        return createSubresult(FETCH_MULTIPLE_PRIMARY);
    }

    public static OperationResult fetchPrimary() {
        return createSubresult(FETCH_ONE_PRIMARY);
    }

    public static OperationResult parsePrimary() {
        return createSubresult(PARSE_PRIMARY);
    }

    public static OperationResult fetchChildren(String name) {
        return createSubresult(FETCH_CHILDREN + name);
    }

    public static OperationResult parseChildren(String name) {
        return createSubresult(PARSE_CHILDREN + name);
    }

    public static OperationResult resolveNames() {
        return createSubresult(RESOLVE_NAMES);
    }

    public static SqlBaseOperationTracker with(OperationResult result) {
        set(result);
        return new SqlBaseOperationTracker();
    }

    @Override
    public void close() {
        free();
    }
}
