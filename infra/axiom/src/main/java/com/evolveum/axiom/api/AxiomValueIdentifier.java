package com.evolveum.axiom.api;

import java.util.Map;
import java.util.Map.Entry;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomIdentifierDefinition;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

public class AxiomValueIdentifier {

    private final Map<AxiomName, Object> components;

    public AxiomValueIdentifier(Map<AxiomName, Object> components) {
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
        if(obj instanceof AxiomValueIdentifier) {
            return components().equals(((AxiomValueIdentifier) obj).components());
        }
        return false;
    }

    public static AxiomValueIdentifier from(Map<AxiomName, Object> build) {
        return new AxiomValueIdentifier(build);
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

    public static AxiomValueIdentifier of(AxiomName key, Object value) {
        return from(ImmutableMap.of(key, value));
    }

    public static AxiomValueIdentifier from(AxiomIdentifierDefinition key, AxiomValue<?> value) {
        Preconditions.checkArgument(value instanceof AxiomStructuredValue, "Value must be complex.");
        AxiomStructuredValue complex = value.asComplex().get();

        // FIXME: Should be offset map?
        ImmutableMap.Builder<AxiomName, Object> components = ImmutableMap.builder();
        for( AxiomName component : key.components()) {
            components.put(component, complex.item(component).get().onlyValue().value());
        }
        return from(components.build());
    }

}
