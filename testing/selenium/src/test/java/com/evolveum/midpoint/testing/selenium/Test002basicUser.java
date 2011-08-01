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

public class Test002basicUser {

	static Selenium se;
	static String baseUrl = "http://localhost:8080/idm";

	private static final transient Trace logger = TraceManager.getTrace(Test002basicUser.class);

	@BeforeClass
	/***
	 * Do login as Admin for each test
	 */
	static public void start() {
		logger.info("--- START ------------------------------");
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
	@AfterClass
	static public void stop() {
		se.stop();
		logger.info("--- STOP ------------------------------");
	}

	// Based on MID-2 jira scenarios
	
	/***
	 * Search user via debug pages
	 * 
	 * Actions:
	 * 		1. login as admin
	 * 		2. click to Accounts
	 * 		3. click to create new user
	 * 		4. fill user create form
	 * 			a) minimal requirements
	 * 			b) fill all fields
	 * 			c) fill all except one reqiured field
	 * 			d) fill passwords fields with two different values
	 */
	
	@Test
	public void test010addUserMinimal() {

		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));

		se.click(se.findLink("leftCreate"));
		se.waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", se.getLocation());

		logger.info("Minimal requirements");
		se.type("createUserForm:name", "barbossa");
		se.type("createUserForm:givenName", "Hector");
		se.type("createUserForm:familyName", "Barbossa");
		se.type("createUserForm:fullName", "Hector Barbossa");
		se.type("createUserForm:email", "");
		se.type("createUserForm:locality", "");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe123.Q");
		se.click("createUserForm:enabled");
		se.click("createUserForm:webAccessEnabled"); // disable
		se.click("createUserForm:createUser"); // enable
		se.waitForText("User created successfully");
		assertTrue(se.isTextPresent("Hector Barbossa"));

		
	}
		
	@Test
	public void test011addUserAllFilled() {
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		
		se.click(se.findLink("leftCreate"));
		se.waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", se.getLocation());
		
		logger.info("All fields filled");
		se.type("createUserForm:name", "elizabeth");
		se.type("createUserForm:givenName", "Elizabeth");
		se.type("createUserForm:familyName", "Swann");
		se.type("createUserForm:fullName", "Elizabeth Swann");
		se.type("createUserForm:email", "elizabeth@blackpearl.pir");
		se.type("createUserForm:locality", "Empress");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe123.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("User created successfully");
		assertTrue(se.isTextPresent("Elizabeth Swann"));
	}
	
	@Test
	public void test012addUserSecondInsert() {
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));

		se.click(se.findLink("leftCreate"));
		se.waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", se.getLocation());
		
		logger.info("try to insert twice");
		se.type("createUserForm:name", "elizabeth");
		se.type("createUserForm:givenName", "Elizabeth");
		se.type("createUserForm:familyName", "Swann");
		se.type("createUserForm:fullName", "Elizabeth Swann Turner");
		se.type("createUserForm:email", "elizabeth@empress.pir");
		se.type("createUserForm:locality", "Empress");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe123.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Failed to create user");
		assertTrue(se.isTextPresent("Couldn't add object 'elizabeth' to model."));
		assertTrue(se.isTextPresent("Failed to create user"));

	}
	
	@Test
	public void test013addUserWrongFilling() {
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));

		se.click(se.findLink("leftCreate"));
		se.waitForText("Web access enabled");
		assertEquals(baseUrl + "/account/userCreate.iface", se.getLocation());

		// test missing name and password not match
		logger.info("missing: name and not matching passsword");
		se.type("createUserForm:name", "");
		se.type("createUserForm:givenName", "Joshamee");
		se.type("createUserForm:familyName", "Gibbs");
		se.type("createUserForm:fullName", "Joshamee Gibbs");
		se.type("createUserForm:email", "elizabeth@Swann.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe213.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");
		assertTrue(se.isTextPresent("Please check password fields."));
		assertTrue(se.isTextPresent("Passwords doesn't match"));

		logger.info("missing: password");
		se.type("createUserForm:name", "Joshamee");
		se.type("createUserForm:givenName", "Joshamee");
		se.type("createUserForm:familyName", "Gibbs");
		se.type("createUserForm:fullName", "Joshamee Gibbs");
		se.type("createUserForm:email", "elizabeth@Swann.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "");
		se.type("createUserForm:password2", "");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");

		logger.info("missing: givenname");
		se.type("createUserForm:name", "Joshamee");
		se.type("createUserForm:givenName", "");
		se.type("createUserForm:familyName", "Gibbs");
		se.type("createUserForm:fullName", "Joshamee Gibbs");
		se.type("createUserForm:email", "elizabeth@Swann.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe213.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");

		logger.info("missing: familyname");
		se.type("createUserForm:name", "Joshamee");
		se.type("createUserForm:givenName", "Joshamee");
		se.type("createUserForm:familyName", "");
		se.type("createUserForm:fullName", "Joshamee Gibbs");
		se.type("createUserForm:email", "elizabeth@Swann.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe213.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");

		logger.info("missing: fullname");
		se.type("createUserForm:name", "Joshamee");
		se.type("createUserForm:givenName", "Joshamee");
		se.type("createUserForm:familyName", "Gibbs");
		se.type("createUserForm:fullName", "");
		se.type("createUserForm:email", "elizabeth@Swann.com");
		se.type("createUserForm:locality", "nowhere");
		se.type("createUserForm:password1", "qwe123.Q");
		se.type("createUserForm:password2", "qwe213.Q");
		se.click("createUserForm:webAccessEnabled");
		se.click("createUserForm:createUser");
		se.waitForText("Value is required");
	}

	/***
	 * Search user via debug pages
	 * 
	 * Actions:
	 * 		1. login as admin
	 * 		2. click to Accounts
	 * 		3. fill search name elizabeth
	 * 		4. click search button
	 * 		5. check if barbossa is not there
	 * 		6. check if elizabeth is there
	 * 		7. fill search name barbossa
	 * 		8. click search button
	 * 		9. check if barbossa is there
	 * 		10. check if elizabeth is not there
	 *  
	 */
	@Test
	public void test020searchUser() throws InterruptedException {
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

		assertTrue(se.isTextPresent("Elizabeth Swann"));
		assertTrue(se.isTextPresent("Hector Barbossa"));

		se.type("admin-content:searchName", "elizabeth");
		se.click("admin-content:searchButton");
		se.waitForText("List Users");
		assertTrue(se.isTextPresent("Elizabeth Swann"));
		assertFalse(se.isTextPresent("Hector Barbossa"));

		se.type("admin-content:searchName", "barbossa");
		se.click("admin-content:searchButton");
		se.waitForText("List Users");

		se.type("admin-content:searchName", "");
		se.click("admin-content:searchButton");
		se.waitForText("List Users");
		assertTrue(se.isTextPresent("Elizabeth Swann"));
		assertTrue(se.isTextPresent("Hector Barbossa"));
	}

	/***
	 * Modify user via debug pages
	 * 
	 * Actions:
	 * 		1. login as admin
	 * 		2. click to Configuration
	 * 		3. click to Import objects
	 * 		4. fill editor with proper XML (new user jack)
	 * 		5. click Accounts
	 * 		6. check if user is there
	 */
	
	@Test
	public void test030importUser() {
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Import And Export"));
		se.click(se.findLink("leftImport"));

		String xmlUser = "<?xml version= '1.0' encoding='UTF-8'?>\n"
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
				+ "            <i:cleartextPassword>Gibbsmentellnotales</i:cleartextPassword>\n"
				+ "</i:password>\n" + "</i:credentials>\n" + "</i:user>";

		se.type("importForm:editor", xmlUser);
		se.click("importForm:uploadButton");
		assertTrue(se.waitForText("Successfully uploaded object 'jack'."));  
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		assertTrue(se.isTextPresent("Cpt. Jack Sparrow")); 
	}

	/***
	 * Modify user via debug pages
	 * 
	 * Actions:
	 * 		1. login as admin
	 * 		2. click to Configuration
	 * 		3. click to Import objects
	 * 		4. fill editor with proper modified XML (user jack)
	 * 		5. click to Upload object
	 * 			a) overwrite is not selected -> FAIL
	 * 			b) overwrite is selected -> PASS
	 * 		6. click Accounts
	 * 		7. check if user full name is changed
	 */
	@Test
	public void test040importModifyUser() {
		
		// failing import allready exists
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Import And Export"));
		se.click(se.findLink("leftImport"));

		String xmlUser = "<?xml version= '1.0' encoding='UTF-8'?>\n"
				+ "<i:user oid=\"c0c010c0-d34d-b33f-f00d-111111111111\" xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n"
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
		assertTrue(se.waitForText("Object 'jack', oid"));
		assertTrue(se.isTextPresent("already exists"));
		
		//overwrite enabled
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Import And Export"));
		se.click(se.findLink("leftImport"));

		se.type("importForm:editor", xmlUser);
		se.click("importForm:enableOverwrite");
		se.click("importForm:uploadButton");
		assertTrue(se.waitForText("Successfully uploaded object 'jack'."));
		
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		assertTrue(se.isTextPresent("Com. Jack Sparrow")); 
		
	}
	/***
	 * modify user via GUI
	 * 
	 * 	1.	login as admin
	 * 	2.	click to Accounts
	 * 	3. 	select and click user jack
	 * 	4.	click to edit button
	 * 	5.	change full name
	 * 	6.	change locality
	 * 	7.	click to save changes
	 * 	8. 	check if user fullname is changed
	 * 	9.	click to user
	 * 	10.	check if locality is changed
	 */
	
	@Test
	public void test050modifyUser() {
		//modify jack (demote)
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
		assertTrue(se.isTextPresent("Black Pearl"));
		se.click("admin-content:editButton");
		se.waitForText("Save changes",30);
		
		for (String s: se.getAllFields() ) {
			if (s.contains("fullNameText")) {
				se.type(s, "SR. Jack Sparrow");
			}
			if (s.contains("localityText")) {
				se.type(s, "Queen Anne's Revenge");
			}
		}
		
		se.click("admin-content:saveButton");
		se.waitForPageToLoad("30000");
		se.waitForText("Changes saved successfully"); 
		assertTrue(se.isTextPresent("Changes saved successfully"));
		assertTrue(se.isTextPresent("SR. Jack Sparrow"));
		
		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			h.put(se.getText(l), l.replace("name", ""));
		}
		se.click(h.get("jack")+"name");
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/userDetails.iface", se.getLocation());
		assertTrue(se.isTextPresent("Queen Anne"));
		
	}
	/***
	 * modify user via GUI
	 * 
	 * 	1.	login as admin
	 * 	2.	click to Accounts
	 * 	3. 	select and click user jack
	 * 	4.	click to edit button
	 * 	5.	change full name
	 * 	6.	change locality
	 * 	7.	click to save changes
	 * 	8. 	check if user fullname is changed
	 * 	9.	click to user
	 * 	10.	check if locality is changed
	 */
	
	@Test
	public void test051modifyUserSpecialChars() {
		//modify jack (demote)
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
		assertTrue(se.isTextPresent("Queen Anne's Revenge"));
		se.click("admin-content:editButton");
		se.waitForText("Save changes",30);
		
		for (String s: se.getAllFields() ) {
			if (s.contains("localityText")) {
				se.type(s, "áčďéěíľĺňôóŕřšťúýžÁČĎÉĚÍĽĹŇÓŔŘŠŤÚÝŽõöüĐŐőŮůŰű");
			}
		}
		
		se.click("admin-content:saveButton");
		se.waitForPageToLoad("30000");
		se.waitForText("Changes saved successfully"); 
		assertTrue(se.isTextPresent("Changes saved successfully"));
		
		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			h.put(se.getText(l), l.replace("name", ""));
		}
		se.click(h.get("jack")+"name");
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/userDetails.iface", se.getLocation());
		assertTrue(se.isTextPresent("áčďéěíľĺňôóŕřšťúýžÁČĎÉĚÍĽĹŇÓŔŘŠŤÚÝŽõöüĐŐőŮůŰű"));	
	}
	
	
	//TODO
	
	@Test
	public void test060modifyUserViaDebug() {
		// failing import allready exists
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Debugging"));
		se.click(se.findLink("leftViewEdit"));
		assertTrue(se.waitForText("View/Edit Object"));
		
	}
	
		
}
