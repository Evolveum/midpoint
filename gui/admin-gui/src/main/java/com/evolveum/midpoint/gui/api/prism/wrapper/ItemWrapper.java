/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Revivable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

/**
 * @author katka
 *
 */
public interface ItemWrapper<I extends Item, VW extends PrismValueWrapper> extends ItemDefinition<I>, Revivable, DebugDumpable, Serializable {


    String debugDump(int indent);

    void setVisibleOverwrite(UserInterfaceElementVisibilityType visible);
    UserInterfaceElementVisibilityType getVisibleOverwrite();
    boolean isVisible(PrismContainerValueWrapper<?> parentContainer, ItemVisibilityHandler visibilityHandler);

    boolean checkRequired(PageBase pageBase);

    PrismContainerValueWrapper<?> getParent();

    boolean isShowEmpty();

    void setShowEmpty(boolean isShowEmpty, boolean recursive);

    boolean isShowInVirtualContainer();

    void setShowInVirtualContainer(boolean showInVirtualContainer);

    ItemPath getPath();

    //NEW

    boolean isReadOnly();

    void setReadOnly(boolean readOnly);

    ExpressionType getFormComponentValidator();

    List<VW> getValues();
    VW getValue() throws SchemaException;

//    boolean isStripe();
//    void setStripe(boolean stripe);

    I getItem();

    boolean isColumn();
    void setColumn(boolean column);

    <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> getDelta() throws SchemaException;

    ItemStatus findObjectStatus();

    <OW extends PrismObjectWrapper<O>, O extends ObjectType> OW findObjectWrapper();

    ItemStatus getStatus();

    boolean isEmpty();

    void remove(VW valueWrapper, ModelServiceLocator locator) throws SchemaException;
    void removeAll(ModelServiceLocator locator) throws SchemaException;

    <PV extends PrismValue> void add(PV newValueWrapper, ModelServiceLocator locator) throws SchemaException;

    boolean isMetadata();
    void setMetadata(boolean isMetadata);

    void setShowMetadataDetails(boolean showMetadataDetails);
    boolean isShowMetadataDetails();

    boolean isProcessProvenanceMetadata();
    void setProcessProvenanceMetadata(boolean processProvenanceMetadata);
    boolean isValidated();

    void setValidated(boolean validated);
}
