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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
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
public class ExtensionAsserter<O extends ObjectType, OA extends PrismObjectAsserter<O,RA>, RA> extends AbstractAsserter<OA> {
	
	private PrismContainerValue<?> extensionContainerValue;
	private OA objectAsserter;

	public ExtensionAsserter(OA objectAsserter) {
		super();
		this.objectAsserter = objectAsserter;
	}
	
	public ExtensionAsserter(OA objectAsserter, String details) {
		super(details);
		this.objectAsserter = objectAsserter;
	}
	
	private PrismObject<O> getObject() {
		return objectAsserter.getObject();
	}
	
	private PrismContainerValue<?> getExtensionContainerValue() {
		if (extensionContainerValue == null) {
			extensionContainerValue = getObject().getExtensionContainerValue();
		}
		return extensionContainerValue;
	}
	
	public ExtensionAsserter<O,OA,RA> assertSize(int expected) {
		assertEquals("Wrong number of items in "+desc(), expected, getExtensionContainerValue().size());
		return this;
	}
	
	public ExtensionAsserter<O,OA,RA> assertItems(QName... expectedItems) {
		for (QName expectedItem: expectedItems) {
			Item<PrismValue,ItemDefinition> item = getExtensionContainerValue().findItem(ItemName.fromQName(expectedItem));
			if (item == null) {
				fail("Expected item "+expectedItem+" in "+desc()+" but there was none. Items present: "+presentItemNames());
			}
		}
		for (Item<?, ?> existingItem : getExtensionContainerValue().getItems()) {
			if (!QNameUtil.contains(expectedItems, existingItem.getElementName())) {
				fail("Unexpected item "+existingItem.getElementName()+" in "+desc()+". Expected items: "+QNameUtil.prettyPrint(expectedItems));
			}
		}
		return this;
	}
	
	public ExtensionAsserter<O,OA,RA> assertAny() {
		assertNotNull("No extension container value in "+desc(), getExtensionContainerValue());
		assertFalse("No items in "+desc(), getExtensionContainerValue().isEmpty());
		return this;
	}
	
	private String presentItemNames() {
		StringBuilder sb = new StringBuilder();
		Iterator<Item<?, ?>> iterator = getExtensionContainerValue().getItems().iterator();
		while (iterator.hasNext()) {
			sb.append(PrettyPrinter.prettyPrint(iterator.next().getElementName()));
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}
	
	public <T> ExtensionAsserter<O,OA,RA> assertPropertyValue(QName propName, T... expectedValues) {
		PrismProperty<T> property = findProperty(propName);
		assertNotNull("No property "+propName+" in "+desc(), property);
		PrismAsserts.assertPropertyValueDesc(property, desc(), expectedValues);
		return this;
	}
	
	public <T> ExtensionAsserter<O,OA,RA> assertValueRaw(QName attrName, T... expectedValues) {
		PrismProperty<T> property = findProperty(attrName);
		assertNotNull("No attribute "+attrName+" in "+desc(), property);
		RawType[] expectedRaw = rawize(attrName, getPrismContext(), expectedValues);
		PrismAsserts.assertPropertyValueDesc(property, desc(), (T[])expectedRaw);
		return this;
	}
	
	private <T> RawType[] rawize(QName attrName, PrismContext prismContext, T[] expectedValues) {
		RawType[] raws = new RawType[expectedValues.length];
		for(int i = 0; i < expectedValues.length; i++) {
			raws[i] = new RawType(new PrismPropertyValueImpl<>(expectedValues[i]), attrName, prismContext);
		}
		return raws;
	}
	
	public <T> ExtensionAsserter<O,OA,RA> assertNoItem(QName itemName) {
		Item<PrismValue,ItemDefinition> item = findItem(itemName);
		assertNull("Unexpected item "+itemName+" in "+desc()+": "+item, item);
		return this;
	}

	private <T> PrismProperty<T> findProperty(QName attrName) {
		return getExtensionContainerValue().findProperty(ItemName.fromQName(attrName));
	}
	
	private <T> Item<PrismValue,ItemDefinition> findItem(QName itemName) {
		return getExtensionContainerValue().findItem(ItemName.fromQName(itemName));
	}
	
	public ExtensionAsserter<O,OA,RA> assertTimestampBetween(QName propertyName, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) {
		PrismProperty<XMLGregorianCalendar> property = findProperty(propertyName);
		assertNotNull("No property "+propertyName+" in "+desc(), property);
		XMLGregorianCalendar timestamp = property.getRealValue();
		assertNotNull("No value of property "+propertyName+" in "+desc(), timestamp);
		TestUtil.assertBetween("Wrong value of property "+propertyName+" in "+desc(), startTs, endTs, timestamp);
		return this;
	}


	protected String desc() {
		return "exension of " + descWithDetails(getObject());
	}

	@Override
	public OA end() {
		return objectAsserter;
	}

}
