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
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.prism.ObjectDeltaAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author semancik
 *
 */
public abstract class ElementContextAsserter<C extends ModelElementContext<O>, O extends ObjectType,RA> extends AbstractAsserter<RA> {
	
	final private C elementContext;

	public ElementContextAsserter(C elementContext) {
		super();
		this.elementContext = elementContext;
	}
	
	public ElementContextAsserter(C elementContext, String detail) {
		super(detail);
		this.elementContext = elementContext;
	}
	
	public ElementContextAsserter(C elementContext, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.elementContext = elementContext;
	}
	
	public C getElementContext() {
		return elementContext;
	}

	public PrismObjectAsserter<O,? extends ElementContextAsserter<C,O,RA>> objectOld() {
		PrismObjectAsserter<O,ElementContextAsserter<C,O,RA>> asserter = new PrismObjectAsserter<>(
				elementContext.getObjectOld(), this, "object old in "+desc());
		copySetupTo(asserter);
		return asserter;
	}
	
	public PrismObjectAsserter<O,? extends ElementContextAsserter<C,O,RA>> objectCurrent() {
		PrismObjectAsserter<O,ElementContextAsserter<C,O,RA>> asserter = new PrismObjectAsserter<>(
				elementContext.getObjectCurrent(), this, "object current in "+desc());
		copySetupTo(asserter);
		return asserter;
	}
	
	public PrismObjectAsserter<O,? extends ElementContextAsserter<C,O,RA>> objectNew() {
		PrismObjectAsserter<O,? extends ElementContextAsserter<C,O,RA>> asserter = new PrismObjectAsserter<>(
				elementContext.getObjectNew(), this, "object new in "+desc());
		copySetupTo(asserter);
		return asserter;
	}
	
	public ObjectDeltaAsserter<O, ? extends ElementContextAsserter<C,O,RA>> primaryDelta() {
		ObjectDeltaAsserter<O, ? extends ElementContextAsserter<C,O,RA>> deltaAsserter = new ObjectDeltaAsserter<>(
				elementContext.getPrimaryDelta(), this, "primary delta in "+desc());
		copySetupTo(deltaAsserter);
		return deltaAsserter;
	}
	
	public ElementContextAsserter<C,O,RA> assertNoPrimaryDelta() {
		assertNull("Unexpected primary delta in "+desc(), elementContext.getPrimaryDelta());
		return this;
	}
	
	public ObjectDeltaAsserter<O, ? extends ElementContextAsserter<C,O,RA>> secondaryDelta() {
		ObjectDeltaAsserter<O, ? extends ElementContextAsserter<C,O,RA>> deltaAsserter = new ObjectDeltaAsserter<>(
				elementContext.getSecondaryDelta(), this, "secondary delta in "+desc());
		copySetupTo(deltaAsserter);
		return deltaAsserter;
	}

	public ElementContextAsserter<C,O,RA> assertNoSecondaryDelta() {
		assertNull("Unexpected secondary delta in "+desc(), elementContext.getSecondaryDelta());
		return this;
	}

	protected String desc() {
		// TODO: better desc
		return descWithDetails(elementContext);
	}
	
	public ElementContextAsserter<C,O,RA> display() {
		display(desc());
		return this;
	}
	
	public ElementContextAsserter<C,O,RA> display(String message) {
		IntegrationTestTools.display(message, elementContext);
		return this;
	}	
}
