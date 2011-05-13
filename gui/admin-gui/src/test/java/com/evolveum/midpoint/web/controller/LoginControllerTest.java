package com.evolveum.midpoint.web.controller;

import static junit.framework.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/application-context-webapp.xml",
		"file:src/main/webapp/WEB-INF/application-context-security.xml",
		"classpath:applicationContext-test.xml" })
public class LoginControllerTest {

	@Autowired(required = true)
	LoginController controller;
	
	@Before
	public void before() {
		assertNotNull(controller);
	}
	
	@Ignore
	@Test
	public void nullUsername() {
		controller.setUserName(null);
		controller.setPassword("qwe123");
		//TODO: remove ignore on test and fix it
		assertNull(controller.login());
	}
}
