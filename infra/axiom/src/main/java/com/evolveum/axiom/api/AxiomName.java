/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Iterator;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

public class AxiomName {

    public static final String AXIOM_NAMESPACE = "https://schema.evolveum.com/ns/axiom/model";

    public static final String MODEL_NAMESPACE = AXIOM_NAMESPACE;
    public static final String TYPE_NAMESPACE = "https://schema.evolveum.com/ns/axiom/types";
    public static final String DATA_NAMESPACE = "https://schema.evolveum.com/ns/axiom/data";

    private static final Interner<AxiomName> INTERNER = Interners.newWeakInterner();

    private final String namespace;
    private final String localName;

    private static final Splitter HASH_SYMBOL = Splitter.on('#');

    AxiomName(String namespace, String localName) {
        this.namespace = Preconditions.checkNotNull(namespace, "namespace");
        this.localName = Preconditions.checkNotNull(localName, "localName");
    }

    public String namespace() {
        return namespace;
    }

    public String localName() {
        return localName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((localName == null) ? 0 : localName.hashCode());
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof AxiomName))
            return false;
        AxiomName other = (AxiomName) obj;
        if (localName == null) {
            if (other.localName != null)
                return false;
        } else if (!localName.equals(other.localName))
            return false;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else if (!namespace.equals(other.namespace))
            return false;
        return true;
    }

    @Override
    public String toString() {
        if(Strings.isNullOrEmpty(namespace)) {
            return localName;
        }
        return "(" + namespace + ")" +localName;
    }

    public static AxiomName axiom(String identifier) {
        return from(AXIOM_NAMESPACE, identifier);
    }

    public static AxiomName from(String namespace, String localName) {
        return INTERNER.intern(new AxiomName(namespace, localName));
    }

    public boolean sameNamespace(AxiomName other) {
        return this.namespace().equals(other.namespace());
    }

    public AxiomName localName(String name) {
        return AxiomName.from(namespace, name);
    }

    public AxiomName namespace(String targetNamespace) {
        return AxiomName.from(targetNamespace, localName);
    }

    public AxiomName defaultNamespace() {
        return AxiomName.from("", localName);
    }

    public static AxiomName local(@NotNull String localName) {
        return from("", localName);
    }

    public static AxiomName parse(String item) {
        Iterator<String> nsLocalName = HASH_SYMBOL.split("#").iterator();
        return from(nsLocalName.next(), nsLocalName.next());
    }

    public static AxiomName builtIn(String localName) {
        return from(TYPE_NAMESPACE, localName);
    }

    public static AxiomName data(String localName) {
        return from(DATA_NAMESPACE, localName);
    }

}
