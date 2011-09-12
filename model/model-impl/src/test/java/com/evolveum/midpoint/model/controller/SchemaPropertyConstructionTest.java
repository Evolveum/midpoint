/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.controller;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml",
		"classpath:application-context-configuration-test-no-repo.xml",
		"classpath:application-context-task.xml" })
public class SchemaPropertyConstructionTest extends AbstractTestNGSpringContextTests {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaPropertyConstructionTest.class);
	private static final File TEST_FOLDER = new File("./src/test/resources/controller/schema");
	@Autowired(required = true)
	private transient SchemaHandler handler;

	@BeforeMethod
	public void beforeMethod() {
		LOGGER.debug("***************** TEST METHOD START *****************");
	}

	@AfterMethod
	public void afterMethod() {
		LOGGER.debug("***************** TEST METHOD END *****************");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullUser() throws SchemaException {
		handler.processPropertyConstructions(null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullUserTemplate() throws SchemaException {
		handler.processPropertyConstructions(new UserType(), null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullOperationResult() throws SchemaException {
		handler.processPropertyConstructions(new UserType(), new UserTemplateType(), null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void userWithoutAttribute() throws Exception {
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"user-without-fullname.xml"))).getValue();
		UserTemplateType template = ((JAXBElement<UserTemplateType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"template.xml"))).getValue();

		OperationResult result = new OperationResult("User without fullname (property construction test)");
		try {
			user = handler.processPropertyConstructions(user, template, result);
		} catch (Exception ex) {
			LOGGER.debug("Error occured, reason: " + ex.getMessage(), ex);
		} finally {
			LOGGER.debug(result.dump());
		}

		XmlAsserts.assertPatch(new File(TEST_FOLDER, "expected-user.xml"), JAXBUtil.silentMarshalWrap(user));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void userWithAttributeAndDefaultTrue() throws Exception {
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"user-with-fullname.xml"))).getValue();
		UserTemplateType template = ((JAXBElement<UserTemplateType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"template.xml"))).getValue();

		OperationResult result = new OperationResult("User with fullname (property construction test)");
		try {
			user = handler.processPropertyConstructions(user, template, result);
			LOGGER.trace("Updated user:\n{}", JAXBUtil.silentMarshalWrap(user));
		} catch (Exception ex) {
			LOGGER.debug("Error occured, reason: " + ex.getMessage(), ex);
		} finally {
			LOGGER.debug(result.dump());
		}

		XmlAsserts.assertPatch(new File(TEST_FOLDER, "expected-user-default.xml"), JAXBUtil.silentMarshalWrap(user));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void userWithAttributeAndDefaultFalse() throws Exception {
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"user-with-fullname.xml"))).getValue();
		UserTemplateType template = ((JAXBElement<UserTemplateType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"template.xml"))).getValue();
		
		List<PropertyConstructionType> properties = template.getPropertyConstruction();
		for (PropertyConstructionType construction : properties) {
			XPathHolder holder = new XPathHolder(construction.getProperty());
			List<XPathSegment> segments = holder.toSegments();
			if (segments.get(segments.size()-1).getQName().getLocalPart().equals("fullName")) {
				construction.getValueConstruction().setDefault(false);
			}
		}

		OperationResult result = new OperationResult("User with fullname (property construction test)");
		try {
			user = handler.processPropertyConstructions(user, template, result);
			LOGGER.trace("Updated user:\n{}", JAXBUtil.silentMarshalWrap(user));
		} catch (Exception ex) {
			LOGGER.debug("Error occured, reason: " + ex.getMessage(), ex);
		} finally {
			LOGGER.debug(result.dump());
		}

		XmlAsserts.assertPatch(new File(TEST_FOLDER, "expected-user.xml"), JAXBUtil.silentMarshalWrap(user));
	}
}
