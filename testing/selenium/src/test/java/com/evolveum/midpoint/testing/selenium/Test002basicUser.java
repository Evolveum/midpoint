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
 * Portions Copyrighted 2011 Peter Prochazka
 */

package com.evolveum.midpoint.testing.selenium;

import java.util.Arrays;
import java.util.HashMap;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;

import org.junit.*;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.thoughtworks.selenium.SeleniumException;

import static org.junit.Assert.*;

public class Test002basicUser {

	WebDriverBackedSelenium selenium;
	static String baseUrl = "http://localhost:8080/idm";
	
	private static final transient Trace logger = TraceManager.getTrace(Test002basicUser.class);

	@Before
	public void start() {

		WebDriver driver = new FirefoxDriver();
		selenium = new WebDriverBackedSelenium(driver, baseUrl);
		selenium.setBrowserLogLevel("5");

		selenium.open("/");
		waitForText("Login");

		selenium.type("loginForm:userName", "administrator");
		selenium.type("loginForm:password", "secret");
		selenium.click("loginForm:loginButton");
		waitForText("Administrator");

		assertEquals(baseUrl + "/index.iface", selenium.getLocation());

	}

	@After
	public void stop() {
		selenium.stop();

	}

	private String findNextLink(String part) {
		for (String s : Arrays.asList(selenium.getAllLinks())) {
			if (s.contains(part)) {
				return s;
			}
		}
		return "";
	}

	private void waitForText(String text) {
		for (int i = 0; i < 300; i++) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
			}
			if (selenium.isTextPresent(text)) {
				return;
			}
		}
		assertTrue(selenium.isTextPresent(text));
	}
