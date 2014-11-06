/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.common;

/**
 * @author semancik
 *
 */
public class InternalsConfig {
	
	public static boolean consistencyChecks = false;
	
	public static boolean encryptionChecks = false;
	
	// We don't want this to be on by default. It will ruin the ability to explicitly import
	// non-encrypted value to repo.
	public static boolean readEncryptionChecks = false;
	
	// Used by testing code. If set to true then any change to a logging configuration from
	// inside midpoint (e.g. change of SystemConfiguration object) will be ignored.
	// DO NOT USE IN PRODUCTION CODE
	public static boolean avoidLoggingChange = false;

	public static void setDevelopmentMode() {
		consistencyChecks = true;
		encryptionChecks = true;
	}
	
	public static void turnOffAllChecks() {
		consistencyChecks = false;
		encryptionChecks = false;
		readEncryptionChecks = false;
	}

	public static void turnOnAllChecks() {
		consistencyChecks = true;
		encryptionChecks = true;
		encryptionChecks = true;
	}
}
