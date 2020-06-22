/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.impl;

import java.util.Optional;

import com.evolveum.axiom.api.AxiomValue;
import com.evolveum.axiom.api.AxiomValueIdentifier;

public interface AxiomItemContext<T> {

    AxiomValueContext<T> addValue(T value);

    AxiomValueContext<?> parent();

    T onlyValue();

    void addOperationalValue(AxiomValueReference<T> value);

    Optional<? extends AxiomValueContext<T>> value(AxiomValueIdentifier id);

    void addCompletedValue(AxiomValue<?> itemDef);

}
