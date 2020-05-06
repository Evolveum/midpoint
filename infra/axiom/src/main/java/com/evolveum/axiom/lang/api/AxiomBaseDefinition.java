/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomBaseDefinition {

    AxiomIdentifier identifier();
    String documentation();
}
