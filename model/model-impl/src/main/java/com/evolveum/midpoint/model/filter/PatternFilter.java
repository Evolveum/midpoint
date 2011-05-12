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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.model.filter;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;

/**
 * Pattern-based filter. Can replace portions of imput matched by
 * patterns with a static values. Works only on strings now.
 *
 * @author Igor Farinic
 * @author Radovan Semancik
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class PatternFilter extends AbstractFilter {

    private static final QName ELEMENT_REPLACE = new QName(SchemaConstants.NS_FILTER,"replace");
    private static final QName ELEMENT_PATTERN = new QName(SchemaConstants.NS_FILTER,"pattern");
    private static final QName ELEMENT_REPLACEMENT = new QName(SchemaConstants.NS_FILTER,"replacement");

    private static transient Trace logger = TraceManager.getTrace(PatternFilter.class);
    private List<Replace> replaces;

    @Override
    public Node apply(Node node) {
        String value = null;
        if (node.getNodeType() == Node.TEXT_NODE) {
            value = ((Text)node).getData();
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            // Little bit simplistic
            // TODO: look inside the node
            value = ((Element)node).getTextContent();
        } else {
            throw new IllegalArgumentException("PatternFilter can only work with text or element nodes, got node type "+node.getNodeType()+" ("+node+")");
        }
        if (value == null) {
            // Nothing to filter
            return node;
        }
        
        List<Replace> repls = getReplaces();
        for (Replace repl : repls) {
            Matcher matcher = repl.getPattern().matcher(value);
            value = matcher.replaceAll(repl.getReplacement());
        }

        Node newNode = node.cloneNode(false);
        if (node.getNodeType() == Node.TEXT_NODE) {
            newNode.setTextContent(value);
        } else {
            // Element Node
            ((Element)newNode).setTextContent(value);
        }

        return newNode;
    }

    // TODO: Error handling and reporting should really be improved here

    private List<Replace> getReplaces() {

        if (replaces != null) {
            return replaces;
        }
        replaces = new ArrayList<Replace>();

        List<Object> parameters = getParameters();

        for (Object o : parameters) {
            if (o instanceof Element) {
                Element e = (Element) o;
                if (ELEMENT_REPLACE.getLocalPart().equals(e.getLocalName())) {
                    NodeList patternNodeList = e.getElementsByTagNameNS(ELEMENT_PATTERN.getNamespaceURI(),ELEMENT_PATTERN.getLocalPart());
                    if (patternNodeList.getLength()!=1) {
                        throw new IllegalArgumentException("Wrong number of "+ELEMENT_PATTERN+" elements ("+patternNodeList.getLength()+")");
                    }
                    String patternStr = ((Element)patternNodeList.item(0)).getTextContent();
                    Pattern pattern = Pattern.compile(patternStr);

                    NodeList replacementNodeList = e.getElementsByTagNameNS(ELEMENT_REPLACEMENT.getNamespaceURI(),ELEMENT_REPLACEMENT.getLocalPart());
                    if (replacementNodeList.getLength()!=1) {
                        throw new IllegalArgumentException("Wrong number of "+ELEMENT_REPLACEMENT+" elements ("+replacementNodeList.getLength()+")");
                    }
                    String replacement = ((Element)replacementNodeList.item(0)).getTextContent();

                    replaces.add(new Replace(pattern, replacement));

                } else {
                    logger.debug("Ignoring unknown parameter {} in PatternFilter",e.getLocalName());
                }
            }
        }

        return replaces;
    }



    static private class Replace {

        private String replacement;

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
         * Set the value of replacement
         *
         * @param replacement new value of replacement
         */
        public void setReplacement(String replacement) {
            this.replacement = replacement;
        }

        private Pattern pattern;

        /**
         * Get the value of pattern
         *
         * @return the value of pattern
         */
        public Pattern getPattern() {
            return pattern;
        }

        /**
         * Set the value of pattern
         *
         * @param pattern new value of pattern
         */
        public void setPattern(Pattern pattern) {
            this.pattern = pattern;
        }


    }

}
