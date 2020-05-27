/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api.schema;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomNamedDefinition {

    AxiomIdentifier name();
    String documentation();
}
