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

package com.evolveum.midpoint.model.security.api;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * Temporary place, till we create special component for it
 *
 * @author lazyman
 * @author Igor Farinic
 */
public class PrincipalUser implements Serializable {//   } UserDetails {

    private static final long serialVersionUID = 8299738301872077768L;
    private UserType user;

    public PrincipalUser(UserType user) {
        Validate.notNull(user, "User must not be null.");
        this.user = user;
    }

//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        Collection<GrantedAuthority> collection = new HashSet<GrantedAuthority>();
//        //todo implement
//
//        return collection;
//    }
//
//    @Override
//    public String getPassword() {
//        return null;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public String getUsername() {
//        return user.getName();
//    }
//
//    @Override
//    public boolean isAccountNonExpired() {
//        return true;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public boolean isAccountNonLocked() {
//        return true;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public boolean isCredentialsNonExpired() {
//        return true;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
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
