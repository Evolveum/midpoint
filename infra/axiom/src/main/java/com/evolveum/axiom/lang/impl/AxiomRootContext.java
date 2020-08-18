/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import com.evolveum.axiom.api.AxiomValueIdentifier;

public interface AxiomRootContext {

    void importIdentifierSpace(NamespaceContext namespaceContext);

    void exportIdentifierSpace(AxiomValueIdentifier namespaceId);

}
