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

import javax.xml.bind.JAXBElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullUser() throws SchemaException {
		handler.processPropertyConstruction(null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullUserTemplate() throws SchemaException {
		handler.processPropertyConstruction(new UserType(), null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullOperationResult() throws SchemaException {
		handler.processPropertyConstruction(new UserType(), new UserTemplateType(), null);
	}

	@SuppressWarnings("unchecked")
	@Test(enabled = false)
	public void userWithoutAttribute() throws Exception {
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"user-without-fullname.xml"))).getValue();
		UserTemplateType template = ((JAXBElement<UserTemplateType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"template.xml"))).getValue();

		OperationResult result = new OperationResult("User without fullname (property construction test)");
		try {
			user = handler.processPropertyConstruction(user, template, result);
		} finally {
			LOGGER.debug(result.dump());
		}

		XmlAsserts.assertPatch(new File(TEST_FOLDER, "expected-user.xml"), JAXBUtil.silentMarshalWrap(user));
	}

	@SuppressWarnings("unchecked")
	@Test(enabled = false)
	public void userWithAttribute() throws Exception {
		UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"user-with-fullname.xml"))).getValue();
		UserTemplateType template = ((JAXBElement<UserTemplateType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"template.xml"))).getValue();

		OperationResult result = new OperationResult("User with fullname (property construction test)");
		try {
			user = handler.processPropertyConstruction(user, template, result);
		} finally {
			LOGGER.debug(result.dump());
		}

		XmlAsserts.assertPatch(new File(TEST_FOLDER, "expected-user.xml"), JAXBUtil.silentMarshalWrap(user));
	}
}
