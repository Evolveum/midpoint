package com.evolveum.axiom.lang.antlr;

import java.util.Map;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomPrefixedName;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.antlr.AxiomParser.ArgumentContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.PrefixedNameContext;
import com.evolveum.axiom.spi.codec.ValueDecoder;

public class AntlrDecoderContext implements AxiomDecoderContext<AxiomParser.PrefixedNameContext, AxiomParser.ArgumentContext> {

    private static final ValueDecoder<PrefixedNameContext, AxiomPrefixedName> PREFIXED_NAME = (v, res, loc) -> {
        String prefix = v.prefix() != null ? v.prefix().getText() : "";
        return AxiomPrefixedName.from(prefix, v.localName().getText());
    };

    private static final ValueDecoder<PrefixedNameContext, AxiomName> ITEM_NAME = (v, res, loc) -> {
        return res.resolve(PREFIXED_NAME.decode(v, res, loc));
    };

    private Map<AxiomName, ValueDecoder<ArgumentContext, Object>> codecs;

    @Override
    public ValueDecoder<ArgumentContext, Object> get(AxiomTypeDefinition typeDef) {
        ValueDecoder<ArgumentContext, Object> codec = codecs.get(typeDef.name());
        if(codec != null) {
            return codec;
        }
        if(typeDef.superType().isPresent()) {
            return get(typeDef.superType().get());
        }
        throw new IllegalStateException("Codec not found for " + typeDef.name());
    }

    @Override
    public ValueDecoder<PrefixedNameContext, AxiomName> itemName() {
        return ITEM_NAME;
    }
}
