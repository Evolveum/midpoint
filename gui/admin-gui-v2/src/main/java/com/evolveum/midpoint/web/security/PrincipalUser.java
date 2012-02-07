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

import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.commons.lang.Validate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author lazyman
 */
public class PrincipalUser implements UserDetails {

    private UserType user;

    public PrincipalUser(UserType user) {
        Validate.notNull(user, "User must not be null.");
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new HashSet<GrantedAuthority>();
        //todo implement

        return collection;
    }

    @Override
    public String getPassword() {
        //todo implement
        return null;
    }

    @Override
    public String getUsername() {
        return user.getName();
    }

    @Override
    public boolean isAccountNonExpired() {
        //todo implement
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        //todo implement
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        //todo implement
        return true;
    }

    @Override
    public boolean isEnabled() {
        //todo implement
        return true;
    }
}
