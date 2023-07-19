/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.registry;

import java.io.Serializable;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface GuiComponentRegistry extends Serializable {

    void addToRegistry(GuiComponentFactory<?> factory);

    <T extends ItemPanelContext<?, ?>> GuiComponentFactory<T> findValuePanelFactory(ItemWrapper<?, ?> parentItemWrapper, PrismValueWrapper<?> valueWrapper);

    void registerWrapperPanel(QName typeName, Class<?> panelClass);

    Class<?> getPanelClass(QName typeName);

    <IW extends ItemWrapper, VW extends PrismValueWrapper, PV extends PrismValue, C extends Containerable> ItemWrapperFactory<IW, VW, PV>
    findWrapperFactory(ItemDefinition<?> def, PrismContainerValue<C> parent);

    <C extends Containerable> PrismContainerWrapperFactory<C> findContainerWrapperFactory(PrismContainerDefinition<C> def);

    <O extends ObjectType> PrismObjectWrapperFactory<O> getObjectWrapperFactory(PrismObjectDefinition<O> objectDef);

    void addToRegistry(ItemWrapperFactory factory);
}
