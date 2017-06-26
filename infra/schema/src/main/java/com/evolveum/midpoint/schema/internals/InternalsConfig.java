/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema.internals;

/**
 * @author semancik
 *
 */
public class InternalsConfig {
	
	/**
	 * Checks for consistency of data structures (e.g. prism objects, containers, contexts).
	 */
	public static boolean consistencyChecks = true;
	
	/**
	 * Additional checks that method arguments make sense (e.g. deltas are not duplicated)
	 */
	private static boolean sanityChecks = true;
	
	public static boolean encryptionChecks = true;
	
	// We don't want this to be on by default. It will ruin the ability to explicitly import
	// non-encrypted value to repo.
	public static boolean readEncryptionChecks = false;
	
	// Used by testing code. If set to true then any change to a logging configuration from
	// inside midpoint (e.g. change of SystemConfiguration object) will be ignored.
	// DO NOT USE IN PRODUCTION CODE
	private static boolean avoidLoggingChange = false;
	
	private static boolean prismMonitoring = false;
	
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

	public static boolean isPrismMonitoring() {
		return prismMonitoring;
	}

	public static void setPrismMonitoring(boolean prismMonitoring) {
		InternalsConfig.prismMonitoring = prismMonitoring;
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
	
	public static boolean isAllowClearDataLogging() {
		return allowClearDataLogging;
	}
	
	public static void setAllowClearDataLogging(boolean allowClearDataLogging) {
		InternalsConfig.allowClearDataLogging = allowClearDataLogging;
	}
	
	public static void resetTestingPaths() {
		testingPaths = null;
	}
	
	public static void reset() {
		consistencyChecks = true;
		sanityChecks = false;
		encryptionChecks = true;
		readEncryptionChecks = false;
		avoidLoggingChange = false;
		allowClearDataLogging = false;
		testingPaths = null;
	}

	public static void setDevelopmentMode() {
		consistencyChecks = true;
		sanityChecks = true;
		encryptionChecks = true;
		prismMonitoring = true;
		allowClearDataLogging = true;
	}
	
	public static void turnOffAllChecks() {
		consistencyChecks = false;
		sanityChecks = false;
		encryptionChecks = false;
		readEncryptionChecks = false;
		prismMonitoring = false;
		allowClearDataLogging = false;
	}

	public static void turnOnAllChecks() {
		consistencyChecks = true;
		sanityChecks = true;
		encryptionChecks = true;
		encryptionChecks = true;
		prismMonitoring = true;
		allowClearDataLogging = true;
	}
}
