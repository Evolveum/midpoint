/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
