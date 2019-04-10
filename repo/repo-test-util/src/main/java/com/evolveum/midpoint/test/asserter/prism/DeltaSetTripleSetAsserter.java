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

import java.util.Collection;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class DeltaSetTripleSetAsserter<T,RA> extends AbstractAsserter<RA> {
	
	private Collection<T> set;

	public DeltaSetTripleSetAsserter(Collection<T> set) {
		super();
		this.set = set;
	}
	
	public DeltaSetTripleSetAsserter(Collection<T> set, String detail) {
		super(detail);
		this.set = set;
	}
	
	public DeltaSetTripleSetAsserter(Collection<T> set, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.set = set;
	}
	
	public static <T> DeltaSetTripleSetAsserter<T,Void> forSet(Collection<T> set) {
		return new DeltaSetTripleSetAsserter<>(set);
	}
	
	public Collection<T> getSet() {
		return set;
	}
	
	public DeltaSetTripleSetAsserter<T,RA> assertSize(int expected) {
		assertEquals("Wrong number of values in " + desc(), expected, set.size());
		return this;
	}
	
	public DeltaSetTripleSetAsserter<T,RA> assertNone() {
		assertSize(0);
		return this;
	}
	
	public DeltaSetTripleSetAsserter<T,RA> assertNull() {
		AssertJUnit.assertNull("Expected null, but found non-null set in " + desc(), set);
		return this;
	}
	
	// TODO
	
	protected String desc() {
		return descWithDetails(set);
	}

	public DeltaSetTripleSetAsserter<T,RA> display() {
		display(desc());
		return this;
	}
	
	public DeltaSetTripleSetAsserter<T,RA> display(String message) {
		IntegrationTestTools.display(message, set);
		return this;
	}
}
