/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.model.common.expression.script.cel;

import dev.cel.common.types.CelKind;
import dev.cel.common.types.CelType;

import java.util.Objects;

public class DynType extends CelType {

    private final String name;

    public DynType(String name) {
        this.name = name;
    }

    @Override
    public CelKind kind() {
        return CelKind.DYN;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        DynType dynType = (DynType) o;
        return Objects.equals(name, dynType.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public String toString() {
        return "DynType(" +
                "name='" + name + '\'' +
                ')';
    }
}
