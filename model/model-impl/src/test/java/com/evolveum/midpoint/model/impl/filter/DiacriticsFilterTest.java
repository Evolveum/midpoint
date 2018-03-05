/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.filter;

import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.prism.PrismPropertyValue;

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
        PrismPropertyValue<String> value = new PrismPropertyValue<String>("");
        value = filter.apply(value);
        AssertJUnit.assertEquals("", value.getValue());
    }

    @Test
    public void testValueTextNode() {
        PrismPropertyValue<String> value = new PrismPropertyValue<String>(input);
        value = filter.apply(value);
        AssertJUnit.assertEquals(expected, value.getValue());
    }
}
