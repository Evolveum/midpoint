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

package com.evolveum.midpoint.test.diff;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.test.util.TestUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.XpathNodeTracker;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Overrides XMLUnit XpathNodeTracker implementation.
 * Generates XPath for differences including:
 * <ul>
 *  <li> oid identification of the nodes
 *  <li> namespace prefixes for nodes
 * </ul>
 *
 * @see XpathNodeTracker
 * 
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class MidPointXpathNodeTracker extends XpathNodeTracker {

    private final List indentationList = new ArrayList();
    private TrackingEntry currentEntry;

    /**
     * Simple constructor
     */
    public MidPointXpathNodeTracker() {
        newLevel();
    }

    /**
     * Clear state data.
     * Call if required to reuse an existing instance.
     */
    @Override
    public void reset() {
        indentationList.clear();
        indent();
    }

    /**
     * Call before examining child nodes one level of indentation into DOM
     */
    @Override
    public void indent() {
        if (currentEntry != null) {
            currentEntry.clearTrackedAttribute();
        }
        newLevel();
    }

    private void newLevel() {
        currentEntry = new TrackingEntry();
        indentationList.add(currentEntry);
    }

    /**
     * Call after processing attributes of an element and turining to
     * compare the child nodes.
     */
    @Override
    public void clearTrackedAttribute() {
        if (currentEntry != null) {
            currentEntry.clearTrackedAttribute();
        }
    }

    /**
     * Call after examining child nodes, ie before returning back one level of indentation from DOM
     */
    @Override
    public void outdent() {
        int last = indentationList.size() - 1;
        indentationList.remove(last);
        --last;
        if (last >= 0) {
            currentEntry = (TrackingEntry) indentationList.get(last);
        }
    }

    /**
     * Call when visiting a node whose xpath location needs tracking
     * @param node the Node being visited
     */
    @Override
    public void visited(Node node) {
        String nodeName;
        String prefix;
        switch (node.getNodeType()) {
            case Node.ATTRIBUTE_NODE:
                nodeName = ((Attr) node).getLocalName();
                if (nodeName == null || nodeName.length() == 0) {
                    nodeName = node.getNodeName();
                }
                //midPoint patch - Xpath has to have namespace prefix
                prefix = ((Attr) node).getPrefix();
                //if (null != prefix) {
                if (!(StringUtils.isEmpty(prefix))) {
                    nodeName = prefix + ":" + nodeName;
                } else {
                    //nodeName = ":" + nodeName;
                }
                visitedAttribute(nodeName);
                break;
            case Node.ELEMENT_NODE:
                nodeName = ((Element) node).getLocalName();
                if (nodeName == null || nodeName.length() == 0) {
                    nodeName = node.getNodeName();
                }
                //midPoint patch - Xpath has to have namespace prefix
                prefix = ((Element) node).getPrefix();
                if (!(StringUtils.isEmpty(prefix))) {
                    nodeName = prefix + ":" + nodeName;
                } else {
                    //nodeName = ":" + nodeName;
                }
                visitedNode(node, nodeName);
                break;
            case Node.COMMENT_NODE:
                visitedNode(node, XPATH_COMMENT_IDENTIFIER);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                visitedNode(node, XPATH_PROCESSING_INSTRUCTION_IDENTIFIER);
                break;
            case Node.CDATA_SECTION_NODE:
            case Node.TEXT_NODE:
                visitedNode(node, XPATH_CHARACTER_NODE_IDENTIFIER);
                break;
            default:
                // ignore unhandled node types
                break;
        }
    }

    @Override
    protected void visitedNode(Node visited, String value) {
        currentEntry.trackNode(visited, value);
    }

    @Override
    protected void visitedAttribute(String visited) {
        currentEntry.trackAttribute(visited);
    }

    /**
     * Preload the items in a NodeList by visiting each in turn
     * Required for pieces of test XML whose node children can be visited
     * out of sequence by a DifferenceEngine comparison
     * @param nodeList the items to preload
     */
    @Override
    public void preloadNodeList(NodeList nodeList) {
        currentEntry.trackNodesAsWellAsValues(true);
        int length = nodeList.getLength();
        for (int i = 0; i < length; ++i) {
            visited(nodeList.item(i));
        }
        currentEntry.trackNodesAsWellAsValues(false);
    }

    /**
     * Preload the items in a List by visiting each in turn
     * Required for pieces of test XML whose node children can be visited
     * out of sequence by a DifferenceEngine comparison
     * @param nodeList the items to preload
     */
    @Override
    public void preloadChildList(List nodeList) {
        currentEntry.trackNodesAsWellAsValues(true);
        int length = nodeList.size();
        for (int i = 0; i < length; ++i) {
            visited((Node) nodeList.get(i));
        }
        currentEntry.trackNodesAsWellAsValues(false);
    }

    /**
     * @return the last visited node as an xpath-location String
     */
    @Override
    public String toXpathString() {
        StringBuffer buf = new StringBuffer();
        TrackingEntry nextEntry;
        for (Iterator iter = indentationList.iterator(); iter.hasNext();) {
            nextEntry = (TrackingEntry) iter.next();
            nextEntry.appendEntryTo(buf);
        }
        return buf.toString();
    }

    /**
     * Wrapper class around a mutable <code>int</code> value
     * Avoids creation of many immutable <code>Integer</code> objects
     */
    private static final class Int {

        private int value;

        public Int(int startAt) {
            value = startAt;
        }

        public void increment() {
            ++value;
        }

        public int getValue() {
            return value;
        }

        public Integer toInteger() {
            return Integer.valueOf(value);
        }
    }

    /**
     * Holds node tracking details - one instance is used for each level of indentation in a DOM
     * Provides reference between a String-ified Node value and the xpath index of that value
     */
    private static final class TrackingEntry {

        private final Map valueMap = new HashMap();
        private String currentValue, currentAttribute;
        private Map nodeReferenceMap;
        private boolean trackNodeReferences = false;
        //private Integer nodeReferenceLookup = null;
        private String oid;

        private Int lookup(String value) {
            return (Int) valueMap.get(value);
        }

        /**
         * Keep a reference to the current visited (non-attribute) node
         * @param visited the non-attribute node visited
         * @param value the String-ified value of the non-attribute node visited
         */
        public void trackNode(Node visited, String value) {
            if (nodeReferenceMap == null || trackNodeReferences) {
                Int occurrence = lookup(value);
                if (occurrence == null) {
                    occurrence = new Int(1);
                    valueMap.put(value, occurrence);
                } else {
                    occurrence.increment();
                }
                if (trackNodeReferences) {
                    nodeReferenceMap.put(visited, occurrence.toInteger());
                }
            } else {
                //nodeReferenceLookup = (Integer) nodeReferenceMap.get(visited);
            }
            currentValue = value;

            oid = TestUtil.getNodeOid(visited);

            clearTrackedAttribute();
        }

        /**
         * Keep a reference to the visited attribute at the current visited node
         * @param value the attribute visited
         */
        public void trackAttribute(String visited) {
            currentAttribute = visited;
        }

        /**
         * Clear any reference to the current visited attribute
         */
        public void clearTrackedAttribute() {
            currentAttribute = null;
        }

        /**
         * Append the details of the current visited node to a StringBuffer
         * @param buf the StringBuffer to append to
         */
        public void appendEntryTo(StringBuffer buf) {
            if (currentValue == null) {
                return;
            }
            buf.append(XPATH_SEPARATOR).append(currentValue);

            if (null != oid) {
                buf.append(XPATH_NODE_INDEX_START).append("@").append(SchemaConstants.C_OID_ATTRIBUTE.getLocalPart()).append("=\"").append(oid).append("\"").append(XPATH_NODE_INDEX_END);
            }

            if (currentAttribute != null) {
                buf.append(XPATH_SEPARATOR).append(XPATH_ATTRIBUTE_IDENTIFIER).append(currentAttribute);
            }
        }

        public void trackNodesAsWellAsValues(boolean yesNo) {
            this.trackNodeReferences = yesNo;
            if (yesNo) {
                nodeReferenceMap = new HashMap();
            }
        }
    }
}
