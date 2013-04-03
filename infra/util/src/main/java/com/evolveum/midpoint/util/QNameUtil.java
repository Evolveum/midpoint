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

package com.evolveum.midpoint.util;

import javax.xml.namespace.QName;
import org.w3c.dom.Node;

/**
 *
 * QName <-> URI conversion.
 * 
 * Very simplistic but better than nothing.
 *
 * @author semancik
 */
public class QNameUtil {

    public static String qNameToUri(QName qname) {
        String qUri = qname.getNamespaceURI();
        StringBuilder sb = new StringBuilder(qUri);

        // TODO: Check if there's already a fragment
        // e.g. http://foo/bar#baz


        if (!qUri.endsWith("#") && !qUri.endsWith("/")) {
            sb.append("#");
        }
        sb.append(qname.getLocalPart());

        return sb.toString();
    }

    public static QName uriToQName(String uri) {

        if (uri == null) {
            throw new IllegalArgumentException("URI is null");
        }
        int index = uri.lastIndexOf("#");
        if (index != -1) {
            String ns = uri.substring(0, index);
            String name = uri.substring(index+1);
            return new QName(ns,name);
        }
        index = uri.lastIndexOf("/");
        // TODO check if this is still in the path section, e.g.
        // if the matched slash is not a beginning of authority
        // section
        if (index != -1) {
            String ns = uri.substring(0, index+1);
            String name = uri.substring(index+1);
            return new QName(ns,name);
        }
        throw new IllegalArgumentException("The URI ("+uri+") does not contain slash character");
    }
	
	public static QName getNodeQName(Node node) {
		return new QName(node.getNamespaceURI(),node.getLocalName());
	}
	
	public static boolean compareQName(QName qname, Node node) {
		return (qname.getNamespaceURI().equals(node.getNamespaceURI()) && qname.getLocalPart().equals(node.getLocalName()));
	}
}
