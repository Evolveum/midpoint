/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkDefinitionType;

/**
 * Wraps work definition information.
 *
 * Contains statically or dynamically defined subtype of AbstractWorkDefinitionType.
 */
public abstract class WorkDefinitionWrapper implements WorkDefinitionSource {

    @NotNull
    public abstract QName getBeanTypeName();

    public static class TypedWorkDefinitionWrapper extends WorkDefinitionWrapper {

        @NotNull
        private final AbstractWorkDefinitionType typedDefinition;

        TypedWorkDefinitionWrapper(@NotNull AbstractWorkDefinitionType typedDefinition) {
            this.typedDefinition = typedDefinition;
        }

        public @NotNull AbstractWorkDefinitionType getTypedDefinition() {
            return typedDefinition;
        }

        @Override
        @NotNull
        public QName getBeanTypeName() {
            return PrismContext.get().getSchemaRegistry()
                    .findComplexTypeDefinitionByCompileTimeClass(typedDefinition.getClass())
                    .getTypeName();
        }

        @Override
        public String toString() {
            return typedDefinition.toString();
        }
    }

    public static class UntypedWorkDefinitionWrapper extends WorkDefinitionWrapper {

        @NotNull
        private final PrismContainerValue<?> untypedDefinition;

        UntypedWorkDefinitionWrapper(@NotNull PrismContainerValue<?> untypedDefinition) {
            this.untypedDefinition = untypedDefinition;
        }

        public @NotNull PrismContainerValue<?> getUntypedDefinition() {
            return untypedDefinition;
        }

        @Override
        @NotNull
        public QName getBeanTypeName() {
            return untypedDefinition.getDefinition().getTypeName();
        }

        @Override
        public String toString() {
            return untypedDefinition.toString();
        }

        public static PrismContainerValue<?> getPcv(WorkDefinitionSource source) {
            if (source == null) {
                return null;
            } else if (source instanceof UntypedWorkDefinitionWrapper) {
                return ((UntypedWorkDefinitionWrapper) source).getUntypedDefinition();
            } else {
                throw new IllegalArgumentException("Expected untyped action, got " + source);
            }
        }
    }
}
