package com.evolveum.axiom.lang.antlr;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.spi.codec.ValueDecoder;

public interface AxiomDecoderContext<I, V> {

    ValueDecoder<I, AxiomName> itemName();

    ValueDecoder<V, Object> get(AxiomTypeDefinition typeDefinition);

}
