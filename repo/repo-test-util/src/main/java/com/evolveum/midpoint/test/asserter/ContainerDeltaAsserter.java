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

import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;

/**
 * @author semancik
 *
 */
public class ContainerDeltaAsserter<C extends Containerable,RA> extends AbstractAsserter<RA> {
	
	private ContainerDelta<C> containerDelta;

	public ContainerDeltaAsserter(ContainerDelta<C> containerDelta) {
		super();
		this.containerDelta = containerDelta;
	}
	
	public ContainerDeltaAsserter(ContainerDelta<C> containerDelta, String detail) {
		super(detail);
		this.containerDelta = containerDelta;
	}
	
	public ContainerDeltaAsserter(ContainerDelta<C> containerDelta, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.containerDelta = containerDelta;
	}
	
	public static <C extends Containerable> ContainerDeltaAsserter<C,Void> forDelta(ContainerDelta<C> containerDelta) {
		return new ContainerDeltaAsserter<>(containerDelta);
	}
	
	public PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> valuesToAdd() {
		Collection<PrismContainerValue<C>> valuesToAdd = containerDelta.getValuesToAdd();
		PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> setAsserter = new PrismContainerValueSetAsserter<>(
				valuesToAdd, this, "values to add in "+desc());
		copySetupTo(setAsserter);
		return setAsserter;
	}
	
	public ContainerDeltaAsserter<C,RA> assertNoValuesToAdd() {
		Collection<PrismContainerValue<C>> valuesToAdd = containerDelta.getValuesToAdd();
		assertNull("Unexpected values to add in "+desc()+": "+valuesToAdd, valuesToAdd);
		return this;
	}

	public PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> valuesToDelete() {
		Collection<PrismContainerValue<C>> valuesToDelete = containerDelta.getValuesToDelete();
		PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> setAsserter = new PrismContainerValueSetAsserter<>(
				valuesToDelete, this, "values to delte in "+desc());
		copySetupTo(setAsserter);
		return setAsserter;
	}
	
	public ContainerDeltaAsserter<C,RA> assertNoValuesToDelete() {
		Collection<PrismContainerValue<C>> valuesToDelete = containerDelta.getValuesToDelete();
		assertNull("Unexpected values to delete in "+desc()+": "+valuesToDelete, valuesToDelete);
		return this;
	}
	
	public PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> valuesToReplace() {
		Collection<PrismContainerValue<C>> valuesToReplace = containerDelta.getValuesToReplace();
		PrismContainerValueSetAsserter<C,ContainerDeltaAsserter<C,RA>> setAsserter = new PrismContainerValueSetAsserter<>(
				valuesToReplace, this, "values to replace in "+desc());
		copySetupTo(setAsserter);
		return setAsserter;
	}
	
	public ContainerDeltaAsserter<C,RA> assertNoValuesToReplace() {
		Collection<PrismContainerValue<C>> valuesToReplace = containerDelta.getValuesToReplace();
		assertNull("Unexpected values to replace in "+desc()+": "+valuesToReplace, valuesToReplace);
		return this;
	}
	
	protected String desc() {
		return descWithDetails(containerDelta);
	}

}
