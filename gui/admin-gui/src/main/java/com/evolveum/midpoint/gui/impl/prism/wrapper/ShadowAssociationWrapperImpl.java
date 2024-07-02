/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ShadowAssociationsUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author skublik
 *
 */
public class ShadowAssociationWrapperImpl extends PrismContainerWrapperImpl<ShadowAssociationValueType>{

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAssociationWrapperImpl.class);

    public ShadowAssociationWrapperImpl(PrismContainerValueWrapper<?> parent, PrismContainer<ShadowAssociationValueType> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public <D extends ItemDelta<?,?>> Collection<D> getDelta()
            throws SchemaException {

        Collection<D> deltas = new ArrayList<D>();

        ContainerDelta<ShadowAssociationValueType> delta = createEmptyDelta(ItemPath.create(ShadowType.F_ASSOCIATIONS,
                getDeltaPathForStatus(getStatus())));

        switch (getStatus()) {

        case ADDED:

            if (CollectionUtils.isEmpty(getValues())) {
                return null;
            }

            //we know that there is always only one value
            PrismContainerValueWrapper<ShadowAssociationValueType> containerValueWrappers = getValues().iterator().next();
            for (ItemWrapper itemWrapper : containerValueWrappers.getItems()) {
                if (!(itemWrapper instanceof PrismReferenceWrapper)) {
                    LOGGER.warn("Item in shadow association value wrapper is not an reference. Should not happen.");
                    continue;
                }
                PrismReferenceWrapper refWrapper = (PrismReferenceWrapper) itemWrapper;
                for (PrismReferenceValueWrapperImpl updatedRefValue : (List<PrismReferenceValueWrapperImpl>)refWrapper.getValues()) {
                    if(updatedRefValue.getNewValue().isEmpty()) {
                        continue;
                    }
                    var associationValue = ShadowAssociationsUtil.createSingleRefRawValue(
                            getItemName(), ObjectTypeUtil.createObjectRef(updatedRefValue.getNewValue()));
                    delta.addValueToAdd(
                            associationValue.asPrismContainerValue().applyDefinition(getItemDefinition()));
                }

             }
            if (delta.isEmpty()) {
                return null;
            }
            return (Collection) MiscUtil.createCollection(delta);
        case NOT_CHANGED:

            containerValueWrappers = getValues().iterator().next();
            for (ItemWrapper itemWrapper : containerValueWrappers.getItems()) {

                if (!(itemWrapper instanceof PrismReferenceWrapper)) {
                    LOGGER.warn("Item in shadow association value wrapper is not an reference. Should not happen.");
                    continue;
                }

                PrismReferenceWrapper refWrapper = (PrismReferenceWrapper) itemWrapper;

                for (PrismReferenceValueWrapperImpl updatedRefValue : (List<PrismReferenceValueWrapperImpl>)refWrapper.getValues()) {
                    if(updatedRefValue.getNewValue().isEmpty()) {
                        continue;
                    }
                    var associationValue = ShadowAssociationsUtil.createSingleRefRawValue(
                            getItemName(), ObjectTypeUtil.createObjectRef(updatedRefValue.getNewValue()));
                    var adapted = associationValue.asPrismContainerValue().applyDefinition(getItemDefinition());

                    switch (updatedRefValue.getStatus()) {
                        case ADDED:
                            delta.addValueToAdd(adapted);
                            break;
                        case NOT_CHANGED:
                            break;
                        case MODIFIED:
                            if (updatedRefValue.getParent().isSingleValue()) {
                                if (updatedRefValue.getNewValue().isEmpty())  {
                                    // if old value is empty, nothing to do.
                                    if (!updatedRefValue.getOldValue().isEmpty()) {
                                        delta.addValueToDelete(associationValue.asPrismContainerValue());
                                    }
                                } else {
                                    delta.addValueToReplace(associationValue.asPrismContainerValue());
                                }
                                break;
                            }

                            if (!updatedRefValue.getNewValue().isEmpty()) {
                                delta.addValueToAdd(associationValue.asPrismContainerValue());
                            }
                            if (!updatedRefValue.getOldValue().isEmpty()) {
                                var oldAssociationValue = ShadowAssociationsUtil.createSingleRefRawValue(
                                        getItemName(),
                                        ObjectTypeUtil.createObjectRef((PrismReferenceValue) updatedRefValue.getOldValue()));
                                oldAssociationValue.asPrismContainerValue().applyDefinition(getItemDefinition());
                                delta.addValueToDelete(oldAssociationValue.asPrismContainerValue());
                            }
                            break;

                        case DELETED:
                            delta.addValueToDelete(adapted);
                            break;
                    }
                }
            }
            break;
        case DELETED :
            containerValueWrappers = getValues().iterator().next();
            for (ItemWrapper itemWrapper : containerValueWrappers.getItems()) {

                if (!(itemWrapper instanceof PrismReferenceWrapper)) {
                    LOGGER.warn("Item in shadow association value wrapper is not an reference. Should not happen.");
                    continue;
                }

                PrismReferenceWrapper refWrapper = (PrismReferenceWrapper) itemWrapper;

                for (PrismReferenceValueWrapperImpl updatedRefValue : (List<PrismReferenceValueWrapperImpl>)refWrapper.getValues()) {
                    if(updatedRefValue.getNewValue().isEmpty()) {
                        continue;
                    }
                    var associationValue = ShadowAssociationsUtil.createSingleRefRawValue(
                            getItemName(), ObjectTypeUtil.createObjectRef(updatedRefValue.getNewValue()));
                    delta.addValueToDelete(
                            associationValue.asPrismContainerValue().applyDefinition(getItemDefinition()));
                }
            }
            break;
        }

        if (!delta.isEmpty()) {
            deltas.add((D) delta);
        }

        if (deltas.isEmpty()) {
            return null;
        }

        return deltas;
    }

}
