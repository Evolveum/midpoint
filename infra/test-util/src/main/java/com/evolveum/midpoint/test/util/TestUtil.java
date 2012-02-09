/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.test.util;

import com.evolveum.midpoint.prism.PropertyValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Unit test utilities.
 *
 * @author Radovan Semancik
 */
public class TestUtil {

    public static <T> void assertPropertyValueSetEquals(Collection<PropertyValue<T>> actual, T... expected) {
        Set<T> set = new HashSet<T>();
        for (PropertyValue<T> value : actual) {
            set.add(value.getValue());
        }
        assertSetEquals(set, expected);
    }

    public static <T> void assertSetEquals(Collection<T> actual, T... expected) {
        assertSetEquals(null, actual, expected);
    }

    public static <T> void assertSetEquals(String message, Collection<T> actual, T... expected) {
        Set<T> expectedSet = new HashSet<T>();
        expectedSet.addAll(Arrays.asList(expected));
        Set<T> actualSet = new HashSet<T>();
        actualSet.addAll(actual);
        if (message != null) {
            assertEquals(message, expectedSet, actualSet);
        } else {
            assertEquals(expectedSet, actualSet);
        }
    }
}
