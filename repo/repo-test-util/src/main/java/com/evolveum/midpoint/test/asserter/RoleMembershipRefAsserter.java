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
import com.evolveum.midpoint.xml.ns._public.common.common_4.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ShadowType;
import com.evolveum.prism.xml.ns._public.types_4.ObjectDeltaType;

/**
 * @author semancik
 *
 */
public class RoleMembershipRefAsserter<R> extends ObjectReferenceAsserter<FocusType,R> {
	
	public RoleMembershipRefAsserter(PrismReferenceValue refVal) {
		super(refVal, FocusType.class);
	}
	
	public RoleMembershipRefAsserter(PrismReferenceValue refVal, String detail) {
		super(refVal, FocusType.class, detail);
	}
	
	public RoleMembershipRefAsserter(PrismReferenceValue refVal, PrismObject<? extends FocusType> resolvedTarget, R returnAsserter, String detail) {
		super(refVal, FocusType.class, resolvedTarget, returnAsserter, detail);
	}
	
	@Override
	public RoleMembershipRefAsserter<R> assertOid() {
		super.assertOid();
		return this;
	}
	
	@Override
	public RoleMembershipRefAsserter<R> assertOid(String expected) {
		super.assertOid(expected);
		return this;
	}
	
	@Override
	public RoleMembershipRefAsserter<R> assertOidDifferentThan(String expected) {
		super.assertOidDifferentThan(expected);
		return this;
	}
	
	public ShadowAsserter<RoleMembershipRefAsserter<R>> shadow() {
		ShadowAsserter<RoleMembershipRefAsserter<R>> asserter = new ShadowAsserter<>((PrismObject<ShadowType>)getRefVal().getObject(), this, "shadow in reference "+desc());
		copySetupTo(asserter);
		return asserter;
	}

	@Override
	public FocusAsserter<FocusType, RoleMembershipRefAsserter<R>> target()
			throws ObjectNotFoundException, SchemaException {
		return new FocusAsserter<>(getResolvedTarget(), this, "object resolved from "+desc());
	}
	
	@Override
	public FocusAsserter<FocusType, RoleMembershipRefAsserter<R>> resolveTarget()
			throws ObjectNotFoundException, SchemaException {
		PrismObject<FocusType> object = resolveTargetObject();
		return new FocusAsserter<>(object, this, "object resolved from "+desc());
	}

}
