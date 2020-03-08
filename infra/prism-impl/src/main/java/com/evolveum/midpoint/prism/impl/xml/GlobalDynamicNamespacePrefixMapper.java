/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.xml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Maps namespaces to preferred prefixes. Should be used through the code to
 * avoid generation of prefixes.
 *
 * Although this is usually used as singleton (static), it can also be instantiated to locally
 * override some namespace mappings. This is useful for prefixes like "tns" (schema) or "ri" (resource schema).
 *
 * @see <a href="https://jira.evolveum.com/browse/MID-349">MID-349</a>
 *
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public class GlobalDynamicNamespacePrefixMapper extends NamespacePrefixMapper implements DynamicNamespacePrefixMapper, DebugDumpable {

    private static final Map<String, String> GLOBAL_NAMESPACE_PREFIX_MAP = new HashMap<>();
    private Map<String, String> localNamespacePrefixMap = new HashMap<>();
    private String defaultNamespace = null;
    private final Set<String> prefixesDeclaredByDefault = new HashSet<>();

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public synchronized void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    @Override
    public void addDeclaredByDefault(String prefix) {
        prefixesDeclaredByDefault.add(prefix);
    }

    @Override
    public synchronized Map<String, String> getNamespacesDeclaredByDefault() {
        Map<String, String> retval = new HashMap<>();
        for (Map.Entry<String, String> entry : GLOBAL_NAMESPACE_PREFIX_MAP.entrySet()) {
            String prefix = entry.getValue();
            if (prefixesDeclaredByDefault.contains(prefix)) {
                retval.put(prefix, entry.getKey());
            }
        }
        return retval;
    }

    @Override
    public synchronized void registerPrefix(String namespace, String prefix, boolean isDefaultNamespace) {
        registerPrefixGlobal(namespace, prefix);
        if (isDefaultNamespace || prefix == null) {
            defaultNamespace = namespace;
        }
    }

    private static synchronized void registerPrefixGlobal(String namespace, String prefix) {
        GLOBAL_NAMESPACE_PREFIX_MAP.put(namespace, prefix);
    }

    @Override
    public synchronized void registerPrefixLocal(String namespace, String prefix) {
        localNamespacePrefixMap.put(namespace, prefix);
    }

    @Override
    public String getPrefix(String namespace) {
        if (defaultNamespace != null && defaultNamespace.equals(namespace)) {
            return "";
        }
        return getPrefixExplicit(namespace);
    }

    private synchronized String getPrefixExplicit(String namespace) {
        String prefix = localNamespacePrefixMap.get(namespace);
        return prefix != null ? prefix : getPreferredPrefix(namespace);
    }

    @Override
    public QName setQNamePrefix(QName qname) {
        String namespace = qname.getNamespaceURI();
        String prefix = getPrefix(namespace);
        if (prefix == null) {
            return qname;
        }
        return new QName(qname.getNamespaceURI(),qname.getLocalPart(),prefix);
    }

    @Override
    public QName setQNamePrefixExplicit(QName qname) {
        String namespace = qname.getNamespaceURI();
        String prefix = getPrefixExplicit(namespace);
        if (prefix == null) {
            return qname;
        }
        return new QName(qname.getNamespaceURI(),qname.getLocalPart(),prefix);
    }

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if (defaultNamespace != null && defaultNamespace.equals(namespaceUri)) {
            return "";
        } else {
            return getPreferredPrefix(namespaceUri, suggestion);
        }
    }

    /**
     * @return preferred prefix for the namespace, if no prefix is assigned yet,
     *         then it will assign a prefix and return it.
     */
    public static synchronized String getPreferredPrefix(String namespace) {
        return getPreferredPrefix(namespace, null);
    }

    /**
     * @return preferred prefix for the namespace, if no prefix is assigned yet,
     *         then it assign hint prefix (if it is not assigned yet) or assign
     *         a new prefix and return it (if hint prefix is already assigned to
     *         other namespace).
     */
    private static synchronized String getPreferredPrefix(String namespace, String hintPrefix) {
        String prefix = GLOBAL_NAMESPACE_PREFIX_MAP.get(namespace);

        if (StringUtils.isEmpty(prefix)) {
            if (StringUtils.isEmpty(hintPrefix)) {
                // FIXME: improve new prefix assignment
                prefix = "gen" + (new Random()).nextInt(999);
            } else {
                if (GLOBAL_NAMESPACE_PREFIX_MAP.containsValue(hintPrefix)) {
                    // FIXME: improve new prefix assignment
                    prefix = "gen" + (new Random()).nextInt(999);
                } else {
                    prefix = hintPrefix;
                }
            }
            GLOBAL_NAMESPACE_PREFIX_MAP.put(namespace, prefix);
        }

        return prefix;

    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public synchronized GlobalDynamicNamespacePrefixMapper clone() {
        GlobalDynamicNamespacePrefixMapper clone = new GlobalDynamicNamespacePrefixMapper();
        clone.defaultNamespace = this.defaultNamespace;
        clone.localNamespacePrefixMap = clonePrefixMap(this.localNamespacePrefixMap);
        return clone;
    }

    private Map<String, String> clonePrefixMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        Map<String, String> clone = new HashMap<>();
        for (Entry<String, String> entry: map.entrySet()) {
            clone.put(entry.getKey(), entry.getValue());
        }
        return clone;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("GlobalDynamicNamespacePrefixMapper(");
        sb.append(defaultNamespace);
        sb.append("):\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Global map:\n");
        DebugUtil.debugDumpMapMultiLine(sb, GLOBAL_NAMESPACE_PREFIX_MAP, indent + 2);
        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append("Local map:\n");
        DebugUtil.debugDumpMapMultiLine(sb, localNamespacePrefixMap, indent + 2);
        return sb.toString();
    }

}
