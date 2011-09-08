package com.evolveum.midpoint.testing.selenium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.evolveum.midpoint.testing.Selenium;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

public class Test004UserPassword {
	Selenium se;
	static String baseUrl="http://localhost:8080/idm";

	private static final transient Trace LOGGER = TraceManager.getTrace(Test004UserPassword.class);
	
	@Before
	public void start() {
		WebDriver driver = new FirefoxDriver();
		se = new Selenium(driver, baseUrl);
		se.open("/");
		se.waitForPageToLoad();
	}

	@After
	public void stop() {
		se.stop();
	}
	
	@Test
	public void test010positiveAdminLoginTest() {	
		se.type("loginForm:userName", "administrator");
		se.type("loginForm:password", "secret");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad();
		assertEquals(baseUrl+"/index.iface", se.getLocation());
	}
	
	@Test
	public void test011positiveUserLoginTest() {	
		se.type("loginForm:userName", "elizabeth");
		se.type("loginForm:password", "qwe123.Q");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad();
		assertEquals(baseUrl+"/index.iface", se.getLocation());
	}
	
	@Test
	public void test020adminChangeUserPassword() {
		se.type("loginForm:userName", "administrator");
		se.type("loginForm:password", "secret");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad();
		assertEquals(baseUrl+"/index.iface", se.getLocation());
		
		//modify elizabeth (demote)
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad();
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		// get hashmap and login
		HashMap<String, String> h = new HashMap<String, String>();
		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			h.put(se.getText(l), l.replace("name", ""));
		}
	
		se.click(h.get("elizabeth")+"name");
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/userDetails.iface", se.getLocation());
		assertTrue(se.isTextPresent("Empress"));
		se.click("admin-content:editButton");
		se.waitForText("Save changes",30);
		se.sleep(5);
		se.type("admin-content:icePnlTbSet:0:password1", "drinkRum");
		se.type("admin-content:icePnlTbSet:0:password2", "drinkRum");
		
		se.click("admin-content:saveButton");
		se.waitForPageToLoad();
		se.waitForText("Changes saved successfully"); 
		assertTrue(se.isTextPresent("Changes saved successfully"));
		se.click("logoutUserLink");
	}
	
	@Test
	public void test021adminChangedPasswordValidation() {
		se.type("loginForm:userName", "elizabeth");
		se.type("loginForm:password", "drinkRum");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad();
		assertEquals(baseUrl+"/index.iface", se.getLocation());
		
		
	}
	
	@Test
	public void test030userChangeOwnPassword() {
		se.type("loginForm:userName", "elizabeth");
		se.type("loginForm:password", "drinkRum");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad();
		assertEquals(baseUrl+"/index.iface", se.getLocation());
		
		//modify elizabeth (demote)
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad();
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		// get hashmap and login
		HashMap<String, String> h = new HashMap<String, String>();
		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			h.put(se.getText(l), l.replace("name", ""));
		}
	
		se.click(h.get("elizabeth")+"name");
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/userDetails.iface", se.getLocation());
		assertTrue(se.isTextPresent("Empress"));
		se.click("admin-content:editButton");
		se.waitForText("Save changes",30);
		se.sleep(5);
		se.type("admin-content:icePnlTbSet:0:password1", "qwe123.Q");
		se.type("admin-content:icePnlTbSet:0:password2", "qwe123.Q");
		
		se.click("admin-content:saveButton");
		se.waitForPageToLoad();
		se.waitForText("Changes saved successfully"); 
		assertTrue(se.isTextPresent("Changes saved successfully"));
		se.click("logoutUserLink");
	}

	@Test
	public void test031userChangedPasswordValidation() {
		se.type("loginForm:userName", "elizabeth");
		se.type("loginForm:password", "qwe123.Q");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad();
		assertEquals(baseUrl+"/index.iface", se.getLocation());		
	}
	
	@Test
	public void test040adminChangeOwnPassword() {
		se.type("loginForm:userName", "administrator");
		se.type("loginForm:password", "secret");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad();
		assertEquals(baseUrl+"/index.iface", se.getLocation());
		
		//modify elizabeth (demote)
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad();
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		// get hashmap and login
		HashMap<String, String> h = new HashMap<String, String>();
		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			h.put(se.getText(l), l.replace("name", ""));
		}
	
		se.click(h.get("administrator")+"name");
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/userDetails.iface", se.getLocation());
		assertTrue(se.isTextPresent("administrator@example.com"));
		se.click("admin-content:editButton");
		se.waitForText("Save changes",30);
		se.sleep(5);
		se.type("admin-content:icePnlTbSet:0:password1", "noBodyLivesForever");
		se.type("admin-content:icePnlTbSet:0:password2", "noBodyLivesForever");
		
		se.click("admin-content:saveButton");
		se.waitForPageToLoad();
		se.waitForText("Changes saved successfully"); 
		assertTrue(se.isTextPresent("Changes saved successfully"));
		se.click("logoutUserLink");
	}
	
	@Test
	public void test041adminChangeOwnPasswordBack() {
		se.type("loginForm:userName", "administrator");
		se.type("loginForm:password", "noBodyLivesForever");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad();
		assertEquals(baseUrl+"/index.iface", se.getLocation());
		
		//modify elizabeth (demote)
		se.click(se.findLink("topAccount"));
		se.waitForPageToLoad();
		assertEquals(baseUrl + "/account/index.iface", se.getLocation());
		assertTrue(se.isTextPresent("New User"));
		// get hashmap and login
		HashMap<String, String> h = new HashMap<String, String>();
		for (String l : se.getAllLinks()) {
			if (!l.contains("Table") || !l.contains("name"))
				continue;
			h.put(se.getText(l), l.replace("name", ""));
		}
	
		se.click(h.get("administrator")+"name");
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/userDetails.iface", se.getLocation());
		assertTrue(se.isTextPresent("administrator@example.com"));
		se.click("admin-content:editButton");
		se.waitForText("Save changes",30);
		se.sleep(5);
		se.type("admin-content:icePnlTbSet:0:password1", "secret");
		se.type("admin-content:icePnlTbSet:0:password2", "secret");
		
		se.click("admin-content:saveButton");
		se.waitForPageToLoad();
		se.waitForText("Changes saved successfully"); 
		assertTrue(se.isTextPresent("Changes saved successfully"));
		se.click("logoutUserLink");
	}
	
	@Ignore
	@Test
	public void test050adminResetUserPassword() {
		//TOD after implementation
	}
}
