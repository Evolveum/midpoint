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
package com.evolveum.midpoint.web.model.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;

/**
 * 
 * @author lazyman
 *
 */

@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/application-context-webapp.xml",
		"file:src/main/webapp/WEB-INF/application-context-init.xml",
		"file:src/main/webapp/WEB-INF/application-context-security.xml",
		"classpath:application-context-test.xml",
		"classpath:application-context-repository-test.xml" })
public class UserManagerImplTest extends AbstractTestNGSpringContextTests  {

	private static final Trace LOGGER = TraceManager.getTrace(UserManagerImplTest.class);
	@Autowired(required = true)
	private transient ObjectTypeCatalog catalog;
	
	@BeforeMethod
	public void before(){ 
		AssertJUnit.assertNotNull(catalog);
		AssertJUnit.assertNotNull(ControllerUtil.getUserManager(catalog));
	}
	
	@Test
	public void userManagerExists() {
//		UserManager manager = ControllerUtil.getUserManager(catalog);
//		
//		manager.g
		LOGGER.info(ControllerUtil.getUserManager(catalog).toString());
	}
}
