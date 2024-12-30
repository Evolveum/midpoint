/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.internals;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author semancik
 *
 */
public class InternalsConfig {

    private static final Trace LOGGER = TraceManager.getTrace(InternalsConfig.class);

    private static final String CONSISTENCY_CHECKS = "consistencyChecks";
    private static final String SANITY_CHECKS = "sanityChecks";
    private static final String ENCRYPTION_CHECKS = "encryptionChecks";
    private static final String READ_ENCRYPTION_CHECKS = "readEncryptionChecks";
    private static final String AVOID_LOGGING_CHANGE = "avoidLoggingChange";
    private static final String ALLOW_CLEAR_DATA_LOGGING = "allowClearDataLogging";
    private static final String PRISM_MONITORING = "prismMonitoring";
    private static final String MODEL_PROFILING = "modelProfiling";
    private static final String DETAILED_AUTHORIZATION_LOG = "detailedAuthorizationLog";
    private static final String SHADOW_CACHING_DEFAULT = "shadowCachingDefault";

    /**
     * Checks for consistency of data structures (e.g. prism objects, containers, contexts).
     */
    public static boolean consistencyChecks = false;

    /**
     * Additional checks that method arguments make sense (e.g. deltas are not duplicated)
     */
    private static boolean sanityChecks = false;

    public static boolean encryptionChecks = true;

    // We don't want this to be on by default. It will ruin the ability to explicitly import
    // non-encrypted value to repo.
    public static boolean readEncryptionChecks = false;

    // Used by testing code. If set to true then any change to a logging configuration from
    // inside midpoint (e.g. change of SystemConfiguration object) will be ignored.
    // DO NOT USE IN PRODUCTION CODE
    private static boolean avoidLoggingChange = false;

    private static boolean prismMonitoring = false;

    private static boolean modelProfiling = false;

    private static boolean allowClearDataLogging = false;

    /**
     * Non-null value enables alternative code paths used in testing. This adds
     * special pieces of code that alter normal behavior of the system. These
     * may reverse the normal evaluation ordering, randomize evaluations or
     * operation ordering and so on. It is used to make tests more thorough,
     * so they have a chance to catch more bugs with the same test code.
     *
     * These alternative testing paths may be quite inefficient.
     * This is NOT supposed to be used in production. This is only for
     * use in testing.
     */
    private static TestingPaths testingPaths = null;

    private static boolean detailedAuthorizationLog = false;

    /**
     * What should be the default shadow caching settings (to be combined with resource-specific settings)?
     *
     * Normally, these are taken from the system configuration. But we can override this, mainly for tests.
     * We use `midpoint.internals.shadowCachingDefault` system property for that.
     */
    @NotNull private static ShadowCachingDefault shadowCachingDefault = ShadowCachingDefault.FROM_SYSTEM_CONFIGURATION;

    /**
     * This is the default setting for {@link #shadowCachingDefault} (if the respective system property is not present).
     * Used for the tests to enforce caching even if nothing is set via system properties.
     */
    @Nullable public static String shadowCachingDefaultDefault;

    public static boolean isPrismMonitoring() {
        return prismMonitoring;
    }

    @VisibleForTesting
    public static void setPrismMonitoring(boolean prismMonitoring) {
        InternalsConfig.prismMonitoring = prismMonitoring;
    }

    public static boolean isModelProfiling() {
        return modelProfiling;
    }

    public static void setModelProfiling(boolean modelProfiling) {
        InternalsConfig.modelProfiling = modelProfiling;
    }

    public static boolean isConsistencyChecks() {
        return consistencyChecks;
    }

    public static void setConsistencyChecks(boolean consistencyChecks) {
        InternalsConfig.consistencyChecks = consistencyChecks;
    }

    public static boolean isSanityChecks() {
        return sanityChecks;
    }

    public static void setSanityChecks(boolean sanityChecks) {
        InternalsConfig.sanityChecks = sanityChecks;
    }

    public static boolean isEncryptionChecks() {
        return encryptionChecks;
    }

    public static void setEncryptionChecks(boolean encryptionChecks) {
        InternalsConfig.encryptionChecks = encryptionChecks;
    }

    public static boolean isReadEncryptionChecks() {
        return readEncryptionChecks;
    }

    public static void setReadEncryptionChecks(boolean readEncryptionChecks) {
        InternalsConfig.readEncryptionChecks = readEncryptionChecks;
    }

    public static boolean isAvoidLoggingChange() {
        return avoidLoggingChange;
    }

    public static void setAvoidLoggingChange(boolean avoidLoggingChange) {
        InternalsConfig.avoidLoggingChange = avoidLoggingChange;
    }

    public static TestingPaths getTestingPaths() {
        return testingPaths;
    }

    public static void setTestingPaths(TestingPaths testingPaths) {
        InternalsConfig.testingPaths = testingPaths;
    }

    public static boolean isDetailedAuthorizationLog() {
        return detailedAuthorizationLog;
    }

    public static void setDetailedAuthorizationLog(boolean detailedAuthorizationLog) {
        InternalsConfig.detailedAuthorizationLog = detailedAuthorizationLog;
    }

    public static boolean isAllowClearDataLogging() {
        return allowClearDataLogging;
    }

    public static void setAllowClearDataLogging(boolean allowClearDataLogging) {
        InternalsConfig.allowClearDataLogging = allowClearDataLogging;
    }

    public static void resetTestingPaths() {
        testingPaths = null;
    }

    @TestOnly
    public static boolean isShadowCachingOnByDefault() {
        return shadowCachingDefault == ShadowCachingDefault.FULL
                || shadowCachingDefault == ShadowCachingDefault.FULL_BUT_USING_FRESH;
    }

