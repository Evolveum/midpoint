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
package com.evolveum.midpoint.web.controller;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * 
 * @author lazyman
 *
 */

@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/application-context-webapp.xml",
		"file:src/main/webapp/WEB-INF/application-context-init.xml",
		"file:src/main/webapp/WEB-INF/application-context-security.xml",
		"classpath:application-context-test.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class LoginControllerTest extends AbstractTestNGSpringContextTests{

	@Autowired(required = true)
	LoginController controller;
	
	@BeforeMethod
	public void before() {
		assertNotNull(controller);
	}
	
	@Test
	public void nullUsername() {
		controller.setUserName(null);
		controller.setUserPassword("qwe123");
		controller.setAdminName(null);
		controller.setAdminPassword("qwe123");
		//TODO: remove ignore on test and fix it
		//faces utils not working - FacesContext.getCurrentInstance() is null
		assertNull(controller.loginAdmin());
	}
}
