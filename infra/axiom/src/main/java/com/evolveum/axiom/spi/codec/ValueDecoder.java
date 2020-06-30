package com.evolveum.axiom.spi.codec;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;

public interface ValueDecoder<I,O> {

    O decode(I input, AxiomNameResolver localResolver, SourceLocation location);


    interface NamespaceIngoring<I,O> extends ValueDecoder<I, O> {

        @Override
        default O decode(I input, AxiomNameResolver localResolver, SourceLocation location) {
            return decode(input, location);
        }

        O decode(I input, SourceLocation location);

    }

    interface Factory<D extends ValueDecoder<?,?>> {

        D create(AxiomTypeDefinition type, AxiomSchemaContext context);

    }
}
