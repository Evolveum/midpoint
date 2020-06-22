package com.evolveum.axiom.api.stream;

import java.util.function.Supplier;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomPrefixedName;
import com.evolveum.axiom.api.stream.AxiomItemStream.Target;
import com.evolveum.axiom.concepts.SourceLocation;
import com.evolveum.axiom.lang.spi.AxiomNameResolver;
import com.evolveum.axiom.lang.spi.AxiomSemanticException;
import com.google.common.base.Preconditions;

public class PrefixedToQNameTarget implements AxiomStreamTarget<AxiomPrefixedName> {

    private final AxiomItemStream.Target target;

    private final Supplier<AxiomNameResolver> argumentResolver;
    private final Supplier<AxiomNameResolver> itemResolver;

    public PrefixedToQNameTarget(Target target, Supplier<AxiomNameResolver> itemResolver,
            Supplier<AxiomNameResolver> valueResolver) {
        super();
        this.target = target;
        this.itemResolver = itemResolver;
        this.argumentResolver = valueResolver;
    }

    protected AxiomName convertItemName(AxiomPrefixedName prefixed, SourceLocation loc) {
        AxiomName result = itemResolver.get().resolve(prefixed);
        AxiomSemanticException.check(result != null, loc, "Unknown item '%s'.", prefixed);
        return result;
    }

    public void startItem(AxiomPrefixedName item, SourceLocation loc) {
        target.startItem(convertItemName(item, loc), loc);
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
    public void startInfra(AxiomPrefixedName item, SourceLocation loc) {
        target.startInfra(convertItemName(item, loc), loc);
    }
    public void endInfra(SourceLocation loc) {
        target.endInfra(loc);
    }

    AxiomItemStream.Target qnameTarget() {
        return target;
    }

}
