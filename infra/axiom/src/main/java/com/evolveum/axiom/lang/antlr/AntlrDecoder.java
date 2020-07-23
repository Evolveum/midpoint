package com.evolveum.axiom.lang.antlr;
import com.evolveum.axiom.spi.codec.ValueDecoder;

public interface AntlrDecoder<O> extends ValueDecoder<AxiomParser.ArgumentContext,O> {

    static <O> AntlrDecoder<O> from(ValueDecoder.NamespaceIngoring<AxiomParser.ArgumentContext, O> decoder) {
        return create(decoder);
    }

    static <O> AntlrDecoder<O> create(ValueDecoder<AxiomParser.ArgumentContext,O> decoder) {
        return decoder::decode;
    }


}
