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
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;


/**
 * @author semancik
 *
 */
public abstract class PrismValueSetAsserter<V extends PrismValue,VA extends PrismValueAsserter<V,? extends PrismValueSetAsserter<V,VA,RA>>, RA> extends AbstractAsserter<RA> {
	
	private Collection<V> valueSet;

	public PrismValueSetAsserter(Collection<V> valueSet) {
		super();
		this.valueSet = valueSet;
	}
	
	public PrismValueSetAsserter(Collection<V> valueSet, String detail) {
		super(detail);
		this.valueSet = valueSet;
	}
	
	public PrismValueSetAsserter(Collection<V> valueSet, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.valueSet = valueSet;
	}
	
	
	public PrismValueSetAsserter<V,VA,RA> assertSize(int expected) {
		assertEquals("Wrong number of values in " + desc(), expected, valueSet.size());
		return this;
	}
	
	public PrismValueSetAsserter<V,VA,RA> assertNone() {
		assertSize(0);
		return this;
	}
	
	public VA single() {
		assertSize(1);
		VA valueAsserter = createValueAsserter(valueSet.iterator().next(), "single value in " + desc());
		copySetupTo((AbstractAsserter)valueAsserter);
		return valueAsserter;
	}
		
	protected abstract VA createValueAsserter(V pval, String detail);

	protected String desc() {
		return getDetails();
	}

}
