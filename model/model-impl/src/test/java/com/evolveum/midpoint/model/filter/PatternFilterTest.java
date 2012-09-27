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

import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.DOMUtil;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.model.filter.PatternFilter.*;

/**
 * @author lazyman
 */
public class PatternFilterTest {

    private static final String input = "midPoint";
    private static final String expected = "mxdPxxnt";
    private Filter filter;

    @BeforeMethod
    public void before() {
        filter = new PatternFilter();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullNode() {
        filter.apply(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testEmptyParameters() {
        PrismPropertyValue<String> value = new PrismPropertyValue<String>(input);
        value = filter.apply(value);
        AssertJUnit.assertEquals(expected, value.getValue());
    }

    @Test
    public void testEmptyValue() {
        PrismPropertyValue<String> value = new PrismPropertyValue<String>("");
        value = filter.apply(value);
        AssertJUnit.assertEquals("", value.getValue());
    }

    @Test
    public void testValue() {
        List<Object> parameters = createGoodParameters();
        filter.setParameters(parameters);

        PrismPropertyValue<String> value = new PrismPropertyValue<String>(input);
        value = filter.apply(value);
        AssertJUnit.assertEquals(expected, value.getValue());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValueInElementBadParameters() {
        List<Object> parameters = createBadParameters();
        filter.setParameters(parameters);

        PrismPropertyValue<String> value = new PrismPropertyValue<String>(input);
        value = filter.apply(value);
        AssertJUnit.assertEquals(expected, value.getValue());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValueInElementBadParameters2() {
        List<Object> parameters = createBadParameters2();
        filter.setParameters(parameters);

        PrismPropertyValue<String> value = new PrismPropertyValue<String>(input);
        value = filter.apply(value);
        AssertJUnit.assertEquals(expected, value.getValue());
    }

    private List<Object> createGoodParameters() {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(new Object());

        Document document = DOMUtil.getDocument();
        Element replace = document.createElementNS(ELEMENT_REPLACE.getNamespaceURI(),
                ELEMENT_REPLACE.getLocalPart());
        parameters.add(replace);

        Element pattern = document.createElementNS(ELEMENT_PATTERN.getNamespaceURI(),
                ELEMENT_PATTERN.getLocalPart());
        pattern.setTextContent("[aeiouy]");
        replace.appendChild(pattern);

        Element replacement = document.createElementNS(ELEMENT_REPLACEMENT.getNamespaceURI(),
                ELEMENT_REPLACEMENT.getLocalPart());
        replacement.setTextContent("x");
        replace.appendChild(replacement);

        // unknown parameter test
        parameters.add(document.createElementNS("http://example.com", "unknown"));

        return parameters;
    }

    private List<Object> createBadParameters() {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(new Object());

        Document document = DOMUtil.getDocument();
        Element replace = document.createElementNS(ELEMENT_REPLACE.getNamespaceURI(),
                ELEMENT_REPLACE.getLocalPart());
        parameters.add(replace);

        Element pattern1 = document.createElementNS(ELEMENT_PATTERN.getNamespaceURI(),
                ELEMENT_PATTERN.getLocalPart());
        pattern1.setTextContent("[aeiouy]");
        replace.appendChild(pattern1);

        Element pattern2 = document.createElementNS(ELEMENT_PATTERN.getNamespaceURI(),
                ELEMENT_PATTERN.getLocalPart());
        pattern2.setTextContent("[a-z]");
        replace.appendChild(pattern2);

        Element replacement = document.createElementNS(ELEMENT_REPLACEMENT.getNamespaceURI(),
                ELEMENT_REPLACEMENT.getLocalPart());
        replacement.setTextContent("x");
        replace.appendChild(replacement);

        return parameters;
    }

    private List<Object> createBadParameters2() {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(new Object());

        Document document = DOMUtil.getDocument();
        Element replace = document.createElementNS(ELEMENT_REPLACE.getNamespaceURI(),
                ELEMENT_REPLACE.getLocalPart());
        parameters.add(replace);

        Element pattern1 = document.createElementNS(ELEMENT_PATTERN.getNamespaceURI(),
                ELEMENT_PATTERN.getLocalPart());
        pattern1.setTextContent("[aeiouy]");
        replace.appendChild(pattern1);

        Element replacement = document.createElementNS(ELEMENT_REPLACEMENT.getNamespaceURI(),
                ELEMENT_REPLACEMENT.getLocalPart());
        replacement.setTextContent("x");
        replace.appendChild(replacement);

        Element replacement2 = document.createElementNS(ELEMENT_REPLACEMENT.getNamespaceURI(),
                ELEMENT_REPLACEMENT.getLocalPart());
        replacement2.setTextContent("2");
        replace.appendChild(replacement2);

        return parameters;
    }
}
