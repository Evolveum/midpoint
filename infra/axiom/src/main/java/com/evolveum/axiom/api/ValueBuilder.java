package com.evolveum.axiom.api;

import com.evolveum.axiom.concepts.Builder;

public interface ValueBuilder<P extends AxiomValue<?>> extends Builder<P> {




    interface BuilderOrValue<P extends AxiomValue<?>> extends Builder.OrProduct<P, ValueBuilder<P>> {

    }
}
