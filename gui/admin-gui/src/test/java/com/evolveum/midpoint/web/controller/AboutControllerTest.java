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

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.web.controller.AboutController.SystemItem;

import static junit.framework.Assert.*;

/**
 * 
 * @author lazyman
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/application-context-webapp.xml",
		"file:src/main/webapp/WEB-INF/application-context-init.xml",
		"file:src/main/webapp/WEB-INF/application-context-security.xml",
		"classpath:application-context-test.xml" })
public class AboutControllerTest {

	@Autowired(required = true)
	AboutController controller;

	@Before
	public void before() {
		assertNotNull(controller);
	}

	@Test
	public void getItems() {
		List<SystemItem> items = controller.getItems();
		assertNotNull(items);
		assertEquals(11, items.size());

		for (SystemItem item : items) {
			assertNotNull(item);
			assertNotNull(item.getProperty());
			assertNotNull(item.getValue());

			assertFalse("".equals(item.getProperty()));
			assertFalse("".equals(item.getValue()));
		}
	}
}
