package com.evolveum.axiom.spi.codec;

import com.evolveum.axiom.api.AxiomPath;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.concepts.SourceLocation;

public interface AxiomPathValueDecoder<I> extends ValueDecoder<I, AxiomPath> {

    @Override
    default AxiomPath decode(I input, AxiomNameResolver localResolver, SourceLocation location) {
        // TODO Auto-generated method stub
        return null;
    }

}
