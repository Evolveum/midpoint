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
package com.evolveum.midpoint.model.filter;

import com.evolveum.midpoint.model.controller.Filter;
import com.evolveum.midpoint.prism.PropertyValue;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author lazyman
 */
public class DiacriticsFilterTest {

    private static final String input = "čišćeľščťžýáíéäöåøřĺąćęłńóśźżrůāēīūŗļķņģšžčāäǟḑēīļņōȯȱõȭŗšțūžÇĞIİÖŞÜáàâéèêíìîóòôúùûáâãçéêíóôõú";
    private static final String expected = "ciscelsctzyaieaoaørlacełnoszzruaeiurlkngszcaaadeilnooooorstuzCGIIOSUaaaeeeiiiooouuuaaaceeiooou";
    private Filter filter;

    @BeforeMethod
    public void before() {
        filter = new DiacriticsFilter();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullNode() {
        filter.apply(null);
    }

    @Test
    public void testEmptyValue() {
        PropertyValue<String> value = new PropertyValue<String>("");
        value = filter.apply(value);
        AssertJUnit.assertEquals("", value.getValue());
    }

    @Test
    public void testValueTextNode() {
        PropertyValue<String> value = new PropertyValue<String>(input);
        value = filter.apply(value);
        AssertJUnit.assertEquals(expected, value.getValue());
    }
}
