/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.registry;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface GuiComponentRegistry extends Serializable {

    public void addToRegistry(GuiComponentFactory factory);

    public <T> GuiComponentFactory findValuePanelFactory(ItemWrapper itemWrapper);

    void registerWrapperPanel(QName typeName, Class<?> panelClass);

    Class<?> getPanelClass(QName typeName);

    <IW extends ItemWrapper, VW extends PrismValueWrapper, PV extends PrismValue> ItemWrapperFactory<IW, VW, PV> findWrapperFactory(ItemDefinition<?> def);

    <O extends ObjectType> PrismObjectWrapperFactory<O> getObjectWrapperFactory(PrismObjectDefinition<O> objectDef);

    void addToRegistry(ItemWrapperFactory factory);


}
