/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.annotation;

import com.evolveum.midpoint.util.annotation.Experimental;

import java.io.Serializable;
import java.util.Objects;

@Experimental
public class ItemDiagramSpecification implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final DiagramElementFormType form;

    public ItemDiagramSpecification(String name, DiagramElementFormType form) {
        this.name = name;
        this.form = form;
    }

    public String getName() {
        return name;
    }

    public DiagramElementFormType getForm() {
        return form;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ItemDiagramSpecification that = (ItemDiagramSpecification) o;
        return Objects.equals(name, that.name) && form == that.form;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, form);
    }
}
