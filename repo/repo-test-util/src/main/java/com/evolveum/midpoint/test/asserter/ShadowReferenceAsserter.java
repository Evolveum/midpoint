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

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author semancik
 *
 */
public class ShadowReferenceAsserter<R> extends ObjectReferenceAsserter<ShadowType,R> {
	
	public ShadowReferenceAsserter(PrismReferenceValue refVal) {
		super(refVal, ShadowType.class);
	}
	
	public ShadowReferenceAsserter(PrismReferenceValue refVal, String detail) {
		super(refVal, ShadowType.class, detail);
	}
	
	public ShadowReferenceAsserter(PrismReferenceValue refVal, R returnAsserter, String detail) {
		super(refVal, ShadowType.class, returnAsserter, detail);
	}
	
	@Override
	public ShadowReferenceAsserter<R> assertOid() {
		super.assertOid();
		return this;
	}
	
	public ShadowReferenceAsserter<R> assertOid(String expected) {
		super.assertOid(expected);
		return this;
	}
	
	public ShadowAsserter<ShadowReferenceAsserter<R>> shadow() {
		ShadowAsserter<ShadowReferenceAsserter<R>> asserter = new ShadowAsserter<>((PrismObject<ShadowType>)getRefVal().getObject(), this, "shadow in reference "+desc());
		copySetupTo(asserter);
		return asserter;
	}

	@Override
	public ShadowAsserter<ObjectReferenceAsserter<ShadowType, R>> resolveTarget()
			throws ObjectNotFoundException, SchemaException {
		PrismObject<ShadowType> object = resolveTargetObject();
		return new ShadowAsserter<>(object, this, "object resolved from "+desc());
	}

}
