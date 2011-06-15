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

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;

import org.junit.*;
import static org.junit.Assert.*;

public class Test001loginPage {

	WebDriverBackedSelenium selenium;
	static String baseUrl="http://localhost:8080/idm";
	
	@Before
	public void start() {
		WebDriver driver = new FirefoxDriver();
		selenium = new WebDriverBackedSelenium(driver, baseUrl);
	}

	@Test
	public void positiveLoginTest() {

		
		selenium.open("/");
		selenium.waitForPageToLoad("30000");
		
		selenium.type("loginForm:userName", "administrator");
		selenium.type("loginForm:password", "secret");
		selenium.click("loginForm:loginButton");
		//selenium.click("css=span.regular");
		selenium.waitForPageToLoad("30000");
		assertEquals(baseUrl+"/index.iface", selenium.getLocation());
	}
	
	@Test
	public void negativeLoginTest() {

		selenium.open("/");
		selenium.waitForPageToLoad("30000");
		//Test invalid user name
		selenium.type("loginForm:userName", "administrator");
		selenium.type("loginForm:password", "secreta");
		selenium.click("loginForm:loginButton");
		//selenium.click("css=span.regular");
		selenium.waitForPageToLoad("30000");
		assertNotSame(baseUrl+"idm/index.iface", selenium.getLocation());
		assertTrue(selenium.isTextPresent("Invalid username and/or password."));
		//test empty form
		selenium.type("loginForm:userName", "");
		selenium.type("loginForm:password", "");
		selenium.click("loginForm:loginButton");
		//selenium.click("css=span.regular");
		selenium.waitForPageToLoad("30000");
		assertNotSame(baseUrl+"idm/index.iface", selenium.getLocation());
		assertTrue(selenium.isTextPresent("Value is required."));
	}
	@After
	public void stop() {
		selenium.stop();
	}
}
