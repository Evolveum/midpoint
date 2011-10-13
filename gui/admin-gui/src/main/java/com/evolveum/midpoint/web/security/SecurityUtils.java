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

package com.evolveum.midpoint.web.security;

import java.io.Serializable;
import java.util.Collection;

import org.springframework.context.annotation.Scope;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author lazyman
 */
@Controller("security")
@Scope("session")
public class SecurityUtils implements Serializable {

	private static final long serialVersionUID = 4319833095810269507L;

	public boolean getIsUserLoggedIn() {
		return isUserInRole("ROLE_USER") || isUserInRole("ROLE_ADMIN");
	}

	public boolean getIsAdminLoggedIn() {
		return isUserInRole("ROLE_ADMIN");
	}

	private boolean isUserInRole(final String role) {
		final Collection<GrantedAuthority> grantedAuthorities = (Collection<GrantedAuthority>) SecurityContextHolder
				.getContext().getAuthentication().getAuthorities();
		for (GrantedAuthority grantedAuthority : grantedAuthorities) {
			if (role.equals(grantedAuthority.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	public String getShortUserName() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		if (principal == null) {
			
		}

		if (principal instanceof PrincipalUser) {
			PrincipalUser user = (PrincipalUser) principal;
			if (user.getFullName() != null) {
				return user.getFullName();
			}

			return user.getName();
		}

		return principal.toString();
	}

	public String getUserOid() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		if (principal == null) {
			return "Not Logged in";
		}
		if (principal instanceof PrincipalUser) {
			PrincipalUser user = (PrincipalUser) principal;
			return user.getOid();
		}
		return principal.toString();
	}
}
