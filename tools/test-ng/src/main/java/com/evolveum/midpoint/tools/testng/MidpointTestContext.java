package com.evolveum.midpoint.tools.testng;

/**
 * Basic contract for test-method context (typically available through thread-local variable).
 */
public interface MidpointTestContext {

    /**
     * Returns the actual instantiated test class.
     */
    Class<?> getTestClass();

    /**
     * Returns the name of the test method.
     */
    String getTestMethodName();

    /**
     * Returns test name in form of "class-simple-name.method".
     */
    default String getTestName() {
        return getTestClass().getSimpleName() + "." + getTestMethodName();
    }

    /**
     * Returns short test name - currently the same like {@link #getTestMethodName()}.
     */
    default String getTestNameShort() {
        return getTestMethodName();
    }

    /**
     * Returns long test name in form of "fully.qualified.class-name.method".
     */
    default String getTestNameLong() {
        return getTestClass().getName() + "." + getTestMethodName();
    }
}
