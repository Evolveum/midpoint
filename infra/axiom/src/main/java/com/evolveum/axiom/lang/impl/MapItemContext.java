package com.evolveum.axiom.lang.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomValueIdentifier;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.concepts.SourceLocation;

public class MapItemContext<V> extends ItemContext<V> {

    private final Map<AxiomValueIdentifier, ValueContext<V>> values = new HashMap<>();

    public MapItemContext(ValueContext<?> sourceContext, AxiomName name, AxiomItemDefinition definition,
            SourceLocation loc) {
        super(sourceContext, name, definition, loc);
    }

    @Override
    public Optional<? extends AxiomValueContext<V>> value(AxiomValueIdentifier id) {
        return Optional.ofNullable(values.get(id));
    }

    void addIdentifier(AxiomValueIdentifier key, ValueContext<V> valueContext) {
        ValueContext<V> previous = values.putIfAbsent(key, valueContext);
        if(previous != null) {
            throw AxiomSemanticException.create(valueContext.startLocation(), "Value %s is already defined at %s", key, previous.startLocation());
        }
    }


}
