/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.security.api.PrincipalUser;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.io.Serializable;
import java.util.Collection;

/**
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
            return "Unknown";
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

    public PrincipalUser getPrincipalUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof PrincipalUser)) {
            throw new IllegalStateException("Principal user is not type of " + PrincipalUser.class.getName());
        }

        return (PrincipalUser) principal;
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
