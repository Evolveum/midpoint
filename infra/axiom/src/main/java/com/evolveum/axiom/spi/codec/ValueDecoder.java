package com.evolveum.axiom.spi.codec;
import java.util.function.Function;

import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.concepts.SourceLocation;

public interface ValueDecoder<I,O> {

    O decode(I input, AxiomNameResolver localResolver, SourceLocation location);

    default ValueDecoder<I,O> onNullThrow(ExceptionFactory<I> factory) {

        return (i, res, loc) -> {
            O ret = this.decode(i, res, loc);
            if(ret == null) {
                throw factory.create(i, loc);
            }
            return ret;
        };
    }

    interface NamespaceIngoring<I,O> extends ValueDecoder<I, O> {

        @Override
        default O decode(I input, AxiomNameResolver localResolver, SourceLocation location) {
            return decode(input, location);
        }

        O decode(I input, SourceLocation location);

    }

    interface ExceptionFactory<I> {

        RuntimeException create(I input, SourceLocation loc);

    }

    interface Factory<D extends ValueDecoder<?,?>> {

        D create(AxiomTypeDefinition type, AxiomSchemaContext context);

    }
}
