package com.evolveum.midpoint.prism;

/**
 * Determines the scope of consistency checks.
 * (Originally this was a boolean, but there are many methods with N boolean arguments, so it was
 * too easy to mix them up.)
 *
 * @author mederly
 */
public enum ConsistencyCheckScope {
    THOROUGH, MANDATORY_CHECKS_ONLY;

    public boolean isThorough() {
        return this == THOROUGH;
    }
}
