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
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class PolyStringAsserter<RA> extends AbstractAsserter<RA> {
	
	private PolyString polystring;

	public PolyStringAsserter(PolyString polystring) {
		super();
		this.polystring = polystring;
	}
	
	public PolyStringAsserter(PolyString polystring, String detail) {
		super(detail);
		this.polystring = polystring;
	}
	
	public PolyStringAsserter(PolyString polystring, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.polystring = polystring;
	}
	
	public static <O extends ObjectType> PolyStringAsserter<Void> forPolyString(PolyString polystring) {
		return new PolyStringAsserter<>(polystring);
	}
	
	public PolyString getPolyString() {
		return polystring;
	}
	
	public PolyStringAsserter<RA> assertOrig(String expected) {
		assertEquals("Wrong orig in "+desc(), expected, polystring.getOrig());
		return this;
	}
	
	public PolyStringAsserter<RA> assertNorm(String expected) {
		assertEquals("Wrong norm in "+desc(), expected, polystring.getNorm());
		return this;
	}
	
	public PolyStringAsserter<RA> assertLangs(String... expectedParams) {
		if (polystring.getLang() == null) {
			if (expectedParams.length == 0) {
				return this;
			} else {
				fail("No langs in "+desc());
			}
		}
		Map<String, String> expectedLangs = MiscUtil.paramsToMap(expectedParams);
		for (Entry<String, String> expectedLangEntry : expectedLangs.entrySet()) {
			String realLangValue = polystring.getLang().get(expectedLangEntry.getKey());
			assertEquals("Wrong lang "+expectedLangEntry.getKey()+" in "+desc(), expectedLangEntry.getValue(), realLangValue);
		}
		for (Entry<String, String> realLangEntry : polystring.getLang().entrySet()) {
			String expectedLangValue = expectedLangs.get(realLangEntry.getKey());
			assertEquals("Wrong lang "+realLangEntry.getKey()+" in "+desc(), expectedLangValue, realLangEntry.getValue());
		}
		return this;
	}
	
	protected String desc() {
		return descWithDetails(polystring);
	}

	public PolyStringAsserter<RA> display() {
		display(desc());
		return this;
	}
	
	public PolyStringAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, polystring);
		return this;
	}
}
