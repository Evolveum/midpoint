/**
 * Copyright (c) 2015-2018 Evolveum
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
package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class WrapperTestUtil {
	
	public static <C extends Containerable,T> void fillInPropertyWrapper(ContainerValueWrapper<C> containerWrapper, ItemPath itemPath, T... newValues) {
		ItemWrapper itemWrapper = containerWrapper.findPropertyWrapper(itemPath);
		assertNotNull("No item wrapper for path "+itemPath+" in "+containerWrapper, itemWrapper);
		fillInPropertyWrapper(containerWrapper, itemWrapper, itemPath.lastNamed().getName(), newValues);
	}
	
	public static <C extends Containerable,T> void fillInPropertyWrapper(ContainerValueWrapper<C> containerWrapper, QName itemName, T... newValues) {
		ItemWrapper itemWrapper = containerWrapper.findPropertyWrapper(itemName);
		assertNotNull("No item wrapper "+itemName+" in "+containerWrapper, itemWrapper);
		fillInPropertyWrapper(containerWrapper, itemWrapper, itemName, newValues);
	}
	
	private static <C extends Containerable,T> void fillInPropertyWrapper(ContainerValueWrapper<C> containerWrapper, ItemWrapper itemWrapper, QName itemName, T... newValues) {
		for (T newValue: newValues) {
			List<ValueWrapper> valueWrappers = itemWrapper.getValues();
			ValueWrapper lastValueWrapper = valueWrappers.get(valueWrappers.size() - 1);
			PrismPropertyValue<T> pval = (PrismPropertyValue<T>) lastValueWrapper.getValue();
			if (!isEmptyValue(pval)) {
				itemWrapper.addValue(true);
				valueWrappers = itemWrapper.getValues();
				lastValueWrapper = valueWrappers.get(valueWrappers.size() - 1);
				pval = (PrismPropertyValue<T>) lastValueWrapper.getValue();
			}
			pval.setValue(newValue);
		}
	}

	private static <T> boolean isEmptyValue(PrismPropertyValue<T> pval) {
		T val = pval.getValue();
		if (val == null) {
			return true;
		}
		if (val instanceof String && ((String)val).isEmpty()) {
			return true;
		}
		if (val instanceof PolyString && ((PolyString)val).isEmpty()) {
			return true;
		}
		return false;
	}

	public static <C extends Containerable,T> void assertPropertyWrapper(ContainerValueWrapper<C> containerWrapper, ItemPath itemPath, T... expectedValues) {
		ItemWrapper itemWrapper = containerWrapper.findPropertyWrapper(itemPath);
		assertNotNull("No item wrapper for path "+itemPath+" in "+containerWrapper, itemWrapper);
		assertPropertyWrapper(containerWrapper, itemWrapper, itemPath.lastNamed().getName(), expectedValues);
	}

	public static <C extends Containerable,T> void assertPropertyWrapper(ContainerValueWrapper<C> containerWrapper, QName itemName, T... expectedValues) {
		ItemWrapper itemWrapper = containerWrapper.findPropertyWrapper(itemName);
		assertNotNull("No item wrapper "+itemName+" in "+containerWrapper, itemWrapper);
		assertPropertyWrapper(containerWrapper, itemWrapper, itemName, expectedValues);
	}
	
	private static <C extends Containerable,T> void assertPropertyWrapper(ContainerValueWrapper<C> containerWrapper, ItemWrapper itemWrapper, QName itemName, T... expectedValues) {
		List<ValueWrapper> valueWrappers = itemWrapper.getValues();
		assertPropertyWrapperValues("item wrapper "+itemName+" in "+containerWrapper, valueWrappers, expectedValues);
	}

	public static <C extends Containerable,T> void assertPropertyWrapperValues(String desc, List<ValueWrapper> valueWrappers, T... expectedValues) {
		if (expectedValues == null) {
			expectedValues = (T[]) new Object[] { null };
		}
		assertEquals("Wrong number of values in "+desc+"; was: "+valueWrappers+", expected: "+Arrays.toString(expectedValues), expectedValues.length, valueWrappers.size());
		if (expectedValues.length == 0) {
			return;
		}
		for (ValueWrapper vw: valueWrappers) {
			PrismValue actualPval = vw.getValue();
			if (actualPval instanceof PrismPropertyValue<?>) {
				T actualValue = ((PrismPropertyValue<T>)actualPval).getValue();
				if (expectedValues == null || expectedValues.length == 0 || ( expectedValues.length == 1 && expectedValues[0] == null)) {
					if (!isEmptyValue((PrismPropertyValue<T>) vw.getValue())) {
						AssertJUnit.fail("Unexpected value "+actualValue+" in value wrapper in "+desc+"; was: "+valueWrappers+", expected: "+Arrays.toString(expectedValues));
					}
				} else {
					boolean found = false;
					for (T expectedValue: expectedValues) {
						if (MiscUtil.equals(expectedValue, actualValue)) {
							found = true;
						}
					}
					if (!found) {
						AssertJUnit.fail("Unexpected value "+actualValue+" in value wrapper in "+desc+"; was: "+valueWrappers+", expected: "+Arrays.toString(expectedValues));
					}
				}
			} else {
				AssertJUnit.fail("expected PrismPropertyValue in value wrapper in "+desc+", but got "+actualPval.getClass());
			}

		}
	}

	public static <C extends Containerable, O extends ObjectType> void assertWrapper(ContainerWrapper<C> containerWrapper, String displayName,
			QName itemName, PrismObject<O> object, ContainerStatus status) {
		assertWrapper(containerWrapper, displayName, itemName==null?null:new ItemPath(itemName), object, status);
	}

	public static <C extends Containerable, O extends ObjectType> void assertWrapper(ContainerWrapper<C> containerWrapper, String displayName,
			ItemPath expectedPath, PrismObject<O> object, ContainerStatus status) {
		PrismContainer<C> container;
		if (expectedPath == null) {
			container = (PrismContainer<C>) object;
		} else {
			container = object.findContainer(expectedPath);
		}
		assertWrapper(containerWrapper, displayName, expectedPath, container, expectedPath==null, status);
	}

	public static <C extends Containerable> void assertWrapper(ContainerWrapper<C> containerWrapper, String displayName, ItemPath expectedPath,
			PrismContainer<C> container, boolean isMain, ContainerStatus status) {
		assertNotNull("null wrapper", containerWrapper);
		assertEquals("Wrong main flag in wrapper "+containerWrapper, expectedPath == null ? ItemPath.EMPTY_PATH : expectedPath, containerWrapper.getPath());
		assertEquals("Wrong main flag in wrapper "+containerWrapper, isMain, containerWrapper.isMain());
		if (container != null) {
			assertEquals("Wrong item in wrapper "+containerWrapper, container, containerWrapper.getItem());
		}
		assertEquals("Wrong displayName in wrapper "+containerWrapper, displayName, containerWrapper.getDisplayName());
		assertEquals("Wrong status in wrapper "+containerWrapper, status, containerWrapper.getStatus());
	}

	public static <O extends ObjectType> void assertWrapper(ObjectWrapper<O> objectWrapper, String displayName, String description, PrismObject<O> object,
			ContainerStatus status) {
		assertNotNull("null wrapper", objectWrapper);
		assertEquals("Wrong object in wrapper "+objectWrapper, object, objectWrapper.getObject());
		assertEquals("Wrong old object in wrapper "+objectWrapper, object, objectWrapper.getObjectOld());
		assertFalse("object and old object not clonned in "+objectWrapper, objectWrapper.getObject() == objectWrapper.getObjectOld());
//		assertEquals("Wrong displayName in wrapper "+objectWrapper, displayName, objectWrapper.getDisplayName());
//		assertEquals("Wrong description in wrapper "+objectWrapper, description, objectWrapper.getDescription());
		assertEquals("Wrong status in wrapper "+objectWrapper, status, objectWrapper.getStatus());
		assertNull("Unexpected old delta in "+objectWrapper, objectWrapper.getOldDelta());
	}

}
