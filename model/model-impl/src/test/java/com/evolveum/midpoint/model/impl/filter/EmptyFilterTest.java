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

import com.evolveum.midpoint.prism.PrismPropertyValue;

import com.evolveum.midpoint.prism.PrismPropertyValueImpl;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import com.evolveum.midpoint.common.filter.Filter;
import org.xml.sax.SAXException;

import java.io.IOException;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

/**
 * @author lazyman
 */
public class EmptyFilterTest {

    private Filter filter;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

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
        PrismPropertyValue<String> value = getPrismContext().itemFactory().createPrismPropertyValue(input);
        value = filter.apply(value);

        AssertJUnit.assertEquals(input, value.getValue());
    }
}
