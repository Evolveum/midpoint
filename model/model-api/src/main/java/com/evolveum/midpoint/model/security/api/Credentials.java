/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.security.api;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.evolveum.midpoint.xml.ns._public.common.common_2.ProtectedStringType;

/**
 * Temporary place, till we create special component for it
 * 
 * @author lazyman
 * @author Igor Farinic
 */
public class Credentials implements Serializable {

	private static final long serialVersionUID = 8374377136310413930L;
	public static final String MESSAGE_DIGEST_TYPE = "SHA-256";
	private ProtectedStringType password;
	private int failedLogins = 0;
	private long lastFailedLoginAttempt;

	public long getLastFailedLoginAttempt() {
		return lastFailedLoginAttempt;
	}

	public void setLastFailedLoginAttempt(long lastFailedLoginAttempt) {
		this.lastFailedLoginAttempt = lastFailedLoginAttempt;
	}

	public int getFailedLogins() {
		return failedLogins;
	}

	public void setFailedLogins(int failedLogins) {
		this.failedLogins = failedLogins;
	}

	public ProtectedStringType getPassword() {
		return password;
	}
	
	public void setPassword(ProtectedStringType password) {
		this.password = password;
	}
	
	public void addFailedLogin() {
		failedLogins++;
	}

	public void clearFailedLogin() {
		failedLogins = 0;
	}

	public static String hashWithSHA2(String text) {
		if (text == null) {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance(MESSAGE_DIGEST_TYPE);
			byte[] bytes = md.digest(text.getBytes("utf-8"));

			builder = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				builder.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
		}

		return builder.toString();
	}
}
