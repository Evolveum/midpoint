/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.xml;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;

import java.util.Map;

/**
 * Maps namespaces to preferred prefixes. Should be used through the code to
 * avoid generation of prefixes.
 *
 * @see <a href="https://jira.evolveum.com/browse/MID-349">MID-349</a>
 *
 * TODO consider removal from the Prism API
 *
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public interface DynamicNamespacePrefixMapper extends DebugDumpable {

    void registerPrefix(String namespace, String prefix, boolean defaultNamespace);

    void registerPrefixLocal(String namespace, String prefix);

    String getPrefix(String namespace);

    QName setQNamePrefix(QName qname);

    /**
     * Makes sure that there is explicit prefix and not a default namespace prefix.
     */
    QName setQNamePrefixExplicit(QName qname);

    DynamicNamespacePrefixMapper clone();

    // Specifies that this prefix should be declared by default (at top of XML files)
    void addDeclaredByDefault(String prefix);

    // non-null
    Map<String,String> getNamespacesDeclaredByDefault();
}
