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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.codec.binary.Base64;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelUtilsTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller");

	@Test(expected = IllegalArgumentException.class)
	public void createReferenceNullOid() {
		ModelUtils.createReference(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createReferenceEmptyOid() {
		ModelUtils.createReference("", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createReferenceNullType() {
		ModelUtils.createReference(SystemObjectsType.SYSTEM_CONFIGURATION.value(), null);
	}

	@Test
	public void createReference() {
		String expectedOid = SystemObjectsType.SYSTEM_CONFIGURATION.value();

		ObjectReferenceType ref = ModelUtils.createReference(expectedOid, ObjectTypes.SYSTEM_CONFIGURATION);
		assertNotNull(ref);
		assertEquals(ref.getOid(), expectedOid);
		assertEquals(ObjectTypes.SYSTEM_CONFIGURATION.getQName(), ref.getType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void validatePagingNull() {
		ModelUtils.validatePaging(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void validatePagingBadOffsetAttribute() {
		ModelUtils.validatePaging(PagingTypeFactory
				.createPaging(-5, 10, OrderDirectionType.ASCENDING, "name"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void validatePagingBadMaxAttribute() {
		ModelUtils.validatePaging(PagingTypeFactory
				.createPaging(5, -10, OrderDirectionType.ASCENDING, "name"));
	}

	@Test
	public void validatePagingGood() {
		ModelUtils
				.validatePaging(PagingTypeFactory.createPaging(5, 10, OrderDirectionType.ASCENDING, "name"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getPasswordFromNullAccount() {
		ModelUtils.getPassword(null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getPasswordExistingAccount() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "account-with-pwd.xml"))).getValue();
		CredentialsType.Password password = ModelUtils.getPassword(account);
		assertNotNull(password);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getPasswordNonExistingAccount() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "account-without-pwd.xml"))).getValue();
		CredentialsType.Password password = ModelUtils.getPassword(account);
		assertNotNull(password);
	}

	@Test(expected = IllegalArgumentException.class)
	public void generatePasswordNullAccount() {
		ModelUtils.generatePassword(null, 5);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void generatePasswordBadLength() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "account-with-pwd.xml"))).getValue();
		int length = 5;
		ModelUtils.generatePassword(account, length);

		CredentialsType.Password password = ModelUtils.getPassword(account);
		assertNotNull(password);
		assertNotNull(password.getAny());
		assertEquals(true, password.getAny() instanceof Element);

		Element element = (Element) password.getAny();
		assertNotNull(element.getTextContent());
		assertEquals(length, new String(Base64.decodeBase64(element.getTextContent())).length());
	}

	@Test(expected = IllegalArgumentException.class)
	public void getAccountTypeDefinitionFromSchemaHandlingNullAccount() {
		ModelUtils.getAccountTypeDefinitionFromSchemaHandling(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getAccountTypeDefinitionFromSchemaHandlingNullResource() {
		ModelUtils.getAccountTypeDefinitionFromSchemaHandling(new AccountShadowType(), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getAccountTypeDefinitionFromSchemaHandlingNonExisting() throws Exception {
		getAccountTypeDefinitionFromSchemaHandlingNonExisting("account-no-schema-handling.xml");
	}

	@Test(expected = IllegalArgumentException.class)
	public void getAccountTypeDefinitionFromSchemaHandlingNonExisting2() throws Exception {
		getAccountTypeDefinitionFromSchemaHandlingNonExisting("account-no-schema-handling2.xml");
	}

	@SuppressWarnings("unchecked")
	private void getAccountTypeDefinitionFromSchemaHandlingNonExisting(String fileName) throws JAXBException {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, fileName))).getValue();

		ModelUtils.getAccountTypeDefinitionFromSchemaHandling(account, account.getResource());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getAccountTypeDefinitionFromSchemaHandlingExisting() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "account-schema-handling.xml"))).getValue();

		ModelUtils.getAccountTypeDefinitionFromSchemaHandling(account, account.getResource());
	}

	@Ignore
	@Test
	public void createPropertyReferenceListType() throws Exception {
		PropertyReferenceListType list = ModelUtils.createPropertyReferenceListType("", null, "resource",
				"account");
		assertNotNull(list);
		assertEquals(2, list.getProperty().size());

		TraceManager.getTrace(ModelUtilsTest.class).info(
				JAXBUtil.marshalWrap(list, new QName(SchemaConstants.NS_C, "PropertyReferenceListType")));

		// PropertyReferenceListType expectedList =
		// ((JAXBElement<PropertyReferenceListType>) JAXBUtil
		// .unmarshal(new File(TEST_FOLDER,
		// "property-list-type.xml"))).getValue();

		XmlAsserts.assertPatch(new File(TEST_FOLDER, "property-list-type.xml"),
				JAXBUtil.marshalWrap(list, new QName(SchemaConstants.NS_C, "PropertyReferenceListType")));
	}
}
