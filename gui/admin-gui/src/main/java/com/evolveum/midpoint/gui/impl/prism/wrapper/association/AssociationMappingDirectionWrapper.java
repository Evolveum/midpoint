/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper.association;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerWrapperImpl;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Wrapper for association mapping containers with custom delta preparation.
 * <p>
 * When a new association mapping container (e.g. inbound/objectRef) is created,
 * some child wrappers produce their own item deltas instead of modifying the
 * newly created {@link PrismContainerValue} directly. These sub-deltas have to be
 * applied to the cloned value before it is added to the parent container delta;
 * otherwise the resulting delta contains an incomplete mapping (for example,
 * an {@code objectRef} container without its {@code ref} definition), causing
 * subsequent save processing to generate an incomplete or invalid delta.</p>
 */
public class AssociationMappingDirectionWrapper extends PrismContainerWrapperImpl<MappingType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationMappingDirectionWrapper.class);

    public AssociationMappingDirectionWrapper(
            @Nullable PrismContainerValueWrapper<?> parent,
            PrismContainer<MappingType> container,
            ItemStatus status) {
        super(parent, container, status);
    }

    @SuppressWarnings({ "all" })
    @Override
    protected PrismContainerValue<MappingType> prepareValueToAdd(
            @NotNull PrismContainerValueWrapper<MappingType> pVal)
            throws SchemaException {

        PrismContainerValue<MappingType> valueToAdd = pVal.getNewValue().clone();

        for (ItemWrapper iw : pVal.getItems()) {
            Collection<ItemDelta> subDeltas = iw.getDelta();
            if (CollectionUtils.isEmpty(subDeltas)) {
                continue;
            }

            for (ItemDelta d : subDeltas) {
                ItemDelta cloned = d.clone();
                if (cloned.getParentPath() != null && cloned.getParentPath().startsWith(getPath())) {
                    cloned.setParentPath(cloned.getParentPath().remainder(getPath()));
                }
                cloned.applyTo(valueToAdd);
                LOGGER.trace("Adding delta for {}: {}", iw, cloned);
            }
        }

        return valueToAdd;
    }
}
