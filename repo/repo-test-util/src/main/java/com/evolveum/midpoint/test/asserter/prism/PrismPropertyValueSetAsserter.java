/**
 * Copyright (c) 2018-2019 Evolveum
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

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismPropertyValue;


/**
 * @author semancik
 *
 */
public class PrismPropertyValueSetAsserter<T,RA> 
		extends PrismValueSetAsserter<PrismPropertyValue<T>, PrismPropertyValueAsserter<T, PrismPropertyValueSetAsserter<T,RA>>, RA> {
	
	public PrismPropertyValueSetAsserter(Collection<PrismPropertyValue<T>> valueSet) {
		super(valueSet);
	}
	
	public PrismPropertyValueSetAsserter(Collection<PrismPropertyValue<T>> valueSet, String detail) {
		super(valueSet, detail);
	}
	
	public PrismPropertyValueSetAsserter(Collection<PrismPropertyValue<T>> valueSet, RA returnAsserter, String detail) {
		super(valueSet, returnAsserter, detail);
	}
	
	public PrismPropertyValueSetAsserter<T,RA> assertSize(int expected) {
		super.assertSize(expected);
		return this;
	}
	
	public PrismPropertyValueSetAsserter<T,RA> assertNone() {
		super.assertNone();
		return this;
	}
	
	@Override
	protected PrismPropertyValueAsserter<T, PrismPropertyValueSetAsserter<T,RA>> createValueAsserter(PrismPropertyValue<T> pval, String detail) {
		return new PrismPropertyValueAsserter<>(pval, this, detail);
	}

	protected String desc() {
		return getDetails();
	}

}
