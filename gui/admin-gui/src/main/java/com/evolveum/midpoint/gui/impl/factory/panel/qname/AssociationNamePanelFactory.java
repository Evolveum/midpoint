/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.factory.panel.TextPanelFactory;
import com.evolveum.midpoint.gui.impl.validator.AssociationNameValidator;
import com.evolveum.midpoint.gui.impl.validator.MappingNameValidator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractMappingType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SimulatedReferenceTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SimulatedReferenceTypeParticipantType;

import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

@Component
public class AssociationNamePanelFactory extends QNameTextPanelFactory {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (!DOMUtil.XSD_QNAME.equals(wrapper.getTypeName())) {
            return false;
        }

        if (ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_NAME).equivalent(
                wrapper.getPath().namedSegmentsOnly())) {
            return true;
        }

        return false;
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        panelCtx.setMandatoryHandler(itemWrapper -> true);
        return super.getPanel(panelCtx);
    }

    @Override
    public void configure(PrismPropertyPanelContext<QName> panelCtx, org.apache.wicket.Component component) {
        super.configure(panelCtx, component);
        InputPanel panel = (InputPanel) component;
        panel.getValidatableComponent().add(new AssociationNameValidator(panelCtx.getItemWrapperModel()));
    }

    @Override
    public Integer getOrder() {
        return 900;
    }
}
