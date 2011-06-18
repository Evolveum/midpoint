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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.test;

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.init.InitialSetup;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.UserManager;
import com.evolveum.midpoint.web.model.dto.GuiUserDto;
import com.evolveum.midpoint.web.model.dto.UserDto;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * Test of spring application context initialization
 * 
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/application-context-webapp.xml",
		"file:src/main/webapp/WEB-INF/application-context-security.xml",
		"file:src/main/webapp/WEB-INF/application-context-init.xml", "classpath:applicationContext-test.xml" })
public class SpringApplicationContextTest {

	@Autowired(required = true)
	private ObjectTypeCatalog objectTypeCatalog;
	@Autowired(required = true)
	private ModelPortType modelService;
	@Autowired(required = true)
	private InitialSetup initialSetup;

	public SpringApplicationContextTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void initApplicationContext() {
		assertNotNull(objectTypeCatalog.listSupportedObjectTypes());
		ObjectManager<UserDto> objectManager = objectTypeCatalog.getObjectManager(UserDto.class,
				GuiUserDto.class);
		UserManager userManager = (UserManager) (objectManager);
		assertNotNull(userManager);

		assertNotNull(modelService);
		assertNotNull(initialSetup);
	}

}