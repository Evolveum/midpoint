/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api.stmt;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.lang.api.AxiomItemDefinition;

public interface AxiomStatementDefinition {

    AxiomIdentifier identifier();

    Optional<AxiomItemDefinition> argument();

    Collection<AxiomItemDefinition> children();

}
