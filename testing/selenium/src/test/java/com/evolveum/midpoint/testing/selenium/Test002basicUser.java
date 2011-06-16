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

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;

import org.junit.*;

import com.thoughtworks.selenium.SeleniumException;

import static org.junit.Assert.*;

public class Test002basicUser {

	WebDriverBackedSelenium selenium;
	static String baseUrl = "http://localhost:8080/idm";

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
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}
			if (selenium.isTextPresent(text)) {
				return;
			}
		}
		assertTrue(selenium.isTextPresent(text));
	}

	// Based on MID-2 jira scenarios
	@Test
	public void test01addUser() throws InterruptedException {

		selenium.click(findNextLink("topAccount"));
		selenium.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", selenium.getLocation());
		assertTrue(selenium.isTextPresent("New User"));

		selenium.click(findNextLink("leftCreate"));
		waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", selenium.getLocation());

		// Minimal requirements
		selenium.type("j_idt44:name", "selena");
		selenium.type("j_idt44:givenName", "selena");
		selenium.type("j_idt44:familyName", "wilson");
		selenium.type("j_idt44:fullName", "Selena Wilson");
		selenium.type("j_idt44:email", "");
		selenium.type("j_idt44:locality", "");
		selenium.type("j_idt44:password1", "qwe123.Q");
		selenium.type("j_idt44:password2", "qwe123.Q");
		selenium.click("j_idt44:enabled");
		selenium.click("j_idt44:webAccessEnabled"); // disable
		selenium.click("j_idt44:createUser"); // enable
		waitForText("User created successfully");
		assertTrue(selenium.isTextPresent("Selena Wilson"));

		selenium.click(findNextLink("leftCreate"));
		waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", selenium.getLocation());

		// All fields filled
		selenium.type("j_idt44:name", "leila");
		selenium.type("j_idt44:givenName", "Leila");
		selenium.type("j_idt44:familyName", "Walker");
		selenium.type("j_idt44:fullName", "Leila Walker");
		selenium.type("j_idt44:email", "leila@walker.com");
		selenium.type("j_idt44:locality", "nowhere");
		selenium.type("j_idt44:password1", "qwe123.Q");
		selenium.type("j_idt44:password2", "qwe123.Q");
		selenium.click("j_idt44:webAccessEnabled");
		selenium.click("j_idt44:createUser");
		waitForText("User created successfully");
		assertTrue(selenium.isTextPresent("Leila Walker"));

		selenium.click(findNextLink("leftCreate"));
		waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", selenium.getLocation());

		// try to insert twice
		selenium.type("j_idt44:name", "leila");
		selenium.type("j_idt44:givenName", "Leila");
		selenium.type("j_idt44:familyName", "Walker");
		selenium.type("j_idt44:fullName", "Leila Walker");
		selenium.type("j_idt44:email", "leila@walker.com");
		selenium.type("j_idt44:locality", "nowhere");
		selenium.type("j_idt44:password1", "qwe123.Q");
		selenium.type("j_idt44:password2", "qwe123.Q");
		selenium.click("j_idt44:webAccessEnabled");
		selenium.click("j_idt44:createUser");
		waitForText("Failed to create user");
		assertTrue(selenium.isTextPresent("could not insert"));
		assertTrue(selenium.isTextPresent("ConstraintViolationException"));

		// test missing name and password not match
		selenium.type("j_idt44:name", "");
		selenium.type("j_idt44:givenName", "Joe");
		selenium.type("j_idt44:familyName", "Dead");
		selenium.type("j_idt44:fullName", "Joe Dead");
		selenium.type("j_idt44:email", "leila@walker.com");
		selenium.type("j_idt44:locality", "nowhere");
		selenium.type("j_idt44:password1", "qwe123.Q");
		selenium.type("j_idt44:password2", "qwe213.Q");
		selenium.click("j_idt44:webAccessEnabled");
		selenium.click("j_idt44:createUser");
		waitForText("Value is required");
		assertTrue(selenium.isTextPresent("Please check password fields."));
		assertTrue(selenium.isTextPresent("Passwords doesn't match"));

		selenium.type("j_idt44:name", "joe");
		selenium.type("j_idt44:givenName", "Joe");
		selenium.type("j_idt44:familyName", "Dead");
		selenium.type("j_idt44:fullName", "Joe Dead");
		selenium.type("j_idt44:email", "leila@walker.com");
		selenium.type("j_idt44:locality", "nowhere");
		selenium.type("j_idt44:password1", "");
		selenium.type("j_idt44:password2", "");
		selenium.click("j_idt44:webAccessEnabled");
		selenium.click("j_idt44:createUser");
		waitForText("Value is required");

		selenium.type("j_idt44:name", "joe");
		selenium.type("j_idt44:givenName", "");
		selenium.type("j_idt44:familyName", "Dead");
		selenium.type("j_idt44:fullName", "Joe Dead");
		selenium.type("j_idt44:email", "leila@walker.com");
		selenium.type("j_idt44:locality", "nowhere");
		selenium.type("j_idt44:password1", "qwe123.Q");
		selenium.type("j_idt44:password2", "qwe213.Q");
		selenium.click("j_idt44:webAccessEnabled");
		selenium.click("j_idt44:createUser");
		waitForText("Value is required");

		selenium.type("j_idt44:name", "joe");
		selenium.type("j_idt44:givenName", "Joe");
		selenium.type("j_idt44:familyName", "");
		selenium.type("j_idt44:fullName", "Joe Dead");
		selenium.type("j_idt44:email", "leila@walker.com");
		selenium.type("j_idt44:locality", "nowhere");
		selenium.type("j_idt44:password1", "qwe123.Q");
		selenium.type("j_idt44:password2", "qwe213.Q");
		selenium.click("j_idt44:webAccessEnabled");
		selenium.click("j_idt44:createUser");
		waitForText("Value is required");

		selenium.type("j_idt44:name", "joe");
		selenium.type("j_idt44:givenName", "Joe");
		selenium.type("j_idt44:familyName", "Dead");
		selenium.type("j_idt44:fullName", "");
		selenium.type("j_idt44:email", "leila@walker.com");
		selenium.type("j_idt44:locality", "nowhere");
		selenium.type("j_idt44:password1", "qwe123.Q");
		selenium.type("j_idt44:password2", "qwe213.Q");
		selenium.click("j_idt44:webAccessEnabled");
		selenium.click("j_idt44:createUser");
		waitForText("Value is required");
	}

	@Test
	public void test02searchUser() {
		System.out.println("searchTest()");

		selenium.click(findNextLink("topAccount"));
		selenium.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", selenium.getLocation());
		assertTrue(selenium.isTextPresent("New User"));

		for (String l : selenium.getAllLinks()) {
			System.out.println(l + " -> " + selenium.getValue(l));
			System.out.println(l + " -> " + selenium.getSelectedId(l));
			System.out.println(l + " -> " + selenium.getText(l));
		}
	}

	
	// @Test
	// public void deleteUserTest() { 
		 
	// }
	 
}
