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

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelUtilsTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller");

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void createReferenceNullOid() {
		ModelUtils.createReference(null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void createReferenceEmptyOid() {
		ModelUtils.createReference("", null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void createReferenceNullType() {
		ModelUtils.createReference(SystemObjectsType.SYSTEM_CONFIGURATION.value(), null);
	}

	@Test
	public void createReference() {
		String expectedOid = SystemObjectsType.SYSTEM_CONFIGURATION.value();

		ObjectReferenceType ref = ModelUtils.createReference(expectedOid, ObjectTypes.SYSTEM_CONFIGURATION);
		assertNotNull(ref);
		assertEquals(ref.getOid(), expectedOid);
		assertEquals(ObjectTypes.SYSTEM_CONFIGURATION.getTypeQName(), ref.getType());
	}

	@Test
	public void validatePagingNull() {
		ModelUtils.validatePaging(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void validatePagingBadOffsetAttribute() {
		ModelUtils.validatePaging(PagingTypeFactory
				.createPaging(-5, 10, OrderDirectionType.ASCENDING, "name"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void validatePagingBadMaxAttribute() {
		ModelUtils.validatePaging(PagingTypeFactory
				.createPaging(5, -10, OrderDirectionType.ASCENDING, "name"));
	}

	@Test
	public void validatePagingGood() {
		ModelUtils
				.validatePaging(PagingTypeFactory.createPaging(5, 10, OrderDirectionType.ASCENDING, "name"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
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

	@Test(expectedExceptions = IllegalArgumentException.class)
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

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getAccountTypeDefinitionFromSchemaHandlingNullAccount() {
		ModelUtils.getAccountTypeFromHandling((ResourceObjectShadowType)null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getAccountTypeDefinitionFromSchemaHandlingNullResource() {
		ModelUtils.getAccountTypeFromHandling(new AccountShadowType(), null);
	}

	@Test
	public void getAccountTypeDefinitionFromSchemaHandlingNonExisting() throws Exception {
		getAccountTypeDefinitionFromSchemaHandlingNonExisting("account-no-schema-handling.xml");
	}

	@Test
	public void getAccountTypeDefinitionFromSchemaHandlingNonExisting2() throws Exception {
		getAccountTypeDefinitionFromSchemaHandlingNonExisting("account-no-schema-handling2.xml");
	}

	@SuppressWarnings("unchecked")
	private void getAccountTypeDefinitionFromSchemaHandlingNonExisting(String fileName) throws JAXBException {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, fileName))).getValue();

		assertNull(ModelUtils.getAccountTypeFromHandling(account, account.getResource()));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getAccountTypeDefinitionFromSchemaHandlingExisting() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "account-schema-handling.xml"))).getValue();

		ModelUtils.getAccountTypeFromHandling(account, account.getResource());
	}

	@Test
	public void createPropertyReferenceListType() throws Exception {
		PropertyReferenceListType list = ModelUtils.createPropertyReferenceListType("", null, "resource",
				"account");
		assertNotNull(list);
		assertEquals(2, list.getProperty().size());

		XmlAsserts.assertPatch(new File(TEST_FOLDER, "property-list-type.xml"), JAXBUtil.marshalWrap(list));
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void unresolveResourceObjectNull() {
		ModelUtils.unresolveResourceObjectShadow(null);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void unresolveResourceObject() throws Exception {
		AccountShadowType account = ((JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "account-schema-handling.xml"))).getValue();
		
		assertNotNull(account.getResource());
		String resourceOid = account.getResource().getOid();
		
		ModelUtils.unresolveResourceObjectShadow(account);
		assertNull(account.getResource());
		assertNotNull(account.getResourceRef());
		
		ObjectReferenceType ref = account.getResourceRef();
		assertEquals(resourceOid, ref.getOid());
		assertEquals(ObjectTypes.RESOURCE.getTypeQName(), ref.getType());
	}
}
