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
package com.evolveum.midpoint.common.test;

import com.evolveum.midpoint.common.diff.MidPointDifferenceListener;
import com.evolveum.midpoint.common.diff.OidQualifier;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.*;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class XmlAsserts {

    private static void setupXmlUnitForTest() {
        // XmlUnit setup
        // Note: compareUnmatched has to be set to false to calculate diff
        // properly, to avoid matching of nodes that are not comparable
        XMLUnit.setCompareUnmatched(false);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalize(true);
        XMLUnit.setNormalizeWhitespace(true);
    }

    private static String readFileAsString(File fileNewXml) throws java.io.IOException {
        byte[] buffer = new byte[(int) fileNewXml.length()];
        FileInputStream f = new FileInputStream(fileNewXml);
        f.read(buffer);
        return new String(buffer);
    }

    public static void assertPatch(File fileNewXml, String patchedXml) throws Exception {
        assertPatch(readFileAsString(fileNewXml), patchedXml);

    }

    @SuppressWarnings("unchecked")
    public static void assertPatch(String origXml, String patchedXml) throws Exception {
        setupXmlUnitForTest();
        Diff d = new Diff(new InputSource(new ByteArrayInputStream(origXml.getBytes("utf-8"))),
                new InputSource(new ByteArrayInputStream(patchedXml.getBytes("utf-8"))));
        DetailedDiff dd = new DetailedDiff(d);
        dd.overrideElementQualifier(new OidQualifier());
        dd.overrideDifferenceListener(new MidPointDifferenceListener());
        List<Difference> differences = dd.getAllDifferences();

        for (Difference diff : differences) {
            switch (diff.getId()) {
                // case DifferenceConstants.NAMESPACE_PREFIX_ID:
                // //ignore namespaces
                // //FIXME: ^^^
                // break;
                case DifferenceConstants.ATTR_VALUE_ID:
                    if (diff.getControlNodeDetail().getNode().getNodeName().contains("type")) {
                        // ignore attribute values for xsi type, because of
                        // namespaces
                        // FIXME: ^^^
                        break;
                    }
                case DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID:
                case DifferenceConstants.SCHEMA_LOCATION_ID:
                    break;
                case DifferenceConstants.TEXT_VALUE_ID:
                    String value = diff.getControlNodeDetail().getValue();
                    String testValue = diff.getTestNodeDetail().getValue();
                    if (value != null && value.contains(":")) {
                        String namespace = lookupNamespaceURI(value.split(":")[0],
                                diff.getControlNodeDetail().getNode());

                        if (testValue != null && testValue.contains(":")) {
                            String namespaceTest = lookupNamespaceURI(value.split(":")[0],
                                    diff.getTestNodeDetail().getNode());

                            boolean equal = namespace != null ? namespace.equals(namespaceTest) : namespaceTest == null;
                            if (!equal) {
                                throw new AssertionError(diff.toString() + "\nNamespaces probably doesn't match" +
                                        " for qname value: expected {" + namespace + "} real {" + namespaceTest + "}");
                            } else {
                                break;
                            }
                        } else {
                            throw new AssertionError(diff.toString());
                        }
                    }
                default:
                    throw new AssertionError(diff.toString());
            }
        }

    }

    private static String lookupNamespaceURI(String prefix, Node node) {
        String namespace = node.lookupNamespaceURI(prefix);

        if (StringUtils.isEmpty(namespace) && node.getParentNode() != null) {
            return lookupNamespaceURI(prefix, node.getParentNode());
        }

        return null;
    }
}
