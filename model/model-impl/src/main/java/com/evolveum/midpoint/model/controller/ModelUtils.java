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
 */
package com.evolveum.midpoint.model.controller;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelUtils {

	private static final Trace LOGGER = TraceManager.getTrace(ModelUtils.class);

	public static void unresolveResourceObjectShadow(ResourceObjectShadowType shadow) {
		Validate.notNull(shadow, "Resource object shadow must not be null.");

		if (shadow.getResource() == null) {
			return;
		}

		ObjectReferenceType reference = createReference(shadow.getResource().getOid(), ObjectTypes.RESOURCE);
		shadow.setResource(null);
		shadow.setResourceRef(reference);
	}

	public static ObjectReferenceType createReference(String oid, ObjectTypes type) {
		Validate.notEmpty(oid, "Oid must not be null or empty.");
		Validate.notNull(type, "Object type must not be null.");

		ObjectReferenceType reference = new ObjectReferenceType();
		reference.setType(type.getTypeQName());
		reference.setOid(oid);

		return reference;
	}

	public static void validatePaging(PagingType paging) {
		if (paging == null) {
			return;
		}

		if (paging.getMaxSize() != null && paging.getMaxSize().longValue() < 0) {
			throw new IllegalArgumentException("Paging max size must be more than 0.");
		}
		if (paging.getOffset() != null && paging.getOffset().longValue() < 0) {
			throw new IllegalArgumentException("Paging offset index must be more than 0.");
		}
	}

	public static AccountType getAccountTypeFromHandling(ResourceObjectShadowType accountShadow,
			ResourceType resource) {
		Validate.notNull(accountShadow, "Resource object shadow must not be null.");
		Validate.notNull(resource, "Resource must not be null.");

		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
		if (schemaHandling == null) {
			return null;
		}
		QName accountObjectClass = accountShadow.getObjectClass();

		for (AccountType accountType : schemaHandling.getAccountType()) {
			if (accountObjectClass.equals(accountType.getObjectClass())) {
				return accountType;
			}
		}

		// no suitable definition found, then use default account
		for (AccountType accountType : schemaHandling.getAccountType()) {
			if (accountType.isDefault()) {
				return accountType;
			}
		}

		return null;
	}

	public static void generatePassword(AccountShadowType account, int length, Protector protector)
			throws EncryptionException {
		Validate.notNull(account, "Account shadow must not be null.");
		Validate.isTrue(length > 0, "Password length must be more than zero.");
		Validate.notNull(protector, "Protector instance must not be null.");

		String pwd = "";
		if (length > 0) {
			pwd = new RandomString(length).nextString();
		}

		CredentialsType.Password password = getPassword(account);
		password.setProtectedString(protector.encryptString(pwd));
	}

	public static CredentialsType.Password getPassword(AccountShadowType account) {
		Validate.notNull(account, "Account shadow must not be null.");

		CredentialsType credentials = account.getCredentials();
		ObjectFactory of = new ObjectFactory();
		if (credentials == null) {
			credentials = of.createCredentialsType();
			account.setCredentials(credentials);
		}
		CredentialsType.Password password = credentials.getPassword();
		if (password == null) {
			password = of.createCredentialsTypePassword();
			credentials.setPassword(password);
		}

		return password;
	}

	public static PropertyReferenceListType createPropertyReferenceListType(String... properties) {
		PropertyReferenceListType list = new PropertyReferenceListType();
		if (properties == null) {
			return list;
		}

		for (String property : properties) {
			if (StringUtils.isEmpty(property)) {
				LOGGER.warn("Trying to add empty or null property to PropertyReferenceListType, skipping.");
				continue;
			}
			list.getProperty().add(Utils.fillPropertyReference(property));
		}

		return list;
	}

	public static AccountType getAccountTypeFromHandling(String accountTypeName, ResourceType resource) {
		Validate.notNull(resource, "Resource must not be null.");

		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
		if (schemaHandling == null) {
			throw new IllegalArgumentException(
					"Provided resource definition doesn't contain schema handling.");
		}

		if (StringUtils.isNotEmpty(accountTypeName)) {
			for (AccountType accountType : schemaHandling.getAccountType()) {
				if (accountTypeName.equals(accountType.getName())) {
					return accountType;
				}
			}
		}

		// no suitable definition found, then use default account
		for (AccountType accountType : schemaHandling.getAccountType()) {
			if (accountType.isDefault()) {
				return accountType;
			}
		}

		throw new IllegalArgumentException("No schema handlig account type for name '" + accountTypeName
				+ "' found.");
	}
}
