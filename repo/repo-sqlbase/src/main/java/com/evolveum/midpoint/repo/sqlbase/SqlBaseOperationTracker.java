package com.evolveum.midpoint.repo.sqlbase;

import com.evolveum.midpoint.schema.result.OperationResult;

public abstract class SqlBaseOperationTracker implements AutoCloseable {
    private static final ThreadLocal<SqlBaseOperationTracker> CURRENT_TRACKER = new ThreadLocal<>();

    private static final String DOT_NAME = SqlBaseOperationTracker.class.getName() + ".";

    private static final String FETCH_ONE_PRIMARY = "primary.fetch.one";

    private static final String FETCH_MULTIPLE_PRIMARY = "primary.fetch.multiple";

    private static final String FETCH_CHILDREN = "children.fetch.";
    private static final String PARSE_CHILDREN = "children.parse.";

    private static final String PARSE_PRIMARY = "primary.parse";

    private static final String RESOLVE_NAMES = "primary.resolveNames";

    private static final String PARSE_JSON_TO_XNODE = "parse.json2xnode.";

    private static final String PARSE_XNODE_TO_PRISM = "parse.xnode2prism.";






    private static final Tracker NOOP_TRACKER = () -> {};
    private static final SqlBaseOperationTracker NOOP = new SqlBaseOperationTracker() {
        @Override
        protected Tracker createSubresult(String name) {
            return NOOP_TRACKER;
        }

        @Override
        public void close()  {

        }
    };

    private static Factory impl = (or) -> NOOP;

    public static SqlBaseOperationTracker get() {
        var maybe = CURRENT_TRACKER.get();
        if (maybe != null) {
            return maybe;
        }
        // Nullables (for operations we are not tracking)
        return NOOP;
    }



    public static Factory setFactory(Factory factory) {
        impl = factory;
        return factory;
    }

    public static void free() {
        CURRENT_TRACKER.remove();
    }

    public static Tracker createTracker(String name) {
        return get().createSubresult(name);
    }


    protected abstract Tracker createSubresult(String name);

    public static Tracker fetchMultiplePrimaries() {
        return createTracker(FETCH_MULTIPLE_PRIMARY);
    }

    public static Tracker fetchPrimary() {
        return createTracker(FETCH_ONE_PRIMARY);
    }

    public static Tracker parsePrimary() {
        return createTracker(PARSE_PRIMARY);
    }

    public static Tracker fetchChildren(String name) {
        return createTracker(FETCH_CHILDREN + name);
    }

    public static Tracker parseChildren(String name) {
        return createTracker(PARSE_CHILDREN + name);
    }

    public static Tracker parseJson2XNode(String name) {
        return createTracker(PARSE_JSON_TO_XNODE + name);
    }

    public static Tracker parseXnode2Prism(String name) {
        return createTracker(PARSE_XNODE_TO_PRISM + name);
    }

    public static Tracker resolveNames() {
        return createTracker(RESOLVE_NAMES);
    }

    @Override
    public void close()  {
        if (CURRENT_TRACKER.get() == this) {
            CURRENT_TRACKER.remove();
        }
    }

    public static SqlBaseOperationTracker with(OperationResult result) {
        if (impl != null) {
            var tracker = impl.create(result);
            CURRENT_TRACKER.set(tracker);
            return tracker;
        }
        return NOOP;
    }



    public interface Tracker extends AutoCloseable {
        @Override
        public void close();
    }



    static class OpResultBased implements Factory {

        @Override
        public SqlBaseOperationTracker create(OperationResult result) {
            var tracker = new SqlBaseOperationTracker() {
                @Override
                protected Tracker createSubresult(String name) {
                    return result.createSubresult(DOT_NAME + name)::close;
                }

            };
            CURRENT_TRACKER.set(tracker);
            return tracker;
        }
    }

    public interface Factory {
        SqlBaseOperationTracker create(OperationResult result);

    }

}
