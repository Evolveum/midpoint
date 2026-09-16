/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Sets provided {@link MidPointTrustDescriptor} to all relevant values in a given {@link Containerable}
 * or {@link PrismContainerValue}:
 *
 * - {@link ExpressionType} objects
 * - {@link SearchFilterType} objects (from where it is propagated to inside expressions when parsing)
 * - // TODO scripting bean
 */
@NullMarked
public class TrustDescriptorSetter {

    /** Sets the provided descriptor to all expressions and scripts within the provided root container. */
    public static void setDescriptors(Containerable root, MidPointTrustDescriptor descriptor) {
        setDescriptors(root.asPrismContainerValue(), descriptor);
    }

    public static void setDescriptors(PrismContainerValue<?> root, MidPointTrustDescriptor descriptor) {
        root.accept(
                new CombinedVisitor<>(descriptor));
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static class CombinedVisitor<V extends Visitable<V>> implements Visitor<V>, JaxbVisitor {

        private final MidPointTrustDescriptor descriptor;

        private CombinedVisitor(MidPointTrustDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public void visit(JaxbVisitable visitable) {
            if (visitable instanceof ExpressionType expressionBean) {
                expressionBean.setTrustDescriptor(descriptor);
            } else {
                // Should we parse not-yet-parsed RawType here?
                JaxbVisitable.visitPrismStructure(visitable, this);
            }
        }

        @Override
        public void visit(V visitable) {
            if (visitable instanceof PrismPropertyValue<?> propertyValue) {
                Object realValue = propertyValue.getRealValue();
                if (realValue instanceof ExpressionType expressionBean) {
                    expressionBean.setTrustDescriptor(descriptor);
                } else if (realValue instanceof SearchFilterType searchFilterBean) {
                    searchFilterBean.setTrustDescriptor(descriptor);
                }
                if (realValue instanceof JaxbVisitable jaxbVisitable) {
                    // in theory, we can have expressions-in-expressions, so let's go inside
                    jaxbVisitable.accept(this);
                }
            }
        }
    }
}
