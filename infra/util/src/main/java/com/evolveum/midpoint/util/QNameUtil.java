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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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

    public static final Trace LOGGER = TraceManager.getTrace(QNameUtil.class);

    // TODO consider where to put all this undeclared-prefixes-things
    // Hopefully in 3.2 everything will find its place

    private static final String UNDECLARED_PREFIX_MARK = "__UNDECLARED__";

    // Whether we want to tolerate undeclared XML prefixes in QNames
    // This is here only for backward compatibility with versions 3.0-3.1.
    // Will be set to false starting with 3.2 (MID-2191)
    private static boolean tolerateUndeclaredPrefixes = false;

    // ThreadLocal "safe mode" override for the above value (MID-2218)
    // This can be set to true for raw reads, allowing to manually fix broken objects
    private static ThreadLocal<Boolean> temporarilyTolerateUndeclaredPrefixes = new ThreadLocal<>();

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

    public static boolean isUnqualified(QName targetTypeQName) {
        return StringUtils.isBlank(targetTypeQName.getNamespaceURI());
    }

    public static boolean isTolerateUndeclaredPrefixes() {
        return tolerateUndeclaredPrefixes;
    }

    public static void setTolerateUndeclaredPrefixes(boolean value) {
        tolerateUndeclaredPrefixes = value;
    }

    public static void setTemporarilyTolerateUndeclaredPrefixes(Boolean value) {
        temporarilyTolerateUndeclaredPrefixes.set(value);
    }

    public static void reportUndeclaredNamespacePrefix(String prefix, String context) {
        if (tolerateUndeclaredPrefixes ||
                (temporarilyTolerateUndeclaredPrefixes != null && Boolean.TRUE.equals(temporarilyTolerateUndeclaredPrefixes.get()))) {
            LOGGER.error("Undeclared namespace prefix '" + prefix+"' in '"+context+"'.");
        } else {
            throw new IllegalArgumentException("Undeclared namespace prefix '"+prefix+"' in '"+context+"'");
        }
    }

    // @pre namespacePrefix != null
    public static String markPrefixAsUndeclared(String namespacePrefix) {
        if (namespacePrefix.startsWith(UNDECLARED_PREFIX_MARK)) {
            return namespacePrefix;
        } else {
            return UNDECLARED_PREFIX_MARK + namespacePrefix;
        }
    }

    public static boolean isPrefixUndeclared(String namespacePrefix) {
        return namespacePrefix != null && namespacePrefix.startsWith(UNDECLARED_PREFIX_MARK);
    }

	public static boolean isUri(String string) {
		if (string == null) {
			return false;
		}
		return string.matches("^\\w+:.*");
	}

	public static String getLocalPart(QName name) {
        return name != null ? name.getLocalPart() : null;
    }

}
