/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.Channel;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;

@Component
public class ProvenanceMetadataWrapperFactory extends MetadataWrapperFactoryImpl<ProvenanceMetadataType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return QNameUtil.match(def.getTypeName(), ProvenanceMetadataType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 9;
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<ProvenanceMetadataType> item, WrapperContext context) {
        return false;
    }

    //    @Override
//    protected PrismContainerValue<ProvenanceAcquisitionType> createNewValue(PrismContainer<ProvenanceAcquisitionType> item) {
//        PrismContainerValue<ProvenanceAcquisitionType> newValue = super.createNewValue(item);
//        ProvenanceAcquisitionType acquisitionType = newValue.asContainerable();
//        GuiProfiledPrincipal principal = SecurityUtils.getPrincipalUser();
//        if (principal != null) {
//            FocusType focus = principal.getFocus();
//            if (focus != null) {
//                acquisitionType.setActorRef(ObjectTypeUtil.createObjectRef(focus, getPrismContext()));
//            }
//        }
//        acquisitionType.setChannel(Channel.USER.getChannel());
//        return newValue;
//    }

    //
//    @Override
//    public void registerWrapperPanel(PrismContainerWrapper<ProvenanceMetadataType> wrapper) {
//        getRegistry().registerWrapperPanel(wrapper.getTypeName(), ProvenanceMetadataPanel.class);
//    }
}
