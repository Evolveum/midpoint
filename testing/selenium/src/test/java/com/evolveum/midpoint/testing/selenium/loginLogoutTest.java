package com.evolveum.midpoint.testing.selenium;

import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.regex.Pattern;

public class loginLogoutTest {

	WebDriverBackedSelenium selenium;
	

	@Test
	public void positiveLoginTest() {

		WebDriver driver = new FirefoxDriver();
		String baseUrl = "http://localhost:8080/";
		selenium = new WebDriverBackedSelenium(driver, baseUrl);
		selenium.start();

		selenium.open("/idm/");
		selenium.waitForPageToLoad("30000");
		assertEquals("midPoint", selenium.getTitle());
		//selenium.click("css=span.regular");
		//selenium.waitForPageToLoad("30000");
		//assertEquals("midPoint", selenium.getTitle());
		//selenium.click("css=#j_idt24 > span");
		//selenium.waitForPageToLoad("30000");
		//assertEquals("midPoint", selenium.getTitle());
		//selenium.type("loginForm:userName", "huhulak");
		//selenium.type("loginForm:password", "huhulesne");
		//selenium.click("css=span.regular");
		//assertEquals("", selenium.getAttribute("Invalid username and/or password."));		
		selenium.stop();

	}
}
