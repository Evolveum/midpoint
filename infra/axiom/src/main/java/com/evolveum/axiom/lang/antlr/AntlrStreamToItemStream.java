package com.evolveum.axiom.lang.antlr;

import java.util.Map;
import java.util.Optional;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.api.stream.AbstractStreamAdapter;
import com.evolveum.axiom.api.stream.AxiomItemStream;
import com.evolveum.axiom.api.stream.AxiomItemStream.TargetWithContext;
import com.evolveum.axiom.api.stream.AxiomStreamTarget;
import com.evolveum.axiom.api.stream.StreamContext;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.antlr.AxiomParser.ArgumentContext;
import com.evolveum.axiom.lang.antlr.AxiomParser.PrefixedNameContext;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;
import com.evolveum.axiom.spi.codec.ValueDecoder;

public class AntlrStreamToItemStream extends AbstractStreamAdapter<AxiomParser.PrefixedNameContext, AxiomParser.ArgumentContext> {

    private final AxiomItemStream.TargetWithContext target;

    private final AxiomDecoderContext<AxiomParser.PrefixedNameContext,AxiomParser.ArgumentContext> codecs;

    private final AxiomNameResolver documentLocal;

    private AntlrStreamToItemStream(TargetWithContext target, AxiomDecoderContext<PrefixedNameContext, ArgumentContext> codecs,
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
        AxiomName name = lookupName(target.currentItem().typeDefinition(), item, loc);
        target.startItem(name, loc);
    }

    private AxiomName lookupName(AxiomTypeDefinition type, PrefixedNameContext item, SourceLocation loc) {
        return codecs.itemName().decode(item, AxiomNameResolver.defaultNamespaceFromType(type).or(documentLocal), loc);
    }

    @Override
    public void startValue(ArgumentContext value, SourceLocation loc) {
        Object finalValue = decodeArgument(currentItem(), value, loc);
        target.startValue(finalValue, loc);
    }


    private Object decodeValue(AxiomItemDefinition itemDef, ArgumentContext value, SourceLocation loc) {
        // FIXME: Codec should somehow obtain stream local context?
        return codec(itemDef).decode(value, documentLocal, loc);
    }

    private ValueDecoder<ArgumentContext, Object> codec(AxiomItemDefinition itemDefinition) {
        return codecs.get(itemDefinition.typeDefinition());
    }

    private Object decodeArgument(AxiomItemDefinition item, ArgumentContext value, SourceLocation loc) {
        if(item.isStructured()) {
            AxiomTypeDefinition type = item.typeDefinition();
            AxiomSyntaxException.check(type.argument().isPresent(), loc, "Item %s does not accept simple value", item.name());
            decodeArgument(type.argument().get(),value,loc);
        }
        return decodeValue(item, value, loc);
    }


    private AxiomItemDefinition currentItem() {
        return target.currentItem();
    }

}
