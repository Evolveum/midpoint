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
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;

import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ShadowAttributesAsserter extends AbstractAsserter {
	
	private PrismObject<ShadowType> shadow;
	private PrismContainer<ShadowAttributesType> attributesContainer;

	public ShadowAttributesAsserter(PrismObject<ShadowType> shadow) {
		super();
		this.shadow = shadow;
	}
	
	public ShadowAttributesAsserter(PrismObject<ShadowType> shadow, String details) {
		super(details);
		this.shadow = shadow;
	}
	
	private PrismContainer<ShadowAttributesType> getAttributesContainer() {
		if (attributesContainer == null) {
			attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		}
		return attributesContainer;
	}
	
	private PrismContainerValue<ShadowAttributesType> getAttributes() {
		return getAttributesContainer().getValue();
	}
	
	public ShadowAttributesAsserter assertResourceAttributeContainer() {
		assertTrue("Wrong type of attribute container in "+desc()+", expected ResourceAttributeContainer but was " + getAttributesContainer().getClass(), getAttributesContainer() instanceof ResourceAttributeContainer);
		return this;
	}
	
	public ShadowAttributesAsserter assertSize(int expected) {
		assertEquals("Wrong number of attributes in "+desc(), expected, getAttributes().size());
		return this;
	}
	
	public ShadowAttributesAsserter assertAttributes(QName... expectedAttributes) {
		for (QName expectedAttribute: expectedAttributes) {
			PrismProperty<Object> attr = getAttributes().findProperty(expectedAttribute);
			if (attr == null) {
				fail("Expected attribute "+expectedAttribute+" in "+desc()+" but there was none. Attributes present: "+presentAttributeNames());
			}
		}
		for (PrismProperty<?> existingAttr : getAttributes().getProperties()) {
			if (!QNameUtil.contains(expectedAttributes, existingAttr.getElementName())) {
				fail("Unexpected attribute "+existingAttr.getElementName()+" in "+desc()+". Expected attributes: "+QNameUtil.prettyPrint(expectedAttributes));
			}
		}
		return this;
	}
	
	private String presentAttributeNames() {
		StringBuilder sb = new StringBuilder();
		Iterator<Item<?, ?>> iterator = getAttributes().getItems().iterator();
		while (iterator.hasNext()) {
			sb.append(PrettyPrinter.prettyPrint(iterator.next().getElementName()));
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	public ShadowAttributesAsserter assertHasPrimaryIdentifier() {
		Collection<ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
		assertFalse("No primary identifiers in "+desc(), primaryIdentifiers.isEmpty());
		return this;
	}
	
	public ShadowAttributesAsserter assertNoPrimaryIdentifier() {
		Collection<ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
		assertTrue("Unexpected primary identifiers in "+desc()+": "+primaryIdentifiers, primaryIdentifiers.isEmpty());
		return this;
	}
	
	public ShadowAttributesAsserter assertHasSecondaryIdentifier() {
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
		assertFalse("No secondary identifiers in "+desc(), secondaryIdentifiers.isEmpty());
		return this;
	}
	
	public ShadowAttributesAsserter assertNoSecondaryIdentifier() {
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
		assertTrue("Unexpected secpndary identifiers in "+desc()+": "+secondaryIdentifiers, secondaryIdentifiers.isEmpty());
		return this;
	}
	
	public <T> ShadowAttributesAsserter assertValue(QName attrName, T... expectedValues) {
		PrismProperty<T> property = findAttribute(attrName);
		PrismAsserts.assertPropertyValueDesc(property, desc(), expectedValues);
		return this;
	}
	
	private <T> PrismProperty<T> findAttribute(QName attrName) {
		return getAttributes().findProperty(attrName);
	}

	private String desc() {
		return descWithDetails(shadow);
	}

}
