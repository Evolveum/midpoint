package com.evolveum.midpoint.testing.selenium;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.regex.Pattern;

public class loginLogoutTest {

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
		
		selenium.type("loginForm:userName", "administrator");
		selenium.type("loginForm:password", "secreta");
		selenium.click("loginForm:loginButton");
		//selenium.click("css=span.regular");
		selenium.waitForPageToLoad("30000");
		assertNotSame(baseUrl+"idm/index.iface", selenium.getLocation());
		
	}
	@After
	public void stop() {
		selenium.stop();
	}
}
