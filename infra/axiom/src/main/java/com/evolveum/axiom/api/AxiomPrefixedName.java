package com.evolveum.axiom.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class AxiomPrefixedName {

    private final @Nullable String prefix;
    private final @NotNull String  localName;

    private AxiomPrefixedName(@Nullable String prefix, @NotNull String localName) {
        this.prefix = Strings.emptyToNull(prefix);
        this.localName = Preconditions.checkNotNull(localName, "localName");
    }


    public static AxiomPrefixedName from(String prefix, String localName) {
        return new AxiomPrefixedName(prefix, localName);
    }

    public static final boolean isPrefixed(String maybePrefixed) {
        // FIXME: Add matching
        return maybePrefixed.contains(":");
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLocalName() {
        return localName;
    }

    @Override
    public String toString() {
        return prefix != null ? prefix +  ":" + localName : localName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((localName == null) ? 0 : localName.hashCode());
        result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AxiomPrefixedName other = (AxiomPrefixedName) obj;
        if (localName == null) {
            if (other.localName != null)
                return false;
        } else if (!localName.equals(other.localName))
            return false;
        if (prefix == null) {
            if (other.prefix != null)
                return false;
        } else if (!prefix.equals(other.prefix))
            return false;
        return true;
    }


    public static AxiomPrefixedName parse(String item) {
        // TODO Auto-generated method stub
        return null;
    }

}
