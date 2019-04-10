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

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ContainerDeltaAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class DeltaSetTripleAsserter<T,D extends DeltaSetTriple<T>,RA> extends AbstractAsserter<RA> {
	
	private D triple;

	public DeltaSetTripleAsserter(D triple) {
		super();
		this.triple = triple;
	}
	
	public DeltaSetTripleAsserter(D triple, String detail) {
		super(detail);
		this.triple = triple;
	}
	
	public DeltaSetTripleAsserter(D triple, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.triple = triple;
	}
	
	public static <T> DeltaSetTripleAsserter<T,DeltaSetTriple<T>,Void> forDeltaSetTriple(DeltaSetTriple<T> triple) {
		return new DeltaSetTripleAsserter<>(triple);
	}
	
	public D getTriple() {
		return triple;
	}
	
	public DeltaSetTripleSetAsserter<T,? extends DeltaSetTripleAsserter<T,D,RA>> zeroSet() {
		return createSetAsserter(triple.getZeroSet(), "zero");
	}
	
	public DeltaSetTripleSetAsserter<T,? extends DeltaSetTripleAsserter<T,D,RA>> plusSet() {
		return createSetAsserter(triple.getPlusSet(), "plus");
	}
	
	public DeltaSetTripleSetAsserter<T,? extends DeltaSetTripleAsserter<T,D,RA>> minusSet() {
		return createSetAsserter(triple.getMinusSet(), "minus");
	}
	
	private DeltaSetTripleSetAsserter<T,DeltaSetTripleAsserter<T,D,RA>> createSetAsserter(Collection<T> set, String name) {
		DeltaSetTripleSetAsserter<T,DeltaSetTripleAsserter<T,D,RA>> setAsserter = new DeltaSetTripleSetAsserter<>(set, this, name+" set of "+desc());
		copySetupTo(setAsserter);
		return setAsserter;
	}
	
	public DeltaSetTripleAsserter<T,D,RA> assertNullZero() {
		assertNullSet(triple.getZeroSet(), "zero");
		return this;
	}
	
	public DeltaSetTripleAsserter<T,D,RA> assertNullPlus() {
		assertNullSet(triple.getPlusSet(), "plus");
		return this;
	}
	
	public DeltaSetTripleAsserter<T,D,RA> assertNullMinus() {
		assertNullSet(triple.getMinusSet(), "minus");
		return this;
	}
	
	private void assertNullSet(Collection<T> set, String name) {
		assertNull("Non-null "+name+" set in "+desc(), set);
	}
	
	public DeltaSetTripleAsserter<T,D,RA> assertEmptyZero() {
		assertEmptySet(triple.getZeroSet(), "zero");
		return this;
	}
	
	public DeltaSetTripleAsserter<T,D,RA> assertEmptyPlus() {
		assertEmptySet(triple.getPlusSet(), "plus");
		return this;
	}
	
	public DeltaSetTripleAsserter<T,D,RA> assertEmptyMinus() {
		assertEmptySet(triple.getMinusSet(), "minus");
		return this;
	}
	
	private void assertEmptySet(Collection<T> set, String name) {
		assertTrue("Non-empty "+name+" set in "+desc(), set.isEmpty());
	}
	
	protected String desc() {
		return descWithDetails(triple);
	}

	public DeltaSetTripleAsserter<T,D,RA> display() {
		display(desc());
		return this;
	}
	
	public DeltaSetTripleAsserter<T,D,RA> display(String message) {
		IntegrationTestTools.display(message, triple);
		return this;
	}
}
