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
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
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
public class PrismValueDeltaSetTripleAsserter<V extends PrismValue,RA> extends DeltaSetTripleAsserter<V,PrismValueDeltaSetTriple<V>,RA> {
	
	public PrismValueDeltaSetTripleAsserter(PrismValueDeltaSetTriple<V> triple) {
		super(triple);
	}
	
	public PrismValueDeltaSetTripleAsserter(PrismValueDeltaSetTriple<V> triple, String detail) {
		super(triple, detail);
	}
	
	public PrismValueDeltaSetTripleAsserter(PrismValueDeltaSetTriple<V> triple, RA returnAsserter, String detail) {
		super(triple, returnAsserter, detail);
	}
	
	public static <V extends PrismValue> PrismValueDeltaSetTripleAsserter<V,Void> forPrismValueDeltaSetTriple(PrismValueDeltaSetTriple<V> triple) {
		return new PrismValueDeltaSetTripleAsserter<>(triple);
	}
	
	@Override
	public PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> zeroSet() {
		return createSetAsserter(getTriple().getZeroSet(), "zero");
	}
	
	@Override
	public PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> plusSet() {
		return createSetAsserter(getTriple().getPlusSet(), "plus");
	}
	
	@Override
	public PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> minusSet() {
		return createSetAsserter(getTriple().getMinusSet(), "minus");
	}
	
	private PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> createSetAsserter(Collection<V> set, String name) {
		PrismValueDeltaSetTripleSetAsserter<V,PrismValueDeltaSetTripleAsserter<V,RA>> setAsserter = new PrismValueDeltaSetTripleSetAsserter<>(set, this, name+" set of "+desc());
		copySetupTo(setAsserter);
		return setAsserter;
	}
	
	@Override
	public PrismValueDeltaSetTripleAsserter<V,RA> assertNullZero() {
		super.assertNullZero();
		return this;
	}
	
	@Override
	public PrismValueDeltaSetTripleAsserter<V,RA> assertNullPlus() {
		super.assertNullPlus();
		return this;
	}
	
	@Override
	public PrismValueDeltaSetTripleAsserter<V,RA> assertNullMinus() {
		super.assertNullMinus();
		return this;
	}
	
	@Override
	public PrismValueDeltaSetTripleAsserter<V,RA> assertEmptyZero() {
		super.assertEmptyZero();
		return this;
	}
	
	@Override
	public PrismValueDeltaSetTripleAsserter<V,RA> assertEmptyPlus() {
		super.assertEmptyPlus();
		return this;
	}
	
	@Override
	public PrismValueDeltaSetTripleAsserter<V,RA> assertEmptyMinus() {
		super.assertEmptyMinus();
		return this;
	}
	
	@Override
	public PrismValueDeltaSetTripleAsserter<V,RA> display() {
		super.display();
		return this;
	}
	
	@Override
	public PrismValueDeltaSetTripleAsserter<V,RA> display(String message) {
		super.display(message);
		return this;
	}
}
