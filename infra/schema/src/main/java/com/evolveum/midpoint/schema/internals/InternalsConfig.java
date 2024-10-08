/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.internals;

import org.apache.commons.configuration2.Configuration;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author semancik
 *
 */
public class InternalsConfig {

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
     * What caching should be turned on for resources that have no specific caching configuration?
     * This is influenced by `midpoint.internals.shadowCachingDefault` system property.
     */
    private static ShadowCachingDefault shadowCachingDefault;

    /**
     * This is the default setting for {@link #shadowCachingDefault} (if the respective system property is not present).
     * Currently, we need different values for tests and standard GUI mode.
     *
     * TODO resolve this somehow before 4.9 release
     */
    public static ShadowCachingDefault shadowCachingDefaultDefault = ShadowCachingDefault.NONE;

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

    public static boolean isShadowCachingOnByDefault() {
        return shadowCachingDefault != ShadowCachingDefault.NONE;
    }

    /**
     * If true, then the cache is turned on by default with long TTL, and for all attributes. To be used for tests.
     */
    public static boolean isShadowCachingFullByDefault() {
        return shadowCachingDefault == ShadowCachingDefault.FULL;
    }

    public static ShadowCachingDefault getShadowCachingDefault() {
        return shadowCachingDefault;
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
                        internalsConfig.getString(SHADOW_CACHING_DEFAULT, shadowCachingDefaultDefault.stringValue));
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

        /** The default is no caching, just like it was in 4.8 and earlier. */
        NONE("none"),

        /** The default is short-lived caching ... TODO specify. This is the default. */
        STANDARD("standard"),

        /** The default is caching of all data, with long TTL. To be used primarily in tests. */
        FULL("full"),

        /** As {@link #FULL} but using fresh data, not cached ones. To be used primarily in tests. */
        FULL_BUT_USING_FRESH("fullButUsingFresh");

        private final String stringValue;

        ShadowCachingDefault(String stringValue) {
            this.stringValue = stringValue;
        }

        public static @NotNull ShadowCachingDefault fromString(@Nullable String valueToFind) {
            if (valueToFind == null) {
                return STANDARD;
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
    }
}
