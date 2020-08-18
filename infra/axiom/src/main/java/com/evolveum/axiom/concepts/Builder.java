package com.evolveum.axiom.concepts;

import java.util.Optional;

import com.evolveum.axiom.concepts.Lazy.Supplier;

public interface Builder<P> {

    P build();

    interface OrProduct<P,B extends Builder<P>> extends Supplier<P> {

        Optional<? extends B> builder();

        Optional<? extends P> product();

        @Override
        default P get() {
            if(product().isPresent()) {
                return product().get();
            }
            if(builder().isPresent()) {
                return builder().get().build();
            }
            throw new IllegalStateException("No product or builder is present");
        }
    }
}
