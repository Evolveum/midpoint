package com.evolveum.axiom.lang.antlr;

import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.stream.AbstractStreamAdapter;
import com.evolveum.axiom.api.stream.AxiomItemStream;
import com.evolveum.axiom.api.stream.AxiomItemStream.TargetWithContext;
import com.evolveum.axiom.api.stream.AxiomStreamTarget;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.antlr.AxiomParser.ArgumentContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.PrefixedNameContext;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;
import com.evolveum.axiom.spi.codec.ValueDecoder;

public class AntlrStreamToItemStream extends AbstractStreamAdapter<AxiomParser.PrefixedNameContext, AxiomParser.ArgumentContext> {

    private final AxiomItemStream.TargetWithContext target;

    private final AxiomDecoderContext<AxiomParser.PrefixedNameContext,AxiomParser.ArgumentContext> codecs;

    private final AxiomNameResolver documentLocal;

    AntlrStreamToItemStream(TargetWithContext target, AxiomDecoderContext<PrefixedNameContext, ArgumentContext> codecs,
            AxiomNameResolver documentLocal) {
        this.target = target;
        this.codecs = codecs;
        this.documentLocal = documentLocal;
    }

    @Override
    protected AxiomStreamTarget<?, ?> target() {
        return target;
    }

    @Override
    public void startInfra(PrefixedNameContext item, SourceLocation loc) {
        AxiomName name = lookupName(target.currentInfra(), item, loc);
        target.startInfra(name, loc);
    }

    @Override
    public void startItem(PrefixedNameContext item, SourceLocation loc) {
        AxiomName name = lookupName(target.currentType(), item, loc);
        target.startItem(name, loc);
    }

    private AxiomName lookupName(AxiomTypeDefinition type, PrefixedNameContext item, SourceLocation loc) {
        AxiomName name = codecs.itemName().decode(item, AxiomNameResolver.defaultNamespaceFromType(type).or(documentLocal), loc);
        AxiomSemanticException.check(name != null && type.itemDefinition(name).isPresent(), loc, "item %s not present in type %s", item.getText(), type.name());
        return name;
    }

    @Override
    public void startValue(ArgumentContext value, SourceLocation loc) {
        Object finalValue;
        if(value != null) {
            finalValue = decodeArgument(target.currentType(), value, loc);
        } else {
            finalValue = null;
        }
        target.startValue(finalValue, loc);
    }

    private Object decodeArgument(AxiomTypeDefinition type, ArgumentContext value, SourceLocation loc) {
        if(type.isComplex()) {
            AxiomSyntaxException.check(type.argument().isPresent(), loc, "Type %s does not accept simple value", type.name());
            Optional<? extends ValueDecoder<ArgumentContext, ?>> maybeCodec = codecs.get(type);
            if(maybeCodec.isPresent()) {
                return decodeValue(maybeCodec.get(),value, loc);
            }
            return decodeArgument(type.argument().get().typeDefinition(),value,loc);
        }
        return decodeValue(codecs.get(type).orElseThrow(() -> new IllegalStateException("Codec not found for " + type.name())),
                value, loc);
    }

    private Object decodeValue(ValueDecoder<ArgumentContext, ?> valueDecoder, ArgumentContext value,
            SourceLocation loc) {
        return valueDecoder.decode(value, documentLocal, loc);
    }

}
