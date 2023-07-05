/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkDefinitionType;

/**
 * Represents statically or dynamically defined activity work definition.
 *
 * Both kinds are subtypes of {@link AbstractWorkDefinitionType}.
 *
 * However, the former ones ({@link Typed}) are compile-time defined, and thus representable as {@link Containerable}
 * Java objects. The latter ones ({@link Untyped}) are defined dynamically, and can be represented only
 * as {@link PrismContainerValue} Java objects; at least for now.
 *
 * The naming is not very clear, as the word "bean" is used ambiguously. To be resolved somehow.
 */
public abstract class WorkDefinitionBean {

    /** Returns the type name, e.g. `c:ReconciliationWorkDefinitionType` or `ext:MyCustomDefinitionType`. */
    public abstract @NotNull QName getBeanTypeName();

    public abstract @NotNull PrismContainerValue<?> getValue();

    /** To be used only for typed values. Fails when called on untyped value. */
    public abstract @NotNull AbstractWorkDefinitionType getBean();

    /** Statically defined (usually, midPoint-provided) beans. */
    public static class Typed extends WorkDefinitionBean {

        @NotNull private final AbstractWorkDefinitionType bean;

        Typed(@NotNull AbstractWorkDefinitionType bean) {
            this.bean = bean;
        }

        public @NotNull AbstractWorkDefinitionType getBean() {
            return bean;
        }

        @Override
        @NotNull
        public QName getBeanTypeName() {
            return PrismContext.get().getSchemaRegistry()
                    .findComplexTypeDefinitionByCompileTimeClass(bean.getClass())
                    .getTypeName();
        }

        @Override
        public @NotNull PrismContainerValue<?> getValue() {
            return bean.asPrismContainerValue();
        }

        @Override
        public String toString() {
            return bean.toString();
        }
    }

    /** Run-time defined, usually customer provided beans. */
    public static class Untyped extends WorkDefinitionBean {

        @NotNull private final PrismContainerValue<?> pcv;

        Untyped(@NotNull PrismContainerValue<?> pcv) {
            this.pcv = pcv;
        }

        public @NotNull PrismContainerValue<?> getValue() {
            return pcv;
        }

        @Override
        public @NotNull AbstractWorkDefinitionType getBean() {
            throw new IllegalStateException("Cannot get bean from untyped work definition: " + this);
        }

        @Override
        @NotNull
        public QName getBeanTypeName() {
            return pcv.getDefinition().getTypeName();
        }

        @Override
        public String toString() {
            return pcv.toString();
        }
    }
}
