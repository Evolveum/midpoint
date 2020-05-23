package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.lang.api.IdentifierSpaceKey;

public interface AxiomRootContext {

    void importIdentifierSpace(NamespaceContext namespaceContext);

    void exportIdentifierSpace(IdentifierSpaceKey namespaceId);

}
