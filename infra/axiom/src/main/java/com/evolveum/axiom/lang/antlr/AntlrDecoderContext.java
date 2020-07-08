package com.evolveum.axiom.lang.antlr;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomPrefixedName;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.stream.AxiomItemStream;
import com.evolveum.axiom.api.stream.AxiomItemTarget;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.antlr.AxiomParser.ArgumentContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.PrefixedNameContext;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;
import com.evolveum.axiom.spi.codec.ValueDecoder;
import com.google.common.collect.ImmutableMap;

public class AntlrDecoderContext implements AxiomDecoderContext<AxiomParser.PrefixedNameContext, AxiomParser.ArgumentContext> {

    private static final ValueDecoder<PrefixedNameContext, AxiomPrefixedName> PREFIXED_NAME = (v, res, loc) -> {
        String prefix = v.prefix() != null ? v.prefix().getText() : "";
        return AxiomPrefixedName.from(prefix, v.localName().getText());
    };

    private static final ValueDecoder<PrefixedNameContext, AxiomName> QUALIFIED_NAME = decoder((v, res, loc) -> res.resolve(PREFIXED_NAME.decode(v, res, loc)))
            .onNullThrow((v, loc) -> AxiomSemanticException.create(loc, "Unable to resolve qualifed name for '%s'", v.getText()));



    private static final AntlrDecoder<AxiomName> TYPE_REFERENCE = AntlrDecoder.create((i,r,l) -> QUALIFIED_NAME.decode(i.prefixedName(), AxiomNameResolver.BUILTIN_TYPES.or(r), l));
    private static final AntlrDecoder<String> STRING_ARGUMENT = AntlrDecoder.from((input, loc) -> {
            if(input.string() != null) {
                return AbstractAxiomAntlrVisitor.convert(input.string());
            }
            return input.getText();
    });

    private static final AntlrDecoder<Boolean> BOOLEAN = AntlrDecoder.from((input, loc) -> {
        return Boolean.parseBoolean(input.getText());
    });


    private static final Map<AxiomName, AntlrDecoder<?>> DEFAULT_BUILTIN_CODECS = ImmutableMap.<AxiomName, AntlrDecoder<?>>builder()
            .put(type("QName"), (v, res, loc) -> QUALIFIED_NAME.decode(v.prefixedName(), res, loc))
            .put(type("String"), STRING_ARGUMENT)
            .put(type("Boolean"), BOOLEAN)
            .put(model("TypeReference"), TYPE_REFERENCE)
            .put(data("DynamicType"), TYPE_REFERENCE) // FIXME: This is hack, we need a way to resolve dynamic type
            .build();


    private Map<AxiomName, AntlrDecoder<?>> codecs = DEFAULT_BUILTIN_CODECS;


    public static final AntlrDecoderContext BUILTIN_DECODERS = new AntlrDecoderContext();

    @Override
    public Optional<AntlrDecoder<?>> get(AxiomTypeDefinition typeDef) {
        AntlrDecoder<?> codec = codecs.get(typeDef.name());
        if(codec != null) {
            return Optional.of(codec);
        }
        if(typeDef.superType().isPresent()) {
            return get(typeDef.superType().get());
        }
        return Optional.empty();
    }

    private static AxiomName data(String localName) {
        return AxiomName.from(AxiomName.DATA_NAMESPACE, localName);
    }

    private static AxiomName model(String localName) {
        return AxiomName.from(AxiomName.MODEL_NAMESPACE, localName);
    }

    private static AxiomName type(String localName) {
        return AxiomName.from(AxiomName.TYPE_NAMESPACE, localName);
    }

    private static ValueDecoder<PrefixedNameContext, AxiomName> decoder(ValueDecoder<PrefixedNameContext, AxiomName> object) {
        return object;
    }

    @Override
    public ValueDecoder<PrefixedNameContext, AxiomName> itemName() {
        return QUALIFIED_NAME;
    }

    public void stream(AxiomModelStatementSource source, AxiomItemStream.TargetWithContext target) {
        source.stream(target, this);
    }
}
