/**
 * Copyright (c) 2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertNotNull;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

/**
 * @author semancik
 *
 */
public class DisplayTypeAsserter<RA> extends AbstractAsserter<RA> {
	
	private final DisplayType displayType;

	public DisplayTypeAsserter(DisplayType displayType, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.displayType = displayType;
	}
	
	DisplayType getDisplayType() {
		assertNotNull("Null " + desc(), displayType);
		return displayType;
	}
	
	public DisplayTypeAsserter<RA> assertNull() {
		AssertJUnit.assertNull("Unexpected " + desc(), displayType);
		return this;
	}
	
	public DisplayTypeAsserter<RA> assertLabel(String expectedOrig) {
		PrismAsserts.assertEqualsPolyString("Wrong label in "+desc(), expectedOrig, displayType.getLabel());
		return this;
	}
	
	public DisplayTypeAsserter<RA> assertPluralLabel(String expectedOrig) {
		PrismAsserts.assertEqualsPolyString("Wrong pluralLabel in "+desc(), expectedOrig, displayType.getPluralLabel());
		return this;
	}
	
	public DisplayTypeAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, displayType);
		return this;
	}
	
	@Override
	protected String desc() {
		return descWithDetails("display");
	}
	
}
