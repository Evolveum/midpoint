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

package com.evolveum.midpoint.model.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.model.security.api.UserDetailsService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author lazyman
 */
@Service(value = "userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Trace LOGGER = TraceManager.getTrace(UserDetailsServiceImpl.class);
    @Autowired(required = true)
    private transient RepositoryService repositoryService;
    @Autowired
    private PrismContext prismContext;

    @Override
    public PrincipalUser getUser(String principal) {
        PrincipalUser user = null;
        try {
            user = findByUsername(principal);
        } catch (Exception ex) {
            LOGGER.warn("Couldn't find user with name '{}', reason: {}.",
                    new Object[]{principal, ex.getMessage()});
        }

        return user;
    }

    @Override
    public void updateUser(PrincipalUser user) {
        try {
            save(user);
        } catch (RepositoryException ex) {
            LOGGER.warn("Couldn't save user '{}, ({})', reason: {}.",
                    new Object[]{user.getFullName(), user.getOid(), ex.getMessage()});
        }
    }

    private PrincipalUser findByUsername(String username) throws SchemaException, ObjectNotFoundException {
//        QueryType query = new QueryType();
//        query.setFilter(createQuery(username));
    	ObjectQuery query = ObjectQuery.createObjectQuery(EqualsFilter.createEqual(UserType.class, prismContext, UserType.F_NAME, username));
        LOGGER.trace("Looking for user, query:\n" + query.dump());

        List<PrismObject<UserType>> list = repositoryService.searchObjects(UserType.class, query, 
                new OperationResult("Find by username"));
        if (list == null) {
            return null;
        }
        LOGGER.trace("Users found: {}.", new Object[]{list.size()});
        if (list.size() == 0 || list.size() > 1) {
            return null;
        }

        return createUser(list.get(0).asObjectable());
    }

    private PrincipalUser createUser(UserType userType) {
    	return new PrincipalUser(userType);
//        CredentialsType credentialsType = userType.getCredentials();
//
//        PrincipalUser user = new PrincipalUser(userType);
//        if (credentialsType != null && credentialsType.getPassword() != null) {
//            PasswordType password = credentialsType.getPassword();
//
//            Credentials credentials = user.getCredentials();
//            credentials.setPassword(password.getProtectedString());
//            if (password.getFailedLogins() == null || password.getFailedLogins() < 0) {
//                credentials.setFailedLogins(0);
//            } else {
//                credentials.setFailedLogins(password.getFailedLogins());
//            }
//            XMLGregorianCalendar calendar = null;
//            if (password.getLastFailedLogin() != null) {
//                calendar = password.getLastFailedLogin().getTimestamp();
//            }
//            if (calendar != null) {
//                credentials.setLastFailedLoginAttempt(calendar.toGregorianCalendar().getTimeInMillis());
//            } else {
//                credentials.setLastFailedLoginAttempt(0);
//            }
//        }
//
//        return user;
    }

//    private Element createQuery(String username) throws SchemaException {
//        return QueryUtil.createEqualFilter(DOMUtil.getDocument(), null, ObjectType.F_NAME, username);
//    }

    private PrincipalUser save(PrincipalUser person) throws RepositoryException {
        try {
            UserType oldUserType = getUserByOid(person.getOid());
            PrismObject<UserType> oldUser = oldUserType.asPrismObject();

            PrismObject<UserType> newUser = person.getUser().asPrismObject();

//            updateUserType(newUserType, person);

            ObjectDelta<UserType> delta = oldUser.diff(newUser);
            repositoryService.modifyObject(UserType.class, delta.getOid(), delta.getModifications(),
                    new OperationResult(OPERATION_UPDATE_USER));
        } catch (Exception ex) {
            throw new RepositoryException(ex.getMessage(), ex);
        }

        return person;
    }

    private UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        ObjectType object = repositoryService.getObject(UserType.class, oid,
                new OperationResult(OPERATION_GET_USER)).asObjectable();
        if (object != null && (object instanceof UserType)) {
            return (UserType) object;
        }

        return null;
    }

//    private void updateUserType(UserType userType, PrincipalUser user) {
//        CredentialsType credentials = userType.getCredentials();
//        if (credentials == null) {
//            credentials = new CredentialsType();
//            userType.setCredentials(credentials);
//        }
//        PasswordType password = credentials.getPassword();
//        if (password == null) {
//            password = new PasswordType();
//            credentials.setPassword(password);
//        }
//
//        password.setFailedLogins(user.getCredentials().getFailedLogins());
//
//        try {
//            GregorianCalendar gc = new GregorianCalendar();
//            gc.setTimeInMillis(user.getCredentials().getLastFailedLoginAttempt());
//            XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
//            LoginEventType loginEvent = new LoginEventType();
//            loginEvent.setTimestamp(calendar);
//            loginEvent.setFrom(null);   //todo implement...
//            password.setLastFailedLogin(loginEvent);
//        } catch (DatatypeConfigurationException ex) {
//            LOGGER.error("Can't save last failed login timestamp, reason: " + ex.getMessage());
//        }
//    }
}
