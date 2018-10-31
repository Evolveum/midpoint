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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;


/**
 * @author semancik
 *
 */
public class PrismContainerValueAsserter<C extends Containerable, RA> extends PrismValueAsserter<PrismContainerValue<C>, RA> {
	
	public PrismContainerValueAsserter(PrismContainerValue<C> prismValue) {
		super(prismValue);
	}
	
	public PrismContainerValueAsserter(PrismContainerValue<C> prismValue, String detail) {
		super(prismValue, detail);
	}
	
	public PrismContainerValueAsserter(PrismContainerValue<C> prismValue, RA returnAsserter, String detail) {
		super(prismValue, returnAsserter, detail);
	}
	
	public <T> PrismContainerValueAsserter<C,RA> assertPropertyEquals(QName propName, T expected) {
		PrismProperty<T> prop = getPrismValue().findProperty(propName);
		if (prop == null && expected == null) {
			return this;
		}
		assertNotNull("No "+propName.getLocalPart()+" in "+desc(), prop);
		T realValue = prop.getRealValue();
		assertNotNull("No value in "+propName.getLocalPart()+" in "+desc(), realValue);
		assertEquals("Wrong "+propName.getLocalPart()+" in "+desc(), expected, realValue);
		return this;
	}
	
	// TODO

	protected String desc() {
		return getDetails();
	}

}
