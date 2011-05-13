package com.evolveum.midpoint.web.security;

import static junit.framework.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/application-context-webapp.xml",
		"file:src/main/webapp/WEB-INF/application-context-security.xml",
		"classpath:applicationContext-test.xml" })
public class MidPointAuthenticationProviderTest {

	@Autowired(required = true)
	MidPointAuthenticationProvider provider;

	@Before
	public void before() {
		assertNotNull(provider);
	}

	@Test
	public void negativeLoginTimeout() {
		
	}

	@Test
	public void negativeMaxFailedLogins() {

	}
}
