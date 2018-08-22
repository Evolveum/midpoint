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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * @author semancik
 *
 */
public class ActivationAsserter<F extends FocusType, FA extends FocusAsserter<F, RA>,RA> extends AbstractAsserter<FA> {
	
	private FA focusAsserter;
	private ActivationType activationType;

	public ActivationAsserter(FA focusAsserter) {
		super();
		this.focusAsserter = focusAsserter;
	}
	
	public ActivationAsserter(FA focusAsserter, String details) {
		super(details);
		this.focusAsserter = focusAsserter;
	}
	
	public static <F extends FocusType> ActivationAsserter<F,FocusAsserter<F,Void>,Void> forFocus(PrismObject<F> focus) {
		return new ActivationAsserter<>(FocusAsserter.forFocus(focus));
	}
	
	ActivationType getActivation() {
		if (activationType == null) {
			activationType = getFocus().asObjectable().getActivation();
			assertNotNull("No activation in "+desc());
		}
		return activationType;
	}
	
	public ActivationAsserter<F, FA, RA> assertAdministrativeStatus(ActivationStatusType expected) {
		assertEquals("Wrong administrative status in " + desc(), expected, getActivation().getAdministrativeStatus());
		return this;
	}
	
	public ActivationAsserter<F, FA, RA> assertValidFrom(XMLGregorianCalendar expected) {
		assertEquals("Wrong validFrom in " + desc(), expected, getActivation().getValidFrom());
		return this;
	}
	
	public ActivationAsserter<F, FA, RA> assertValidFrom(Date expected) {
		assertEquals("Wrong validFrom in " + desc(), XmlTypeConverter.createXMLGregorianCalendar(expected), getActivation().getValidFrom());
		return this;
	}
	
	public ActivationAsserter<F, FA, RA> assertValidTo(XMLGregorianCalendar expected) {
		assertEquals("Wrong validTo in " + desc(), expected, getActivation().getValidTo());
		return this;
	}
	
	public ActivationAsserter<F, FA, RA> assertValidTo(Date expected) {
		assertEquals("Wrong validTo in " + desc(), XmlTypeConverter.createXMLGregorianCalendar(expected), getActivation().getValidTo());
		return this;
	}
		
	PrismObject<F> getFocus() {
		return focusAsserter.getObject();
	}
	
	@Override
	public FA end() {
		return focusAsserter;
	}

	@Override
	protected String desc() {
		return descWithDetails("activation of "+getFocus());
	}
	
}
