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
package com.evolveum.midpoint.common.security;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.Validate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author semancik
 *
 */
public class MidPointPrincipal implements UserDetails {
	
	private static final long serialVersionUID = 8299738301872077768L;
    private UserType user;
    private Collection<Authorization> authorizations = new ArrayList<Authorization>();

    public MidPointPrincipal(UserType user) {
        Validate.notNull(user, "User must not be null.");
        this.user = user;
    }

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getAuthorities()
	 */
	@Override
	public Collection<Authorization> getAuthorities() {
		return authorizations;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getPassword()
	 */
	@Override
	public String getPassword() {
		// We won't return password
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#getUsername()
	 */
	@Override
	public String getUsername() {
		return getUser().getName().getOrig();
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonExpired()
	 */
	@Override
	public boolean isAccountNonExpired() {
		// TODO
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonLocked()
	 */
	@Override
	public boolean isAccountNonLocked() {
		// TODO
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isCredentialsNonExpired()
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		// TODO
		return true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.core.userdetails.UserDetails#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		CredentialsType credentials = user.getCredentials();
        if (credentials == null || credentials.getPassword() == null){ 
        	return false;
        }
        
        if (user.getActivation() == null) {
            return false;
        }

        ActivationType activation = user.getActivation();
        long time = System.currentTimeMillis();
        if (activation.getValidFrom() != null) {
            long from = MiscUtil.asDate(activation.getValidFrom()).getTime();
            if (time < from) {
                return false;
            }
        }
        if (activation.getValidTo() != null) {
            long to = MiscUtil.asDate(activation.getValidTo()).getTime();
            if (to > time) {
                return false;
            }
        }
        if (activation.isEnabled() == null) {
            return false;
        }
        
        return activation.isEnabled();
	}

	public UserType getUser() {
        return user;
    }

    public PolyStringType getName() {
        return getUser().getName();
    }

    public String getFamilyName() {
        PolyStringType string = getUser().getFamilyName();
        return string != null ? string.getOrig() : null;
    }

    public String getFullName() {
        PolyStringType string = getUser().getFullName();
        return string != null ? string.getOrig() : null;
    }

    public String getGivenName() {
        PolyStringType string = getUser().getGivenName();
        return string != null ? string.getOrig() : null;
    }

    public String getOid() {
        return getUser().getOid();
    }
}
