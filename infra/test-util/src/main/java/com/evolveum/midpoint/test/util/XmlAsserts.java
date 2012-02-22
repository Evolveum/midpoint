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
package com.evolveum.midpoint.test.util;

import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.*;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

public class XmlAsserts {


    private static String readFileAsString(File fileNewXml) throws java.io.IOException {
        byte[] buffer = new byte[(int) fileNewXml.length()];
        FileInputStream f = new FileInputStream(fileNewXml);
        f.read(buffer);
        return new String(buffer);
    }

    private static String lookupNamespaceURI(String prefix, Node node) {
        String namespace = node.lookupNamespaceURI(prefix);

        if (StringUtils.isEmpty(namespace) && node.getParentNode() != null) {
            return lookupNamespaceURI(prefix, node.getParentNode());
        }

        return null;
    }
}
