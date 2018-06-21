package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by honchar.
 */
public abstract class AbstractAssociationWrapper<C extends Containerable> extends ContainerWrapper<C> {
    AbstractAssociationWrapper(PrismContainer<C> container, ContainerStatus objectStatus, ContainerStatus status, ItemPath path) {
        super(container, objectStatus, status, path);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public abstract PrismContainer<C> createContainerAddDelta() throws SchemaException;

    @Override
    public abstract <O extends ObjectType> void collectModifications(ObjectDelta<O> delta) throws SchemaException;

}
