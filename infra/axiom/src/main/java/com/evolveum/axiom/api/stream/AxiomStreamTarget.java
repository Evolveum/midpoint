package com.evolveum.axiom.api.stream;

import com.evolveum.axiom.concepts.SourceLocation;

public interface AxiomStreamTarget<N> {

    void startItem(N item, SourceLocation loc);
    void endItem(SourceLocation loc);

    void startValue(Object value, SourceLocation loc);
    void endValue(SourceLocation loc);

    default void startInfra(N item, SourceLocation loc) {};
    default void endInfra(SourceLocation loc) {};

}
