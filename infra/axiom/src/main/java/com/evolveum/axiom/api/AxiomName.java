/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class AxiomName {

    public static final String AXIOM_NAMESPACE = "https://ns.evolveum.com/axiom/language";
    private final String namespace;
    private final String localName;

    public AxiomName(String namespace, String localName) {
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
        return new AxiomName(AXIOM_NAMESPACE, identifier);
    }

    public static AxiomName from(String namespace, String localName) {
        return new AxiomName(namespace, localName);
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

}
