package com.evolveum.midpoint.prism;

/**
 * Determines the scope of consistency checks.
 *
 * (Originally this was a boolean, but there are many 'checkConsistence'-style methods with a set of boolean arguments,
 * so it was too easy to mix them up with this new one.)
 *
 * @author mederly
 */
public enum ConsistencyCheckScope {
    /**
     * Full-scale checks.
     */
    THOROUGH,
    /**
     * Mandatory checks, e.g. checks for duplicate container IDs (see MID-1951).
     * Should be rather quick.
     *
     * TODO Current solution is not that optimal. We should distinguish between checks that deal with midPoint internal workings
     * (throwing IllegalArgumentException/IllegalStateException on failure), which can be turned off, as it is today. Another set
     * of checks should be applied only on users' inputs (when importing data, when accepting inputs via SOAP/REST/whathever interfaces, ...)
     * - and these should throw perhaps SchemaException that can be caught and handled appropriately.
     *
     * However, for the time being we consider this approach (i.e. that both kinds of checks are implemented the same way) to be an acceptable one.
     */
    MANDATORY_CHECKS_ONLY;

    public boolean isThorough() {
        return this == THOROUGH;
    }

    public static ConsistencyCheckScope fromBoolean(boolean consistencyChecksSwitchValue) {
        return consistencyChecksSwitchValue ? THOROUGH : MANDATORY_CHECKS_ONLY;
    }
}