/*
	// Based on MID-2 jira scenarios
	@Test
	public void test01addUser() {

		selenium.click(findNextLink("topAccount"));
		selenium.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", selenium.getLocation());
		assertTrue(selenium.isTextPresent("New User"));

		selenium.click(findNextLink("leftCreate"));
		waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", selenium.getLocation());

		logger.info("Minimal requirements");
		selenium.type("createUserForm:name", "selena");
		selenium.type("createUserForm:givenName", "selena");
		selenium.type("createUserForm:familyName", "wilson");
		selenium.type("createUserForm:fullName", "Selena Wilson");
		selenium.type("createUserForm:email", "");
		selenium.type("createUserForm:locality", "");
		selenium.type("createUserForm:password1", "qwe123.Q");
		selenium.type("createUserForm:password2", "qwe123.Q");
		selenium.click("createUserForm:enabled");
		selenium.click("createUserForm:webAccessEnabled"); // disable
		selenium.click("createUserForm:createUser"); // enable
		waitForText("User created successfully");
		assertTrue(selenium.isTextPresent("Selena Wilson"));

		selenium.click(findNextLink("leftCreate"));
		waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", selenium.getLocation());

		logger.info("All fields filled");
		selenium.type("createUserForm:name", "leila");
		selenium.type("createUserForm:givenName", "Leila");
		selenium.type("createUserForm:familyName", "Walker");
		selenium.type("createUserForm:fullName", "Leila Walker");
		selenium.type("createUserForm:email", "leila@walker.com");
		selenium.type("createUserForm:locality", "nowhere");
		selenium.type("createUserForm:password1", "qwe123.Q");
		selenium.type("createUserForm:password2", "qwe123.Q");
		selenium.click("createUserForm:webAccessEnabled");
		selenium.click("createUserForm:createUser");
		waitForText("User created successfully");
		assertTrue(selenium.isTextPresent("Leila Walker"));

		selenium.click(findNextLink("leftCreate"));
		waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", selenium.getLocation());

		logger.info("try to insert twice");
		selenium.type("createUserForm:name", "leila");
		selenium.type("createUserForm:givenName", "Leila");
		selenium.type("createUserForm:familyName", "Walker");
		selenium.type("createUserForm:fullName", "Leila Walker");
		selenium.type("createUserForm:email", "leila@walker.com");
		selenium.type("createUserForm:locality", "nowhere");
		selenium.type("createUserForm:password1", "qwe123.Q");
		selenium.type("createUserForm:password2", "qwe123.Q");
		selenium.click("createUserForm:webAccessEnabled");
		selenium.click("createUserForm:createUser");
		waitForText("Failed to create user");
		assertTrue(selenium.isTextPresent("could not insert"));
		assertTrue(selenium.isTextPresent("ConstraintViolationException"));

		// test missing name and password not match
		logger.info("missing: name");
		selenium.type("createUserForm:name", "");
		selenium.type("createUserForm:givenName", "Joe");
		selenium.type("createUserForm:familyName", "Dead");
		selenium.type("createUserForm:fullName", "Joe Dead");
		selenium.type("createUserForm:email", "leila@walker.com");
		selenium.type("createUserForm:locality", "nowhere");
		selenium.type("createUserForm:password1", "qwe123.Q");
		selenium.type("createUserForm:password2", "qwe213.Q");
		selenium.click("createUserForm:webAccessEnabled");
		selenium.click("createUserForm:createUser");
		waitForText("Value is required");
		assertTrue(selenium.isTextPresent("Please check password fields."));
		assertTrue(selenium.isTextPresent("Passwords doesn't match"));

		logger.info("missing: password");
		selenium.type("createUserForm:name", "joe");
		selenium.type("createUserForm:givenName", "Joe");
		selenium.type("createUserForm:familyName", "Dead");
		selenium.type("createUserForm:fullName", "Joe Dead");
		selenium.type("createUserForm:email", "leila@walker.com");
		selenium.type("createUserForm:locality", "nowhere");
		selenium.type("createUserForm:password1", "");
		selenium.type("createUserForm:password2", "");
		selenium.click("createUserForm:webAccessEnabled");
		selenium.click("createUserForm:createUser");
		waitForText("Value is required");

		logger.info("missing: givenname");
		selenium.type("createUserForm:name", "joe");
		selenium.type("createUserForm:givenName", "");
		selenium.type("createUserForm:familyName", "Dead");
		selenium.type("createUserForm:fullName", "Joe Dead");
		selenium.type("createUserForm:email", "leila@walker.com");
		selenium.type("createUserForm:locality", "nowhere");
		selenium.type("createUserForm:password1", "qwe123.Q");
		selenium.type("createUserForm:password2", "qwe213.Q");
		selenium.click("createUserForm:webAccessEnabled");
		selenium.click("createUserForm:createUser");
		waitForText("Value is required");

		logger.info("missing: familyname");
		selenium.type("createUserForm:name", "joe");
		selenium.type("createUserForm:givenName", "Joe");
		selenium.type("createUserForm:familyName", "");
		selenium.type("createUserForm:fullName", "Joe Dead");
		selenium.type("createUserForm:email", "leila@walker.com");
		selenium.type("createUserForm:locality", "nowhere");
		selenium.type("createUserForm:password1", "qwe123.Q");
		selenium.type("createUserForm:password2", "qwe213.Q");
		selenium.click("createUserForm:webAccessEnabled");
		selenium.click("createUserForm:createUser");
		waitForText("Value is required");

		logger.info("missing: fullname");
		selenium.type("createUserForm:name", "joe");
		selenium.type("createUserForm:givenName", "Joe");
		selenium.type("createUserForm:familyName", "Dead");
		selenium.type("createUserForm:fullName", "");
		selenium.type("createUserForm:email", "leila@walker.com");
		selenium.type("createUserForm:locality", "nowhere");
		selenium.type("createUserForm:password1", "qwe123.Q");
		selenium.type("createUserForm:password2", "qwe213.Q");
		selenium.click("createUserForm:webAccessEnabled");
		selenium.click("createUserForm:createUser");
		waitForText("Value is required");
	}
*/
	@Test
	public void test02searchUser() throws InterruptedException {
		logger.info("searchTest()");

		selenium.click(findNextLink("topAccount"));
		selenium.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", selenium.getLocation());
		assertTrue(selenium.isTextPresent("New User"));

		//get hashmap and login
		HashMap<String,String> h = new HashMap<String,String>(); 
		for (String l : selenium.getAllLinks()) {
			if ( ! l.contains("Table") || ! l.contains("name")) continue;
			h.put(selenium.getText(l),l.replace(":name",""));
		}
		
		for (String k: h.keySet()) {
			logger.info(k + " -> " + h.get(k));
		}
	}

	
	// @Test
	// public void deleteUserTest() { 
		 
	// }
	 
}