    /**
     * If true, then the cache is turned on by default with long TTL, and for all attributes. To be used for tests.
     */
    @TestOnly
    public static boolean isShadowCachingFullByDefault() {
        return shadowCachingDefault == ShadowCachingDefault.FULL;
    }

    public static @NotNull ShadowCachingDefault getShadowCachingDefault() {
        return shadowCachingDefault;
    }

    /** Use with care! The standard way is to provide the default for tests via the system property. */
    @TestOnly
    public static void setShadowCachingDefault(@NotNull ShadowCachingDefault shadowCachingDefault) {
        InternalsConfig.shadowCachingDefault = shadowCachingDefault;
    }

    public static void set(Configuration internalsConfig) {
        if (internalsConfig.containsKey("developmentMode")) {
            boolean developmentMode = internalsConfig.getBoolean("developmentMode");
            if (developmentMode) {
                setDevelopmentMode();
            } else {
                reset();
            }
        }

        consistencyChecks = internalsConfig.getBoolean(CONSISTENCY_CHECKS, consistencyChecks);
        sanityChecks = internalsConfig.getBoolean(SANITY_CHECKS, sanityChecks);
        encryptionChecks = internalsConfig.getBoolean(ENCRYPTION_CHECKS, encryptionChecks);
        readEncryptionChecks = internalsConfig.getBoolean(READ_ENCRYPTION_CHECKS, readEncryptionChecks);
        avoidLoggingChange = internalsConfig.getBoolean(AVOID_LOGGING_CHANGE, avoidLoggingChange);
        allowClearDataLogging = internalsConfig.getBoolean(ALLOW_CLEAR_DATA_LOGGING, allowClearDataLogging);
        prismMonitoring = internalsConfig.getBoolean(PRISM_MONITORING, prismMonitoring);
        modelProfiling = internalsConfig.getBoolean(MODEL_PROFILING, modelProfiling);
        // TODO: testingPaths
        detailedAuthorizationLog = internalsConfig.getBoolean(DETAILED_AUTHORIZATION_LOG, detailedAuthorizationLog);

        shadowCachingDefault =
                ShadowCachingDefault.fromString(
                        internalsConfig.getString(SHADOW_CACHING_DEFAULT, shadowCachingDefaultDefault));

        LOGGER.info("Default setting for shadow caching: {}", shadowCachingDefault);
    }

    public static void reset() {
        consistencyChecks = false;
        sanityChecks = false;
        encryptionChecks = true;
        readEncryptionChecks = false;
        avoidLoggingChange = false;
        allowClearDataLogging = false;
        prismMonitoring = false;
        modelProfiling = false;
        testingPaths = null;
        detailedAuthorizationLog = false;
        // intentionally not manipulating shadow caching (at least for now)
    }

    public static void setDevelopmentMode() {
        consistencyChecks = true;
        sanityChecks = true;
        encryptionChecks = true;
        prismMonitoring = true;
        modelProfiling = true;
        allowClearDataLogging = true;
    }

    public static void turnOffAllChecks() {
        consistencyChecks = false;
        sanityChecks = false;
        encryptionChecks = false;
        readEncryptionChecks = false;
        prismMonitoring = false;
        modelProfiling = false;
        allowClearDataLogging = false;
    }

    public static void turnOnAllChecks() {
        consistencyChecks = true;
        sanityChecks = true;
        encryptionChecks = true;
        readEncryptionChecks = true;
        prismMonitoring = true;
        modelProfiling = true;
        allowClearDataLogging = true;
    }

    public static boolean nonCriticalExceptionsAreFatal() {
        // TODO: return true in development mode: MID-3529
        return false;
    }

    public enum ShadowCachingDefault {

        /** Default shadow caching settings should be taken from the system configuration. */
        FROM_SYSTEM_CONFIGURATION("fromSystemConfiguration", false),

        /** The default is no caching, just like it was in 4.8 and earlier. */
        @TestOnly
        NONE("none", false),

        /** The default is caching of all data, with long TTL. */
        @TestOnly
        FULL("full", false),

        /**
         * Emulates the default out-of-the-box settings in 4.9, with the following differences:
         *
         * . TTL is long, to avoid breaking tests that manipulate the clock
         * (note that the standard TTL is 1 day for practical reasons);
         * . caches all attributes, not only the defined ones (but this may change in the future).
         *
         * Effectively, it is quite similar to {@link #FULL}, with a crucial difference in that it uses fresh data,
         * not cached ones.
         *
         * This is the default value for tests.
         */
        @TestOnly
        FULL_BUT_USING_FRESH("fullButUsingFresh", true);

        private final String stringValue;

        /**
         * Is this value the standard one for the tests? Exactly one of the values should have it set.
         * Checked by tests that should not be run under non-standard caching configs.
         */
        @TestOnly
        private final boolean standardForTests;

        ShadowCachingDefault(String stringValue, boolean standardForTests) {
            this.stringValue = stringValue;
            this.standardForTests = standardForTests;
        }

        public static @NotNull ShadowCachingDefault fromString(@Nullable String valueToFind) {
            if (valueToFind == null) {
                return FROM_SYSTEM_CONFIGURATION;
            }
            for (ShadowCachingDefault value : values()) {
                if (valueToFind.equals(value.stringValue)) {
                    return value;
                }
            }
            throw new IllegalStateException(
                    "Unknown shadow caching default value: '%s'. Valid ones are: %s".formatted(
                            valueToFind,
                            Arrays.stream(values())
                                    .map(v -> "'" + v + "'")
                                    .collect(Collectors.joining(", "))));
        }

        @Override
        public String toString() {
            return stringValue;
        }

        @TestOnly
        public boolean isStandardForTests() {
            return standardForTests;
        }
    }
}
