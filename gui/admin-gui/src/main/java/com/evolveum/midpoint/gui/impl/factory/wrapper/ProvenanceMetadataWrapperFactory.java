/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ProvenanceMetadataPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;

import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

@Component
public class ProvenanceMetadataWrapperFactory extends MetadataWrapperFactoryImpl<ProvenanceMetadataType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return QNameUtil.match(def.getTypeName(), new QName(ProvenanceMetadataType.COMPLEX_TYPE.getNamespaceURI(), "ProvenanceMetadata")); //TODO temporary hack
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    public void registerWrapperPanel(PrismContainerWrapper<ProvenanceMetadataType> wrapper) {
        getRegistry().registerWrapperPanel(wrapper.getTypeName(), ProvenanceMetadataPanel.class);
    }
}
