/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
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
public interface ItemWrapper<V extends PrismValue, I extends Item<V, ID>, ID extends ItemDefinition<I>, VW extends PrismValueWrapper> extends ItemDefinition<I>, Revivable, DebugDumpable, Serializable {


    String debugDump(int indent);

    void setVisibleOverwrite(UserInterfaceElementVisibilityType visible);
    boolean isVisible(PrismContainerValueWrapper parentContainer, ItemVisibilityHandler visibilityHandler);

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

    boolean isStripe();
    void setStripe(boolean stripe);

    I getItem();

    boolean isColumn();
    void setColumn(boolean column);

    <D extends ItemDelta<V, ID>> void applyDelta(D delta) throws SchemaException;

    <D extends ItemDelta<V, ID>> Collection<D> getDelta() throws SchemaException;

    <O extends ObjectType> ItemStatus findObjectStatus();

    <OW extends PrismObjectWrapper<O>, O extends ObjectType> OW findObjectWrapper();

    ItemStatus getStatus();
}
