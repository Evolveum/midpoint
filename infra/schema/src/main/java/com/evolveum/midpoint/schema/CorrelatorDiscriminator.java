/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationUseType;

import java.util.Objects;

public class CorrelatorDiscriminator {

    private String correlatorIdentifier;
    private CorrelationUseType use;

    public CorrelatorDiscriminator(String correlatorIdentifier, CorrelationUseType use) {
        this.correlatorIdentifier = correlatorIdentifier;
        this.use = use;
    }

    public boolean match(CompositeCorrelatorType correlatorType) {
        if (correlatorType == null) {
            return false;
        }

        return Objects.equals(correlatorType.getName(), correlatorIdentifier)
                && correlatorType.getUse() == use;
    }
}

