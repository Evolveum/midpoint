/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import java.util.HashSet;
import java.util.Set;

import com.evolveum.midpoint.prism.PrismContainerValue;

/**
 * @author lazyman
 */
public class IdGeneratorResult {

    private final Set<PrismContainerValue<?>> values = new HashSet<>();
    private boolean generatedOid;

    public Set<PrismContainerValue<?>> getValues() {
        return values;
    }

    public boolean isGeneratedOid() {
        return generatedOid;
    }

    public void setGeneratedOid(boolean generatedOid) {
        this.generatedOid = generatedOid;
    }

    public boolean isTransient(PrismContainerValue<?> value) {
        return generatedOid || values.contains(value);
    }
}
