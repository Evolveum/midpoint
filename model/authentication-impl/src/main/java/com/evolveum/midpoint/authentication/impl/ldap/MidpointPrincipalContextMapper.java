/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.ldap;

import java.util.Collection;
import java.util.List;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.evaluator.context.PasswordAuthenticationContext;
import com.evolveum.midpoint.authentication.api.evaluator.context.PreAuthenticationContext;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author skublik
 */

public class MidpointPrincipalContextMapper implements UserDetailsContextMapper {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointPrincipalContextMapper.class);

    @Autowired
    @Qualifier("preAuthenticatedEvaluator")
    private AuthenticationEvaluator<PreAuthenticationContext, PreAuthenticatedAuthenticationToken> preAuthenticatedEvaluator;

    public MidpointPrincipalContextMapper() {
    }

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
            Collection<? extends GrantedAuthority> authorities) {

        if (!(ctx instanceof LdapDirContextAdapter) || ((LdapDirContextAdapter) ctx).getNamingAttr() == null) {
            LOGGER.debug("Couldn't define midpoint user");
            throw new AuthenticationServiceException("web.security.provider.invalid");
        }

        String userNameEffective;
        try {
            userNameEffective = resolveLdapName(ctx, username, ((LdapDirContextAdapter) ctx).getNamingAttr());
        } catch (ObjectNotFoundException e) {
            throw new UsernameNotFoundException("web.security.provider.invalid.credentials", e);
        } catch (NamingException e) {
            throw new SystemException(e.getMessage(), e);
        }

        Class<? extends FocusType> focusType = ((LdapDirContextAdapter) ctx).getFocusType();
        List<ObjectReferenceType> requireAssignment = ((LdapDirContextAdapter) ctx).getRequireAssignment();
        AuthenticationChannel channel = ((LdapDirContextAdapter) ctx).getChannel();
        ConnectionEnvironment connEnv = ((LdapDirContextAdapter) ctx).getConnectionEnvironment();

        PreAuthenticationContext authContext = new PreAuthenticationContext(userNameEffective, focusType, requireAssignment, channel);

        try {
            PreAuthenticatedAuthenticationToken token = preAuthenticatedEvaluator.authenticate(
                    connEnv, authContext);
            return (UserDetails) token.getPrincipal();
        } catch (DisabledException | AuthenticationServiceException | UsernameNotFoundException e) {
            throw new AuditedAuthenticationException(e);
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
            } else if (ldapResponse.size() == 0) {
                LOGGER.debug("LDAP attribute, which define username is empty");
                throw new AuthenticationServiceException("web.security.provider.invalid");
            } else {
                LOGGER.debug("LDAP attribute, which define username contains more values {}", ldapResponse.getAll());
                throw new AuthenticationServiceException("web.security.provider.invalid"); // naming attribute contains multiple values
            }
        }
        return username; // fallback to typed-in username in case ldap value is missing
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException();
    }
}
