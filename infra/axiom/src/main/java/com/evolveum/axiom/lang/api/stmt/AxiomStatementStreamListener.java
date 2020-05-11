/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api.stmt;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.impl.AxiomSyntaxException;

public interface AxiomStatementStreamListener {

    void endStatement();
    void startStatement(AxiomIdentifier identifier,  String sourceName,  int line, int posInLine) throws AxiomSyntaxException;
    void argument(String argument ,  String sourceName,  int line, int posInLine);
    void argument(AxiomIdentifier argument, String sourceName, int sourceLine, int sourcePosition);
}
