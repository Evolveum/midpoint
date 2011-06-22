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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.WebDriver;

import org.junit.*;

import com.evolveum.midpoint.testing.Selenium;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

import static org.junit.Assert.*;

public class Test002basicUser {

	Selenium se;
	static String baseUrl = "http://localhost:8080/idm";

	private static final transient Trace logger = TraceManager.getTrace(Test002basicUser.class);

	@Before
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

	@After
	public void stop() {
		se.stop();

	}

	// Based on MID-2 jira scenarios
	@Test
	public void test01addUser() {

		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));

		se.click(se.findLink("leftCreate"));
		se.waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", se.getLocation());

		logger.info("Minimal requirements");
		se.type("createUserForm:name", "selena");
		se.type("createUserForm:givenName", "selena");
		se.type("createUserForm:familyName", "wilson");
		se.type("createUserForm:fullName", "Selena Wilson");
		se.type("createUserForm:email", "");
		se.type("createUserForm:locality", "");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe123.Q");
		se.click("createUserForm:enabled");
		se.click("createUserForm:webAccessEnabled"); // disable
		se.click("createUserForm:createUser"); // enable
		se.waitForText("User created successfully");
		assertTrue(se.isTextPresent("Selena Wilson"));

		se.click(se.findLink("leftCreate"));
		se.waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", se.getLocation());

		logger.info("All fields filled");
		se.type("createUserForm:name", "leila");
		se.type("createUserForm:givenName", "Leila");
		se.type("createUserForm:familyName", "Walker");
		se.type("createUserForm:fullName", "Leila Walker");
		se.type("createUserForm:email", "leila@walker.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe123.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("User created successfully");
		assertTrue(se.isTextPresent("Leila Walker"));

		se.click(se.findLink("leftCreate"));
		se.waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", se.getLocation());

		logger.info("try to insert twice");
		se.type("createUserForm:name", "leila");
		se.type("createUserForm:givenName", "Leila");
		se.type("createUserForm:familyName", "Walker");
		se.type("createUserForm:fullName", "Leila Walker");
		se.type("createUserForm:email", "leila@walker.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe123.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Failed to create user");
		assertTrue(se.isTextPresent("could not insert"));
		assertTrue(se.isTextPresent("ConstraintViolationException"));

		// test missing name and password not match
		logger.info("missing: name");
		se.type("createUserForm:name", "");
		se.type("createUserForm:givenName", "Joe");
		se.type("createUserForm:familyName", "Dead");
		se.type("createUserForm:fullName", "Joe Dead");
		se.type("createUserForm:email", "leila@walker.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe213.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");
		assertTrue(se.isTextPresent("Please check password fields."));
		assertTrue(se.isTextPresent("Passwords doesn't match"));

		logger.info("missing: password");
		se.type("createUserForm:name", "joe");
		se.type("createUserForm:givenName", "Joe");
		se.type("createUserForm:familyName", "Dead");
		se.type("createUserForm:fullName", "Joe Dead");
		se.type("createUserForm:email", "leila@walker.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "");
		se.type("createUserForm:password2", "");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");

		logger.info("missing: givenname");
		se.type("createUserForm:name", "joe");
		se.type("createUserForm:givenName", "");
		se.type("createUserForm:familyName", "Dead");
		se.type("createUserForm:fullName", "Joe Dead");
		se.type("createUserForm:email", "leila@walker.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe213.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");

		logger.info("missing: familyname");
		se.type("createUserForm:name", "joe");
		se.type("createUserForm:givenName", "Joe");
		se.type("createUserForm:familyName", "");
		se.type("createUserForm:fullName", "Joe Dead");
		se.type("createUserForm:email", "leila@walker.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe213.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");

		logger.info("missing: fullname");
		se.type("createUserForm:name", "joe");
		se.type("createUserForm:givenName", "Joe");
		se.type("createUserForm:familyName", "Dead");
		se.type("createUserForm:fullName", "");
		se.type("createUserForm:email", "leila@walker.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe213.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");
	}

	@Test
	public void test02searchUser() throws InterruptedException {
		logger.info("searchTest()");

		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));

		// get hashmap and login
		HashMap<String, String> h = new HashMap<String, String>();
		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			h.put(se.getText(l), l.replace(":name", ""));
		}

		for (String k : h.keySet()) {
			logger.info(k + " -> " + h.get(k));
		}

		assertTrue(se.isTextPresent("Leila Walker"));
		assertTrue(se.isTextPresent("Selena Wilson"));

		se.type("admin-content:searchName", "leila");
		se.click("admin-content:searchButton");
		se.waitForText("List Users");
		assertTrue(se.isTextPresent("Leila Walker"));
		assertFalse(se.isTextPresent("Selena Wilson"));

		se.type("admin-content:searchName", "selena");
		se.click("admin-content:searchButton");
		se.waitForText("List Users");

		se.type("admin-content:searchName", "");
		se.click("admin-content:searchButton");
		se.waitForText("List Users");
		assertTrue(se.isTextPresent("Leila Walker"));
		assertTrue(se.isTextPresent("Selena Wilson"));
	}

	@Test
	public void test03importUser() {
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Import And Export"));
		se.click(se.findLink("leftImport"));

		String xmlUser = " <?xml version= '1.0' encoding='UTF-8'?>"
				+ "<i:user oid='c0c010c0-d34d-b33f-f00d-111111111111' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
				+ "xmlns:i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'\n"
				+ "xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'\n"
				+ "xmlns:piracy='http://midpoint.evolveum.com/xml/ns/samples/piracy'>\n"
				+ "<c:name>jack</c:name>\n" + "<c:extension>\n" + "<piracy:ship>Black Pearl</piracy:ship>\n"
				+ "</c:extension>\n" + "<i:fullName>Cpt. Jack Sparrow</i:fullName>\n"
				+ "<i:givenName>Jack</i:givenName>\n" + "<i:familyName>Sparrow</i:familyName>\n"
				+ "<i:additionalNames>yet another name</i:additionalNames>\n"
				+ "<i:honorificPrefix>Cpt.</i:honorificPrefix>\n"
				+ "    <i:honorificSuffix>PhD.</i:honorificSuffix>\n"
				+ "<i:eMailAddress>jack.sparrow@evolveum.com</i:eMailAddress>\n"
				+ "<i:telephoneNumber>555-1234</i:telephoneNumber>\n"
				+ "<i:employeeNumber>emp1234</i:employeeNumber>\n"
				+ "<i:employeeType>CAPTAIN</i:employeeType>\n"
				+ "<i:organizationalUnit>Leaders</i:organizationalUnit>\n"
				+ "<i:locality>Black Pearl</i:locality>\n" + "<i:credentials>\n" + "<i:password>\n"
				+ "            <i:cleartextPassword>deadmentellnotales</i:cleartextPassword>\n"
				+ "</i:password>\n" + "</i:credentials>\n" + "</i:user>";

		se.type("importForm:editor", xmlUser);
		se.click("importForm:uploadButton");
		assertTrue(se.waitForText("Added object: jack"));
		
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		assertTrue(se.isTextPresent("Cpt. Jack Sparrow")); 
	}

	
	@Test
	public void test04importModifyUser() {
		
		// failing import allready exists
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Import And Export"));
		se.click(se.findLink("leftImport"));

		String xmlUser = " <?xml version= '1.0' encoding='UTF-8'?>"
				+ "<i:user oid='c0c010c0-d34d-b33f-f00d-111111111111' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
				+ "xmlns:i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'\n"
				+ "xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'\n"
				+ "xmlns:piracy='http://midpoint.evolveum.com/xml/ns/samples/piracy'>\n"
				+ "<c:name>jack</c:name>\n" + "<c:extension>\n" + "<piracy:ship>Black Pearl</piracy:ship>\n"
				+ "</c:extension>\n" + "<i:fullName>Com. Jack Sparrow</i:fullName>\n"
				+ "<i:givenName>Jack</i:givenName>\n" + "<i:familyName>Sparrow</i:familyName>\n"
				+ "<i:additionalNames>yet another name</i:additionalNames>\n"
				+ "<i:honorificPrefix>Cpt.</i:honorificPrefix>\n"
				+ "    <i:honorificSuffix>PhD.</i:honorificSuffix>\n"
				+ "<i:eMailAddress>jack.sparrow@evolveum.com</i:eMailAddress>\n"
				+ "<i:telephoneNumber>555-1234</i:telephoneNumber>\n"
				+ "<i:employeeNumber>emp1234</i:employeeNumber>\n"
				+ "<i:employeeType>CAPTAIN</i:employeeType>\n"
				+ "<i:organizationalUnit>Leaders</i:organizationalUnit>\n"
				+ "<i:locality>Black Pearl</i:locality>\n" + "<i:credentials>\n" + "<i:password>\n"
				+ "            <i:cleartextPassword>deadmentellnotales</i:cleartextPassword>\n"
				+ "</i:password>\n" + "</i:credentials>\n" + "</i:user>";

		se.type("importForm:editor", xmlUser);
		se.click("importForm:uploadButton");
		assertTrue(se.waitForText("Failed to add object jack"));
		assertTrue(se.isTextPresent("already exists in store"));
		
		//overwrite enabled
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Import And Export"));
		se.click(se.findLink("leftImport"));

		se.type("importForm:editor", xmlUser);
		se.click("importForm:enableOverwrite");
		se.click("importForm:uploadButton");
		assertTrue(se.waitForText("Added object: jack"));
		
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		assertTrue(se.isTextPresent("Com. Jack Sparrow")); 
		
	}
	
	
	@Test
	public void test99deleteUser() {
		
		//delete leila
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

		se.click(h.get("leila") + "deleteCheckbox");
		se.click("admin-content:deleteUser");
		se.waitForText("Confirm delete");
		se.click("admin-content:deleteUserNo");
		se.waitForText("List Users");
		se.click("admin-content:deleteUser");
		se.waitForText("Confirm delete");
		se.click("admin-content:deleteUserYes");
		se.waitForText("List Users");
		assertFalse(se.isTextPresent("Leila Walker"));

		//delete selena
		se.click(se.findLink("topHome"));
		se.waitForPageToLoad("30000");

		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");

		assertTrue(se.isTextPresent("New User"));

		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			logger.info("Adding:" + se.getText(l), l.replace("name", ""));
			h.put(se.getText(l), l.replace("name", ""));
		}

		se.click(h.get("selena") + "deleteCheckbox");
		se.click("admin-content:deleteUser");
		se.waitForText("Confirm delete");
		se.click("admin-content:deleteUserYes");
		se.waitForText("List Users");
		assertFalse(se.isTextPresent("Selena")); 
		
		//delete jack
		se.click(se.findLink("topHome"));
		se.waitForPageToLoad("30000");

		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");

		assertTrue(se.isTextPresent("New User"));

		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			logger.info("Adding:" + se.getText(l), l.replace("name", ""));
			h.put(se.getText(l), l.replace("name", ""));
		}

		se.click(h.get("Jack") + "deleteCheckbox");
		se.click("admin-content:deleteUser");
		se.waitForText("Confirm delete");
		se.click("admin-content:deleteUserYes");
		se.waitForText("List Users");
		assertFalse(se.isTextPresent("Jack"));
		
	}
}
