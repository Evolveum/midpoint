package com.evolveum.midpoint.web.security;

import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
		"classpath:application-context-repository-test.xml" })
public class MidPointAuthenticationProviderTest extends AbstractTestNGSpringContextTests  {

	@Autowired(required = true)
	MidPointAuthenticationProvider provider;

	@BeforeMethod
	public void before() {
		assertNotNull(provider);
	}

	@Test(expectedExceptions = BadCredentialsException.class)
	public void nullUsername() {
		try {
			Authentication authentication = new UsernamePasswordAuthenticationToken(null, "qwe123");
			provider.authenticate(authentication);
		} catch (BadCredentialsException ex) {
			assertEquals("web.security.provider.invalid", ex.getMessage());
			throw ex;
		}
	}

	@Test(expectedExceptions = BadCredentialsException.class)
	public void emptyUsername() {
		try {
			Authentication authentication = new UsernamePasswordAuthenticationToken("", "qwe123");
			provider.authenticate(authentication);
		} catch (BadCredentialsException ex) {
			assertEquals("web.security.provider.invalid", ex.getMessage());
			throw ex;
		}
	}

	@Test(expectedExceptions = BadCredentialsException.class)
	public void nullPassword() {
		try {
			Authentication authentication = new UsernamePasswordAuthenticationToken("administrator", null);
			provider.authenticate(authentication);
		} catch (BadCredentialsException ex) {
			assertEquals("web.security.provider.invalid", ex.getMessage());
			throw ex;
		}
	}

	@Test(expectedExceptions = BadCredentialsException.class)
	public void emptyPassword() {
		try {
			Authentication authentication = new UsernamePasswordAuthenticationToken("administrator", "");
			provider.authenticate(authentication);
		} catch (BadCredentialsException ex) {
			assertEquals("web.security.provider.invalid", ex.getMessage());
			throw ex;
		}
	}

	@Test(expectedExceptions = BadCredentialsException.class)
	public void nonExistingUser() {
		final String username = "administrator";
		try {
			UserDetailsService service = Mockito.mock(UserDetailsService.class);
			when(service.getUser(username)).thenReturn(null);
			provider.userManagerService = service;

			Authentication authentication = new UsernamePasswordAuthenticationToken(username, "qwe123");
			provider.authenticate(authentication);
		} catch (BadCredentialsException ex) {
			assertEquals("web.security.provider.invalid", ex.getMessage());
			throw ex;
		}
	}

	@Test(expectedExceptions = BadCredentialsException.class)
	public void negativeLoginTimeout() {
		provider.setLoginTimeout(-10);
		provider.setMaxFailedLogins(1);

		final String username = "administrator";
		final PrincipalUser user = new PrincipalUser("1", username, true);
		Credentials credentials = user.getCredentials();
		credentials.setPassword("asdf", "base64");
		try {
			UserDetailsService service = Mockito.mock(UserDetailsService.class);
			when(service.getUser(username)).thenReturn(user);
			provider.userManagerService = service;

			Authentication authentication = new UsernamePasswordAuthenticationToken(username, "qwe123");
			provider.authenticate(authentication);
			provider.authenticate(authentication);
		} catch (BadCredentialsException ex) {
			assertEquals("web.security.provider.invalid", ex.getMessage());
			throw ex;
		}
	}

	@Test(expectedExceptions = BadCredentialsException.class)
	public void positiveLoginTimeout() {
		provider.setLoginTimeout(5);
		provider.setMaxFailedLogins(1);

		final String username = "administrator";
		final PrincipalUser user = new PrincipalUser("1", username, true);
		Credentials credentials = user.getCredentials();
		credentials.setPassword("asdf", "base64");
		try {
			UserDetailsService service = Mockito.mock(UserDetailsService.class);
			when(service.getUser(username)).thenReturn(user);
			provider.userManagerService = service;

			Authentication authentication = new UsernamePasswordAuthenticationToken(username, "qwe123");
			doAuthenticate(authentication, 1);
			provider.authenticate(authentication);
		} catch (BadCredentialsException ex) {
			assertEquals("web.security.provider.locked", ex.getMessage());
			assertNotNull(ex.getExtraInformation());
			assertTrue(4L <= (Long) ((Object[]) ex.getExtraInformation())[0]);

			throw ex;
		}
	}

	@Test(expectedExceptions = BadCredentialsException.class)
	public void negativeMaxLogins() {
		provider.setLoginTimeout(5);
		provider.setMaxFailedLogins(-3);

		final String username = "administrator";
		final PrincipalUser user = new PrincipalUser("1", username, true);
		Credentials credentials = user.getCredentials();
		credentials.setPassword("asdf", "base64");
		try {
			UserDetailsService service = Mockito.mock(UserDetailsService.class);
			when(service.getUser(username)).thenReturn(user);
			provider.userManagerService = service;

			Authentication authentication = new UsernamePasswordAuthenticationToken(username, "qwe123");
			doAuthenticate(authentication, 3);
			provider.authenticate(authentication);
		} catch (BadCredentialsException ex) {
			assertEquals("web.security.provider.invalid", ex.getMessage());

			throw ex;
		}
	}

	@Test(expectedExceptions = BadCredentialsException.class)
	public void positiveMaxLogins() {
		provider.setLoginTimeout(5);
		provider.setMaxFailedLogins(3);

		final String username = "administrator";
		final PrincipalUser user = new PrincipalUser("1", username, true);
		Credentials credentials = user.getCredentials();
		credentials.setPassword("asdf", "base64");
		try {
			UserDetailsService service = Mockito.mock(UserDetailsService.class);
			when(service.getUser(username)).thenReturn(user);
			provider.userManagerService = service;

			Authentication authentication = new UsernamePasswordAuthenticationToken(username, "qwe123");
			doAuthenticate(authentication, 3);
			provider.authenticate(authentication);
		} catch (BadCredentialsException ex) {
			assertEquals("web.security.provider.locked", ex.getMessage());
			assertNotNull(ex.getExtraInformation());
			assertTrue(4L <= (Long) ((Object[]) ex.getExtraInformation())[0]);

			throw ex;
		}
	}

	private void doAuthenticate(Authentication authentication, int times) {
		for (int i = 0; i < times; i++) {
			try {
				provider.authenticate(authentication);
			} catch (BadCredentialsException ex) {
				// we can ignore it here
			}
		}
	}
}
