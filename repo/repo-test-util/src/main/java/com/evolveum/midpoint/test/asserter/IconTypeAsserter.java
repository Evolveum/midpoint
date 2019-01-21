/**
 * Copyright (c) 2019 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_4.IconType;

/**
 * @author semancik
 *
 */
public class IconTypeAsserter<RA> extends AbstractAsserter<RA> {
	
	private final IconType iconType;

	public IconTypeAsserter(IconType iconType, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.iconType = iconType;
	}
	
	IconType getIconType() {
		assertNotNull("Null " + desc(), iconType);
		return iconType;
	}
	
	public IconTypeAsserter<RA> assertNull() {
		AssertJUnit.assertNull("Unexpected " + desc(), iconType);
		return this;
	}
	
	public IconTypeAsserter<RA> assertCssClass(String expected) {
		assertEquals("Wrong label in "+desc(), expected, iconType.getCssClass());
		return this;
	}
	
	public IconTypeAsserter<RA> assertColor(String expected) {
		assertEquals("Wrong color in "+desc(), expected, iconType.getColor());
		return this;
	}
	
	public IconTypeAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, iconType);
		return this;
	}
	
	@Override
	protected String desc() {
		return descWithDetails("icon");
	}
	
}
