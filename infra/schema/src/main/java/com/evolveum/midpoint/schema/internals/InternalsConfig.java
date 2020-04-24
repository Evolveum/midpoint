/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.internals;

import org.apache.commons.configuration2.Configuration;

/**
 * @author semancik
 *
 */
public class InternalsConfig {

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

    public static boolean isPrismMonitoring() {
        return prismMonitoring;
    }

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

    public static void set(Configuration internalsConfig) {
        if (internalsConfig.containsKey("developmentMode")) {
            boolean developmentMode = internalsConfig.getBoolean("developmentMode");
            if (developmentMode) {
                setDevelopmentMode();
            } else {
                reset();
            }
        }

        consistencyChecks = internalsConfig.getBoolean("consistencyChecks", consistencyChecks);
        sanityChecks = internalsConfig.getBoolean("sanityChecks", sanityChecks);
        encryptionChecks = internalsConfig.getBoolean("encryptionChecks", encryptionChecks);
        readEncryptionChecks = internalsConfig.getBoolean("readEncryptionChecks", readEncryptionChecks);
        avoidLoggingChange = internalsConfig.getBoolean("avoidLoggingChange", avoidLoggingChange);
        allowClearDataLogging = internalsConfig.getBoolean("allowClearDataLogging", allowClearDataLogging);
        prismMonitoring = internalsConfig.getBoolean("prismMonitoring", prismMonitoring);
        modelProfiling = internalsConfig.getBoolean("modelProfiling", modelProfiling);
        // TODO: testingPaths
        detailedAuthorizationLog = internalsConfig.getBoolean("detailedAuhotizationLog", detailedAuthorizationLog);

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
        encryptionChecks = true;
        prismMonitoring = true;
        modelProfiling = true;
        allowClearDataLogging = true;
    }

    public static boolean nonCriticalExceptionsAreFatal() {
        // TODO: return true in development mode: MID-3529
        return false;
    }
}
