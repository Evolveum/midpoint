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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;


/**
 * @author semancik
 *
 */
public class PrismContainerValueSetAsserter<C extends Containerable,RA> 
		extends PrismValueSetAsserter<PrismContainerValue<C>, PrismContainerValueAsserter<C, PrismContainerValueSetAsserter<C,RA>>, RA> {
	
	public PrismContainerValueSetAsserter(Collection<PrismContainerValue<C>> valueSet) {
		super(valueSet);
	}
	
	public PrismContainerValueSetAsserter(Collection<PrismContainerValue<C>> valueSet, String detail) {
		super(valueSet, detail);
	}
	
	public PrismContainerValueSetAsserter(Collection<PrismContainerValue<C>> valueSet, RA returnAsserter, String detail) {
		super(valueSet, returnAsserter, detail);
	}
	
	public PrismContainerValueSetAsserter<C,RA> assertSize(int expected) {
		super.assertSize(expected);
		return this;
	}
	
	public PrismContainerValueSetAsserter<C,RA> assertNone() {
		super.assertNone();
		return this;
	}
	
	@Override
	protected PrismContainerValueAsserter<C, PrismContainerValueSetAsserter<C,RA>> createValueAsserter(PrismContainerValue<C> pval, String detail) {
		return new PrismContainerValueAsserter<>(pval, this, detail);
	}

	protected String desc() {
		return getDetails();
	}

}
