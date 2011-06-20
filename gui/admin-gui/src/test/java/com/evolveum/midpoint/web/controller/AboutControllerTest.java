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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/application-context-webapp.xml",
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
