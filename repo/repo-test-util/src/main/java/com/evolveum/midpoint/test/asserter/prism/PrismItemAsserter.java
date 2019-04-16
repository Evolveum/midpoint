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

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;


/**
 * @author semancik
 *
 */
public abstract class PrismItemAsserter<I extends Item, RA> extends AbstractAsserter<RA> {
	
	private I item;

	public PrismItemAsserter(I item) {
		super();
		this.item = item;
	}
	
	public PrismItemAsserter(I item, String detail) {
		super(detail);
		this.item = item;
	}
	
	public PrismItemAsserter(I item, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.item = item;
	}
	
	public I getItem() {
		return item;
	}
	
	public PrismItemAsserter<I,RA> assertSize(int expected) {
		assertEquals("Wrong number of values in "+desc(), expected, item.size());
		return this;
	}
	
	public PrismItemAsserter<I,RA> assertComplete() {
		assertFalse("Expected complete item, but it was incomplete "+desc(), item.isIncomplete());
		return this;
	}

	public PrismItemAsserter<I,RA> assertIncomplete() {
		assertTrue("Expected incomplete item, but it was complete "+desc(), item.isIncomplete());
		return this;
	}

	// TODO

	protected String desc() {
		return getDetails();
	}

}
