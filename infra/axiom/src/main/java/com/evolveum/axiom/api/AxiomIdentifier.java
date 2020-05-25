/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import com.google.common.base.Preconditions;

public class AxiomIdentifier {

    public static final String AXIOM_NAMESPACE = "https://ns.evolveum.com/axiom/language";
    private final String namespace;
    private final String localName;

    public AxiomIdentifier(String namespace, String localName) {
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
        if (!(obj instanceof AxiomIdentifier))
            return false;
        AxiomIdentifier other = (AxiomIdentifier) obj;
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
        return "(" + namespace + ")" +localName;
    }

    public static AxiomIdentifier axiom(String identifier) {
        return new AxiomIdentifier(AXIOM_NAMESPACE, identifier);
    }

    public static AxiomIdentifier from(String namespace, String localName) {
        return new AxiomIdentifier(namespace, localName);
    }

    public boolean sameNamespace(AxiomIdentifier other) {
        return this.namespace().equals(other.namespace());
    }

}
