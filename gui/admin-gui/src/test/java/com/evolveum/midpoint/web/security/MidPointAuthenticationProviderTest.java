package com.evolveum.midpoint.web.security;

import static junit.framework.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

	@Test(expected = BadCredentialsException.class)
	public void nullUsername() {
		Authentication authentication = new UsernamePasswordAuthenticationToken(null, "qwe123");
		provider.authenticate(authentication);
	}

	@Test
	public void emptyUsername() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("", "qwe123");
		provider.authenticate(authentication);
	}

	@Test
	public void nullPassword() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("administrator", null);
		provider.authenticate(authentication);
	}

	@Test
	public void emptyPassword() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("administrator", "");
		provider.authenticate(authentication);
	}

	@Test
	public void negativeLoginTimeout() {
		provider.setLoginTimeout(-10);

	}

	@Test
	public void negativeMaxFailedLogins() {

	}

	@Test
	public void nonExistingUser() {

	}
}
