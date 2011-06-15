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

import static org.junit.Assert.*;

public class Test002basicUser {

	WebDriverBackedSelenium selenium;
	static String baseUrl = "http://localhost:8080/idm";

	@Before
	public void start() {
		WebDriver driver = new FirefoxDriver();
		selenium = new WebDriverBackedSelenium(driver, baseUrl);
	}

	@After
	public void stop() {
		selenium.stop();
	}

	private void login() {
		selenium.open("/");
		// selenium.waitForPageToLoad("30000");
		waitForText("Login");
		selenium.type("loginForm:userName", "administrator");
		selenium.type("loginForm:password", "secret");
		selenium.click("loginForm:loginButton");
		waitForText("Welcome to midPoint");
		
		assertEquals(baseUrl + "/index.iface", selenium.getLocation());
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
		for (int i = 0; i < 60; i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			if (selenium.isTextPresent(text))
				break;
		}
		assertTrue(selenium.isTextPresent(text));
	}
	
	// Based on MID-2 jira scenarios
	@Test
	public void addUserTest() throws InterruptedException {

		login();
		selenium.click(findNextLink("topAccount"));
		selenium.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", selenium.getLocation());
		assertTrue(selenium.isTextPresent("New User"));
		selenium.click(findNextLink("leftCreate"));
		waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", selenium.getLocation());
		
		System.out.println(Arrays.asList(selenium.getAllFields()));

	}
/*
	@Test
	public void deleteUserTest() {
		//login();
		//String a[] = selenium.getAllLinks();
		//System.out.println(selenium.getAllLinks());
	}
*/
}
