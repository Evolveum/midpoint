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

package com.evolveum.midpoint.util.diff;

import org.custommonkey.xmlunit.ElementNameQualifier;
import org.custommonkey.xmlunit.ElementQualifier;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Oid Qualifier implements XmlUnit interface that compares nodes based on node names and oid
 *
 * @see ElementQualifier
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class OidQualifier implements ElementQualifier {

    private static final ElementNameQualifier NAME_QUALIFIER =
            new ElementNameQualifier();

    /**
     * Uses element names and the oid to compare
     * elements. 
     *
     */
    public OidQualifier() {
    }

    public boolean qualifyForComparison(Element currentControl,
            Element currentTest) {
        return compareNodes(currentControl, currentTest);
    }

    private boolean compareNodes(Node currentControl, Node currentTest) {
        // if they are elements, compare names of the two nodes
        if (!NAME_QUALIFIER.qualifyForComparison((Element) currentControl,
                (Element) currentTest)) {
            return false;
        }

        Node currentControlOidNode = currentControl.getAttributes().getNamedItem("oid");
        Node currentTestOidNode = currentTest.getAttributes().getNamedItem("oid");

        //if one of them contains oid param and second one not, then they are not comparable
        if (((null != currentControlOidNode) && (null == currentTestOidNode)) ||
            ((null == currentControlOidNode) && (null != currentTestOidNode)) ) {
            return false;
        }

        //if none of them contains oid param, then they are comparable
        if  ((null == currentControlOidNode) && (null == currentTestOidNode)) {
            return true;
        }

        String currentControlOid = currentControlOidNode.getTextContent();
        String currentTestOid = currentTestOidNode.getTextContent();

        if (!currentTestOid.equals(currentControlOid)) {
            //if both contains oid params and oids are not the same, then they are not comparable
            return false;
        } else {
            //if both contains oid params and oids are the same, then they are comparable
            return true;
        }
    }
}
