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

import com.evolveum.midpoint.prism.PrismValue;


/**
 * @author semancik
 *
 */
public abstract class PrismValueAsserter<V extends PrismValue, RA> extends AbstractAsserter<RA> {
	
	private V prismValue;

	public PrismValueAsserter(V prismValue) {
		super();
		this.prismValue = prismValue;
	}
	
	public PrismValueAsserter(V prismValue, String detail) {
		super(detail);
		this.prismValue = prismValue;
	}
	
	public PrismValueAsserter(V prismValue, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.prismValue = prismValue;
	}
	
	public V getPrismValue() {
		return prismValue;
	}
	
	// TODO

	protected String desc() {
		return getDetails();
	}

}
