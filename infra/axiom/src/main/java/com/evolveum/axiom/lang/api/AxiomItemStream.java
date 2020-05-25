package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.spi.AxiomIdentifierResolver;
import com.evolveum.axiom.lang.spi.SourceLocation;

public interface AxiomItemStream {

    interface Target {
        void startItem(AxiomIdentifier item, SourceLocation loc);
        void startValue(Object value, SourceLocation loc);
        void endValue(SourceLocation loc);
        void endItem(SourceLocation loc);
    }

    interface TargetWithResolver extends Target {

        AxiomIdentifierResolver itemResolver();
        AxiomIdentifierResolver valueResolver();

    }

}
