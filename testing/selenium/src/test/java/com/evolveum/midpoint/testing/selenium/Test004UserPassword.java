package com.evolveum.midpoint.testing.selenium;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

public class Test004UserPassword {
	WebDriverBackedSelenium se;
	static String baseUrl="http://localhost:8080/idm";

	private static final transient Trace logger = TraceManager.getTrace(Test004UserPassword.class);
	
	@Before
	public void start() {
		WebDriver driver = new FirefoxDriver();
		se = new WebDriverBackedSelenium(driver, baseUrl);
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
		se.type("loginForm:userName", "jack");
		se.type("loginForm:password", "deadmentellnotales");
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
