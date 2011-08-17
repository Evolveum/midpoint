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

import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.web.model.RepositoryException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;

/**
 * 
 * @author lazyman
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private static final Trace TRACE = TraceManager.getTrace(UserDetailsServiceImpl.class);
	@Autowired(required = true)
	private transient ModelPortType modelService;

	@Override
	public PrincipalUser getUser(String principal) {
		PrincipalUser user = null;
		try {
			user = findByUsername(principal);
		} catch (FaultMessage ex) {
			TRACE.warn("Couldn't find user with name '{}', reason: {}.",
					new Object[] { principal, ex.getMessage() });
		}

		return user;
	}

	@Override
	public void updateUser(PrincipalUser user) {
		try {
			save(user);
		} catch (RepositoryException ex) {
			TRACE.warn("Couldn't save user '{}, ({})', reason: {}.",
					new Object[] { user.getFullName(), user.getOid(), ex.getMessage() });
		}
	}

	private PrincipalUser findByUsername(String username) throws FaultMessage {
		QueryType query = new QueryType();
		query.setFilter(createQuery(username));
		TRACE.trace("Looking for user, query:\n" + DOMUtil.printDom(query.getFilter()));

		ObjectListType list = modelService.searchObjects(
				ObjectTypes.USER.getObjectTypeUri(),
				query, new PagingType(),
				new Holder<OperationResultType>(new OperationResultType()));
		if (list == null) {
			return null;
		}
		List<ObjectType> objects = list.getObject();
		TRACE.trace("Users found: {}.", new Object[] { objects.size() });
		if (objects.size() == 0 || objects.size() > 1) {
			return null;
		}

		return createUser((UserType) objects.get(0));
	}

	private PrincipalUser createUser(UserType userType) {
		boolean enabled = false;
		CredentialsType credentialsType = userType.getCredentials();
		if (credentialsType != null && credentialsType.isAllowedIdmGuiAccess() != null) {
			enabled = credentialsType.isAllowedIdmGuiAccess();
		}

		PrincipalUser user = new PrincipalUser(userType.getOid(), userType.getName(), enabled);
		user.setFamilyName(userType.getFamilyName());
		user.setFullName(userType.getFullName());
		user.setGivenName(userType.getGivenName());

		if (credentialsType != null && credentialsType.getPassword() != null) {
			CredentialsType.Password password = credentialsType.getPassword();

			Credentials credentials = user.getCredentials();
			Element pwd = getValue(password.getAny());
			credentials.setPassword(pwd.getTextContent(), pwd.getLocalName());
			if (password.getFailedLogins() == null || password.getFailedLogins().intValue() < 0) {
				credentials.setFailedLogins(0);
			} else {
				credentials.setFailedLogins(password.getFailedLogins().intValue());
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

	private Element getValue(Object object) {
		if (object == null) {
			return null;
		}

		if (object instanceof Node) {
			Node node = (Node) object;
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) node;
			}
		}

		return null;
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
			UserType userType = getUserByOid(person.getOid());
			UserType oldUserType = (UserType) JAXBUtil.clone(userType);

			updateUserType(userType, person);

			ObjectFactory of = new ObjectFactory();
			ObjectContainerType userContainer = of.createObjectContainerType();
			userContainer.setObject(userType);

			ObjectModificationType modification = CalculateXmlDiff.calculateChanges(oldUserType, userType);
			if (modification != null && modification.getOid() != null) {
				modelService.modifyObject(
						ObjectTypes.USER.getObjectTypeUri(),
						modification, new Holder<OperationResultType>(
						new OperationResultType()));
			}
		} catch (com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage ex) {
			StringBuilder message = new StringBuilder();
			message.append("Can't save user, reason: ");
			if (ex.getFaultInfo() != null) {
				message.append(ex.getFaultInfo().getMessage());
			} else {
				message.append(ex.getMessage());
			}
			throw new RepositoryException(message.toString(), ex);
		} catch (DiffException ex) {
			throw new RepositoryException("Can't save user. Unexpected error: "
					+ "Couldn't create create diff.", ex);
		} catch (JAXBException ex) {
			// TODO: finish
		}

		return null;
	}

	private UserType getUserByOid(String oid) throws FaultMessage {
		ObjectType object = modelService.getObject(
				ObjectTypes.USER.getObjectTypeUri(),
				oid, new PropertyReferenceListType(),
				new Holder<OperationResultType>(new OperationResultType()));
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
		CredentialsType.Password password = credentials.getPassword();
		if (password == null) {
			password = new CredentialsType.Password();
			credentials.setPassword(password);
		}

		password.setFailedLogins(new BigInteger(Integer.toString(user.getCredentials().getFailedLogins())));

		try {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTimeInMillis(user.getCredentials().getLastFailedLoginAttempt());
			XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
			password.setLastFailedLoginTimestamp(calendar);
		} catch (DatatypeConfigurationException ex) {
			TRACE.error("Can't save last failed login timestamp, reason: " + ex.getMessage());
		}
	}
}
