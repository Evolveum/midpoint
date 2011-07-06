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

import java.util.HashMap;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;

import org.junit.*;

import com.evolveum.midpoint.testing.Selenium;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

import static org.junit.Assert.*;

public class Test010simpleProvisioning {
	Selenium se;
	static String baseUrl = "http://localhost:8080/idm";

	private static final transient Trace logger = TraceManager.getTrace(Test010simpleProvisioning.class);

	@Before
	/***
	 * Do login as Admin for each test
	 */
	public void start() {

		WebDriver driver = new FirefoxDriver();
		// WebDriver driver = new ChromeDriver();
		se = new Selenium(driver, baseUrl);
		se.setBrowserLogLevel("5");

		se.open("/");
		se.waitForText("Login", 10);

		se.type("loginForm:userName", "administrator");
		se.type("loginForm:password", "secret");
		se.click("loginForm:loginButton");
		se.waitForText("Administrator", 10);

		assertEquals(baseUrl + "/index.iface", se.getLocation());

	}

	/*
	 * close browser
	 */
	@After
	public void stop() {
		se.stop();

	}
	
	@Test
	public void test001addUserToLDAP() {
		//modify jack (
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		// get hashmap and login
		HashMap<String, String> h = new HashMap<String, String>();
		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			h.put(se.getText(l), l.replace("name", ""));
		}
	
		se.click(h.get("jack")+"name");
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/userDetails.iface", se.getLocation());
		assertTrue(se.isTextPresent("Oid"));
		se.click("admin-content:editButton");
		se.waitForText("Save changes",30); //button is present
		se.click("admin-content:icePnlTbSet:0.1");	//tab accounts
		
		
		
		
	}
}
