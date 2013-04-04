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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.prism.xml.ns._public.query_2.OrderDirectionType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ModelUtilsTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller");
	@Autowired(required = true)
	private Protector protector;
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void validatePagingNull() {
		ModelUtils.validatePaging(null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void validatePagingBadOffsetAttribute() {
		ModelUtils.validatePaging(ObjectPaging.createPaging(-5, 10, ObjectType.F_NAME, OrderDirection.ASCENDING));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void validatePagingBadMaxAttribute() {
		ModelUtils.validatePaging(ObjectPaging
				.createPaging(5, -10, ObjectType.F_NAME, OrderDirection.ASCENDING));
	}

	@Test
	public void validatePagingGood() {
		ModelUtils
				.validatePaging(ObjectPaging
						.createPaging(5, 10, ObjectType.F_NAME, OrderDirection.ASCENDING));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void getPasswordFromNullAccount() {
		ModelUtils.getPassword(null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getPasswordExistingAccount() throws Exception {
		ShadowType account = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER, "account-with-pwd.xml"), ShadowType.class);
		PasswordType password = ModelUtils.getPassword(account);
		assertNotNull(password);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getPasswordNonExistingAccount() throws Exception {
		ShadowType account = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER, "account-without-pwd.xml"), ShadowType.class);
		PasswordType password = ModelUtils.getPassword(account);
		assertNotNull(password);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void generatePasswordNullAccount() throws EncryptionException {
		ModelUtils.generatePassword(null, 5, protector);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void generatePasswordBadLength() throws Exception {
		ShadowType account = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER, "account-with-pwd.xml"), ShadowType.class);
		int length = 5;
		ModelUtils.generatePassword(account, length, protector);

		PasswordType password = ModelUtils.getPassword(account);
		assertNotNull(password);
		assertNotNull(password.getValue());
		assertNotNull(password.getValue().getEncryptedData());

		String decrypted = protector.decryptString(password.getValue());
		assertEquals(length, decrypted.length());
	}

	@Test
	public void createPropertyReferenceListType() throws Exception {
		PropertyReferenceListType list = ModelUtils.createPropertyReferenceListType("", null, "resource",
				"account");
		assertNotNull(list);
		assertEquals(2, list.getProperty().size());

		// TODO
//		XmlAsserts.assertPatch(new File(TEST_FOLDER, "property-list-type.xml"), JAXBUtil.marshalWrap(list));
	}

	@Test
	public void testActivationDisabled() throws Exception {
		ActivationType activation = new ActivationType();
		activation.setEnabled(false);

		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(new Date().getTime());
		calendar.add(Calendar.DAY_OF_YEAR, -1);

		XMLGregorianCalendar from = DatatypeFactory.newInstance().newXMLGregorianCalendar(
				(GregorianCalendar) calendar);
		activation.setValidFrom(from);

		assertFalse(ModelUtils.isActivationEnabled(activation));
	}

	@Test
	public void testActivationBeforeFrom() throws Exception {
		ActivationType activation = new ActivationType();

		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(new Date().getTime());
		calendar.add(Calendar.DAY_OF_YEAR, 1);

		XMLGregorianCalendar from = DatatypeFactory.newInstance().newXMLGregorianCalendar(
				(GregorianCalendar) calendar);
		activation.setValidFrom(from);

		assertFalse(ModelUtils.isActivationEnabled(activation));
	}

	@Test
	public void testActivationAfterTo() throws Exception {
		ActivationType activation = new ActivationType();

		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(new Date().getTime());
		calendar.add(Calendar.DAY_OF_YEAR, -3);
		XMLGregorianCalendar from = DatatypeFactory.newInstance().newXMLGregorianCalendar(
				(GregorianCalendar) calendar);
		activation.setValidFrom(from);

		calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(new Date().getTime());
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		XMLGregorianCalendar to = DatatypeFactory.newInstance().newXMLGregorianCalendar(
				(GregorianCalendar) calendar);
		activation.setValidTo(to);

		assertFalse(ModelUtils.isActivationEnabled(activation));
	}

	@Test
	public void testActivationCorrect() throws Exception {
		ActivationType activation = new ActivationType();

		Calendar calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(new Date().getTime());
		calendar.add(Calendar.DAY_OF_YEAR, -3);
		XMLGregorianCalendar from = DatatypeFactory.newInstance().newXMLGregorianCalendar(
				(GregorianCalendar) calendar);
		activation.setValidFrom(from);

		calendar = GregorianCalendar.getInstance();
		calendar.setTimeInMillis(new Date().getTime());
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		XMLGregorianCalendar to = DatatypeFactory.newInstance().newXMLGregorianCalendar(
				(GregorianCalendar) calendar);
		activation.setValidTo(to);

		assertTrue(ModelUtils.isActivationEnabled(activation));
	}
}
