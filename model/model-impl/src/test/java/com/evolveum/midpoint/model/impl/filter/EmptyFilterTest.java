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

import com.evolveum.midpoint.model.impl.filter.EmptyFilter;
import com.evolveum.midpoint.prism.PrismPropertyValue;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.common.filter.Filter;

/**
 * @author lazyman
 */
public class EmptyFilterTest {

    private Filter filter;

    @BeforeMethod
    public void before() {
        filter = new EmptyFilter();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullNode() {
        filter.apply(null);
    }

    @Test
    public void testNode() {
        String input = "test content";
        PrismPropertyValue<String> value = new PrismPropertyValue<>(input);
        value = filter.apply(value);

        AssertJUnit.assertEquals(input, value.getValue());
    }
}
