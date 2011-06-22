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

package com.evolveum.midpoint.testing;
/***
 * Adding missing usefull features to Selenium framework 
 *  @author mamut
 */
import java.util.Arrays;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;

import com.google.common.base.Supplier;


public class Selenium extends WebDriverBackedSelenium {
	
	public Selenium(WebDriver baseDriver, String baseUrl) {
		super(baseDriver, baseUrl);
	}

	public Selenium(Supplier<WebDriver> maker, String baseUrl) {
		super(maker, baseUrl);
	}
	
	/***
	 * Wait for text for 10 seconds
	 * @param text - text to wait
	 */
	public void waitForText(String text) {
		this.waitForText(text, 10);
	}
	
	/***
	 * Wait for text until timeout reached
	 * @param text - text to wait
	 * @param timeout - timeout in sec
	 * @return true/false if text is after timeout there
	 */

	public boolean waitForText(String text, int timeout) {
		
		for (int i = 0; i < timeout*3; i++) {
			try {
				Thread.sleep(333);
			} catch (InterruptedException e) {
			}
			if (this.isTextPresent(text)) {
				return true;
			}
		}
		return this.isTextPresent(text);
	}
	
	/***
	 * Sleep for
	 * @param sleep - number seconds to wait
	 */
	public void sleep (int sleep) {
		try {
			Thread.sleep(1000*sleep);
		} catch (InterruptedException e) {
		}
	}
	
	/***
	 * Try to find possible link from subpart
	 * @param part
	 * @return
	 */
	public String findLink(String part) {
		for (String s : Arrays.asList(this.getAllLinks())) {
			if (s.contains(part)) {
				return s;
			}
		}
		return null;
	}
	
}
