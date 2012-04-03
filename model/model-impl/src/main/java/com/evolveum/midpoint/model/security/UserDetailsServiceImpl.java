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

import com.evolveum.midpoint.model.security.api.Credentials;
import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.model.security.api.UserDetailsService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author lazyman
 */
@Service(value = "userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Trace LOGGER = TraceManager.getTrace(UserDetailsServiceImpl.class);
    @Autowired(required = true)
    private transient RepositoryService repositoryService;

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
        QueryType query = new QueryType();
        query.setFilter(createQuery(username));
        LOGGER.trace("Looking for user, query:\n" + DOMUtil.printDom(query.getFilter()));

        List<PrismObject<UserType>> list = repositoryService.searchObjects(UserType.class, query, new PagingType(),
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
        CredentialsType credentialsType = userType.getCredentials();
        
        PrincipalUser user = new PrincipalUser(userType, true);
        if (credentialsType != null && credentialsType.getPassword() != null) {
            PasswordType password = credentialsType.getPassword();

            Credentials credentials = user.getCredentials();
            credentials.setPassword(password.getProtectedString());
            if (password.getFailedLogins() == null || password.getFailedLogins() < 0) {
                credentials.setFailedLogins(0);
            } else {
                credentials.setFailedLogins(password.getFailedLogins());
            }
            XMLGregorianCalendar calendar = password.getLastFailedLoginTimestamp();
            if (calendar != null) {
                credentials.setLastFailedLoginAttempt(calendar.toGregorianCalendar().getTimeInMillis());
            } else {
                credentials.setLastFailedLoginAttempt(0);
            }
        }

        return user;
    }

    private Element createQuery(String username) {
        Document document = DOMUtil.getDocument();
        Element and = document.createElementNS(SchemaConstants.NS_C, "c:and");
        document.appendChild(and);

        Element type = document.createElementNS(SchemaConstants.NS_C, "c:type");
        type.setAttribute("uri", "http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd#UserType");
        and.appendChild(type);

        Element equal = document.createElementNS(SchemaConstants.NS_C, "c:equal");
        and.appendChild(equal);
        Element value = document.createElementNS(SchemaConstants.NS_C, "c:value");
        equal.appendChild(value);
        Element name = document.createElementNS(SchemaConstants.NS_C, "c:name");
        name.setTextContent(username);
        value.appendChild(name);

        return and;
    }

    private PrincipalUser save(PrincipalUser person) throws RepositoryException {
        try {
            UserType newUserType = getUserByOid(person.getOid());
            PrismObject<UserType> newUser = newUserType.asPrismObject();

            PrismObject<UserType> oldUser = newUser.clone();

            updateUserType(newUserType, person);

            ObjectDelta<UserType> delta = oldUser.diff(newUser);
            repositoryService.modifyObject(UserType.class, delta.getOid(), delta.getModifications(), 
                    new OperationResult(OPERATION_UPDATE_USER));
        } catch (Exception ex) {
            throw new RepositoryException(ex.getMessage(),  ex);
        }

        return person;
    }

    private UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        ObjectType object = repositoryService.getObject(UserType.class, oid, null,
                new OperationResult(OPERATION_GET_USER)).asObjectable();
        if (object != null && (object instanceof UserType)) {
            return (UserType) object;
        }

        return null;
    }

    private void updateUserType(UserType userType, PrincipalUser user) {
        CredentialsType credentials = userType.getCredentials();
        if (credentials == null) {
            credentials = new CredentialsType();
            userType.setCredentials(credentials);
        }
        PasswordType password = credentials.getPassword();
        if (password == null) {
            password = new PasswordType();
            credentials.setPassword(password);
        }

        password.setFailedLogins(user.getCredentials().getFailedLogins());

        try {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTimeInMillis(user.getCredentials().getLastFailedLoginAttempt());
            XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            password.setLastFailedLoginTimestamp(calendar);
        } catch (DatatypeConfigurationException ex) {
            LOGGER.error("Can't save last failed login timestamp, reason: " + ex.getMessage());
        }
    }
}
