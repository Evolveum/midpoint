/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

/**
 * @author skublik
 *
 */
public class ShadowAssociationWrapperImpl extends PrismContainerWrapperImpl<ShadowAssociationType>{

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAssociationWrapperImpl.class);

    public ShadowAssociationWrapperImpl(PrismContainerValueWrapper<?> parent, PrismContainer<ShadowAssociationType> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public <D extends ItemDelta<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> Collection<D> getDelta()
            throws SchemaException {

        Collection<D> deltas = new ArrayList<D>();

        ContainerDelta<ShadowAssociationType> delta = createEmptyDelta(getDeltaPathForStatus(getStatus()));

        switch (getStatus()) {

        case ADDED:

            if (CollectionUtils.isEmpty(getValues())) {
                return null;
            }

            //we know that there is always only one value
            PrismContainerValueWrapper<ShadowAssociationType> containerValueWrappers = getValues().iterator().next();
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
                    ShadowAssociationType shadowAssociationType = new ShadowAssociationType();
                    shadowAssociationType.asPrismContainerValue().applyDefinition(getItemDefinition());
                    shadowAssociationType.setName(refWrapper.getItemName());
                    shadowAssociationType.setShadowRef(ObjectTypeUtil.createObjectRef((PrismReferenceValue) updatedRefValue.getNewValue()));
                    delta.addValueToAdd(shadowAssociationType.asPrismContainerValue());
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
                    ShadowAssociationType shadowAssociationType = new ShadowAssociationType();
                    shadowAssociationType.asPrismContainerValue().applyDefinition(getItemDefinition());
                    shadowAssociationType.setName(refWrapper.getItemName());
                    shadowAssociationType.setShadowRef(ObjectTypeUtil.createObjectRef((PrismReferenceValue) updatedRefValue.getNewValue()));

                    switch (updatedRefValue.getStatus()) {
                    case ADDED:
                        delta.addValueToAdd(shadowAssociationType.asPrismContainerValue());
                        break;
                    case NOT_CHANGED:
                        break;
                    case DELETED:
                        delta.addValueToDelete(shadowAssociationType.asPrismContainerValue());
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
                    ShadowAssociationType shadowAssociationType = new ShadowAssociationType();
                    shadowAssociationType.asPrismContainerValue().applyDefinition(getItemDefinition());
                    shadowAssociationType.setName(refWrapper.getItemName());
                    shadowAssociationType.setShadowRef(ObjectTypeUtil.createObjectRef((PrismReferenceValue) updatedRefValue.getNewValue()));
                    delta.addValueToDelete(shadowAssociationType.asPrismContainerValue());
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

//    @Override
//    public PrismContainer<ShadowAssociationType> createContainerAddDelta() throws SchemaException {
//        if (CollectionUtils.isEmpty(getValues())) {
//            return null;
//        }
//
//        PrismContainer<ShadowAssociationType> shadowAssociation = getItemDefinition().instantiate();
//
//        //we know that there is always only one value
//        PrismContainerValueWrapper<ShadowAssociationType> containerValueWrappers = getValues().iterator().next();
//        for (ItemWrapper itemWrapper : containerValueWrappers.getItems()) {
//
//            if (!(itemWrapper instanceof PrismReferenceWrapper)) {
//                LOGGER.warn("Item in shadow association value wrapper is not an reference. Should not happen.");
//                continue;
//            }
//
//            PrismReferenceWrapper refWrapper = (PrismReferenceWrapper) itemWrapper;
//            if (!refWrapper.hasChanged()) {
//                return null;
//            }
//
//            PrismReference updatedRef = refWrapper.getUpdatedItem(getItem().getPrismContext());
//
//            for (PrismReferenceValue updatedRefValue : updatedRef.getValues()) {
//                ShadowAssociationType shadowAssociationType = new ShadowAssociationType();
//                shadowAssociationType.setName(refWrapper.getName());
//                shadowAssociationType.setShadowRef(ObjectTypeUtil.createObjectRef(updatedRefValue));
//                shadowAssociation.add(shadowAssociationType.asPrismContainerValue());
//            }
//
//         }
//
//        if (shadowAssociation.isEmpty() || shadowAssociation.getValues().isEmpty()) {
//            return null;
//        }
//        return shadowAssociation;
//    }
//
//    @Override
//    public <O extends ObjectType> void collectModifications(ObjectDelta<O> delta) throws SchemaException {
//
//        if (CollectionUtils.isEmpty(getValues())) {
//            return;
//        }
//
//        ContainerValueWrapper<ShadowAssociationType> containerValueWrappers = getValues().iterator().next();
//
//        for (ItemWrapper itemWrapper : containerValueWrappers.getItems()) {
//
//            if (!(itemWrapper instanceof ReferenceWrapper)) {
//                LOGGER.warn("Item in shadow association value wrapper is not an reference. Should not happen.");
//                continue;
//            }
//
//            ReferenceWrapper refWrapper = (ReferenceWrapper) itemWrapper;
//            if (!refWrapper.hasChanged()) {
//                continue;
//            }
//
//            for (ValueWrapper refValue : refWrapper.getValues()) {
//
//                PrismReferenceValue prismRefValue = (PrismReferenceValue) refValue.getValue();
//                ShadowAssociationType shadowAssociationType = new ShadowAssociationType();
//                shadowAssociationType.setName(refWrapper.getName());
//                shadowAssociationType.setShadowRef(ObjectTypeUtil.createObjectRef(prismRefValue));
//                switch (refValue.getStatus()) {
//                    case ADDED:
//                        if (!refValue.hasValueChanged()) {
//                            continue;
//                        }
//                        delta.addModificationAddContainer(refWrapper.getPath(), shadowAssociationType);
//                        break;
//                    case DELETED:
//                        delta.addModificationDeleteContainer(refWrapper.getPath(), shadowAssociationType);
//                    default:
//                        break;
//                }
//
//
//
//            }
//        }
//
//    }
}
