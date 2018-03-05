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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pattern-based filter. Can replace portions of input matched by patterns with
 * a static values. Works only on strings now.
 *
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public class PatternFilter extends AbstractFilter {

    public static final QName ELEMENT_REPLACE = new QName(SchemaConstants.NS_FILTER, "replace");
    public static final QName ELEMENT_PATTERN = new QName(SchemaConstants.NS_FILTER, "pattern");
    public static final QName ELEMENT_REPLACEMENT = new QName(SchemaConstants.NS_FILTER, "replacement");
    private static final Trace LOGGER = TraceManager.getTrace(PatternFilter.class);

    @Override
    public <T extends Object> PrismPropertyValue<T> apply(PrismPropertyValue<T> propertyValue) {
        Validate.notNull(propertyValue, "Node must not be null.");
        String value = getStringValue(propertyValue);
        if (StringUtils.isEmpty(value)) {
            return propertyValue;
        }

        Validate.notEmpty(getParameters(), "Parameters must not be null or empty.");
        List<Replace> replaces = getReplaces();
        for (Replace replace : replaces) { Matcher matcher = replace.getPattern().matcher(value);
            value = matcher.replaceAll(replace.getReplacement());
        }
        propertyValue.setValue((T) value);

        return propertyValue;
    }

    private List<Replace> getReplaces() {
        List<Replace> replaces = new ArrayList<>();

        List<Object> parameters = getParameters();
        for (Object object : parameters) {
            if (!(object instanceof Element)) {
                continue;
            }

            Element element = (Element) object;
            if (!ELEMENT_REPLACE.getLocalPart().equals(element.getLocalName())) {
                LOGGER.debug("Ignoring unknown parameter {} in PatternFilter",
                        new Object[]{element.getLocalName()});
                continue;
            }

            NodeList patternNodeList = element.getElementsByTagNameNS(ELEMENT_PATTERN.getNamespaceURI(),
                    ELEMENT_PATTERN.getLocalPart());
            if (patternNodeList.getLength() != 1) {
                throw new IllegalArgumentException("Wrong number of " + ELEMENT_PATTERN + " elements ("
                        + patternNodeList.getLength() + ")");
            }
            String patternStr = ((Element) patternNodeList.item(0)).getTextContent();
            Pattern pattern = Pattern.compile(patternStr);

            NodeList replacementNodeList = element.getElementsByTagNameNS(
                    ELEMENT_REPLACEMENT.getNamespaceURI(), ELEMENT_REPLACEMENT.getLocalPart());
            if (replacementNodeList.getLength() != 1) {
                throw new IllegalArgumentException("Wrong number of " + ELEMENT_REPLACEMENT + " elements ("
                        + replacementNodeList.getLength() + ")");
            }
            String replacement = ((Element) replacementNodeList.item(0)).getTextContent();

            replaces.add(new Replace(pattern, replacement));
        }

        return replaces;
    }

    private static class Replace {

        private String replacement;
        private Pattern pattern;

        public Replace(Pattern pattern, String replacement) {
            this.replacement = replacement;
            this.pattern = pattern;
        }

        /**
         * Get the value of replacement
         *
         * @return the value of replacement
         */
        public String getReplacement() {
            return replacement;
        }

        /**
         * Get the value of pattern
         *
         * @return the value of pattern
         */
        public Pattern getPattern() {
            return pattern;
        }
    }
}
