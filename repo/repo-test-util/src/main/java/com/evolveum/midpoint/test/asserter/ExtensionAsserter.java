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
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.asserter.prism.PrismContainerValueAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.Iterator;

import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
public class ExtensionAsserter<O extends ObjectType, OA extends PrismObjectAsserter<O,RA>, RA> extends PrismContainerValueAsserter<ExtensionType,OA> {
	
	private OA objectAsserter;

	public ExtensionAsserter(OA objectAsserter) {
		super((PrismContainerValue<ExtensionType>) objectAsserter.getObject().getExtensionContainerValue());
		this.objectAsserter = objectAsserter;
	}
	
	public ExtensionAsserter(OA objectAsserter, String details) {
		super((PrismContainerValue<ExtensionType>) objectAsserter.getObject().getExtensionContainerValue(), details);
		this.objectAsserter = objectAsserter;
	}
	
	private PrismObject<O> getObject() {
		return objectAsserter.getObject();
	}
		
	@Override
	public ExtensionAsserter<O,OA,RA> assertSize(int expected) {
		super.assertSize(expected);
		return this;
	}
	
	@Override
	public ExtensionAsserter<O,OA,RA> assertItems(QName... expectedItems) {
		super.assertItems(expectedItems);
		return this;
	}
	
	@Override
	public ExtensionAsserter<O,OA,RA> assertAny() {
		super.assertAny();
		return this;
	}
	
	@Override
	public <T> ExtensionAsserter<O,OA,RA> assertPropertyValuesEqual(QName propName, T... expectedValues) {
		super.assertPropertyValuesEqual(propName, expectedValues);
		return this;
	}
	
	@Override
	public <T> ExtensionAsserter<O,OA,RA> assertPropertyValuesEqualRaw(QName attrName, T... expectedValues) {
		super.assertPropertyValuesEqualRaw(attrName, expectedValues);
		return this;
	}
	
	@Override
	public <T> ExtensionAsserter<O,OA,RA> assertNoItem(QName itemName) {
		super.assertNoItem(itemName);
		return this;
	}

	@Override
	public ExtensionAsserter<O,OA,RA> assertTimestampBetween(QName propertyName, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
		super.assertTimestampBetween(propertyName, startTs, endTs);
		return this;
	}
	
	@Override
	public <CC extends Containerable> PrismContainerValueAsserter<CC,ExtensionAsserter<O,OA,RA>> containerSingle(QName subcontainerQName) {
		return (PrismContainerValueAsserter<CC, ExtensionAsserter<O, OA, RA>>) super.containerSingle(subcontainerQName);
	}


	protected String desc() {
		return "exension of " + descWithDetails(getObject());
	}

	@Override
	public OA end() {
		return objectAsserter;
	}

}
