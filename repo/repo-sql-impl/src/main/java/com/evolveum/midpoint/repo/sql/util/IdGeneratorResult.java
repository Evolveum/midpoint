/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.PrismContainerValue;

import java.util.HashSet;
import java.util.Set;

/**
 * @author lazyman
 */
public class IdGeneratorResult {

    private Set<PrismContainerValue> values = new HashSet<>();
    private boolean generatedOid;

    public Set<PrismContainerValue> getValues() {
        return values;
    }

    public boolean isGeneratedOid() {
        return generatedOid;
    }

    public void setGeneratedOid(boolean generatedOid) {
        this.generatedOid = generatedOid;
    }

    public boolean isTransient(PrismContainerValue value) {
        return generatedOid || values.contains(value);
    }
}
