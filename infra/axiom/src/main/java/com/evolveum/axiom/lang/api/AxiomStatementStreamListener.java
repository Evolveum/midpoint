package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomStatementStreamListener {

    void startStatement(AxiomIdentifier statement);
    void argument(AxiomIdentifier identifier);
    void argument(String identifier);
    void endStatement();
}
