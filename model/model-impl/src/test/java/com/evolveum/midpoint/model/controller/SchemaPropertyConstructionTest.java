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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.exception.SchemaException;
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
}
