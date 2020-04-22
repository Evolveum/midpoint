/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.*;

import java.util.Arrays;
import java.util.List;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public class WrapperTestUtil {

    public static <C extends Containerable, T> void fillInPropertyWrapper(
            ModelServiceLocator modelServiceLocator, PrismContainerValueWrapper<C> containerWrapper,
            ItemPath itemPath, T... newValues)
            throws SchemaException {
        PrismPropertyWrapper itemWrapper = containerWrapper.findProperty(itemPath);
        assertNotNull("No item wrapper for path " + itemPath + " in " + containerWrapper, itemWrapper);
        fillInPropertyWrapper(modelServiceLocator, itemWrapper, itemPath.lastName(), newValues);
    }

    private static <T> void fillInPropertyWrapper(ModelServiceLocator modelServiceLocator,
            PrismPropertyWrapper itemWrapper, ItemName itemName, T... newValues)
            throws SchemaException {
        for (T newValue : newValues) {
            List<PrismValueWrapper> valueWrappers = itemWrapper.getValues();
            PrismValueWrapper lastValueWrapper = valueWrappers.get(valueWrappers.size() - 1);
            PrismPropertyValue<T> pval = (PrismPropertyValue<T>) lastValueWrapper.getNewValue();
            pval.setValue(newValue);
        }
    }

    public static <C extends Containerable, T> void assertPropertyWrapper(
            PrismContainerValueWrapper<C> containerWrapper, ItemPath itemPath, T... expectedValues)
            throws SchemaException {
        ItemWrapper itemWrapper = containerWrapper.findItem(itemPath, ItemWrapper.class);
        assertNotNull("No item wrapper for path " + itemPath + " in " + containerWrapper, itemWrapper);
        assertPropertyWrapper(containerWrapper, itemWrapper, itemPath.lastName(), expectedValues);
    }

    // todo better name
    public static <C extends Containerable, T> void assertPropertyWrapperByName(
            PrismContainerValueWrapper<C> containerWrapper, ItemName itemName, T... expectedValues)
            throws SchemaException {
        ItemWrapper itemWrapper = containerWrapper.findItem(itemName, ItemWrapper.class);
        assertNotNull("No item wrapper " + itemName + " in " + containerWrapper, itemWrapper);
        assertPropertyWrapper(containerWrapper, itemWrapper, itemName, expectedValues);
    }

    private static <C extends Containerable, T> void assertPropertyWrapper(
            PrismContainerValueWrapper<C> containerWrapper, ItemWrapper itemWrapper,
            ItemName itemName, T... expectedValues) {
        List<PrismValueWrapper> valueWrappers = itemWrapper.getValues();
        assertPropertyWrapperValues("item wrapper " + itemName + " in " + containerWrapper, valueWrappers, expectedValues);
    }

    public static <T> void assertPropertyWrapperValues(
            String desc, List<PrismValueWrapper> valueWrappers, T... expectedValues) {
        if (expectedValues == null) {
            expectedValues = (T[]) new Object[] { null };
        }
        assertEquals("Wrong number of values in " + desc + "; was: " + valueWrappers + ", expected: " + Arrays.toString(expectedValues), expectedValues.length, valueWrappers.size());
        if (expectedValues.length == 0) {
            return;
        }
        for (PrismValueWrapper vw : valueWrappers) {
            PrismValue actualPval = vw.getNewValue();
            if (actualPval instanceof PrismPropertyValue<?>) {
                T actualValue = ((PrismPropertyValue<T>) actualPval).getValue();
                if (expectedValues == null || expectedValues.length == 0 || (expectedValues.length == 1 && expectedValues[0] == null)) {
                    if (!isEmptyValue((PrismPropertyValue<T>) vw.getOldValue())) {
                        AssertJUnit.fail("Unexpected value " + actualValue + " in value wrapper in " + desc + "; was: " + valueWrappers + ", expected: " + Arrays.toString(expectedValues));
                    }
                } else {
                    boolean found = false;
                    for (T expectedValue : expectedValues) {
                        if (MiscUtil.equals(expectedValue, actualValue)) {
                            found = true;
                        }
                    }
                    if (!found) {
                        AssertJUnit.fail("Unexpected value " + actualValue + " in value wrapper in " + desc + "; was: " + valueWrappers + ", expected: " + Arrays.toString(expectedValues));
                    }
                }
            } else {
                AssertJUnit.fail("expected PrismPropertyValue in value wrapper in " + desc + ", but got " + actualPval.getClass());
            }

        }
    }

    private static <T> boolean isEmptyValue(PrismPropertyValue<T> pval) {
        T val = pval.getValue();
        if (val == null) {
            return true;
        }
        if (val instanceof String && ((String) val).isEmpty()) {
            return true;
        }
        if (val instanceof PolyString && ((PolyString) val).isEmpty()) {
            return true;
        }
        return false;
    }

    public static <C extends Containerable, O extends ObjectType> void assertWrapper(
            PrismContainerWrapper<C> containerWrapper, String displayName,
            ItemPath expectedPath, PrismObject<O> object, ItemStatus status)
            throws SchemaException {
        PrismContainer<C> container;
        if (expectedPath == null) {
            container = (PrismContainer<C>) object;
        } else {
            container = object.findContainer(expectedPath);
        }
        assertWrapper(containerWrapper, displayName, expectedPath, container, expectedPath == null, status);
    }

    public static <C extends Containerable> void assertWrapper(
            PrismContainerWrapper<C> containerWrapper, String displayName, ItemPath expectedPath,
            PrismContainer<C> container, boolean isMain, ItemStatus status)
            throws SchemaException {
        assertNotNull("null wrapper", containerWrapper);
        PrismAsserts.assertEquivalent("Wrong path in wrapper " + containerWrapper,
                expectedPath == null ? ItemPath.EMPTY_PATH : expectedPath, containerWrapper.getPath());
        if (container != null) {
            assertEquals("Wrong item in wrapper " + containerWrapper, container, containerWrapper.getItem());
        }
        assertEquals("Wrong displayName in wrapper " + containerWrapper, displayName, containerWrapper.getValue().getDisplayName());
        assertEquals("Wrong status in wrapper " + containerWrapper, status, containerWrapper.getStatus());
    }

    public static <O extends ObjectType> void assertWrapper(
            PrismObjectWrapper<O> objectWrapper, String displayName, String description,
            PrismObject<O> object, PrismObject<O> objectOld, ItemStatus status) {
        assertNotNull("null wrapper", objectWrapper);
        assertEquals("Wrong object in wrapper " + objectWrapper, object, objectWrapper.getObject());
        if (ItemStatus.ADDED != objectWrapper.getStatus()) {
            assertEquals("Wrong old object in wrapper " + objectWrapper, objectOld, objectWrapper.getObjectOld());
        }
        assertFalse("object and old object not cloned in " + objectWrapper, objectWrapper.getObject() == objectWrapper.getObjectOld());
        assertEquals("Wrong displayName in wrapper " + objectWrapper, displayName, objectWrapper.getValue().getDisplayName());
        assertEquals("Wrong status in wrapper " + objectWrapper, status, objectWrapper.getStatus());
    }

}
