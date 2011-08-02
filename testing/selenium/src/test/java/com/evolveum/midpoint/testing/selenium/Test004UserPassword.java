package com.evolveum.midpoint.testing.selenium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.evolveum.midpoint.testing.Selenium;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

public class Test004UserPassword {
	Selenium se;
	static String baseUrl="http://localhost:8080/idm";

	private static final transient Trace logger = TraceManager.getTrace(Test004UserPassword.class);
	
	@Before
	public void start() {
		WebDriver driver = new FirefoxDriver();
		se = new Selenium(driver, baseUrl);
		se.open("/");
		se.waitForPageToLoad("10000");
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
		se.waitForPageToLoad("10000");
		assertEquals(baseUrl+"/index.iface", se.getLocation());
	}
	
	@Test
	public void test011positiveUserLoginTest() {	
		se.type("loginForm:userName", "elizabeth");
		se.type("loginForm:password", "qwe123.Q");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad("10000");
		assertEquals(baseUrl+"/index.iface", se.getLocation());
	}
	
	@Test
	public void test020adminChangeUserPassword() {
		se.type("loginForm:userName", "administrator");
		se.type("loginForm:password", "secret");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad("10000");
		assertEquals(baseUrl+"/index.iface", se.getLocation());
		
		
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
	
		se.click(h.get("elizabeth")+"name");
		se.waitForPageToLoad("30000");
		assertEquals(baseUrl + "/account/userDetails.iface", se.getLocation());
		assertTrue(se.isTextPresent("Empress"));
		se.click("admin-content:editButton");
		se.waitForText("Save changes",30);
		
		for (String s: se.getAllFields() ) {
			if (s.contains("password")) {
				se.type(s, "drinkRum");
			}
		}
		
		se.click("admin-content:saveButton");
		se.waitForPageToLoad("30000");
		se.waitForText("Changes saved successfully"); 
		assertTrue(se.isTextPresent("Changes saved successfully"));
		
		se.click("j_idt25"); 	//TODO refactor to logoutlink
		se.waitForPageToLoad("10000");
		
		assertEquals(baseUrl+"/login.iface", se.getLocation());
		se.waitForText("User Login");
		
		se.type("loginForm:userName", "elizabeth");
		se.type("loginForm:password", "drunkRum");
		se.click("loginForm:loginButton");
		se.waitForPageToLoad("10000");
		assertEquals(baseUrl+"/index.iface", se.getLocation());
	}
	
	@Test
	public void test020userChangeOwnPassword() {
	}
	
	@Test
	public void test030adminResetUserPassword() {
		//TODO
	}
	
	@Test
	public void test040adminChangeOwnPassword() {
	}
}
