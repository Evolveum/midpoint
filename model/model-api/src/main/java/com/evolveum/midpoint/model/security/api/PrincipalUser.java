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

import org.apache.commons.lang.StringUtils;

/**
 * Temporary place, till we create special component for it
 *
 * @author lazyman
 * @author Igor Farinic
 */
public class PrincipalUser implements Serializable {

	private static final long serialVersionUID = 8299738301872077768L;
	private String oid;
	private String name;
	private String givenName;
	private String familyName;
	private String fullName;
	private Credentials credentials;
	private boolean enabled;

	public PrincipalUser(String oid, String name, boolean enabled) {
		if (StringUtils.isEmpty(oid)) {
			throw new IllegalArgumentException("User oid can't be null, or empty.");
		}
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("User name can't be null.");
		}
		this.oid = oid;
		this.name = name;
		this.enabled = enabled;
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public Credentials getCredentials() {
		if (credentials == null) {
			credentials = new Credentials();
		}

		return credentials;
	}

	void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public boolean isEnabled() {
		return enabled;
	}

	void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getOid() {
		return oid;
	}

	public String getName() {
		return name;
	}
}
