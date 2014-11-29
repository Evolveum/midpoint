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

package com.evolveum.midpoint.util;

import java.util.Arrays;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
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

	/**
	 * Matching with considering wildcard namespace (null).
	 */
	public static boolean match(QName a, QName b) {
		return match(a, b, false);
	}

	// case insensitive is related to local parts
	public static boolean match(QName a, QName b, boolean caseInsensitive) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (!caseInsensitive) {
			// traditional comparison
			if (StringUtils.isBlank(a.getNamespaceURI()) || StringUtils.isBlank(b.getNamespaceURI())) {
				return a.getLocalPart().equals(b.getLocalPart());
			} else {
				return a.equals(b);
			}
		} else {
			// relaxed (case-insensitive) one
			if (!a.getLocalPart().equalsIgnoreCase(b.getLocalPart())) {
				return false;
			}
			if (StringUtils.isBlank(a.getNamespaceURI()) || StringUtils.isBlank(b.getNamespaceURI())) {
				return true;
			} else {
				return a.getNamespaceURI().equals(b.getNamespaceURI());
			}
		}

	}
	
	public static QName resolveNs(QName a, Collection<QName> col){
		if (col == null) {
			return null;
		}
		QName found = null;
		for (QName b: col) {
			if (match(a, b)) {
				if (found != null){
					throw new IllegalStateException("Found more than one suitable qnames( "+ found + b + ") for attribute: " + a);
				}
				found = b;
			}
		}
		return found;
	}
	
	public static boolean matchAny(QName a, Collection<QName> col) {
		if (resolveNs(a, col) == null){
			return false;
		}
		return true;
//		if (col == null) {
//			return false;
//		}
//		for (QName b: col) {
//			if (match(a, b)) {
//				return true;
//			}
//		}
//		return false;
	}
	
	public static Collection<QName> createCollection(QName... qnames) {
		return Arrays.asList(qnames);
	}

	public static QName nullNamespace(QName qname) {
		return new QName(null, qname.getLocalPart(), qname.getPrefix());
	}
}
