/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api.stmt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.impl.AxiomSyntaxException;

public interface AxiomStatementStreamListener {

    void endStatement( @Nullable SourceLocation sourceLocation);
    void startStatement(@NotNull AxiomIdentifier identifier,  @Nullable SourceLocation sourceLocation) throws AxiomSyntaxException;
    void argument(@NotNull AxiomIdentifier convert, @Nullable SourceLocation sourceLocation);
    void argument(@NotNull String convert, @Nullable SourceLocation sourceLocation);
}
