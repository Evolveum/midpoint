package com.evolveum.axiom.lang.antlr;

import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.spi.codec.ValueDecoder;

public interface AxiomDecoderContext<I, V> {

    ValueDecoder<I, AxiomName> itemName();

    Optional<? extends ValueDecoder<V, ?>> get(AxiomTypeDefinition typeDefinition);

}
