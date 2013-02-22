/**
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
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
