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

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelUtils {

	private static final Trace LOGGER = TraceManager.getTrace(ModelUtils.class);

	public static void validatePaging(ObjectPaging paging) {
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

	public static void generatePassword(ShadowType account, int length, Protector protector)
			throws EncryptionException {
		Validate.notNull(account, "Account shadow must not be null.");
		Validate.isTrue(length > 0, "Password length must be more than zero.");
		Validate.notNull(protector, "Protector instance must not be null.");

		String pwd = "";
		if (length > 0) {
			pwd = new RandomString(length).nextString();
		}

		PasswordType password = getPassword(account);
		password.setValue(protector.encryptString(pwd));
	}

	public static PasswordType getPassword(ShadowType account) {
		Validate.notNull(account, "Account shadow must not be null.");

		CredentialsType credentials = account.getCredentials();
		ObjectFactory of = new ObjectFactory();
		if (credentials == null) {
			credentials = of.createCredentialsType();
			account.setCredentials(credentials);
		}
		PasswordType password = credentials.getPassword();
		if (password == null) {
			password = of.createPasswordType();
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

	public static boolean isActivationEnabled(ActivationType activation) {
		if (activation == null) {
			return true;
		}

		if (activation.isEnabled() != null) {
			return activation.isEnabled();
		}

		Calendar actual = Calendar.getInstance();
		if (activation.getValidFrom() != null) {
			GregorianCalendar calendar = activation.getValidFrom().toGregorianCalendar();
			if (actual.before(calendar)) {
				return false;
			}
		}

		if (activation.getValidTo() != null) {
			GregorianCalendar calendar = activation.getValidTo().toGregorianCalendar();
			if (actual.after(calendar)) {
				return false;
			}
		}

		return true;
	}
}
