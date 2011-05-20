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

import java.util.LinkedList;
import java.util.List;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;

/**
 * 
 *
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class DiffConstants {

    public static List<Difference> IGNORE_DIFFERENCES;
    public static List<String> CONTAINER_PROPERTIES;
    public static List<String> REPLACE_PROPERTIES;

    static {
        //TODO: for now following changes are ignored, improve in the future to accept more changes, e.g. in comments
        IGNORE_DIFFERENCES = new LinkedList<Difference>();
        IGNORE_DIFFERENCES.add(DifferenceConstants.HAS_DOCTYPE_DECLARATION);
        IGNORE_DIFFERENCES.add(DifferenceConstants.DOCTYPE_NAME);
        IGNORE_DIFFERENCES.add(DifferenceConstants.DOCTYPE_PUBLIC_ID);
        IGNORE_DIFFERENCES.add(DifferenceConstants.DOCTYPE_SYSTEM_ID);
        IGNORE_DIFFERENCES.add(DifferenceConstants.NAMESPACE_PREFIX);
        IGNORE_DIFFERENCES.add(DifferenceConstants.CHILD_NODELIST_SEQUENCE);
        IGNORE_DIFFERENCES.add(DifferenceConstants.CHILD_NODELIST_LENGTH);
        IGNORE_DIFFERENCES.add(DifferenceConstants.ATTR_SEQUENCE);
        IGNORE_DIFFERENCES.add(DifferenceConstants.COMMENT_VALUE);

        //TODO: for now replace and container properties are hardcoded, but later we have to find a solution how to define it in schema or some other place
        CONTAINER_PROPERTIES = new LinkedList<String>();
        CONTAINER_PROPERTIES.add("account");
        CONTAINER_PROPERTIES.add("user");
        CONTAINER_PROPERTIES.add("resource");
        CONTAINER_PROPERTIES.add("extension");
        CONTAINER_PROPERTIES.add("attributes");

        REPLACE_PROPERTIES = new LinkedList<String>();
        REPLACE_PROPERTIES.add("schemaHandling");
        REPLACE_PROPERTIES.add("schema");
        REPLACE_PROPERTIES.add("configuration");
        REPLACE_PROPERTIES.add("synchronizationState");
        REPLACE_PROPERTIES.add("synchronization");
        REPLACE_PROPERTIES.add("scripts");

    }

    public static boolean isForContainerProperty(Difference difference) {
        String propertyName = difference.getControlNodeDetail().getNode().getParentNode().getNodeName();
        return DiffConstants.CONTAINER_PROPERTIES.contains(propertyName);
    }

    public static boolean isForReplaceProperty(Difference difference) {
        String controlXpath = difference.getControlNodeDetail().getXpathLocation();
        String testXpath = difference.getTestNodeDetail().getXpathLocation();
        for (String replaceProperty : DiffConstants.REPLACE_PROPERTIES) {
            if (null != controlXpath) {
                if (controlXpath.contains(replaceProperty)) {
                    return true;
                }
            } else {
                if (testXpath.contains(replaceProperty)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getReplacePropertyName(Difference difference) {
        String controlXpath = difference.getControlNodeDetail().getXpathLocation();
        String testXpath = difference.getTestNodeDetail().getXpathLocation();
        for (String replaceProperty : DiffConstants.REPLACE_PROPERTIES) {
            if (null != controlXpath) {
                if (controlXpath.contains(replaceProperty)) {
                    return replaceProperty;
                }
            } else {
                if (testXpath.contains(replaceProperty)) {
                    return replaceProperty;
                }
            }
        }
        throw new IllegalArgumentException("Difference is not for replace property");
    }
}
