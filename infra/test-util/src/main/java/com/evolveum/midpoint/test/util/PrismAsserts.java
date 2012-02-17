/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.test.util;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.evolveum.midpoint.prism.PrismPropertyValue;

/**
 * @author semancik
 *
 */
public class PrismAsserts {
	
	public static void assertPropertyValues(Collection expected, Collection<PrismPropertyValue<Object>> results) {
        assertEquals(expected.size(), results.size());

        Set<Object> values = new HashSet<Object>();
        for (PrismPropertyValue result : results) {
            values.add(result.getValue());
        }
        assertEquals(expected, values);

//        Object[] array = expected.toArray();
//        PropertyValue[] array1 = new PropertyValue[results.size()];
//        results.toArray(array1);
//
//        for (int i=0;i<expected.size();i++) {
//            assertEquals(array[i], array1[i].getValue());
//        }
    }

}
