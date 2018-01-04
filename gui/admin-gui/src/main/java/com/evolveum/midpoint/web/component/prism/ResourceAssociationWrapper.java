package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Created by honchar.
 */
public class ResourceAssociationWrapper extends AbstractAssociationWrapper<ResourceObjectAssociationType> {

    private static transient Trace LOGGER = TraceManager.getTrace(ShadowAssociationWrapper.class);

    ResourceAssociationWrapper(PrismContainer<ResourceObjectAssociationType> container, ContainerStatus objectStatus, ContainerStatus status, ItemPath path) {
        super(container, objectStatus, status, path);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public PrismContainer<ResourceObjectAssociationType> createContainerAddDelta() throws SchemaException {
        if (CollectionUtils.isEmpty(getValues())) {
            return null;
        }

        PrismContainer<ResourceObjectAssociationType> resourceAssociation = getItemDefinition().instantiate();

        //we know that there is always only one value
        ContainerValueWrapper<ResourceObjectAssociationType> containerValueWrappers = getValues().iterator().next();
        for (ItemWrapper itemWrapper : containerValueWrappers.getItems()) {

            if (!(itemWrapper instanceof ReferenceWrapper)) {
                LOGGER.warn("Item in shadow association value wrapper is not an reference. Should not happen.");
                continue;
            }

            ReferenceWrapper refWrapper = (ReferenceWrapper) itemWrapper;
            if (!refWrapper.hasChanged()) {
                return null;
            }

            PrismReference updatedRef = refWrapper.getUpdatedItem(getItem().getPrismContext());

            for (PrismReferenceValue updatedRefValue : updatedRef.getValues()) {
                ResourceObjectAssociationType resourceAssociationType = new ResourceObjectAssociationType();
//                resourceAssociationType.setName(refWrapper.getName());
//                resourceAssociationType.setref(ObjectTypeUtil.createObjectRef(updatedRefValue));
                resourceAssociation.add(resourceAssociationType.asPrismContainerValue());
            }

        }

        if (resourceAssociation.isEmpty() || resourceAssociation.getValues().isEmpty()) {
            return null;
        }
        return resourceAssociation;
    }

    @Override
    public <O extends ObjectType> void collectModifications(ObjectDelta<O> delta) throws SchemaException {

        if (CollectionUtils.isEmpty(getValues())) {
            return;
        }

        ContainerValueWrapper<ResourceObjectAssociationType> containerValueWrappers = getValues().iterator().next();

        for (ItemWrapper itemWrapper : containerValueWrappers.getItems()) {

            if (!(itemWrapper instanceof ReferenceWrapper)) {
                LOGGER.warn("Item in shadow association value wrapper is not an reference. Should not happen.");
                continue;
            }

            ReferenceWrapper refWrapper = (ReferenceWrapper) itemWrapper;
            if (!refWrapper.hasChanged()) {
                continue;
            }

            for (ValueWrapper refValue : refWrapper.getValues()) {

                PrismReferenceValue prismRefValue = (PrismReferenceValue) refValue.getValue();
                ShadowAssociationType shadowAssociationType = new ShadowAssociationType();
                shadowAssociationType.setName(refWrapper.getName());
                shadowAssociationType.setShadowRef(ObjectTypeUtil.createObjectRef(prismRefValue));
                switch (refValue.getStatus()) {
                    case ADDED:
                        if (!refValue.hasValueChanged()) {
                            continue;
                        }
                        delta.addModificationAddContainer(refWrapper.getPath(), shadowAssociationType);
                        break;
                    case DELETED:
                        delta.addModificationDeleteContainer(refWrapper.getPath(), shadowAssociationType);
                    default:
                        break;
                }



            }
        }

    }


}

