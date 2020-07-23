package com.evolveum.axiom.api.stream;

import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomPrefixedName;
import com.evolveum.axiom.api.stream.AxiomItemStream.Target;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.google.common.base.Preconditions;

public class PrefixedToQNameTarget implements AxiomStreamTarget<AxiomPrefixedName, Object> {

    private final AxiomItemStream.Target target;

    private final Supplier<AxiomNameResolver> argumentResolver;
    private final Supplier<AxiomNameResolver> item;
    private final Supplier<AxiomNameResolver> infra;


    public PrefixedToQNameTarget(Target target, Supplier<AxiomNameResolver> itemResolver,
            Supplier<AxiomNameResolver> valueResolver, Supplier<AxiomNameResolver> infraResolver) {
        super();
        this.target = target;
        this.item = itemResolver;
        this.argumentResolver = valueResolver;
        this.infra = infraResolver;
    }

    static AxiomName convertItemName(Supplier<AxiomNameResolver> resolver, AxiomPrefixedName prefixed, SourceLocation loc) {
        AxiomName result = resolver.get().resolve(prefixed);
        AxiomSemanticException.check(result != null, loc, "Unknown item '%s'.", prefixed);
        return result;
    }

    public void startItem(AxiomPrefixedName name, SourceLocation loc) {
        target.startItem(convertItemName(item, name, loc), loc);
    }
    public void endItem(SourceLocation loc) {
        target.endItem(loc);
    }
    public void startValue(Object value, SourceLocation loc) {
        // FIXME: Do we want to do this?
        if(value instanceof AxiomPrefixedName) {
            value = Preconditions.checkNotNull(argumentResolver.get().resolve((AxiomPrefixedName) value));
        }
        target.startValue(value, loc);
    }
    public void endValue(SourceLocation loc) {
        target.endValue(loc);
    }
    public void startInfra(AxiomPrefixedName name, SourceLocation loc) {
        target.startInfra(convertItemName(infra, name, loc), loc);
    }
    public void endInfra(SourceLocation loc) {
        target.endInfra(loc);
    }

    AxiomItemStream.Target qnameTarget() {
        return target;
    }

}
