/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.MetadataContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StorageMetadataType;

import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.List;

@Component
public class MetadataWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {

    @Override
    public int getOrder() {
        return super.getOrder() - 10000;
    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismContainerDefinition;
    }

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def, parent)) {
            return false;
        }

        // Recently, this line started to fail to compile. It is somewhat logical, as ValueMetadata is a PrismContainer,
        // not a PrismContainerValue (parent), hence that "instanceof" cannot return true.
//        return parent instanceof ValueMetadata;
        return false;
    }

    @Override
    public void registerWrapperPanel(PrismContainerWrapper<C> wrapper) {
        getRegistry().registerWrapperPanel(wrapper.getTypeName(), MetadataContainerPanel.class);
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<C> item, WrapperContext context) {
        return false;
    }

    @Override
    public PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def, WrapperContext context) throws SchemaException {
        WrapperContext ctx = context.clone();
        ctx.setReadOnly(true);
        ctx.setMetadata(true);
        PrismContainerWrapper<C> wrapper = super.createWrapper(parent, def, ctx);
        return wrapper;
    }

}
