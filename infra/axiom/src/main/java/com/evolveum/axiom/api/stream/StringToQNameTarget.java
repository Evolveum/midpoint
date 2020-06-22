package com.evolveum.axiom.api.stream;

import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomPrefixedName;
import com.evolveum.axiom.api.stream.AxiomItemStream.Target;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.google.common.base.Preconditions;

public class StringToQNameTarget implements AxiomStreamTarget<String> {

    private PrefixedToQNameTarget target;

    public StringToQNameTarget(Target target, Supplier<AxiomNameResolver> itemResolver,
            Supplier<AxiomNameResolver> valueResolver, Supplier<AxiomNameResolver> infraResolver) {
        super();
        this.target = new PrefixedToQNameTarget(target, itemResolver, valueResolver, infraResolver);
    }

    public void endItem(SourceLocation loc) {
        target.endItem(loc);
    }
    public void startValue(Object value, SourceLocation loc) {
        // FIXME: Do we want to do this?
        /*if(value instanceof AxiomPrefixedName) {
            value = Preconditions.checkNotNull(argumentResolver.get().resolve((AxiomPrefixedName) value));
        }*/
        target.startValue(value, loc);
    }
    public void endValue(SourceLocation loc) {
        target.endValue(loc);
    }

    @Override
    public void startItem(String item, SourceLocation loc) {
        if(AxiomPrefixedName.isPrefixed(item)) {
            target.startItem(AxiomPrefixedName.parse(item), loc);
        }
        target.qnameTarget().startItem(AxiomName.parse(item), loc);
    }

    public void startInfra(String item, SourceLocation loc) {

    }

}
