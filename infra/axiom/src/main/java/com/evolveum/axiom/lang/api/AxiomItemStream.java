package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.spi.SourceLocation;

public interface AxiomItemStream {

    interface Listener {
        void startItem(AxiomIdentifier item, SourceLocation loc);
        void startValue(Object value, SourceLocation loc);
        void endValue(SourceLocation loc);
        void endItem(SourceLocation loc);
    }
}
