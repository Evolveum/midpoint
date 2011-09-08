package com.evolveum.midpoint.testing.selenium;

import java.util.HashMap;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;

import org.junit.*;

import com.evolveum.midpoint.testing.Selenium;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

import static org.junit.Assert.*;

public class Test999deleteUsers {

	Selenium se;
	static String baseUrl = "http://localhost:8080/idm";

	private static final transient Trace LOGGER = TraceManager.getTrace(Test999deleteUsers.class);

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

	/***
	 * Search user and delete it form  midPoint
	 * 
	 * Actions:
	 * 		1. login as admin
	 * 		2. click to configuration
	 * 		3. click to List Objects
	 * 		4. select from list User
	 *		5. click list button
	 *		6. click to delete button near user jack
	 *		7. do not delete (click NO)
	 *		8. click to delete button near user barbossa
	 *		9. delete it (click YES)
	 *		10. validate if user barbossa is removed
	*/
	@Test
	public void test01deleteUserViaDebug() {
		// failing import allready exists
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Debugging"));
		se.click(se.findLink("leftList"));
		assertTrue(se.waitForText("List Objects"));
		se.select("debugListForm:selectOneMenuList", "label=User");
		se.click("debugListForm:listObjectsButton");
		
		//validate if both user are there
		assertTrue(se.isTextPresent("barbossa"));
		
		// get hashmap and login
		HashMap<String, String> h = new HashMap<String, String>();
		for (String l : se.getAllLinks()) {
			if (!l.contains("debugListForm:link"))
				continue;
			h.put(se.getText(l), l.replace("debugListForm:link", ""));
		}
		se.click("debugListForm:deleteButton" + h.get("jack"));
		assertTrue(se.waitForText("Do you really want to delete this object?", 10));
		se.click("debugListForm:no");
		
		se.click("debugListForm:deleteButton" + h.get("barbossa"));
		assertTrue(se.waitForText("Do you really want to delete this object?", 10));
		se.click("debugListForm:yes");
		se.waitForText("List Objects");
	
		// validate
		assertFalse(se.isTextPresent("barbossa"));
	}

	
	/***
	 * Search user and delete it form  midPoint
	 * 
	 * Actions:
	 * 		1. login as admin
	 * 		2. click to accounts
	 * 		3. find user
	 * 		4. mark user to delete
	 * 		5. click Delete button
	 * 			a) click NO			-> FAIL
	 * 			b) click YES 		-> PASS
	 * 		
	 * 	Do for user:
	 * 		* Elizabeth
	 * 		* barbossa
	 * 
	 * Validation:
	 * 	* user not exists after remove
	 */
	@Test
	public void test02deleteUserViaGUI() {
		
		//delete elizabeth
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

		se.click(h.get("elizabeth") + "deleteCheckbox");
		se.click("admin-content:deleteUser");
		se.waitForText("Confirm delete");
		se.click("admin-content:deleteUserNo");
		se.waitForText("List Users");
		se.click("admin-content:deleteUser");
		se.waitForText("Confirm delete");
		se.click("admin-content:deleteUserYes");
		se.waitForText("List Users");
		assertFalse(se.isTextPresent("Elizabeth Swann"));
		
		//delete jack
		LOGGER.info("Delete jack via GUI");
		se.click(se.findLink("topHome"));
		se.waitForPageToLoad("30000");

		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad("30000");

		assertTrue(se.isTextPresent("New User"));

		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			LOGGER.info("Adding:" + se.getText(l), l.replace("name", ""));
			h.put(se.getText(l), l.replace("name", ""));
		}

		se.click(h.get("jack") + "deleteCheckbox");
		se.click("admin-content:deleteUser");
		se.waitForText("Confirm delete");
		se.click("admin-content:deleteUserYes");
		se.waitForText("List Users");
		assertFalse(se.isTextPresent("Jack"));
		
	}
}
