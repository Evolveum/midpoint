/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.prism.impl.binding.AbstractPlainStructured;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.jspecify.annotations.Nullable;

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

    /** Sets the provided descriptor to all expressions and scripts within the provided value. */
    public static void setDescriptors(AbstractPlainStructured root, MidPointTrustDescriptor descriptor) {
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
            setTrustDescriptorsIfApplicable(visitable);
            // We can have expressions in bulk actions, filters in expressions, and so on - hence we go deeper even if we
            // set the trust descriptor on the current object.
            JaxbVisitable.visitPrismStructure(visitable, this);
        }

        @Override
        public void visit(V visitable) {
            if (visitable instanceof PrismPropertyValue<?> propertyValue) {
                Object realValue = propertyValue.getRealValue();
                setTrustDescriptorsIfApplicable(realValue);
                // We can have expressions in bulk actions, filters in expressions, and so on - hence we go deeper even if we
                // set the trust descriptor on the current object.
                if (realValue instanceof JaxbVisitable jaxbVisitable) {
                    jaxbVisitable.accept(this);
                }
            }
        }

        private void setTrustDescriptorsIfApplicable(@Nullable Object value) {
            if (value instanceof ExpressionType expressionBean) {
                expressionBean.setTrustDescriptor(descriptor);
            } else if (value instanceof SearchFilterType searchFilterBean) {
                searchFilterBean.setTrustDescriptor(descriptor);
            } else if (value instanceof ExecuteScriptType executeScriptBean) {
                executeScriptBean.setTrustDescriptor(descriptor);
            }
        }
    }
}
