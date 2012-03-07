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
 * Portions Copyrighted 2011 Igor Farinic
 */

package com.evolveum.midpoint.prism.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Maps namespaces to preferred prefixes. Should be used through the code to
 * avoid generation of prefixes.
 * 
 * Although this is usually used as singleton (static), it can also be instantiated to locally
 * override some namespace mappings. This is useful for prefixes like "tns" (schema) or "ri" (resource schema).
 * 
 * @see MID-349
 * 
 * @author Igor Farinic
 * @author Radovan Semancik
 * 
 */
public class GlobalDynamicNamespacePrefixMapper extends NamespacePrefixMapper implements DynamicNamespacePrefixMapper, Dumpable {

	private static final Map<String, String> globalNamespacePrefixMap = new HashMap<String, String>();
	private final Map<String, String> localNamespacePrefixMap = new HashMap<String, String>();
	private String defaultNamespace = null;
	
	public String getDefaultNamespace() {
		return defaultNamespace;
	}

	public void setDefaultNamespace(String defaultNamespace) {
		this.defaultNamespace = defaultNamespace;
	}

	public void registerPrefix(String namespace, String prefix) {
		globalNamespacePrefixMap.put(namespace, prefix);
	}
	
	public void registerPrefixLocal(String namespace, String prefix) {
		localNamespacePrefixMap.put(namespace, prefix);
	}
	
	public String getPrefix(String namespace) {
		String prefix = localNamespacePrefixMap.get(namespace);
		if (prefix == null) {
			return getPreferredPrefix(namespace);
		}
		return prefix;
	}
	
	public QName setQNamePrefix(QName qname) {
		String namespace = qname.getNamespaceURI();
		String prefix = getPrefix(namespace);
		if (prefix == null) {
			return qname;
		}
		return new QName(qname.getNamespaceURI(),qname.getLocalPart(),prefix);
	}
	
	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
		//for JAXB we are mapping midpoint common namespace to default namespace
		if (defaultNamespace != null && defaultNamespace.equals(namespaceUri)) {
			return "";
		} 
		return getPreferredPrefix(namespaceUri, suggestion);
	}

	/**
	 * 
	 * @param namespace
	 * @return preferred prefix for the namespace, if no prefix is assigned yet,
	 *         then it will assign a prefix and return it.
	 */
	public static synchronized String getPreferredPrefix(String namespace) {
		return getPreferredPrefix(namespace, null);
	}

	/**
	 * @param namespace
	 * @param hintPrefix
	 * @return preferred prefix for the namespace, if no prefix is assigned yet,
	 *         then it assign hint prefix (if it is not assigned yet) or assign
	 *         a new prefix and return it (if hint prefix is already assigned to
	 *         other namespace).
	 */
	public static synchronized String getPreferredPrefix(String namespace, String hintPrefix) {
		String prefix = globalNamespacePrefixMap.get(namespace);

		if (StringUtils.isEmpty(prefix)) {
			if (StringUtils.isEmpty(hintPrefix)) {
				// FIXME: improve new prefix assignment
				prefix = "gen" + (new Random()).nextInt(999);
			} else {
				if (globalNamespacePrefixMap.containsValue(hintPrefix)) {
					// FIXME: improve new prefix assignment
					prefix = "gen" + (new Random()).nextInt(999);
				} else {
					prefix = hintPrefix;
				}
			}
			globalNamespacePrefixMap.put(namespace, prefix);
		}

		return prefix;

	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.Dumpable#dump()
	 */
	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder("GlobalDynamicNamespacePrefixMapper(");
		sb.append(defaultNamespace);
		sb.append("):\n");
		DebugUtil.indentDebugDump(sb, 1);
		sb.append("Global map:\n");
		DebugUtil.debugDumpMapMultiLine(sb, globalNamespacePrefixMap, 2);
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, 1);
		sb.append("Local map:\n");
		DebugUtil.debugDumpMapMultiLine(sb, localNamespacePrefixMap, 2);
		return sb.toString();
	}

}