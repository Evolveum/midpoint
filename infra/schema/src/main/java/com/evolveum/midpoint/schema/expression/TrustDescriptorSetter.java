/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
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

    public static void setDescriptors(PrismValue root, MidPointTrustDescriptor descriptor) {
        root.accept(
                new CombinedVisitor<>(descriptor));
    }

    /** Sets the provided descriptor to all expressions and scripts within the provided value. */
    public static void setDescriptors(JaxbVisitable root, MidPointTrustDescriptor descriptor) {
        root.accept(
                new CombinedVisitor<>(descriptor));
    }

    public static <F extends ObjectType> void setDescriptors(
            ObjectDelta<F> delta, MidPointTrustDescriptor midPointTrustDescriptor) {
        if (delta.isAdd()) {
            setDescriptors(delta.getObjectableToAdd(), midPointTrustDescriptor);
        } else if (delta.isModify()) {
            for (ItemDelta<?, ?> modification : delta.getModifications()) {
                for (PrismValue newValue : modification.getNewValues()) {
                    setDescriptors(newValue, midPointTrustDescriptor);
                }
            }
        } else {
            assert delta.isDelete();
            // nothing to do here
        }
    }

    @SuppressWarnings("ClassCanBeRecord")
    private static class CombinedVisitor<V extends Visitable<V>>
            implements Visitor<V>, JaxbVisitor, ConfigurableVisitor<V> {

        private final MidPointTrustDescriptor descriptor;

        private CombinedVisitor(MidPointTrustDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public void visit(JaxbVisitable visitable) {
            setTrustDescriptorsIfApplicable(visitable);
            JaxbVisitable.visitPrismStructure(visitable, this);
        }

        @Override
        public void visit(V visitable) {
            if (visitable instanceof PrismPropertyValue<?> propertyValue) {
                if (propertyValue.isRaw()) {
                    ((XNodeImpl) propertyValue.getRawElement()).setTrustDescriptor(descriptor);
                } else {
                    Object realValue = propertyValue.getRealValue();
                    setTrustDescriptorsIfApplicable(realValue);
                    if (realValue instanceof JaxbVisitable jaxbVisitable) {
                        jaxbVisitable.accept(this);
                    }
                }
            } else if (visitable instanceof PrismReferenceValue referenceValue) {
                // TODO remove this code - after filter is visited by default accept method in PrismReferenceValueImpl
                SearchFilterType filter = referenceValue.getFilter();
                if (filter != null) {
                    filter.accept(this);
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

        @Override
        public boolean shouldVisitEmbeddedObjects() {
            return true;
        }
    }
}
