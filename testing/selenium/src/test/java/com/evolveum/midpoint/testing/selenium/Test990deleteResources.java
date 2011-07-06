package com.evolveum.midpoint.testing.selenium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.testing.Selenium;

public class Test990deleteResources {
	Selenium se;
	static String baseUrl = "http://localhost:8080/idm";

	private static final transient Trace logger = TraceManager.getTrace(Test990deleteResources.class);

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
	 * 		4. select from list Resource
	 *		5. click list button
	 *		8. click to delete button near resource Localhost OpenDJ
	 *		9. delete it (click YES)
	 *		10. validate if user Localhost OpenDJ is removed
	*/
	@Test
	public void test01deleteResourceViaDebug() {
		// failing import allready exists
		se.click(se.findLink("topConfiguration"));
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/config/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("Debugging"));
		se.click(se.findLink("leftList"));
		assertTrue(se.waitForText("List Objects"));
		se.select("debugListForm:selectOneMenuList", "label=Resource");
		se.click("debugListForm:listObjectsButton");
		
		//validate if both user are there
		assertTrue(se.isTextPresent("Localhost OpenDJ"));
		
		// get hashmap and login
		HashMap<String, String> h = new HashMap<String, String>();
		for (String l : se.getAllLinks()) {
			if (!l.contains("debugListForm:link"))
				continue;
			h.put(se.getText(l), l.replace("debugListForm:link", ""));
		}
		se.click("debugListForm:deleteButton" + h.get("Localhost OpenDJ"));
		assertTrue(se.waitForText("Do you really want to delete this object?", 10));
		se.click("debugListForm:yes");
		
		se.waitForText("List Objects");
		se.select("debugListForm:selectOneMenuList", "label=Resource");
		se.click("debugListForm:listObjectsButton");
		
		// validate
		assertFalse(se.isTextPresent("Localhost OpenDJ"));
	}

}
