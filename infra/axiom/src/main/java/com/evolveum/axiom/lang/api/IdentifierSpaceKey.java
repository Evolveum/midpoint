package com.evolveum.axiom.lang.api;

import java.util.Map;
import java.util.Map.Entry;

import com.evolveum.axiom.api.AxiomName;
import com.google.common.collect.ImmutableMap;

public class IdentifierSpaceKey {

    private final Map<AxiomName, Object> components;

    public IdentifierSpaceKey(Map<AxiomName, Object> components) {
        this.components = ImmutableMap.copyOf(components);
    }

    public Map<AxiomName, Object> components() {
        return components;
    }

    @Override
    public int hashCode() {
        return components().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj instanceof IdentifierSpaceKey) {
            return components().equals(((IdentifierSpaceKey) obj).components());
        }
        return false;
    }

    public static IdentifierSpaceKey from(Map<AxiomName, Object> build) {
        return new IdentifierSpaceKey(build);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("[");
        boolean first = true;
        for(Entry<AxiomName, Object> val : components().entrySet()) {
            if(!first) {
                b.append(",");
            }
            b.append(val.getKey()).append("=").append(val.getValue());
        }
        b.append("]");
        return b.toString();
    }

    public static IdentifierSpaceKey of(AxiomName key, Object value) {
        return from(ImmutableMap.of(key, value));
    }

}
