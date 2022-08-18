/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author katka
 *
 */
public interface PrismContainerWrapper<C extends Containerable> extends ItemWrapper<PrismContainer<C>, PrismContainerValueWrapper<C>>, PrismContainerDefinition<C>{

    void setExpanded(boolean expanded);

    boolean isExpanded();

    ItemStatus getStatus();

    void setVirtual(boolean virtual);

    boolean isVirtual();

    <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException;
    <T extends Containerable> PrismContainerWrapper<T> findContainer(String identifier);
    <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) throws SchemaException;
    <R extends Referencable> PrismReferenceWrapper<R> findReference(ItemPath path) throws SchemaException;
    <T extends Containerable> PrismContainerValueWrapper<T> findContainerValue(ItemPath path) throws SchemaException;
    <IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException;

    PrismContainerWrapper<? extends Containerable> getSelectedChild();

    @Experimental
    void setIdentifier(String identifier);
    String getIdentifier();

    @Experimental
    default PrismContainerWrapper<C> cloneVirtualContainerWithNewValue(PrismContainerValueWrapper<? extends Containerable> parent, ModelServiceLocator modelServiceLocator){
        throw new UnsupportedOperationException();
    }
}


