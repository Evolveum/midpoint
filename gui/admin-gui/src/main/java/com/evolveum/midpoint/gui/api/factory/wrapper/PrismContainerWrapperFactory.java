/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public interface PrismContainerWrapperFactory<C extends Containerable> extends ItemWrapperFactory<PrismContainerWrapper<C>, PrismContainerValueWrapper<C>, PrismContainerValue<C>> {


    PrismContainerValueWrapper<C> createContainerValueWrapper(PrismContainerWrapper<C> objectWrapper, PrismContainerValue<C> objectValue, ValueStatus status, WrapperContext context);

//    <I extends Item> ItemWrapper<?, ?> createChildWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper, WrapperContext context) throws SchemaException;
//    <I extends Item> ItemWrapper<?, ?> createChildWrapper(PrismContainerValueWrapper<?> parent, I childItem, ItemStatus status, WrapperContext context) throws SchemaException;

}
