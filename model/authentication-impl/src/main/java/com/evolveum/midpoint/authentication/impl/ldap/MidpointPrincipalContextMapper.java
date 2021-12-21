/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.ldap;

import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipalManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.util.Collection;

/**
 * @author skublik
 */

public class MidpointPrincipalContextMapper implements UserDetailsContextMapper {

    GuiProfiledPrincipalManager principalManager;

    public MidpointPrincipalContextMapper(GuiProfiledPrincipalManager principalManager) {
        this.principalManager = principalManager;
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
            Collection<? extends GrantedAuthority> authorities) {

        String userNameEffective = username;
        Class<? extends FocusType> focusType = UserType.class;
        try {
            if (ctx instanceof LdapDirContextAdapter && ((LdapDirContextAdapter) ctx).getNamingAttr() != null) {
                userNameEffective = resolveLdapName(ctx, username, ((LdapDirContextAdapter) ctx).getNamingAttr());
                focusType = ((LdapDirContextAdapter) ctx).getFocusType();
            }
            return principalManager.getPrincipal(userNameEffective, focusType);

        } catch (ObjectNotFoundException e) {
            throw new UsernameNotFoundException("UserProfileServiceImpl.unknownUser", e);
        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | NamingException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    private String resolveLdapName(DirContextOperations ctx, String username, String ldapNamingAttr) throws NamingException, ObjectNotFoundException {
        Attribute ldapResponse = ctx.getAttributes().get(ldapNamingAttr);
        if (ldapResponse != null) {
            if (ldapResponse.size() == 1) {
                Object namingAttrValue = ldapResponse.get(0);

                if (namingAttrValue != null) {
                    return namingAttrValue.toString().toLowerCase();
                }
            } else {
                throw new ObjectNotFoundException("Bad response"); // naming attribute contains multiple values
            }
        }
        return username; // fallback to typed-in username in case ldap value is missing
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException();
    }
}
