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

package com.evolveum.midpoint.repo.sql.testing;

import org.testng.AssertJUnit;

/**
 * @author semancik
 *
 */
public class SqlRepoTestUtil {

	public static void assertVersionProgress(String prevVersion, String nextVersion) {
		String error = checkVersionProgress(prevVersion, nextVersion);
		if (error != null) {
			AssertJUnit.fail(error);
		}
	}

	public static String checkVersionProgress(String prevVersion, String nextVersion) {
		String error = checkVersionProgressInternal(prevVersion, nextVersion);
		if (error == null) {
			return null;
		}
		return "Invalid version progress from '"+prevVersion+"' to '"+nextVersion+"': "+error;
	}

	private static String checkVersionProgressInternal(String prevVersion, String nextVersion) {
		if (nextVersion == null) {
			return "null next version";
		}
		if (prevVersion == null) {
			// anythig is OK
			return null;
		}
		if (prevVersion.equals(nextVersion)) {
			return "version are same";
		}
		int prevInt;
		try {
			prevInt = Integer.parseInt(prevVersion);
		} catch (NumberFormatException e) {
			return "previous version is not numeric";
		}
		int nextInt;
		try {
			nextInt = Integer.parseInt(nextVersion);
		} catch (NumberFormatException e) {
			return "next version is not numeric";
		}
		if (nextInt <= prevInt) {
			return "wrong numeric order";
		}
		return null;
	}

}
