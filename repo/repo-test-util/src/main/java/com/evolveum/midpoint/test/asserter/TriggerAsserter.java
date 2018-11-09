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
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.test.IntegrationTestTools;
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
public class TriggerAsserter<R> extends AbstractAsserter<R> {
	
	final private TriggerType trigger;

	public TriggerAsserter(TriggerType trigger) {
		super();
		this.trigger = trigger;
	}
	
	public TriggerAsserter(TriggerType trigger, String detail) {
		super(detail);
		this.trigger = trigger;
	}
	
	public TriggerAsserter(TriggerType trigger, R returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.trigger = trigger;
	}
	
	protected TriggerType getTrigger() {
		return trigger;
	}
	
	public TriggerAsserter<R> assertHandlerUri() {
		assertNotNull("No handler URI in "+desc(), trigger.getHandlerUri());
		return this;
	}
	
	public TriggerAsserter<R> assertHandlerUri(String expected) {
		assertEquals("Wrong handler URI in "+desc(), expected, trigger.getHandlerUri());
		return this;
	}
	
	public TriggerAsserter<R> assertTimestamp() {
		assertNotNull("No timestamp in "+desc(), trigger.getTimestamp());
		return this;
	}
	
	public TriggerAsserter<R> assertTimestamp(XMLGregorianCalendar expected) {
		assertEquals("Wrong timestamp in "+desc(), expected, trigger.getTimestamp());
		return this;
	}
	
	public TriggerAsserter<R> assertTimestampBetween(XMLGregorianCalendar start, XMLGregorianCalendar end) {
		TestUtil.assertBetween("Wrong timestamp in "+desc(), start, end, trigger.getTimestamp());
		return this;
	}
	
	public TriggerAsserter<R> assertTimestampFutureBetween(XMLGregorianCalendar start, XMLGregorianCalendar end, String durationOffset) {
		TestUtil.assertBetween("Wrong timestamp in "+desc(), 
				XmlTypeConverter.addDuration(start, durationOffset),
				XmlTypeConverter.addDuration(end, durationOffset),
				trigger.getTimestamp());
		return this;
	}
	
	protected String desc() {
		// TODO: better desc
		return descWithDetails(trigger);
	}
	
	public TriggerAsserter<R> display() {
		display(desc());
		return this;
	}
	
	public TriggerAsserter<R> display(String message) {
		IntegrationTestTools.display(message, trigger);
		return this;
	}	
}
