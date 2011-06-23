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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.model.ModelServiceOld;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelUtils {

	public static void validatePaging(PagingType paging) {
		if (paging.getMaxSize() != null && paging.getMaxSize().longValue() < 0) {
			throw new IllegalArgumentException("Paging max size must be more than 0.");
		}
		if (paging.getOffset() != null && paging.getOffset().longValue() < 0) {
			throw new IllegalArgumentException("Paging offset index must be more than 0.");
		}
	}

	public static AccountType getAccountTypeDefinitionFromSchemaHandling(
			ResourceObjectShadowType accountShadow, ResourceType resource) {
		Validate.notNull(accountShadow);
		Validate.notNull(resource);
		
		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
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

		throw new IllegalArgumentException("Provided wrong AccountShadow or SchemaHandling. "
				+ "No AccountType definition found for provided account's object class: "
				+ accountObjectClass);
	}

	public static void generatePassword(AccountShadowType account, int length) {
		String pwd = "";
		if (length > 0) {
			pwd = new RandomString(length).nextString();
		}

		CredentialsType.Password password = ModelServiceOld.getPassword(account);
		if (password.getAny() != null) {
			return;
		}

		Document document = DOMUtil.getDocument();
		Element hash = document.createElementNS(SchemaConstants.NS_C, "c:base64");
		hash.setTextContent(Base64.encodeBase64String(pwd.getBytes()));
		password.setAny(hash);
	}
}
